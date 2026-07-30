package com.fanhua.jobtrack.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.UnauthorizedException;
import com.fanhua.jobtrack.config.properties.JwtProperties;
import com.fanhua.jobtrack.infrastructure.audit.AuditAction;
import com.fanhua.jobtrack.infrastructure.audit.AuditLogService;
import com.fanhua.jobtrack.module.auth.entity.AuthSession;
import com.fanhua.jobtrack.module.auth.mapper.AuthSessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * 登录会话与 Refresh Token Rotation。
 *
 * 核心设计：
 * - Refresh Token 形如 {@code sessionId.secret}，服务端只存 SHA-256 哈希；
 * - 轮换通过一条带哈希条件的 UPDATE 原子完成，影响行数为 0 表示并发或重放；
 * - 会话撤销以数据库为准，Redis 仅作"已撤销"快速标记；Redis 故障时降级查库，
 *   Access Token 短生命周期（30 分钟）作为最终兜底。
 */
@Slf4j
@Service
public class AuthSessionService {

    private static final String REDIS_REVOKED_PREFIX = "jobtrack:auth:session:revoked:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder B64_URL = Base64.getUrlEncoder().withoutPadding();

    private final AuthSessionMapper sessionMapper;
    private final StringRedisTemplate redisTemplate;
    private final AuditLogService auditLogService;
    private final long refreshTtlDays;

    public AuthSessionService(AuthSessionMapper sessionMapper,
                              StringRedisTemplate redisTemplate,
                              AuditLogService auditLogService,
                              JwtProperties jwtProperties) {
        this.sessionMapper = sessionMapper;
        this.redisTemplate = redisTemplate;
        this.auditLogService = auditLogService;
        this.refreshTtlDays = jwtProperties.getRefreshTtlDays();
    }

    /** 新建会话，返回明文 Refresh Token（仅此一次返回，服务端不落盘） */
    public IssuedSession createSession(Long userId, String deviceName, String userAgent, String ip) {
        LocalDateTime now = LocalDateTime.now();
        AuthSession session = new AuthSession();
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        session.setId(sessionId);
        session.setUserId(userId);
        session.setDeviceName(deviceName);
        session.setUserAgent(userAgent);
        session.setIpAddress(ip);
        session.setCreatedAt(now);
        session.setLastActiveAt(now);
        session.setExpiresAt(now.plusDays(refreshTtlDays));

        String plainToken = generatePlainToken(sessionId);
        session.setRefreshTokenHash(sha256Hex(plainToken));
        sessionMapper.insert(session);
        return new IssuedSession(sessionId, plainToken, session.getExpiresAt());
    }

    /**
     * 旋转 Refresh Token。
     * 原子 UPDATE 保证同一时刻只有一个请求能用旧令牌换到新令牌；
     * 旧令牌再次出现即判定为可能的重放攻击：撤销会话 + 安全日志 + 明确错误。
     * 注意 noRollbackFor：重放检测路径中"撤销会话"的写入必须提交，不能随异常回滚。
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public IssuedSession rotate(String plainToken) {
        ParsedRefreshToken parsed = parse(plainToken);
        LocalDateTime now = LocalDateTime.now();

        AuthSession session = sessionMapper.selectById(parsed.sessionId());
        if (session == null) {
            throw unauthorized(ErrorCode.AUTH_REFRESH_INVALID);
        }
        assertSessionUsable(session, now);

        String newPlainToken = generatePlainToken(parsed.sessionId());
        int affected = sessionMapper.rotateRefreshToken(
                parsed.sessionId(), sha256Hex(plainToken), sha256Hex(newPlainToken), now);

        if (affected == 0) {
            // 未命中条件：区分过期/已撤销/哈希不匹配
            AuthSession current = sessionMapper.selectById(parsed.sessionId());
            if (current == null) {
                throw unauthorized(ErrorCode.AUTH_REFRESH_INVALID);
            }
            assertSessionUsable(current, now);
            // 会话仍有效但哈希不匹配：旧令牌被重放，立即撤销会话
            LocalDateTime revokeTime = LocalDateTime.now();
            sessionMapper.revokeById(parsed.sessionId(), revokeTime);
            markRevokedInRedis(parsed.sessionId());
            log.warn("检测到旧 Refresh Token 重放，会话已撤销: sessionId={}, userId={}",
                    parsed.sessionId(), current.getUserId());
            auditLogService.record(AuditAction.REFRESH_REPLAY_DETECTED, current.getUserId(),
                    AuditAction.RES_AUTH_SESSION, parsed.sessionId(), true,
                    "旧 Refresh Token 重放，会话强制撤销", current.getIpAddress(),
                    current.getUserAgent(), org.slf4j.MDC.get("traceId"));
            throw unauthorized(ErrorCode.AUTH_REFRESH_REUSED);
        }

        return new IssuedSession(parsed.sessionId(), newPlainToken, session.getExpiresAt());
    }

    /** 查询会话撤销状态：Redis 标记优先，Redis 异常降级查库 */
    public boolean isSessionRevoked(String sessionId) {
        try {
            String value = redisTemplate.opsForValue().get(REDIS_REVOKED_PREFIX + sessionId);
            return "1".equals(value);
        } catch (Exception e) {
            log.warn("Redis 撤销标记查询失败，降级查询数据库: sessionId={}, error={}", sessionId, e.getMessage());
            AuthSession session = sessionMapper.selectById(sessionId);
            return session == null || session.getRevokedAt() != null;
        }
    }

