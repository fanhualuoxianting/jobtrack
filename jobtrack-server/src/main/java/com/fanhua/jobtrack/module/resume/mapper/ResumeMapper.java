package com.fanhua.jobtrack.module.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanhua.jobtrack.module.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

    /** 被投递记录引用的简历数量（删除保护） */
    @Select("SELECT COUNT(*) FROM jt_job_application WHERE user_id = #{userId} AND resume_id = #{resumeId} AND deleted = 0")
    long countApplicationsOfResume(@Param("userId") Long userId, @Param("resumeId") Long resumeId);

    /**
     * 设置默认简历前锁定该用户全部未删除简历行（串行化并发设置请求）。
     * 简历属低频小集合，行锁代价可忽略，换取并发下默认唯一性。
     */
    @Select("SELECT id FROM jt_resume WHERE user_id = #{userId} AND deleted = 0 FOR UPDATE")
    java.util.List<Long> lockUserResumeIdsForUpdate(@Param("userId") Long userId);

    /** 清除用户原默认简历 */
    @Update("UPDATE jt_resume SET is_default = 0, updated_at = NOW() WHERE user_id = #{userId} AND is_default = 1 AND deleted = 0")
    int clearDefault(@Param("userId") Long userId);

    /** 设置目标简历为默认（SQL 层带 user_id 归属条件） */
    @Update("UPDATE jt_resume SET is_default = 1, updated_at = NOW() WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    int setDefault(@Param("userId") Long userId, @Param("id") Long id);
}
