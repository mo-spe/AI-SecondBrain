# AI Research Agent — Tools 设计文档

> 基于架构设计：`ai-research-agent-architecture.md`
> 基于工作流设计：`ai-research-agent-workflow.md`
> 基于数据库设计：`ai-research-agent-database.md`
> 日期：2026-07-28
> 版本：V1

---

## 〇、Tool 接口定义

```java
package com.secondbrain.research.tool;

/**
 * Agent Tool 接口。
 *
 * 所有 Tool 必须实现此接口。Tool 是无状态的 —— 每次调用独立，
 * 所有上下文通过 input 参数和 AgentContext 传递。
 */
public interface Tool {

    /** Tool 唯一标识 */
    String getName();

    /** Tool 描述（用于 LLM function calling 的 description 字段） */
    String getDescription();

    /** 输入参数 Schema（JSON Schema 格式，用于 LLM function calling） */
    ToolInputSchema getInputSchema();

    /**
     * 执行 Tool。
     *
     * @param input 输入参数（key-value，由 Orchestrator 根据 InputSchema 校验后传入）
     * @param context 当前 Agent 执行上下文
     * @return Tool 执行结果
     */
    ToolResult execute(Map<String, Object> input, AgentContext context);

    /** 是否需要用户级权限校验 */
    default boolean requiresAuth() { return true; }

    /** 是否幂等（相同 input 多次调用结果一致） */
    default boolean isIdempotent() { return false; }

    /** 默认超时时间（毫秒） */
    default long getTimeoutMs() { return 10_000; }

    /** 最大重试次数 */
    default int getMaxRetries() { return 1; }

    /** 重试退避策略（毫秒） */
    default long getRetryBackoffMs() { return 1000; }
}

/**
 * Tool 输入 Schema
 */
public class ToolInputSchema {
    String type = "object";
    Map<String, PropertyDef> properties;
    List<String> required;

    public record PropertyDef(String type, String description, Object defaultValue) {}
}

/**
 * Tool 执行结果
 */
public class ToolResult {
    boolean success;
    Object data;              // 成功时的结构化数据
    String errorCode;         // 失败时的错误码
    String errorMessage;      // 失败时的人类可读错误信息
    boolean retryable;        // 是否可重试
    long durationMs;          // 执行耗时
    Map<String, Object> metadata;  // 额外元数据（token 消耗、命中缓存等）
}
```

---

## 一、KnowledgeSearchTool

### 1.1 基本信息

| 属性 | 值 |
|------|-----|
| **Tool Name** | `knowledge_search` |
| **Description** | 在用户个人知识库中执行混合搜索（关键词 + 语义 + 图谱扩展）。返回与查询最相关的知识点列表，含相关性评分和掌握度。 |
| **权限** | 需要 userId + workspaceId（从 AgentContext 提取） |
| **幂等** | 是（相同查询在 5 分钟内返回相同结果，使用 Redis 缓存） |
| **超时** | 10,000ms |
| **重试** | 最多 1 次，线性退避 2s |

### 1.2 Input Schema

```json
{
  "type": "object",
  "properties": {
    "query": {
      "type": "string",
      "description": "搜索查询文本。支持自然语言问题或关键词。示例: 'Spring AI Agent Tool Calling 机制'"
    },
    "searchMode": {
      "type": "string",
      "enum": ["keyword", "semantic", "hybrid"],
      "description": "搜索模式：keyword(仅ES关键词)、semantic(仅向量语义)、hybrid(两者融合，默认)",
      "default": "hybrid"
    },
    "topK": {
      "type": "integer",
      "description": "返回结果数上限",
      "default": 20,
      "minimum": 1,
      "maximum": 50
    },
    "expandByGraph": {
      "type": "boolean",
      "description": "是否通过知识图谱扩展结果（查询相邻节点）",
      "default": true
    },
    "graphHopCount": {
      "type": "integer",
      "description": "图谱扩展跳数（仅 expandByGraph=true 时生效）",
      "default": 1,
      "minimum": 1,
      "maximum": 2
    },
    "filterByTags": {
      "type": "array",
      "items": { "type": "string" },
      "description": "按标签过滤（标签名列表）",
      "default": []
    },
    "filterByMastery": {
      "type": "object",
      "properties": {
        "min": { "type": "integer", "minimum": 0, "maximum": 5 },
        "max": { "type": "integer", "minimum": 0, "maximum": 5 }
      },
      "description": "按掌握度范围过滤"
    },
    "minRelevanceScore": {
      "type": "number",
      "description": "最低相关性分数阈值 (0.0-1.0)",
      "default": 0.3,
      "minimum": 0.0,
      "maximum": 1.0
    }
  },
  "required": ["query"]
}
```

### 1.3 Output Schema

```json
{
  "type": "object",
  "properties": {
    "results": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "nodeId": { "type": "integer" },
          "title": { "type": "string" },
          "summary": { "type": "string" },
          "snippet": { "type": "string", "description": "匹配的文本片段" },
          "relevanceScore": { "type": "number", "description": "综合相关性评分 0-1" },
          "semanticScore": { "type": "number", "description": "语义相似度" },
          "keywordScore": { "type": "number", "description": "关键词匹配分数" },
          "graphScore": { "type": "number", "description": "图谱距离加权分数" },
          "masteryLevel": { "type": "integer", "description": "用户掌握度 0-5" },
          "importance": { "type": "integer", "description": "重要程度 1-5" },
          "tags": { "type": "array", "items": { "type": "string" } },
          "relationToQuery": {
            "type": "string",
            "enum": ["direct_match", "graph_neighbor", "prerequisite", "related"],
            "description": "与查询的关系类型"
          }
        }
      }
    },
    "searchStats": {
      "type": "object",
      "properties": {
        "mode": { "type": "string", "description": "实际使用的搜索模式" },
        "keywordResults": { "type": "integer", "description": "关键词搜索结果数" },
        "semanticResults": { "type": "integer", "description": "语义搜索结果数" },
        "graphExpandedNodes": { "type": "integer", "description": "图谱扩展节点数" },
        "totalUniqueResults": { "type": "integer" },
        "searchDurationMs": { "type": "integer" },
        "cacheHit": { "type": "boolean" }
      }
    },
    "degradedMode": {
      "type": "boolean",
      "description": "是否在降级模式下执行（部分渠道不可用）"
    },
    "degradedReason": {
      "type": "string",
      "description": "降级原因（如 'ES unavailable, using semantic only'）"
    }
  }
}
```

### 1.4 内部实现逻辑

```
KnowledgeSearchTool.execute(input, context)
    │
    ├── 1. 检查 Redis 缓存
    │      key: research:cache:search:{userId}:{MD5(query+mode+topK)}
    │      hit → 直接返回（含 searchStats.cacheHit=true）
    │
    ├── 2. 并行执行搜索
    │      IF mode=keyword OR hybrid:
    │        Thread A: ElasticsearchService.multiFieldSearch(query, userId)
    │                   → 映射到 KnowledgeDocument → 提取 keywordScore
    │      IF mode=semantic OR hybrid:
    │        Thread B: VectorSearchService.searchSimilar(query, userId, topK*2)
    │                   → 映射到 KnowledgeReference → 提取 semanticScore
    │      IF 某渠道失败:
    │        → 不阻塞，另一渠道继续
    │        → 标记 degradedMode=true
    │
    ├── 3. 结果融合（仅 hybrid 模式）
    │      a. 合并两个结果集（按 nodeId 去重）
    │      b. 综合评分: score = semanticScore × 0.5 + keywordScore × 0.3
    │      c. 排序 → 截取 topK
    │
    ├── 4. 图谱扩展（if expandByGraph=true）
    │      a. 取 topK 中的 nodeId 列表
    │      b. 调用 KnowledgeGraphService.getGraph(userId, workspaceId)
    │         → 在内存中遍历 {nodeId} 的 graphHopCount 跳邻居
    │      c. 邻居节点加权: graphScore = 1.0 / (hopDistance + 1)
    │      d. 合并到结果集 → 重排序
    │      e. 邻居节点标记 relationToQuery="graph_neighbor"
    │
    ├── 5. 过滤
    │      - filterByTags: 调用 KnowledgeTagService.listByNode() 检查
    │      - filterByMastery: 直接比较
    │      - minRelevanceScore: 过滤低分结果
    │
    ├── 6. 缓存结果（TTL=5min）→ 返回
    │
    └── 7. 空结果处理
           results=[] → success=true（不是错误，表示知识库中无相关内容）
           searchStats.totalUniqueResults=0
```

