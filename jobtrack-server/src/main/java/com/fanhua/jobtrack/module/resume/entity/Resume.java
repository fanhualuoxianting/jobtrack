package com.fanhua.jobtrack.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历版本实体。
 * storage_key 是存储根目录下的相对路径，仅服务端使用，不对外返回。
 */
@Data
@TableName("jt_resume")
public class Resume {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String versionName;

    private String originalFileName;

    private String storageKey;

    private String mimeType;

    private Long fileSize;

    private String sha256;

    private Integer isDefault;

    private String note;

    private LocalDateTime uploadedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
