package com.fanhua.jobtrack.module.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanhua.jobtrack.module.interview.entity.Interview;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface InterviewMapper extends BaseMapper<Interview> {

    @Select("SELECT * FROM jt_interview WHERE id=#{id} AND user_id=#{userId} AND deleted=0")
    Interview selectOwned(@Param("userId") Long userId, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM jt_interview WHERE user_id=#{userId} AND application_id=#{applicationId} "
            + "AND round_no=#{roundNo} AND deleted=0")
    long countByRound(@Param("userId") Long userId, @Param("applicationId") Long applicationId,
                      @Param("roundNo") Integer roundNo);

    @Select("SELECT COUNT(*) FROM jt_interview WHERE user_id=#{userId} AND application_id=#{applicationId} "
            + "AND deleted=0")
    long countByApplication(@Param("userId") Long userId, @Param("applicationId") Long applicationId);

    @Select("SELECT * FROM jt_interview WHERE user_id=#{userId} AND application_id=#{applicationId} AND deleted=0 "
            + "ORDER BY round_no ASC, id ASC")
    java.util.List<Interview> selectByApplication(@Param("userId") Long userId,
                                                   @Param("applicationId") Long applicationId);

    @Select("""
            <script>
            SELECT i.*
            FROM jt_interview i
            JOIN jt_job_application a ON a.id=i.application_id AND a.user_id=i.user_id AND a.deleted=0
            WHERE i.user_id=#{userId} AND i.deleted=0
            <if test='status != null'> AND i.status=#{status} </if>
            <if test='companyId != null'> AND a.company_id=#{companyId} </if>
            <if test='applicationId != null'> AND i.application_id=#{applicationId} </if>
            <if test='from != null'> AND i.scheduled_start &gt;= #{from} </if>
            <if test='to != null'> AND i.scheduled_start &lt;= #{to} </if>
            ORDER BY i.scheduled_start ASC, i.id ASC
            </script>
            """)
    Page<Interview> selectPage(Page<Interview> page, @Param("userId") Long userId,
                               @Param("status") String status, @Param("companyId") Long companyId,
                               @Param("applicationId") Long applicationId,
                               @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Update("""
            <script>
            UPDATE jt_interview
            <set>
              <if test='interviewType != null'>interview_type=#{interviewType},</if>
              <if test='scheduledStart != null'>scheduled_start=#{scheduledStart},</if>
              <if test='scheduledEnd != null'>scheduled_end=#{scheduledEnd},</if>
              <if test='timezone != null'>timezone=#{timezone},</if>
              <if test='location != null'>location=#{location},</if>
              <if test='meetingUrl != null'>meeting_url=#{meetingUrl},</if>
              <if test='interviewer != null'>interviewer=#{interviewer},</if>
              <if test='contactInfo != null'>contact_info=#{contactInfo},</if>
              <if test='note != null'>note=#{note},</if>
              version=version+1, updated_at=UTC_TIMESTAMP()
            </set>
            WHERE id=#{id} AND user_id=#{userId} AND version=#{version} AND deleted=0 AND status='SCHEDULED'
            </script>
            """)
    int updateEditable(@Param("userId") Long userId, @Param("id") Long id, @Param("version") Integer version,
                       @Param("interviewType") String interviewType, @Param("scheduledStart") LocalDateTime scheduledStart,
                       @Param("scheduledEnd") LocalDateTime scheduledEnd, @Param("timezone") String timezone,
                       @Param("location") String location, @Param("meetingUrl") String meetingUrl,
                       @Param("interviewer") String interviewer, @Param("contactInfo") String contactInfo,
                       @Param("note") String note);

    @Update("UPDATE jt_interview SET status='CANCELLED', cancel_reason=#{reason}, version=version+1, updated_at=UTC_TIMESTAMP() "
            + "WHERE id=#{id} AND user_id=#{userId} AND version=#{version} AND deleted=0 AND status='SCHEDULED'")
    int cancel(@Param("userId") Long userId, @Param("id") Long id, @Param("version") Integer version,
               @Param("reason") String reason);

    @Update("UPDATE jt_interview SET status='COMPLETED', result=#{result}, feedback=#{feedback}, completed_at=UTC_TIMESTAMP(), "
            + "version=version+1, updated_at=UTC_TIMESTAMP() WHERE id=#{id} AND user_id=#{userId} AND version=#{version} "
            + "AND deleted=0 AND status='SCHEDULED'")
    int complete(@Param("userId") Long userId, @Param("id") Long id, @Param("version") Integer version,
                 @Param("result") String result, @Param("feedback") String feedback);

    @Update("UPDATE jt_interview SET deleted=1, version=version+1, updated_at=UTC_TIMESTAMP() "
            + "WHERE id=#{id} AND user_id=#{userId} AND version=#{version} AND deleted=0")
    int softDelete(@Param("userId") Long userId, @Param("id") Long id, @Param("version") Integer version);
}
