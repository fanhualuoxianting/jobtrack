package com.fanhua.jobtrack.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanhua.jobtrack.module.auth.entity.AuthSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface AuthSessionMapper extends BaseMapper<AuthSession> {

    /**
     * Refresh Token Rotation 的原子轮换：
     * 只有"会话仍存在、未撤销、未过期且当前哈希匹配"时才允许写入新哈希。
     * 影响行数为 0 表示并发冲突或旧令牌重放，由调用方区分处理。
     */
    @Update("""
            UPDATE jt_auth_session
            SET refresh_token_hash = #{newHash},
                last_active_at = #{now}
            WHERE id = #{sessionId}
              AND refresh_token_hash = #{oldHash}
              AND revoked_at IS NULL
              AND expires_at > #{now}
            """)
    int rotateRefreshToken(@Param("sessionId") String sessionId,
                           @Param("oldHash") String oldHash,
                           @Param("newHash") String newHash,
                           @Param("now") LocalDateTime now);

    /** 撤销指定会话 */
    @Update("""
            UPDATE jt_auth_session
            SET revoked_at = #{now}
            WHERE id = #{sessionId}
              AND revoked_at IS NULL
            """)
    int revokeById(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    /** 撤销用户全部会话；exceptSessionId 为空时表示注销全部设备 */
    @Update("""
            UPDATE jt_auth_session
            SET revoked_at = #{now}
            WHERE user_id = #{userId}
              AND revoked_at IS NULL
              AND (#{exceptSessionId} IS NULL OR id <> #{exceptSessionId})
            """)
    int revokeByUserId(@Param("userId") Long userId,
                       @Param("exceptSessionId") String exceptSessionId,
                       @Param("now") LocalDateTime now);

    /** 触活时间更新（不校验令牌哈希，用于刷新成功后或活跃打点） */
    @Update("""
            UPDATE jt_auth_session
            SET last_active_at = #{now}
            WHERE id = #{sessionId}
              AND revoked_at IS NULL
            """)
    int touchLastActive(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);
}
