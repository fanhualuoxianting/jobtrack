package com.fanhua.jobtrack.module.position.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanhua.jobtrack.module.position.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PositionMapper extends BaseMapper<Position> {

    /** 删除岗位前的关联检查：该岗位是否存在未删除投递 */
    @Select("SELECT COUNT(*) FROM jt_job_application WHERE user_id = #{userId} AND position_id = #{positionId} AND deleted = 0")
    long countApplicationsOfPosition(@Param("userId") Long userId, @Param("positionId") Long positionId);
}
