package com.fanhua.jobtrack.module.company.vo;

import com.fanhua.jobtrack.common.util.DateTimeUtils;
import com.fanhua.jobtrack.module.company.entity.Company;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 公司列表项 VO
 */
@Data
@Schema(description = "公司列表项")
public class CompanyListVO {

    @Schema(description = "公司ID")
    private Long id;

    @Schema(description = "公司名称")
    private String name;

    @Schema(description = "简称")
    private String shortName;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "规模")
    private String scale;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "岗位数量")
    private long positionCount;

    @Schema(description = "投递数量")
    private long applicationCount;

    @Schema(description = "最近更新时间")
    private OffsetDateTime updatedAt;

    public static CompanyListVO from(Company entity, long positionCount, long applicationCount) {
        CompanyListVO vo = new CompanyListVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setShortName(entity.getShortName());
        vo.setIndustry(entity.getIndustry());
        vo.setScale(entity.getScale());
        vo.setCity(entity.getCity());
        vo.setPositionCount(positionCount);
        vo.setApplicationCount(applicationCount);
        vo.setUpdatedAt(DateTimeUtils.toOffset(entity.getUpdatedAt()));
        return vo;
    }
}
