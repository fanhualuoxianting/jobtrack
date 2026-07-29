package com.fanhua.jobtrack.common.api;

import lombok.Data;

import java.util.List;

/**
 * 分页响应结构
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long page;
    private long pageSize;
    private long total;
    private long totalPages;

    public static <T> PageResult<T> of(List<T> records, long page, long pageSize, long total) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setTotalPages(pageSize > 0 ? (total + pageSize - 1) / pageSize : 0);
        return result;
    }
}
