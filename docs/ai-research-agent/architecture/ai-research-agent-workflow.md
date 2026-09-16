# AI Research Agent — Agent Workflow 设计文档

> 基于架构设计：`ai-research-agent-architecture.md`
> 基于数据库设计：`ai-research-agent-database.md`
> 日期：2026-07-28
> 版本：V1

---

## 一、端到端工作流：以「研究 Spring AI Agent」为例

### 1.1 总体流程（14 步）

```
用户输入：「研究 Spring AI Agent」
    │
    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ STEP 1: Planner Agent                                                    │
│   理解研究目标 → 分析用户知识背景 → 评估复杂度 → 生成 Research Plan       │
│   输出: complexity=STANDARD, agentChain=[Knowledge,Gap,Research,          │
│         Critic,Synthesizer,Writer], 3 个 Task                            │
├──────────────────────────────────────────────────────────────────────────┤
│ STEP 2: Knowledge Agent                                                  │
│   三步并行检索：语义检索 + 关键词检索 + 图谱扩展                           │
│   输出: 12 个相关知识节点, 掌握度评估报告                                  │
├──────────────────────────────────────────────────────────────────────────┤
│ STEP 3: Knowledge Gap Agent                                              │
│   图谱结构分析 + 前置知识推断 + 社区对比                                   │
│   输出: 3 个显性缺口 + 2 个隐性缺口                                        │
├──────────────────────────────────────────────────────────────────────────┤
│ STEP 4-6: Research Agent (per Task)                                      │
│   Task 1: "Spring AI Agent 基础概念" ─ 搜索 → 抓取 → 提取 → 保存来源       │
│   Task 2: "Tool Calling 机制"       ─ 搜索 → 抓取 → 提取 → 保存来源       │
│   Task 3: "生产级部署考虑"           ─ 搜索 → 抓取 → 提取 → 保存来源       │
│   输出: 15 个 research_source, 8 个 research_finding                     │
├──────────────────────────────────────────────────────────────────────────┤
│ STEP 7-9: Critic Agent (per finding/batch)                               │
│   来源检查 + 可信度评估 + 一致性验证 + 时效性检查                          │
│   输出: 8 个 research_conclusion (3 high, 3 medium, 1 low, 1 speculation) │
├──────────────────────────────────────────────────────────────────────────┤
│ STEP 10: [条件循环] 发现新缺口?                                            │
│   Critic 发现 Task 1 和 Task 3 之间有知识断层 → GapAgent 再次分析          │
│   → ResearchAgent 补充搜索 → 最多 3 轮迭代                                 │
├──────────────────────────────────────────────────────────────────────────┤
│ STEP 11-12: Synthesizer Agent                                           │
│   聚合所有结论 → 生成结构化研究报告 (Markdown)                              │
│   输出: research_report (12KB, 包含所有结论 + 来源引用 + 冲突标注)         │
├──────────────────────────────────────────────────────────────────────────┤
│ STEP 13: Knowledge Writer Agent                                          │
│   从结论提取新知识 → 去重 → 生成候选                                       │
│   输出: 5 个 research_knowledge_candidate (PENDING, 等待用户确认)          │
├──────────────────────────────────────────────────────────────────────────┤
│ STEP 14: 用户确认                                                         │
│   用户在 /research/{id}/preview-write 勾选 → write-back 执行              │
│   输出: 3 个新 knowledge_node + 5 个 knowledge_relation                   │
└──────────────────────────────────────────────────────────────────────────┘
```

### 1.2 时间预算估算

| 阶段 | Agent | 预估耗时 | Token 消耗 |
|------|-------|----------|------------|
| 1 | Planner | 5-10s | ~3K |
| 2 | Knowledge | 3-8s | ~5K |
| 3 | Gap | 5-10s | ~4K |
| 4-6 | Research ×3 | 15-45s | ~15K |
| 7-9 | Critic | 8-15s | ~8K |
| 10 | 条件循环 | 0-60s | 0-20K |
| 11-12 | Synthesizer | 10-20s | ~10K |
| 13 | KnowledgeWriter | 5-10s | ~5K |
| **总计** | | **51-118s** | **~50-70K tokens** |

---

## 二、Agent 详细定义

### 2.1 Planner Agent

#### Role

研究规划者。分析用户研究目标与知识背景，评估复杂度，生成结构化的 Research Plan。

#### Input

```json
{
  "researchGoal": "研究 Spring AI Agent",
  "userContext": {
    "totalNodes": 156,
    "relevantNodes": 12,
    "topTags": ["Spring Boot", "AI", "Java", "Agent"],
    "graphConnectivity": 0.42,
    "averageMastery": 2.8
  },
  "preferences": {
    "maxDepth": "STANDARD",
    "focusAreas": []
  }
}
```

#### Output（结构化 JSON）

```json
{
  "complexity": "STANDARD",
  "rationale": "用户在 Spring Boot 和 AI 领域有一定基础（12 个相关节点），但 Agent 方向知识点较少（掌握度 2.8/5），需要外部搜索补充。建议标准流程。",
  "agentChain": [
    "KnowledgeAgent",
    "GapAgent",
    "ResearchAgent",
    "CriticAgent",
    "SynthesizerAgent",
    "KnowledgeWriterAgent"
  ],
  "estimatedTokens": 55000,
  "estimatedDurationMs": 90000,
  "tasks": [
    {
      "title": "Spring AI Agent 基础概念与架构",
      "description": "理解 Spring AI 中 Agent 的核心概念、架构设计和基本用法",
      "question": "Spring AI Agent 是什么？它的核心组件有哪些？与直接使用 LLM 有何区别？",
      "requiresExternalSearch": true,
      "dependsOn": null,
      "priority": "HIGH"
    },
    {
      "title": "Tool Calling 机制深度研究",
      "description": "研究 Agent 如何调用 Tool、Tool 的定义和注册方式、多 Tool 协作",
      "question": "Spring AI Agent 的 Tool Calling 机制如何工作？支持哪些 Tool 类型？如何处理 Tool 调用失败？",
      "requiresExternalSearch": true,
      "dependsOn": 1,
      "priority": "HIGH"
    },
    {
      "title": "生产级部署与最佳实践",
      "description": "研究 Agent 在生产环境中的性能优化、安全考虑和监控方案",
      "question": "Spring AI Agent 在生产环境如何部署？有哪些性能优化策略和安全最佳实践？",
      "requiresExternalSearch": true,
      "dependsOn": [1, 2],
      "priority": "MEDIUM"
    }
  ]
}
```

#### Tool

无外部 Tool 调用。纯 LLM 推理。

但需要读取用户知识概况（通过注入的 `userContext`，由 Orchestrator 在调用前通过 `KnowledgeService.getUserStats()` + 标签统计预先填充）。

#### State

```
IDLE → ANALYZING_GOAL → ASSESSING_KNOWLEDGE → ESTIMATING_COMPLEXITY
  → STRUCTURING_TASKS → COMPLETED
```

#### Error

| 错误场景 | 处理 |
|----------|------|
| LLM 返回非 JSON | 重试 1 次，附加格式纠正 prompt；二次失败 → 降级为 SIMPLE（KnowledgeAgent → SynthesizerAgent） |
| Token 超限（userContext 过大） | 截断 userContext（只保留 top 10 标签 + 数字统计） |
| LLM 超时 (30s) | 降级为 SIMPLE 流程，跳过规划直接执行 KnowledgeAgent |

