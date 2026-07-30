package com.fanhua.jobtrack.module.position.service;

import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.module.position.dto.PositionQueryRequest;
import com.fanhua.jobtrack.module.position.dto.PositionStatusRequest;
import com.fanhua.jobtrack.module.position.dto.PositionUpsertRequest;
import com.fanhua.jobtrack.module.position.vo.PositionDetailVO;
import com.fanhua.jobtrack.module.position.vo.PositionListVO;
import com.fanhua.jobtrack.module.position.vo.PositionOptionVO;

import java.util.List;

/**
 * 岗位服务：与公司一样全程在 SQL 层绑定当前用户 ID
 */
public interface PositionService {

    PageResult<PositionListVO> pagePositions(Long userId, PositionQueryRequest query);

    PositionDetailVO getDetail(Long userId, Long id);

    PositionDetailVO create(Long userId, PositionUpsertRequest request);

    PositionDetailVO update(Long userId, Long id, PositionUpsertRequest request);

    /** 打开/关闭岗位 */
    PositionDetailVO changeStatus(Long userId, Long id, PositionStatusRequest request);

    /** 删除岗位；存在投递记录时建议改为 CLOSED，直接删除返回 409 */
    void delete(Long userId, Long id);

    /** 岗位下拉选项（可按公司过滤） */
    List<PositionOptionVO> listOptions(Long userId, Long companyId);
}
