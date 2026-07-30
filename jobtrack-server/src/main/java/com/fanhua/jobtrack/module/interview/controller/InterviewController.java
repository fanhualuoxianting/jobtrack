package com.fanhua.jobtrack.module.interview.controller;

import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.common.api.Result;
import com.fanhua.jobtrack.module.interview.dto.InterviewCancelRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewCompleteRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewCreateRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewQueryRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewUpdateRequest;
import com.fanhua.jobtrack.module.interview.service.InterviewService;
import com.fanhua.jobtrack.module.interview.vo.InterviewVO;
import com.fanhua.jobtrack.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interviews")
public class InterviewController {
    private final InterviewService service;

    public InterviewController(InterviewService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Result<InterviewVO>> create(@Valid @RequestBody InterviewCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success("创建面试成功",
                service.create(SecurityUtils.currentUserId(), request)));
    }

    @GetMapping
    public Result<PageResult<InterviewVO>> page(@Valid @ParameterObject InterviewQueryRequest request) {
        return Result.success("查询成功", service.page(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/{id}")
    public Result<InterviewVO> detail(@PathVariable Long id) {
        return Result.success(service.detail(SecurityUtils.currentUserId(), id));
    }

    @GetMapping("/by-application/{applicationId}")
    public Result<List<InterviewVO>> byApplication(@PathVariable Long applicationId) {
        return Result.success(service.listByApplication(SecurityUtils.currentUserId(), applicationId));
    }

    @PatchMapping("/{id}")
    public Result<InterviewVO> update(@PathVariable Long id, @Valid @RequestBody InterviewUpdateRequest request) {
        return Result.success("修改面试成功", service.update(SecurityUtils.currentUserId(), id, request));
    }

    @PostMapping("/{id}/cancel")
    public Result<InterviewVO> cancel(@PathVariable Long id, @Valid @RequestBody InterviewCancelRequest request) {
        return Result.success("取消面试成功", service.cancel(SecurityUtils.currentUserId(), id, request));
    }

    @PostMapping("/{id}/complete")
    public Result<InterviewVO> complete(@PathVariable Long id, @Valid @RequestBody InterviewCompleteRequest request) {
        return Result.success("完成面试成功", service.complete(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Integer version) {
        service.delete(SecurityUtils.currentUserId(), id, version);
        return Result.success("删除面试成功", null);
    }
}