#### Retry

- 最大重试次数：1
- 退避策略：立即重试（无退避，因为不涉及外部 API）

#### Timeout

- LLM 调用超时：30s
- Agent 总超时：60s

---

### 2.2 Knowledge Agent

#### Role

知识库检索专家。从用户现有知识库中检索与研究生题最相关的内容，评估用户对每个子领域的掌握程度。

#### Input

```json
{
  "projectId": 1,
  "taskId": null,
  "researchGoal": "研究 Spring AI Agent",
  "researchPlan": { "tasks": [...] },
  "searchQueries": [
    "Spring AI Agent 架构",
    "Spring AI Tool Calling",
    "Java AI Agent 框架"
  ]
}
```

搜索查询由 Planner 的 Task 列表 + LLM 扩展生成（每个 Task 扩展 2-3 个搜索关键词）。

#### Output（结构化 JSON）

```json
{
  "knowledgeReferences": [
    {
      "nodeId": 342,
      "title": "Spring Boot 3.x 核心特性",
      "snippet": "Spring Boot 3.x 引入了 AOT 编译、虚拟线程支持...",
      "relevanceScore": 0.87,
      "masteryLevel": 4,
      "relationType": "prerequisite"
    },
    {
      "nodeId": 511,
      "title": "AI Agent 基础概念",
      "snippet": "Agent 是一种能自主使用工具、规划步骤、执行任务的 AI 系统...",
      "relevanceScore": 0.92,
      "masteryLevel": 2,
      "relationType": "related"
    }
  ],
  "masteryAssessment": {
    "overallMastery": 0.35,
    "subAreaMastery": {
      "agent_basics": 0.40,
      "tool_calling": 0.15,
      "deployment": 0.10
    },
    "strongAreas": ["Spring Boot 基础", "Java 开发"],
    "weakAreas": ["Agent 架构设计", "Tool Calling 机制", "生产级 Agent 部署"]
  },
  "searchStats": {
    "semanticResults": 15,
    "keywordResults": 18,
    "graphExpandedResults": 8,
    "totalUniqueResults": 22,
    "searchDurationMs": 3200
  }
}
```

#### Tool

| Tool | 用途 | 超时 |
|------|------|------|
| **KnowledgeSearchTool** | 三步检索（语义+关键词+图谱） | 10s |
| **KnowledgeReadTool** | 读取单个知识点完整内容（top 5） | 3s |

**KnowledgeSearchTool 内部流程**（三步并行）：

```
输入: List<String> queries

并行执行:
  Thread 1: Embedding(query) → Vector Search (knowledge_embedding, cosine, top 20)
  Thread 2: Elasticsearch multi-field (title, summary, contentMd), top 20
  Thread 3: (等待 Thread 1+2 完成) → 取交集 nodeId → KnowledgeGraphService.getNeighbors(1-hop)

综合排序:
  score = similarity × 0.5 + es_score × 0.3 + graph_distance × 0.2

输出: List<KnowledgeReference> (sorted by score, max 30)
```

#### State

```
IDLE → GENERATING_QUERIES → SEARCHING (并行三步)
  → READING_DETAIL (top 5 完整内容)
  → ASSESSING_MASTERY → COMPLETED
```

#### Error

| 错误场景 | 处理 |
|----------|------|
| ES 不可用 | 降级：仅用 Vector Search + 图谱 |
| Vector Search 不可用 | 降级：仅用 ES 关键词搜索 |
| 两者均不可用 | 返回空结果 + 标记 `searchDegraded: true`，后续 Agent 仍继续执行 |
| 图谱不可用 | 跳过图谱扩展步骤 |
| 检索结果 < 3 个 | 标记 `knowledgeScarce: true`，Planner 应强制启用 ResearchAgent |

#### Retry

- ES/Vector Search 单次失败：重试 1 次，退避 2s
- LLM 查询生成失败：重试 1 次

#### Timeout

- 单次搜索：10s
- 完整检索：15s
- Agent 总超时：30s

---

### 2.3 Knowledge Gap Agent

#### Role

知识盲区分析师。对比用户现有知识与研究目标，发现显性和隐性知识缺口。

#### Input

```json
{
  "projectId": 1,
  "researchGoal": "研究 Spring AI Agent",
  "researchPlan": { "tasks": [...] },
  "knowledgeReferences": [ ... ],
  "masteryAssessment": { "subAreaMastery": {...}, "weakAreas": [...] },
  "workspaceId": 2
}
```

#### Output（结构化 JSON）

```json
{
  "gaps": [
    {
      "category": "EXPLICIT",
      "topic": "Spring AI Agent Tool Calling 机制",
      "description": "用户了解 Spring Boot 基础，但对 Spring AI 中 Tool 的定义、注册、调用流程完全空白",
      "priority": "CRITICAL",
      "correspondingTaskId": 2,
      "discoveryMethod": "MASTERY_THRESHOLD",
      "relatedNodes": [511]
    },
    {
      "category": "IMPLICIT",
      "topic": "Agent 状态管理与 Memory 模式",
      "description": "通过图谱结构分析发现：用户有 'State Machine' 节点和 'Agent 基础' 节点，但缺少 'Agent Memory' 中间连接节点",
      "priority": "HIGH",
      "correspondingTaskId": null,
      "discoveryMethod": "GRAPH_STRUCTURE_ANALYSIS",
      "relatedNodes": [511, 203]
    },
    {
      "category": "IMPLICIT",
      "topic": "Agent 评估与测试方法",
      "description": "社区对比分析：workspace 中 3 位同事有 'Agent Evaluation' 相关知识，用户缺少",
      "priority": "MEDIUM",
      "correspondingTaskId": null,
      "discoveryMethod": "COMMUNITY_COMPARISON",
      "relatedNodes": []
    }
  ],
  "gapAnalysisStats": {
    "totalGaps": 3,
    "explicitGaps": 1,
    "implicitGaps": 2,
    "criticalGaps": 1
  },
  "recommendation": {
    "shouldCreateNewTask": true,
    "newTaskSuggestion": {
      "title": "Agent 状态管理与 Memory 模式",
      "question": "Spring AI Agent 如何管理对话状态和长期记忆？",
      "requiresExternalSearch": true
    }
  }
}
```

#### Tool

| Tool | 用途 | 超时 |
|------|------|------|
| **KnowledgeGraphTool** | 图谱结构分析、邻居遍历、路径查询 | 5s |

**KnowledgeGraphTool 内部逻辑**：

```
1. GRAPH_STRUCTURE_ANALYSIS:
   - 取 Knowledge Agent 返回的 top 节点
   - 查询每个节点的子节点（parent-child 关系）
   - 通过 LLM 生成该知识领域的"完整知识树"结构
   - 对比 → 找出用户缺失的分支

2. PREREQUISITE_CHAIN:
   - 取研究目标中涉及的领域概念
   - 沿 prerequisite 关系反向遍历
   - 标记链中缺失的节点

3. COMMUNITY_COMPARISON (workspace 内):
   - 查询 workspace 中其他成员拥有但用户没有的标签
   - 聚类 → 标记为潜在缺失领域
```

