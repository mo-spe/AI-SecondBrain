package com.secondbrain.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.dto.GraphEdge;
import com.secondbrain.dto.GraphNode;
import com.secondbrain.dto.KnowledgeGraph;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.research.orchestrator.AgentContext;
import com.secondbrain.service.AiService;
import com.secondbrain.service.KnowledgeGraphService;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.service.ResearchMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识缺口分析 Agent.
 *
 * <p>对比用户已有知识与研究目标，发现显性和隐性知识缺口。
 * 显性缺口来自 KnowledgeAgent 的掌握度评估，
 * 隐性缺口通过图谱结构分析和 LLM 知识树对比发现。</p>
 *
 * <p>输出供 ResearchAgent 生成针对性的外部搜索查询。</p>
 *
 * @author AI
 */
@Component
public class GapAgent implements ResearchAgent {

    private static final Logger log = LoggerFactory.getLogger(GapAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 知识库节点数阈值，低于此值跳过图谱分析
     */
    private static final int MIN_NODES_FOR_GRAPH_ANALYSIS = 10;

    private final AiService aiService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final KnowledgeService knowledgeService;
    private final ResearchMemoryService researchMemoryService;

    public GapAgent(AiService aiService,
                    KnowledgeGraphService knowledgeGraphService,
                    KnowledgeService knowledgeService,
                    ResearchMemoryService researchMemoryService) {
        this.aiService = aiService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.knowledgeService = knowledgeService;
        this.researchMemoryService = researchMemoryService;
    }

    @Override
    public String getName() {
        return "GapAgent";
    }

    @Override
    public long getTimeoutMs() {
        return 40_000;
    }

    @Override
    public boolean shouldExecute(AgentContext context) {
        // 知识库节点过少时跳过高阶图谱分析
        try {
            long nodeCount = knowledgeService.countByUserId(context.getUserId(),
                    context.getWorkspaceId());
            if (nodeCount < MIN_NODES_FOR_GRAPH_ANALYSIS) {
                log.info("gap_agent_skipped_low_nodes projectId={} count={}",
                        context.getProjectId(), nodeCount);
                return false;
            }
        } catch (Exception e) {
            log.warn("gap_agent_node_count_failed projectId={}", context.getProjectId(), e);
        }
        return true;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Long projectId = context.getProjectId();
        Long userId = context.getUserId();
        Long workspaceId = context.getWorkspaceId();

        log.info("gap_agent_start projectId={}", projectId);

        try {
            // Step 1: 读取 KnowledgeAgent 输出
            String knowledgeOutput = context.getAgentOutput("KnowledgeAgent");
            Map<String, Object> knowledgeData = knowledgeOutput != null
                    ? objectMapper.readValue(knowledgeOutput, Map.class)
                    : Map.of();

            // Step 2: 分析掌握度 → 显性缺口
            List<Map<String, Object>> explicitGaps = analyzeMastery(knowledgeData);

            // Step 3: 图谱结构分析 → 隐性缺口
            List<Map<String, Object>> implicitGaps = new ArrayList<>();
            try {
                KnowledgeGraph graph = knowledgeGraphService.getGraph(userId, workspaceId);
                implicitGaps = analyzeGraphStructure(context, graph, knowledgeData);
            } catch (Exception e) {
                log.warn("graph_analysis_failed projectId={}", projectId, e);
            }

            // Step 4: 编译缺口列表
            List<Map<String, Object>> allGaps = new ArrayList<>();
            allGaps.addAll(explicitGaps);
            allGaps.addAll(implicitGaps);

            // Step 5: 优先级排序
            allGaps.sort(Comparator.comparingInt(
                    (Map<String, Object> g) -> priorityWeight((String) g.get("priority"))).reversed());

            // Step 6: 统计
            long criticalCount = allGaps.stream()
                    .filter(g -> "CRITICAL".equals(g.get("priority"))).count();
            long highCount = allGaps.stream()
                    .filter(g -> "HIGH".equals(g.get("priority"))).count();

            Map<String, Object> gapAnalysisStats = new LinkedHashMap<>();
            gapAnalysisStats.put("totalGaps", allGaps.size());
            gapAnalysisStats.put("explicitGaps", explicitGaps.size());
            gapAnalysisStats.put("implicitGaps", implicitGaps.size());
            gapAnalysisStats.put("criticalGaps", criticalCount);
            gapAnalysisStats.put("highGaps", highCount);

            // Step 7: 生成建议
            boolean shouldCreateTask = criticalCount > 0 || highCount > 1;
            Map<String, Object> recommendation = new LinkedHashMap<>();
            recommendation.put("shouldCreateNewTask", shouldCreateTask);
            recommendation.put("suggestedFocusAreas",
                    allGaps.stream().limit(5).map(g -> g.get("topic")).toList());

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("gaps", allGaps);
            output.put("gapAnalysisStats", gapAnalysisStats);
            output.put("recommendation", recommendation);

            String outputJson = objectMapper.writeValueAsString(output);

            // 持久化知识缺口记忆，供前端"知识缺口"导航展示
            persistGapMemories(projectId, userId, allGaps);

            log.info("gap_agent_done projectId={} explicit={} implicit={} total={}",
                    projectId, explicitGaps.size(), implicitGaps.size(), allGaps.size());
            return AgentResult.completed(getName(), outputJson);
        } catch (Exception e) {
            log.error("gap_agent_failed projectId={}", projectId, e);
            return AgentResult.failed(getName(), "知识缺口分析失败: " + e.getMessage());
        }
    }

    /**
     * 将知识缺口列表持久化为 KNOWLEDGE_GAP 类型记忆.
     *
     * <p>每个缺口保存为一条独立记忆，key 使用缺口主题。重新执行研究时先清理旧记忆。</p>
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @param gaps      缺口列表
     */
    private void persistGapMemories(Long projectId, Long userId, List<Map<String, Object>> gaps) {
        try {
            researchMemoryService.deleteByProjectAndType(projectId, userId, "KNOWLEDGE_GAP");
            for (int i = 0; i < gaps.size(); i++) {
                Map<String, Object> gap = gaps.get(i);
                String topic = (String) gap.getOrDefault("topic", "缺口-" + (i + 1));
                StringBuilder md = new StringBuilder();
                md.append("### ").append(topic).append("\n\n");
                md.append("- **类别**：").append(gap.getOrDefault("category", "UNKNOWN")).append("\n");
                md.append("- **优先级**：").append(gap.getOrDefault("priority", "MEDIUM")).append("\n");
                md.append("- **发现方式**：").append(gap.getOrDefault("discoveryMethod", "")).append("\n\n");
                Object desc = gap.get("description");
                if (desc != null) {
                    md.append(desc).append("\n");
                }
                String memoryKey = topic.length() > 80 ? topic.substring(0, 80) : topic;
                researchMemoryService.save(projectId, userId, memoryKey, "KNOWLEDGE_GAP", md.toString());
            }
        } catch (Exception e) {
            log.warn("persist_gap_memories_failed projectId={}", projectId, e);
        }
    }

    /**
     * 从 KnowledgeAgent 的掌握度评估中提取显性缺口.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> analyzeMastery(Map<String, Object> knowledgeData) {
        List<Map<String, Object>> gaps = new ArrayList<>();

        try {
            String masteryStr = (String) knowledgeData.get("masteryAssessment");
            if (masteryStr == null || masteryStr.isBlank()) return gaps;

            Map<String, Object> mastery = objectMapper.readValue(masteryStr, Map.class);

            List<String> weakAreas = (List<String>) mastery.get("weakAreas");
            if (weakAreas != null) {
                for (String area : weakAreas) {
                    Map<String, Object> gap = new LinkedHashMap<>();
                    gap.put("category", "EXPLICIT");
                    gap.put("topic", area);
                    gap.put("description", "知识库中该领域掌握度不足");
                    gap.put("priority", "HIGH");
                    gap.put("discoveryMethod", "MASTERY_THRESHOLD");
                    gap.put("relatedNodes", List.of());
                    gaps.add(gap);
                }
            }

            String overallMastery = (String) mastery.get("overallMastery");
            if ("low".equals(overallMastery)) {
                // 整体掌握度低时，标记更多内容为显性缺口
                List<String> strongAreas = (List<String>) mastery.get("strongAreas");
                if (strongAreas == null || strongAreas.isEmpty()) {
                    Map<String, Object> gap = new LinkedHashMap<>();
                    gap.put("category", "EXPLICIT");
                    gap.put("topic", "整体研究主题");
                    gap.put("description", "知识库中对研究主题整体掌握度评估为低");
                    gap.put("priority", "CRITICAL");
                    gap.put("discoveryMethod", "MASTERY_THRESHOLD");
                    gap.put("relatedNodes", List.of());
                    gaps.add(gap);
                }
            }
        } catch (Exception e) {
            log.warn("analyze_mastery_failed", e);
        }

        return gaps;
    }

    /**
     * 通过图谱结构分析发现隐性缺口.
     *
     * <p>方法：从 KnowledgeAgent 搜索到的节点出发，
     * 通过 LLM 生成该领域的"完整知识树"，
     * 与用户实际图谱中的子节点对比，缺失部分即为隐性缺口。</p>
     */
    private List<Map<String, Object>> analyzeGraphStructure(AgentContext context,
                                                             KnowledgeGraph graph,
                                                             Map<String, Object> knowledgeData) {
        List<Map<String, Object>> gaps = new ArrayList<>();

        try {
            // 收集用户图谱中的主要节点
            Set<String> userNodeLabels = new HashSet<>();
            for (GraphNode node : graph.getNodes()) {
                userNodeLabels.add(node.getLabel());
            }

            if (userNodeLabels.isEmpty()) return gaps;

            // 构建图谱邻接关系描述
            StringBuilder graphSummary = new StringBuilder();
            graphSummary.append("用户知识图谱节点（前30个）:\n");
            graph.getNodes().stream().limit(30).forEach(n ->
                    graphSummary.append("  - ").append(n.getLabel())
                            .append(" (类型: ").append(n.getType())
                            .append(", 掌握度: ").append(n.getMasteryLevel()).append("/5)\n"));

            // 通过 LLM 识别缺失的子领域
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content",
                            "研究目标: " + context.getResearchGoal() + "\n\n"
                                    + graphSummary + "\n\n"
                                    + "基于以上用户知识图谱，分析该研究目标下用户可能缺失的子领域或知识点。\n"
                                    + "请用 JSON 数组格式输出（最多 5 个缺口）：\n"
                                    + "[{\"topic\": \"缺失主题\", "
                                    + "\"description\": \"为什么这个缺口很重要\", "
                                    + "\"priority\": \"CRITICAL/HIGH/MEDIUM/LOW\"}]"));

            String response = aiService.chat(context.getUserId(),
                    AiScenario.RESEARCH.getCode(), messages);

            if (response != null && !response.isBlank()) {
                // 提取 JSON 数组
                String jsonArray = extractJsonArray(response);
                if (jsonArray != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> llmGaps = objectMapper.readValue(jsonArray, List.class);
                    for (Map<String, Object> g : llmGaps) {
                        Map<String, Object> gap = new LinkedHashMap<>();
                        gap.put("category", "IMPLICIT");
                        gap.put("topic", g.get("topic"));
                        gap.put("description", g.get("description"));
                        gap.put("priority", g.getOrDefault("priority", "MEDIUM"));
                        gap.put("discoveryMethod", "GRAPH_STRUCTURE_ANALYSIS");
                        gap.put("relatedNodes", List.of());
                        gaps.add(gap);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("graph_structure_analysis_failed projectId={}", context.getProjectId(), e);
        }

        return gaps;
    }

    /**
     * 从 LLM 响应中提取 JSON 数组.
     */
    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return null;
    }

    private int priorityWeight(String priority) {
        return switch (priority) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }
}
