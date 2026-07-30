package com.fanhua.jobtrack.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanhua.jobtrack.module.application.entity.ApplicationIdempotencyRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApplicationIdempotencyMapper extends BaseMapper<ApplicationIdempotencyRecord> {
    @Select("SELECT * FROM jt_application_idempotency WHERE user_id = #{userId} AND application_id = #{applicationId} "
            + "AND idempotency_key = #{key} LIMIT 1")
    ApplicationIdempotencyRecord selectByBusinessKey(@Param("userId") Long userId,
                                                     @Param("applicationId") Long applicationId,
                                                     @Param("key") String key);

    @Insert("INSERT IGNORE INTO jt_application_idempotency "
            + "(user_id, application_id, idempotency_key, request_hash, target_status, reason, result_status) "
            + "VALUES (#{userId}, #{applicationId}, #{idempotencyKey}, #{requestHash}, #{targetStatus}, #{reason}, 'PROCESSING')")
    int insertIgnore(@Param("userId") Long userId, @Param("applicationId") Long applicationId,
                     @Param("idempotencyKey") String idempotencyKey, @Param("requestHash") String requestHash,
                     @Param("targetStatus") String targetStatus, @Param("reason") String reason);

    @Update("UPDATE jt_application_idempotency SET result_status = 'SUCCEEDED', result_version = #{version}, "
            + "result_to_status = #{toStatus}, updated_at = NOW() WHERE id = #{id}")
    int markSucceeded(@Param("id") Long id, @Param("version") Integer version, @Param("toStatus") String toStatus);
}