#### State

```
IDLE → ANALYZING_MASTERY → TRAVERSING_GRAPH → COMMUNITY_COMPARISON
  → GENERATING_GAP_LIST → PRIORITIZING → COMPLETED
```

#### Error

| 错误场景 | 处理 |
|----------|------|
| 图谱查询超时 | 跳过图谱分析，仅用掌握度阈值判断（只产出 EXPLICIT 缺口） |
| 社区对比无数据 | 跳过社区对比步骤 |
| LLM 生成知识树失败 | 降级为仅基于现有标签统计生成缺口列表 |
| 无缺口 | 返回空 gaps + `recommendation.shouldCreateNewTask: false` |

#### Retry

- 图谱查询失败：重试 1 次，退避 2s
- LLM 调用失败：重试 1 次

#### Timeout

- 图谱查询：5s
- LLM 调用：20s
- Agent 总超时：40s

---

### 2.4 Research Agent

#### Role

外部信息检索专家。针对每个研究 Task，从互联网检索相关信息并结构化提取。

#### Input

```json
{
  "projectId": 1,
  "taskId": 2,
  "taskTitle": "Tool Calling 机制深度研究",
  "taskQuestion": "Spring AI Agent 的 Tool Calling 机制如何工作？支持哪些 Tool 类型？如何处理 Tool 调用失败？",
  "previousSources": [],
  "knowledgeGaps": ["Tool Calling 机制", "Tool 类型", "错误处理"]
}
```

#### Output（结构化 JSON）

```json
{
  "taskId": 2,
  "sources": [
    {
      "title": "Spring AI - Agent Documentation",
      "url": "https://docs.spring.io/spring-ai/reference/api/agents.html",
      "sourceType": "official_doc",
      "snippet": "Spring AI provides a flexible Agent abstraction that supports Tool Calling...",
      "relevanceScore": 0.95,
      "reliability": "high"
    },
    {
      "title": "Building AI Agents with Spring AI - Baeldung",
      "url": "https://www.baeldung.com/spring-ai-agents",
      "sourceType": "article",
      "snippet": "In this tutorial, we'll explore how to build AI Agents using Spring AI's Tool Calling...",
      "relevanceScore": 0.88,
      "reliability": "medium"
    }
  ],
  "findings": [
    {
      "statement": "Spring AI Agent 通过 @Tool 注解定义可调用工具，支持方法级和类级 Tool 注册",
      "category": "fact",
      "sourceIds": [1, 2],
      "confidence": "high"
    },
    {
      "statement": "Tool Calling 在底层基于 OpenAI Function Calling 协议，同时兼容其他 LLM Provider",
      "category": "insight",
      "sourceIds": [1],
      "confidence": "high"
    }
  ],
  "searchStats": {
    "queriesExecuted": 2,
    "pagesFetched": 8,
    "pagesSucceeded": 6,
    "pagesFailed": 2,
    "pagesSkipped": 0,
    "durationMs": 18500,
    "totalTokensUsed": 8000
  }
}
```

#### Tool

| Tool | 用途 | 超时 |
|------|------|------|
| **WebSearchTool** | 外部搜索（生成查询 → 调用 Search API → 返回 URL 列表） | 10s |
| **WebFetchTool** | 批量抓取网页正文（RestTemplate + Jsoup） | 15s/page |
| **ExtractTool** | LLM 从正文提取关键信息，生成摘要 + finding | 15s |

**Research Agent 主流程**（内部 6 步流水线）：

```
1. GENERATE_QUERIES
   LLM: taskQuestion + knowledgeGaps → 2-3 个搜索查询词
   示例: ["Spring AI Tool Calling mechanism", "Spring AI @Tool annotation example"]

2. WEB_SEARCH (并行)
   WebSearchTool.execute("Spring AI Tool Calling mechanism")
   WebSearchTool.execute("Spring AI @Tool annotation example")
   → 合并 Top 10 个 URL

3. DEDUP_URLS
   根据 contentHash 或 URL 去重（排除已在 previousSources 中的 URL）

4. WEB_FETCH (并行，最多 5 个并发)
   WebFetchTool.fetch(url1), WebFetchTool.fetch(url2), ...
   超时 15s/page，失败跳过

5. EXTRACT (按 source 独立执行)
   对每个成功抓取的 source → ExtractTool.extract(fullContent, taskQuestion)
   → 生成 snippet + findings

6. COMPILE_RESULTS
   聚合所有 source + finding → 结构化输出
```

#### State

```
IDLE → GENERATING_QUERIES → SEARCHING → FETCHING → EXTRACTING → COMPILING → COMPLETED
```

#### Error

| 错误场景 | 处理 |
|----------|------|
| Search API 不可用 | 标记 `searchDegraded: true`，返回空 sources；不影响其他 Task |
| 所有 Search API 均失败 | 该 Task 标记 FAILED → Orchestrator 决策：继续下一个 Task 或降级 |
| 单页抓取失败/超时 | 跳过该 URL，不阻塞其他页面 |
| 50% 以上页面抓取失败 | 降低该 Task findings 的默认 confidence → medium |
| ExtractTool LLM 超时 | 跳过该 source 的 extraction，仅保留 snippet |
| 所有抓取均失败 | 该 Task 标记 FAILED |
| 搜索结果为空 | 尝试用备选查询词重试 1 次 |
| 搜索结果重复（>80% 已缓存） | 使用已缓存的 source，跳过抓取步骤 |

#### Retry

- Search API 单次失败：指数退避重试 2 次 (1s, 3s)
- 查询词生成失败：切换 LLM 重试 1 次
- 页面抓取失败：不重试（可能 dead link）
- ExtractTool 失败：不重试（保留 snippet）

#### Timeout

- 搜索查询生成：10s
- 单次搜索 API 调用：10s
- 单页抓取：15s
- 单页提取：15s
- Agent 总超时：120s（3 个 Task 并行时取 max）

---

### 2.5 Critic Agent

#### Role

结论验证者。对 Resarch Agent 产生的每条 finding 进行可信度验证、交叉检查、冲突发现。

#### Input

```json
{
  "projectId": 1,
  "taskId": 2,
  "findings": [ ... ],
  "sources": [ ... ],
  "existingConclusions": []
}
```

#### Output（结构化 JSON）

```json
{
  "conclusions": [
    {
      "statement": "Spring AI Agent 通过 @Tool 注解定义可调用工具",
      "confidence": "high",
      "supportingSources": [
        { "sourceId": 1, "quote": "Tools can be defined using the @Tool annotation...",
          "relevance": "direct" },
        { "sourceId": 3, "quote": "The @Tool annotation marks a method as callable by the Agent",
          "relevance": "direct" }
      ],
      "conflictingSources": [],
      "isKeyFinding": true,
      "isControversial": false,
      "criticNotes": "2 个高可信度官方来源一致确认，结论可直接采纳。"
    },
    {
      "statement": "Spring AI Agent 在生产环境中建议使用 Redis 作为对话记忆后端",
      "confidence": "low",
      "supportingSources": [
        { "sourceId": 5, "quote": "...can use Redis for persistence...", "relevance": "indirect" }
      ],
      "conflictingSources": [
        { "sourceId": 6, "quote": "The default in-memory store is sufficient for most use cases" }
      ],
      "isKeyFinding": false,
      "isControversial": true,
      "criticNotes": "单一来源且存在冲突信息，建议标注为争议项，不写入知识库。"
    }
  ],
  "validationStats": {
    "totalFindings": 8,
    "promotedToConclusion": 8,
    "highConfidence": 3,
    "mediumConfidence": 3,
    "lowConfidence": 1,
    "speculation": 1,
    "controversial": 1
  }
}
```

