package com.fanhua.jobtrack.module.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公司分页查询参数
 */
@Data
@Schema(description = "公司查询参数")
public class CompanyQueryRequest {

    @Schema(description = "关键词：匹配名称与简称")
    @Size(max = 100)
    private String keyword;

    @Schema(description = "城市筛选")
    @Size(max = 80)
    private String city;

    @Schema(description = "行业筛选")
    @Size(max = 80)
    private String industry;

    @Schema(description = "页码", defaultValue = "1")
    private long page = 1;

    @Schema(description = "每页大小（最大 100）", defaultValue = "20")
    private long pageSize = 20;

    @Schema(description = "排序字段白名单：updatedAt/createdAt/name", defaultValue = "updatedAt")
    @Pattern(regexp = "^(updatedAt|createdAt|name)$", message = "不支持的排序字段")
    private String sortBy = "updatedAt";

    @Schema(description = "排序方向 asc/desc", defaultValue = "desc")
    @Pattern(regexp = "^(asc|desc)$", message = "排序方向只支持 asc/desc")
    private String sortOrder = "desc";

    /** pageSize 上限保护 */
    public long safePageSize() {
        if (pageSize < 1) return 20;
        return Math.min(pageSize, 100);
    }

    public long safePage() {
        return Math.max(page, 1);
    }
}
