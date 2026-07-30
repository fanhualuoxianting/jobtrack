package com.fanhua.jobtrack.security;

import com.fanhua.jobtrack.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 服务单元测试：签发、解析、签名篡改、过期、密钥强度
 */
class JwtTokenServiceTest {

    private JwtTokenService service;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-0123456789abcdef");
        properties.setAccessTtlMinutes(30);
        service = new JwtTokenService(properties);
    }

    @Test
    @DisplayName("签发的 Token 包含全部必要声明")
    void createAndParse() {
        String token = service.createAccessToken(42L, "fanhua", List.of("ROLE_USER"), "sid0123");

        Claims claims = service.parse(token);
        assertEquals("fanhua", claims.getSubject());
        assertEquals(42L, claims.get("userId", Long.class));
        assertEquals("sid0123", claims.get("sessionId", String.class));
        assertEquals(List.of("ROLE_USER"), claims.get("roles", List.class));
        assertNotNull(claims.getId());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().getTime() > System.currentTimeMillis());
        assertEquals(1800, service.getAccessTtlSeconds());
    }

    @Test
    @DisplayName("篡改签名的 Token 被拒绝")
    void tamperedTokenRejected() {
        String token = service.createAccessToken(1L, "demo", List.of("ROLE_USER"), "s1");
        // 修改 payload 区段一个字符（保持结构合法但签名失效）
        int lastDot = token.lastIndexOf('.');
        String tampered = token.substring(0, lastDot - 1)
                + (token.charAt(lastDot - 1) == 'a' ? 'b' : 'a')
                + token.substring(lastDot);
        assertThrows(JwtException.class, () -> service.parse(tampered));
    }

    @Test
    @DisplayName("其他密钥签发的 Token 被拒绝")
    void foreignKeyRejected() {
        JwtProperties other = new JwtProperties();
        other.setSecret("another-secret-key-0123456789abcdef00");
        JwtTokenService otherService = new JwtTokenService(other);
        String foreignToken = otherService.createAccessToken(1L, "demo", List.of("ROLE_USER"), "s1");

        assertThrows(JwtException.class, () -> service.parse(foreignToken));
    }

    @Test
    @DisplayName("过期的 Token 抛出 ExpiredJwtException")
    void expiredTokenRejected() {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("unit-test-secret-key-0123456789abcdef");
        shortLived.setAccessTtlMinutes(0); // 立即过期
        String token = new JwtTokenService(shortLived)
                .createAccessToken(1L, "demo", List.of("ROLE_USER"), "s1");

        assertThrows(ExpiredJwtException.class, () -> service.parse(token));
    }

    @Test
    @DisplayName("密钥过短时启动失败")
    void shortSecretRejected() {
        JwtProperties weak = new JwtProperties();
        weak.setSecret("too-short");
        assertThrows(IllegalStateException.class, () -> new JwtTokenService(weak));
    }

    @Test
    @DisplayName("密钥缺失时启动失败")
    void blankSecretRejected() {
        JwtProperties empty = new JwtProperties();
        empty.setSecret("");
        assertThrows(IllegalStateException.class, () -> new JwtTokenService(empty));
    }
}
