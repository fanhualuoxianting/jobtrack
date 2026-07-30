package com.fanhua.jobtrack.module.resume.service;

import com.fanhua.jobtrack.module.resume.entity.Resume;
import com.fanhua.jobtrack.module.resume.vo.ResumeVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历服务
 */
public interface ResumeService {

    /**
     * 上传简历。
     * 流程：临时目录落盘 → 联合校验 → 流式 SHA-256 → 查重 → 事务内写库 → 移入正式目录。
     * 同用户上传完全相同文件（SHA-256 一致）时拒绝并返回 409。
     */
    ResumeVO upload(Long userId, MultipartFile file, String versionName, String note);

    List<ResumeVO> list(Long userId);

    ResumeVO getDetail(Long userId, Long id);

    ResumeVO updateInfo(Long userId, Long id, String versionName, String note);

    /** 事务内串行设置默认简历，保证同用户最终只有一条默认 */
    ResumeVO setDefault(Long userId, Long id);

    /** 下载专用实体查询（含 storageKey，不暴露给 Controller 以外的层） */
    Resume getOwnedResume(Long userId, Long id);

    /** 逻辑删除（投递引用则 409），提交后物理删除，失败走补偿队列 */
    void delete(Long userId, Long id);
}