#### Tool

| Tool | 用途 | 超时 |
|------|------|------|
| **VerifyTool** | 在已收集的 sources 中检索证据支撑某条 statement | 5s |

**VerifyTool 内部逻辑**：

```
输入: statement + List<Source>

1. 对每条 statement:
   - 在每个 source.snippet + source.fullContent 中做语义匹配
   - 找到 "该 statement 在哪里被提到" 的具体引用

2. 汇总:
   - 直接引用 (direct): source 原文明确指出该事实
   - 间接推导 (indirect): source 隐含支持但未明确说明
   - 无来源: source 中找不到相关段落
```

**Critic Agent 主流程**：

```
1. SOURCE_CHECK
   对每条 finding，调用 VerifyTool 验证是否有 source 支撑
   无 source 的 → speculation

2. RELIABILITY_SCORING
   评估支撑来源的可靠性:
   official_doc (1.0) > paper (0.9) > github (0.7) > article (0.6) > blog (0.4)

3. CONSISTENCY_CHECK (LLM)
   对同主题的多条 finding，检查是否存在矛盾:
   - 输入: [finding_A, finding_B, finding_C]
   - LLM 判断: A 和 B 是否矛盾？B 和 C 是否一致？
   - 矛盾 → 标记 conflictingSources

4. TIMELINESS_CHECK
   技术类来源检查发布时间:
   - < 1 年 → 无影响
   - 1-2 年 → 降低一级 confidence
   - > 2 年 → 降低两级 + 标记 "可能过时"

5. PROMOTE_TO_CONCLUSION
   将 validated finding → research_conclusion 记录
```

#### State

```
IDLE → VERIFYING_SOURCES → SCORING_RELIABILITY → CHECKING_CONSISTENCY
  → CHECKING_TIMELINESS → PROMOTING → COMPLETED
```

#### Error

| 错误场景 | 处理 |
|----------|------|
| VerifyTool 超时 | 跳过该 finding 的 source 验证，标记 confidence=medium |
| LLM 一致性检查超时 | 跳过一致性检查，标记 `consistencyCheckSkipped: true` |
| 单条 finding 无法验证 | 标记 confidence=speculation，不阻塞其他 finding |
| 全部 finding 为 speculation | 标记该项目 `critically_low_confidence`，Synthesizer 展示时明确警告 |

#### Retry

- VerifyTool 失败：重试 1 次
- LLM 调用失败：重试 1 次

#### Timeout

- VerifyTool：5s
- LLM 一致性检查：20s
- Agent 总超时：60s

---

### 2.6 Synthesizer Agent

#### Role

研究报告生成者。聚合所有研究步骤的中间结果，生成结构化、可追溯的最终研究报告。

#### Input

```json
{
  "projectId": 1,
  "researchGoal": "研究 Spring AI Agent",
  "researchPlan": { ... },
  "tasks": [
    { "title": "...", "status": "COMPLETED", "resultSummary": "..." }
  ],
  "conclusions": [ ... ],
  "sources": [ ... ],
  "gaps": [ ... ],
  "executionStats": {
    "totalDurationMs": 85000,
    "totalTokens": 52000,
    "sourcesCollected": 15,
    "conclusionsGenerated": 8,
    "iterationsUsed": 1
  }
}
```

#### Output（结构化 JSON → 报告 Markdown）

结构化输出：

```json
{
  "reportTitle": "Spring AI Agent 研究报告",
  "summary": "Spring AI 提供了一套完整的 Agent 抽象层，支持 Tool Calling、对话记忆管理和多 Provider 兼容。本报告覆盖了 Agent 基础架构、Tool Calling 机制和生产级部署考虑。关键发现：Spring AI Agent 通过 @Tool 注解定义工具，底层基于 Function Calling 协议；Agent 状态管理支持多种后端（内存/Redis/JDBC）；生产环境需关注安全过滤和速率限制。",
  "keyFindings": [
    {
      "rank": 1,
      "statement": "Spring AI Agent 基于 @Tool 注解 + Function Calling 协议实现 Tool Calling",
      "confidence": "high",
      "sourceCount": 3,
      "isControversial": false
    },
    {
      "rank": 2,
      "statement": "Agent Memory 支持多种后端，生产环境推荐 Redis",
      "confidence": "low",
      "sourceCount": 2,
      "isControversial": true
    }
  ],
  "knowledgeGaps": [
    {
      "topic": "Agent 评估与测试方法",
      "description": "如何评估 Agent 行为质量、响应准确率和 Tool 调用正确率，目前缺乏有效资料",
      "priority": "MEDIUM",
      "suggestedNextResearch": "Agent 评估框架与 Benchmark 调研"
    }
  ],
  "controversialTopics": [
    {
      "topic": "Agent Memory 后端选择",
      "positionA": "生产环境推荐 Redis",
      "sourceForA": 5,
      "positionB": "默认内存存储足够大多数场景",
      "sourceForB": 6,
      "resolution": "建议根据并发量和持久化需求选择，低并发场景内存方案可行"
    }
  ]
}
```

最终研究报告中每条引用格式：`[来源{n}]`，对应 `research_source.id`。

#### Tool

无外部 Tool 调用。纯 LLM 推理 + 聚合。

#### State

```
IDLE → OUTLINING → WRITING_SECTIONS → FORMATTING_CITATIONS
  → GENERATING_SUMMARY → FINALIZING → COMPLETED
```

#### Error

| 错误场景 | 处理 |
|----------|------|
| LLM 上下文超限（token 过多） | 分批处理：每批最多 10 条 conclusion + 5 个 source；最后合并 |
| LLM 生成中断 | 从最后一个完整 section 后继续（检查点续写） |
| 结论过多（>50 条） | 只取 key_finding + high/medium confidence 的 conclusion |

#### Retry

- LLM 调用失败：重试 1 次
- 分批处理时单批失败：重试该批 1 次

#### Timeout

- 报告大纲生成：15s
- 单批 section 生成：30s
- Agent 总超时：120s

---

### 2.7 Knowledge Writer Agent

#### Role

知识沉淀者。从研究结论中提取可独立存在的知识点，与用户现有知识去重，生成待确认的知识候选。

#### Input

```json
{
  "projectId": 1,
  "report": { "keyFindings": [...], "knowledgeGaps": [...] },
  "conclusions": [ ... ],
  "userExistingKnowledge": {
    "nodeIds": [342, 511, ...],
    "recentTitles": ["Spring Boot 3.x 核心特性", "AI Agent 基础概念"],
    "embeddingCache": { "342": [0.123, 0.456, ...], ... }
  }
}
```

#### Output（结构化 JSON）

