package com.fanhua.jobtrack.security;

import com.fanhua.jobtrack.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 签发与校验。
 * 签名算法固定 HS256，密钥长度启动时校验，过期/签名错误分类抛出。
 */
@Slf4j
@Service
public class JwtTokenService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final long accessTtlSeconds;

    public JwtTokenService(JwtProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT 密钥未配置或过短（至少 32 字节），请通过 JWT_SECRET 环境变量注入");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = properties.getAccessTtlMinutes() * 60;
    }

    /**
     * 签发 Access Token。
     * claims: sub=用户名, userId, sessionId, roles, jti, iat, exp
     */
    public String createAccessToken(Long userId, String username, List<String> roles, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("sessionId", sessionId)
                .claim("roles", roles)
                .id(UUID.randomUUID().toString().replace("-", ""))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    /**
     * 解析并校验 Token。过期与签名无效分别抛出，供过滤器区分错误码。
     */
    public Claims parse(String token) throws ExpiredJwtException, SignatureException, JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
