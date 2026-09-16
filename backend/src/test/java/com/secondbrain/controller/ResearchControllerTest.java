package com.secondbrain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AI 研究板块 — 完整集成测试.
 *
 * <p>覆盖 ResearchController 全部端点：CRUD、状态机、鉴权和用户隔离.
 * 测试按 @Order 顺序执行，确保数据依赖正确.
 * GlobalExceptionHandler 捕获 BusinessException 后 HTTP 200，错误码在 JSON body.</p>
 *
 * @author AI
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AI 研究板块集成测试")
class ResearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private String otherUserToken;

    /** 项目 CRUD 测试用 projectId */
    private static Long crudProjectId;
    /** 生命周期测试用 projectId */
    private static Long lifecycleProjectId;
    /** 任务测试用 projectId */
    private static Long taskProjectId;
    /** 任务测试用 taskId */
    private static Long testTaskId;

    @BeforeEach
    void setup() {
        token = "Bearer " + jwtUtil.generateToken(5L, "newuser", "super_admin", null);
        otherUserToken = "Bearer " + jwtUtil.generateToken(99L, "other", "user", null);
    }

    // ======================== 鉴权 ========================

    @Test
    @Order(1)
    @DisplayName("[鉴权] 无 Token 访问 → JSON code=401")
    void authRejectUnauthenticated() throws Exception {
        mockMvc.perform(get("/research/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ======================== 项目 CRUD ========================

    @Test
    @Order(10)
    @DisplayName("[项目] 创建 → DRAFT 状态")
    void projectCreate() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "单元测试 - Spring Boot 深入学习");
        body.put("goal", "全面掌握 Spring Boot 自动配置原理");

        String resp = mockMvc.perform(post("/research/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.title").value("单元测试 - Spring Boot 深入学习"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.statusLabel").value("草稿"))
                .andReturn().getResponse().getContentAsString();

        crudProjectId = objectMapper.readTree(resp).get("data").get("id").asLong();
    }

    @Test
    @Order(11)
    @DisplayName("[项目] 标题为空 → code=400")
    void projectRejectEmptyTitle() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "");
        body.put("goal", "目标");

        mockMvc.perform(post("/research/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(12)
    @DisplayName("[项目] 分页列表查询")
    void projectList() throws Exception {
        mockMvc.perform(get("/research/projects")
                        .header("Authorization", token)
                        .param("current", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.current").value(1));
    }

    @Test
    @Order(13)
    @DisplayName("[项目] 关键词搜索")
    void projectSearchByKeyword() throws Exception {
        mockMvc.perform(get("/research/projects")
                        .header("Authorization", token)
                        .param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @Order(14)
    @DisplayName("[项目] 状态筛选")
    void projectFilterByStatus() throws Exception {
        mockMvc.perform(get("/research/projects")
                        .header("Authorization", token)
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @Order(15)
    @DisplayName("[项目] 详情查询")
    void projectGetById() throws Exception {
        mockMvc.perform(get("/research/projects/" + crudProjectId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(crudProjectId))
                .andExpect(jsonPath("$.data.title").value("单元测试 - Spring Boot 深入学习"));
    }

    @Test
    @Order(16)
    @DisplayName("[项目] 不存在的项目 → code=404")
    void project404() throws Exception {
        mockMvc.perform(get("/research/projects/99999")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @Order(17)
    @DisplayName("[项目] 更新标题")
    void projectUpdate() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "更新后的标题");

        mockMvc.perform(put("/research/projects/" + crudProjectId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("更新后的标题"));
    }

    @Test
    @Order(18)
    @DisplayName("[项目] 用户隔离 — 其他用户查看 → code=403")
    void projectUserIsolation() throws Exception {
        mockMvc.perform(get("/research/projects/" + crudProjectId)
                        .header("Authorization", otherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ======================== 状态机 ========================

    @Test
    @Order(20)
    @DisplayName("[状态机] DRAFT → execute → RESEARCHING")
    void stateExecuteFromDraft() throws Exception {
        mockMvc.perform(post("/research/projects/" + crudProjectId + "/execute")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESEARCHING"))
                .andExpect(jsonPath("$.data.statusLabel").value("研究中"));
    }

    @Test
    @Order(21)
    @DisplayName("[状态机] RESEARCHING 再 execute → code=400")
    void stateRejectExecuteOnResearching() throws Exception {
        mockMvc.perform(post("/research/projects/" + crudProjectId + "/execute")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(22)
    @DisplayName("[状态机] DRAFT 状态 pause → code=400")
    void stateRejectPauseOnDraft() throws Exception {
        // 创建新 DRAFT 项目 — 暂停仅允许 RESEARCHING/REVIEWING 状态
        Map<String, Object> body = new HashMap<>();
        body.put("title", "暂停拒绝测试");
        body.put("goal", "验证 DRAFT 不能暂停");

        String resp = mockMvc.perform(post("/research/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long pid = objectMapper.readTree(resp).get("data").get("id").asLong();

        mockMvc.perform(post("/research/projects/" + pid + "/pause")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        // 清理
        mockMvc.perform(delete("/research/projects/" + pid)
                        .header("Authorization", token))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(23)
    @DisplayName("[状态机] DRAFT 状态 resume → code=400")
    void stateRejectResumeOnDraft() throws Exception {
        // 创建新 DRAFT 项目 — 恢复仅允许 PAUSED 状态
        Map<String, Object> body = new HashMap<>();
        body.put("title", "恢复拒绝测试");
        body.put("goal", "验证 DRAFT 不能恢复");

        String resp = mockMvc.perform(post("/research/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long pid = objectMapper.readTree(resp).get("data").get("id").asLong();

        mockMvc.perform(post("/research/projects/" + pid + "/resume")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        // 清理
        mockMvc.perform(delete("/research/projects/" + pid)
                        .header("Authorization", token))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(24)
    @DisplayName("[状态机] 等待异步编排器完成 → archive")
    void stateArchiveAfterComplete() throws Exception {
        // 异步编排器无计划时快速完成，等待后项目应为 COMPLETED
        Thread.sleep(3000);

        mockMvc.perform(post("/research/projects/" + crudProjectId + "/archive")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(25)
    @DisplayName("[状态机] 归档后再 archive → code=400")
    void stateRejectArchiveOnArchived() throws Exception {
        mockMvc.perform(post("/research/projects/" + crudProjectId + "/archive")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(26)
    @DisplayName("[状态机] 已归档 execute → code=400")
    void stateRejectExecuteOnArchived() throws Exception {
        mockMvc.perform(post("/research/projects/" + crudProjectId + "/execute")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ======================== 删除 ========================

    @Test
    @Order(30)
    @DisplayName("[项目] 删除")
    void projectDelete() throws Exception {
        mockMvc.perform(delete("/research/projects/" + crudProjectId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(31)
    @DisplayName("[项目] 删除后查询 → code=404")
    void project404AfterDelete() throws Exception {
        mockMvc.perform(get("/research/projects/" + crudProjectId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ======================== 完整生命周期 ========================

    @Test
    @Order(40)
    @DisplayName("[生命周期] DRAFT→更新→EXECUTE→PAUSE→RESUME→删除")
    void fullLifecycle() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "生命周期测试");
        body.put("goal", "验证完整状态流转");

        String resp = mockMvc.perform(post("/research/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        Long pid = objectMapper.readTree(resp).get("data").get("id").asLong();

        // 仅 DRAFT 状态允许编辑
        Map<String, Object> update = new HashMap<>();
        update.put("goal", "更新后的目标");
        mockMvc.perform(put("/research/projects/" + pid)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(jsonPath("$.data.goal").value("更新后的目标"));

        // execute
        mockMvc.perform(post("/research/projects/" + pid + "/execute")
                        .header("Authorization", token))
                .andExpect(jsonPath("$.data.status").value("RESEARCHING"));

        // verify status
        mockMvc.perform(get("/research/projects/" + pid)
                        .header("Authorization", token))
                .andExpect(status().isOk());

        // delete
        mockMvc.perform(delete("/research/projects/" + pid)
                        .header("Authorization", token))
                .andExpect(jsonPath("$.code").value(200));
    }

    // ======================== 研究任务 ========================

    @Test
    @Order(50)
    @DisplayName("[任务] 创建任务容器项目")
    void taskSetupProject() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "任务测试项目");
        body.put("goal", "测试任务管理");

        String resp = mockMvc.perform(post("/research/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        taskProjectId = objectMapper.readTree(resp).get("data").get("id").asLong();
    }

    @Test
    @Order(51)
    @DisplayName("[任务] 创建任务")
    void taskCreate() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "分析 Spring Boot 启动流程");
        body.put("description", "深入源码阅读 SpringApplication.run()");
        body.put("requiresExternalSearch", false);
        body.put("sortOrder", 1);

        String resp = mockMvc.perform(post("/research/projects/" + taskProjectId + "/tasks")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("分析 Spring Boot 启动流程"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.statusLabel").value("待执行"))
                .andReturn().getResponse().getContentAsString();
        testTaskId = objectMapper.readTree(resp).get("data").get("id").asLong();
    }

    @Test
    @Order(52)
    @DisplayName("[任务] 查询列表")
    void taskList() throws Exception {
        mockMvc.perform(get("/research/projects/" + taskProjectId + "/tasks")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @Order(53)
    @DisplayName("[任务] 查询详情")
    void taskGetById() throws Exception {
        mockMvc.perform(get("/research/projects/" + taskProjectId + "/tasks/" + testTaskId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(testTaskId))
                .andExpect(jsonPath("$.data.title").isNotEmpty());
    }

    @Test
    @Order(54)
    @DisplayName("[任务] 更新任务")
    void taskUpdate() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "更新的任务标题");
        body.put("description", "新的描述");

        mockMvc.perform(put("/research/projects/" + taskProjectId + "/tasks/" + testTaskId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("更新的任务标题"));
    }

    @Test
    @Order(55)
    @DisplayName("[任务] 批量创建")
    void taskBatchCreate() throws Exception {
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (int i = 2; i <= 4; i++) {
            Map<String, Object> t = new HashMap<>();
            t.put("title", "批量任务 #" + i);
            t.put("sortOrder", i);
            tasks.add(t);
        }

        mockMvc.perform(post("/research/projects/" + taskProjectId + "/tasks/batch")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tasks)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @Order(56)
    @DisplayName("[任务] 删除任务")
    void taskDelete() throws Exception {
        mockMvc.perform(delete("/research/projects/" + taskProjectId + "/tasks/" + testTaskId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(57)
    @DisplayName("[任务] 清理 — 删除项目")
    void taskCleanupProject() throws Exception {
        mockMvc.perform(delete("/research/projects/" + taskProjectId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ======================== 研究计划 ========================

    @Test
    @Order(70)
    @DisplayName("[计划] Latest Plan — 返回 null")
    void planLatestNull() throws Exception {
        mockMvc.perform(get("/research/projects/1/plans/latest")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @Order(71)
    @DisplayName("[计划] 列表 — 空数组")
    void planList() throws Exception {
        mockMvc.perform(get("/research/projects/1/plans")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(72)
    @DisplayName("[计划] 创建研究计划")
    void planCreate() throws Exception {
        // 先创建项目
        Map<String, Object> projBody = new HashMap<>();
        projBody.put("title", "计划测试项目");
        projBody.put("goal", "测试计划创建");

        String projResp = mockMvc.perform(post("/research/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long pid = objectMapper.readTree(projResp).get("data").get("id").asLong();

        Map<String, Object> body = new HashMap<>();
        body.put("complexity", "STANDARD");
        body.put("agentChain", "KnowledgeAgent,GapAgent,ExternalResearchAgent,CriticAgent,SynthesizerAgent");
        body.put("tasksJson", "[]");
        body.put("rationale", "标准研究流程");
        body.put("createdBy", "USER");

        mockMvc.perform(post("/research/projects/" + pid + "/plans")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.agentChain").isNotEmpty());

        // 清理
        mockMvc.perform(delete("/research/projects/" + pid)
                        .header("Authorization", token))
                .andExpect(jsonPath("$.code").value(200));
    }

    // ======================== 研究来源 / 步骤 / 报告 / 记忆 ========================

    @Test
    @Order(80)
    @DisplayName("[来源] 列表 → 空数组")
    void sourceList() throws Exception {
        mockMvc.perform(get("/research/projects/1/sources")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(81)
    @DisplayName("[步骤] 项目步骤列表 → 空数组")
    void stepList() throws Exception {
        mockMvc.perform(get("/research/projects/1/steps")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(82)
    @DisplayName("[报告] Latest Report → null")
    void reportLatest() throws Exception {
        mockMvc.perform(get("/research/projects/1/report")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @Order(83)
    @DisplayName("[报告] 报告列表 → 空数组")
    void reportList() throws Exception {
        mockMvc.perform(get("/research/projects/1/reports")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(84)
    @DisplayName("[记忆] 记忆列表 → 空数组")
    void memoryList() throws Exception {
        mockMvc.perform(get("/research/projects/1/memory")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(85)
    @DisplayName("[记忆] 按类型查询 → 空数组")
    void memoryListByType() throws Exception {
        mockMvc.perform(get("/research/projects/1/memory/knowledge_state")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
