package com.fanhua.jobtrack.module.application.controller;

import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.common.api.Result;
import com.fanhua.jobtrack.module.application.dto.ApplicationCreateRequest;
import com.fanhua.jobtrack.module.application.dto.ApplicationQueryRequest;
import com.fanhua.jobtrack.module.application.dto.ApplicationTransitionRequest;
import com.fanhua.jobtrack.module.application.dto.ApplicationUpdateRequest;
import com.fanhua.jobtrack.module.application.domain.ApplicationStatus;
import com.fanhua.jobtrack.module.application.service.ApplicationService;
import com.fanhua.jobtrack.module.application.vo.ApplicationTimelineVO;
import com.fanhua.jobtrack.module.application.vo.ApplicationVO;
import com.fanhua.jobtrack.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "投递管理")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @Operation(summary = "创建投递", description = "初始状态固定为 SAVED，并写入初始时间线")
    public ResponseEntity<Result<ApplicationVO>> create(@Valid @RequestBody ApplicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success("创建成功", applicationService.create(SecurityUtils.currentUserId(), request)));
    }

    @GetMapping
    @Operation(summary = "分页查询投递", description = "默认隐藏归档记录，排序字段由后端白名单控制")
    public Result<PageResult<ApplicationVO>> page(@Valid @ParameterObject ApplicationQueryRequest request) {
        return Result.success("查询成功", applicationService.page(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "投递详情")
    public Result<ApplicationVO> detail(@PathVariable Long id) {
        return Result.success(applicationService.detail(SecurityUtils.currentUserId(), id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "编辑投递非状态字段", description = "status 只能通过 transition 接口修改")
    public Result<ApplicationVO> update(@PathVariable Long id,
                                        @Valid @RequestBody ApplicationUpdateRequest request) {
        return Result.success("修改成功", applicationService.update(SecurityUtils.currentUserId(), id, request));
    }

    @PostMapping("/{id}/transitions")
    @Operation(summary = "执行状态流转", description = "使用 expectedVersion + 数据库幂等键防止并发覆盖与重复双写")
    public Result<ApplicationVO> transition(@PathVariable Long id,
                                            @Valid @RequestBody ApplicationTransitionRequest request) {
        return Result.success("状态流转成功",
                applicationService.transition(SecurityUtils.currentUserId(), id, request));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "查询投递状态时间线")
    public Result<List<ApplicationTimelineVO>> timeline(@PathVariable Long id) {
        return Result.success(applicationService.timeline(SecurityUtils.currentUserId(), id));
    }

    @GetMapping("/{id}/allowed-transitions")
    @Operation(summary = "查询合法目标状态")
    public Result<List<ApplicationStatus>> allowedTargets(@PathVariable Long id) {
        return Result.success(applicationService.allowedTargets(SecurityUtils.currentUserId(), id));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档投递")
    public Result<ApplicationVO> archive(@PathVariable Long id) {
        return Result.success("归档成功", applicationService.archive(SecurityUtils.currentUserId(), id));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "恢复投递")
    public Result<ApplicationVO> restore(@PathVariable Long id) {
        return Result.success("恢复成功", applicationService.restore(SecurityUtils.currentUserId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除草稿投递", description = "仅允许删除 SAVED 草稿，已进入业务流程的记录使用归档")
    public Result<Void> delete(@PathVariable Long id) {
        applicationService.delete(SecurityUtils.currentUserId(), id);
        return Result.success("删除成功", null);
    }
}
