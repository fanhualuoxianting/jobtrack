package com.fanhua.jobtrack.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanhua.jobtrack.module.application.entity.JobApplication;
import com.fanhua.jobtrack.module.application.vo.ApplicationListRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface JobApplicationMapper extends BaseMapper<JobApplication> {

    @Select("SELECT * FROM jt_job_application WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    JobApplication selectOwned(@Param("userId") Long userId, @Param("id") Long id);

    @Select("SELECT * FROM jt_job_application WHERE user_id = #{userId} AND position_id = #{positionId} "
            + "AND deleted = 0 AND archived = 0 "
            + "AND status NOT IN ('ACCEPTED','REJECTED','WITHDRAWN','CLOSED') LIMIT 1")
    JobApplication selectActiveByPosition(@Param("userId") Long userId, @Param("positionId") Long positionId);

    @Select("""
            <script>
            SELECT a.id, a.company_id, c.name AS company_name, a.position_id, p.title AS position_title,
                   a.resume_id, r.version_name AS resume_version_name, a.status, a.priority, a.source,
                   a.applied_at, a.referral_name, a.next_action, a.next_action_at, a.note, a.archived,
                   a.created_at, a.updated_at, a.version
            FROM jt_job_application a
            JOIN jt_company c ON c.id = a.company_id AND c.user_id = a.user_id AND c.deleted = 0
            JOIN jt_position p ON p.id = a.position_id AND p.user_id = a.user_id AND p.deleted = 0
            LEFT JOIN jt_resume r ON r.id = a.resume_id AND r.user_id = a.user_id
            WHERE a.user_id = #{userId} AND a.deleted = 0
              AND a.archived = #{archived}
              <if test='companyId != null'> AND a.company_id = #{companyId} </if>
              <if test='positionId != null'> AND a.position_id = #{positionId} </if>
              <if test='status != null and status != ""'> AND a.status = #{status} </if>
              <if test='source != null and source != ""'> AND a.source = #{source} </if>
              <if test='appliedFrom != null'> AND a.applied_at &gt;= #{appliedFrom} </if>
              <if test='appliedTo != null'> AND a.applied_at &lt;= #{appliedTo} </if>
              <if test='keyword != null and keyword != ""'>
                AND (c.name LIKE CONCAT('%', #{keyword}, '%')
                     OR p.title LIKE CONCAT('%', #{keyword}, '%')
                     OR a.note LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test='interviewFrom != null'>
                AND EXISTS (SELECT 1 FROM jt_interview i WHERE i.application_id = a.id
                    AND i.user_id = #{userId} AND i.deleted = 0 AND i.scheduled_start &gt;= #{interviewFrom})
              </if>
              <if test='interviewTo != null'>
                AND EXISTS (SELECT 1 FROM jt_interview i WHERE i.application_id = a.id
                    AND i.user_id = #{userId} AND i.deleted = 0 AND i.scheduled_start &lt;= #{interviewTo})
              </if>
            ORDER BY ${sortColumn} ${sortDirection}, a.id DESC
            </script>
            """)
    Page<ApplicationListRow> selectPageRows(Page<ApplicationListRow> page,
                                             @Param("userId") Long userId,
                                             @Param("companyId") Long companyId,
                                             @Param("positionId") Long positionId,
                                             @Param("status") String status,
                                             @Param("source") String source,
                                             @Param("archived") Integer archived,
                                             @Param("keyword") String keyword,
                                             @Param("appliedFrom") LocalDateTime appliedFrom,
                                             @Param("appliedTo") LocalDateTime appliedTo,
                                             @Param("interviewFrom") LocalDateTime interviewFrom,
                                             @Param("interviewTo") LocalDateTime interviewTo,
                                             @Param("sortColumn") String sortColumn,
                                             @Param("sortDirection") String sortDirection);

    @Update("UPDATE jt_job_application SET status = #{status}, version = version + 1, updated_at = NOW() "
            + "WHERE id = #{id} AND user_id = #{userId} AND version = #{expectedVersion} AND deleted = 0")
    int updateStatusWithVersion(@Param("userId") Long userId, @Param("id") Long id,
                                @Param("status") String status, @Param("expectedVersion") Integer expectedVersion);

    @Update("""
            <script>
            UPDATE jt_job_application
            <set>
              <if test='resumeId != null'>resume_id = #{resumeId},</if>
              <if test='source != null'>source = #{source},</if>
              <if test='appliedAt != null'>applied_at = #{appliedAt},</if>
              <if test='referralName != null'>referral_name = #{referralName},</if>
              <if test='expectedSalaryMin != null'>expected_salary_min = #{expectedSalaryMin},</if>
              <if test='expectedSalaryMax != null'>expected_salary_max = #{expectedSalaryMax},</if>
              <if test='currency != null'>currency = #{currency},</if>
              <if test='nextAction != null'>next_action = #{nextAction},</if>
              <if test='nextActionAt != null'>next_action_at = #{nextActionAt},</if>
              <if test='note != null'>note = #{note},</if>
              version = version + 1,
              updated_at = NOW()
            </set>
            WHERE id = #{id} AND user_id = #{userId} AND version = #{version} AND deleted = 0
            </script>
            """)
    int updateEditable(@Param("userId") Long userId, @Param("id") Long id, @Param("version") Integer version,
                       @Param("resumeId") Long resumeId, @Param("source") String source,
                       @Param("appliedAt") LocalDateTime appliedAt,
                       @Param("referralName") String referralName,
                       @Param("expectedSalaryMin") java.math.BigDecimal expectedSalaryMin,
                       @Param("expectedSalaryMax") java.math.BigDecimal expectedSalaryMax,
                       @Param("currency") String currency, @Param("nextAction") String nextAction,
                       @Param("nextActionAt") LocalDateTime nextActionAt, @Param("note") String note);

    @Update("UPDATE jt_job_application SET archived = #{archived}, version = version + 1, updated_at = NOW() "
            + "WHERE id = #{id} AND user_id = #{userId} AND version = #{version} AND deleted = 0")
    int updateArchived(@Param("userId") Long userId, @Param("id") Long id,
                       @Param("version") Integer version, @Param("archived") Integer archived);

    @Update("UPDATE jt_job_application SET deleted = 1, version = version + 1, updated_at = NOW() "
            + "WHERE id = #{id} AND user_id = #{userId} AND version = #{version} "
            + "AND status = 'SAVED' AND deleted = 0")
    int softDeleteSaved(@Param("userId") Long userId, @Param("id") Long id, @Param("version") Integer version);
}
