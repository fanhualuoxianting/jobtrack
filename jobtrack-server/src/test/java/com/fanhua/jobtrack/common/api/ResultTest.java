package com.fanhua.jobtrack.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一返回结构测试
 */
class ResultTest {

    @Test
    @DisplayName("success() 返回正确结构")
    void testSuccess() {
        Result<String> result = Result.success("hello");

        assertEquals("SUCCESS", result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals("hello", result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("success(message, data) 自定义消息")
    void testSuccessWithMessage() {
        Result<Integer> result = Result.success("查询成功", 42);

        assertEquals("SUCCESS", result.getCode());
        assertEquals("查询成功", result.getMessage());
        assertEquals(42, result.getData());
    }

    @Test
    @DisplayName("error() 返回错误码和消息")
    void testError() {
        Result<Void> result = Result.error("USER_EMAIL_EXISTS", "邮箱已存在");

        assertEquals("USER_EMAIL_EXISTS", result.getCode());
        assertEquals("邮箱已存在", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("traceId 可链式设置")
    void testTraceId() {
        Result<Void> result = Result.<Void>error("INTERNAL_ERROR", "错误").traceId("abc123");

        assertEquals("abc123", result.getTraceId());
    }

    @Test
    @DisplayName("PageResult 分页计算正确")
    void testPageResult() {
        List<String> records = List.of("a", "b", "c");
        PageResult<String> page = PageResult.of(records, 1, 20, 125);

        assertEquals(3, page.getRecords().size());
        assertEquals(1, page.getPage());
        assertEquals(20, page.getPageSize());
        assertEquals(125, page.getTotal());
        assertEquals(7, page.getTotalPages());
    }

    @Test
    @DisplayName("PageResult 整除时 totalPages 正确")
    void testPageResultExactDivision() {
        PageResult<String> page = PageResult.of(List.of(), 1, 10, 100);
        assertEquals(10, page.getTotalPages());
    }

    @Test
    @DisplayName("PageResult 空数据")
    void testPageResultEmpty() {
        PageResult<String> page = PageResult.of(List.of(), 1, 20, 0);
        assertEquals(0, page.getTotal());
        assertEquals(0, page.getTotalPages());
    }
}