```json
{
  "candidates": [
    {
      "title": "Spring AI Agent Tool Calling 机制",
      "contentMd": "# Spring AI Agent Tool Calling\n\n## 核心概念\n\nSpring AI Agent 通过 @Tool 注解定义可调用工具...\n\n## 使用方式\n\n```java\n@Tool(name = \"getWeather\", description = \"获取城市天气\")\npublic String getWeather(String city) { ... }\n```\n\n## 底层协议\n\n基于 OpenAI Function Calling 协议，同时兼容 Anthropic Tool Use 和 Gemini Function Calling。",
      "summary": "Spring AI Agent 的 Tool Calling 机制基于 @Tool 注解和 Function Calling 协议",
      "tags": ["Spring AI", "Agent", "Tool Calling", "Java"],
      "importance": 4,
      "sourceIds": [1, 3, 7],
      "conclusionIds": [1, 2, 3],
      "dedupCheck": {
        "isDuplicate": false,
        "mostSimilarNodeId": null,
        "similarityScore": 0.0
      }
    }
  ],
  "stats": {
    "totalCandidates": 5,
    "highConfidence": 3,
    "mediumConfidence": 2,
    "duplicatesFiltered": 1,
    "mergedIntoCandidate": 2
  }
}
```

#### Tool

| Tool | 用途 | 超时 |
|------|------|------|
| **KnowledgeWriteTool** | 语义去重检查 + 写入候选记录 | 5s |

**KnowledgeWriteTool 内部流程**：

```
输入: List<CandidateToCheck>, userExistingKnowledge

1. DEDUP_CHECK (对每个 candidate):
   - Embedding(candidate.title + candidate.summary)
   - Vector Search (knowledge_embedding, cosine, top 3)
   - 对每个匹配的 node:
     if cosine_similarity > 0.85 → 标记为重复，合并建议
     if 0.70-0.85 → 标记为可能重复，展示给用户判断
     if < 0.70 → 新知识

2. TAGS_SUGGEST:
   - LLM: 根据 candidate.contentMd 生成 3-5 个标签名
   - 优先匹配已有标签（完全匹配则复用 tagId）

3. IMPORTANCE_ASSESSMENT:
   - LLM: 根据用户知识库中该领域的覆盖度评估 importance (1-5)
   - 覆盖度高 → importance 较低（补充细节）
   - 覆盖度低 → importance 较高（新领域知识）

4. WRITE_CANDIDATE:
   - 插入 research_knowledge_candidate 记录 (status=PENDING)
```

#### State

```
IDLE → EXTRACTING_KNOWLEDGE → DEDUP_CHECKING → TAGGING → WRITING_CANDIDATES → COMPLETED
```

#### Error

| 错误场景 | 处理 |
|----------|------|
| Embedding 服务不可用 | 跳过去重检查，标记 `dedupSkipped: true`，所有候选标记为待确认 |
| 全部候选为重复 | 返回空 candidates + `stats.duplicatesFiltered` + 建议用户查看已有知识 |
| 写入 candidate 失败 | 不影响其他 candidate，失败项单独标记；全部失败则返回错误 |

#### Retry

- Embedding 调用失败：重试 1 次
- LLM 标签生成失败：重试 1 次
- DB 写入失败：不重试（避免重复写入）

#### Timeout

- 去重检查 (批量 Embedding)：15s
- LLM 标签生成：10s
- Agent 总超时：40s

---

## 三、Agent State Machine

### 3.1 单 Agent 状态机

```
                    ┌──────────────────────────────────────────┐
                    │              Orchestrator 调度             │
                    └──────────────────────────────────────────┘
                                        │
                                        ▼
                                  ┌──────────┐
                                  │   IDLE   │
                                  └────┬─────┘
                                       │ execute(context)
                                       ▼
                                  ┌──────────┐
                            ┌────→│ THINKING │
                            │     └────┬─────┘
                            │          │ 需要调用 Tool
                            │          ▼
                            │     ┌──────────────┐
                            │     │ TOOL_CALLING │──────────────┐
                            │     └──────┬───────┘              │
                            │            │ Tool 返回结果         │ Tool 失败 (retryable)
                            │            ▼                      │
                            │     ┌──────────────────┐          │
                            │     │ WAITING_TOOL_RES │          │
                            │     └──────┬───────────┘          │
                            │            │                      │
                            │            ▼                      │
                            │     ┌──────────────────┐          │
                            │     │ PROCESSING_RESULT│←─────────┘
                            │     └──────┬───────────┘
                            │            │ 判断是否需要继续调用 Tool
                            │            │ (yes) ──────────────┘
                            │            │ (no)
                            │            ▼
                            │     ┌────────────┐
                            │     │  COMPLETED │
                            │     └────────────┘
                            │
                            │     错误路径:
                            │     THINKING ───────── TOOL_CALLING ───────── WAITING_TOOL_RES
                            │         │                    │                      │
                            │         │ LLM 失败            │ Tool 不可用           │ Tool 超时
                            │         ▼                    ▼                      ▼
                            │     ┌──────────────────────────────────────────────────┐
                            │     │                    FAILED                        │
                            │     │  errorType + errorMessage + retryable (bool)      │
                            │     └──────────────────────────────────────────────────┘
                            │                        │
                            │        ┌───────────────┼───────────────┐
                            │        │ retryable     │ 非 retryable   │ maxRetries
                            │        ▼               ▼                ▼
                            │   RETRY (重试)    SKIP (跳过该    ESCALATE (升级给
                            │   重置状态为        Step，继续下     Orchestrator
                            │   IDLE)            一个 Step)       决策)
                            └────────────────────────────────────────┘
```

### 3.2 Orchestrator（编排器）状态机

```
                              ┌──────────────┐
                              │   PROJECT    │
                              │   CREATED    │
                              └──────┬───────┘
                                     │ execute()
                                     ▼
                              ┌──────────────┐
                              │  PLANNING    │──── 失败 ────→ FAILED
                              └──────┬───────┘
                                     │ plan ready
                                     ▼
                              ┌──────────────┐
                              │  ANALYZING   │ (Knowledge Agent + Gap Agent)
                              │  KNOWLEDGE   │──── 部分失败 ────→ 降级继续
                              └──────┬───────┘
                                     │ gaps found
                                     ▼
                              ┌──────────────┐
                     ┌───────→│  RESEARCHING │ (Research Agent × N Tasks)
                     │        └──────┬───────┘
                     │               │ tasks completed
                     │               ▼
                     │        ┌──────────────┐
                     │        │  REVIEWING   │ (Critic Agent)
                     │        └──────┬───────┘
                     │               │
                     │               ├── 新缺口发现 + iter < max ──→ GAP_AGENT → RESEARCH_AGENT ──┐
                     │               │                                                          │
                     │               │ 新缺口发现 + iter >= max ──→ 降级 (记录未解决缺口)          │
                     │               │                                                          │
                     │               ▼                                                          │
                     │        ┌──────────────┐                                                  │
                     │        │ SYNTHESIZING │                                                  │
                     │        └──────┬───────┘                                                  │
                     │               │                                                          │
                     │               ▼                                                          │
                     │        ┌──────────────┐                                                  │
                     │        │   WRITING    │ (Knowledge Writer)                               │
                     │        │  CANDIDATES  │                                                  │
                     │        └──────┬───────┘                                                  │
                     │               │                                                          │
                     │               ▼                                                          │
                     │        ┌──────────────┐                                                  │
                     │        │  COMPLETED   │                                                  │
                     │        └──────────────┘                                                  │
                     │                                                                          │
                     └──────────────────────────────────────────────────────────────────────────┘
                                              循环迭代 (max 3 轮)

                     ┌──────────────┐
                     │   RUNNING    │── pause() ──→ PAUSED ──→ execute() → (从中断点恢复)
                     └──────────────┘
                            │
                            └── cancel() ──→ CANCELLED (不可恢复, 保留已收集数据)
                            │
                            └── 超时 (60min 无进展) ──→ FAILED (可重试)
