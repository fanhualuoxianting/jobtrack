package com.fanhua.jobtrack.security;

import com.fanhua.jobtrack.module.auth.service.AuthSessionService;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器。
 * - 无 Token：直接放行，由授权规则和 EntryPoint 决定是否 401；
 * - Token 非法：不中断链条，但在请求属性中标记错误码，由 EntryPoint 输出规范 JSON；
 * - Token 合法：检查会话撤销标记后写入 SecurityContext。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** EntryPoint 读取此属性决定返回的错误码 */
    public static final String ATTR_AUTH_ERROR_CODE = "JOBTRACK_AUTH_ERROR_CODE";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final AuthSessionService authSessionService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, AuthSessionService authSessionService) {
        this.jwtTokenService = jwtTokenService;
        this.authSessionService = authSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = jwtTokenService.parse(token);
            // jjwt 反序列化数字可能为 Integer/Long，统一按 Number 处理
            Number userIdNum = claims.get("userId", Number.class);
            Long userId = userIdNum == null ? null : userIdNum.longValue();
            String sessionId = claims.get("sessionId", String.class);
            String username = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            if (userId == null || sessionId == null || username == null || roles == null) {
                markError(request, ErrorCode.AUTH_TOKEN_INVALID);
                filterChain.doFilter(request, response);
                return;
            }

            // 会话撤销检查（Redis 快速标记 + Redis 故障降级查库）
            if (authSessionService.isSessionRevoked(sessionId)) {
                markError(request, ErrorCode.AUTH_SESSION_REVOKED);
                filterChain.doFilter(request, response);
                return;
            }

            LoginUser loginUser = new LoginUser(userId, username, sessionId, roles);
            var authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
            var authentication = new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException e) {
            markError(request, ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (SignatureException e) {
            markError(request, ErrorCode.AUTH_TOKEN_INVALID);
        } catch (JwtException | IllegalArgumentException e) {
            markError(request, ErrorCode.AUTH_TOKEN_INVALID);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void markError(HttpServletRequest request, ErrorCode errorCode) {
        request.setAttribute(ATTR_AUTH_ERROR_CODE, errorCode.getCode());
    }
}