### 1.5 异常处理

| 异常 | 错误码 | 处理 |
|------|--------|------|
| ES 不可用 | `ES_UNAVAILABLE` | 降级为纯 semantic 模式；两者都不可用 → 返回空结果 |
| Vector Search 不可用 | `VECTOR_UNAVAILABLE` | 降级为纯 keyword 模式；两者都不可用 → 返回空结果 |
| 图谱服务不可用 | `GRAPH_UNAVAILABLE` | expandByGraph 自动跳过，标记 degradedMode |
| userId 缺失 | `AUTH_REQUIRED` | 返回失败，retryable=false |
| query 为空 | `INVALID_INPUT` | 返回失败，retryable=false |
| Redis 缓存读写失败 | （内部吞掉） | 不影响搜索执行，仅跳过缓存 |

---

## 二、KnowledgeDetailTool

### 2.1 基本信息

| 属性 | 值 |
|------|-----|
| **Tool Name** | `knowledge_detail` |
| **Description** | 读取单个知识点的完整内容（含 Markdown 正文、标签、图谱关系）。用于 Agent 需要深入理解某个知识点时。 |
| **权限** | 需要 userId + workspaceId |
| **幂等** | 是（读取操作） |
| **超时** | 5,000ms |
| **重试** | 最多 1 次，线性退避 1s |

### 2.2 Input Schema

```json
{
  "type": "object",
  "properties": {
    "nodeId": {
      "type": "integer",
      "description": "知识点 ID"
    },
    "includeContent": {
      "type": "boolean",
      "description": "是否包含完整 Markdown 正文",
      "default": true
    },
    "includeRelations": {
      "type": "boolean",
      "description": "是否包含图谱关系（相邻节点 + 关系类型）",
      "default": false
    },
    "includeTags": {
      "type": "boolean",
      "description": "是否包含标签信息",
      "default": true
    },
    "includeReviewStats": {
      "type": "boolean",
      "description": "是否包含复习统计（复习次数、正确率、下次复习时间）",
      "default": false
    },
    "maxRelatedNodes": {
      "type": "integer",
      "description": "最大关联节点数（仅 includeRelations=true 时生效）",
      "default": 10,
      "minimum": 1,
      "maximum": 30
    }
  },
  "required": ["nodeId"]
}
```

### 2.3 Output Schema

```json
{
  "type": "object",
  "properties": {
    "node": {
      "type": "object",
      "properties": {
        "id": { "type": "integer" },
        "title": { "type": "string" },
        "summary": { "type": "string" },
        "contentMd": { "type": "string", "description": "Markdown 正文（仅 includeContent=true）" },
        "importance": { "type": "integer" },
        "masteryLevel": { "type": "integer" },
        "reviewCount": { "type": "integer" },
        "nextReviewTime": { "type": "string", "format": "date-time" },
        "createTime": { "type": "string", "format": "date-time" },
        "tags": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "tagId": { "type": "integer" },
              "tagName": { "type": "string" },
              "tagColor": { "type": "string" }
            }
          }
        }
      }
    },
    "relations": {
      "type": "array",
      "description": "图谱关系（仅 includeRelations=true）",
      "items": {
        "type": "object",
        "properties": {
          "targetNodeId": { "type": "integer" },
          "targetTitle": { "type": "string" },
          "relationType": { "type": "string", "enum": ["contains", "depends", "related", "inherits", "implements", "derived_from", "supports", "contradicts"] },
          "relationName": { "type": "string" },
          "strength": { "type": "integer" }
        }
      }
    },
    "reviewStats": {
      "type": "object",
      "description": "复习统计（仅 includeReviewStats=true）",
      "properties": {
        "totalReviews": { "type": "integer" },
        "correctRate": { "type": "number" },
        "lastReviewTime": { "type": "string", "format": "date-time" },
        "nextReviewTime": { "type": "string", "format": "date-time" },
        "streakDays": { "type": "integer" }
      }
    }
  }
}
```

### 2.4 内部实现逻辑

```
KnowledgeDetailTool.execute(input, context)
    │
    ├── 1. 调用 KnowledgeService.getById(nodeId, userId, workspaceId)
    │      → 返回 KnowledgeNodeVO（含 tags 字段，由 getById 自动 JOIN 填充）
    │      → 如果 nodeId 不存在 → 返回 { success: false, errorCode: "NOT_FOUND" }
    │
    ├── 2. 如果 includeRelations=true:
    │      a. KnowledgeGraphService.getGraph(userId, workspaceId)
    │      b. 在 graph 中定位 nodeId 的边
    │      c. 过滤出相邻节点 → 按 strength 排序 → 截取 maxRelatedNodes
    │      d. 批量查询相邻节点的 title（从 KnowledgeNodeVO 或 graph 中获取）
    │
    ├── 3. 如果 includeReviewStats=true:
    │      ReviewCardService 查询该 node 的复习卡片统计
    │
    └── 4. 返回完整结果
```

### 2.5 异常处理

| 异常 | 错误码 | 处理 |
|------|--------|------|
| nodeId 不存在 | `NOT_FOUND` | 返回失败，retryable=false |
| 无权访问（不属于该用户/workspace） | `FORBIDDEN` | 返回失败，retryable=false |
| 图谱服务不可用 | `GRAPH_UNAVAILABLE` | relations 返回空数组，不阻塞主流程 |
| contentMd 为空 | — | 正常返回，contentMd="" |

---

## 三、KnowledgeGraphTool

### 3.1 基本信息

| 属性 | 值 |
|------|-----|
| **Tool Name** | `knowledge_graph` |
| **Description** | 查询用户知识图谱。支持：查询实体节点、查询关系边、查询邻居节点、查询两节点间最短路径。用于 Gap Agent 发现知识盲区和隐性关联。 |
| **权限** | 需要 userId + workspaceId |
| **幂等** | 是（读取操作） |
| **超时** | 10,000ms |
| **重试** | 最多 1 次，退避 2s |

### 3.2 Input Schema

```json
{
  "type": "object",
  "properties": {
    "operation": {
      "type": "string",
      "enum": ["get_node", "get_relations", "get_neighbors", "find_path", "get_subgraph"],
      "description": "操作类型"
    },
    "nodeId": {
      "type": "integer",
      "description": "目标节点 ID（get_node / get_neighbors / get_relations 使用）"
    },
    "nodeIds": {
      "type": "array",
      "items": { "type": "integer" },
      "description": "节点 ID 列表（get_subgraph 使用）"
    },
    "sourceNodeId": {
      "type": "integer",
      "description": "起始节点 ID（find_path 使用）"
    },
    "targetNodeId": {
      "type": "integer",
      "description": "目标节点 ID（find_path 使用）"
    },
    "relationTypes": {
      "type": "array",
      "items": { "type": "string" },
      "description": "过滤关系类型（空=全部）",
      "default": []
    },
    "hopCount": {
      "type": "integer",
      "description": "邻居查询跳数（get_neighbors 使用）",
      "default": 1,
      "minimum": 1,
      "maximum": 3
    },
    "maxResults": {
      "type": "integer",
      "description": "最大返回节点数",
      "default": 30,
      "minimum": 1,
      "maximum": 100
    },
    "includeIsolated": {
      "type": "boolean",
      "description": "是否包含孤立节点（get_subgraph 使用）",
      "default": false
    }
  },
  "required": ["operation"]
}
```

