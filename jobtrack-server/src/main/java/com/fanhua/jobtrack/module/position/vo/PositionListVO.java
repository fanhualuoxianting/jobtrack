package com.fanhua.jobtrack.module.position.vo;

import com.fanhua.jobtrack.common.util.DateTimeUtils;
import com.fanhua.jobtrack.module.position.entity.Position;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 岗位列表项 VO（附带公司名，避免前端 N+1）
 */
@Data
@Schema(description = "岗位列表项")
public class PositionListVO {

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
    private String status;
    private OffsetDateTime deadlineAt;
    private OffsetDateTime updatedAt;

    public static PositionListVO from(Position entity, String companyName) {
        PositionListVO vo = new PositionListVO();
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
        vo.setStatus(entity.getStatus());
        vo.setDeadlineAt(DateTimeUtils.toOffset(entity.getDeadlineAt()));
        vo.setUpdatedAt(DateTimeUtils.toOffset(entity.getUpdatedAt()));
        return vo;
    }
}
