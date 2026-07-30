package com.fanhua.jobtrack.module.interview.service;

import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.module.interview.dto.InterviewCancelRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewCompleteRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewCreateRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewQueryRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewUpdateRequest;
import com.fanhua.jobtrack.module.interview.vo.InterviewVO;

import java.util.List;

public interface InterviewService {
    InterviewVO create(Long userId, InterviewCreateRequest request);
    PageResult<InterviewVO> page(Long userId, InterviewQueryRequest request);
    InterviewVO detail(Long userId, Long id);
    List<InterviewVO> listByApplication(Long userId, Long applicationId);
    InterviewVO update(Long userId, Long id, InterviewUpdateRequest request);
    InterviewVO cancel(Long userId, Long id, InterviewCancelRequest request);
    InterviewVO complete(Long userId, Long id, InterviewCompleteRequest request);
    void delete(Long userId, Long id, Integer version);
}