### 3.3 Output Schema

```json
{
  "type": "object",
  "properties": {
    "operation": { "type": "string", "description": "实际执行的操作" },
    "nodes": {
      "type": "array",
      "description": "节点列表",
      "items": {
        "type": "object",
        "properties": {
          "id": { "type": "string" },
          "label": { "type": "string", "description": "节点标题" },
          "type": { "type": "string", "description": "节点类型（knowledge/tag/category）" },
          "importance": { "type": "integer" },
          "masteryLevel": { "type": "integer" },
          "tags": { "type": "array", "items": { "type": "string" } }
        }
      }
    },
    "edges": {
      "type": "array",
      "description": "边列表",
      "items": {
        "type": "object",
        "properties": {
          "sourceId": { "type": "string" },
          "targetId": { "type": "string" },
          "relationType": { "type": "string" },
          "label": { "type": "string" },
          "strength": { "type": "integer" }
        }
      }
    },
    "path": {
      "type": "array",
      "description": "节点路径（仅 find_path 操作）",
      "items": {
        "type": "object",
        "properties": {
          "nodeId": { "type": "string" },
          "label": { "type": "string" },
          "relationToNext": { "type": "string" }
        }
      }
    },
    "pathLength": {
      "type": "integer",
      "description": "路径长度（边数，仅 find_path）"
    },
    "stats": {
      "type": "object",
      "properties": {
        "totalNodes": { "type": "integer" },
        "totalEdges": { "type": "integer" },
        "graphDensity": { "type": "number", "description": "图密度" }
      }
    }
  }
}
```

### 3.4 各操作内部实现

```
操作 1: get_node
  1. KnowledgeService.getById(nodeId) → 组装 GraphNode
  2. 返回 { nodes: [node], edges: [] }

操作 2: get_relations
  1. KnowledgeGraphService.getGraph(userId, workspaceId)
  2. 过滤 edges (sourceId=nodeId 或 targetId=nodeId)
  3. 可选按 relationTypes 过滤
  4. 返回 { nodes: [相关节点], edges: [匹配的边] }

操作 3: get_neighbors
  1. KnowledgeGraphService.getGraph(userId, workspaceId)
  2. BFS 从 nodeId 出发，遍历 hopCount 跳
  3. 每层邻居按边 strength 排序 → 截取 maxResults
  4. 返回 { nodes: [neighbors], edges: [相关边] }

操作 4: find_path
  1. KnowledgeGraphService.getGraph(userId, workspaceId)
  2. 内存中运行 BFS/Dijkstra 最短路径（基于边 strength 倒数作为权重）
  3. 路径 > 5 跳 → 返回 { found: false, reason: "PATH_TOO_LONG" }
  4. 返回 { nodes: [], edges: [], path: [有序节点列表], pathLength: N }

操作 5: get_subgraph
  1. KnowledgeGraphService.getGraph(userId, workspaceId)
  2. 过滤：仅保留 nodeIds 中的节点 + 它们之间的边
  3. 可选 includeIsolated: 保留没有边连接的节点
  4. 返回 { nodes: [子图节点], edges: [子图边] }
```

### 3.5 异常处理

| 异常 | 错误码 | 处理 |
|------|--------|------|
| 图谱服务不可用 | `GRAPH_UNAVAILABLE` | 返回失败，retryable=true |
| 节点不存在 | `NODE_NOT_FOUND` | 返回失败，retryable=false |
| 路径不存在（两节点不连通） | `PATH_NOT_FOUND` | 返回 { found: false, reason: "NO_PATH" }（success=true） |
| 图过大（>500 节点） | `GRAPH_TOO_LARGE` | 采样模式：随机选取 200 个节点 + 它们的边 |

---

## 四、ResearchHistoryTool

### 4.1 基本信息

| 属性 | 值 |
|------|-----|
| **Tool Name** | `research_history` |
| **Description** | 查询用户的历史研究记录。支持查询：历史研究项目列表、与当前研究主题相关的研究、历史研究结论。用于 Planner 和 Synthesizer 避免重复研究。 |
| **权限** | 需要 userId + workspaceId |
| **幂等** | 是（读取操作，但结果随时间变化） |
| **超时** | 5,000ms |
| **重试** | 最多 1 次，退避 1s |

### 4.2 Input Schema

```json
{
  "type": "object",
  "properties": {
    "operation": {
      "type": "string",
      "enum": ["list_recent", "find_related", "get_conclusions", "get_by_id"],
      "description": "操作类型"
    },
    "limit": {
      "type": "integer",
      "description": "返回记录数上限（list_recent/find_related 使用）",
      "default": 10,
      "minimum": 1,
      "maximum": 50
    },
    "topic": {
      "type": "string",
      "description": "研究主题关键词（find_related 使用）"
    },
    "projectId": {
      "type": "integer",
      "description": "研究项目 ID（get_conclusions/get_by_id 使用）"
    },
    "status": {
      "type": "string",
      "enum": ["COMPLETED", "FAILED", "ARCHIVED"],
      "description": "按状态过滤（list_recent 使用）",
      "default": "COMPLETED"
    },
    "includeConclusions": {
      "type": "boolean",
      "description": "是否包含研究结论（list_recent/find_related 使用）",
      "default": false
    }
  },
  "required": ["operation"]
}
```

### 4.3 Output Schema

```json
{
  "type": "object",
  "properties": {
    "operation": { "type": "string" },
    "projects": {
      "type": "array",
      "description": "研究项目列表",
      "items": {
        "type": "object",
        "properties": {
          "id": { "type": "integer" },
          "title": { "type": "string" },
          "goal": { "type": "string" },
          "status": { "type": "string" },
          "complexity": { "type": "string" },
          "resultSummary": { "type": "string" },
          "keyFindingCount": { "type": "integer" },
          "sourceCount": { "type": "integer" },
          "startedAt": { "type": "string", "format": "date-time" },
          "completedAt": { "type": "string", "format": "date-time" },
          "relevanceScore": { "type": "number", "description": "与当前 topic 的相关度（仅 find_related）" }
        }
      }
    },
    "conclusions": {
      "type": "array",
      "description": "历史结论列表",
      "items": {
        "type": "object",
        "properties": {
          "id": { "type": "integer" },
          "projectId": { "type": "integer" },
          "projectTitle": { "type": "string" },
          "statement": { "type": "string" },
          "confidence": { "type": "string" },
          "isKeyFinding": { "type": "boolean" },
          "sourceCount": { "type": "integer" },
          "createdAt": { "type": "string", "format": "date-time" }
        }
      }
    },
    "totalCount": { "type": "integer" },
    "hasMore": { "type": "boolean" }
  }
}
```

### 4.4 内部实现逻辑

```
操作 1: list_recent
  1. ResearchProjectMapper.selectList(wrapper):
       - where user_id=userId
       - where status=status (default: COMPLETED)
       - order by completed_at DESC
       - limit
  2. 如果 includeConclusions=true:
       - 批量查询每个 project 的 key_finding conclusions
       - ResearchConclusionMapper.selectList(project_id in [...] AND is_key_finding=1)
  3. 返回

操作 2: find_related
  1. 全量查询该用户所有 COMPLETED 项目
  2. 对每个项目的 title+goal 与 topic 做语义匹配:
       a. 生成 topic 的 embedding
       b. 与每个项目的 title embedding 计算 cosine 相似度
       c. 筛选 similarity > 0.4 的项目
  3. 按 relevanceScore 降序 → limit
  4. 返回（附带结论如果 includeConclusions=true）

操作 3: get_conclusions
  1. ResearchConclusionMapper.selectList(wrapper):
       - where project_id=projectId
       - order by is_key_finding DESC, confidence
  2. 返回

操作 4: get_by_id
  1. ResearchProjectMapper.selectById(projectId)
  2. 校验 userId 权限
  3. 返回单个项目详情
```