```

### 3.3 暂停/恢复机制

```
暂停流程：
  1. 用户点击暂停
  2. Orchestrator 发送 cancel 信号给当前 Agent
  3. Agent 完成当前 Step (不中断正在执行的 Tool 调用)
  4. 序列化 AgentContext → JSON
  5. 写入 agent_execution.context_snapshot
  6. 写入 Redis: research:context:{projectId}
  7. project.status = PAUSED
  8. 前端收到 project_paused 事件

恢复流程：
  1. 用户点击继续
  2. POST /projects/{id}/execute
  3. 检查 project.status == PAUSED
  4. 从 Redis 加载 context_snapshot (fallback: DB context_snapshot)
  5. 反序列化 AgentContext
  6. 定位 current_task_id + current_agent
  7. 确定下一个应执行的 Agent
  8. 创建新 agent_execution 记录 (retry_count++)
  9. project.status = 上次暂停前的状态 (RESEARCHING/REVIEWING/...)
  10. 从下一个 PENDING Step 继续执行

可中断点：
  - Agent 之间切换时 (最安全)
  - Step 完成之后 (当前 Step 不中断)
  - Tool 调用结果返回后 (不中断 Tool 执行)
```

---

## 四、Budget 预算控制

### 4.1 Loop Budget（最大循环次数）

```
硬限制: max_iterations = 3 (可配置，范围 1-5)

循环计数规则:
  - Planner → Knowledge → Gap → Research → Critic = 1 轮
  - Critic 发现新缺口 → Gap → Research → Critic = +1 轮
  - 达到 max_iterations 后 → 强制执行 SynthesizerAgent
  - 未解决的缺口记录在 report.knowledgeGaps 中

循环检测 (防无限):
  Orchestrator 维护 agentExecutionHistory:
    - 检测重复序列 pattern (如 GAP→RESEARCH→CRITIC→GAP→RESEARCH→CRITIC)
    - 如果最近 5 个 Agent 调用序列与之前完全相同 → 终止循环
    - agentName + taskId 相同的连续 3 次执行 → 终止循环
```

### 4.2 Research Budget（研究预算）

```
全局配额 (per user per day):
  - 最大研究项目数: 20 个/天
  - 最大并发项目: 1 个/用户 (其他排队)
  - 单项目最大执行次数: 5 次 (含重试)

项目级配额:
  - 最大 Task 数: 10 个/项目
  - 单 Task 最大 Source 数: 20 个
  - 单 Task 最大 Finding 数: 15 个
  - 总 Source 数: 50 个/项目
  - 总 Conclusion 数: 30 个/项目

超出限制的处理:
  - Task 数超限 → Planner 输出中只保留 top 10 (按 priority 排序)
  - Source 数超限 → 按 relevanceScore 截断
  - Finding 数超限 → 按 confidence 截断
```

### 4.3 Token Budget（Token 预算）

```
项目级:
  - 总 Token 预算: 150,000 tokens/项目
  - 其中 prompt tokens: 100,000
  - 其中 completion tokens: 50,000

Agent 级分配:
  - Planner: 5,000
  - Knowledge: 8,000
  - Gap: 6,000
  - Research (per Task): 12,000
  - Critic (per batch): 10,000
  - Synthesizer: 20,000
  - KnowledgeWriter: 8,000

超限处理:
  - Agent 级超限 → 截断输出 + 标记 tokenExceeded
  - 项目级超限 → 终止当前 Agent + 强制 Synthesizer 用已有数据生成报告
  - 80% 阈值预警 → Orchestrator 发送 SSE warning 事件给前端

Token 追踪:
  每次 LLM 调用后记录 agent_message (prompt_tokens + completion_tokens)
  Orchestrator 维护 runningTokenCount
```

### 4.4 Time Budget（时间预算）

```
项目级:
  - 总时间预算: 10 分钟/项目 (600,000ms)
  - 无进展超时: 5 分钟 (300,000ms) → 自动标记 FAILED

Agent 级:
  - Planner: 60s
  - Knowledge: 30s
  - Gap: 40s
  - Research (per Task): 120s
  - Critic: 60s
  - Synthesizer: 120s
  - KnowledgeWriter: 40s

Tool 级:
  - WebSearchTool: 10s/call
  - WebFetchTool: 15s/page
  - ExtractTool: 15s
  - KnowledgeSearchTool: 10s
  - KnowledgeReadTool: 3s
  - VerifyTool: 5s

超限处理:
  - Agent 超时 → 标记当前 Step FAILED + 尝试降级
  - 项目超时 → 保存快照 + 强制 Synthesizer + 标记 TIMED_OUT
```

### 4.5 Tool Call Budget（Tool 调用预算）

```
项目级:
  - 总 Tool 调用次数: 100 次/项目
  - WebSearchTool: 10 次/项目 (2-3 关键词 × 3 Task)
  - WebFetchTool: 30 次/项目 (每个搜索 Top 10 URL)
  - ExtractTool: 30 次/项目
  - KnowledgeSearchTool: 3 次/项目
  - KnowledgeReadTool: 10 次/项目
  - VerifyTool: 20 次/项目
  - KnowledgeGraphTool: 5 次/项目

并发控制:
  - WebFetchTool 并发: 5 个
  - WebSearchTool 并发: 2 个
  - KnowledgeSearchTool 并发: 3 个 (语义/关键词/图谱)

超限处理:
  - 达到限制 → 停止该类型 Tool 的调用
  - 不影响其他类型 Tool
