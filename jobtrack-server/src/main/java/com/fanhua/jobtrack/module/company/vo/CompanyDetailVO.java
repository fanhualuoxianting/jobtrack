package com.fanhua.jobtrack.module.company.vo;

import com.fanhua.jobtrack.common.util.DateTimeUtils;
import com.fanhua.jobtrack.module.company.entity.Company;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 公司详情 VO
 */
@Data
@Schema(description = "公司详情")
public class CompanyDetailVO {

    private Long id;
    private String name;
    private String shortName;
    private String industry;
    private String scale;
    private String city;
    private String website;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer version;

    public static CompanyDetailVO from(Company entity) {
        CompanyDetailVO vo = new CompanyDetailVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setShortName(entity.getShortName());
        vo.setIndustry(entity.getIndustry());
        vo.setScale(entity.getScale());
        vo.setCity(entity.getCity());
        vo.setWebsite(entity.getWebsite());
        vo.setDescription(entity.getDescription());
        vo.setCreatedAt(DateTimeUtils.toOffset(entity.getCreatedAt()));
        vo.setUpdatedAt(DateTimeUtils.toOffset(entity.getUpdatedAt()));
        vo.setVersion(entity.getVersion());
        return vo;
    }
}
