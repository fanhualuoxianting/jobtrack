package com.fanhua.jobtrack.module.position.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 岗位组合筛选查询参数
 */
@Data
@Schema(description = "岗位查询参数")
public class PositionQueryRequest {

    @Schema(description = "关键词：匹配岗位名、部门、岗位要求")
    @Size(max = 100)
    private String keyword;

    @Schema(description = "公司ID筛选")
    private Long companyId;

    @Schema(description = "城市筛选")
    @Size(max = 80)
    private String city;

    @Schema(description = "工作类型筛选")
    @Pattern(regexp = "^$|^(INTERNSHIP|FULL_TIME|PART_TIME)$")
    private String workType;

    @Schema(description = "办公方式筛选")
    @Pattern(regexp = "^$|^(ONSITE|REMOTE|HYBRID)$")
    private String workplaceType;

    @Schema(description = "状态筛选 OPEN/CLOSED")
    @Pattern(regexp = "^$|^(OPEN|CLOSED)$")
    private String status;

    @Schema(description = "来源筛选")
    @Size(max = 50)
    private String source;

    @Schema(description = "薪资下限（过滤薪资上限低于该值的岗位）")
    @DecimalMin(value = "0", message = "薪资不能为负数")
    private BigDecimal salaryMin;

    @Schema(description = "薪资上限（过滤薪资下限高于该值的岗位）")
    @DecimalMin(value = "0", message = "薪资不能为负数")
    private BigDecimal salaryMax;

    @Schema(description = "截止时间范围-起")
    private OffsetDateTime deadlineFrom;

    @Schema(description = "截止时间范围-止")
    private OffsetDateTime deadlineTo;

    private long page = 1;
    private long pageSize = 20;

    @Schema(description = "排序白名单：updatedAt/createdAt/deadlineAt/salaryMax")
    @Pattern(regexp = "^(updatedAt|createdAt|deadlineAt|salaryMax)$", message = "不支持的排序字段")
    private String sortBy = "updatedAt";

    @Pattern(regexp = "^(asc|desc)$", message = "排序方向只支持 asc/desc")
    private String sortOrder = "desc";

    public long safePageSize() {
        if (pageSize < 1) return 20;
        return Math.min(pageSize, 100);
    }

    public long safePage() {
        return Math.max(page, 1);
    }
}
