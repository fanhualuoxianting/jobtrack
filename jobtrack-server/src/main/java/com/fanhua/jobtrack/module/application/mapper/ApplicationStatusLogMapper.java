package com.fanhua.jobtrack.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanhua.jobtrack.module.application.entity.ApplicationStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApplicationStatusLogMapper extends BaseMapper<ApplicationStatusLog> {
    @Select("SELECT * FROM jt_application_status_log WHERE application_id = #{applicationId} "
            + "AND user_id = #{userId} AND deleted = 0 ORDER BY changed_at, id")
    List<ApplicationStatusLog> selectTimeline(@Param("userId") Long userId, @Param("applicationId") Long applicationId);
}
