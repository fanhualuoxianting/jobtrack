package com.fanhua.jobtrack.module.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanhua.jobtrack.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardIntegrationTest extends AbstractIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redis;

    private String suffix;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() throws Exception {
        suffix = Long.toString(System.nanoTime(), 36);
        tokenA = registerAndLogin("dash_a_" + suffix);
        tokenB = registerAndLogin("dash_b_" + suffix);
    }

    @Test
    @DisplayName("统计基于状态历史计算漏斗和转化率，并返回即将到来的面试")
    void summaryFunnelConversionAndUpcoming() throws Exception {
        long appId = createAppliedApplication(tokenA, "BOSS");
        transition(tokenA, appId, "INTERVIEWING", 1, null);
        transition(tokenA, appId, "OFFERED", 2, null);
        long upcomingAppId = createAppliedApplication(tokenA, "BOSS");
        createInterview(tokenA, upcomingAppId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
        long companyId = companyId(appId);
        String query = "?startDate=2026-07-01&endDate=2026-08-31&timezone=Asia/Shanghai&companyId=" + companyId;
        String allQuery = "?startDate=2026-07-01&endDate=2026-08-31&timezone=Asia/Shanghai";

        mockMvc.perform(get("/api/v1/dashboard/summary" + query).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalApplications").value(1))
                .andExpect(jsonPath("$.data.interviewApplications").value(1))
                .andExpect(jsonPath("$.data.offeredApplications").value(1))
                .andExpect(jsonPath("$.data.interviewConversionRate").value(100.0))
                .andExpect(jsonPath("$.data.offerConversionRate").value(100.0));
        mockMvc.perform(get("/api/v1/dashboard/funnel" + query).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].count").value(1))
                .andExpect(jsonPath("$.data[2].count").value(1)).andExpect(jsonPath("$.data[3].count").value(1));
        mockMvc.perform(get("/api/v1/dashboard/sources" + query).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].name").value("BOSS"));
        mockMvc.perform(get("/api/v1/dashboard/trends" + allQuery + "&granularity=DAY")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()" ).value(62));
        mockMvc.perform(get("/api/v1/dashboard/industries" + allQuery).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].name").value("未分类"));
        mockMvc.perform(get("/api/v1/dashboard/cycle-time" + allQuery).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.appliedToInterviewing.sampleCount").value(2));
        mockMvc.perform(get("/api/v1/dashboard/upcoming" + allQuery).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()" ).value(1));
    }

    @Test
    @DisplayName("默认排除归档、显式包含归档且用户和公司筛选隔离")
    void archivedAndOwnershipFilters() throws Exception {
        long appId = createAppliedApplication(tokenA, "REFERRAL");
        long companyId = companyId(appId);
        mockMvc.perform(post("/api/v1/applications/" + appId + "/archive").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        String common = "?startDate=2026-07-01&endDate=2026-08-31&timezone=Asia/Shanghai";
        mockMvc.perform(get("/api/v1/dashboard/summary" + common).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalApplications").value(0));
        mockMvc.perform(get("/api/v1/dashboard/summary" + common + "&includeArchived=true")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalApplications").value(1));
        mockMvc.perform(get("/api/v1/dashboard/summary" + common + "&companyId=" + companyId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("统计参数、空数据、趋势空桶和缓存键行为明确")
    void validationEmptyTrendAndCache() throws Exception {
        String empty = "?startDate=2026-07-01&endDate=2026-07-03&timezone=UTC&granularity=DAY";
        mockMvc.perform(get("/api/v1/dashboard/summary" + empty).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalApplications").value(0))
                .andExpect(jsonPath("$.data.interviewConversionRate").value(0.0));
        mockMvc.perform(get("/api/v1/dashboard/trends" + empty).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()" ).value(3))
                .andExpect(jsonPath("$.data[0].count").value(0));
        mockMvc.perform(get("/api/v1/dashboard/summary?startDate=2026-08-01&endDate=2026-07-01")
                        .header("Authorization", "Bearer " + tokenB)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/dashboard/summary?timezone=No/Such_Zone")
                        .header("Authorization", "Bearer " + tokenB)).andExpect(status().isBadRequest());
        long appId = createAppliedApplication(tokenA, "CAMPUS");
        String query = "?startDate=2026-07-01&endDate=2026-08-31&timezone=Asia/Shanghai";
        mockMvc.perform(get("/api/v1/dashboard/summary" + query).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        assertTrue(redis.keys("dashboard:v1:*").stream().anyMatch(key -> key.contains("summary")));
        mockMvc.perform(get("/api/v1/dashboard/summary" + query).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/companies/" + companyId(appId)).header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "看板公司更新-" + suffix, "industry", "互联网"))))
                .andExpect(status().isOk());
    }

    private long createAppliedApplication(String token, String source) throws Exception {
        long companyId = body(mockMvc.perform(post("/api/v1/companies").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "看板公司-" + suffix + "-" + System.nanoTime()))))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asLong();
        long positionId = body(mockMvc.perform(post("/api/v1/positions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("companyId", companyId, "title", "看板岗位-" + suffix + "-" + System.nanoTime(),
                                "workType", "INTERNSHIP", "workplaceType", "REMOTE"))))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asLong();
        long id = body(mockMvc.perform(post("/api/v1/applications").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("companyId", companyId, "positionId", positionId,
                                "source", source, "appliedAt", OffsetDateTime.now(ZoneOffset.UTC)))))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asLong();
        transition(token, id, "APPLIED", 0, null);
        return id;
    }

    private long createInterview(String token, long appId, OffsetDateTime start) throws Exception {
        return body(mockMvc.perform(post("/api/v1/interviews").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("applicationId", appId, "roundNumber", 1,
                                "roundName", "看板面试", "interviewType", "VIDEO", "scheduledStartAt", start,
                                "scheduledEndAt", start.plusHours(1), "timezone", "UTC"))))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asLong();
    }

    private void transition(String token, long appId, String target, int version, String reason) throws Exception {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("targetStatus", target); request.put("expectedVersion", version);
        request.put("idempotencyKey", target + "-" + suffix + "-" + appId + "-" + version);
        if (reason != null) request.put("reason", reason);
        mockMvc.perform(post("/api/v1/applications/" + appId + "/transitions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request))).andExpect(status().isOk());
    }

    private long companyId(long applicationId) throws Exception {
        return body(mockMvc.perform(get("/api/v1/applications/" + applicationId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andReturn()).at("/data/companyId").asLong();
    }

    private String registerAndLogin(String name) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", name, "email", name + "@example.com", "password", "JobTrack@123456", "confirmPassword", "JobTrack@123456"))))
                .andExpect(status().isCreated());
        return body(mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("account", name + "@example.com", "password", "JobTrack@123456"))))
                .andExpect(status().isOk()).andReturn()).at("/data/accessToken").asText();
    }

    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
    private JsonNode body(MvcResult result) throws Exception { return objectMapper.readTree(result.getResponse().getContentAsString()); }
}
