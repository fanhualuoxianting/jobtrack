package com.fanhua.jobtrack.module.resume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.BusinessException;
import com.fanhua.jobtrack.common.exception.ConflictException;
import com.fanhua.jobtrack.common.exception.NotFoundException;
import com.fanhua.jobtrack.infrastructure.audit.AuditLogService;
import com.fanhua.jobtrack.infrastructure.file.FileCleanupService;
import com.fanhua.jobtrack.infrastructure.file.FileStorageService;
import com.fanhua.jobtrack.infrastructure.file.FileValidationService;
import com.fanhua.jobtrack.module.resume.entity.Resume;
import com.fanhua.jobtrack.module.resume.mapper.ResumeMapper;
import com.fanhua.jobtrack.module.resume.service.ResumeService;
import com.fanhua.jobtrack.module.resume.vo.ResumeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 简历服务实现。
 *
 * 一致性策略（文档级的取舍说明）：
 * - 上传 = 临时文件 → 校验/摘要 → 事务（写库 + 移动正式目录），移动失败即回滚，
 *   保证 DB 记录与正式文件同生同灭；
 * - 事务回滚（含移动失败、DB 失败）后，临时文件由 afterCompletion 统一清理；
 * - 删除 = 事务内逻辑删除 + 事务提交后物理删除；物理删除失败进入补偿队列重试；
 * - 默认简历唯一性依赖 SELECT ... FOR UPDATE 行锁在同一事务内串行化。
 */
