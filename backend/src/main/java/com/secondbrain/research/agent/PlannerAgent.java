package com.secondbrain.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.research.orchestrator.AgentContext;
import com.secondbrain.service.AiService;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.service.ResearchPlanService;
import com.secondbrain.service.ResearchTaskService;
import com.secondbrain.vo.ResearchPlanVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 研究规划 Agent.
 *
 * <p>作为研究流程的第一个 Agent，负责分析研究目标、评估复杂度、
 * 确定 Agent 执行链路、拆解研究任务。
 * 纯 LLM 推理 Agent，不调用外部搜索工具。</p>
 *
 * <p>复杂度决策：
 * - SIMPLE：知识库已有足够内容，仅需 KnowledgeAgent + SynthesizerAgent
 * - STANDARD：需要外部搜索，走完整六 Agent 链路
 * - DEEP：复杂主题需迭代验证，GapAgent→ResearchAgent→CriticAgent 循环最多 3 次</p>
 *
 * @author AI
 */
@Component
public class PlannerAgent implements ResearchAgent {

    private static final Logger log = LoggerFactory.getLogger(PlannerAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiService aiService;
    private final KnowledgeService knowledgeService;
    private final ResearchPlanService researchPlanService;
    private final ResearchTaskService researchTaskService;

    public PlannerAgent(AiService aiService,
                        KnowledgeService knowledgeService,
                        ResearchPlanService researchPlanService,
                        ResearchTaskService researchTaskService) {
        this.aiService = aiService;
        this.knowledgeService = knowledgeService;
        this.researchPlanService = researchPlanService;
        this.researchTaskService = researchTaskService;
    }

    @Override
    public String getName() {
        return "PlannerAgent";
    }

    @Override
    public long getTimeoutMs() {
        return 60_000;
    }

    @Override
    public int getMaxRetries() {
        return 1;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Long projectId = context.getProjectId();
        Long userId = context.getUserId();
        Long workspaceId = context.getWorkspaceId();

        log.info("planner_agent_start projectId={}", projectId);

        try {
            // Step 1: 收集用户知识库统计信息
            Map<String, Object> userContext = buildUserContext(userId, workspaceId);

            // Step 2: 构建 LLM 提示词并生成研究计划
            String planJson = generatePlan(context.getResearchGoal(), userContext, userId);

            // Step 3: 解析并验证 LLM 输出
            Map<String, Object> plan = parseAndValidatePlan(planJson);

            // Step 4: 持久化研究计划
            String complexity = (String) plan.getOrDefault("complexity", "STANDARD");
            String rationale = (String) plan.getOrDefault("rationale", "");

            @SuppressWarnings("unchecked")
            List<String> agentChainList = (List<String>) plan.get("agentChain");
            String agentChain = agentChainList != null
                    ? String.join(",", agentChainList) : "KnowledgeAgent,SynthesizerAgent";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) plan.get("tasks");
            String tasksJson = objectMapper.writeValueAsString(tasks != null ? tasks : List.of());

            Integer estimatedTokens = plan.get("estimatedTokens") instanceof Number
                    ? ((Number) plan.get("estimatedTokens")).intValue() : null;

            ResearchPlanVO planVO = researchPlanService.createPlan(projectId, complexity,
                    agentChain, tasksJson, rationale, estimatedTokens,
                    "PLANNER_AGENT", userId);

            context.setResearchPlan(planVO);

            // Step 5: 批量创建研究任务
            if (tasks != null && !tasks.isEmpty()) {
                List<com.secondbrain.dto.CreateResearchTaskRequest> taskRequests = new ArrayList<>();
                for (Map<String, Object> task : tasks) {
                    com.secondbrain.dto.CreateResearchTaskRequest req =
                            new com.secondbrain.dto.CreateResearchTaskRequest();
                    req.setTitle((String) task.get("title"));
                    req.setDescription((String) task.get("description"));
                    req.setQuestion((String) task.get("question"));
                    req.setRequiresExternalSearch(
                            Boolean.TRUE.equals(task.get("requiresExternalSearch")));
                    // dependsOn 在 batchCreate 中处理（基于索引映射）
                    taskRequests.add(req);
                }
                if (!taskRequests.isEmpty()) {
                    researchTaskService.batchCreate(projectId, taskRequests, userId);
                }
            }

            // Step 6: 构建输出
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("complexity", complexity);
            output.put("agentChain", agentChainList);
            output.put("rationale", rationale);
            output.put("tasks", tasks);
            output.put("estimatedTokens", estimatedTokens);
            output.put("planId", planVO.getId());
            output.put("planVersion", planVO.getVersion());

            String outputJson = objectMapper.writeValueAsString(output);
            context.putAgentOutput(getName(), outputJson);

            log.info("planner_agent_done projectId={} complexity={} tasks={}",
                    projectId, complexity, tasks != null ? tasks.size() : 0);
            return AgentResult.completed(getName(), outputJson);
        } catch (Exception e) {
            log.error("planner_agent_failed projectId={}", projectId, e);

            // 降级为 SIMPLE 流程
            try {
                degradeToSimple(context);
                return AgentResult.completed(getName(),
                        "{\"complexity\":\"SIMPLE\",\"rationale\":\"Planner 降级：LLM 调用失败，采用简单流程。\"}");
            } catch (Exception de) {
                return AgentResult.failed(getName(), "规划失败: " + e.getMessage());
            }
        }
    }

