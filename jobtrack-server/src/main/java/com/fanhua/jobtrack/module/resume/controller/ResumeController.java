package com.fanhua.jobtrack.module.resume.controller;

import com.fanhua.jobtrack.common.api.Result;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.BusinessException;
import com.fanhua.jobtrack.common.exception.NotFoundException;
import com.fanhua.jobtrack.infrastructure.audit.AuditLogService;
import com.fanhua.jobtrack.infrastructure.file.FileStorageService;
import com.fanhua.jobtrack.module.resume.dto.ResumeUpdateRequest;
import com.fanhua.jobtrack.module.resume.entity.Resume;
import com.fanhua.jobtrack.module.resume.service.ResumeService;
import com.fanhua.jobtrack.module.resume.vo.ResumeVO;
import com.fanhua.jobtrack.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 简历管理接口：上传/列表/详情/修改/默认/下载/删除
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resumes")
@Tag(name = "简历管理")
public class ResumeController {

    private final ResumeService resumeService;
    private final FileStorageService storageService;
    private final AuditLogService auditLogService;

    public ResumeController(ResumeService resumeService,
                            FileStorageService storageService,
                            AuditLogService auditLogService) {
        this.resumeService = resumeService;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传简历", description = "仅支持 PDF/DOCX，联合校验扩展名、Content-Type 与真实结构；同内容重复上传返回 409")
    public ResponseEntity<Result<ResumeVO>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "versionName", required = false) String versionName,
                                                   @RequestParam(value = "note", required = false) String note) {
        ResumeVO vo = resumeService.upload(SecurityUtils.currentUserId(), file, versionName, note);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success("上传成功", vo));
    }

    @GetMapping
    @Operation(summary = "简历列表")
    public Result<List<ResumeVO>> list() {
        return Result.success(resumeService.list(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "简历详情")
    public Result<ResumeVO> detail(@PathVariable Long id) {
        return Result.success(resumeService.getDetail(SecurityUtils.currentUserId(), id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改版本名/备注")
    public Result<ResumeVO> update(@PathVariable Long id, @Valid @RequestBody ResumeUpdateRequest request) {
        return Result.success("修改成功",
                resumeService.updateInfo(SecurityUtils.currentUserId(), id, request.getVersionName(), request.getNote()));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设置默认简历", description = "事务串行保证同用户只有一条默认记录")
    public Result<ResumeVO> setDefault(@PathVariable Long id) {
        return Result.success("已设为默认", resumeService.setDefault(SecurityUtils.currentUserId(), id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "下载简历")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUserId();
        Resume resume = resumeService.getOwnedResume(userId, id); // 归属校验，越权 404
        Resource resource = storageService.load(resume.getStorageKey());
        if (!resource.exists()) {
            // 记录存在但物理文件缺失：数据一致性告警 + 专用业务错误码
            log.error("简历物理文件缺失: resumeId={}, storageKey={}", id, resume.getStorageKey());
            throw new NotFoundException(ErrorCode.RESUME_FILE_MISSING.getCode(), "简历文件已丢失，请重新上传", true);
        }
        String contentDisposition = buildContentDisposition(resume.getOriginalFileName());
        auditLogService.recordAfterCommit("RESUME_DOWNLOAD", userId, "RESUME", String.valueOf(id), true,
                "下载简历", null, null, org.slf4j.MDC.get("traceId"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resume.getMimeType()))
                .contentLength(resume.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除简历", description = "被投递引用时返回 409")
    public Result<Void> delete(@PathVariable Long id) {
        resumeService.delete(SecurityUtils.currentUserId(), id);
        return Result.success("删除成功", null);
    }

    /**
     * 构造 Content-Disposition：
     * - filename 仅保留 ASCII 安全字符（旧浏览器兜底），丢弃路径片段与控制字符；
     * - filename* 使用 RFC 5987 UTF-8 百分号编码，兼容中文文件名。
     */
    private String buildContentDisposition(String originalFileName) {
        String cleaned = originalFileName == null ? "resume" : originalFileName;
        cleaned = cleaned.replace("\\", "/");
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0) {
            cleaned = cleaned.substring(slash + 1);
        }
        if (cleaned.isBlank() || "..".equals(cleaned)) {
            cleaned = "resume";
        }
        String ascii = cleaned.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
        String encoded = URLEncoder.encode(cleaned, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }
}
