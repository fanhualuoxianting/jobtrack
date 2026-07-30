package com.fanhua.jobtrack.module.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanhua.jobtrack.AbstractIntegrationTest;
import com.fanhua.jobtrack.module.reminder.service.ReminderService;
import com.fanhua.jobtrack.module.reminder.stream.NotificationStreamService;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InterviewIntegrationTest extends AbstractIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ReminderService reminderService;
    @Autowired private NotificationStreamService streamService;

    private String suffix;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() throws Exception {
        suffix = Long.toString(System.nanoTime(), 36);
        tokenA = registerAndLogin("iv_a_" + suffix);
        tokenB = registerAndLogin("iv_b_" + suffix);
    }

    @Test
    @DisplayName("创建首场面试自动推进投递并写入历史与提醒")
    void createInterviewAutoTransitionsAndBuildsReminders() throws Exception {
        long appId = createAppliedApplication(tokenA);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.ofHours(9)).plusHours(2).withSecond(0).withNano(0);
        OffsetDateTime end = start.plusHours(1);
        MvcResult created = mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("applicationId", appId, "roundNumber", 1, "roundName", "技术一面",
                                "interviewType", "VIDEO", "scheduledStartAt", start, "scheduledEndAt", end,
                                "timezone", "Asia/Tokyo", "meetingUrl", "https://example.com/meeting"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.scheduledStartAt").value(org.hamcrest.Matchers.containsString("+09:00")))
                .andReturn();
        long interviewId = body(created).at("/data/id").asLong();

        mockMvc.perform(get("/api/v1/applications/" + appId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("INTERVIEWING"));
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_application_status_log WHERE application_id=?", Long.class, appId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_reminder WHERE interview_id=? AND status='PENDING'", Long.class, interviewId));
    }

    @Test
    @DisplayName("时间、时区和同轮次重复规则在数据库写入前校验")
    void invalidTimeZoneTimeAndDuplicateRound() throws Exception {
        long appId = createAppliedApplication(tokenA);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        String invalid = json(Map.of("applicationId", appId, "roundNumber", 0, "roundName", "错误",
                "interviewType", "VIDEO", "scheduledStartAt", start, "scheduledEndAt", start.minusMinutes(1),
                "timezone", "No/Such_Zone"));
        mockMvc.perform(post("/api/v1/interviews").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());

        OffsetDateTime validStart = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        String first = json(Map.of("applicationId", appId, "roundNumber", 1, "roundName", "技术一面",
                "interviewType", "PHONE", "scheduledStartAt", validStart, "scheduledEndAt", validStart.plusHours(1),
                "timezone", "UTC"));
        mockMvc.perform(post("/api/v1/interviews").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(first)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/interviews").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INTERVIEW_DUPLICATE_ROUND"));
    }

    @Test
    @DisplayName("修改面试使用乐观锁并按新时间重建未发送提醒")
    void updateInterviewOptimisticLockAndRebuildsReminders() throws Exception {
        long appId = createAppliedApplication(tokenA);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).withSecond(0).withNano(0);
        long interviewId = createInterview(tokenA, appId, 1, start);
        int version = interviewVersion(interviewId);
        OffsetDateTime moved = start.plusDays(1);
        mockMvc.perform(patch("/api/v1/interviews/" + interviewId).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("version", version,
                                "scheduledStartAt", moved, "scheduledEndAt", moved.plusHours(1), "timezone", "UTC"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(version + 1));
        assertEquals(4, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_reminder WHERE interview_id=?", Long.class, interviewId));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_reminder WHERE interview_id=? AND status='CANCELLED'", Long.class, interviewId));
        mockMvc.perform(patch("/api/v1/interviews/" + interviewId).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("version", version,
                                "location", "stale"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
    }

    @Test
    @DisplayName("取消同步取消待发送提醒，完成记录结果且不自动推进投递")
    void cancelAndCompleteActions() throws Exception {
        long appId = createAppliedApplication(tokenA);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3);
        long cancelId = createInterview(tokenA, appId, 1, start);
        int cancelVersion = interviewVersion(cancelId);
        mockMvc.perform(post("/api/v1/interviews/" + cancelId + "/cancel")
                        .header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", cancelVersion, "reason", "对方改期"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_reminder WHERE interview_id=? AND status='CANCELLED'", Long.class, cancelId));

        long completeId = createInterview(tokenA, appId, 2, start.plusDays(1));
        int completeVersion = interviewVersion(completeId);
        mockMvc.perform(post("/api/v1/interviews/" + completeId + "/complete")
                        .header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", completeVersion, "result", "PASS", "feedback", "基础扎实"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.result").value("PASS"));
        assertEquals("INTERVIEWING", applicationStatus(appId));
    }

    @Test
    @DisplayName("终态投递、其他用户面试资源统一隔离为 404")
    void terminalAndOwnershipIsolation() throws Exception {
        long appId = createAppliedApplication(tokenA);
        long interviewId = createInterview(tokenA, appId, 1, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
        mockMvc.perform(get("/api/v1/interviews/" + interviewId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/interviews/by-application/" + appId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        transition(tokenA, appId, "CLOSED", 2, "关闭岗位");
        mockMvc.perform(post("/api/v1/interviews").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("applicationId", appId,
                                "roundNumber", 2, "roundName", "二面", "interviewType", "VIDEO",
                                "scheduledStartAt", OffsetDateTime.now(ZoneOffset.UTC).plusDays(4),
                                "scheduledEndAt", OffsetDateTime.now(ZoneOffset.UTC).plusDays(4).plusHours(1), "timezone", "UTC"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INTERVIEW_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("两个扫描线程只抢占并发送同一条到期提醒一次")
    void concurrentScannerSendsOnce() throws Exception {
        long appId = createAppliedApplication(tokenA);
        long interviewId = createInterview(tokenA, appId, 1, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
        jdbcTemplate.update("UPDATE jt_reminder SET scheduled_at=UTC_TIMESTAMP(), status='PENDING' "
                + "WHERE interview_id=? AND reminder_type='INTERVIEW_1_HOUR'", interviewId);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch fire = new CountDownLatch(1);
        pool.submit(() -> { await(fire); reminderService.scanOnce(); });
        pool.submit(() -> { await(fire); reminderService.scanOnce(); });
        fire.countDown(); pool.shutdown(); assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_reminder WHERE interview_id=? AND status='SENT'", Long.class, interviewId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jt_reminder WHERE interview_id=? AND status='PENDING'", Long.class, interviewId));
    }

    @Test
    @DisplayName("通知列表、未读数和重复已读操作按用户隔离")
    void notificationCenterIsolationAndIdempotentRead() throws Exception {
        long appId = createAppliedApplication(tokenA);
        long interviewId = createInterview(tokenA, appId, 1, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
        jdbcTemplate.update("UPDATE jt_reminder SET scheduled_at=UTC_TIMESTAMP(), status='PENDING' "
                + "WHERE interview_id=? AND reminder_type='INTERVIEW_1_HOUR'", interviewId);
        reminderService.scanOnce();
        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(1));
        long reminderId = jdbcTemplate.queryForObject(
                "SELECT id FROM jt_reminder WHERE interview_id=? AND status='SENT' LIMIT 1", Long.class, interviewId);
        mockMvc.perform(patch("/api/v1/notifications/" + reminderId + "/read")
                        .header("Authorization", "Bearer " + tokenB)).andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/notifications/" + reminderId + "/read")
                        .header("Authorization", "Bearer " + tokenA)).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/notifications/" + reminderId + "/read")
                        .header("Authorization", "Bearer " + tokenA)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications?readState=READ").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records.length()").value(1));
    }

    @Test
    @DisplayName("SSE 每用户连接数受限且断开后清理连接")
    void sseConnectionLimitAndCleanup() {
        long userId = jdbcTemplate.queryForObject("SELECT id FROM jt_user WHERE username=?", Long.class, "iv_a_" + suffix);
        SseEmitter one = streamService.connect(userId);
        SseEmitter two = streamService.connect(userId);
        SseEmitter three = streamService.connect(userId);
        assertEquals(3, streamService.connectionCount(userId));
        assertThrows(RuntimeException.class, () -> streamService.connect(userId));
        streamService.disconnect(userId, one); streamService.disconnect(userId, two); streamService.disconnect(userId, three);
        assertEquals(0, streamService.connectionCount(userId));
    }

    private long createAppliedApplication(String token) throws Exception {
        long companyId = createCompany(token, "面试公司-" + suffix);
        long positionId = createPosition(token, companyId, "面试岗位-" + suffix);
        long appId = body(mockMvc.perform(post("/api/v1/applications").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("companyId", companyId, "positionId", positionId))))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asLong();
        transition(token, appId, "APPLIED", 0, null);
        return appId;
    }

    private long createInterview(String token, long appId, int round, OffsetDateTime start) throws Exception {
        return body(mockMvc.perform(post("/api/v1/interviews").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("applicationId", appId,
                                "roundNumber", round, "roundName", "第" + round + "轮", "interviewType", "VIDEO",
                                "scheduledStartAt", start, "scheduledEndAt", start.plusHours(1), "timezone", "UTC"))))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asLong();
    }

    private void transition(String token, long appId, String target, int version, String reason) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("targetStatus", target); body.put("expectedVersion", version); body.put("idempotencyKey", target + "-" + suffix + "-" + version);
        if (reason != null) body.put("reason", reason);
        mockMvc.perform(post("/api/v1/applications/" + appId + "/transitions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(body))).andExpect(status().isOk());
    }

    private int interviewVersion(long id) { return jdbcTemplate.queryForObject("SELECT version FROM jt_interview WHERE id=?", Integer.class, id); }
    private String applicationStatus(long id) { return jdbcTemplate.queryForObject("SELECT status FROM jt_job_application WHERE id=?", String.class, id); }

    private long createCompany(String token, String name) throws Exception {
        return body(mockMvc.perform(post("/api/v1/companies").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", name))))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asLong();
    }

    private long createPosition(String token, long companyId, String title) throws Exception {
        return body(mockMvc.perform(post("/api/v1/positions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("companyId", companyId, "title", title,
                                "workType", "INTERNSHIP", "workplaceType", "ONSITE"))))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asLong();
    }

    private String registerAndLogin(String name) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", name, "email", name + "@example.com",
                                "password", "JobTrack@123456", "confirmPassword", "JobTrack@123456"))))
                .andExpect(status().isCreated());
        return body(mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("account", name + "@example.com", "password", "JobTrack@123456"))))
                .andExpect(status().isOk()).andReturn()).at("/data/accessToken").asText();
    }

    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
    private JsonNode body(MvcResult result) throws Exception { return objectMapper.readTree(result.getResponse().getContentAsString()); }
    private void await(CountDownLatch latch) { try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
