package com.fanhua.jobtrack.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanhua.jobtrack.common.api.Result;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 已认证但权限不足时的 403 JSON 响应
 * 使用 Spring 管理的 ObjectMapper（已注册 JavaTimeModule）
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.error(
                ErrorCode.RESOURCE_FORBIDDEN.getCode(),
                ErrorCode.RESOURCE_FORBIDDEN.getDefaultMessage());
        body.setTraceId(MDC.get("traceId"));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
