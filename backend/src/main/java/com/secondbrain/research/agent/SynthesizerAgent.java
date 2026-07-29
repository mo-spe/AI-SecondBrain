package com.secondbrain.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.entity.ResearchReport;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.mapper.ResearchReportMapper;
import com.secondbrain.research.orchestrator.AgentContext;
import com.secondbrain.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告综合 Agent.
 *
 * <p>汇总所有前置 Agent 的输出（知识库、缺口、外部搜索、验证结论），
 * 生成结构化的最终研究报告（Markdown 格式）并持久化到 research_report 表。
 * 具备多级回退：完整报告 → 精简报告 → 无 LLM 基础汇总。</p>
 *
 * @author AI
 */
@Component
public class SynthesizerAgent implements ResearchAgent {

    private static final Logger log = LoggerFactory.getLogger(SynthesizerAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiService aiService;
    private final ResearchReportMapper researchReportMapper;

    public SynthesizerAgent(AiService aiService, ResearchReportMapper researchReportMapper) {
        this.aiService = aiService;
        this.researchReportMapper = researchReportMapper;
    }

    @Override
    public String getName() {
        return "SynthesizerAgent";
    }

    @Override
    public long getTimeoutMs() {
        return 120_000;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Long projectId = context.getProjectId();
        Long userId = context.getUserId();

        log.info("synthesizer_agent_start projectId={}", projectId);

        try {
            String researchOutput = context.getAgentOutput("ResearchAgent");
            String criticOutput = context.getAgentOutput("CriticAgent");
            String gapOutput = context.getAgentOutput("GapAgent");

            // 提取研究发现摘要
            List<Map<String, Object>> findings = extractItems(researchOutput, "findings");
            List<Map<String, Object>> conclusions = extractItems(criticOutput, "conclusions");

            // 多级回退生成报告
            String reportMarkdown = generateReportWithFallback(context.getResearchGoal(),
                    findings, conclusions, gapOutput, userId);

            // 生成摘要（短 prompt，通常不会超时）
            String summary = generateSummaryWithFallback(reportMarkdown, findings, userId);

            // 持久化到数据库
            saveReport(projectId, reportMarkdown, summary, researchOutput, criticOutput, gapOutput,
                    findings.size(), conclusions.size());

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("reportTitle", "研究: " + context.getResearchGoal());
            output.put("summary", summary);
            output.put("reportMarkdown", reportMarkdown);

            String outputJson = objectMapper.writeValueAsString(output);
            log.info("synthesizer_agent_done projectId={} reportLength={}",
                    projectId, reportMarkdown.length());
            return AgentResult.completed(getName(), outputJson);
        } catch (Exception e) {
            log.error("synthesizer_agent_failed projectId={}", projectId, e);

            // 最后一次尝试：纯本地生成基础报告
            try {
                String fallbackReport = buildFallbackReport(context.getResearchGoal(),
                        extractItems(context.getAgentOutput("ResearchAgent"), "findings"),
                        extractItems(context.getAgentOutput("CriticAgent"), "conclusions"));
                saveReport(projectId, fallbackReport, "研究自动汇总（LLM 生成失败）",
                        context.getAgentOutput("ResearchAgent"),
                        context.getAgentOutput("CriticAgent"),
                        context.getAgentOutput("GapAgent"), 0, 0);

                Map<String, Object> output = new LinkedHashMap<>();
                output.put("reportTitle", "研究: " + context.getResearchGoal());
                output.put("summary", "研究自动汇总（LLM 生成失败）");
                output.put("reportMarkdown", fallbackReport);
                return AgentResult.completed(getName(), objectMapper.writeValueAsString(output));
            } catch (Exception inner) {
                log.error("fallback_report_failed projectId={}", projectId, inner);
                return AgentResult.failed(getName(), "报告综合失败: " + e.getMessage());
            }
        }
    }

    /**
     * 多级回退：完整报告 → 精简报告 → 无 LLM 本地汇总.
     */
    private String generateReportWithFallback(String goal, List<Map<String, Object>> findings,
                                               List<Map<String, Object>> conclusions,
                                               String gapOutput, Long userId) {
        // 第1级：完整报告（仅包含最关键的发现，最多8条）
        try {
            return generateReport(goal, findings, conclusions, gapOutput, 8, userId);
        } catch (Exception e) {
            log.warn("report_level1_full_failed, trying level2", e);
        }

        // 第2级：精简报告（仅3条发现）
        try {
            return generateReport(goal, findings, conclusions, gapOutput, 3, userId);
        } catch (Exception e) {
            log.warn("report_level2_compact_failed, using fallback", e);
        }

        // 第3级：无 LLM 本地汇总
        return buildFallbackReport(goal, findings, conclusions);
    }

    /**
     * LLM 报告生成，限制发现数量控制 prompt 大小，使用流式调用避免超时.
     */
    private String generateReport(String goal, List<Map<String, Object>> findings,
                                   List<Map<String, Object>> conclusions,
                                   String gapOutput, int maxFindings, Long userId) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("研究目标: ").append(goal).append("\n\n");

        List<Map<String, Object>> topFindings = findings.size() > maxFindings
                ? findings.subList(0, maxFindings) : findings;
        ctx.append("研究发现（").append(topFindings.size()).append(" 条）:\n");
        for (int i = 0; i < topFindings.size(); i++) {
            Map<String, Object> f = topFindings.get(i);
            String stmt = (String) f.get("statement");
            if (stmt != null && stmt.length() > 250) {
                stmt = stmt.substring(0, 250) + "...";
            }
            ctx.append("- [F").append(i + 1).append("] ")
                    .append(f.getOrDefault("category", "fact")).append(": ")
                    .append(stmt).append("\n");
        }

        if (!conclusions.isEmpty()) {
            List<Map<String, Object>> topConclusions = conclusions.size() > 8
                    ? conclusions.subList(0, 8) : conclusions;
            ctx.append("\n验证结论（").append(topConclusions.size()).append(" 条）:\n");
            for (int i = 0; i < topConclusions.size(); i++) {
                Map<String, Object> c = topConclusions.get(i);
                String stmt = (String) c.get("statement");
                if (stmt != null && stmt.length() > 250) {
                    stmt = stmt.substring(0, 250) + "...";
                }
                ctx.append("- [C").append(i + 1).append("] [")
                        .append(c.getOrDefault("confidence", "medium")).append("] ")
                        .append(stmt).append("\n");
            }
        }

        if (gapOutput != null && !gapOutput.isBlank()) {
            String gaps = gapOutput.length() > 600 ? gapOutput.substring(0, 600) + "..." : gapOutput;
            ctx.append("\n知识缺口分析:\n").append(gaps).append("\n");
        }

        String prompt = "你是一名资深领域研究员。基于以下研究数据，生成一份结构完整、内容详实的中文研究报告（Markdown 格式，不少于 1500 字）。\n\n"
                + "报告必须严格包含以下 7 个章节，每个章节都要有具体内容，不要空泛陈述：\n\n"
                + "## 1. 研究概述\n"
                + "   - 阐述研究的背景与动机（为什么要做这个研究，业界/学术的痛点是什么）\n"
                + "   - 明确说明研究目标与范围（研究什么、不研究什么）\n"
                + "   - 简述采用的研究方法与流程\n\n"
                + "## 2. 核心概念与理论基础\n"
                + "   - 系统梳理领域内的关键概念、术语与定义（至少 5 个核心概念）\n"
                + "   - 阐述概念之间的联系与理论体系\n\n"
                + "## 3. 关键研究发现（深度展开）\n"
                + "   - 对每个发现 Fn 做详细论述：它的含义是什么？为什么成立？证据是什么？\n"
                + "   - 用具体例子/场景/数据点来支撑\n\n"
                + "## 4. 验证后的高可信结论\n"
                + "   - 列出每条结论 Cn，明确其置信度与支撑来源\n"
                + "   - 说明结论的适用边界和前提条件\n\n"
                + "## 5. 当前认知的知识缺口与未解决问题\n"
                + "   - 哪些问题仍不确定？哪些矛盾需要进一步澄清？\n"
                + "   - 未来研究方向的具体建议\n\n"
                + "## 6. 实践指导与建议\n"
                + "   - 给决策者：3-5 条可落地的建议（何时/何地/如何应用）\n"
                + "   - 风险提示：应用不当可能带来的负面效果与规避方法\n\n"
                + "## 7. 总结与展望\n"
                + "   - 一段凝练的总结（3-5 句）\n"
                + "   - 对未来 1-3 年的趋势预测\n\n"
                + "要求：\n"
                + "1) 每个发现/结论都要展开论述，不要一句话带过；\n"
                + "2) 多用具体子标题、列表、粗体等 Markdown 结构，便于阅读；\n"
                + "3) 语言专业但易懂，避免废话和套话；\n"
                + "4) 全文使用中文，总字数不少于 1500 字。\n\n"
                + "-----------------\n研究数据：\n\n"
                + ctx + "\n\n";

        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", prompt));

        StringBuilder lastChunk = new StringBuilder();
        log.info("synthesizer_stream_start goal_len={}", goal.length());
        String result = aiService.chatStream(userId, AiScenario.RESEARCH.getCode(), messages,
                chunk -> {
                    lastChunk.append(chunk);
                });
        log.info("synthesizer_stream_done totalLen={}",
                result != null ? result.length() : 0);
        return result;
    }

