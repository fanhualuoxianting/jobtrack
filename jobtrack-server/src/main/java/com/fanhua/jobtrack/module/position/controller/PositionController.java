package com.fanhua.jobtrack.module.position.controller;

import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.common.api.Result;
import com.fanhua.jobtrack.module.position.dto.PositionQueryRequest;
import com.fanhua.jobtrack.module.position.dto.PositionStatusRequest;
import com.fanhua.jobtrack.module.position.dto.PositionUpsertRequest;
import com.fanhua.jobtrack.module.position.service.PositionService;
import com.fanhua.jobtrack.module.position.vo.PositionDetailVO;
import com.fanhua.jobtrack.module.position.vo.PositionListVO;
import com.fanhua.jobtrack.module.position.vo.PositionOptionVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 岗位管理接口
 */
@RestController
@RequestMapping("/api/v1/positions")
@Tag(name = "岗位管理")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    @Operation(summary = "分页查询岗位", description = "支持关键词、公司、城市、类型、状态、来源、薪资与时间范围组合筛选")
    public Result<PageResult<PositionListVO>> page(@Valid @ParameterObject PositionQueryRequest query) {
        return Result.success("查询成功", positionService.pagePositions(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/options")
    @Operation(summary = "岗位下拉选项（OPEN 状态，可按公司过滤）")
    public Result<List<PositionOptionVO>> options(@RequestParam(required = false) Long companyId) {
        return Result.success(positionService.listOptions(SecurityUtils.currentUserId(), companyId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "岗位详情")
    public Result<PositionDetailVO> detail(@PathVariable Long id) {
        return Result.success(positionService.getDetail(SecurityUtils.currentUserId(), id));
    }

    @PostMapping
    @Operation(summary = "新增岗位")
    public ResponseEntity<Result<PositionDetailVO>> create(@Valid @RequestBody PositionUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success("创建成功", positionService.create(SecurityUtils.currentUserId(), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改岗位")
    public Result<PositionDetailVO> update(@PathVariable Long id, @Valid @RequestBody PositionUpsertRequest request) {
        return Result.success("修改成功", positionService.update(SecurityUtils.currentUserId(), id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "开启或关闭岗位")
    public Result<PositionDetailVO> changeStatus(@PathVariable Long id,
                                                 @Valid @RequestBody PositionStatusRequest request) {
        return Result.success("状态已更新", positionService.changeStatus(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除岗位", description = "存在投递记录时返回 409，建议改为 CLOSED")
    public Result<Void> delete(@PathVariable Long id) {
        positionService.delete(SecurityUtils.currentUserId(), id);
        return Result.success("删除成功", null);
    }
}
