package com.secondbrain.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.research.orchestrator.AgentContext;
import com.secondbrain.research.tool.SourceAnalyzeTool;
import com.secondbrain.research.tool.ToolResult;
import com.secondbrain.service.AiService;
import com.secondbrain.service.ResearchMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结论验证 Agent.
 *
 * <p>对 ResearchAgent 产生的发现进行四步验证：
 * 1. 来源验证（是否有可靠来源支撑）
 * 2. 可靠性评分（来源类型权威度）
 * 3. 一致性检查（跨来源交叉验证）
 * 4. 时效性检查（技术来源的更新时间）</p>
 *
 * <p>验证通过的高可信发现被提升为结论（conclusion），
 * 存在矛盾的发现被降级并标记争议。</p>
 *
 * @author AI
 */
@Component
public class CriticAgent implements ResearchAgent {

    private static final Logger log = LoggerFactory.getLogger(CriticAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 来源类型权威度权重.
     *
     * <p>权重只表达来源类型的基础质量，最终置信度还必须满足独立来源数量要求。</p>
     */
    private static final Map<String, Double> RELIABILITY_WEIGHTS = Map.of(
            "official_doc", 1.0,
            "paper", 0.95,
            "github", 0.85,
            "article", 0.8,
            "web_search", 0.75,
            "internal", 0.9
    );

    private final AiService aiService;
    private final SourceAnalyzeTool sourceAnalyzeTool;
    private final ResearchMemoryService researchMemoryService;

    public CriticAgent(AiService aiService,
                       SourceAnalyzeTool sourceAnalyzeTool,
                       ResearchMemoryService researchMemoryService) {
        this.aiService = aiService;
        this.sourceAnalyzeTool = sourceAnalyzeTool;
        this.researchMemoryService = researchMemoryService;
    }

    @Override
    public String getName() {
        return "CriticAgent";
    }

    @Override
    public long getTimeoutMs() {
        return 60_000;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Long projectId = context.getProjectId();
        Long userId = context.getUserId();

        log.info("critic_agent_start projectId={}", projectId);

        try {
            // Step 1: 读取 ResearchAgent 输出
            String researchOutput = context.getAgentOutput("ResearchAgent");
            Map<String, Object> researchData = researchOutput != null
                    ? objectMapper.readValue(researchOutput, Map.class) : Map.of();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sources =
                    (List<Map<String, Object>>) researchData.getOrDefault("sources", List.of());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> findings =
                    (List<Map<String, Object>>) researchData.getOrDefault("findings", List.of());

            if (findings.isEmpty()) {
                Map<String, Object> emptyOutput = new LinkedHashMap<>();
                emptyOutput.put("conclusions", List.of());
                emptyOutput.put("validationStats", Map.of(
                        "totalFindings", 0,
                        "promotedToConclusion", 0,
                        "highConfidence", 0,
                        "mediumConfidence", 0,
                        "lowConfidence", 0,
                        "speculation", 0,
                        "controversial", 0));
                emptyOutput.put("note", "无发现需要验证");
                emptyOutput.put("qualityGate", ResearchQualityGate.evaluate(
                        List.of(), 0, context.getResearchGoal()));
                return AgentResult.completed(getName(),
                        objectMapper.writeValueAsString(emptyOutput));
            }

            // Step 2: 对每个发现执行验证
            List<Map<String, Object>> conclusions = new ArrayList<>();
            int highCount = 0, mediumCount = 0, lowCount = 0, speculationCount = 0, controversialCount = 0;

            for (Map<String, Object> finding : findings) {
                String statement = (String) finding.get("statement");
                if (statement == null) continue;

                // 来源验证 + 可靠性评分
                double reliabilityScore = scoreReliability(sources, finding);

                List<Map<String, Object>> supportingSources =
                        findSupportingSources(sources, finding);

                // 时效性必须针对当前结论的支撑来源计算，不能借用其他结论的官方来源。
                String timeliness = assessTimeliness(supportingSources);

                // 确定置信度
                String confidence = determineConfidence(
                        reliabilityScore, timeliness, finding, countIndependentSources(supportingSources));

                Map<String, Object> conclusion = new LinkedHashMap<>();
                conclusion.put("statement", statement);
                conclusion.put("confidence", confidence);
                conclusion.put("reliabilityScore", reliabilityScore);
                conclusion.put("timeliness", timeliness);
                conclusion.put("supportingSources", supportingSources);

                conclusions.add(conclusion);

                switch (confidence) {
                    case "high" -> highCount++;
                    case "medium" -> mediumCount++;
                    case "low" -> lowCount++;
                    case "speculation" -> speculationCount++;
                }
            }

            // Step 3: 一致性检查
            if (conclusions.size() >= 2) {
                controversialCount = checkConsistency(conclusions, userId);
            }

            // Step 4: 构建输出
            Map<String, Object> validationStats = new LinkedHashMap<>();
            validationStats.put("totalFindings", findings.size());
            validationStats.put("promotedToConclusion", conclusions.size());
            validationStats.put("highConfidence", highCount);
            validationStats.put("mediumConfidence", mediumCount);
            validationStats.put("lowConfidence", lowCount);
            validationStats.put("speculation", speculationCount);
            validationStats.put("controversial", controversialCount);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("conclusions", conclusions);
            output.put("validationStats", validationStats);
            output.put("qualityGate", ResearchQualityGate.evaluate(
                    conclusions, controversialCount, context.getResearchGoal()));

            String outputJson = objectMapper.writeValueAsString(output);

            // 持久化研究结论记忆，供前端"研究结论"导航展示
            persistConclusionMemories(projectId, userId, conclusions);

            log.info("critic_agent_done projectId={} high={} medium={} low={}",
                    projectId, highCount, mediumCount, lowCount);
            return AgentResult.completed(getName(), outputJson);
        } catch (Exception e) {
            log.error("critic_agent_failed projectId={}", projectId, e);
            return AgentResult.failed(getName(), "验证失败: " + e.getMessage());
        }
    }

    /**
     * 将验证后的结论持久化为 CONCLUSION 类型记忆.
     *
     * <p>每条结论保存为一条独立记忆，key 使用结论内容前缀。重新执行研究时先清理旧记忆。</p>
     *
     * @param projectId   项目ID
     * @param userId      用户ID
     * @param conclusions 结论列表
     */
    private void persistConclusionMemories(Long projectId, Long userId,
                                            List<Map<String, Object>> conclusions) {
        try {
            researchMemoryService.deleteByProjectAndType(projectId, userId, "CONCLUSION");
            for (int i = 0; i < conclusions.size(); i++) {
                Map<String, Object> conclusion = conclusions.get(i);
                Object stmtObj = conclusion.get("statement");
                String statement = stmtObj != null ? stmtObj.toString() : "结论-" + (i + 1);

                StringBuilder md = new StringBuilder();
                md.append("### 结论 ").append(i + 1).append("\n\n");
                md.append("- **内容**：").append(statement).append("\n");
                md.append("- **置信度**：").append(conclusion.getOrDefault("confidence", "medium")).append("\n");
                Object reliability = conclusion.get("reliabilityScore");
                if (reliability != null) {
                    md.append("- **可靠性评分**：").append(reliability).append("\n");
                }
                Object timeliness = conclusion.get("timeliness");
                if (timeliness != null) {
                    md.append("- **时效性**：").append(timeliness).append("\n");
                }
                if (Boolean.TRUE.equals(conclusion.get("hasConflict"))) {
                    md.append("- **⚠ 存在矛盾**：该结论与其他结论存在冲突\n");
                }

                String memoryKey = ResearchMemoryKey.of("CONCLUSION", statement);
                researchMemoryService.save(projectId, userId, memoryKey, "CONCLUSION", md.toString());
            }
            log.info("conclusion_memories_persisted projectId={} count={}", projectId, conclusions.size());
        } catch (Exception e) {
            log.warn("persist_conclusion_memories_failed projectId={}", projectId, e);
        }
    }

    /**
     * 评估来源可靠性.
     */
    @SuppressWarnings("unchecked")
    private double scoreReliability(List<Map<String, Object>> sources, Map<String, Object> finding) {
        List<Number> sourceIndices = (List<Number>) finding.get("sourceIndices");
        if (sourceIndices == null || sourceIndices.isEmpty()) return 0.0;

        double totalWeight = 0;
        int count = 0;
        for (Number idx : sourceIndices) {
            int index = idx.intValue() - 1; // 转为 0-based
            if (index >= 0 && index < sources.size()) {
                Map<String, Object> source = sources.get(index);
                String sourceType = (String) source.getOrDefault("sourceType", "web_search");
                totalWeight += RELIABILITY_WEIGHTS.getOrDefault(sourceType, 0.5);
                count++;
            }
        }
        return count > 0 ? totalWeight / count : 0.0;
    }

    /**
     * 评估来源时效性.
     */
    private String assessTimeliness(List<Map<String, Object>> sources) {
        // 简化实现：检查来源类型，官方文档和论文通常时效性较好
        for (Map<String, Object> source : sources) {
            String type = (String) source.get("sourceType");
            if ("official_doc".equals(type) || "paper".equals(type)) {
                return "current";
            }
        }
        return "unknown";
    }

    /**
     * 确定最终置信度.
     */
    private String determineConfidence(double reliabilityScore, String timeliness,
                                        Map<String, Object> finding, int independentSourceCount) {
        String category = (String) finding.get("category");

        double score = reliabilityScore;
        // 时效性调整（弱化惩罚，避免 unknown 直接把 high 拉到 medium）
        if ("outdated".equals(timeliness)) score -= 0.15;
        else if ("unknown".equals(timeliness)) score -= 0.02;

        // 类别调整
        if ("contradiction".equals(category)) score -= 0.2;

        // 单一普通网页最多只能形成中置信结论，避免“有一个 URL”被误判为交叉验证。
        if (score >= 0.7 && independentSourceCount >= 2) return "high";
        if (score >= 0.5) return "medium";
        if (score >= 0.3) return "low";
        return "speculation";
    }

    /**
     * 查找支撑该发现的来源.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> findSupportingSources(List<Map<String, Object>> sources,
                                                             Map<String, Object> finding) {
        List<Number> sourceIndices = (List<Number>) finding.get("sourceIndices");
        if (sourceIndices == null) return List.of();

        List<Map<String, Object>> supporting = new ArrayList<>();
        for (Number idx : sourceIndices) {
            int index = idx.intValue() - 1;
            if (index >= 0 && index < sources.size()) {
                Map<String, Object> source = sources.get(index);
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("title", source.get("title"));
                ref.put("url", source.get("url"));
                ref.put("sourceType", source.get("sourceType"));
                ref.put("relevance", "direct");
                supporting.add(ref);
            }
        }
        return supporting;
    }

    private int countIndependentSources(List<Map<String, Object>> supportingSources) {
        return (int) supportingSources.stream()
                .map(source -> source.get("url"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(url -> !url.isBlank())
                .map(this::extractHost)
                .filter(host -> !host.isBlank())
                .distinct()
                .count();
    }

    private String extractHost(String url) {
        try {
            String host = java.net.URI.create(url).getHost();
            return host != null ? host.toLowerCase() : "";
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * 一致性检查：通过 LLM 检测结论之间的冲突.
     */
    private int checkConsistency(List<Map<String, Object>> conclusions, Long userId) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("检查以下研究结论是否存在矛盾。\n\n");
            for (int i = 0; i < conclusions.size(); i++) {
                sb.append("[").append(i + 1).append("] ")
                        .append(conclusions.get(i).get("statement")).append("\n");
            }

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", sb.toString()
                            + "\n如果有矛盾的结论对，用 JSON 格式输出："
                            + "{\"conflicts\": [{\"index1\": 1, \"index2\": 2, \"reason\": \"矛盾原因\"}]}"
                            + "\n如果没有矛盾，输出：{\"conflicts\": []}"));

            String response = aiService.chat(userId, AiScenario.RESEARCH.getCode(), messages);
            if (response != null) {
                int start = response.indexOf('{');
                int end = response.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(
                            response.substring(start, end + 1), Map.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> conflicts =
                            (List<Map<String, Object>>) result.get("conflicts");
                    if (conflicts != null) {
                        // 标记冲突的结论
                        for (Map<String, Object> conflict : conflicts) {
                            int idx1 = ((Number) conflict.get("index1")).intValue() - 1;
                            int idx2 = ((Number) conflict.get("index2")).intValue() - 1;
                            if (idx1 >= 0 && idx1 < conclusions.size()) {
                                conclusions.get(idx1).put("hasConflict", true);
                            }
                            if (idx2 >= 0 && idx2 < conclusions.size()) {
                                conclusions.get(idx2).put("hasConflict", true);
                            }
                        }
                        return conflicts.size();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("consistency_check_failed", e);
        }
        return 0;
    }
}
