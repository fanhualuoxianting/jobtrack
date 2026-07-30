package com.fanhua.jobtrack.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.enums.UserStatus;
import com.fanhua.jobtrack.common.exception.BusinessException;
import com.fanhua.jobtrack.common.exception.ConflictException;
import com.fanhua.jobtrack.common.exception.LockedException;
import com.fanhua.jobtrack.common.exception.UnauthorizedException;
import com.fanhua.jobtrack.common.exception.NotFoundException;
import com.fanhua.jobtrack.config.properties.JwtProperties;
import com.fanhua.jobtrack.infrastructure.audit.AuditAction;
import com.fanhua.jobtrack.infrastructure.audit.AuditLogService;
import com.fanhua.jobtrack.infrastructure.audit.ClientIpUtil;
import com.fanhua.jobtrack.module.auth.dto.LoginRequest;
import com.fanhua.jobtrack.module.auth.dto.RegisterRequest;
import com.fanhua.jobtrack.module.auth.entity.AuthSession;
import com.fanhua.jobtrack.module.auth.service.AuthService;
import com.fanhua.jobtrack.module.auth.service.AuthSessionService;
import com.fanhua.jobtrack.module.auth.service.LoginAttemptService;
import com.fanhua.jobtrack.module.auth.vo.AuthTokenVO;
import com.fanhua.jobtrack.module.auth.vo.RegisterVO;
import com.fanhua.jobtrack.module.auth.vo.SessionVO;
import com.fanhua.jobtrack.module.auth.vo.UserVO;
import com.fanhua.jobtrack.module.user.entity.User;
import com.fanhua.jobtrack.module.user.entity.UserSetting;
import com.fanhua.jobtrack.module.user.mapper.UserMapper;
import com.fanhua.jobtrack.module.user.mapper.UserSettingMapper;
import com.fanhua.jobtrack.security.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