### 4.5 异常处理

| 异常 | 错误码 | 处理 |
|------|--------|------|
| 用户无历史研究 | — | 返回空数组（success=true） |
| projectId 不存在 | `NOT_FOUND` | 返回失败，retryable=false |
| topic 为空（find_related） | `INVALID_INPUT` | 返回失败，retryable=false |

---

## 五、WebSearchTool

### 5.1 基本信息

| 属性 | 值 |
|------|-----|
| **Tool Name** | `web_search` |
| **Description** | 在互联网上搜索信息，返回相关网页的 URL、标题和摘要。支持多 Search API 后端，自动 fallback。结果带缓存。 |
| **权限** | 需要 userId（用于 rate limiting 和个性化过滤） |
| **幂等** | 是（相同查询 1 小时内返回缓存结果） |
| **超时** | 10,000ms |
| **重试** | 最多 2 次，指数退避 1s, 3s |

### 5.2 Input Schema

```json
{
  "type": "object",
  "properties": {
    "query": {
      "type": "string",
      "description": "搜索查询词。应为简洁的关键词组合，而非自然语言问题。示例: 'Spring AI Agent tool calling annotation'",
      "maxLength": 200
    },
    "maxResults": {
      "type": "integer",
      "description": "最大返回结果数",
      "default": 10,
      "minimum": 1,
      "maximum": 20
    },
    "searchType": {
      "type": "string",
      "enum": ["general", "news", "scholar", "code"],
      "description": "搜索类型：general(通用)、news(新闻，最近 30 天)、scholar(学术)、code(代码仓库)",
      "default": "general"
    },
    "language": {
      "type": "string",
      "enum": ["zh", "en", "auto"],
      "description": "搜索语言偏好",
      "default": "auto"
    },
    "safeSearch": {
      "type": "boolean",
      "description": "安全搜索",
      "default": true
    }
  },
  "required": ["query"]
}
```

### 5.3 Output Schema

```json
{
  "type": "object",
  "properties": {
    "results": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "title": { "type": "string", "description": "网页标题" },
          "url": { "type": "string", "description": "网页 URL" },
          "snippet": { "type": "string", "description": "搜索结果摘要" },
          "source": { "type": "string", "description": "来源域名（如 docs.spring.io）" },
          "publishedDate": { "type": "string", "description": "发布日期（如有）" },
          "rank": { "type": "integer", "description": "搜索排名" }
        }
      }
    },
    "searchMetadata": {
      "type": "object",
      "properties": {
        "query": { "type": "string" },
        "searchEngine": { "type": "string", "description": "实际使用的搜索引擎" },
        "totalEstimatedResults": { "type": "integer" },
        "searchDurationMs": { "type": "integer" },
        "cacheHit": { "type": "boolean" }
      }
    }
  }
}
```

### 5.4 内部实现逻辑

```
WebSearchTool.execute(input, context)
    │
    ├── 1. 检查 Redis 缓存
    │      key: research:cache:search:{MD5(query+searchType+language)}
    │      TTL: 1h
    │
    ├── 2. Rate Limiting 检查
    │      Redis: research:rate:websearch:{userId} → 30 次/小时
    │
    ├── 3. 多级 Search API fallback:
    │      Primary: Brave Search API (如果用户配置了 API key)
    │        └─ 失败/不可用 → Fallback 1: SerpAPI
    │             └─ 失败/不可用 → Fallback 2: DuckDuckGo (无 API key 要求)
    │                  └─ 失败 → 返回空结果 + searchUnavailable: true
    │
    ├── 4. 解析搜索结果
    │      - 标准化 title, url, snippet
    │      - 提取 source 域名
    │      - 去重（相同 URL）
    │      - 过滤：排除已知低质量域名的 URL（可配置黑名单）
    │
    ├── 5. 缓存 → 返回
    │
    └── 注意: 这是轻量搜索，仅返回 URL + snippet。
            完整页面内容由 WebFetchTool 单独抓取。
```

### 5.5 异常处理

| 异常 | 错误码 | 处理 |
|------|--------|------|
| 所有 Search API 均不可用 | `SEARCH_UNAVAILABLE` | 返回空结果 + searchUnavailable=true，success=true |
| Rate Limit 触发 | `RATE_LIMITED` | 返回失败，retryable=true（2 次退避后仍失败 → retryable=false） |
| 查询为空 | `INVALID_INPUT` | 返回失败，retryable=false |
| 搜索超时 (10s) | `SEARCH_TIMEOUT` | 重试下一个 fallback 搜索引擎 |

---

## 六、WebFetchTool

### 6.1 基本信息

| 属性 | 值 |
|------|-----|
| **Tool Name** | `web_fetch` |
| **Description** | 抓取指定 URL 的网页完整正文内容。返回清洗后的纯文本/Markdown。用于 Agent 需要仔细阅读某个网页时。支持批量抓取（最多 5 个 URL 并行）。 |
| **权限** | 需要 userId（用于 rate limiting） |
| **幂等** | 是（相同 URL 24 小时内返回缓存内容；content_hash 用于去重） |
| **超时** | 15,000ms（单 URL）；60,000ms（批量 5 URL） |
| **重试** | 单个 URL 不重试（dead link 重试无意义） |

### 6.2 Input Schema

```json
{
  "type": "object",
  "properties": {
    "urls": {
      "type": "array",
      "items": { "type": "string", "format": "uri" },
      "description": "要抓取的 URL 列表",
      "maxItems": 5
    },
    "extractMode": {
      "type": "string",
      "enum": ["text", "markdown", "html"],
      "description": "提取格式：text(纯文本)、markdown(保留基本格式)、html(原始HTML片段)",
      "default": "text"
    },
    "maxContentLength": {
      "type": "integer",
      "description": "单页最大内容长度（字符数），超出截断",
      "default": 100000,
      "minimum": 1000,
      "maximum": 500000
    },
    "includeMetadata": {
      "type": "boolean",
      "description": "是否包含网页元数据（标题、描述、发布日期）",
      "default": true
    }
  },
  "required": ["urls"]
}
```

### 6.3 Output Schema

```json
{
  "type": "object",
  "properties": {
    "pages": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "url": { "type": "string" },
          "status": { "type": "string", "enum": ["success", "failed", "timeout", "skipped"] },
          "title": { "type": "string", "description": "网页标题（来自 <title> 标签）" },
          "content": { "type": "string", "description": "清洗后的正文内容" },
          "contentLength": { "type": "integer" },
          "contentHash": { "type": "string", "description": "内容 SHA256 哈希（用于去重）" },
          "metadata": {
            "type": "object",
            "properties": {
              "description": { "type": "string" },
              "publishedDate": { "type": "string" },
              "siteName": { "type": "string" },
              "language": { "type": "string" }
            }
          },
          "errorMessage": { "type": "string", "description": "失败原因（仅 status=failed/timeout）" },
          "fetchDurationMs": { "type": "integer" }
        }
      }
    },
    "stats": {
      "type": "object",
      "properties": {
        "total": { "type": "integer" },
        "successCount": { "type": "integer" },
        "failedCount": { "type": "integer" },
        "timeoutCount": { "type": "integer" },
        "skippedCount": { "type": "integer", "description": "因缓存/去重跳过的 URL 数" },
        "totalDurationMs": { "type": "integer" }
      }
    }
  }
}
```

