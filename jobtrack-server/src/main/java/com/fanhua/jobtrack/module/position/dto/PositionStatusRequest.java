package com.fanhua.jobtrack.module.position.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 岗位状态启停请求
 */
@Data
@Schema(description = "岗位状态修改请求")
public class PositionStatusRequest {

    @Schema(description = "目标状态", example = "CLOSED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^(OPEN|CLOSED)$", message = "状态只支持 OPEN/CLOSED")
    private String status;
}