```

---

## 五、失败场景全面处理

### 5.1 Agent 执行失败

| 场景 | 检测方式 | 处理 |
|------|----------|------|
| Planner 失败 | LLM 返回非 JSON 或超时 | 降级为 SIMPLE 流程 (Knowledge → Synthesizer) |
| Knowledge Agent 失败 | 全部检索渠道不可用 | 返回空知识列表，标记 `knowledgeScarce: true`，继续执行 |
| Gap Agent 失败 | 图谱不可用 + LLM 失败 | 跳过 Gap 阶段，直接进入 Research |
| Research Agent 失败 (单 Task) | 搜索 + 抓取全部失败 | 该 Task 标记 FAILED，继续下一个 Task |
| Research Agent 失败 (全部 Task) | 所有 Task 均 FAILED | 降级为仅使用知识库内容生成报告 |
| Critic Agent 失败 | LLM 连续 2 次失败 | 所有 finding 直接 promote（不验证），confidence 保持原始值 |
| Synthesizer 失败 | LLM 失败或 token 超限 | 生成最简单的摘要报告（无 LLM，纯模板拼接） |
| KnowledgeWriter 失败 | Embedding 服务不可用 | 跳过，标记 `writeDisabled: true`，用户可稍后手动执行 write-back |

### 5.2 Tool 调用失败

| 场景 | Tool | 处理 |
|------|------|------|
| ES 集群不可用 | KnowledgeSearchTool | 降级为纯 Vector Search |
| Milvus/Qdrant 不可用 | KnowledgeSearchTool | 降级为纯 ES 搜索 |
| 图谱服务不可用 | KnowledgeGraphTool | 跳过图谱扩展，标记 `graphDegraded: true` |
| Search API 限流 (429) | WebSearchTool | 退避 5s 后重试 1 次 |
| Search API 无结果 | WebSearchTool | 返回空列表，标记 `noResults: true` |
| 网页 403/404 | WebFetchTool | 跳过该 URL |
| 网页超时 | WebFetchTool | 跳过该 URL，不计入重试 |
| 网页内容过大 (>1MB) | WebFetchTool | 截取前 100KB |
| ExtractTool LLM 超时 | ExtractTool | 仅保留 snippet，不生成 finding |
| 数据库写入失败 | KnowledgeWriteTool | 记录错误日志，返回 partial success |

### 5.3 网络搜索失败

```
WebSearchTool 三级降级:

Level 1: Primary Search API (Brave Search / SerpAPI)
  └─ 失败 → Level 2: Fallback Search API (DuckDuckGo)
       └─ 失败 → Level 3: 返回空结果 + 标记 searchUnavailable

所有搜索均失败的处理:
  - 不影响 Knowledge Agent (内部搜索)
  - Research Agent 该 Task 标记 FAILED
  - Orchestrator 尝试下一个 Task (如果有)
  - 所有 Task 均无搜索结果 → 降级为仅用知识库内容
```

### 5.4 LLM 调用失败

```
AiService 统一处理 (复用现有):

1. 单次失败:
   - 自动重试 1 次 (复用现有 AiService 的重试逻辑)
   - 如果是 streaming 调用，降级为非 streaming 调用

2. Provider 不可用:
   - 切换到备用 Provider (按 user_ai_config 优先级)
   - 通知用户建议配置备用 Provider

3. Token 超限 (context too long):
   - 截断输入 (保留 system prompt + 最近 N 条消息)
   - 如果是 Synthesizer，分批处理

4. Rate Limit (429):
   - 退避 10s 后重试
   - 重试 2 次后仍 429 → 标记 `provider_rate_limited`
```

### 5.5 Token 超限

```
检测:
  - 每次 LLM 调用前，用 tokenizer 估算 token 数
  - 运行时追踪累计 token (agent_message 汇总)

处理:
  阶段 1 (80% 预警):
    - Orchestrator 发送 warning SSE 事件
    - 后续 Agent 使用更简洁的 prompt (减少 system prompt 长度)
    - 关闭 verbose logging (减少 agent_message 记录)

  阶段 2 (90% 紧急):
    - 停止非必要的 Agent (如跳过 GapAgent 的二次分析)
    - 截断 source.fullContent (只保留 snippet)
    - 合并 batch 处理

  阶段 3 (100% 超限):
    - 终止当前 Agent
    - 强制 Synthesizer 用已有数据生成报告
    - 报告中标注 "tokenBudgetExceeded: true"
```

### 5.6 用户中断

```
中断类型:

1. 取消 (Cancel):
   - Orchestrator 发送 cancel 信号
   - 当前 Agent 完成当前 Step 后停止
   - project.status = CANCELLED
   - 已保存的数据 (sources, findings, steps) 保留
   - 不可恢复

2. 暂停 (Pause):
   - 当前 Agent 完成当前 Step
   - 序列化 AgentContext → context_snapshot
   - project.status = PAUSED
   - 可恢复

3. 浏览器关闭/断线:
   - 后端继续执行 (不依赖前端连接)
   - 前端重连后:
     1. GET /projects/{id}/status 获取最新状态
     2. 重新建立 SSE 连接
     3. 拉取错过的 events (从 research_step 表补充)
```

### 5.7 Agent 无限循环防护

```
1. 硬限制: max_iterations = 3

2. 循环模式检测:
   a. Agent 序列检测:
      - 维护最近 10 个 Agent 名称的滑动窗口
      - 检测重复子序列 (如 GAP→RESEARCH→CRITIC)
      - 重复出现 3 次 → 终止循环

   b. Tool 调用去重:
      - 维护已调用 Tool 的哈希表 (toolName + MD5(input))
      - 完全相同参数重复调用 2 次 → 拒绝执行

   c. 搜索结果去重:
      - 维护每个 Task 的 seenUrls 集合
      - 已抓取的 URL 跳过

3. 研究结论循环:
   - 新 finding 的 statement 与已有 conclusion 的 statement
   - 语义相似度 > 0.9 → 跳过 (已得出结论)

4. 超时强制终止:
   - 项目总执行时间 > 10 分钟 → 强制 Synthesizer
```

---

## 六、结构化输出规范

### 6.1 所有 Agent 输出必须包含的元数据

```json
{
  "meta": {
    "agentName": "ResearchAgent",
    "executionId": 42,
    "projectId": 1,
    "taskId": 2,
    "timestamp": "2026-07-28T10:30:00Z",
    "durationMs": 18500,
    "status": "COMPLETED",
    "tokensUsed": {
      "prompt": 5000,
      "completion": 3000
    },
    "retryCount": 0,
    "errors": []
  }
}
```

### 6.2 错误输出规范

```json
{
  "meta": {
    "agentName": "ResearchAgent",
    "status": "FAILED",
    "errors": [
      {
        "code": "WEB_SEARCH_TIMEOUT",
        "message": "Brave Search API 超时 (10s)",
        "retryable": true,
        "retryCount": 1,
        "maxRetries": 2,
        "severity": "WARNING"
      },
      {
        "code": "WEB_FETCH_PARTIAL",
        "message": "8 个 URL 中 2 个抓取失败: [https://example.com (timeout), https://example2.com (404)]",
        "retryable": false,
        "severity": "INFO"
      }
    ],
    "degradedOutput": {
      "sources": [ ... ],
      "stats": {
        "pagesFetched": 8,
        "pagesSucceeded": 6,
        "pagesFailed": 2
      }
    }
  }
}
```

### 6.3 SSE 事件规范

```json
// Agent 开始
{"event": "agent_started", "data": {"agentName": "PlannerAgent", "title": "正在分析研究目标...", "timestamp": "..."}}

// Step 开始
{"event": "step_started", "data": {"agentName": "PlannerAgent", "stepType": "THINKING", "title": "理解研究目标", "stepIndex": 1, "totalSteps": 3}}

// Step 进度
{"event": "step_progress", "data": {"agentName": "PlannerAgent", "content": "正在评估用户知识背景...", "progress": 0.5}}

// Tool 调用
{"event": "tool_call", "data": {"agentName": "ResearchAgent", "toolName": "WebSearchTool", "input": {"query": "Spring AI Agent"}, "toolCallIndex": 1}}

