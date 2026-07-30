package com.fanhua.jobtrack.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录会话：Refresh Token Rotation 的服务端状态。
 * 表中只保存 Refresh Token 的 SHA-256 哈希，绝不保存明文令牌。
 */
@Data
@TableName("jt_auth_session")
public class AuthSession {

    /** 会话 ID（UUID 去掉横线，32 位十六进制） */
    @TableId(type = IdType.INPUT)
    private String id;

    private Long userId;

    private String deviceName;

    private String userAgent;

    private String ipAddress;

    /** 当前有效 Refresh Token 的 SHA-256 哈希（64 位十六进制） */
    private String refreshTokenHash;

    private LocalDateTime createdAt;

    private LocalDateTime lastActiveAt;

    private LocalDateTime expiresAt;

    /** 注销时间；NULL 表示会话有效 */
    private LocalDateTime revokedAt;
}
