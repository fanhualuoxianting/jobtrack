package com.fanhua.jobtrack.module.company.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公司下拉选项 VO
 */
@Data
@Schema(description = "公司选项")
public class CompanyOptionVO {

    @Schema(description = "公司ID")
    private Long id;

    @Schema(description = "公司名称")
    private String name;
}
