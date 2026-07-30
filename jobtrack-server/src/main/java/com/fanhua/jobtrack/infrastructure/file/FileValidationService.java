package com.fanhua.jobtrack.infrastructure.file;

import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 上传文件联合校验：扩展名 + 客户端 Content-Type + 文件魔数/真实结构三者一致。
 *
 * - PDF：文件头必须为 %PDF-；
 * - DOCX：ZIP 容器，且内部必须存在 [Content_Types].xml 与 word/document.xml，
 *   可拦截"普通 ZIP 改名 DOCX"；
 * - 客户端 Content-Type 只作为参考之一，必须与真实结构判定类型一致，
 *   防止伪造 Content-Type 通过白名单。
 */
@Slf4j
@Component
public class FileValidationService {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final String PDF_MAGIC = "%PDF-";
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};
    private static final int MAX_ZIP_ENTRIES = 1_000;
    private static final long MAX_ZIP_ENTRY_UNCOMPRESSED_BYTES = 20L * 1024 * 1024;
    private static final long MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES = 50L * 1024 * 1024;
    private static final long MAX_ZIP_COMPRESSION_RATIO = 200L;
    private static final long COMPRESSION_RATIO_GRACE_BYTES = 1024L * 1024;

    /** 服务端判定结果：真实类型 + 标准 MIME + 归一扩展名 */
    public record VerifiedFile(String kind, String mimeType, String extension) {
    }

    /**
     * 校验已落盘的临时文件。
     *
     * @param tempPath        临时文件
     * @param originalName    原始文件名（提取扩展名用）
     * @param clientMime      客户端声明的 Content-Type
     * @param declaredSize    客户端声明的文件大小
     */
    public VerifiedFile validate(Path tempPath, String originalName, String clientMime, long declaredSize) {
        try {
            long actualSize = Files.size(tempPath);
            if (actualSize <= 0) {
                throw unsupported("空文件不允许上传");
            }
            if (actualSize > MAX_FILE_SIZE || declaredSize > MAX_FILE_SIZE) {
                throw new com.fanhua.jobtrack.common.exception.PayloadTooLargeException(
                        ErrorCode.RESUME_FILE_TOO_LARGE.getCode(), "文件超过 10MB 限制");
            }

            String extension = extractExtension(originalName);
            byte[] head = readHead(tempPath, 8);

            VerifiedFile result;
            if ("pdf".equals(extension)) {
                if (!startsWithText(head, PDF_MAGIC)) {
                    throw unsupported("文件内容与 PDF 格式不符");
                }
                validatePdfStructure(tempPath);
                result = new VerifiedFile("PDF", "application/pdf", "pdf");
            } else if ("docx".equals(extension)) {
                if (!startsWithBytes(head, ZIP_MAGIC) || !isRealDocx(tempPath)) {
                    throw unsupported("文件内容与合法的 DOCX 结构不符");
                }
                result = new VerifiedFile("DOCX",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
            } else {
                throw unsupported("仅支持 PDF 和 DOCX 格式");
            }

            // 客户端 Content-Type：声明具体类型但与真实结构不符时拒绝；
            // 空值或 application/octet-stream 视为"未声明"，以服务端判定为准。
            // （部分 HTTP 客户端如 requests/curl 默认发 octet-stream，不算伪造）
            String normalizedClientMime = clientMime == null ? "" : clientMime.trim().toLowerCase(Locale.ROOT);
            boolean declaredGeneric = normalizedClientMime.isEmpty()
                    || "application/octet-stream".equals(normalizedClientMime)
                    || "binary/octet-stream".equals(normalizedClientMime);
            if (!declaredGeneric && !result.mimeType().equals(normalizedClientMime)) {
                throw unsupported("声明的文件类型与真实内容不一致");
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("文件校验读取失败: {}", e.getMessage());
            throw unsupported("文件内容无法读取");
        }
    }

    /**
     * PDFBox 深度解析，拒绝只有文件头、但无法解析目录/对象结构的伪 PDF。
     */
    private void validatePdfStructure(Path path) {
        try (PDDocument ignored = Loader.loadPDF(path.toFile())) {
            // 解析成功即代表 PDFBox 能读取基本文档结构；不需要把内容读入内存。
        } catch (IOException | RuntimeException e) {
            log.debug("PDF 深度解析失败: {}", e.getMessage());
            throw unsupported("PDF 文件结构损坏或无法解析");
        }
    }

    /** 校验 ZIP 内部结构并限制解压资源（拦截普通 ZIP 改名、ZIP Bomb 和路径穿越） */
    private boolean isRealDocx(Path path) {
        Set<String> entries = new HashSet<>();
        long totalUncompressed = 0;
        int entryCount = 0;
        try (InputStream in = Files.newInputStream(path);
             ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES || !isSafeZipEntryName(entry.getName())) {
                    return false;
                }
                entries.add(entry.getName());
                long entryBytes = 0;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    entryBytes += read;
                    totalUncompressed += read;
                    if (entryBytes > MAX_ZIP_ENTRY_UNCOMPRESSED_BYTES
                            || totalUncompressed > MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES) {
                        return false;
                    }
                    long compressedSize = entry.getCompressedSize();
                    if (compressedSize > 0
                            && entryBytes > compressedSize * MAX_ZIP_COMPRESSION_RATIO + COMPRESSION_RATIO_GRACE_BYTES) {
                        return false;
                    }
                }
                if (entries.contains("[Content_Types].xml") && entries.contains("word/document.xml")) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isSafeZipEntryName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")
                || normalized.contains("/../") || normalized.startsWith("../")
                || normalized.endsWith("/..") || ".".equals(normalized)) {
            return false;
        }
        try {
            Path path = Paths.get(normalized).normalize();
            return !path.isAbsolute() && !path.startsWith("..");
        } catch (RuntimeException e) {
            return false;
        }
    }

    private byte[] readHead(Path path, int length) throws IOException {
        byte[] buffer = new byte[length];
        try (InputStream in = Files.newInputStream(path)) {
            int read = in.readNBytes(buffer, 0, length);
            if (read < length) {
                byte[] actual = new byte[Math.max(read, 0)];
                System.arraycopy(buffer, 0, actual, 0, actual.length);
                return actual;
            }
            return buffer;
        }
    }

    private boolean startsWithText(byte[] head, String text) {
        byte[] expected = text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (head.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (head[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithBytes(byte[] head, byte[] expected) {
        if (head.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (head[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private String extractExtension(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private BusinessException unsupported(String message) {
        return new BusinessException(ErrorCode.RESUME_FILE_TYPE_UNSUPPORTED.getCode(), message);
    }
}
