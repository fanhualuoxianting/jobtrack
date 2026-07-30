package com.fanhua.jobtrack.module.company.controller;

import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.common.api.Result;
import com.fanhua.jobtrack.module.company.dto.CompanyQueryRequest;
import com.fanhua.jobtrack.module.company.dto.CompanyUpsertRequest;
import com.fanhua.jobtrack.module.company.service.CompanyService;
import com.fanhua.jobtrack.module.company.vo.CompanyDetailVO;
import com.fanhua.jobtrack.module.company.vo.CompanyListVO;
import com.fanhua.jobtrack.module.company.vo.CompanyOptionVO;
import com.fanhua.jobtrack.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公司管理接口。当前用户从安全上下文获取，不接受前端传入的 userId。
 */
@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "公司管理")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "分页查询公司")
    public Result<PageResult<CompanyListVO>> page(@Valid @ParameterObject CompanyQueryRequest query) {
        return Result.success("查询成功", companyService.pageCompanies(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/options")
    @Operation(summary = "公司下拉选项")
    public Result<List<CompanyOptionVO>> options() {
        return Result.success(companyService.listOptions(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "公司详情")
    public Result<CompanyDetailVO> detail(@PathVariable Long id) {
        return Result.success(companyService.getDetail(SecurityUtils.currentUserId(), id));
    }

    @PostMapping
    @Operation(summary = "新增公司")
    public ResponseEntity<Result<CompanyDetailVO>> create(@Valid @RequestBody CompanyUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success("创建成功", companyService.create(SecurityUtils.currentUserId(), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改公司")
    public Result<CompanyDetailVO> update(@PathVariable Long id, @Valid @RequestBody CompanyUpsertRequest request) {
        return Result.success("修改成功", companyService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除公司", description = "存在关联岗位或投递时返回 409")
    public Result<Void> delete(@PathVariable Long id) {
        companyService.delete(SecurityUtils.currentUserId(), id);
        return Result.success("删除成功", null);
    }
}