@Slf4j
@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final FileStorageService storageService;
    private final FileValidationService validationService;
    private final FileCleanupService cleanupService;
    private final AuditLogService auditLogService;

    public ResumeServiceImpl(ResumeMapper resumeMapper,
                             FileStorageService storageService,
                             FileValidationService validationService,
                             FileCleanupService cleanupService,
                             AuditLogService auditLogService) {
        this.resumeMapper = resumeMapper;
        this.storageService = storageService;
        this.validationService = validationService;
        this.cleanupService = cleanupService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public ResumeVO upload(Long userId, MultipartFile file, String versionName, String note) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), "未选择文件或文件为空");
        }

        Path tempPath = null;
        try {
            // 1. 流式落盘到临时目录（未通过校验前绝不进入正式目录）
            tempPath = storageService.saveTemporary(file);

            // 2. 联合校验：扩展名 + Content-Type + 魔数/真实结构
            FileValidationService.VerifiedFile verified = validationService.validate(
                    tempPath, file.getOriginalFilename(), file.getContentType(), file.getSize());

            // 3. 流式 SHA-256
            String sha256 = storageService.sha256Of(tempPath);

            // 4. 同用户相同内容查重：拒绝重复上传（三方行为一致：后端 409 / 前端提示 / 测试覆盖）
            Resume existed = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                    .eq(Resume::getUserId, userId)
                    .eq(Resume::getSha256, sha256)
                    .last("LIMIT 1"));
            if (existed != null) {
                throw new ConflictException(ErrorCode.RESUME_DUPLICATE.getCode(),
                        "已存在相同内容的简历版本：" + existed.getVersionName());
            }

            // 5. 组装记录并写库（和 move 在同一事务内）
            Resume resume = new Resume();
            resume.setUserId(userId);
            resume.setVersionName(resolveVersionName(versionName, file.getOriginalFilename()));
            resume.setOriginalFileName(sanitizeOriginalName(file.getOriginalFilename()));
            resume.setMimeType(verified.mimeType());
            resume.setFileSize(file.getSize());
            resume.setSha256(sha256);
            resume.setNote(note == null || note.isBlank() ? null : note.trim());
            // 第一份简历自动设为默认
            boolean firstResume = resumeMapper.selectCount(
                    new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, userId)) == 0;
            resume.setIsDefault(firstResume ? 1 : 0);
            String storageKey = storageService.generateStorageKey(userId, verified.extension());
            resume.setStorageKey(storageKey);
            resumeMapper.insert(resume); // DB 失败 → 异常 → 事务回滚 → 临时文件统一清理

            // 6. 移入正式目录；失败即回滚数据库插入，保证两端一致性
            storageService.moveToFinal(tempPath, storageKey);

            audit("RESUME_UPLOAD", userId, resume.getId(), "上传简历版本");
            return ResumeVO.from(resume);
        } catch (IOException e) {
            log.error("文件存储失败: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "文件存储失败，请稍后重试");
        } finally {
            // 无论成功/失败/回滚，临时文件都不残留
            cleanupTemp(tempPath);
        }
    }

    @Override
    public List<ResumeVO> list(Long userId) {
        return resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, userId)
                        .orderByDesc(Resume::getIsDefault)
                        .orderByDesc(Resume::getUploadedAt))
                .stream().map(ResumeVO::from).toList();
    }

    @Override
    public ResumeVO getDetail(Long userId, Long id) {
        return ResumeVO.from(getOwnedResume(userId, id));
    }

    @Override
    @Transactional
    public ResumeVO updateInfo(Long userId, Long id, String versionName, String note) {
        Resume resume = getOwnedResume(userId, id);
        resume.setVersionName(versionName.trim());
        resume.setNote(note == null || note.isBlank() ? null : note.trim());
        int affected = resumeMapper.update(resume, new LambdaQueryWrapper<Resume>()
                .eq(Resume::getId, id)
                .eq(Resume::getUserId, userId));
        if (affected == 0) {
            throw new ConflictException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode(),
                    "记录已被其他请求修改，请刷新后重试");
        }
        audit("RESUME_UPDATE", userId, id, "修改简历信息");
        return ResumeVO.from(resume);
    }

    @Override
    @Transactional
    public ResumeVO setDefault(Long userId, Long id) {
        // 行锁锁定该用户全部简历，串行化并发设置请求，保证最终只有一条默认
        resumeMapper.lockUserResumeIdsForUpdate(userId);
        getOwnedResume(userId, id); // 归属校验（404）
        resumeMapper.clearDefault(userId);
        int affected = resumeMapper.setDefault(userId, id);
        if (affected == 0) {
            throw new NotFoundException("简历不存在");
        }
        audit("RESUME_SET_DEFAULT", userId, id, "设置默认简历");
        return ResumeVO.from(getOwnedResume(userId, id));
    }

    @Override
    public Resume getOwnedResume(Long userId, Long id) {
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getId, id)
                .eq(Resume::getUserId, userId));
        if (resume == null) {
            throw new NotFoundException("简历不存在");
        }
        return resume;
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        Resume resume = getOwnedResume(userId, id);
        if (resumeMapper.countApplicationsOfResume(userId, id) > 0) {
            throw new ConflictException(ErrorCode.RESUME_IN_USE.getCode(),
                    "简历已被投递记录引用，请先调整相关投递后再删除");
        }
        int affected = resumeMapper.delete(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getId, id)
                .eq(Resume::getUserId, userId));
        if (affected == 0) {
            throw new NotFoundException("简历不存在");
        }
        audit("RESUME_DELETE", userId, id, "删除简历");

        // 物理删除放在事务提交后；失败进入补偿队列，不静默忽略
        String storageKey = resume.getStorageKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storageService.delete(storageKey);
                } catch (IOException | SecurityException e) {
                    cleanupService.registerPendingDelete(storageKey);
                    log.error("简历物理文件删除失败，已转入补偿队列: {}, error={}", storageKey, e.getMessage());
                }
            }
        });
    }

    // ---------- 内部辅助 ----------

    /** 版本名缺省时用原始文件名（去扩展名） */
    private String resolveVersionName(String versionName, String originalFilename) {
        if (versionName != null && !versionName.isBlank()) {
            return versionName.trim();
        }
        String base = originalFilename == null ? "未命名简历" : originalFilename;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return base.length() > 100 ? base.substring(0, 100) : base;
    }

    /** 原始文件名仅作元数据保存：去掉任何路径片段与控制字符，绝不参与路径拼接 */
    private String sanitizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "unnamed";
        }
        String cleaned = originalName.replace("\\", "/");
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0) {
            cleaned = cleaned.substring(slash + 1);
        }
        cleaned = cleaned.replaceAll("[\\p{Cntrl}]", "_");
        return cleaned.length() > 255 ? cleaned.substring(cleaned.length() - 255) : cleaned;
    }

    private void cleanupTemp(Path tempPath) {
        if (tempPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempPath);
        } catch (IOException e) {
            log.error("临时文件清理失败: {}, error={}", tempPath, e.getMessage());
        }
    }

    private void audit(String action, Long userId, Long resumeId, String detail) {
        auditLogService.recordAfterCommit(action, userId, "RESUME", String.valueOf(resumeId), true, detail,
                null, null, org.slf4j.MDC.get("traceId"));
    }
}