    /**
     * 多级回退生成摘要.
     */
    private String generateSummaryWithFallback(String report, List<Map<String, Object>> findings,
                                                Long userId) {
        try {
            String shortReport = report.length() > 800 ? report.substring(0, 800) + "..." : report;
            String prompt = "用一句话概括这份研究报告的核心结论（50字以内，中文）:\n\n" + shortReport;
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", prompt));
            return aiService.chat(userId, AiScenario.RESEARCH.getCode(), messages);
        } catch (Exception e) {
            log.warn("summary_generation_failed", e);
            if (!findings.isEmpty()) {
                Object stmt = findings.get(0).get("statement");
                return stmt != null ? stmt.toString() : "无摘要";
            }
            return "无摘要";
        }
    }

    /**
     * 无 LLM 本地基础报告（兜底方案）.
     */
    private String buildFallbackReport(String goal, List<Map<String, Object>> findings,
                                        List<Map<String, Object>> conclusions) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 研究: ").append(goal).append("\n\n");
        sb.append("## 研究概述\n\n");
        sb.append("本研究共发现 ").append(findings.size()).append(" 条发现，")
                .append("验证 ").append(conclusions.size()).append(" 条结论。\n\n");

        if (!findings.isEmpty()) {
            sb.append("## 研究发现\n\n");
            for (int i = 0; i < findings.size(); i++) {
                Map<String, Object> f = findings.get(i);
                sb.append("- [").append(f.getOrDefault("category", "fact")).append("] ")
                        .append(f.get("statement")).append("\n");
            }
        }