    /**
     * 构建用户知识库统计信息.
     */
    private Map<String, Object> buildUserContext(Long userId, Long workspaceId) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        try {
            long totalNodes = knowledgeService.countByUserId(userId, workspaceId);
            ctx.put("totalNodes", totalNodes);
        } catch (Exception e) {
            ctx.put("totalNodes", 0);
        }
        return ctx;
    }

    /**
     * 通过 LLM 生成研究计划.
     */
    private String generatePlan(String researchGoal, Map<String, Object> userContext, Long userId) {
        String systemPrompt = """
                你是一个 AI 研究规划专家。你的任务是根据用户的研究目标，制定结构化的研究计划。

                你必须以 JSON 格式输出，包含以下字段：
                - complexity: "SIMPLE" | "STANDARD" | "DEEP"
                - rationale: 复杂度评估的推理过程（中文，200字以内）
                - agentChain: Agent 执行链路数组
                - estimatedTokens: 预估总 token 消耗
                - tasks: 研究任务列表，每个任务包含：
                  - title: 任务标题
                  - description: 详细描述
                  - question: 核心研究问题
                  - requiresExternalSearch: 是否需要外部搜索
                  - dependsOn: 前置任务序号（null 表示无依赖，数字表示依赖第几个任务）
                  - priority: "HIGH" | "MEDIUM" | "LOW"

                复杂度决策规则：
                - SIMPLE：用户知识库已有充分相关内容（相关节点 >= 10，平均掌握度 >= 3）
                  agentChain: ["KnowledgeAgent", "SynthesizerAgent"]
                - STANDARD：用户有一定基础但需要外部信息补充
                  agentChain: ["KnowledgeAgent", "GapAgent", "ResearchAgent", "CriticAgent", "SynthesizerAgent", "KnowledgeWriterAgent"]
                - DEEP：复杂主题，需要多轮迭代验证
                  agentChain: 同 STANDARD（Orchestrator 会自动循环）

                仅输出 JSON，不要有任何额外文本。""";

        String userPrompt = "研究目标: " + researchGoal + "\n\n"
                + "用户知识库概况: " + userContext + "\n\n"
                + "请生成研究计划 JSON。";

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt));

        return aiService.chat(userId, AiScenario.RESEARCH.getCode(), messages);
    }

    /**
     * 解析并验证 LLM 输出的 JSON.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAndValidatePlan(String planJson) throws Exception {
        // 清理可能的 markdown 代码块包装
        String json = planJson.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf('\n');
            int end = json.lastIndexOf("```");
            if (start > 0 && end > start) {
                json = json.substring(start + 1, end).trim();
            }
        }

        Map<String, Object> plan = objectMapper.readValue(json, Map.class);

        // 验证必填字段
        if (!plan.containsKey("complexity")) {
            plan.put("complexity", "STANDARD");
        }
        if (!plan.containsKey("agentChain")) {
            plan.put("agentChain", List.of("KnowledgeAgent", "SynthesizerAgent"));
        }
        if (!plan.containsKey("tasks")) {
            plan.put("tasks", List.of());
        }

        return plan;
    }

    /**
     * 降级为 SIMPLE 流程.
     */
    private void degradeToSimple(AgentContext context) throws Exception {
        String agentChain = "KnowledgeAgent,SynthesizerAgent";
        String tasksJson = "[{\"title\":\"研究\",\"description\":\"自动生成的研究任务\","
                + "\"question\":\"" + context.getResearchGoal() + "\","
                + "\"requiresExternalSearch\":false,\"dependsOn\":null,\"priority\":\"HIGH\"}]";

        ResearchPlanVO planVO = researchPlanService.createPlan(
                context.getProjectId(), "SIMPLE", agentChain, tasksJson,
                "Planner 降级：LLM 调用失败，采用简单流程。",
                null, "PLANNER_AGENT", context.getUserId());

        context.setResearchPlan(planVO);
        context.putAgentOutput(getName(), "{\"complexity\":\"SIMPLE\",\"degraded\":true}");
    }
}
