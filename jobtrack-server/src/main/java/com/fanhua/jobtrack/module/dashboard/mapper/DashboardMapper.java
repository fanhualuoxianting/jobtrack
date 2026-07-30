package com.fanhua.jobtrack.module.dashboard.mapper;

import com.fanhua.jobtrack.module.dashboard.vo.DashboardBreakdownItem;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardUpcomingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DashboardMapper {

    @Select("""
            <script>
            SELECT
              COUNT(*) AS total_applications,
              COALESCE(SUM(CASE WHEN a.archived = 0 AND a.status NOT IN ('ACCEPTED','REJECTED','WITHDRAWN','CLOSED') THEN 1 ELSE 0 END), 0) AS in_progress_applications,
              COALESCE(SUM(CASE WHEN a.archived = 0 AND a.next_action_at IS NOT NULL AND a.next_action_at &lt;= #{appNow}
                  AND a.status NOT IN ('ACCEPTED','REJECTED','WITHDRAWN','CLOSED') THEN 1 ELSE 0 END), 0) AS pending_tasks,
              COALESCE(SUM(CASE WHEN a.status = 'OFFERED' THEN 1 ELSE 0 END), 0) AS offer_applications,
              COALESCE(SUM(CASE WHEN a.status = 'ACCEPTED' THEN 1 ELSE 0 END), 0) AS accepted_offers,
              COALESCE(SUM(CASE WHEN EXISTS (SELECT 1 FROM jt_application_status_log l WHERE l.application_id=a.id AND l.user_id=a.user_id AND l.deleted=0
                  AND l.to_status IN ('APPLIED','ASSESSMENT','INTERVIEWING','OFFERED','ACCEPTED','REJECTED','WITHDRAWN','CLOSED')) THEN 1 ELSE 0 END), 0) AS advanced_applications,
              COALESCE(SUM(CASE WHEN EXISTS (SELECT 1 FROM jt_application_status_log l WHERE l.application_id=a.id AND l.user_id=a.user_id AND l.deleted=0
                  AND l.to_status IN ('INTERVIEWING','OFFERED','ACCEPTED')) THEN 1 ELSE 0 END), 0) AS interview_applications,
              COALESCE(SUM(CASE WHEN EXISTS (SELECT 1 FROM jt_application_status_log l WHERE l.application_id=a.id AND l.user_id=a.user_id AND l.deleted=0
                  AND l.to_status IN ('OFFERED','ACCEPTED')) THEN 1 ELSE 0 END), 0) AS offered_applications,
              (SELECT COUNT(*) FROM jt_interview i WHERE i.user_id=#{userId} AND i.deleted=0 AND i.status='SCHEDULED'
                  AND i.scheduled_start BETWEEN #{upcomingFrom} AND #{upcomingTo}) AS upcoming_interviews,
              (SELECT COUNT(*) FROM jt_reminder r WHERE r.user_id=#{userId} AND r.deleted=0
                  AND r.status IN ('PENDING','READY') AND r.scheduled_at &lt;= UTC_TIMESTAMP()) AS overdue_reminders
            FROM jt_job_application a
            WHERE a.user_id=#{userId} AND a.deleted=0
              AND COALESCE(a.applied_at,a.created_at) &gt;= #{appFrom}
              AND COALESCE(a.applied_at,a.created_at) &lt; #{appTo}
            <if test='includeArchived == false'> AND a.archived=0 </if>
            <if test='companyId != null'> AND a.company_id=#{companyId} </if>
            <if test='source != null and source != ""'> AND COALESCE(NULLIF(a.source,''),'UNKNOWN')=#{source} </if>
            </script>
            """)
    DashboardSummaryRow selectSummary(@Param("userId") Long userId,
                                      @Param("appFrom") LocalDateTime appFrom,
                                      @Param("appTo") LocalDateTime appTo,
                                      @Param("appNow") LocalDateTime appNow,
                                      @Param("upcomingFrom") LocalDateTime upcomingFrom,
                                      @Param("upcomingTo") LocalDateTime upcomingTo,
                                      @Param("companyId") Long companyId,
                                      @Param("source") String source,
                                      @Param("includeArchived") boolean includeArchived);

    @Select("""
            <script>
            SELECT
              COALESCE(SUM(CASE WHEN EXISTS (SELECT 1 FROM jt_application_status_log l WHERE l.application_id=a.id AND l.user_id=a.user_id AND l.deleted=0 AND l.to_status IN ('APPLIED','ASSESSMENT','INTERVIEWING','OFFERED','ACCEPTED','REJECTED','WITHDRAWN','CLOSED')) THEN 1 ELSE 0 END),0) AS applied_count,
              COALESCE(SUM(CASE WHEN EXISTS (SELECT 1 FROM jt_application_status_log l WHERE l.application_id=a.id AND l.user_id=a.user_id AND l.deleted=0 AND l.to_status IN ('ASSESSMENT','INTERVIEWING','OFFERED','ACCEPTED','REJECTED','WITHDRAWN','CLOSED')) THEN 1 ELSE 0 END),0) AS assessment_count,
              COALESCE(SUM(CASE WHEN EXISTS (SELECT 1 FROM jt_application_status_log l WHERE l.application_id=a.id AND l.user_id=a.user_id AND l.deleted=0 AND l.to_status IN ('INTERVIEWING','OFFERED','ACCEPTED','REJECTED','WITHDRAWN','CLOSED')) THEN 1 ELSE 0 END),0) AS interview_count,
              COALESCE(SUM(CASE WHEN EXISTS (SELECT 1 FROM jt_application_status_log l WHERE l.application_id=a.id AND l.user_id=a.user_id AND l.deleted=0 AND l.to_status IN ('OFFERED','ACCEPTED')) THEN 1 ELSE 0 END),0) AS offered_count,
              COALESCE(SUM(CASE WHEN EXISTS (SELECT 1 FROM jt_application_status_log l WHERE l.application_id=a.id AND l.user_id=a.user_id AND l.deleted=0 AND l.to_status='ACCEPTED') THEN 1 ELSE 0 END),0) AS accepted_count
            FROM jt_job_application a
            WHERE a.user_id=#{userId} AND a.deleted=0
              AND COALESCE(a.applied_at,a.created_at) &gt;= #{appFrom}
              AND COALESCE(a.applied_at,a.created_at) &lt; #{appTo}
            <if test='includeArchived == false'> AND a.archived=0 </if>
            <if test='companyId != null'> AND a.company_id=#{companyId} </if>
            <if test='source != null and source != ""'> AND COALESCE(NULLIF(a.source,''),'UNKNOWN')=#{source} </if>
            </script>
            """)
    DashboardFunnelRow selectFunnel(@Param("userId") Long userId,
                                    @Param("appFrom") LocalDateTime appFrom,
                                    @Param("appTo") LocalDateTime appTo,
                                    @Param("companyId") Long companyId,
                                    @Param("source") String source,
                                    @Param("includeArchived") boolean includeArchived);

    @Select("""
            <script>
            SELECT ${bucketExpression} AS bucket, COUNT(*) AS count
            FROM jt_job_application a
            WHERE a.user_id=#{userId} AND a.deleted=0
              AND COALESCE(a.applied_at,a.created_at) &gt;= #{appFrom}
              AND COALESCE(a.applied_at,a.created_at) &lt; #{appTo}
            <if test='includeArchived == false'> AND a.archived=0 </if>
            <if test='companyId != null'> AND a.company_id=#{companyId} </if>
            <if test='source != null and source != ""'> AND COALESCE(NULLIF(a.source,''),'UNKNOWN')=#{source} </if>
            GROUP BY ${bucketExpression}
            ORDER BY bucket
            </script>
            """)
    List<DashboardTrendRow> selectTrends(@Param("userId") Long userId,
                                         @Param("appFrom") LocalDateTime appFrom,
                                         @Param("appTo") LocalDateTime appTo,
                                         @Param("timezone") String timezone,
                                         @Param("bucketExpression") String bucketExpression,
                                         @Param("companyId") Long companyId,
                                         @Param("source") String source,
                                         @Param("includeArchived") boolean includeArchived);

    @Select("""
            <script>
            SELECT COALESCE(NULLIF(a.source,''),'UNKNOWN') AS name, COUNT(*) AS count
            FROM jt_job_application a
            WHERE a.user_id=#{userId} AND a.deleted=0
              AND COALESCE(a.applied_at,a.created_at) &gt;= #{appFrom}
              AND COALESCE(a.applied_at,a.created_at) &lt; #{appTo}
            <if test='includeArchived == false'> AND a.archived=0 </if>
            <if test='companyId != null'> AND a.company_id=#{companyId} </if>
            GROUP BY COALESCE(NULLIF(a.source,''),'UNKNOWN') ORDER BY count DESC, name
            </script>
            """)
    List<DashboardBreakdownItem> selectSources(@Param("userId") Long userId,
                                               @Param("appFrom") LocalDateTime appFrom,
                                               @Param("appTo") LocalDateTime appTo,
                                               @Param("companyId") Long companyId,
                                               @Param("includeArchived") boolean includeArchived);

    @Select("""
            <script>
            SELECT COALESCE(NULLIF(c.industry,''),'未分类') AS name, COUNT(*) AS count
            FROM jt_job_application a
            JOIN jt_company c ON c.id=a.company_id AND c.user_id=a.user_id AND c.deleted=0
            WHERE a.user_id=#{userId} AND a.deleted=0
              AND COALESCE(a.applied_at,a.created_at) &gt;= #{appFrom}
              AND COALESCE(a.applied_at,a.created_at) &lt; #{appTo}
            <if test='includeArchived == false'> AND a.archived=0 </if>
            <if test='companyId != null'> AND a.company_id=#{companyId} </if>
            GROUP BY COALESCE(NULLIF(c.industry,''),'未分类') ORDER BY count DESC, name
            </script>
            """)
    List<DashboardBreakdownItem> selectIndustries(@Param("userId") Long userId,
                                                  @Param("appFrom") LocalDateTime appFrom,
                                                  @Param("appTo") LocalDateTime appTo,
                                                  @Param("companyId") Long companyId,
                                                  @Param("includeArchived") boolean includeArchived);

    @Select("""
            <script>
            WITH per_application AS (
              SELECT a.id,
                MIN(CASE WHEN l.to_status='APPLIED' THEN l.changed_at END) AS applied_time,
                MIN(CASE WHEN l.to_status='INTERVIEWING' THEN l.changed_at END) AS interviewing_time,
                MIN(CASE WHEN l.to_status='OFFERED' THEN l.changed_at END) AS offered_time,
                MIN(CASE WHEN l.to_status IN ('ACCEPTED','REJECTED','WITHDRAWN','CLOSED') THEN l.changed_at END) AS terminal_time
              FROM jt_job_application a
              LEFT JOIN jt_application_status_log l ON l.application_id=a.id AND l.user_id=a.user_id AND l.deleted=0
              WHERE a.user_id=#{userId} AND a.deleted=0
                AND COALESCE(a.applied_at,a.created_at) &gt;= #{appFrom}
                AND COALESCE(a.applied_at,a.created_at) &lt; #{appTo}
              <if test='includeArchived == false'> AND a.archived=0 </if>
              <if test='companyId != null'> AND a.company_id=#{companyId} </if>
              <if test='source != null and source != ""'> AND COALESCE(NULLIF(a.source,''),'UNKNOWN')=#{source} </if>
              GROUP BY a.id
            ), metrics AS (
              SELECT 'APPLIED_TO_INTERVIEWING' AS metric_type, TIMESTAMPDIFF(MINUTE, applied_time, interviewing_time)/60.0 AS hours FROM per_application WHERE applied_time IS NOT NULL AND interviewing_time IS NOT NULL
              UNION ALL SELECT 'APPLIED_TO_OFFERED', TIMESTAMPDIFF(MINUTE, applied_time, offered_time)/60.0 FROM per_application WHERE applied_time IS NOT NULL AND offered_time IS NOT NULL
              UNION ALL SELECT 'APPLIED_TO_TERMINAL', TIMESTAMPDIFF(MINUTE, applied_time, terminal_time)/60.0 FROM per_application WHERE applied_time IS NOT NULL AND terminal_time IS NOT NULL
            )
            SELECT metric_type, COUNT(*) AS sample_count, AVG(hours) AS average_hours, MIN(hours) AS min_hours, MAX(hours) AS max_hours
            FROM metrics GROUP BY metric_type
            </script>
            """)
    List<DashboardCycleRow> selectCycleTimes(@Param("userId") Long userId,
                                             @Param("appFrom") LocalDateTime appFrom,
                                             @Param("appTo") LocalDateTime appTo,
                                             @Param("companyId") Long companyId,
                                             @Param("source") String source,
                                             @Param("includeArchived") boolean includeArchived);

    @Select("""
            <script>
            SELECT i.id AS interview_id, i.application_id, c.name AS company_name, p.title AS position_title,
                   COALESCE(i.round_name,i.title) AS round_name, i.scheduled_start AS scheduled_start_at, i.timezone
            FROM jt_interview i
            JOIN jt_job_application a ON a.id=i.application_id AND a.user_id=i.user_id AND a.deleted=0
            JOIN jt_company c ON c.id=a.company_id AND c.user_id=a.user_id AND c.deleted=0
            JOIN jt_position p ON p.id=a.position_id AND p.user_id=a.user_id AND p.deleted=0
            WHERE i.user_id=#{userId} AND i.deleted=0 AND i.status='SCHEDULED'
              AND i.scheduled_start &gt;= #{upcomingFrom} AND i.scheduled_start &lt; #{upcomingTo}
            <if test='includeArchived == false'> AND a.archived=0 </if>
            <if test='companyId != null'> AND a.company_id=#{companyId} </if>
            <if test='source != null and source != ""'> AND COALESCE(NULLIF(a.source,''),'UNKNOWN')=#{source} </if>
            ORDER BY i.scheduled_start, i.id LIMIT 10
            </script>
            """)
    List<DashboardUpcomingVO> selectUpcoming(@Param("userId") Long userId,
                                             @Param("upcomingFrom") LocalDateTime upcomingFrom,
                                             @Param("upcomingTo") LocalDateTime upcomingTo,
                                             @Param("companyId") Long companyId,
                                             @Param("source") String source,
                                             @Param("includeArchived") boolean includeArchived);
}
