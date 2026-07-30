package com.fanhua.jobtrack.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，仅映射数据库结构，不作为接口返回值
 */
@Data
@TableName("jt_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String phone;

    private String passwordHash;

    private String nickname;

    private String avatarUrl;

    /** USER / ADMIN */
    private String role;

    /** ACTIVE / DISABLED */
    private String status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
