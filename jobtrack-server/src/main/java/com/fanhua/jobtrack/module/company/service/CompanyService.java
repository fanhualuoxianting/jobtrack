package com.fanhua.jobtrack.module.company.service;

import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.module.company.dto.CompanyQueryRequest;
import com.fanhua.jobtrack.module.company.dto.CompanyUpsertRequest;
import com.fanhua.jobtrack.module.company.vo.CompanyDetailVO;
import com.fanhua.jobtrack.module.company.vo.CompanyListVO;
import com.fanhua.jobtrack.module.company.vo.CompanyOptionVO;

import java.util.List;

/**
 * 公司服务：所有操作都在 SQL 层绑定当前用户 ID，防止水平越权
 */
public interface CompanyService {

    /** 分页查询当前用户公司（关键词/城市/行业组合筛选） */
    PageResult<CompanyListVO> pageCompanies(Long userId, CompanyQueryRequest query);

    /** 公司详情；不存在或不属于当前用户统一 404 */
    CompanyDetailVO getDetail(Long userId, Long id);

    /** 新增公司；规范化名称重复返回 409 */
    CompanyDetailVO create(Long userId, CompanyUpsertRequest request);

    /** 修改公司；并发修改依赖乐观锁，冲突返回 409 */
    CompanyDetailVO update(Long userId, Long id, CompanyUpsertRequest request);

    /** 删除公司；存在未删除岗位或投递记录时拒绝 */
    void delete(Long userId, Long id);

    /** 当前用户全部公司下拉选项 */
    List<CompanyOptionVO> listOptions(Long userId);
}
