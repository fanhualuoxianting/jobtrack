package com.fanhua.jobtrack.infrastructure.file;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 文件存储接口。MVP 使用本地磁盘实现，可替换为对象存储。
 * 调用方只持有 storageKey（相对根目录的存储键），不感知真实物理路径。
 */
public interface FileStorageService {

    /** 将上传内容流式写入临时目录，返回临时文件路径（不落正式目录） */
    Path saveTemporary(MultipartFile file) throws IOException;

    /**
     * 将临时文件移入正式目录（storageKey 由调用方生成，系统侧二次校验路径安全）。
     * 失败时调用方需负责回滚业务并清理临时文件。
     */
    void moveToFinal(Path tempPath, String storageKey) throws IOException;

    /** 按 storageKey 加载资源；键非法或文件逃逸根目录时抛异常 */
    Resource load(String storageKey);

    /** 物理删除；删除失败的补偿责任在调用方 */
    void delete(String storageKey) throws IOException;

    /** 流式计算文件 SHA-256（分块读取，不整载内存） */
    String sha256Of(Path path) throws IOException;

    /** 校验 storageKey 合法性并解析为根目录内路径（防路径穿越/符号链接逃逸） */
    Path resolveInside(String storageKey);

    /** 正式文件是否存在 */
    boolean exists(String storageKey);

    /** 生成正式存储键（随机文件名，目录按用户与年月分片） */
    String generateStorageKey(Long userId, String extension);
}
