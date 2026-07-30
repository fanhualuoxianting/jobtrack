package com.fanhua.jobtrack.module.resume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanhua.jobtrack.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 简历管理集成测试。
 * 文件存储指向 JUnit 临时目录（不碰真实上传目录）；数据库为 Testcontainers 真实 MySQL。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResumeIntegrationTest extends AbstractIntegrationTest {

    private static Path testFileRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String tokenA;
    private String tokenB;

    @DynamicPropertySource
    static void fileRootProperties(DynamicPropertyRegistry registry) {
        try {
            testFileRoot = Files.createTempDirectory("jt-resume-it-");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        registry.add("jobtrack.file-root", testFileRoot::toString);
    }

    @BeforeEach
    void setUp() throws Exception {
        suffix = Long.toString(System.nanoTime(), 36);
        tokenA = registerAndLogin("ra_" + suffix);
        tokenB = registerAndLogin("rb_" + suffix);
    }

    // ---------- 测试素材 ----------

    private byte[] pdfBytes() {
        return "%PDF-1.7\n1 0 obj<</Type/Catalog>>\nendobj\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private byte[] docxBytes() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<?xml version=\"1.0\"?><Types></Types>".getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<?xml version=\"1.0\"?><w:document></w:document>".getBytes());
            zip.closeEntry();
        }
        return baos.toByteArray();
    }

    private String registerAndLogin(String name) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"JobTrack@123456","confirmPassword":"JobTrack@123456"}
                                """.formatted(name, name)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"" + name + "@example.com\",\"password\":\"JobTrack@123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).at("/data/accessToken").asText();
    }

    private long uploadResume(String token, String filename, String contentType, byte[] content) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", filename, contentType, content))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asLong();
    }

    // ---------- 用例 ----------

    @Test
    @DisplayName("上传 PDF 成功：首份自动默认、SHA-256 正确、VO 不含 storageKey")
    void uploadPdf() throws Exception {
        byte[] pdf = pdfBytes();
        MvcResult result = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "Java后端简历.pdf", "application/pdf", pdf))
                        .param("note", "第一版")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isDefault").value(true))
                .andExpect(jsonPath("$.data.mimeType").value("application/pdf"))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertFalse(data.has("storageKey"), "响应不得泄露服务器存储路径");

        // 与本地独立计算的 SHA-256 对比
        try (var in = new java.io.ByteArrayInputStream(pdf)) {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            try (var dis = new java.security.DigestInputStream(in, digest)) {
                dis.readAllBytes();
            }
            String expected = java.util.HexFormat.of().formatHex(digest.digest());
            assertEquals(expected, data.get("sha256").asText());
        }
    }

    @Test
    @DisplayName("上传 DOCX 成功（合法 ZIP 结构）")
    void uploadDocx() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "简历.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes()))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mimeType")
                        .value("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andReturn();
        assertNotNull(objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id"));
    }

    @Test
    @DisplayName("空文件拒绝（400）")
    void emptyFileRejected() throws Exception {
        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("超大文件拒绝（multipart 层 413）")
    void oversizedFileRejected() throws Exception {
        byte[] big = new byte[11 * 1024 * 1024];
        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "big.pdf", "application/pdf", big))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("RESUME_FILE_TOO_LARGE"));
    }

    @Test
    @DisplayName("EXE 改名 PDF 拒绝（魔数不符）")
    void exeMasqueradingAsPdfRejected() throws Exception {
        byte[] exe = {0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00}; // MZ 头
        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "evil.pdf", "application/pdf", exe))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESUME_FILE_TYPE_UNSUPPORTED"));
    }

    @Test
    @DisplayName("伪造 Content-Type 拒绝（声明与真实不一致）")
    void forgedContentTypeRejected() throws Exception {
        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "简历.pdf", "text/plain", pdfBytes()))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESUME_FILE_TYPE_UNSUPPORTED"));
    }

    @Test
    @DisplayName("普通 ZIP 改名 DOCX 拒绝（缺少必备内部结构）")
    void plainZipAsDocxRejected() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            zip.putNextEntry(new ZipEntry("readme.txt"));
            zip.write("hello".getBytes());
            zip.closeEntry();
        }
        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "fake.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", baos.toByteArray()))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESUME_FILE_TYPE_UNSUPPORTED"));
    }

    @Test
    @DisplayName("路径穿越文件名被安全清洗，Content-Disposition 不泄露")
    void pathTraversalFilenameSanitized() throws Exception {
        long id = uploadResume(tokenA, "../../evil.pdf", "application/pdf", pdfBytes());

        MvcResult detail = mockMvc.perform(get("/api/v1/resumes/" + id).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        String originalName = objectMapper.readTree(detail.getResponse().getContentAsString())
                .at("/data/originalFileName").asText();
        assertFalse(originalName.contains(".."), "存储的原始文件名不应包含路径穿越");
        assertFalse(originalName.contains("/"), "存储的原始文件名不应包含路径分隔符");

        MvcResult download = mockMvc.perform(get("/api/v1/resumes/" + id + "/download")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        String disposition = download.getResponse().getHeader("Content-Disposition");
        assertNotNull(disposition);
        assertFalse(disposition.contains(".."), "Content-Disposition 不应包含路径穿越");
    }

    @Test
    @DisplayName("中文文件名下载：RFC 5987 编码 + ASCII 兜底 + 内容与大小")
    void chineseFilenameDownload() throws Exception {
        byte[] content = pdfBytes();
        long id = uploadResume(tokenA, "张三的简历.pdf", "application/pdf", content);

        MvcResult download = mockMvc.perform(get("/api/v1/resumes/" + id + "/download")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Length", String.valueOf(content.length)))
                .andReturn();
        String disposition = download.getResponse().getHeader("Content-Disposition");
        assertTrue(disposition.contains("filename*=UTF-8''%E5%BC%A0%E4%B8%89"), "应包含 RFC 5987 中文编码");
        assertArrayEquals(content, download.getResponse().getContentAsByteArray());
    }

    @Test
    @DisplayName("同用户重复上传相同内容返回 409")
    void duplicateContentRejected() throws Exception {
        uploadResume(tokenA, "简历.pdf", "application/pdf", pdfBytes());
        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "换个名字.pdf", "application/pdf", pdfBytes()))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESUME_DUPLICATE"));
    }

    @Test
    @DisplayName("越权详情/下载/删除统一 404")
    void crossUserAccessBlocked() throws Exception {
        long id = uploadResume(tokenA, "隐私.pdf", "application/pdf", pdfBytes());

        mockMvc.perform(get("/api/v1/resumes/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/resumes/" + id + "/download").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/resumes/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        // B 的列表不包含 A 的简历
        mockMvc.perform(get("/api/v1/resumes").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("被投递引用的简历删除返回 409")
    void deleteReferencedResumeBlocked() throws Exception {
        long resumeId = uploadResume(tokenA, "投递用.pdf", "application/pdf", pdfBytes());

        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM jt_user WHERE username = ?", Long.class, "ra_" + suffix);
        // 构造关联：公司/岗位/投递（真实外键链）
        jdbcTemplate.update("INSERT INTO jt_company (user_id, name) VALUES (?, ?)", userId, "引用公司" + suffix);
        Long companyId = jdbcTemplate.queryForObject(
                "SELECT id FROM jt_company WHERE user_id = ? AND name = ?", Long.class, userId, "引用公司" + suffix);
        jdbcTemplate.update("INSERT INTO jt_position (user_id, company_id, title) VALUES (?, ?, ?)",
                userId, companyId, "引用岗位");
        Long positionId = jdbcTemplate.queryForObject(
                "SELECT id FROM jt_position WHERE user_id = ? AND title = ?", Long.class, userId, "引用岗位");
        jdbcTemplate.update(
                "INSERT INTO jt_job_application (user_id, company_id, position_id, resume_id) VALUES (?, ?, ?, ?)",
                userId, companyId, positionId, resumeId);

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESUME_IN_USE"));
    }

    @Test
    @DisplayName("设置默认简历：旧默认自动取消")
    void setDefaultExclusive() throws Exception {
        long first = uploadResume(tokenA, "v1.pdf", "application/pdf", pdfBytes());
        long second = uploadResume(tokenA, "v2.pdf", "application/pdf",
                ("%PDF-1.7 altered content for distinct hash\n%%EOF").getBytes());

        mockMvc.perform(put("/api/v1/resumes/" + second + "/default").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));

        mockMvc.perform(get("/api/v1/resumes/" + first).header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data.isDefault").value(false));
        mockMvc.perform(get("/api/v1/resumes/" + second).header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    @DisplayName("并发设置默认简历：最终仍只有一条默认")
    void concurrentSetDefaultStillUnique() throws Exception {
        long id1 = uploadResume(tokenA, "c1.pdf", "application/pdf", pdfBytes());
        long id2 = uploadResume(tokenA, "c2.pdf", "application/pdf", ("%PDF-2.0 different\n%%EOF").getBytes());
        long id3 = uploadResume(tokenA, "c3.pdf", "application/pdf", ("%PDF-3.0 unique!!\n%%EOF").getBytes());
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM jt_user WHERE username = ?", Long.class, "ra_" + suffix);

        int threads = 3;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch fire = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        long[] ids = {id1, id2, id3};
        for (int i = 0; i < threads; i++) {
            final long targetId = ids[i];
            pool.submit(() -> {
                try {
                    ready.countDown();
                    fire.await();
                    mockMvc.perform(put("/api/v1/resumes/" + targetId + "/default")
                                    .header("Authorization", "Bearer " + tokenA))
                            .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 并发下允许个别请求冲突失败，但最终状态必须唯一
                }
            });
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        fire.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        Long defaultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_resume WHERE user_id = ? AND deleted = 0 AND is_default = 1",
                Long.class, userId);
        assertEquals(1L, defaultCount, "并发设置后默认简历必须唯一");
        assertTrue(successCount.get() >= 1, "至少应有一个设置请求成功");
    }

    @Test
    @DisplayName("数据库写入失败后临时文件被清理，无残留")
    void tempFileCleanedOnDbFailure() throws Exception {
        Path tmpDir = testFileRoot.resolve(".tmp");
        long before = Files.exists(tmpDir) ? Files.list(tmpDir).count() : 0;

        // note 超过数据库 VARCHAR(500) 限制，触发 Data truncation，整个事务回滚
        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "fail.pdf", "application/pdf", pdfBytes()))
                        .param("note", "x".repeat(600))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isInternalServerError());
        Thread.sleep(300); // 等待 finally 清理
        long after = Files.exists(tmpDir) ? Files.list(tmpDir).count() : 0;
        assertEquals(before, after, "数据库失败不应残留临时文件");
    }

    @Test
    @DisplayName("物理文件缺失时下载返回 404 RESUME_FILE_MISSING")
    void missingPhysicalFile() throws Exception {
        long id = uploadResume(tokenA, "将丢失.pdf", "application/pdf", pdfBytes());
        String storageKey = jdbcTemplate.queryForObject(
                "SELECT r.storage_key FROM jt_resume r WHERE r.id = ?", String.class, id);
        assertNotNull(storageKey);
        // 直接删除正式文件模拟丢失
        Path target = testFileRoot.resolve(storageKey);
        assertTrue(Files.exists(target));
        Files.delete(target);

        mockMvc.perform(get("/api/v1/resumes/" + id + "/download").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_FILE_MISSING"));
    }

    @Test
    @DisplayName("存储键路径穿越解析防护（单元级直测）")
    void storageKeyTraversalBlocked(@Autowired com.fanhua.jobtrack.infrastructure.file.FileStorageService storageService) {
        for (String evil : new String[]{"../x", "..\\x", "%2e%2e/x", "C:/x", "/abs/path", "a/../../b"}) {
            assertThrows(RuntimeException.class, () -> storageService.resolveInside(evil),
                    "应拒绝非法存储键: " + evil);
        }
        // 正常键可以解析且位于根目录内
        Path safe = storageService.resolveInside("1/2026/07/abc.pdf");
        assertTrue(safe.toAbsolutePath().normalize().startsWith(testFileRoot.normalize()));
    }
}
