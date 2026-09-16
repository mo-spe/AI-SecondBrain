package com.secondbrain.research.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 来源深度分析工具.
 *
 * <p>通过 LLM 对抓取的网页内容进行深度分析：提取关键事实、
 * 评估内容可靠性、检测与已有结论的冲突。</p>
 *
 * @author AI
 */
@Component
public class SourceAnalyzeTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(SourceAnalyzeTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiService aiService;

    public SourceAnalyzeTool(AiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public String getName() {
        return "source_analyze";
    }

    @Override
    public String getDescription() {
        return "深度分析网页内容：提取事实、评估可靠性、检测矛盾";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "content", Map.of("type", "string", "description", "要分析的文本内容"),
                "question", Map.of("type", "string", "description", "研究问题（用于相关度判断）"),
                "analysisType", Map.of("type", "string",
                        "enum", List.of("fact_extraction", "reliability", "consistency", "full"),
                        "description", "分析类型，默认 full")
        ));
        schema.put("required", List.of("content", "question"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String content = (String) params.get("content");
        String question = (String) params.get("question");
        String analysisType = (String) params.getOrDefault("analysisType", "full");

        if (content == null || content.isBlank()) {
            return ToolResult.failure("INVALID_PARAM", "content 不能为空", false);
        }
        if (question == null || question.isBlank()) {
            return ToolResult.failure("INVALID_PARAM", "question 不能为空", false);
        }

        Long userId = params.get("userId") instanceof Number
                ? ((Number) params.get("userId")).longValue() : null;
        if (userId == null) {
            return ToolResult.failure("INVALID_PARAM", "userId 不能为空", false);
        }

        // 截断过长内容
        String truncatedContent = content.length() > 8000
                ? content.substring(0, 8000) + "...[截断]" : content;

        try {
            String prompt = buildAnalysisPrompt(truncatedContent, question, analysisType);
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", prompt));

            String response = aiService.chat(userId, AiScenario.RESEARCH.getCode(), messages);
            return ToolResult.success(response);
        } catch (Exception e) {
            log.error("source_analyze_failed", e);
            return ToolResult.failure("ANALYZE_ERROR", e.getMessage(), true);
        }
    }

    private String buildAnalysisPrompt(String content, String question, String analysisType) {
        StringBuilder sb = new StringBuilder();
        sb.append("分析以下网页内容与研究问题的相关性。\n\n");
        sb.append("研究问题: ").append(question).append("\n\n");
        sb.append("网页内容:\n").append(content).append("\n\n");

        sb.append("请用 JSON 格式输出分析结果：\n");
        sb.append("{\n");
        sb.append("  \"relevance\": \"high/medium/low\",\n");
        sb.append("  \"reliability\": \"high/medium/low/unverified\",\n");
        sb.append("  \"reliabilityReason\": \"判断理由（50字内）\",\n");
        sb.append("  \"keyFacts\": [\"从该来源提取的关键事实\"],\n");
        sb.append("  \"potentialBias\": \"可能存在的偏见或局限\",\n");
        sb.append("  \"timeliness\": \"current/acceptable/outdated/unknown\",\n");
        sb.append("  \"summary\": \"内容摘要（100字内）\"\n");
        sb.append("}");

        return sb.toString();
    }
}
