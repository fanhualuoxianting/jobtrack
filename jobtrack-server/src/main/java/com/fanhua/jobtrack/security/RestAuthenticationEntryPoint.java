package com.fanhua.jobtrack.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanhua.jobtrack.common.api.Result;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证入口点：统一返回规范 JSON，绝不允许退回 Spring Security 默认 HTML 错误页。
 * 错误码优先取 JwtAuthenticationFilter 写入的请求属性。
 * 使用 Spring 管理的 ObjectMapper（已注册 JavaTimeModule）。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String code = (String) request.getAttribute(JwtAuthenticationFilter.ATTR_AUTH_ERROR_CODE);
        String message;
        if (code == null) {
            code = ErrorCode.AUTH_UNAUTHORIZED.getCode();
            message = ErrorCode.AUTH_UNAUTHORIZED.getDefaultMessage();
        } else if (ErrorCode.AUTH_TOKEN_EXPIRED.getCode().equals(code)) {
            message = ErrorCode.AUTH_TOKEN_EXPIRED.getDefaultMessage();
        } else if (ErrorCode.AUTH_SESSION_REVOKED.getCode().equals(code)) {
            message = ErrorCode.AUTH_SESSION_REVOKED.getDefaultMessage();
        } else {
            message = ErrorCode.AUTH_TOKEN_INVALID.getDefaultMessage();
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.error(code, message);
        body.setTraceId(MDC.get("traceId"));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
