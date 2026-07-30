package com.fanhua.jobtrack.module.application.service;

import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.module.application.dto.ApplicationCreateRequest;
import com.fanhua.jobtrack.module.application.dto.ApplicationQueryRequest;
import com.fanhua.jobtrack.module.application.dto.ApplicationTransitionRequest;
import com.fanhua.jobtrack.module.application.dto.ApplicationUpdateRequest;
import com.fanhua.jobtrack.module.application.vo.ApplicationTimelineVO;
import com.fanhua.jobtrack.module.application.vo.ApplicationVO;
import com.fanhua.jobtrack.module.application.domain.ApplicationStatus;

import java.util.List;

public interface ApplicationService {
    ApplicationVO create(Long userId, ApplicationCreateRequest request);
    PageResult<ApplicationVO> page(Long userId, ApplicationQueryRequest request);
    ApplicationVO detail(Long userId, Long id);
    ApplicationVO update(Long userId, Long id, ApplicationUpdateRequest request);
    ApplicationVO transition(Long userId, Long id, ApplicationTransitionRequest request);
    List<ApplicationTimelineVO> timeline(Long userId, Long id);
    List<ApplicationStatus> allowedTargets(Long userId, Long id);
    ApplicationVO archive(Long userId, Long id);
    ApplicationVO restore(Long userId, Long id);
    void delete(Long userId, Long id);
}