        if (!conclusions.isEmpty()) {
            sb.append("\n## 验证结论\n\n");
            for (Map<String, Object> c : conclusions) {
                sb.append("- [").append(c.getOrDefault("confidence", "unknown")).append("] ")
                        .append(c.get("statement")).append("\n");
            }
        }

        sb.append("\n> 注：本报告由系统自动汇总生成，未经 LLM 增强。\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(String json, String key) {
        if (json == null) return List.of();
        try {
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            return (List<Map<String, Object>>) data.getOrDefault(key, List.of());
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 将生成的报告持久化到 research_report 表.
     */
    private void saveReport(Long projectId, String reportMarkdown, String summary,
                            String researchOutput, String criticOutput, String gapOutput,
                            int sourceCount, int conclusionCount) {
        try {
            List<ResearchReport> existingReports = researchReportMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResearchReport>()
                            .eq(ResearchReport::getProjectId, projectId)
                            .orderByDesc(ResearchReport::getVersion)
                            .last("LIMIT 1"));
            int nextVersion = existingReports.isEmpty() ? 1 : existingReports.get(0).getVersion() + 1;

            ResearchReport report = new ResearchReport();
            report.setProjectId(projectId);
            report.setVersion(nextVersion);
            report.setTitle("研究: v" + nextVersion);
            report.setSummary(summary);
            report.setContentMd(reportMarkdown);
            report.setKeyFindings(researchOutput);
            report.setKnowledgeGaps(gapOutput);
            report.setSourceCount(sourceCount);
            report.setConclusionCount(conclusionCount);
            report.setGeneratedBy("SynthesizerAgent");
            researchReportMapper.insert(report);

            log.info("research_report_saved projectId={} version={} reportId={}",
                    projectId, nextVersion, report.getId());
        } catch (Exception e) {
            log.error("save_report_failed projectId={}", projectId, e);
        }
    }
}
