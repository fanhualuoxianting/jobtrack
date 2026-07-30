package com.fanhua.jobtrack.module.company.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公司实体：属于单个用户的私有数据
 */
@Data
@TableName("jt_company")
public class Company {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String shortName;

    private String industry;

    private String scale;

    private String city;

    private String website;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
