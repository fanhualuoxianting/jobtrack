package com.fanhua.jobtrack.module.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanhua.jobtrack.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 阶段 5 核心集成测试：使用基类中的真实 MySQL 8.4/Redis 8 容器。 */
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() throws Exception {
        suffix = Long.toString(System.nanoTime(), 36);
        tokenA = registerAndLogin("aa_" + suffix);
        tokenB = registerAndLogin("bb_" + suffix);
    }

    @Test
    @DisplayName("创建投递并生成初始历史")
    void createAndInitialTimeline() throws Exception {
        long companyId = createCompany(tokenA, "公司-" + suffix);
        long positionId = createPosition(tokenA, companyId, "Java实习-" + suffix);

        MvcResult result = createApplication(tokenA, companyId, positionId);
        JsonNode data = body(result).at("/data");
        assertEquals("SAVED", data.at("/status").asText());
        long appId = data.at("/id").asLong();

        mockMvc.perform(get("/api/v1/applications/" + appId + "/timeline")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].fromStatus").doesNotExist())
                .andExpect(jsonPath("$.data[0].toStatus").value("SAVED"));
    }

    @Test
    @DisplayName("资源越权统一返回 404，岗位与公司不匹配也不泄露")
    void ownershipAndCompanyMismatchAreNotFound() throws Exception {
        long companyA = createCompany(tokenA, "A公司-" + suffix);
        long positionA = createPosition(tokenA, companyA, "A岗位-" + suffix);
        long companyB = createCompany(tokenB, "B公司-" + suffix);

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyB + ",\"positionId\":" + positionA + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyB + ",\"positionId\":" + positionA + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("关闭岗位不能创建投递且重复投递返回稳定 409")
    void closedAndDuplicatePositionRules() throws Exception {
        long companyId = createCompany(tokenA, "规则公司-" + suffix);
        long positionId = createPosition(tokenA, companyId, "规则岗位-" + suffix);
        createApplication(tokenA, companyId, positionId);

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"positionId\":" + positionId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPLICATION_ALREADY_EXISTS"));

        jdbcTemplate.update("UPDATE jt_position SET status='CLOSED' WHERE id=?", positionId);
        long anotherPosition = createPosition(tokenA, companyId, "关闭岗位-" + suffix);
        jdbcTemplate.update("UPDATE jt_position SET status='CLOSED' WHERE id=?", anotherPosition);
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"positionId\":" + anotherPosition + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPLICATION_POSITION_CLOSED"));
    }

    @Test
    @DisplayName("普通 PATCH 不能修改 status，合法与非法状态流转分别受状态机控制")
    void patchCannotChangeStatusAndStateMachineWorks() throws Exception {
        long companyId = createCompany(tokenA, "状态公司-" + suffix);
        long positionId = createPosition(tokenA, companyId, "状态岗位-" + suffix);
        long appId = body(createApplication(tokenA, companyId, positionId)).at("/data/id").asLong();
        int version = body(mockMvc.perform(get("/api/v1/applications/" + appId)
                        .header("Authorization", "Bearer " + tokenA)).andReturn()).at("/data/version").asInt();

        mockMvc.perform(patch("/api/v1/applications/" + appId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + version + ",\"status\":\"APPLIED\",\"note\":\"保留\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SAVED"));

        int updatedVersion = body(mockMvc.perform(get("/api/v1/applications/" + appId)
                        .header("Authorization", "Bearer " + tokenA)).andReturn()).at("/data/version").asInt();
        mockMvc.perform(post("/api/v1/applications/" + appId + "/transitions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"INTERVIEWING\",\"expectedVersion\":"
                                + updatedVersion + ",\"idempotencyKey\":\"bad-" + suffix + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APPLICATION_INVALID_STATUS_TRANSITION"));

        mockMvc.perform(post("/api/v1/applications/" + appId + "/transitions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"APPLIED\",\"expectedVersion\":"
                                + updatedVersion + ",\"idempotencyKey\":\"apply-" + suffix + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    @Test
    @DisplayName("版本冲突与并发流转：两个线程只有一个成功")
    void optimisticLockConcurrentTransition() throws Exception {
        long companyId = createCompany(tokenA, "并发公司-" + suffix);
        long positionId = createPosition(tokenA, companyId, "并发岗位-" + suffix);
        long appId = body(createApplication(tokenA, companyId, positionId)).at("/data/id").asLong();
        int version = body(mockMvc.perform(get("/api/v1/applications/" + appId)
                .header("Authorization", "Bearer " + tokenA)).andReturn()).at("/data/version").asInt();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch fire = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            final String key = "concurrent-" + suffix + "-" + i;
            pool.submit(() -> {
                try {
                    ready.countDown();
                    fire.await();
                    int status = mockMvc.perform(post("/api/v1/applications/" + appId + "/transitions")
                                    .header("Authorization", "Bearer " + tokenA)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"targetStatus\":\"APPLIED\",\"expectedVersion\":"
                                            + version + ",\"idempotencyKey\":\"" + key + "\"}"))
                            .andReturn().getResponse().getStatus();
                    if (status == 200) success.incrementAndGet();
                    if (status == 409) conflict.incrementAndGet();
                } catch (Exception ignored) {
                }
                return null;
            });
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        fire.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals(1, success.get());
        assertEquals(1, conflict.get());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_application_status_log WHERE application_id=?", Long.class, appId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_application_status_log WHERE application_id=? AND to_status='APPLIED'",
                Long.class, appId));
    }

    @Test
    @DisplayName("同幂等键重试返回第一次结果，不同请求参数返回 409")
    void idempotencySameAndDifferentRequest() throws Exception {
        long companyId = createCompany(tokenA, "幂等公司-" + suffix);
        long positionId = createPosition(tokenA, companyId, "幂等岗位-" + suffix);
        long appId = body(createApplication(tokenA, companyId, positionId)).at("/data/id").asLong();
        int version = body(mockMvc.perform(get("/api/v1/applications/" + appId)
                .header("Authorization", "Bearer " + tokenA)).andReturn()).at("/data/version").asInt();
        String key = "same-" + suffix;
        String request = "{\"targetStatus\":\"APPLIED\",\"reason\":\"已投递\",\"expectedVersion\":"
                + version + ",\"idempotencyKey\":\"" + key + "\"}";
        mockMvc.perform(post("/api/v1/applications/" + appId + "/transitions")
                        .header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/applications/" + appId + "/transitions")
                        .header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_application_status_log WHERE application_id=?", Long.class, appId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_application_idempotency WHERE application_id=?", Long.class, appId));

        mockMvc.perform(post("/api/v1/applications/" + appId + "/transitions")
                        .header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"ASSESSMENT\",\"expectedVersion\":2,\"idempotencyKey\":\""
                                + key + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    @DisplayName("归档隐藏、恢复可见、越权时间线返回 404，草稿可删除")
    void archiveRestoreDeleteAndIsolation() throws Exception {
        long companyId = createCompany(tokenA, "归档公司-" + suffix);
        long positionId = createPosition(tokenA, companyId, "归档岗位-" + suffix);
        long appId = body(createApplication(tokenA, companyId, positionId)).at("/data/id").asLong();
        mockMvc.perform(post("/api/v1/applications/" + appId + "/archive")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.archived").value(true));
        mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records").isEmpty());
        mockMvc.perform(get("/api/v1/applications?archived=true").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records.length()").value(1));
        mockMvc.perform(get("/api/v1/applications/" + appId + "/timeline")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/applications/" + appId + "/restore")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.archived").value(false));
        mockMvc.perform(delete("/api/v1/applications/" + appId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    private long createCompany(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return body(result).at("/data/id").asLong();
    }

    private long createPosition(String token, long companyId, String title) throws Exception {
        String json = "{\"companyId\":" + companyId + ",\"title\":\"" + title
                + "\",\"workType\":\"INTERNSHIP\",\"workplaceType\":\"ONSITE\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/positions")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn();
        return body(result).at("/data/id").asLong();
    }

    private MvcResult createApplication(String token, long companyId, long positionId) throws Exception {
        return mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":" + companyId + ",\"positionId\":" + positionId + "}"))
                .andExpect(status().isCreated()).andReturn();
    }

    private String registerAndLogin(String name) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + name + "\",\"email\":\"" + name
                                + "@example.com\",\"password\":\"JobTrack@123456\",\"confirmPassword\":\"JobTrack@123456\"}"))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"" + name + "@example.com\",\"password\":\"JobTrack@123456\"}"))
                .andExpect(status().isOk()).andReturn();
        return body(result).at("/data/accessToken").asText();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