### 6.4 内部实现逻辑

```
WebFetchTool.execute(input, context)
    │
    ├── 0. 安全校验
    │      - 过滤内网 IP/私有地址 (127.0.0.0/8, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
    │      - 仅允许 http:// 和 https:// 协议
    │      - 拒绝非标准端口（仅 80, 443）
    │
    ├── 1. 对每个 URL，并行执行:
    │
    │   a. 检查 Redis 缓存
    │      key: research:cache:web:{MD5(url)} → TTL: 24h
    │      hit → status=skipped, 返回缓存内容
    │
    │   b. 发起 HTTP GET
    │      - RestTemplate (timeout: connect=5s, read=10s)
    │      - User-Agent: "AI-SecondBrain-Research/1.0"
    │      - 跟随重定向（最多 3 次）
    │      - 最大响应体: 5MB
    │      - 超时 → status=timeout
    │      - 4xx/5xx → status=failed
    │
    │   c. 内容提取（Jsoup）
    │      - 解析 HTML DOM
    │      - 提取 <title>, <meta description>, <meta keywords>
    │      - 移除无用标签: script, style, nav, footer, header, aside, iframe, noscript
    │      - 移除广告/评论区域（常见 class/id 模式匹配）
    │      - 提取正文（优先 <article>, <main>, [role="main"] → fallback: body）
    │      - 格式化为纯文本或 Markdown（根据 extractMode）
    │      - 截断至 maxContentLength
    │
    │   d. 计算 contentHash = SHA256(content)
    │      → 如果 contentHash 已存在于 research_source 表中 → 标记为 duplicate
    │
    │   e. 缓存 → 返回
    │
    └── 2. 聚合所有结果 → 返回 pages[] + stats
```

### 6.5 异常处理

| 异常 | 错误码 | 处理 |
|------|--------|------|
| URL 格式无效 | `INVALID_URL` | 该 URL 标记 failed，不阻塞其他 URL |
| 连接超时 | `CONNECTION_TIMEOUT` | 该 URL 标记 timeout |
| 读取超时 | `READ_TIMEOUT` | 该 URL 标记 timeout |
| HTTP 403/404 | `HTTP_ERROR` | 该 URL 标记 failed + httpStatus |
| 内容 > 5MB | `CONTENT_TOO_LARGE` | 截取前 5MB（记录 truncated=true） |
| 内网地址 | `PRIVATE_IP_REJECTED` | 该 URL 标记 failed |
| 内容无法解析（非 HTML） | `PARSE_ERROR` | 尝试作为纯文本返回；失败则标记 failed |
| 全部 URL 失败 | — | success=true, stats.successCount=0（不抛错，由 Agent 决策） |

---

## 七、SourceAnalyzeTool

### 7.1 基本信息

| 属性 | 值 |
|------|-----|
| **Tool Name** | `source_analyze` |
| **Description** | 对已抓取的网页内容进行深度分析：提取关键事实、评估可信度、检测与已有结论的矛盾。输出结构化 findings。这是 Research Agent 完成抓取后的核心分析步骤。 |
| **权限** | 需要 userId（LLM 调用配额） |
| **幂等** | 否（LLM 输出有随机性；但相同 sourceId 的已分析结果可从 DB 读取） |
| **超时** | 30,000ms（含 LLM 调用） |
| **重试** | 最多 1 次（LLM 调用失败时） |

### 7.2 Input Schema

```json
{
  "type": "object",
  "properties": {
    "sourceIds": {
      "type": "array",
      "items": { "type": "integer" },
      "description": "要分析的 research_source ID 列表",
      "maxItems": 5
    },
    "analysisGoal": {
      "type": "string",
      "description": "分析目标（当前研究问题），指导 LLM 提取相关内容",
      "maxLength": 500
    },
    "extractFindings": {
      "type": "boolean",
      "description": "是否提取结构化 findings",
      "default": true
    },
    "assessReliability": {
      "type": "boolean",
      "description": "是否评估来源可信度",
      "default": true
    },
    "detectConflicts": {
      "type": "boolean",
      "description": "是否检测与已有研究结论的冲突",
      "default": true
    },
    "existingConclusionIds": {
      "type": "array",
      "items": { "type": "integer" },
      "description": "已有结论 ID 列表（用于冲突检测时对照）",
      "default": []
    }
  },
  "required": ["sourceIds", "analysisGoal"]
}
```

### 7.3 Output Schema

```json
{
  "type": "object",
  "properties": {
    "analyzedSources": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "sourceId": { "type": "integer" },
          "title": { "type": "string" },
          "url": { "type": "string" },
          "findings": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "statement": { "type": "string", "description": "从来源中提取的事实/观点" },
                "category": { "type": "string", "enum": ["fact", "insight", "question", "contradiction", "gap"] },
                "confidence": { "type": "string", "enum": ["high", "medium", "low", "speculation"] },
                "quote": { "type": "string", "description": "来源中支撑该发现的原文引用" },
                "relevanceScore": { "type": "number", "description": "与 analysisGoal 的相关度 0-1" }
              }
            }
          },
          "reliabilityAssessment": {
            "type": "object",
            "properties": {
              "score": { "type": "string", "enum": ["high", "medium", "low", "unverified"] },
              "factors": {
                "type": "object",
                "properties": {
                  "sourceType": { "type": "string", "description": "official_doc/paper/article/github/blog/forum" },
                  "authority": { "type": "string", "description": "来源权威度评估" },
                  "timeliness": { "type": "string", "description": "时效性评估" },
                  "objectivity": { "type": "string", "description": "客观性评估" }
                }
              },
              "notes": { "type": "string" }
            }
          },
          "conflictsDetected": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "newFinding": { "type": "string" },
                "conflictingConclusionId": { "type": "integer" },
                "conflictingStatement": { "type": "string" },
                "conflictType": { "type": "string", "enum": ["direct_contradiction", "partial_disagreement", "different_scope"] },
                "resolution": { "type": "string", "description": "LLM 建议的冲突解决方式" }
              }
            }
          }
        }
      }
    },
    "summary": {
      "type": "object",
      "properties": {
        "totalFindings": { "type": "integer" },
        "highConfidenceFindings": { "type": "integer" },
        "totalConflictsFound": { "type": "integer" },
        "analysisDurationMs": { "type": "integer" },
        "tokensUsed": { "type": "integer" }
      }
    }
  }
}
```

### 7.4 内部实现逻辑

```
SourceAnalyzeTool.execute(input, context)
    │
    ├── 1. 加载 sources
    │      ResearchSourceMapper.selectByIds(sourceIds)
    │      过滤: source.projectId == context.projectId
    │
    ├── 2. 如果 detectConflicts=true:
    │      加载 existingConclusionIds 对应的 conclusions
    │      ResearchConclusionMapper.selectByIds(existingConclusionIds)
    │
    ├── 3. 对每个 source，调用 LLM 分析:
    │
    │   AiService.chat(userId, "RESEARCH_ANALYSIS", messages):
    │     system prompt:
    │       "你是一个技术研究分析专家。请分析以下网页内容。
    │        研究问题：{analysisGoal}
    │        任务：
    │        1. 提取与研究生题相关的事实和观点（每个附上原文引用）
    │        2. 评估来源的可信度（来源类型、权威性、时效性、客观性）
    │        3. 如果提供了已有结论，检查是否存在冲突"
    │
    │     user message:
    │       "来源标题：{title}
    │        来源URL：{url}
    │        来源类型：{sourceType}
    │        内容：
    │        {content}"
    │
    │   要求 LLM 以 JSON 格式返回（指定 schema）
    │
    ├── 4. 解析 LLM 返回的 JSON → 结构化 findings
    │      非 JSON → 重试 1 次（附加格式纠正 prompt）
    │
    ├── 5. 保存 findings 到 DB
    │      ResearchFindingMapper.insert(finding)
    │      关联 source.id
    │
    ├── 6. 聚合结果 → 返回
    │
    └── 注意: 分析结果同时写入 research_finding 表。
            source 的 reliability 字段也会据此更新。
```