// Tool 结果
{"event": "tool_result", "data": {"agentName": "ResearchAgent", "toolName": "WebSearchTool", "summary": "找到 8 个结果", "durationMs": 3200}}

// 发现
{"event": "finding_found", "data": {"agentName": "ResearchAgent", "statement": "...", "confidence": "high", "sourceCount": 2}}

// 结论
{"event": "conclusion_reached", "data": {"agentName": "CriticAgent", "statement": "...", "confidence": "high", "isKeyFinding": true}}

// 冲突
{"event": "conflict_detected", "data": {"agentName": "CriticAgent", "topic": "Agent Memory 后端选择", "positionA": "...", "positionB": "..."}}

// Task 状态
{"event": "task_status", "data": {"taskId": 1, "status": "COMPLETED", "title": "Spring AI Agent 基础概念"}}

// 项目状态
{"event": "project_status", "data": {"status": "RESEARCHING", "progress": 0.45, "currentIteration": 1, "maxIterations": 3}}

// 预算预警
{"event": "budget_warning", "data": {"type": "TOKEN", "current": 120000, "limit": 150000, "percentage": 0.80}}

// 错误
{"event": "error", "data": {"code": "WEB_SEARCH_TIMEOUT", "message": "外部搜索超时，正在重试...", "severity": "WARNING", "retryable": true}}

// 完成
{"event": "project_completed", "data": {"projectId": 1, "summary": "研究完成", "durationMs": 85000, "totalTokens": 52000}}
```

---

## 七、Agent 间通信协议

### 7.1 AgentContext 结构

```java
public class AgentContext {
    // 基础信息
    Long projectId;
    Long userId;
    Long workspaceId;
    Long executionId;

    // 研究目标
    String researchGoal;
    ResearchPlan researchPlan;       // Planner 输出

    // 研究中间结果 (按 Agent 名索引)
    Map<String, Object> agentOutputs;  // "PlannerAgent" → plan, "KnowledgeAgent" → references

    // 累积数据
    List<ResearchSource> allSources;
    List<ResearchFinding> allFindings;
    List<ResearchConclusion> allConclusions;
    List<KnowledgeGap> allGaps;

    // 执行状态
    String currentAgent;
    Long currentTaskId;
    int currentIteration;
    int maxIterations;
    List<String> agentExecutionHistory;  // ["PlannerAgent", "KnowledgeAgent", ...]

    // 预算追踪
    TokenBudget tokenBudget;
    TimeBudget timeBudget;
    ToolCallBudget toolCallBudget;

    // 失败信息
    List<AgentError> errors;
    boolean degradedMode;

    // 检查点
    Instant lastCheckpoint;
    String checkpointAgent;
}
```

### 7.2 Agent 间数据传递方式

```
Orchestrator 在 Agent 间传递 AgentContext:

1. Agent A 执行完毕 → 输出写入 agentOutputs["AgentA"]
2. Orchestrator 提取 Agent B 所需的输入 → 从 agentOutputs 组装
3. Agent B 执行 → 读取输入 + 可选读取全局状态 (allSources, allConclusions)
4. Agent B 输出 → 写入 agentOutputs["AgentB"]
5. 关键数据同步写入 allSources/allFindings/allConclusions

Agent 之间不直接通信 — 所有交互通过 Orchestrator + AgentContext 中介。
```

---

## 八、实施要点

### 8.1 Agent 实现约束

1. **无状态 Agent**：所有 Agent 实现 `ResearchAgent` 接口，不持有实例字段状态。所有状态通过 `AgentContext` 传递。
2. **幂等性**：相同输入 + 相同 context 应产生相同输出（LLM 非确定性除外）。Tool 调用通过 content_hash / execution_key 去重。
3. **可中断**：Agent 的 `execute()` 支持通过 `AgentContext` 中的 cancel 信号中断。长时间 Tool 调用通过 Future.cancel() 中断。
4. **可观测**：每个 Step 都记录到 `research_step` 表。每个 LLM 调用记录到 `agent_message` 表。
5. **降级优先**：外部依赖失败不应导致整个研究失败。Agent 应设计降级路径。

### 8.2 Orchestrator 职责

- 根据 ResearchPlan 中的 agentChain 依次调度 Agent
- 管理 AgentContext 的生命周期
- 监控 Budget 使用情况
- 处理暂停/恢复/取消
- 推送 SSE 事件
- 保存检查点
- 循环检测与终止

### 8.3 与现有系统的集成点

| 集成点 | 现有模块 | 调用方式 |
|--------|----------|----------|
| LLM 调用 | AiService / StreamingAiService | 所有 Agent 统一使用 |
| 知识检索 | ElasticsearchService, VectorSearchService | KnowledgeSearchTool |
| 图谱查询 | KnowledgeGraphService | KnowledgeGraphTool |
| 知识读写 | KnowledgeService, PendingKnowledgeService | KnowledgeReadTool, KnowledgeWriteTool |
| 异步任务 | AsyncTaskService, Kafka | ResearchProject 级别 |
| 实时推送 | WebSocket STOMP | SSE 事件推送 |
| 缓存 | CacheService (Redis) | AgentContext 快照, 搜索缓存 |
| 用户认证 | JWT + Spring Security | 所有 Controller |
| 工作空间 | WorkspaceService | 权限校验 |

---

## 附录 A：Agent 接口定义

```java
package com.secondbrain.research.agent;

import com.secondbrain.research.orchestrator.AgentContext;

/**
 * Agent 接口。
 *
 * 所有 Research Agent 必须实现此接口。Agent 实现必须是无状态的 ——
 * 所有执行状态通过 AgentContext 传递。
 */
public interface ResearchAgent {

    /**
     * Agent 唯一标识。
     */
    String getName();

    /**
     * 执行 Agent 逻辑。
     *
     * @param context 当前研究上下文（包含所有中间结果和预算状态）
     * @return Agent 执行结果（结构化 JSON）
     */
    AgentResult execute(AgentContext context);

    /**
     * Orchestrator 在调度前调用，判断此 Agent 是否应执行。
     *
     * @param context 当前研究上下文
     * @return true 表示应执行
     */
    default boolean shouldExecute(AgentContext context) {
        return true;
    }

    /**
     * Agent 执行超时时间（毫秒）。
     */
    default long getTimeoutMs() {
        return 120_000;
    }

    /**
     * 最大重试次数。
     */
    default int getMaxRetries() {
        return 1;
    }
}
```

## 附录 B：预算追踪器接口

```java
package com.secondbrain.research.orchestrator;

/**
 * Token 预算追踪器。
 */
public interface TokenBudget {
    void consume(String agentName, int promptTokens, int completionTokens);
    boolean isExceeded();
    boolean isWarning();       // > 80%
    int getRemaining();
    int getTotal();
}

/**
 * 时间预算追踪器。
 */
public interface TimeBudget {
    void startAgent(String agentName, long timeoutMs);
    void endAgent(String agentName);
    boolean isAgentTimeout(String agentName);
    boolean isProjectTimeout();
    long getElapsedMs();
}

/**
 * Tool 调用预算追踪器。
 */
public interface ToolCallBudget {
    boolean canCall(String toolName);
    void recordCall(String toolName, String inputHash);
    int getRemaining(String toolName);
    boolean isDuplicateCall(String toolName, String inputHash);
}
```
