package com.fanhua.jobtrack.module.position.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 岗位实体：必须关联一个属于当前用户的公司
 */
@Data
@TableName("jt_position")
public class Position {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long companyId;

    private String title;

    private String department;

    private String city;

    /** INTERNSHIP / FULL_TIME / PART_TIME */
    private String workType;

    /** ONSITE / REMOTE / HYBRID */
    private String workplaceType;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    /** DAY / MONTH / YEAR */
    private String salaryUnit;

    private String currency;

    private String source;

    private String sourceUrl;

    private String description;

    private String requirements;

    /** OPEN / CLOSED */
    private String status;

    private LocalDateTime publishedAt;

    private LocalDateTime deadlineAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
