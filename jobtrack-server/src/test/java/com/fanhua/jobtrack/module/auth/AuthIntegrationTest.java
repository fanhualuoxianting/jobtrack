package com.fanhua.jobtrack.module.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanhua.jobtrack.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证与会话集成测试（Testcontainers 真实 MySQL + Redis）。
 * 覆盖：注册/冲突/登录/限流锁定/刷新轮换/重放攻击/注销/多设备/会话越权。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String RT_COOKIE = "JOBTRACK_RT";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------- 工具方法 ----------

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime(), 36);
    }

    private String registerPayload(String username, String email) {
        return """
                {"username":"%s","email":"%s","password":"JobTrack@123456","confirmPassword":"JobTrack@123456","nickname":"测试"}
                """.formatted(username, email);
    }

    private void register(String username, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(username, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    /** 登录并返回 [accessToken, refreshToken] */
    private String[] login(String account) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"" + account + "\",\"password\":\"JobTrack@123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = body.at("/data/accessToken").asText();
        Cookie rtCookie = result.getResponse().getCookie(RT_COOKIE);
        assertNotNull(rtCookie, "登录必须设置 Refresh Cookie");
        assertTrue(rtCookie.isHttpOnly(), "Refresh Cookie 必须 HttpOnly");
        assertEquals("Strict", rtCookie.getAttribute("SameSite"));
        return new String[]{accessToken, rtCookie.getValue()};
    }

    // ---------- 用例 ----------

    @Test
    @DisplayName("注册成功，响应不包含密码字段且写入用户设置")
    void registerSuccess() throws Exception {
        String name = "reg_" + uniqueSuffix();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(name, name + "@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.username").value(name))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("password"), "注册响应绝不能包含密码字段");
        assertFalse(body.contains("passwordHash"), "注册响应绝不能包含密码哈希字段");
        assertNotNull(result.getResponse().getHeader("X-Request-ID"), "响应必须携带 TraceId");
    }

    @Test
    @DisplayName("重复用户名注册返回 409 USER_USERNAME_EXISTS")
    void registerDuplicateUsername() throws Exception {
        String name = "dup_" + uniqueSuffix();
        register(name, name + "@a.com");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(name, name + "@b.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_USERNAME_EXISTS"));
    }

    @Test
    @DisplayName("重复邮箱注册返回 409 USER_EMAIL_EXISTS")
    void registerDuplicateEmail() throws Exception {
        String name = "dupm_" + uniqueSuffix();
        register(name + "_1", name + "@example.com");
        // 邮箱大小写不同也算重复
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(name + "_2", name.toUpperCase() + "@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_EXISTS"));
    }

    @Test
    @DisplayName("两次密码不一致返回 400")
    void registerPasswordMismatch() throws Exception {
        String name = "pm_" + uniqueSuffix();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"JobTrack@123456","confirmPassword":"Different@123"}
                                """.formatted(name, name)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REGISTER_PASSWORD_MISMATCH"));
    }

    @Test
    @DisplayName("用户名格式非法返回 400 VALIDATION_ERROR")
    void registerInvalidUsername() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ab","email":"ok@example.com","password":"JobTrack@123456","confirmPassword":"JobTrack@123456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("登录成功返回 token 结构与用户摘要")
    void loginSuccess() throws Exception {
        String name = "login_" + uniqueSuffix();
        register(name, name + "@example.com");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"" + name + "@example.com\",\"password\":\"JobTrack@123456\",\"deviceName\":\"JUnit\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(jsonPath("$.data.user.username").value(name))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("passwordHash"));
    }

    @Test
    @DisplayName("密码错误返回 401 且不暴露账号是否存在")
    void loginWrongPassword() throws Exception {
        String name = "wp_" + uniqueSuffix();
        register(name, name + "@example.com");
        // 存在的账号密码错误
        MvcResult r1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"" + name + "@example.com\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
                .andReturn();
        // 不存在的账号返回相同错误码与消息，防止账号枚举
        MvcResult r2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"no_such_user@example.com\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
                .andReturn();
        JsonNode b1 = objectMapper.readTree(r1.getResponse().getContentAsString());
        JsonNode b2 = objectMapper.readTree(r2.getResponse().getContentAsString());
        assertEquals(b1.get("message").asText(), b2.get("message").asText(), "两种失败必须返回相同消息");
    }

    @Test
    @DisplayName("未携带 Token 访问受保护接口返回 401 JSON 而非默认错误页")
    void unauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("伪造 Token 访问返回 401 AUTH_TOKEN_INVALID")
    void forgedTokenRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.invalidsig"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("Access Token 可访问 /me；无 Cookie 刷新返回 401")
    void meAndMissingRefreshCookie() throws Exception {
        String name = "me_" + uniqueSuffix();
        register(name, name + "@example.com");
        String[] tokens = login(name + "@example.com");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokens[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(name));

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_INVALID"));
    }

    @Test
    @DisplayName("Refresh Token 轮换成功且旧令牌立即失效")
    void refreshRotation() throws Exception {
        String name = "rot_" + uniqueSuffix();
        register(name, name + "@example.com");
        String[] tokens = login(name + "@example.com");
        String oldRt = tokens[1];

        // 第一次轮换：成功并拿到新 RT
        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(RT_COOKIE, oldRt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn();
        Cookie newRtCookie = refreshed.getResponse().getCookie(RT_COOKIE);
        assertNotNull(newRtCookie);
        String newRt = newRtCookie.getValue();
        assertNotEquals(oldRt, newRt, "轮换必须签发新的 Refresh Token");

        // 旧 RT 再次使用：判定重放，撤销会话
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(RT_COOKIE, oldRt)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_REUSED"));

        // 重放导致会话被撤销：新 RT 也已失效
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(RT_COOKIE, newRt)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_REVOKED"));
    }

    @Test
    @DisplayName("登出后 Refresh Token 失效，Access Token 拒绝访问")
    void logoutInvalidatesSession() throws Exception {
        String name = "lo_" + uniqueSuffix();
        register(name, name + "@example.com");
        String[] tokens = login(name + "@example.com");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + tokens[0])
                        .cookie(new Cookie(RT_COOKIE, tokens[1])))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie",
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("Max-Age=0"))));

        // RT 已撤销
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(RT_COOKIE, tokens[1])))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_REVOKED"));

        // AT 因会话撤销标记被拒绝
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokens[0]))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_REVOKED"));
    }

    @Test
    @DisplayName("多设备会话：列表、注销指定、注销其他设备")
    void multiDeviceSessions() throws Exception {
        String name = "md_" + uniqueSuffix();
        register(name, name + "@example.com");
        String[] dev1 = login(name + "@example.com");
        String[] dev2 = login(name + "@example.com");

        // 两个会话
        MvcResult sessionsResult = mockMvc.perform(get("/api/v1/auth/sessions")
                        .header("Authorization", "Bearer " + dev1[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();
        JsonNode sessions = objectMapper.readTree(sessionsResult.getResponse().getContentAsString()).at("/data");
        String otherSessionId = null;
        for (JsonNode s : sessions) {
            if (!s.get("current").asBoolean()) {
                otherSessionId = s.get("sessionId").asText();
            }
        }
        assertNotNull(otherSessionId, "应存在另一个设备的会话");

        // 注销另一台设备
        mockMvc.perform(delete("/api/v1/auth/sessions/" + otherSessionId)
                        .header("Authorization", "Bearer " + dev1[0]))
                .andExpect(status().isOk());

        // 被注销设备的 AT 不再可用
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + dev2[0]))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_REVOKED"));

        // logout-all keepCurrent=true：只剩当前设备
        String[] dev3 = login(name + "@example.com");
        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header("Authorization", "Bearer " + dev3[0])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keepCurrent\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/sessions")
                        .header("Authorization", "Bearer " + dev3[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("用户 B 无法注销用户 A 的会话（404 而非 403，不泄露存在性）")
    void crossUserSessionRevokeForbidden() throws Exception {
        String a = "iso_a" + uniqueSuffix();
        String b = "iso_b" + uniqueSuffix();
        register(a, a + "@example.com");
        register(b, b + "@example.com");
        String[] tokensA = login(a + "@example.com");
        String[] tokensB = login(b + "@example.com");

        // 拿到 A 的 sessionId
        MvcResult sessionsA = mockMvc.perform(get("/api/v1/auth/sessions")
                        .header("Authorization", "Bearer " + tokensA[0]))
                .andExpect(status().isOk())
                .andReturn();
        String sessionIdA = objectMapper.readTree(sessionsA.getResponse().getContentAsString())
                .at("/data/0/sessionId").asText();

        // B 尝试注销 A 的会话 → 404
        mockMvc.perform(delete("/api/v1/auth/sessions/" + sessionIdA)
                        .header("Authorization", "Bearer " + tokensB[0]))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // A 的会话仍有效
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokensA[0]))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("连续失败达到阈值后锁定，正确密码也被拒绝（423）")
    void loginLockAfterFailures() throws Exception {
        String name = "lock_" + uniqueSuffix();
        register(name, name + "@example.com");
        String account = name + "@example.com";

        // test profile 阈值为 3：前两次 401，第三次触发 423
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"account\":\"" + account + "\",\"password\":\"WrongPass1\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"" + account + "\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("AUTH_LOGIN_LOCKED"));

        // 锁定期间正确密码也 423
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"" + account + "\",\"password\":\"JobTrack@123456\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("AUTH_LOGIN_LOCKED"));
    }
}
