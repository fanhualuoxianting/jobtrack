package com.fanhua.jobtrack.module.position.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 岗位下拉选项 VO
 */
@Data
@Schema(description = "岗位选项")
public class PositionOptionVO {

    private Long id;
    private Long companyId;
    private String title;
    private String city;
}