/**
 * 认证业务编排。
 *
 * 安全要点：
 * - 登录失败统一返回 "账号或密码错误"，不暴露账号是否存在；
 * - 失败计数与临时锁定由 LoginAttemptService 负责；
 * - 密码 BCrypt 存储；Refresh Token 只写入 HttpOnly Cookie；
 * - 所有审计日志异步落库，失败不影响主事务。
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserSettingMapper userSettingMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthSessionService sessionService;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;
    private final JwtProperties jwtProperties;

    public AuthServiceImpl(UserMapper userMapper,
                           UserSettingMapper userSettingMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenService jwtTokenService,
                           AuthSessionService sessionService,
                           LoginAttemptService loginAttemptService,
                           AuditLogService auditLogService,
                           JwtProperties jwtProperties) {
        this.userMapper = userMapper;
        this.userSettingMapper = userSettingMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.sessionService = sessionService;
        this.loginAttemptService = loginAttemptService;
        this.auditLogService = auditLogService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public RegisterVO register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(
                    ErrorCode.REGISTER_PASSWORD_MISMATCH.getCode(),
                    ErrorCode.REGISTER_PASSWORD_MISMATCH.getDefaultMessage());
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (existsByUsername(username)) {
            throw new ConflictException(ErrorCode.USER_USERNAME_EXISTS.getCode(),
                    ErrorCode.USER_USERNAME_EXISTS.getDefaultMessage());
        }
        if (existsByEmail(email)) {
            throw new ConflictException(ErrorCode.USER_EMAIL_EXISTS.getCode(),
                    ErrorCode.USER_EMAIL_EXISTS.getDefaultMessage());
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setNickname(request.getNickname() == null || request.getNickname().isBlank()
                ? username : request.getNickname().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setStatus(UserStatus.ACTIVE.name());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 唯一约束兜底：并发注册同一用户名/邮箱时数据库拒绝，转换为 409
            String msg = e.getMessage() != null && e.getMessage().contains("email")
                    ? ErrorCode.USER_EMAIL_EXISTS.getCode() : ErrorCode.USER_USERNAME_EXISTS.getCode();
            throw new ConflictException(msg, "用户名或邮箱已被注册");
        }

        // 注册成功同时创建默认用户设置（同一事务）
        UserSetting setting = new UserSetting();
        setting.setUserId(user.getId());
        userSettingMapper.insert(setting);

        auditLogService.record(AuditAction.REGISTER, user.getId(),
                AuditAction.RES_USER, String.valueOf(user.getId()), true, "用户注册",
                null, null, org.slf4j.MDC.get("traceId"));

        RegisterVO vo = new RegisterVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        return vo;
    }

    @Override
    @Transactional
    public LoginResult login(LoginRequest request, HttpServletRequest httpRequest) {
        String account = request.getAccount().trim().toLowerCase(Locale.ROOT);
        String ip = ClientIpUtil.getClientIp(httpRequest);
        String userAgent = ClientIpUtil.getUserAgent(httpRequest);

        // 1. 锁定检查（423）
        if (loginAttemptService.isLocked(account, ip)) {
            auditLogService.record(AuditAction.LOGIN_LOCKED, null,
                    AuditAction.RES_USER, maskAccount(account), false,
                    "锁定期间的登录尝试", ip, userAgent, org.slf4j.MDC.get("traceId"));
            throw new LockedException(ErrorCode.AUTH_LOGIN_LOCKED.getCode(),
                    ErrorCode.AUTH_LOGIN_LOCKED.getDefaultMessage());
        }

        // 2. 查用户并校验密码；不存在与密码错误统一 401，不暴露账号存在性
        User user = findByAccount(account);
        boolean passwordOk = user != null
                && passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!passwordOk) {
            boolean lockedNow = loginAttemptService.recordFailure(account, ip);
            auditLogService.record(AuditAction.LOGIN_FAIL, null,
                    AuditAction.RES_USER, maskAccount(account), false,
                    "账号或密码错误", ip, userAgent, org.slf4j.MDC.get("traceId"));
            if (lockedNow) {
                throw new LockedException(ErrorCode.AUTH_LOGIN_LOCKED.getCode(),
                        ErrorCode.AUTH_LOGIN_LOCKED.getDefaultMessage());
            }
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS.getCode(),
                    ErrorCode.AUTH_INVALID_CREDENTIALS.getDefaultMessage());
        }

        // 3. 禁用账号：对外仍返回统一凭证错误；审计中记录真实原因
        if (UserStatus.DISABLED.name().equals(user.getStatus())) {
            auditLogService.record(AuditAction.LOGIN_FAIL, user.getId(),
                    AuditAction.RES_USER, maskAccount(account), false,
                    "账号被禁用", ip, userAgent, org.slf4j.MDC.get("traceId"));
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS.getCode(),
                    ErrorCode.AUTH_INVALID_CREDENTIALS.getDefaultMessage());
        }

        // 4. 成功：清失败计数、更新最后登录时间、创建会话并签发令牌
        loginAttemptService.recordSuccess(account, ip);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        AuthSessionService.IssuedSession issued = sessionService.createSession(
                user.getId(), request.getDeviceName(), userAgent, ip);
        AuthTokenVO tokenVO = buildTokenVO(user, issued.sessionId());

        auditLogService.record(AuditAction.LOGIN_SUCCESS, user.getId(),
                AuditAction.RES_AUTH_SESSION, issued.sessionId(), true, "登录成功",
                ip, userAgent, org.slf4j.MDC.get("traceId"));

        return new LoginResult(tokenVO, issued.plainRefreshToken(), refreshTtlSeconds());
    }

    @Override
    public RefreshResult refresh(String plainRefreshToken, HttpServletRequest httpRequest) {
        if (plainRefreshToken == null || plainRefreshToken.isBlank()) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID.getCode(),
                    ErrorCode.AUTH_REFRESH_INVALID.getDefaultMessage());
        }
        // 轮换原子完成；旧令牌重放在服务内检测并撤销会话
        AuthSessionService.IssuedSession rotated = sessionService.rotate(plainRefreshToken);

        AuthSession session = sessionService.getById(rotated.sessionId());
        if (session == null) {
            throw new UnauthorizedException(
                    ErrorCode.AUTH_REFRESH_INVALID.getCode(), ErrorCode.AUTH_REFRESH_INVALID.getDefaultMessage());
        }
        User user = userMapper.selectById(session.getUserId());
        if (user == null || UserStatus.DISABLED.name().equals(user.getStatus())) {
            sessionService.revoke(rotated.sessionId());
            throw new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_DISABLED.getCode(),
                    ErrorCode.AUTH_ACCOUNT_DISABLED.getDefaultMessage());
        }

        AuthTokenVO tokenVO = buildTokenVO(user, rotated.sessionId());
        auditLogService.record(AuditAction.REFRESH_TOKEN, user.getId(),
                AuditAction.RES_AUTH_SESSION, rotated.sessionId(), true, "令牌刷新",
                ClientIpUtil.getClientIp(httpRequest), ClientIpUtil.getUserAgent(httpRequest),
                org.slf4j.MDC.get("traceId"));
        return new RefreshResult(tokenVO, rotated.plainRefreshToken(), refreshTtlSeconds());
    }

    @Override
    public void logout(Long userId, String sessionId) {
        sessionService.revoke(sessionId);
        auditLogService.record(AuditAction.LOGOUT, userId,
                AuditAction.RES_AUTH_SESSION, sessionId, true, "注销当前设备",
                null, null, org.slf4j.MDC.get("traceId"));
    }

    @Override
    public void logoutAll(Long userId, String currentSessionId, boolean keepCurrent) {
        String except = keepCurrent ? currentSessionId : null;
        sessionService.revokeAll(userId, except);
        auditLogService.record(AuditAction.LOGOUT_ALL, userId,
                AuditAction.RES_AUTH_SESSION, currentSessionId, true,
                keepCurrent ? "注销除当前设备外全部设备" : "注销全部设备",
                null, null, org.slf4j.MDC.get("traceId"));
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return toUserVO(user);
    }

    @Override
    public List<SessionVO> listSessions(Long userId, String currentSessionId) {
        return sessionService.listActiveSessions(userId).stream()
                .map(s -> toSessionVO(s, s.getId().equals(currentSessionId)))
                .toList();
    }

    @Override
    public void revokeSession(Long userId, String sessionId) {
        boolean revoked = sessionService.revokeOwned(sessionId, userId);
        if (!revoked) {
            // 会话不存在或不属于当前用户，统一按不存在处理，避免泄露他人会话存在性
            throw new NotFoundException("会话不存在");
        }
        auditLogService.record(AuditAction.SESSION_REVOKED, userId,
                AuditAction.RES_AUTH_SESSION, sessionId, true, "注销指定设备",
                null, null, org.slf4j.MDC.get("traceId"));
    }

    // ---------- 内部辅助 ----------

    private boolean existsByUsername(String username) {
        return userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private boolean existsByEmail(String email) {
        return userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }

    private User findByAccount(String account) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getEmail, account)
                .or()
                .eq(User::getUsername, account)
                .last("LIMIT 1");
        return userMapper.selectOne(wrapper);
    }

    private AuthTokenVO buildTokenVO(User user, String sessionId) {
        String role = "ROLE_" + user.getRole();
        String accessToken = jwtTokenService.createAccessToken(
                user.getId(), user.getUsername(), List.of(role), sessionId);
        AuthTokenVO vo = new AuthTokenVO();
        vo.setAccessToken(accessToken);
        vo.setExpiresIn(jwtTokenService.getAccessTtlSeconds());
        vo.setUser(toUserVO(user));
        return vo;
    }

    private long refreshTtlSeconds() {
        return jwtProperties.getRefreshTtlDays() * 24 * 3600;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setLastLoginAt(toOffset(user.getLastLoginAt()));
        return vo;
    }

    private SessionVO toSessionVO(AuthSession session, boolean current) {
        SessionVO vo = new SessionVO();
        vo.setSessionId(session.getId());
        vo.setDeviceName(session.getDeviceName());
        vo.setUserAgent(session.getUserAgent());
        vo.setIpAddress(session.getIpAddress());
        vo.setCreatedAt(toOffset(session.getCreatedAt()));
        vo.setLastActiveAt(toOffset(session.getLastActiveAt()));
        vo.setExpiresAt(toOffset(session.getExpiresAt()));
        vo.setCurrent(current);
        return vo;
    }

    /** 数据库 DATETIME 无时区，按系统配置的 Asia/Shanghai 解释为 +08:00 */
    private OffsetDateTime toOffset(LocalDateTime local) {
        if (local == null) {
            return null;
        }
        ZoneOffset offset = ZoneId.of("Asia/Shanghai").getRules().getOffset(local.atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        return local.atOffset(offset);
    }

    /** 账号脱敏用于审计展示 */
    private String maskAccount(String account) {
        return com.fanhua.jobtrack.common.util.MaskingUtils.maskAccount(account);
    }
}
