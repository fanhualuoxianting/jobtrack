package com.fanhua.jobtrack.module.reminder.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanhua.jobtrack.module.reminder.entity.ReminderRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReminderMapper {

    @Insert("INSERT IGNORE INTO jt_reminder "
            + "(user_id,interview_id,application_id,reminder_type,scheduled_at,biz_type,biz_id,title,content,remind_at,channel,status,idempotency_key,max_retries) "
            + "VALUES (#{userId},#{interviewId},#{applicationId},#{reminderType},#{scheduledAt},'INTERVIEW',#{interviewId},#{title},#{content},#{scheduledAt},'IN_APP','PENDING',#{idempotencyKey},3)")
    int insertIgnore(@Param("userId") Long userId, @Param("interviewId") Long interviewId,
                     @Param("applicationId") Long applicationId, @Param("reminderType") String reminderType,
                     @Param("scheduledAt") LocalDateTime scheduledAt, @Param("title") String title,
                     @Param("content") String content, @Param("idempotencyKey") String idempotencyKey);

    @Update("UPDATE jt_reminder SET status='CANCELLED', cancelled_at=UTC_TIMESTAMP(), version=version+1, updated_at=UTC_TIMESTAMP() "
            + "WHERE user_id=#{userId} AND interview_id=#{interviewId} AND status IN ('PENDING','READY') AND deleted=0")
    int cancelPending(@Param("userId") Long userId, @Param("interviewId") Long interviewId);

    @Select("SELECT id FROM jt_reminder WHERE status='PENDING' AND scheduled_at <= UTC_TIMESTAMP() AND deleted=0 "
            + "ORDER BY scheduled_at ASC, id ASC LIMIT #{limit}")
    List<Long> selectDueIds(@Param("limit") int limit);

    @Update("UPDATE jt_reminder SET status='READY', updated_at=UTC_TIMESTAMP() WHERE id=#{id} AND status='PENDING' AND deleted=0")
    int claim(@Param("id") Long id);

    @Select("SELECT * FROM jt_reminder WHERE id=#{id} AND status='READY' AND deleted=0")
    ReminderRecord selectReady(@Param("id") Long id);

    @Update("UPDATE jt_reminder SET status='SENT', sent_at=UTC_TIMESTAMP(), updated_at=UTC_TIMESTAMP() "
            + "WHERE id=#{id} AND status='READY' AND deleted=0")
    int markSent(@Param("id") Long id);

    @Update("UPDATE jt_reminder SET status=CASE WHEN retry_count + 1 >= max_retries THEN 'FAILED' ELSE 'PENDING' END, "
            + "retry_count=retry_count+1, last_error=#{error}, fail_reason=#{error}, "
            + "scheduled_at=CASE WHEN retry_count + 1 >= max_retries THEN scheduled_at ELSE UTC_TIMESTAMP() + INTERVAL (retry_count + 1) MINUTE END, "
            + "updated_at=UTC_TIMESTAMP() WHERE id=#{id} AND status='READY' AND deleted=0")
    int markFailed(@Param("id") Long id, @Param("error") String error);

    @Select("SELECT * FROM jt_reminder WHERE user_id=#{userId} AND id=#{id} AND deleted=0")
    ReminderRecord selectOwned(@Param("userId") Long userId, @Param("id") Long id);

    @Select("""
            <script>
            SELECT * FROM jt_reminder
            WHERE user_id=#{userId} AND deleted=0 AND status='SENT'
            <if test='readState == "UNREAD"'> AND read_at IS NULL </if>
            <if test='readState == "READ"'> AND read_at IS NOT NULL </if>
            ORDER BY created_at DESC, id DESC
            </script>
            """)
    Page<ReminderRecord> selectNotifications(Page<ReminderRecord> page, @Param("userId") Long userId,
                                              @Param("readState") String readState);

    @Select("SELECT COUNT(*) FROM jt_reminder WHERE user_id=#{userId} AND status='SENT' AND read_at IS NULL AND deleted=0")
    long countUnread(@Param("userId") Long userId);

    @Update("UPDATE jt_reminder SET read_at=COALESCE(read_at,UTC_TIMESTAMP()), updated_at=UTC_TIMESTAMP() "
            + "WHERE id=#{id} AND user_id=#{userId} AND status='SENT' AND deleted=0")
    int markRead(@Param("userId") Long userId, @Param("id") Long id);

    @Update("UPDATE jt_reminder SET read_at=COALESCE(read_at,UTC_TIMESTAMP()), updated_at=UTC_TIMESTAMP() "
            + "WHERE user_id=#{userId} AND status='SENT' AND read_at IS NULL AND deleted=0")
    int markAllRead(@Param("userId") Long userId);
}
