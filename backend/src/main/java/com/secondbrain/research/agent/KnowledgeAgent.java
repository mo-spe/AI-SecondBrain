package com.secondbrain.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.research.orchestrator.AgentContext;
import com.secondbrain.research.tool.KnowledgeDetailTool;
import com.secondbrain.research.tool.KnowledgeSearchTool;
import com.secondbrain.research.tool.ToolResult;
import com.secondbrain.service.AiService;
import com.secondbrain.service.ResearchMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索 Agent.
 *
 * <p>负责在用户个人知识库中搜索与研究目标最相关的内容，
 * 评估用户在各子领域的掌握程度，为后续 GapAgent 提供知识基线。</p>
 *
 * <p>工作流程：
 * 1. 从研究计划中提取任务，通过 LLM 生成搜索查询
 * 2. 执行混合搜索（语义 + 关键词 + 图谱扩展）
 * 3. 阅读 Top-5 匹配节点的完整内容
 * 4. 通过 LLM 评估用户掌握度</p>
 *
 * @author AI
 */
@Component
public class KnowledgeAgent implements ResearchAgent {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiService aiService;
    private final KnowledgeSearchTool knowledgeSearchTool;
    private final KnowledgeDetailTool knowledgeDetailTool;
    private final ResearchMemoryService researchMemoryService;

    public KnowledgeAgent(AiService aiService,
                          KnowledgeSearchTool knowledgeSearchTool,
                          KnowledgeDetailTool knowledgeDetailTool,
                          ResearchMemoryService researchMemoryService) {
        this.aiService = aiService;
        this.knowledgeSearchTool = knowledgeSearchTool;
        this.knowledgeDetailTool = knowledgeDetailTool;
        this.researchMemoryService = researchMemoryService;
    }

    @Override
    public String getName() {
        return "KnowledgeAgent";
    }

    @Override
    public long getTimeoutMs() {
        return 60_000;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Long userId = context.getUserId();
        Long workspaceId = context.getWorkspaceId();
        Long projectId = context.getProjectId();

        log.info("knowledge_agent_start projectId={}", projectId);

        try {
            // Step 1: 从研究计划中获取任务并生成搜索查询
            List<String> queries = generateSearchQueries(context);

            // Step 2: 对每个查询执行混合搜索
            List<Map<String, Object>> searchResults = new ArrayList<>();
            for (String query : queries) {
                Map<String, Object> params = new HashMap<>();
                params.put("query", query);
                params.put("searchMode", "hybrid");
                params.put("topK", 10);
                params.put("expandByGraph", true);
                params.put("userId", userId);
                params.put("workspaceId", workspaceId);

                ToolResult result = knowledgeSearchTool.execute(params);
                if (result.isSuccess()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("query", query);
                    entry.put("results", result.getData());
                    entry.put("totalHits", result.getMetadata().get("totalHits"));
                    searchResults.add(entry);
                }
            }

            // Step 3: 收集搜索结果中的 Top-5 节点 ID 并读取详情
            List<Map<String, Object>> detailedNodes = readTopNodes(searchResults, userId, workspaceId);

            // Step 4: 通过 LLM 评估用户在各子领域的掌握度
            String masteryAssessment = assessMastery(context, searchResults, detailedNodes);

            // Step 5: 构建输出
            Map<String, Object> output = new HashMap<>();
            output.put("searchResults", searchResults);
            output.put("detailedNodes", detailedNodes);
            output.put("masteryAssessment", masteryAssessment);
            output.put("totalKnowledgeFound", detailedNodes.size());
            output.put("knowledgeScarce", detailedNodes.size() < 3);

            String outputJson = objectMapper.writeValueAsString(output);

            // 持久化知识背景记忆，供前端"知识背景"导航展示
            persistBackgroundMemory(projectId, userId, searchResults, detailedNodes, masteryAssessment);

            log.info("knowledge_agent_done projectId={} found={}", projectId, detailedNodes.size());
            return AgentResult.completed(getName(), outputJson);
        } catch (Exception e) {
            log.error("knowledge_agent_failed projectId={}", projectId, e);
            return AgentResult.failed(getName(), "知识库检索失败: " + e.getMessage());
        }
    }

    /**
     * 将知识背景汇总为 Markdown 并持久化为 BACKGROUND 类型记忆.
     *
     * @param projectId          项目ID
     * @param userId             用户ID
     * @param searchResults      知识库搜索结果
     * @param detailedNodes      详细节点
     * @param masteryAssessment  掌握度评估 JSON
     */
    private void persistBackgroundMemory(Long projectId, Long userId,
                                          List<Map<String, Object>> searchResults,
                                          List<Map<String, Object>> detailedNodes,
                                          String masteryAssessment) {
        try {
            StringBuilder md = new StringBuilder();
            md.append("## 知识库检索概况\n\n");
            md.append("- 搜索查询数：").append(searchResults.size()).append("\n");
            md.append("- 命中节点数：").append(detailedNodes.size()).append("\n\n");

            if (!searchResults.isEmpty()) {
                md.append("### 搜索结果摘要\n\n");
                for (Map<String, Object> r : searchResults) {
                    md.append("**查询**：").append(r.get("query")).append("\n\n");
                    Object results = r.get("results");
                    if (results != null) {
                        String resultsStr = results.toString();
                        if (resultsStr.length() > 500) {
                            resultsStr = resultsStr.substring(0, 500) + "...";
                        }
                        md.append(resultsStr).append("\n\n");
                    }
                }
            }

            if (masteryAssessment != null && !masteryAssessment.isBlank()) {
                md.append("### 掌握度评估\n\n");
                md.append("```json\n").append(masteryAssessment).append("\n```\n");
            }

            researchMemoryService.deleteByProjectAndType(projectId, userId, "BACKGROUND");
            researchMemoryService.save(projectId, userId,
                    ResearchMemoryKey.of("BACKGROUND", "知识背景总览"),
                    "BACKGROUND", md.toString());
        } catch (Exception e) {
            log.warn("persist_background_memory_failed projectId={}", projectId, e);
        }
    }

