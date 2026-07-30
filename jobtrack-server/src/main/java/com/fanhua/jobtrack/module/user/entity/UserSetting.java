package com.fanhua.jobtrack.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户设置实体
 */
@Data
@TableName("jt_user_setting")
public class UserSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String timezone;

    private Integer defaultReminderMinutes;

    private Integer weekStart;

    private Integer emailNotification;

    private String theme;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
