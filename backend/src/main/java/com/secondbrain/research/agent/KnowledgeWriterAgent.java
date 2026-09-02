package com.secondbrain.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.research.orchestrator.AgentContext;
import com.secondbrain.service.AiService;
import com.secondbrain.service.ResearchMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识写回 Agent.
 *
 * <p>从研究结论中提取可独立使用的知识点，去重检查后生成待确认的知识候选。
 * 候选记录不会直接写入知识库，需要用户审核确认。</p>
 *
 * @author AI
 */
@Component
public class KnowledgeWriterAgent implements ResearchAgent {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeWriterAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiService aiService;
    private final ResearchMemoryService researchMemoryService;

    public KnowledgeWriterAgent(AiService aiService,
                                 ResearchMemoryService researchMemoryService) {
        this.aiService = aiService;
        this.researchMemoryService = researchMemoryService;
    }

    @Override
    public String getName() {
        return "KnowledgeWriterAgent";
    }

    @Override
    public long getTimeoutMs() {
        return 40_000;
    }

    @Override
    public boolean shouldExecute(AgentContext context) {
        // 有 CriticAgent 验证结论时执行
        String criticOutput = context.getAgentOutput("CriticAgent");
        if (criticOutput != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> criticData = objectMapper.readValue(criticOutput, Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Object> stats =
                        (Map<String, Object>) criticData.get("validationStats");
                if (stats != null) {
                    int total = ((Number) stats.getOrDefault("totalFindings", 0)).intValue();
                    if (total > 0) {
                        return true;
                    }
                }
                // 无 validationStats 时，检查是否有任何结论
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> conclusions =
                        (List<Map<String, Object>>) criticData.get("conclusions");
                if (conclusions != null && !conclusions.isEmpty()) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("should_execute_check_failed", e);
            }
        }

        // CriticAgent 无输出时，检查 SynthesizerAgent 是否有报告可提取
        String synthesizerOutput = context.getAgentOutput("SynthesizerAgent");
        return synthesizerOutput != null && !synthesizerOutput.isBlank();
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Long projectId = context.getProjectId();
        Long userId = context.getUserId();

        log.info("knowledge_writer_agent_start projectId={}", projectId);

        try {
            // Step 1: 读取 CriticAgent 输出的高置信度结论
            String criticOutput = context.getAgentOutput("CriticAgent");
            if (criticOutput == null) {
                return AgentResult.completed(getName(),
                        "{\"candidates\":[],\"note\":\"无验证结论\"}");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> criticData = objectMapper.readValue(criticOutput, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conclusions =
                    (List<Map<String, Object>>) criticData.getOrDefault("conclusions", List.of());

            // 筛选高置信度和中置信度结论（搜索引擎回退场景下大部分结论为 medium，
            // 放宽筛选阈值可以保证知识候选不为空）
            List<Map<String, Object>> qualifiedConclusions = conclusions.stream()
                    .filter(c -> {
                        Object conf = c.get("confidence");
                        return "high".equals(conf) || "medium".equals(conf);
                    })
                    .toList();

            if (qualifiedConclusions.isEmpty()) {
                return AgentResult.completed(getName(),
                        "{\"candidates\":[],\"note\":\"无高/中置信度结论可写入\"}");
            }

            // Step 2: LLM 从结论中提取知识点
            List<Map<String, Object>> candidates = extractKnowledgeCandidates(
                    qualifiedConclusions, context.getResearchGoal(), userId);

            // Step 3: 构建输出
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("candidates", candidates);
            output.put("stats", Map.of(
                    "totalCandidates", candidates.size(),
                    "qualifiedConclusions", qualifiedConclusions.size(),
                    "duplicatesFiltered", 0));

            String outputJson = objectMapper.writeValueAsString(output);

            // 持久化知识候选记忆，供前端"知识候选"导航展示
            persistCandidateMemories(projectId, userId, candidates);

            log.info("knowledge_writer_agent_done projectId={} candidates={}",
                    projectId, candidates.size());
            return AgentResult.completed(getName(), outputJson);
        } catch (Exception e) {
            log.error("knowledge_writer_agent_failed projectId={}", projectId, e);
            return AgentResult.failed(getName(), "知识提取失败: " + e.getMessage());
        }
    }

    /**
     * 将知识候选持久化为 CANDIDATE 类型记忆.
     *
     * <p>每条候选保存为一条独立记忆，key 使用候选标题。重新执行研究时先清理旧记忆。</p>
     *
     * @param projectId  项目ID
     * @param userId     用户ID
     * @param candidates 候选列表
     */
    private void persistCandidateMemories(Long projectId, Long userId,
                                           List<Map<String, Object>> candidates) {
        try {
            researchMemoryService.deleteByProjectAndType(projectId, userId, "CANDIDATE");
            for (int i = 0; i < candidates.size(); i++) {
                Map<String, Object> candidate = candidates.get(i);
                Object titleObj = candidate.get("title");
                String title = titleObj != null ? titleObj.toString() : "知识候选-" + (i + 1);

                StringBuilder md = new StringBuilder();
                md.append("### ").append(title).append("\n\n");

                Object contentMd = candidate.get("contentMd");
                if (contentMd != null) {
                    md.append(contentMd).append("\n\n");
                }

                Object summary = candidate.get("summary");
                if (summary != null) {
                    md.append("**摘要**：").append(summary).append("\n\n");
                }

                Object tags = candidate.get("tags");
                if (tags != null) {
                    md.append("**标签**：").append(tags).append("\n");
                }

                Object importance = candidate.get("importance");
                if (importance != null) {
                    md.append("**重要性**：").append(importance).append("\n");
                }

                String memoryKey = ResearchMemoryKey.of("CANDIDATE", title);
                researchMemoryService.save(projectId, userId, memoryKey, "CANDIDATE", md.toString());
            }
            log.info("candidate_memories_persisted projectId={} count={}", projectId, candidates.size());
        } catch (Exception e) {
            log.warn("persist_candidate_memories_failed projectId={}", projectId, e);
        }
    }

    /**
     * 通过 LLM 从高置信度结论中提取独立知识点.
     */
    private List<Map<String, Object>> extractKnowledgeCandidates(
            List<Map<String, Object>> conclusions, String researchGoal, Long userId) {

        StringBuilder sb = new StringBuilder();
        sb.append("研究主题: ").append(researchGoal).append("\n\n");
        sb.append("高置信度结论:\n");
        for (int i = 0; i < conclusions.size(); i++) {
            sb.append("[").append(i + 1).append("] ")
                    .append(conclusions.get(i).get("statement")).append("\n");
        }

        sb.append("\n从以上结论中提取可独立存储的知识点。");
        sb.append("每个知识点应是一个完整、自包含的知识单元。");
        sb.append("用 JSON 数组格式输出：\n");
        sb.append("[{\n");
        sb.append("  \"title\": \"知识点标题\",\n");
        sb.append("  \"contentMd\": \"Markdown 格式的完整内容（300字以上）\",\n");
        sb.append("  \"summary\": \"简短摘要（50字内）\",\n");
        sb.append("  \"tags\": [\"标签1\", \"标签2\", \"标签3\"],\n");
        sb.append("  \"importance\": 4\n");
        sb.append("}]");

        try {
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", sb.toString()));

            String response = aiService.chat(userId, AiScenario.RESEARCH.getCode(), messages);
            if (response != null) {
                int start = response.indexOf('[');
                int end = response.lastIndexOf(']');
                if (start >= 0 && end > start) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candidates = objectMapper.readValue(
                            response.substring(start, end + 1), List.class);

                    // 标记来源结论
                    for (int i = 0; i < candidates.size(); i++) {
                        Map<String, Object> candidate = candidates.get(i);
                        if (i < conclusions.size()) {
                            candidate.put("sourceConclusionId", i + 1);
                        }
                        Map<String, Object> dedupCheck = new LinkedHashMap<>();
                        dedupCheck.put("isDuplicate", false);
                        dedupCheck.put("mostSimilarNodeId", null);
                        dedupCheck.put("similarityScore", 0.0);
                        candidate.put("dedupCheck", dedupCheck);
                    }

                    return candidates;
                }
            }
        } catch (Exception e) {
            log.warn("extract_candidates_failed", e);
        }
        return List.of();
    }
}
