package com.fanhua.jobtrack.module.position.vo;

import com.fanhua.jobtrack.common.util.DateTimeUtils;
import com.fanhua.jobtrack.module.position.entity.Position;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 岗位详情 VO
 */
@Data
@Schema(description = "岗位详情")
public class PositionDetailVO {

    private Long id;
    private Long companyId;
    private String companyName;
    private String title;
    private String department;
    private String city;
    private String workType;
    private String workplaceType;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryUnit;
    private String currency;
    private String source;
    private String sourceUrl;
    private String description;
    private String requirements;
    private String status;
    private OffsetDateTime publishedAt;
    private OffsetDateTime deadlineAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer version;

    public static PositionDetailVO from(Position entity, String companyName) {
        PositionDetailVO vo = new PositionDetailVO();
        vo.setId(entity.getId());
        vo.setCompanyId(entity.getCompanyId());
        vo.setCompanyName(companyName);
        vo.setTitle(entity.getTitle());
        vo.setDepartment(entity.getDepartment());
        vo.setCity(entity.getCity());
        vo.setWorkType(entity.getWorkType());
        vo.setWorkplaceType(entity.getWorkplaceType());
        vo.setSalaryMin(entity.getSalaryMin());
        vo.setSalaryMax(entity.getSalaryMax());
        vo.setSalaryUnit(entity.getSalaryUnit());
        vo.setCurrency(entity.getCurrency());
        vo.setSource(entity.getSource());
        vo.setSourceUrl(entity.getSourceUrl());
        vo.setDescription(entity.getDescription());
        vo.setRequirements(entity.getRequirements());
        vo.setStatus(entity.getStatus());
        vo.setPublishedAt(DateTimeUtils.toOffset(entity.getPublishedAt()));
        vo.setDeadlineAt(DateTimeUtils.toOffset(entity.getDeadlineAt()));
        vo.setCreatedAt(DateTimeUtils.toOffset(entity.getCreatedAt()));
        vo.setUpdatedAt(DateTimeUtils.toOffset(entity.getUpdatedAt()));
        vo.setVersion(entity.getVersion());
        return vo;
    }
}
