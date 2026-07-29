package com.secondbrain.research.tool;

import com.secondbrain.dto.GraphEdge;
import com.secondbrain.dto.GraphNode;
import com.secondbrain.dto.KnowledgeGraph;
import com.secondbrain.service.KnowledgeGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识图谱工具.
 *
 * <p>提供图谱节点查询、邻居遍历、路径查找等图操作。
 * 底层复用 KnowledgeGraphService 获取图谱数据，在内存中执行图算法。</p>
 *
 * @author AI
 */
@Component
public class KnowledgeGraphTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphTool.class);

    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphTool(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Override
    public String getName() {
        return "knowledge_graph";
    }

    @Override
    public String getDescription() {
        return "知识图谱操作：查询节点、获取邻居、查找路径、提取子图";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "operation", Map.of("type", "string",
                        "enum", List.of("get_node", "get_neighbors", "find_path", "get_subgraph"),
                        "description", "操作类型"),
                "nodeId", Map.of("type", "string", "description", "节点ID"),
                "maxHops", Map.of("type", "integer", "description", "最大跳数"),
                "nodeIds", Map.of("type", "array", "description", "节点ID列表（用于 get_subgraph）")
        ));
        schema.put("required", List.of("operation"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String operation = (String) params.get("operation");
        Long userId = params.get("userId") instanceof Number
                ? ((Number) params.get("userId")).longValue() : null;
        Long workspaceId = params.get("workspaceId") instanceof Number
                ? ((Number) params.get("workspaceId")).longValue() : null;

        if (userId == null) {
            return ToolResult.failure("INVALID_PARAM", "userId 不能为空", false);
        }

        try {
            KnowledgeGraph graph = knowledgeGraphService.getGraph(userId, workspaceId);

            return switch (operation) {
                case "get_node" -> executeGetNode(graph, params);
                case "get_neighbors" -> executeGetNeighbors(graph, params);
                case "find_path" -> executeFindPath(graph, params);
                case "get_subgraph" -> executeGetSubgraph(graph, params);
                default -> ToolResult.failure("INVALID_OPERATION", "不支持的操作: " + operation, false);
            };
        } catch (Exception e) {
            log.error("knowledge_graph_tool_failed op={} userId={}", operation, userId, e);
            return ToolResult.failure("GRAPH_ERROR", e.getMessage(), true);
        }
    }

    /**
     * 获取单个节点信息.
     */
    private ToolResult executeGetNode(KnowledgeGraph graph, Map<String, Object> params) {
        String nodeId = (String) params.get("nodeId");
        if (nodeId == null) {
            return ToolResult.failure("INVALID_PARAM", "nodeId 不能为空", false);
        }

        GraphNode node = graph.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElse(null);

        if (node == null) {
            return ToolResult.failure("NOT_FOUND", "节点不存在: " + nodeId, false);
        }

        List<GraphEdge> edges = graph.getEdges().stream()
                .filter(e -> e.getSource().equals(nodeId) || e.getTarget().equals(nodeId))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("node", node);
        result.put("relations", edges);
        result.put("degree", edges.size());

        return ToolResult.success(formatNodeResult(node, edges));
    }

    /**
     * BFS 获取邻居节点.
     */
    private ToolResult executeGetNeighbors(KnowledgeGraph graph, Map<String, Object> params) {
        String nodeId = (String) params.get("nodeId");
        int maxHops = params.get("maxHops") instanceof Number
                ? ((Number) params.get("maxHops")).intValue() : 1;

        if (nodeId == null) {
            return ToolResult.failure("INVALID_PARAM", "nodeId 不能为空", false);
        }

        // 构建邻接表
        Map<String, List<String>> adjacency = buildAdjacency(graph);

        // BFS
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> distances = new HashMap<>();

        queue.add(nodeId);
        visited.add(nodeId);
        distances.put(nodeId, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int dist = distances.get(current);
            if (dist >= maxHops) continue;

            for (String neighbor : adjacency.getOrDefault(current, List.of())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    distances.put(neighbor, dist + 1);
                    queue.add(neighbor);
                }
            }
        }

        visited.remove(nodeId);

        List<GraphNode> neighborNodes = graph.getNodes().stream()
                .filter(n -> visited.contains(n.getId()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("节点 ").append(nodeId).append(" 的邻居（")
                .append(maxHops).append(" 跳内）:\n");
        for (GraphNode n : neighborNodes) {
            Integer dist = distances.get(n.getId());
            sb.append("  [").append(dist).append("跳] ").append(n.getLabel())
                    .append(" (类型: ").append(n.getType()).append(")\n");
        }

        ToolResult result = ToolResult.success(sb.toString());
        result.getMetadata().put("neighborCount", neighborNodes.size());
        result.getMetadata().put("maxHops", maxHops);
        return result;
    }

    /**
     * 查找两节点间的最短路径（BFS，以边强度为权重）.
     */
    private ToolResult executeFindPath(KnowledgeGraph graph, Map<String, Object> params) {
        String fromId = (String) params.get("fromId");
        String toId = (String) params.get("toId");

        if (fromId == null || toId == null) {
            return ToolResult.failure("INVALID_PARAM", "fromId 和 toId 不能为空", false);
        }

        Map<String, List<String>> adjacency = buildAdjacency(graph);

        // BFS 最短路径
        Queue<String> queue = new LinkedList<>();
        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.add(fromId);
        visited.add(fromId);

        boolean found = false;
        while (!queue.isEmpty() && !found) {
            String current = queue.poll();
            for (String neighbor : adjacency.getOrDefault(current, List.of())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                    if (neighbor.equals(toId)) {
                        found = true;
                        break;
                    }
                }
            }
        }

        if (!found) {
            return ToolResult.success("节点 " + fromId + " 到 " + toId + " 之间无路径");
        }

        // 回溯路径
        List<String> path = new ArrayList<>();
        String current = toId;
        while (current != null) {
            path.add(0, current);
            current = parent.get(current);
        }

        List<GraphNode> pathNodes = graph.getNodes().stream()
                .filter(n -> path.contains(n.getId()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("最短路径（").append(path.size() - 1).append(" 步）:\n");
        for (int i = 0; i < path.size(); i++) {
            String id = path.get(i);
            GraphNode node = pathNodes.stream()
                    .filter(n -> n.getId().equals(id))
                    .findFirst().orElse(null);
            String label = node != null ? node.getLabel() : id;
            sb.append("  ").append(i + 1).append(". ").append(label);
            if (i < path.size() - 1) sb.append(" →\n");
        }

        return ToolResult.success(sb.toString());
    }

    /**
     * 提取指定节点集的子图.
     */
    private ToolResult executeGetSubgraph(KnowledgeGraph graph, Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> nodeIds = (List<String>) params.get("nodeIds");

        if (nodeIds == null || nodeIds.isEmpty()) {
            return ToolResult.failure("INVALID_PARAM", "nodeIds 不能为空", false);
        }

        Set<String> idSet = new HashSet<>(nodeIds);

        List<GraphNode> subNodes = graph.getNodes().stream()
                .filter(n -> idSet.contains(n.getId()))
                .toList();

        List<GraphEdge> subEdges = graph.getEdges().stream()
                .filter(e -> idSet.contains(e.getSource()) && idSet.contains(e.getTarget()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("子图: ").append(subNodes.size()).append(" 节点, ")
                .append(subEdges.size()).append(" 边\n");
        for (GraphNode n : subNodes) {
            sb.append("  - ").append(n.getLabel())
                    .append(" (").append(n.getType()).append(")\n");
        }

        return ToolResult.success(sb.toString());
    }

    /**
     * 构建无向邻接表.
     */
    private Map<String, List<String>> buildAdjacency(KnowledgeGraph graph) {
        Map<String, List<String>> adjacency = new HashMap<>();
        for (GraphEdge edge : graph.getEdges()) {
            adjacency.computeIfAbsent(edge.getSource(), k -> new ArrayList<>())
                    .add(edge.getTarget());
            adjacency.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>())
                    .add(edge.getSource());
        }
        return adjacency;
    }

    private String formatNodeResult(GraphNode node, List<GraphEdge> edges) {
        StringBuilder sb = new StringBuilder();
        sb.append("节点: ").append(node.getLabel()).append("\n");
        sb.append("类型: ").append(node.getType()).append("\n");
        sb.append("重要度: ").append(node.getImportance()).append("\n");
        sb.append("掌握度: ").append(node.getMasteryLevel()).append("\n");
        sb.append("关联边 (").append(edges.size()).append(" 条):\n");
        for (GraphEdge e : edges) {
            String other = e.getSource().equals(node.getId()) ? e.getTarget() : e.getSource();
            sb.append("  - ").append(e.getLabel()).append(" → ").append(other).append("\n");
        }
        return sb.toString();
    }
}