### 7.5 异常处理

| 异常 | 错误码 | 处理 |
|------|--------|------|
| sourceId 不存在 | `SOURCE_NOT_FOUND` | 跳过该 source，继续分析其他 |
| LLM 调用超时 (30s) | `LLM_TIMEOUT` | 该 source 标记 failed，重试 1 次 |
| LLM 返回非 JSON | `LLM_FORMAT_ERROR` | 重试 1 次（附带 "请严格输出 JSON" 提示）；仍失败 → 仅保存原始 snippet |
| 内容为空 | `EMPTY_SOURCE` | 跳过该 source |
| 全部 source 分析失败 | — | success=false, errorCode="ALL_FAILED" |

---

## 八、ResearchTaskTool

### 8.1 基本信息

| 属性 | 值 |
|------|-----|
| **Tool Name** | `research_task` |
| **Description** | 管理研究任务的 CRUD。支持创建任务、更新状态、查询任务依赖。由 Planner（创建）和 Orchestrator（状态更新）使用。 |
| **权限** | 需要 userId + projectId |
| **幂等** | 创建：否；查询：是；状态更新：是（相同状态更新无副作用） |
| **超时** | 5,000ms |
| **重试** | 最多 1 次（DB 写入失败时） |

### 8.2 Input Schema

```json
{
  "type": "object",
  "properties": {
    "operation": {
      "type": "string",
      "enum": ["create", "update_status", "list_by_project", "get_detail"],
      "description": "操作类型"
    },
    "projectId": {
      "type": "integer",
      "description": "研究项目 ID（所有操作都需要）"
    },
    "taskId": {
      "type": "integer",
      "description": "任务 ID（update_status/get_detail 使用）"
    },
    "task": {
      "type": "object",
      "description": "任务数据（create 使用）",
      "properties": {
        "title": { "type": "string", "maxLength": 300 },
        "description": { "type": "string" },
        "question": { "type": "string", "description": "该任务要回答的研究问题" },
        "requiresExternalSearch": { "type": "boolean", "default": false },
        "dependsOn": { "type": "integer", "description": "依赖的前置 taskId（null 表示无依赖）" },
        "priority": { "type": "string", "enum": ["HIGH", "MEDIUM", "LOW"], "default": "MEDIUM" },
        "sortOrder": { "type": "integer", "default": 0 }
      },
      "required": ["title", "description"]
    },
    "newStatus": {
      "type": "string",
      "enum": ["PENDING", "RUNNING", "COMPLETED", "FAILED", "SKIPPED", "WAITING_USER"],
      "description": "新状态（update_status 使用）"
    },
    "resultSummary": {
      "type": "string",
      "description": "任务结果摘要（update_status=COMPLETED 时使用）"
    }
  },
  "required": ["operation", "projectId"]
}
```

### 8.3 Output Schema

```json
{
  "type": "object",
  "properties": {
    "operation": { "type": "string" },
    "tasks": {
      "type": "array",
      "description": "任务列表",
      "items": {
        "type": "object",
        "properties": {
          "id": { "type": "integer" },
          "projectId": { "type": "integer" },
          "title": { "type": "string" },
          "description": { "type": "string" },
          "question": { "type": "string" },
          "status": { "type": "string" },
          "dependsOn": { "type": "integer" },
          "requiresExternalSearch": { "type": "boolean" },
          "priority": { "type": "string" },
          "resultSummary": { "type": "string" },
          "sortOrder": { "type": "integer" },
          "startedAt": { "type": "string", "format": "date-time" },
          "completedAt": { "type": "string", "format": "date-time" }
        }
      }
    },
    "dependencyGraph": {
      "type": "object",
      "description": "任务依赖图（list_by_project 时附带）",
      "properties": {
        "readyTasks": { "type": "array", "items": { "type": "integer" }, "description": "可以开始执行的任务 ID" },
        "blockedTasks": { "type": "array", "items": { "type": "integer" }, "description": "被阻塞的任务 ID" }
      }
    }
  }
}
```

### 8.4 内部实现逻辑

```
操作 1: create
  1. 校验 projectId 存在 + 属于当前用户
  2. 校验 dependsOn（如果存在）：该 task 存在于同一 project
  3. 构建 ResearchTask entity
  4. researchTaskMapper.insert(task)
  5. 返回 { tasks: [task] }

操作 2: update_status
  1. 加载 task → 校验 projectId 匹配 + userId 权限
  2. 校验状态转换合法性:
       PENDING → RUNNING, SKIPPED
       RUNNING → COMPLETED, FAILED, WAITING_USER
       FAILED → RUNNING (重试)
       WAITING_USER → RUNNING (用户响应后)
  3. 更新 status + resultSummary（如果 COMPLETED）+ completedAt
  4. 如果新状态=COMPLETED:
       - 检查依赖该 task 的其他 task 是否全部满足 → 触发下游 task
  5. 推送 SSE event: task_status

操作 3: list_by_project
  1. researchTaskMapper.selectList(wrapper):
       - where project_id=projectId
       - order by sort_order ASC
  2. 构建 dependencyGraph:
       - readyTasks: status=PENDING 且 dependsOn 全部 COMPLETED 或 null
       - blockedTasks: status=PENDING 且 dependsOn 有未完成的
  3. 返回

操作 4: get_detail
  1. 加载 task + 关联 steps
  2. researchStepMapper.selectList(task_id=taskId order by sort_order)
  3. 返回 task + steps[]
```

### 8.5 异常处理

| 异常 | 错误码 | 处理 |
|------|--------|------|
| 依赖的 taskId 不存在 | `DEPENDENCY_NOT_FOUND` | 返回失败，retryable=false |
| 非法状态转换 | `INVALID_TRANSITION` | 返回失败，retryable=false |
| projectId 不存在 | `PROJECT_NOT_FOUND` | 返回失败，retryable=false |
| DB 写入失败 | `DB_ERROR` | 返回失败，retryable=true |

---

## 九、ResearchMemoryTool

### 9.1 基本信息

| 属性 | 值 |
|------|-----|
| **Tool Name** | `research_memory` |
| **Description** | 管理 Agent 在研究过程中的持久记忆。支持读写记忆项，跨执行会话共享。用于 Agent 在多次迭代间保持上下文。 |
| **权限** | 需要 userId + projectId |
| **幂等** | write：是（相同 memoryKey 覆盖）；read：是；delete：是 |
| **超时** | 3,000ms |
| **重试** | 最多 1 次 |

### 9.2 Input Schema

```json
{
  "type": "object",
  "properties": {
    "operation": {
      "type": "string",
      "enum": ["read", "write", "delete", "list_keys", "search"],
      "description": "操作类型"
    },
    "projectId": {
      "type": "integer",
      "description": "研究项目 ID"
    },
    "memoryKey": {
      "type": "string",
      "description": "记忆键（read/write/delete 操作使用）。命名约定: agent_name.category.key，如 'knowledge_agent.search_result.spring_ai'",
      "maxLength": 100
    },
    "memoryType": {
      "type": "string",
      "enum": ["knowledge_state", "gap_found", "search_result", "user_preference", "decision", "intermediate"],
      "description": "记忆类型（write 操作使用）"
    },
    "content": {
      "type": "object",
      "description": "记忆内容（write 操作使用，任意 JSON 对象）"
    },
    "ttlMinutes": {
      "type": "integer",
      "description": "过期时间（分钟，write 操作使用）。0=不过期。",
      "default": 0,
      "minimum": 0,
      "maximum": 10080
    },
    "query": {
      "type": "object",
      "description": "搜索条件（search 操作使用）",
      "properties": {
        "memoryType": { "type": "string" },
        "keyword": { "type": "string", "description": "在 content JSON 中搜索关键词" },
        "limit": { "type": "integer", "default": 20 }
      }
    }
  },
  "required": ["operation", "projectId"]
}
```