    /** 撤销指定会话（同时写 Redis 快速标记） */
    @Transactional
    public void revoke(String sessionId) {
        sessionMapper.revokeById(sessionId, LocalDateTime.now());
        markRevokedInRedis(sessionId);
    }

    /** 撤销用户全部会话；exceptSessionId 非空时保留该设备 */
    @Transactional
    public void revokeAll(Long userId, String exceptSessionId) {
        sessionMapper.revokeByUserId(userId, exceptSessionId, LocalDateTime.now());
        // 批量撤销时逐条打 Redis 标记成本高，改为列出会话后批量打标
        LambdaQueryWrapper<AuthSession> wrapper = new LambdaQueryWrapper<AuthSession>()
                .select(AuthSession::getId)
                .eq(AuthSession::getUserId, userId)
                .ne(exceptSessionId != null, AuthSession::getId, exceptSessionId);
        List<AuthSession> sessions = sessionMapper.selectList(wrapper);
        for (AuthSession s : sessions) {
            markRevokedInRedis(s.getId());
        }
    }

    /** 按会话 ID 查询（不做归属过滤，调用方负责鉴权判断） */
    public AuthSession getById(String sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    /** 用户有效会话列表（未撤销且未过期） */
    public List<AuthSession> listActiveSessions(Long userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<AuthSession>()
                .eq(AuthSession::getUserId, userId)
                .isNull(AuthSession::getRevokedAt)
                .gt(AuthSession::getExpiresAt, LocalDateTime.now())
                .orderByDesc(AuthSession::getLastActiveAt));
    }

    /** 校验会话归属后撤销（防止越权注销他人设备） */
    @Transactional
    public boolean revokeOwned(String sessionId, Long userId) {
        AuthSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return false;
        }
        revoke(sessionId);
        return true;
    }

    private void assertSessionUsable(AuthSession session, LocalDateTime now) {
        if (session.getRevokedAt() != null) {
            throw unauthorized(ErrorCode.AUTH_SESSION_REVOKED);
        }
        if (!session.getExpiresAt().isAfter(now)) {
            throw unauthorized(ErrorCode.AUTH_REFRESH_EXPIRED);
        }
    }

    private UnauthorizedException unauthorized(ErrorCode errorCode) {
        return new UnauthorizedException(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    private void markRevokedInRedis(String sessionId) {
        try {
            redisTemplate.opsForValue().set(
                    REDIS_REVOKED_PREFIX + sessionId, "1", Duration.ofDays(refreshTtlDays));
        } catch (Exception e) {
            // 数据库 revoke 是权威记录，标记失败只影响撤销生效速度，记 WARN
            log.warn("Redis 撤销标记写入失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    /** 生成明文 Refresh Token：32 位会话 ID + '.' + 43 位随机串 */
    private String generatePlainToken(String sessionId) {
        byte[] secret = new byte[32];
        SECURE_RANDOM.nextBytes(secret);
        return sessionId + "." + B64_URL.encodeToString(secret);
    }

    /** 解析 Refresh Token；格式非法统一按"无效"处理 */
    private ParsedRefreshToken parse(String plainToken) {
        if (plainToken == null) {
            throw unauthorized(ErrorCode.AUTH_REFRESH_INVALID);
        }
        int dot = plainToken.indexOf('.');
        if (dot <= 0 || dot != 32 || plainToken.length() <= dot + 1) {
            throw unauthorized(ErrorCode.AUTH_REFRESH_INVALID);
        }
        return new ParsedRefreshToken(plainToken.substring(0, dot));
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private record ParsedRefreshToken(String sessionId) {
    }

    /** 新签发的会话令牌 */
    public record IssuedSession(String sessionId, String plainRefreshToken, LocalDateTime expiresAt) {
    }
}
