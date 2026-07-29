package com.fanhua.jobtrack.common.handler;

import com.fanhua.jobtrack.common.exception.BusinessException;
import com.fanhua.jobtrack.common.exception.ConflictException;
import com.fanhua.jobtrack.common.exception.ForbiddenException;
import com.fanhua.jobtrack.common.exception.NotFoundException;
import com.fanhua.jobtrack.common.api.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理器单元测试
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("NotFoundException 返回 404")
    void testNotFound() {
        NotFoundException ex = new NotFoundException("投递记录不存在");
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().getCode());
    }

    @Test
    @DisplayName("ForbiddenException 返回 403")
    void testForbidden() {
        ForbiddenException ex = new ForbiddenException("无权访问");
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("RESOURCE_FORBIDDEN", response.getBody().getCode());
    }

    @Test
    @DisplayName("ConflictException 返回 409")
    void testConflict() {
        ConflictException ex = new ConflictException("记录已被修改");
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("OPTIMISTIC_LOCK_CONFLICT", response.getBody().getCode());
    }

    @Test
    @DisplayName("普通 BusinessException 返回 400")
    void testBusinessException() {
        BusinessException ex = new BusinessException("COMPANY_NAME_EXISTS", "公司名称重复");
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("COMPANY_NAME_EXISTS", response.getBody().getCode());
        assertEquals("公司名称重复", response.getBody().getMessage());
    }
}
