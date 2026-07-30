package com.fanhua.jobtrack.module.company;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 公司与岗位集成测试。
 * 覆盖：CRUD、名称规范化查重、组合筛选、越权隔离、URL/薪资校验、重复岗位、关联删除保护。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CompanyPositionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() throws Exception {
        suffix = Long.toString(System.nanoTime(), 36);
        tokenA = registerAndLogin("u_a" + suffix);
        tokenB = registerAndLogin("u_b" + suffix);
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
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
    }

    /** 用户 A 创建公司，返回公司 ID */
    private long createCompany(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"city\":\"南京\",\"industry\":\"互联网\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asLong();
    }

    @Test
    @DisplayName("新增公司成功；列表返回所属公司且带统计数")
    void createAndListCompany() throws Exception {
        long id = createCompany(tokenA, "示例科技公司" + suffix);

        mockMvc.perform(get("/api/v1/companies/" + id).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("示例科技公司" + suffix))
                .andExpect(jsonPath("$.data.city").value("南京"));

        mockMvc.perform(get("/api/v1/companies?keyword=示例科技").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].positionCount").value(0));
    }

    @Test
    @DisplayName("公司名称规范化后重复返回 409")
    void duplicateCompanyName() throws Exception {
        createCompany(tokenA, "重名公司" + suffix);
        // 首尾空格不影响重复判定
        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  重名公司" + suffix + "  \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANY_NAME_EXISTS"));
    }

    @Test
    @DisplayName("用户 B 无法查看、修改、删除用户 A 的公司（统一 404）")
    void crossUserCompanyIsolation() throws Exception {
        long id = createCompany(tokenA, "隔离公司" + suffix);

        mockMvc.perform(get("/api/v1/companies/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(put("/api/v1/companies/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"被篡改\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/companies/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // B 的列表中不应出现 A 的公司
        mockMvc.perform(get("/api/v1/companies?keyword=隔离公司" + suffix)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("创建岗位成功并出现在列表中")
    void createPosition() throws Exception {
        long companyId = createCompany(tokenA, "岗位公司" + suffix);
        MvcResult result = mockMvc.perform(post("/api/v1/positions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId":%d,"title":"Java 实习生","workType":"INTERNSHIP","workplaceType":"ONSITE",
                                 "salaryMin":150,"salaryMax":250,"salaryUnit":"DAY","source":"BOSS"}
                                """.formatted(companyId)))
                .andExpect(status().isCreated())
                .andReturn();
        long positionId = objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asLong();

        mockMvc.perform(get("/api/v1/positions/" + positionId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("岗位公司" + suffix))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        mockMvc.perform(get("/api/v1/positions?keyword=Java&companyId=" + companyId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("岗位绑定他人公司返回 404（不暴露公司存在）")
    void positionWithForeignCompany() throws Exception {
        long foreignCompanyId = createCompany(tokenA, "A 的公司" + suffix);
        mockMvc.perform(post("/api/v1/positions")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId":%d,"title":"越权岗位","workType":"INTERNSHIP","workplaceType":"ONSITE"}
                                """.formatted(foreignCompanyId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("薪资下限大于上限返回 400")
    void salaryRangeInvalid() throws Exception {
        long companyId = createCompany(tokenA, "薪资公司" + suffix);
        mockMvc.perform(post("/api/v1/positions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId":%d,"title":"薪资异常","workType":"INTERNSHIP","workplaceType":"ONSITE","salaryMin":300,"salaryMax":100}
                                """.formatted(companyId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("岗位链接非法返回 400")
    void sourceUrlInvalid() throws Exception {
        long companyId = createCompany(tokenA, "URL 公司" + suffix);
        mockMvc.perform(post("/api/v1/positions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId":%d,"title":"URL 异常","workType":"INTERNSHIP","workplaceType":"ONSITE","sourceUrl":"not-a-url"}
                                """.formatted(companyId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("同公司相同岗位名和来源重复返回 409 ；不同来源允许")
    void duplicatePosition() throws Exception {
        long companyId = createCompany(tokenA, "重复岗位公司" + suffix);
        String body = """
                {"companyId":%d,"title":"重复岗位","workType":"INTERNSHIP","workplaceType":"ONSITE","source":"BOSS"}
                """.formatted(companyId);
        mockMvc.perform(post("/api/v1/positions").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/positions").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POSITION_DUPLICATE"));

        // 不同来源允许同名岗位
        mockMvc.perform(post("/api/v1/positions").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId":%d,"title":"重复岗位","workType":"INTERNSHIP","workplaceType":"ONSITE","source":"官网"}
                                """.formatted(companyId)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("删除有岗位的公司返回 409；删除岗位后公司可删")
    void deleteCompanyWithPositions() throws Exception {
        long companyId = createCompany(tokenA, "待删公司" + suffix);
        MvcResult pos = mockMvc.perform(post("/api/v1/positions").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId":%d,"title":"占位岗位","workType":"INTERNSHIP","workplaceType":"ONSITE"}
                                """.formatted(companyId)))
                .andExpect(status().isCreated())
                .andReturn();
        long positionId = objectMapper.readTree(pos.getResponse().getContentAsString()).at("/data/id").asLong();

        mockMvc.perform(delete("/api/v1/companies/" + companyId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANY_HAS_RELATIONS"));

        mockMvc.perform(delete("/api/v1/positions/" + positionId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/companies/" + companyId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("删除有投递记录的岗位返回 409（可改为 CLOSED）")
    void deletePositionWithApplications() throws Exception {
        long companyId = createCompany(tokenA, "投递保护公司" + suffix);
        MvcResult pos = mockMvc.perform(post("/api/v1/positions").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId":%d,"title":"投递保护岗位","workType":"INTERNSHIP","workplaceType":"ONSITE"}
                                """.formatted(companyId)))
                .andExpect(status().isCreated())
                .andReturn();
        long positionId = objectMapper.readTree(pos.getResponse().getContentAsString()).at("/data/id").asLong();

        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM jt_user WHERE username = ?", Long.class, "u_a" + suffix);
        jdbcTemplate.update(
                "INSERT INTO jt_job_application (user_id, company_id, position_id, status) VALUES (?, ?, ?, 'APPLIED')",
                userId, companyId, positionId);

        mockMvc.perform(delete("/api/v1/positions/" + positionId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POSITION_HAS_APPLICATIONS"));

        // 可正常改为 CLOSED
        mockMvc.perform(patch("/api/v1/positions/" + positionId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    @DisplayName("排序字段白名单：非法 sortBy 返回 400")
    void sortByWhitelist() throws Exception {
        mockMvc.perform(get("/api/v1/companies?sortBy=deleted--").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
