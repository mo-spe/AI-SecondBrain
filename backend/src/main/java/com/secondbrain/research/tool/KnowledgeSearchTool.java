package com.secondbrain.research.tool;

import com.secondbrain.dto.GraphEdge;
import com.secondbrain.dto.KnowledgeGraph;
import com.secondbrain.service.CacheService;
import com.secondbrain.service.KnowledgeGraphService;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.vo.KnowledgeNodeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 知识库混合搜索工具.
 *
 * <p>组合语义搜索、关键词搜索和图谱扩展，返回融合排序后的结果。</p>
 *
 * @author AI
 */
@Component
public class KnowledgeSearchTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchTool.class);

    private final KnowledgeService knowledgeService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final CacheService cacheService;

    public KnowledgeSearchTool(KnowledgeService knowledgeService,
                               KnowledgeGraphService knowledgeGraphService,
                               CacheService cacheService) {
        this.knowledgeService = knowledgeService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.cacheService = cacheService;
    }

    @Override
    public String getName() {
        return "knowledge_search";
    }

    @Override
    public String getDescription() {
        return "搜索用户个人知识库，支持混合检索（语义+关键词+知识图谱扩展），返回融合排序后的结果";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "query", Map.of("type", "string", "description", "搜索查询字符串"),
                "searchMode", Map.of("type", "string", "enum", List.of("hybrid", "semantic", "keyword"),
                        "description", "搜索模式，默认 hybrid"),
                "topK", Map.of("type", "integer", "description", "返回结果数，默认 20，最大 50"),
                "expandByGraph", Map.of("type", "boolean", "description", "是否通过知识图谱扩展，默认 true")
        ));
        schema.put("required", List.of("query"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String query = (String) params.get("query");
        if (query == null || query.isBlank()) {
            return ToolResult.failure("INVALID_PARAM", "query 不能为空", false);
        }

        String searchMode = params.getOrDefault("searchMode", "hybrid").toString();
        int topK = params.containsKey("topK") ? ((Number) params.get("topK")).intValue() : 20;
        boolean expandByGraph = params.getOrDefault("expandByGraph", true).toString().equals("true");

        // userId 和 workspaceId 需要从 params 中获取（由 Agent 传入）
        Long userId = params.get("userId") instanceof Number
                ? ((Number) params.get("userId")).longValue() : null;
        Long workspaceId = params.get("workspaceId") instanceof Number
                ? ((Number) params.get("workspaceId")).longValue() : null;

        if (userId == null) {
            return ToolResult.failure("INVALID_PARAM", "userId 不能为空", false);
        }

        long startMs = System.currentTimeMillis();
        String cacheKey = "research:cache:search:" + userId + ":"
                + query.hashCode() + ":" + searchMode + ":" + topK;

        try {
            List<KnowledgeNodeVO> results = cacheService.getOrLoad(cacheKey,
                    (Class<List<KnowledgeNodeVO>>) (Object) List.class,
                    5, TimeUnit.MINUTES,
                    () -> hybridSearch(query, searchMode, topK, expandByGraph, userId, workspaceId));

            long duration = System.currentTimeMillis() - startMs;
            ToolResult result = ToolResult.success(formatResults(results));
            result.setDurationMs(duration);
            result.getMetadata().put("totalHits", results.size());
            result.getMetadata().put("searchMode", searchMode);
            return result;
        } catch (Exception e) {
            log.error("knowledge_search_failed query={} userId={}", query, userId, e);
            return ToolResult.failure("SEARCH_ERROR", e.getMessage(), true);
        }
    }

    /**
     * 混合搜索：并行执行语义搜索和关键词搜索，融合图谱扩展结果.
     */
    private List<KnowledgeNodeVO> hybridSearch(String query, String searchMode, int topK,
                                                boolean expandByGraph, Long userId, Long workspaceId) {
        List<KnowledgeNodeVO> semanticResults = new ArrayList<>();
        List<KnowledgeNodeVO> keywordResults = new ArrayList<>();

        if ("hybrid".equals(searchMode) || "semantic".equals(searchMode)) {
            try {
                semanticResults = knowledgeService.semanticSearch(query, userId, topK, workspaceId);
            } catch (Exception e) {
                log.warn("semantic_search_failed query={}", query, e);
            }
        }

        if ("hybrid".equals(searchMode) || "keyword".equals(searchMode)) {
            try {
                keywordResults = knowledgeService.multiFieldSearch(query, userId, workspaceId);
            } catch (Exception e) {
                log.warn("keyword_search_failed query={}", query, e);
            }
        }

        // 融合排序：semantic * 0.6 + keyword * 0.4
        Map<Long, Double> idToScore = new HashMap<>();
        for (int i = 0; i < semanticResults.size(); i++) {
            KnowledgeNodeVO node = semanticResults.get(i);
            double score = 0.6 * (1.0 - (double) i / semanticResults.size());
            idToScore.merge(node.getId(), score, Double::sum);
        }
        for (int i = 0; i < keywordResults.size(); i++) {
            KnowledgeNodeVO node = keywordResults.get(i);
            double score = 0.4 * (1.0 - (double) i / keywordResults.size());
            idToScore.merge(node.getId(), score, Double::sum);
        }

        // 图谱扩展
        Set<Long> expandedIds = new HashSet<>();
        if (expandByGraph) {
            try {
                KnowledgeGraph graph = knowledgeGraphService.getGraph(userId, workspaceId);
                Set<Long> existingIds = new HashSet<>(idToScore.keySet());
                for (GraphEdge edge : graph.getEdges()) {
                    try {
                        Long sourceId = Long.valueOf(edge.getSource());
                        Long targetId = Long.valueOf(edge.getTarget());
                        if (existingIds.contains(sourceId)
                                && !existingIds.contains(targetId)) {
                            expandedIds.add(targetId);
                            idToScore.put(targetId, 0.1);
                        }
                        if (existingIds.contains(targetId)
                                && !existingIds.contains(sourceId)) {
                            expandedIds.add(sourceId);
                            idToScore.put(sourceId, 0.1);
                        }
                    } catch (NumberFormatException e) {
                        log.debug("skip_non_numeric_edge source={} target={}",
                                edge.getSource(), edge.getTarget());
                    }
                }
            } catch (Exception e) {
                log.warn("graph_expansion_failed query={}", query, e);
            }
        }

        // 收集所有结果并按分数排序
        Set<Long> allIds = new HashSet<>(idToScore.keySet());
        List<KnowledgeNodeVO> allResults = new ArrayList<>(semanticResults);
        for (KnowledgeNodeVO kw : keywordResults) {
            if (allResults.stream().noneMatch(r -> r.getId().equals(kw.getId()))) {
                allResults.add(kw);
            }
        }

        for (KnowledgeNodeVO node : allResults) {
            node.setScore(idToScore.getOrDefault(node.getId(), 0.0));
        }

        allResults.sort(Comparator.comparingDouble(KnowledgeNodeVO::getScore).reversed());
        return allResults.subList(0, Math.min(topK, allResults.size()));
    }

    private String formatResults(List<KnowledgeNodeVO> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            KnowledgeNodeVO node = results.get(i);
            sb.append(String.format("[%d] %s (相关度: %.2f, 掌握度: %d/5)\n摘要: %s\n\n",
                    i + 1, node.getTitle(), node.getScore(), node.getMasteryLevel(),
                    node.getSummary() != null ? node.getSummary() : "无"));
        }
        return sb.toString();
    }
}
