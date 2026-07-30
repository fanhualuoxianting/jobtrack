package com.fanhua.jobtrack.module.company.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanhua.jobtrack.module.company.entity.Company;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CompanyMapper extends BaseMapper<Company> {

    /**
     * 批量统计各公司下未删除岗位数（避免列表页 N+1 查询）。
     * 返回键：company_id / cnt
     */
    @Select("""
            <script>
            SELECT company_id, COUNT(*) AS cnt
            FROM jt_position
            WHERE user_id = #{userId} AND deleted = 0 AND company_id IN
            <foreach collection='companyIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach>
            GROUP BY company_id
            </script>
            """)
    List<Map<String, Object>> countPositionsByCompanies(@Param("userId") Long userId,
                                                        @Param("companyIds") List<Long> companyIds);

    /** 批量统计各公司下未删除投递数 */
    @Select("""
            <script>
            SELECT company_id, COUNT(*) AS cnt
            FROM jt_job_application
            WHERE user_id = #{userId} AND deleted = 0 AND company_id IN
            <foreach collection='companyIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach>
            GROUP BY company_id
            </script>
            """)
    List<Map<String, Object>> countApplicationsByCompanies(@Param("userId") Long userId,
                                                           @Param("companyIds") List<Long> companyIds);

    /** 删除前关联检查：该公司是否存在未删除岗位 */
    @Select("SELECT COUNT(*) FROM jt_position WHERE user_id = #{userId} AND company_id = #{companyId} AND deleted = 0")
    long countPositionsOfCompany(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /** 删除前关联检查：该公司是否存在未删除投递 */
    @Select("SELECT COUNT(*) FROM jt_job_application WHERE user_id = #{userId} AND company_id = #{companyId} AND deleted = 0")
    long countApplicationsOfCompany(@Param("userId") Long userId, @Param("companyId") Long companyId);
}