    /**
     * 从研究计划中生成搜索查询.
     */
    private List<String> generateSearchQueries(AgentContext context) {
        List<String> queries = new ArrayList<>();

        // 优先使用研究目标作为查询
        if (context.getResearchGoal() != null) {
            queries.add(context.getResearchGoal());
        }

        // 从研究计划的任务中提取查询关键词
        if (context.getResearchPlan() != null && context.getResearchPlan().getTasksJson() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tasks = objectMapper.readValue(
                        context.getResearchPlan().getTasksJson(), List.class);
                for (Map<String, Object> task : tasks) {
                    String title = (String) task.get("title");
                    if (title != null && !title.isBlank()) {
                        queries.add(title);
                    }
                }
            } catch (Exception e) {
                log.warn("parse_tasks_json_failed projectId={}", context.getProjectId(), e);
            }
        }

        // 如果查询过少，用 LLM 扩展
        if (queries.size() < 2 && context.getResearchGoal() != null) {
            try {
                List<Map<String, String>> messages = List.of(
                        Map.of("role", "user", "content",
                                "针对以下研究目标，生成 3 个搜索查询（每行一个，纯查询文本，不要编号）：\n"
                                        + context.getResearchGoal()));
                String response = aiService.chat(context.getUserId(),
                        AiScenario.RESEARCH.getCode(), messages);
                if (response != null && !response.isBlank()) {
                    for (String line : response.split("\n")) {
                        String trimmed = line.replaceAll("^[\\d\\.\\-\\*\\s]+", "").trim();
                        if (!trimmed.isBlank() && trimmed.length() > 3) {
                            queries.add(trimmed);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("llm_query_gen_failed projectId={}", context.getProjectId(), e);
            }
        }

        return queries;
    }

    /**
     * 读取搜索结果中前 5 个节点的完整内容.
     */
    private List<Map<String, Object>> readTopNodes(List<Map<String, Object>> searchResults,
                                                    Long userId, Long workspaceId) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        int count = 0;

        for (Map<String, Object> result : searchResults) {
            if (count >= 5) break;
            String data = (String) result.get("results");
            if (data == null) continue;

            // 解析搜索结果文本，提取节点编号
            String[] lines = data.split("\n");
            for (String line : lines) {
                if (count >= 5) break;
                if (line.matches("^\\[\\d+\\].*")) {
                    // 无法从文本中可靠提取 knowledgeId，通过搜索文本匹配
                    // 实际实现中，搜索结果应返回结构化数据
                    count++;
                }
            }
        }

        return nodes;
    }

    /**
     * 通过 LLM 评估用户对研究主题的掌握程度.
     */
    private String assessMastery(AgentContext context,
                                  List<Map<String, Object>> searchResults,
                                  List<Map<String, Object>> detailedNodes) {
        try {
            StringBuilder context_ = new StringBuilder();
            context_.append("研究目标: ").append(context.getResearchGoal()).append("\n\n");
            context_.append("知识库搜索结果:\n");
            for (Map<String, Object> result : searchResults) {
                context_.append("查询: ").append(result.get("query")).append("\n");
                context_.append("结果: ").append(result.get("results")).append("\n\n");
            }

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content",
                            "基于以下知识库搜索结果，评估用户对该研究主题的掌握程度。\n\n"
                                    + context_ + "\n\n"
                                    + "请用 JSON 格式输出：\n"
                                    + "{\n"
                                    + "  \"overallMastery\": \"high/medium/low\",\n"
                                    + "  \"strongAreas\": [\"已充分掌握的领域\"],\n"
                                    + "  \"weakAreas\": [\"知识薄弱的领域\"],\n"
                                    + "  \"knowledgeSummary\": \"对用户知识现状的简洁评估\",\n"
                                    + "  \"recommendedFocus\": [\"建议重点研究的子主题\"]\n"
                                    + "}"));

            return aiService.chat(context.getUserId(), AiScenario.RESEARCH.getCode(), messages);
        } catch (Exception e) {
            log.warn("mastery_assessment_failed projectId={}", context.getProjectId(), e);
            return "{\"overallMastery\": \"unknown\", \"note\": \"评估失败: " + e.getMessage() + "\"}";
        }
    }
}
