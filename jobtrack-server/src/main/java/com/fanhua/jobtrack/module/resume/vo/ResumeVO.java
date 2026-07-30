package com.fanhua.jobtrack.module.resume.vo;

import com.fanhua.jobtrack.common.util.DateTimeUtils;
import com.fanhua.jobtrack.module.resume.entity.Resume;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 简历 VO：不返回 storage_key 等服务器路径信息
 */
@Data
@Schema(description = "简历版本")
public class ResumeVO {

    @Schema(description = "简历ID")
    private Long id;

    @Schema(description = "版本名称")
    private String versionName;

    @Schema(description = "原始文件名")
    private String originalFileName;

    @Schema(description = "服务端判定的 MIME 类型")
    private String mimeType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "SHA-256 摘要")
    private String sha256;

    @Schema(description = "是否为默认简历")
    private Boolean isDefault;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "上传时间")
    private OffsetDateTime uploadedAt;

    private Integer version;

    public static ResumeVO from(Resume entity) {
        ResumeVO vo = new ResumeVO();
        vo.setId(entity.getId());
        vo.setVersionName(entity.getVersionName());
        vo.setOriginalFileName(entity.getOriginalFileName());
        vo.setMimeType(entity.getMimeType());
        vo.setFileSize(entity.getFileSize());
        vo.setSha256(entity.getSha256());
        vo.setIsDefault(entity.getIsDefault() != null && entity.getIsDefault() == 1);
        vo.setNote(entity.getNote());
        vo.setUploadedAt(DateTimeUtils.toOffset(entity.getUploadedAt()));
        vo.setVersion(entity.getVersion());
        return vo;
    }
}
