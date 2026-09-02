package com.secondbrain.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.entity.ResearchSource;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.mapper.ResearchSourceMapper;
import com.secondbrain.research.orchestrator.AgentContext;
import com.secondbrain.research.orchestrator.ToolCallBudget;
import com.secondbrain.research.tool.ToolResult;
import com.secondbrain.research.tool.WebFetchTool;
import com.secondbrain.research.tool.WebSearchTool;
import com.secondbrain.service.AiService;
import com.secondbrain.service.ResearchMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部研究 Agent.
 *
 * <p>负责对每个研究任务执行外部网页搜索、内容抓取和发现提取。
 * 搜索结果保存为 ResearchSource，供 CriticAgent 验证和 SynthesizerAgent 引用。</p>
 *
 * @author AI
 */
@Component
public class ExternalResearchAgent implements ResearchAgent {

    private static final Logger log = LoggerFactory.getLogger(ExternalResearchAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiService aiService;
    private final WebSearchTool webSearchTool;
    private final WebFetchTool webFetchTool;
    private final ResearchSourceMapper researchSourceMapper;
    private final ResearchMemoryService researchMemoryService;

    public ExternalResearchAgent(AiService aiService,
                                 WebSearchTool webSearchTool,
                                 WebFetchTool webFetchTool,
                                 ResearchSourceMapper researchSourceMapper,
                                 ResearchMemoryService researchMemoryService) {
        this.aiService = aiService;
        this.webSearchTool = webSearchTool;
        this.webFetchTool = webFetchTool;
        this.researchSourceMapper = researchSourceMapper;
        this.researchMemoryService = researchMemoryService;
    }

    @Override
    public String getName() {
        return "ResearchAgent";
    }

    @Override
    public long getTimeoutMs() {
        return 120_000;
    }

    @Override
    public int getMaxRetries() {
        return 2;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Long projectId = context.getProjectId();
        Long userId = context.getUserId();
        Long workspaceId = context.getWorkspaceId();

        log.info("research_agent_start projectId={}", projectId);

        try {
            // Step 1: 获取待研究的任务列表
            List<Map<String, Object>> tasks = extractTasks(context);
            if (tasks.isEmpty()) {
                return AgentResult.completed(getName(),
                        "{\"sources\":[],\"findings\":[],\"note\":\"无外部搜索任务\"}");
            }

            // Step 2: 筛选需要外部搜索的任务
            List<Map<String, Object>> externalTasks = tasks.stream()
                    .filter(t -> Boolean.TRUE.equals(t.get("requiresExternalSearch")))
                    .toList();

            if (externalTasks.isEmpty()) {
                return AgentResult.completed(getName(),
                        "{\"sources\":[],\"findings\":[],\"note\":\"所有任务均可从知识库回答\"}");
            }

            // Step 3: 对每个任务执行搜索和抓取
            Map<String, Object> previousResearchData = parsePreviousResearchData(context);
            List<Map<String, Object>> allSources = copyItems(previousResearchData, "sources");
            List<Map<String, Object>> allFindings = copyItems(previousResearchData, "findings");
            int totalFetched = 0;
            int totalFailed = 0;

            taskLoop:
            for (Map<String, Object> task : externalTasks) {
                List<Map<String, Object>> taskSources = new ArrayList<>();
                String taskQuestion = (String) task.getOrDefault("question",
                        task.getOrDefault("title", "研究"));
                Object taskIdObj = task.get("taskId");

                // 生成搜索查询
                List<String> queries = generateSearchQueries(taskQuestion, userId);

                // 执行搜索
                for (String query : queries) {
                    if (isDuplicateToolCall(context, webSearchTool.getName(), query)) {
                        continue;
                    }
                    if (!reserveToolCall(context, webSearchTool.getName(), query)) {
                        break taskLoop;
                    }
                    Map<String, Object> searchParams = new HashMap<>();
                    searchParams.put("query", query);
                    searchParams.put("maxResults", 5);

                    ToolResult searchResult = webSearchTool.execute(searchParams);
                    if (!searchResult.isSuccess()) continue;

                    // 从搜索结果中提取 URL
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> rawResults =
                            (List<Map<String, String>>) searchResult.getMetadata().get("rawResults");
                    if (rawResults == null) continue;

                    // 抓取前 3 个结果
                    int fetched = 0;
                    for (Map<String, String> r : rawResults) {
                        if (fetched >= 3) break;
                        String url = r.get("url");
                        if (url == null || url.isBlank()) continue;
                        if (containsSourceUrl(allSources, url) || containsSourceUrl(taskSources, url)) {
                            continue;
                        }
                        if (isDuplicateToolCall(context, webFetchTool.getName(), url)) {
                            continue;
                        }
                        if (!reserveToolCall(context, webFetchTool.getName(), url)) {
                            break taskLoop;
                        }

                        Map<String, Object> fetchParams = new HashMap<>();
                        fetchParams.put("url", url);

                        ToolResult fetchResult = webFetchTool.execute(fetchParams);
                        if (fetchResult.isSuccess()) {
                            totalFetched++;
                            // 保存为 ResearchSource
                            ResearchSource source = new ResearchSource();
                            source.setProjectId(projectId);
                            source.setTaskId(taskIdObj instanceof Number
                                    ? ((Number) taskIdObj).longValue() : null);
                            source.setTitle(r.get("title"));
                            source.setUrl(url);
                            source.setSourceType("web_search");
                            source.setSnippet(r.get("snippet"));
                            source.setFullContent(fetchResult.getData());
                            source.setRelevanceScore(BigDecimal.valueOf(0.8));
                            source.setReliability("unverified");
                            source.setFetchStatus("success");
                            source.setFetchedAt(LocalDateTime.now());

                            try {
                                researchSourceMapper.insert(source);
                            } catch (Exception e) {
                                log.warn("save_source_failed url={}", url, e);
                            }

                            Map<String, Object> sourceInfo = new LinkedHashMap<>();
                            sourceInfo.put("title", r.get("title"));
                            sourceInfo.put("url", url);
                            sourceInfo.put("snippet", r.get("snippet"));
                            sourceInfo.put("sourceType", "web_search");
                            sourceInfo.put("content", abbreviateContent(fetchResult.getData()));
                            taskSources.add(sourceInfo);
                        } else {
                            totalFailed++;
                        }
                        fetched++;
                    }
                }

                // 提取发现
                if (!taskSources.isEmpty()) {
                    int globalSourceOffset = allSources.size();
                    List<Map<String, Object>> findings = extractFindings(
                            taskQuestion, taskSources, userId);
                    allFindings.addAll(EvidenceCitationNormalizer.normalize(
                            findings, taskSources.size(), globalSourceOffset));
                    allSources.addAll(taskSources);
                }
            }

            // Step 4: 构建输出
            // 持久化研究发现到 research_memory 表，供前端"研究发现"导航展示
            persistFindingMemories(projectId, userId, allFindings);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("sources", allSources);
            output.put("findings", allFindings);
            output.put("searchStats", Map.of(
                    "tasksProcessed", externalTasks.size(),
                    "pagesFetched", totalFetched,
                    "pagesFailed", totalFailed,
                    "sourcesCollected", allSources.size(),
                    "findingsExtracted", allFindings.size()));
            output.put("evidenceStatus", allSources.isEmpty()
                    ? "UNAVAILABLE" : allFindings.isEmpty() ? "INSUFFICIENT" : "AVAILABLE");
            output.put("toolBudget", buildToolBudgetSnapshot(context));
            if (allSources.isEmpty()) {
                output.put("note", "外部搜索未获得可验证来源，本轮不生成外部研究发现");
                context.setDegradedMode(true);
            }

            String outputJson = objectMapper.writeValueAsString(output);
            log.info("research_agent_done projectId={} sources={} findings={}",
                    projectId, allSources.size(), allFindings.size());
            return AgentResult.completed(getName(), outputJson);
        } catch (Exception e) {
            log.error("research_agent_failed projectId={}", projectId, e);
            return AgentResult.failed(getName(), "外部研究失败: " + e.getMessage());
        }
    }

    /**
     * 从研究计划中提取任务列表.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractTasks(AgentContext context) {
        List<Map<String, Object>> followUpTasks = extractFollowUpTasks(context);
        if (!followUpTasks.isEmpty()) {
            return followUpTasks;
        }
        if (context.getResearchPlan() != null
                && context.getResearchPlan().getTasksJson() != null) {
            try {
                return objectMapper.readValue(
                        context.getResearchPlan().getTasksJson(), List.class);
            } catch (Exception e) {
                log.warn("parse_tasks_json_failed", e);
            }
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFollowUpTasks(AgentContext context) {
        String criticOutput = context.getAgentOutput("CriticAgent");
        if (criticOutput == null || criticOutput.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> criticData = objectMapper.readValue(criticOutput, Map.class);
            Object gateValue = criticData.get("qualityGate");
            if (!(gateValue instanceof Map<?, ?> qualityGate)) {
                return List.of();
            }
            Object queryValue = qualityGate.get("followUpQueries");
            if (!(queryValue instanceof List<?> queries)) {
                return List.of();
            }

            List<Map<String, Object>> tasks = new ArrayList<>();
            for (Object query : queries) {
                if (query instanceof String question && !question.isBlank()) {
                    Map<String, Object> task = new LinkedHashMap<>();
                    task.put("question", question);
                    task.put("requiresExternalSearch", true);
                    tasks.add(task);
                }
            }
            return tasks;
        } catch (Exception e) {
            log.warn("parse_follow_up_queries_failed projectId={}", context.getProjectId(), e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePreviousResearchData(AgentContext context) {
        String previousOutput = context.getAgentOutput(getName());
        if (previousOutput == null || previousOutput.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(previousOutput, Map.class);
        } catch (Exception e) {
            log.warn("parse_previous_research_output_failed projectId={}", context.getProjectId(), e);
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> copyItems(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof List<?> items)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                result.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return result;
    }

    private boolean containsSourceUrl(List<Map<String, Object>> sources, String url) {
        return sources.stream().anyMatch(source -> url.equals(source.get("url")));
    }

    private String abbreviateContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        int contentLimit = Math.min(content.length(), 3000);
        return content.substring(0, contentLimit);
    }

    private boolean reserveToolCall(AgentContext context, String toolName, String parameter) {
        ToolCallBudget budget = context.getToolCallBudget();
        if (budget == null) {
            return true;
        }
        String parameterHash = Integer.toHexString(parameter.hashCode());
        if (!budget.canCall()) {
            context.setRecoveryStopReason("TOOL_CALL_BUDGET_EXHAUSTED");
            log.warn("research_tool_budget_exhausted tool={} projectId={} used={} max={}",
                    toolName, context.getProjectId(), budget.getUsed(), budget.getMaxCalls());
            return false;
        }
        budget.recordCall(toolName, parameterHash);
        return true;
    }

    private boolean isDuplicateToolCall(AgentContext context, String toolName, String parameter) {
        ToolCallBudget budget = context.getToolCallBudget();
        if (budget == null) {
            return false;
        }
        boolean duplicate = budget.isDuplicateCall(
                toolName, Integer.toHexString(parameter.hashCode()));
        if (duplicate) {
            log.info("research_tool_duplicate_skipped tool={} projectId={}",
                    toolName, context.getProjectId());
        }
        return duplicate;
    }

    private Map<String, Object> buildToolBudgetSnapshot(AgentContext context) {
        ToolCallBudget budget = context.getToolCallBudget();
        if (budget == null) {
            return Map.of();
        }
        return Map.of(
                "used", budget.getUsed(),
                "remaining", budget.getRemaining(),
                "max", budget.getMaxCalls());
    }

    /**
     * 通过 LLM 生成搜索查询.
     */
    private List<String> generateSearchQueries(String question, Long userId) {
        try {
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content",
                            "为以下研究问题生成 2 个简洁的网页搜索查询（每行一个，纯查询文本，不要编号）：\n"
                                    + question));
            String response = aiService.chat(userId, AiScenario.RESEARCH.getCode(), messages);
            if (response != null) {
                List<String> queries = new ArrayList<>();
                for (String line : response.split("\n")) {
                    String trimmed = line.replaceAll("^[\\d\\.\\-\\*\\s]+", "").trim();
                    if (!trimmed.isBlank() && trimmed.length() > 3) {
                        queries.add(trimmed);
                    }
                }
                return queries.isEmpty() ? List.of(question) : queries;
            }
        } catch (Exception e) {
            log.warn("query_gen_failed", e);
        }
        return List.of(question);
    }

    /**
     * 通过 LLM 从搜索结果中提取关键发现.
     */
    private List<Map<String, Object>> extractFindings(String question,
                                                       List<Map<String, Object>> sources,
                                                       Long userId) {
        try {
            StringBuilder context_ = new StringBuilder();
            context_.append("研究问题: ").append(question).append("\n\n");
            context_.append("收集到的来源:\n");
            for (int i = 0; i < sources.size(); i++) {
                Map<String, Object> s = sources.get(i);
                context_.append("[").append(i + 1).append("] ")
                        .append(s.get("title")).append("\n");
                context_.append("URL: ").append(s.get("url")).append("\n");
                String snippet = (String) s.get("snippet");
                if (snippet != null && snippet.length() > 300) {
                    snippet = snippet.substring(0, 300) + "...";
                }
                context_.append("摘要: ").append(snippet).append("\n\n");
                String content = (String) s.get("content");
                if (content != null && !content.isBlank()) {
                    int contentLimit = Math.min(content.length(), 3000);
                    context_.append("正文摘录: ").append(content, 0, contentLimit).append("\n\n");
                }
            }

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content",
                            "从以下来源中提取针对研究问题的关键发现。\n\n"
                                    + context_ + "\n"
                                    + "请用 JSON 数组格式输出（最多 5 个发现）：\n"
                                    + "[{\"statement\": \"发现内容\", "
                                    + "\"category\": \"fact/insight/contradiction\", "
                                    + "\"confidence\": \"high/medium/low\", "
                                    + "\"sourceIndices\": [1, 2]}]"));

            String response = aiService.chat(userId, AiScenario.RESEARCH.getCode(), messages);
            if (response != null) {
                int start = response.indexOf('[');
                int end = response.lastIndexOf(']');
                if (start >= 0 && end > start) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> findings = objectMapper.readValue(
                            response.substring(start, end + 1), List.class);
                    return findings;
                }
            }
        } catch (Exception e) {
            log.warn("extract_findings_failed", e);
        }
        return List.of();
    }

    /**
     * 将研究发现持久化为 FINDING 类型记忆.
     *
     * <p>每条发现保存为一条独立记忆，key 使用发现内容前缀。重新执行研究时先清理旧记忆。</p>
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @param findings  发现列表
     */
    private void persistFindingMemories(Long projectId, Long userId,
                                         List<Map<String, Object>> findings) {
        try {
            researchMemoryService.deleteByProjectAndType(projectId, userId, "FINDING");
            for (int i = 0; i < findings.size(); i++) {
                Map<String, Object> finding = findings.get(i);
                Object stmtObj = finding.get("statement");
                String statement = stmtObj != null ? stmtObj.toString() : "发现-" + (i + 1);

                StringBuilder md = new StringBuilder();
                md.append("### 发现 ").append(i + 1).append("\n\n");
                md.append("- **内容**：").append(statement).append("\n");
                md.append("- **类别**：").append(finding.getOrDefault("category", "fact")).append("\n");
                md.append("- **置信度**：").append(finding.getOrDefault("confidence", "medium")).append("\n");

                Object sourceIndices = finding.get("sourceIndices");
                if (sourceIndices != null) {
                    md.append("- **来源索引**：").append(sourceIndices).append("\n");
                }

                String memoryKey = ResearchMemoryKey.of("FINDING", statement);
                researchMemoryService.save(projectId, userId, memoryKey, "FINDING", md.toString());
            }
            log.info("finding_memories_persisted projectId={} count={}", projectId, findings.size());
        } catch (Exception e) {
            log.warn("persist_finding_memories_failed projectId={}", projectId, e);
        }
    }
}
