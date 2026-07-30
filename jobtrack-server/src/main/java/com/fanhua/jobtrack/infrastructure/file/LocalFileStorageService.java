package com.fanhua.jobtrack.infrastructure.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 本地文件存储实现。
 *
 * 目录结构：{root}/{userId}/{yyyy}/{MM}/{uuid}.{ext}
 * - 正式文件名只由系统生成（UUID），用户原始文件名不作为任何路径组成部分；
 * - 临时目录独立，正式数据通过 move 进入，避免半成品文件流入正式目录；
 * - resolveInside 对所有存储键做路径穿越与符号链接逃逸双重防护。
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootPath;
    private final Path tempPath;

    public LocalFileStorageService(@Value("${jobtrack.file-root:./data/uploads}") String fileRoot) throws IOException {
        this.rootPath = Paths.get(fileRoot).toAbsolutePath().normalize();
        this.tempPath = this.rootPath.resolve(".tmp");
        Files.createDirectories(this.tempPath);
        log.info("文件存储根目录: {}", this.rootPath);
    }

    @Override
    public Path saveTemporary(MultipartFile file) throws IOException {
        Path temp = Files.createTempFile(tempPath, "upload-", ".part");
        // 流式拷贝，不整载内存
        try (InputStream in = file.getInputStream();
             OutputStream out = Files.newOutputStream(temp)) {
            in.transferTo(out);
        }
        return temp;
    }

    @Override
    public void moveToFinal(Path tempPath, String storageKey) throws IOException {
        Path target = resolveInside(storageKey);
        Files.createDirectories(target.getParent());
        try {
            Files.move(tempPath, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailed) {
            // 跨文件系统等场景降级为拷贝+删除
            Files.copy(tempPath, target, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(tempPath);
        }
    }

    @Override
    public Resource load(String storageKey) {
        return new FileSystemResource(resolveInside(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveInside(storageKey));
    }

    @Override
    public String sha256Of(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(path);
                 DigestInputStream dis = new DigestInputStream(in, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // 仅驱动摘要计算
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 解析存储键为根目录内的安全路径。
     * 拒绝：空键、URL 编码字符、反斜杠、上级目录片段、Windows 盘符、绝对路径、符号链接逃逸。
     */
    @Override
    public Path resolveInside(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("存储键为空");
        }
        // URL 编码的穿越字符（%2e%2e 等）在键中一律不允许
        if (storageKey.contains("%")) {
            throw new SecurityException("存储键包含非法编码字符");
        }
        if (storageKey.contains("\\")) {
            throw new SecurityException("存储键包含非法路径分隔符");
        }
        if (storageKey.contains(":")) {
            // Windows 盘符或冒号绝对路径
            throw new SecurityException("存储键包含盘符或绝对路径特征");
        }
        Path candidate = rootPath.resolve(storageKey).normalize();
        if (!candidate.startsWith(rootPath)) {
            throw new SecurityException("存储键逃逸存储根目录");
        }
        // 符号链接逃逸防护：目标存在且为链接时，解析真实路径再次校验
        if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(candidate)) {
            try {
                Path realTarget = candidate.toRealPath();
                if (!realTarget.startsWith(rootPath)) {
                    throw new SecurityException("符号链接逃逸存储根目录");
                }
            } catch (IOException e) {
                throw new SecurityException("无法验证存储路径安全性", e);
            }
        }
        return candidate;
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            return Files.exists(resolveInside(storageKey));
        } catch (SecurityException e) {
            return false;
        }
    }

    /** 生成正式存储键（UUID 文件名，目录按用户与年月分片） */
    @Override
    public String generateStorageKey(Long userId, String extension) {
        java.time.YearMonth ym = java.time.YearMonth.now();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return userId + "/" + ym.getYear() + "/" + String.format("%02d", ym.getMonthValue())
                + "/" + uuid + "." + extension;
    }
}
