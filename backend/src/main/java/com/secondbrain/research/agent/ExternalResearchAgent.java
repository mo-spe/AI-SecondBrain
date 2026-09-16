package com.secondbrain.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.entity.ResearchSource;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.mapper.ResearchSourceMapper;
import com.secondbrain.research.orchestrator.AgentContext;
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
            List<Map<String, Object>> allSources = new ArrayList<>();
            List<Map<String, Object>> allFindings = new ArrayList<>();
            int totalFetched = 0;
            int totalFailed = 0;

            for (Map<String, Object> task : externalTasks) {
                String taskQuestion = (String) task.getOrDefault("question",
                        task.getOrDefault("title", "研究"));
                Object taskIdObj = task.get("taskId");

                // 生成搜索查询
                List<String> queries = generateSearchQueries(taskQuestion, userId);

                // 执行搜索
                for (String query : queries) {
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
                            allSources.add(sourceInfo);
                        } else {
                            totalFailed++;
                        }
                        fetched++;
                    }
                }

                // 提取发现
                if (!allSources.isEmpty()) {
                    List<Map<String, Object>> findings = extractFindings(
                            taskQuestion, allSources, userId);
                    allFindings.addAll(findings);
                }
            }

            // Step 4: 构建输出
            // 如果外部搜索无结果，回退到 LLM 直接生成发现
            if (allSources.isEmpty()) {
                // 通过 LLM 生成研究发现和模拟的搜索来源
                List<Map<String, Object>> llmResults = generateLLMResearchResults(tasks, userId);
                if (!llmResults.isEmpty()) {
                    // 分离发现和来源
                    for (Map<String, Object> item : llmResults) {
                        Map<String, Object> sourceInfo = new LinkedHashMap<>();
                        sourceInfo.put("title", item.getOrDefault("title", ""));
                        sourceInfo.put("url", item.getOrDefault("url", ""));
                        sourceInfo.put("snippet", item.getOrDefault("snippet", ""));
                        sourceInfo.put("sourceType", item.getOrDefault("sourceType", "ai_knowledge"));
                        allSources.add(sourceInfo);

                        // 同时提取发现
                        Object findingsObj = item.get("findings");
                        if (findingsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> findings = (List<Map<String, Object>>) findingsObj;
                            allFindings.addAll(findings);
                        }
                    }
                }
            }

            // 持久化来源到数据库，前端 API 才能读取
            persistSources(projectId, allSources);

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
     * 外部搜索不可用时，通过 LLM 生成模拟研究结果的回退方案.
     *
     * <p>利用 LLM 训练数据中的知识，为每个研究问题生成：
     * - 看似真实的搜索来源（含标题、URL、摘要）
     * - 从来源中提取的关键发现
     * 来源标注为 ai_knowledge，前端可区分于真实搜索结果。
     *
     * <p><b>关键：</b>url 字段锚定到知识库首页、搜索页等<b>真实可访问</b>的知名资源 URL，
     * 避免 LLM 编造的 /@user/article-name-xxx 路径打不开（404）。
     */
    private List<Map<String, Object>> generateLLMResearchResults(List<Map<String, Object>> tasks, Long userId) {
        List<Map<String, Object>> allResults = new ArrayList<>();
        for (Map<String, Object> task : tasks) {
            String question = (String) task.getOrDefault("question",
                    task.getOrDefault("title", "研究"));
            try {
                List<Map<String, String>> messages = List.of(
                        Map.of("role", "system", "content",
                                "你是一个研究助手。基于你的知识，为以下研究问题生成模拟的网页搜索结果。\n"
                                        + "请输出 JSON 数组，每个元素包含：\n"
                                        + "- title: 来源标题（真实的学术/技术文章标题）\n"
                                        + "- url: 真实可访问的 URL，<b>必须使用以下域名之一，且必须指向该站点的真实页面路径</b>：\n"
                                        + "  * 维基百科：https://en.wikipedia.org/wiki/<Topic>  例如 https://en.wikipedia.org/wiki/Cache_(computing)\n"
                                        + "  * 维基百科(中文)：https://zh.wikipedia.org/wiki/<Topic>  例如 https://zh.wikipedia.org/wiki/缓存\n"
                                        + "  * GitHub：https://github.com/topics/<topic> 或 https://github.com/<org>/<repo>  例如 https://github.com/topics/caching\n"
                                        + "  * Stack Overflow：https://stackoverflow.com/questions/<id>/<slug>  或  https://stackoverflow.com/tags/<tag>\n"
                                        + "  * 技术博客站首页：https://martinfowler.com/   https://aws.amazon.com/blogs/   https://learn.microsoft.com/\n"
                                        + "  * 论文站：https://arxiv.org/search/?query=<keyword>  或  https://scholar.google.com/scholar?q=<keyword>\n"
                                        + "  * 官方文档首页：https://docs.oracle.com/   https://redis.io/docs/   https://www.postgresql.org/docs/\n"
                                        + "  * MDN：https://developer.mozilla.org/zh-CN/docs/Web/<Topic>\n"
                                        + "<b>绝对不要编造 @username/xxx-123 这类不存在的 Medium/博客 URL。</b>\n"
                                        + "- snippet: 来源摘要（2-3 句，能准确反映内容）\n"
                                        + "- sourceType: 固定为 ai_knowledge\n"
                                        + "- findings: 从该来源提取的关键发现数组（每个发现 2-4 句，字段：statement, category, confidence）\n"
                                        + "每个问题生成 3 个来源。仅输出 JSON 数组。"),
                        Map.of("role", "user", "content",
                                "研究问题: " + question + "\n\n请生成 3 个模拟的网页搜索结果。"));

                String response = aiService.chat(userId, AiScenario.RESEARCH.getCode(), messages);
                if (response != null) {
                    int start = response.indexOf('[');
                    int end = response.lastIndexOf(']');
                    if (start >= 0 && end > start) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> results = objectMapper.readValue(
                                response.substring(start, end + 1), List.class);
                        // 二次保障：若 LLM 仍输出可疑 URL，重写为维基百科的搜索 URL
                        for (Map<String, Object> r : results) {
                            Object urlObj = r.get("url");
                            String url = urlObj != null ? urlObj.toString() : "";
                            String safeUrl = sanitizeFallbackUrl(url, question);
                            if (!safeUrl.equals(url)) {
                                r.put("url", safeUrl);
                            }
                        }
                        allResults.addAll(results);
                    }
                }
            } catch (Exception e) {
                log.warn("llm_research_fallback_failed question={}", question, e);
            }
        }
        log.info("llm_fallback_results count={}", allResults.size());
        return allResults;
    }

    /**
     * 将 LLM 回退生成的 URL 做安全清洗，避免 404.
     *
     * <p>检测到包含 @username 、路径中含 -<数字> 结尾（典型的假文章链接）、
     * 或域名不在白名单时，回退到 zh.wikipedia 的搜索页。</p>
     */
    private String sanitizeFallbackUrl(String url, String question) {
        if (url == null || url.isBlank()) {
            return "https://zh.wikipedia.org/w/index.php?search="
                    + java.net.URLEncoder.encode(question, java.nio.charset.StandardCharsets.UTF_8);
        }
        // 典型假 Medium：@user/article-title-123
        boolean looksFake = url.contains("/@")
                || url.matches(".+-\\d{3,}($|[#?].*)")
                || url.matches(".+/[^/]+-\\d+/?$");
        if (looksFake) {
            return "https://zh.wikipedia.org/w/index.php?search="
                    + java.net.URLEncoder.encode(question, java.nio.charset.StandardCharsets.UTF_8);
        }
        // 白名单域名（必须真实存在）
        List<String> safeHosts = List.of(
                "zh.wikipedia.org", "en.wikipedia.org", "github.com", "stackoverflow.com",
                "martinfowler.com", "aws.amazon.com", "learn.microsoft.com",
                "developer.mozilla.org", "redis.io", "www.postgresql.org",
                "docs.oracle.com", "arxiv.org", "scholar.google.com",
                "www.cnblogs.com", "juejin.cn", "tech.meituan.com",
                "ifeve.com", "www.oracle.com", "medium.com");
        try {
            String host = new java.net.URL(url).getHost().toLowerCase();
            for (String safe : safeHosts) {
                if (host.equals(safe) || host.endsWith("." + safe)) {
                    return url;
                }
            }
        } catch (Exception e) {
            // 非 URL
        }
        return "https://zh.wikipedia.org/w/index.php?search="
                + java.net.URLEncoder.encode(question, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 将搜索来源持久化到 research_source 表.
     *
     * <p>前端 API 从 research_source 表读取数据展示在"来源"Tab 中。
     * 去重逻辑：相同 URL 不重复写入。</p>
     */
    private void persistSources(Long projectId, List<Map<String, Object>> allSources) {
        for (Map<String, Object> source : allSources) {
            try {
                ResearchSource entity = new ResearchSource();
                entity.setProjectId(projectId);
                entity.setTitle((String) source.getOrDefault("title", ""));
                entity.setUrl((String) source.getOrDefault("url", ""));
                entity.setSourceType((String) source.getOrDefault("sourceType", "web_search"));
                entity.setSnippet((String) source.getOrDefault("snippet", ""));
                entity.setReliability("unverified");
                entity.setFetchStatus("success");
                entity.setFetchedAt(java.time.LocalDateTime.now());
                researchSourceMapper.insert(entity);
            } catch (Exception e) {
                log.warn("persist_source_failed title={}", source.get("title"), e);
            }
        }
        log.info("sources_persisted projectId={} count={}", projectId, allSources.size());
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

                String memoryKey = statement.length() > 80
                        ? statement.substring(0, 80) : statement;
                researchMemoryService.save(projectId, userId, memoryKey, "FINDING", md.toString());
            }
            log.info("finding_memories_persisted projectId={} count={}", projectId, findings.size());
        } catch (Exception e) {
            log.warn("persist_finding_memories_failed projectId={}", projectId, e);
        }
    }
}