### 9.3 Output Schema

```json
{
  "type": "object",
  "properties": {
    "operation": { "type": "string" },
    "memory": {
      "type": "object",
      "description": "单条记忆（read 操作）",
      "properties": {
        "id": { "type": "integer" },
        "memoryKey": { "type": "string" },
        "memoryType": { "type": "string" },
        "content": { "type": "object" },
        "lastAccessedAt": { "type": "string", "format": "date-time" },
        "expiresAt": { "type": "string", "format": "date-time" },
        "createdAt": { "type": "string", "format": "date-time" }
      }
    },
    "memories": {
      "type": "array",
      "description": "记忆列表（list_keys/search 操作）",
      "items": { "$ref": "#/properties/memory" }
    },
    "keys": {
      "type": "array",
      "description": "记忆键列表（list_keys 操作）",
      "items": { "type": "string" }
    },
    "totalCount": { "type": "integer" },
    "written": { "type": "boolean", "description": "是否写入成功（write 操作）" },
    "deleted": { "type": "boolean", "description": "是否删除成功（delete 操作）" }
  }
}
```

### 9.4 内部实现逻辑

```
ResearchMemoryTool.execute(input, context)
    │
    ├── 操作 read:
    │      researchMemoryMapper.selectOne(project_id + memory_key)
    │      → 更新 last_accessed_at
    │      → 返回 content
    │
    ├── 操作 write:
    │      INSERT ... ON DUPLICATE KEY UPDATE (project_id + memory_key 唯一索引)
    │        content = newContent
    │        memoryType = newType (if provided)
    │        update_time = now()
    │        expires_at = now() + ttlMinutes (if ttlMinutes > 0)
    │      → 返回 { written: true }
    │
    ├── 操作 delete:
    │      researchMemoryMapper.delete(project_id + memory_key)
    │      → 返回 { deleted: true }
    │
    ├── 操作 list_keys:
    │      researchMemoryMapper.selectList(wrapper):
    │        - where project_id (AND expires_at IS NULL OR expires_at > NOW())
    │        - select memory_key, memoryType
    │      → 返回 keys[] + totalCount
    │
    └── 操作 search:
           researchMemoryMapper.selectList(wrapper):
             - where project_id
             - AND (memoryType = query.memoryType if provided)
             - AND (JSON_SEARCH(content, 'one', '%keyword%') if keyword provided)
             - AND (expires_at IS NULL OR expires_at > NOW())
             - order by update_time DESC, limit
           → 返回 memories[]
```

### 9.5 异常处理

| 异常 | 错误码 | 处理 |
|------|--------|------|
| memoryKey 不存在（read） | — | 返回 { found: false }（success=true） |
| DB 写入失败 | `DB_ERROR` | 返回失败，retryable=true |
| content 非有效 JSON | `INVALID_CONTENT` | 返回失败，retryable=false |

---

## 十、KnowledgeCandidateTool

### 10.1 基本信息

| 属性 | 值 |
|------|-----|
| **Tool Name** | `knowledge_candidate` |
| **Description** | 管理研究产生的知识候选。支持创建候选、列表查询、状态变更（PENDING→ACCEPTED→REJECTED）。**不能直接修改知识库**——ACCEPTED 状态的候选需经用户确认后才由 KnowledgeWriter Agent 写入 knowledge_node 表。 |
| **权限** | 需要 userId + projectId |
| **幂等** | create：否（每次创建新记录）；状态变更：是；查询：是 |
| **超时** | 5,000ms |
| **重试** | 最多 1 次（DB 写入失败时） |

### 10.2 Input Schema

```json
{
  "type": "object",
  "properties": {
    "operation": {
      "type": "string",
      "enum": ["create", "list_by_project", "get_by_id", "update_status", "batch_update_status"],
      "description": "操作类型"
    },
    "projectId": {
      "type": "integer",
      "description": "研究项目 ID"
    },
    "candidate": {
      "type": "object",
      "description": "候选知识数据（create 使用）",
      "properties": {
        "title": { "type": "string", "maxLength": 200 },
        "contentMd": { "type": "string", "description": "Markdown 正文" },
        "summary": { "type": "string", "description": "摘要" },
        "tags": {
          "type": "array",
          "items": { "type": "string" },
          "description": "建议标签名列表"
        },
        "importance": { "type": "integer", "minimum": 1, "maximum": 5, "default": 3 },
        "sourceIds": {
          "type": "array",
          "items": { "type": "integer" },
          "description": "支撑该知识的 research_source ID 列表"
        },
        "conclusionIds": {
          "type": "array",
          "items": { "type": "integer" },
          "description": "支撑该知识的 research_conclusion ID 列表"
        }
      },
      "required": ["title", "contentMd"]
    },
    "candidateId": {
      "type": "integer",
      "description": "候选 ID（get_by_id/update_status 使用）"
    },
    "newStatus": {
      "type": "string",
      "enum": ["PENDING", "ACCEPTED", "REJECTED", "MODIFIED"],
      "description": "新状态（update_status 使用）"
    },
    "userFeedback": {
      "type": "string",
      "description": "用户修改意见（newStatus=MODIFIED 时必填）"
    },
    "batchUpdates": {
      "type": "array",
      "description": "批量状态更新（batch_update_status 使用）",
      "items": {
        "type": "object",
        "properties": {
          "candidateId": { "type": "integer" },
          "newStatus": { "type": "string", "enum": ["PENDING", "ACCEPTED", "REJECTED", "MODIFIED"] },
          "userFeedback": { "type": "string" }
        },
        "required": ["candidateId", "newStatus"]
      }
    },
    "statusFilter": {
      "type": "string",
      "enum": ["PENDING", "ACCEPTED", "REJECTED", "MODIFIED", "WRITTEN", "ALL"],
      "description": "按状态过滤（list_by_project 使用）",
      "default": "ALL"
    }
  },
  "required": ["operation", "projectId"]
}
```

### 10.3 Output Schema

```json
{
  "type": "object",
  "properties": {
    "operation": { "type": "string" },
    "candidates": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "id": { "type": "integer" },
          "projectId": { "type": "integer" },
          "title": { "type": "string" },
          "summary": { "type": "string" },
          "contentMd": { "type": "string" },
          "tags": { "type": "array", "items": { "type": "string" } },
          "importance": { "type": "integer" },
          "status": { "type": "string", "enum": ["PENDING", "ACCEPTED", "REJECTED", "MODIFIED", "WRITTEN"] },
          "sourceIds": { "type": "array", "items": { "type": "integer" } },
          "conclusionIds": { "type": "array", "items": { "type": "integer" } },
          "writtenNodeId": { "type": "integer", "description": "写入知识库后的 knowledge_node.id（仅 WRITTEN 状态）" },
          "userFeedback": { "type": "string" },
          "dedupInfo": {
            "type": "object",
            "description": "去重信息（创建时自动计算）",
            "properties": {
              "isDuplicate": { "type": "boolean" },
              "similarNodeId": { "type": "integer" },
              "similarNodeTitle": { "type": "string" },
              "similarityScore": { "type": "number" }
            }
          },
          "createdAt": { "type": "string", "format": "date-time" },
          "reviewedAt": { "type": "string", "format": "date-time" },
          "writtenAt": { "type": "string", "format": "date-time" }
        }
      }
    },
    "stats": {
      "type": "object",
      "properties": {
        "total": { "type": "integer" },
        "pending": { "type": "integer" },
        "accepted": { "type": "integer" },
        "rejected": { "type": "integer" },
        "modified": { "type": "integer" },
        "written": { "type": "integer" }
      }
    }
  }
}
```

### 10.4 内部实现逻辑

```
KnowledgeCandidateTool.execute(input, context)
    │
    ├── 操作 create:
    │   │
    │   ├── 1. 去重检查
    │   │      a. Embedding(candidate.title + candidate.summary)
    │   │      b. VectorSearchService.searchSimilar(embedding, userId, topK=3)
    │   │      c. 对每个匹配的 node:
    │   │           cosine_similarity > 0.85 → dedupInfo.isDuplicate=true
    │   │                                        记录 similarNodeId + similarNodeTitle
    │   │           0.70-0.85 → dedupInfo.isDuplicate=false, 但提示 "可能重复"
    │   │
    │   ├── 2. 匹配标签
    │   │      a. KnowledgeTagService.listByUser(userId) → 获取已有标签列表
    │   │      b. candidate.tags 中的标签名与已有标签名完全匹配 → 复用 tagId
    │   │      c. 不匹配的 → 标记为新建标签建议
    │   │
    │   ├── 3. importance 建议
    │   │      LLM (可选): 根据用户知识库覆盖度建议 importance 1-5
    │   │      覆盖度高 → 较低 importance; 覆盖度低 → 较高 importance
    │   │
    │   └── 4. 写入
    │          researchKnowledgeCandidateMapper.insert(candidate)
    │          status = "PENDING"
    │
    ├── 操作 list_by_project:
    │      researchKnowledgeCandidateMapper.selectList(wrapper):
    │        - where project_id=projectId
    │        - AND status = statusFilter (if != "ALL")
    │        - order by create_time DESC
    │      → 返回 candidates[] + stats
    │
    ├── 操作 get_by_id:
    │      加载 candidate + 校验 projectId 匹配
    │      → 返回
    │
    ├── 操作 update_status:
    │   │
    │   ├── 加载 candidate → 校验 projectId 匹配
    │   ├── 校验状态转换:
    │   │     PENDING → ACCEPTED, REJECTED, MODIFIED
    │   │     MODIFIED → ACCEPTED, REJECTED (用户修改后确认或拒绝)
    │   │     ACCEPTED → WRITTEN (由 KnowledgeWriter 写库后更新)
    │   │     不允许: REJECTED → (任何)  (拒绝不可逆)
    │   │     不允许: WRITTEN → (任何)    (已写入不可逆)
    │   │     不允许: ACCEPTED → REJECTED (确认后不可拒绝)
    │   ├── 更新 status + reviewed_at + userFeedback (如有)
    │   └── 返回
    │
    └── 操作 batch_update_status:
           对 batchUpdates[] 中的每一项执行 update_status 逻辑
           返回成功/失败计数

    ⚠️ 安全约束:
       - 此 Tool 仅管理 candidate 记录
       - 不调用 KnowledgeService.create() 或 KnowledgeService.updateKnowledge()
       - 只有 KnowledgeWriter Agent (或用户在 /write-back API 中) 触发实际写入知识库
```

### 10.5 异常处理

| 异常 | 错误码 | 处理 |
|------|--------|------|
| 标题为空 | `INVALID_INPUT` | 返回失败，retryable=false |
| 去重检查失败（Embedding 服务不可用） | `DEDUP_SKIPPED` | 继续创建，标记 dedupInfo=null，不影响主流程 |
| 非法状态转换 | `INVALID_TRANSITION` | 返回失败，retryable=false |
| candidateId 不存在 | `NOT_FOUND` | 返回失败，retryable=false |
| DB 写入失败 | `DB_ERROR` | 返回失败，retryable=true |

---

## 十一、Tool 调用安全约束

### 11.1 全局安全规则

| 规则 | 说明 |
|------|------|
| **用户隔离** | 所有 Tool 调用必须校验 userId + workspaceId，只能操作自己的数据 |
| **SSRF 防护** | WebFetchTool 禁止访问内网地址、私有 IP、非标准端口 |
| **Rate Limiting** | WebSearchTool (30次/h), WebFetchTool (50次/h), LLM 调用 (100次/h) |
| **内容安全** | WebFetchTool 抓取内容在传给 LLM 前做基本清洗（移除 script 标签） |
| **数据最小化** | Agent 不读取无关知识节点；Tool 输出不包含其他用户的任何信息 |

### 11.2 Tool 权限矩阵

| Tool | 需要 userId | 需要 workspaceId | 需要 projectId | 可被 Agent 调用 | 可被用户直接调用 (API) |
|------|------------|-----------------|----------------|----------------|----------------------|
| KnowledgeSearchTool | ✓ | ✓ | — | ✓ | ✓ (已有 search API) |
| KnowledgeDetailTool | ✓ | ✓ | — | ✓ | ✓ (已有 getById API) |
| KnowledgeGraphTool | ✓ | ✓ | — | ✓ | ✓ (已有 graph API) |
| ResearchHistoryTool | ✓ | ✓ | — | ✓ | — |
| WebSearchTool | ✓ | — | ✓ | ✓ | — |
| WebFetchTool | ✓ | — | ✓ | ✓ | — |
| SourceAnalyzeTool | ✓ | — | ✓ | ✓ | — |
| ResearchTaskTool | ✓ | — | ✓ | ✓ | ✓ |
| ResearchMemoryTool | ✓ | — | ✓ | ✓ | — |
| KnowledgeCandidateTool | ✓ | — | ✓ | ✓ | ✓ (write-back API) |

---

## 十二、Tool 注册与发现

### 12.1 Tool 注册中心

```java
package com.secondbrain.research.tool;

/**
 * Tool 注册中心。
 *
 * 管理所有 Tool 实例，提供按名称查找和 LLM function calling schema 生成。
 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new HashMap<>();

    @Autowired
    public ToolRegistry(List<Tool> toolList) {
        for (Tool tool : toolList) {
            tools.put(tool.getName(), tool);
        }
    }

    /** 按名称获取 Tool */
    public Tool getTool(String name) {
        Tool tool = tools.get(name);
        if (tool == null) {
            throw new ToolNotFoundException(name);
        }
        return tool;
    }

    /** 获取所有 Tool 名称 */
    public Set<String> getToolNames() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    /** 生成 LLM function calling 的 tools 参数 */
    public List<Map<String, Object>> generateFunctionDefinitions(List<String> toolNames) {
        return toolNames.stream()
            .map(name -> {
                Tool tool = getTool(name);
                return Map.of(
                    "type", "function",
                    "function", Map.of(
                        "name", tool.getName(),
                        "description", tool.getDescription(),
                        "parameters", tool.getInputSchema().toJsonSchema()
                    )
                );
            })
            .toList();
    }
}
```

### 12.2 Agent-Tool 绑定

| Agent | 可用 Tool |
|-------|----------|
| **PlannerAgent** | ResearchHistoryTool, ResearchTaskTool |
| **KnowledgeAgent** | KnowledgeSearchTool, KnowledgeDetailTool, KnowledgeGraphTool |
| **GapAgent** | KnowledgeGraphTool, KnowledgeSearchTool, ResearchHistoryTool |
| **ResearchAgent** | WebSearchTool, WebFetchTool, SourceAnalyzeTool, KnowledgeSearchTool, ResearchMemoryTool |
| **CriticAgent** | SourceAnalyzeTool, KnowledgeDetailTool, ResearchHistoryTool, ResearchMemoryTool |
| **SynthesizerAgent** | ResearchTaskTool, ResearchMemoryTool (读取所有中间结果) |
| **KnowledgeWriterAgent** | KnowledgeSearchTool (去重检查), KnowledgeCandidateTool, ResearchMemoryTool |
