# AI Research Agent 架构设计文档

> 基于 AI-SecondBrain 现有代码库的完整架构设计
> 日期：2026-07-28
> 状态：DRAFT

---

## 前置分析：现有能力复用评估

### 可直接复用的能力

| 现有模块 | 复用方式 | 说明 |
|----------|----------|------|
| 用户认证 (JWT + Spring Security) | 直接复用 | Research Agent 所有操作均需 userId + workspaceId |
| 知识节点 CRUD (knowledge_node) | 直接复用 | Knowledge Agent 读取，Knowledge Writer Agent 写入 |
| 知识图谱 (knowledge_relation) | 直接复用 | 研究依赖图谱发现隐性知识缺口 |
| 向量/Embedding (knowledge_embedding) | 直接复用 | 语义搜索用户已有知识 |
| Elasticsearch (关键词+语义搜索) | 直接复用 | 多维检索用户知识库 |
| RAG 管线 (检索→上下文→生成) | 直接复用 | Knowledge Agent 核心检索机制 |
| AI 服务层 (多Provider/流式/用户配置) | 直接复用 | 所有 Agent 的 LLM 调用统一走 AiService / StreamingAiService |
| 异步任务系统 (async_task + Kafka) | 直接复用 | Research Task 的异步执行与状态追踪 |
| WebSocket (STOMP) | 直接复用 | Agent 执行过程实时推送到前端 |
| Redis (缓存/分布式锁) | 直接复用 | Research Session 状态缓存、并发控制 |
| Workspace 隔离 | 直接复用 | Research Project 归属 workspace，成员共享 |
| 通知系统 | 直接复用 | 研究完成/发现新知时推送通知 |
| 标签系统 | 直接复用 | Knowledge Writer 生成的知识点关联标签 |
| 导出系统 | 直接复用 | 导出研究报告 |
| Ebbinghaus 复习 | 直接复用 | 研究产生的新知识纳入复习计划 |

### 需要扩展的能力

| 现有模块 | 扩展内容 |
|----------|----------|
| `knowledge_node` | 新增 `source_type`（外部研究/RAG提取/手动）、`source_id`（关联 research_source） |
| `knowledge_relation` | 新增关系类型：`derived_from`（研究推导）、`supports`（证据支持）、`contradicts`（证据矛盾） |
| `async_task` | 新增 task_type：`RESEARCH_STEP`、`RESEARCH_PROJECT` |
| `ai_provider` / `user_ai_config` | 新增 `research` scenario 的默认 provider/model 配置 |
| Kafka topics | 新增 `research-workflow` topic |
| WebSocket channels | 新增 `/topic/research/{projectId}` 频道 |

### 需要新建的模块

后台新增 7 张表、8 个 Service、1 个 Controller、7 个 Agent 类；前端新增 2 个页面、3 个组件。

---

## 一、整体系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                              │
│  Research.vue ─→ ResearchWorkspace.vue ─→ ResearchProject.vue   │
│       │              │                    │                      │
│       ▼              ▼                    ▼                      │
│  学习路径/盲区    项目列表/创建      研究过程可视化              │
│  分析入口         项目管理            Agent步骤展示              │
└─────────────────────────────────────────────────────────────────┘
                              │ HTTP + SSE + WebSocket
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Controller 层                                 │
│  ResearchController    POST /research/projects                   │
│                        POST /research/projects/{id}/execute      │
│                        GET  /research/projects/{id}/status       │
│                        GET  /research/projects/{id}/stream (SSE) │
│                        GET  /research/projects/{id}/result       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Orchestration 层                              │
│  ResearchOrchestrator ── 编排 Planner → Knowledge Agent          │
│       │                     → Gap Agent → Research Agent         │
│       │                     → Critic → Synthesizer               │
│       │                     → Knowledge Writer                   │
│       ▼                                                          │
│  AgentContext ── 研究上下文（userId, projectId, 状态机,          │
│                  中间结果, source列表, 结论列表）                 │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────────┐
│  Agent 层        │ │  Tool 层     │ │  Memory 层       │
│  PlannerAgent    │ │ KnowledgeTool│ │ ResearchMemory    │
│  KnowledgeAgent  │ │ SearchTool   │ │ (Redis + DB)     │
│  GapAgent        │ │ WebSearchTool│ │ 会话状态          │
│  ResearchAgent   │ │ ExtractTool  │ │ 中间结果          │
│  CriticAgent     │ │ VerifyTool   │ │ Agent思考链       │
│  SynthesizerAgent│ │ WriteTool    │ │                   │
│  WriterAgent     │ └──────────────┘ └──────────────────┘
└─────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Infrastructure 层（全部复用现有）              │
│  AiService │ StreamingAiService │ ElasticsearchService          │
│  VectorSearchService │ EmbeddingService │ RagService            │
│  KnowledgeService │ KnowledgeGraphService │ CacheService        │
│  Kafka │ WebSocket │ AsyncTaskService │ NotificationService     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、模块划分

```
backend/src/main/java/com/secondbrain/
├── research/                          # 新增：AI Research Agent 模块
│   ├── agent/                         # Agent 实现
│   │   ├── ResearchAgent.java         # Agent 接口
│   │   ├── PlannerAgent.java          # 规划 Agent
│   │   ├── KnowledgeAgent.java        # 知识库分析 Agent
│   │   ├── GapAgent.java              # 知识盲区分析 Agent
│   │   ├── ResearchAgentImpl.java     # 外部研究 Agent
│   │   ├── CriticAgent.java           # 验证 Agent
│   │   ├── SynthesizerAgent.java      # 综合 Agent
│   │   └── KnowledgeWriterAgent.java  # 知识沉淀 Agent
│   ├── orchestrator/                  # 编排层
│   │   ├── ResearchOrchestrator.java  # 研究编排器
│   │   └── AgentContext.java          # Agent 执行上下文
│   ├── tool/                          # Agent Tools
│   │   ├── Tool.java                  # Tool 接口
│   │   ├── KnowledgeSearchTool.java   # 知识库搜索
│   │   ├── WebSearchTool.java         # 外部搜索
│   │   ├── KnowledgeGraphTool.java    # 图谱查询
│   │   └── KnowledgeWriteTool.java    # 知识写入
│   ├── memory/                        # Research Memory
│   │   └── ResearchMemoryService.java # 会话状态管理
│   ├── controller/
│   │   └── ResearchController.java    # REST API
│   ├── service/
│   │   ├── ResearchProjectService.java
│   │   └── ResearchSourceService.java
│   ├── entity/
│   │   ├── ResearchProject.java
│   │   ├── ResearchTask.java
│   │   ├── ResearchStep.java
│   │   ├── ResearchSource.java
│   │   ├── ResearchConclusion.java
│   │   └── ResearchResult.java
│   ├── mapper/
│   │   ├── ResearchProjectMapper.java
│   │   ├── ResearchTaskMapper.java
│   │   ├── ResearchStepMapper.java
│   │   ├── ResearchSourceMapper.java
│   │   ├── ResearchConclusionMapper.java
│   │   └── ResearchResultMapper.java
│   └── dto/
│       ├── CreateResearchProjectRequest.java
│       ├── ResearchProjectResponse.java
│       ├── ResearchStepEvent.java
│       ├── ResearchResultResponse.java
│       └── ResearchSourceDTO.java
```

---

## 三、Agent 架构

### Agent 接口设计

```java
public interface ResearchAgent {
    /**
     * 执行 Agent，返回下一步的 Agent 类型（或 null 表示结束）。
     * Agent 实现不应持有状态，所有上下文通过 AgentContext 传递。
     */
    String execute(AgentContext context);

    /**
     * 当前 Agent 是否应该执行。
     * 由 Planner 或上一 Agent 动态决定。
     */
    default boolean shouldExecute(AgentContext context) { return true; }
}
```

### 七个 Agent 职责

| Agent | 输入 | 输出 | 调用的 Tool |
|-------|------|------|------------|
| **PlannerAgent** | 研究目标 + 用户知识背景 | 结构化 ResearchPlan (Task[] + Step[]) | 无（纯 LLM 推理） |
| **KnowledgeAgent** | 研究主题/关键词 | 相关知识节点列表 + 掌握评估 | KnowledgeSearchTool, KnowledgeGraphTool |
| **GapAgent** | 知识图谱 + 研究目标 | 显性缺口 + 隐性缺口列表 | KnowledgeGraphTool |
| **ResearchAgent** | 研究子问题 + 知识缺口 | 外部资料列表 + 来源标注 | WebSearchTool |
| **CriticAgent** | 研究结论 + 来源列表 | 可信度评分 + 冲突标记 | WebSearchTool, VerifyTool |
| **SynthesizerAgent** | 所有中间结果 | 研究报告 (Markdown) + 关键发现 | 无（纯 LLM 推理） |
| **KnowledgeWriterAgent** | 新知识 + 研究报告 | 待确认知识列表（写入 knowledge_node + knowledge_relation） | KnowledgeWriteTool |

---

## 四、Agent Workflow

### 动态工作流选择

Planner Agent 分析研究目标复杂度后动态决定：

**简单研究**（仅需知识库查询）：
```
User → Planner → Knowledge Agent → Synthesizer → 结果
```
跳过 Gap、Research、Critic Agent。

**标准研究**（需要外部信息）：
```
User → Planner → Knowledge Agent → Gap Agent → Research Agent
              → Critic Agent → Synthesizer → Knowledge Writer
```

**深度研究**（复杂主题，迭代验证）：
```
User → Planner → Knowledge Agent → Gap Agent
     → Research Agent → Critic Agent
     → [发现新缺口 → Gap Agent → Research Agent → Critic Agent]  ← 循环最多 3 轮
     → Synthesizer → Knowledge Writer
```

### 关键决策点

Planner Agent 判断逻辑：
1. 用户知识库中已有足够相关内容 → 跳过 Research Agent
2. 研究主题是全新领域 → 必须走完整流程
3. 用户指定了特定方向 → 跳过 Gap Agent

---

## 五、Research Project 生命周期

```
状态流转：
  DRAFT ──→ PLANNING ──→ RESEARCHING ──→ REVIEWING ──→ SYNTHESIZING ──→ COMPLETED
    │          │              │               │               │               │
    │          │              │               │               │               ▼
    │          │              │               │               │           ARCHIVED
    │          │              │               │               │
    └──────────┴──────────────┴───────────────┴───────────────┴────→ FAILED
                                                                      │
                                                                      ▼
                                                                   RETRY
```

**状态说明**：
- `DRAFT`：用户创建项目，填写研究目标，尚未启动
- `PLANNING`：Planner Agent 执行中，生成 Research Plan
- `RESEARCHING`：Research Agent 执行中，检索外部信息
- `REVIEWING`：Critic Agent 验证中
- `SYNTHESIZING`：Synthesizer Agent 生成报告中
- `COMPLETED`：研究完整通过质量门禁，结果可供查看
- `PARTIAL`：已产出部分结果，但存在失败、预算耗尽或质量门禁未通过
- `ARCHIVED`：用户归档
- `FAILED`：执行失败（外部搜索失败、LLM 调用失败等）

---

## 六、Research Task 生命周期

一个 Research Project 包含多个 Research Task：

```
Research Project: "Spring AI Agent 架构研究"
  ├── Task 1: Spring AI Agent 基础概念 (PENDING → COMPLETED)
  ├── Task 2: Tool Calling 机制研究 (PENDING → RUNNING → COMPLETED)
  ├── Task 3: Agent Memory 设计模式 (PENDING)
  └── Task 4: Spring AI vs LangChain Agent 对比 (PENDING)
```

**Task 状态**：
```
PENDING → RUNNING → COMPLETED
    │         │
    │         └──→ FAILED → RETRY → RUNNING
    └──→ WAITING_USER (需要用户输入/确认)
```

**Task 依赖**：Task 之间可设置前置依赖（如 Task 3 依赖 Task 2 完成）。

---

## 七、Agent 状态机

每个 Agent 执行时的内部状态：

```
IDLE → THINKING → TOOL_CALLING → WAITING_TOOL_RESULT → PROCESSING_RESULT
  │                                                     │
  └─────────────────────────────────────────────────────┘
                                                         │
                                                    COMPLETED / FAILED
```

每个 Step 产生一个 `ResearchStepEvent`，通过 SSE/WebSocket 推送到前端。

---

## 八、数据库设计

### 新增表

```sql
-- 研究项目表
CREATE TABLE research_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    workspace_id BIGINT,
    title VARCHAR(300) NOT NULL COMMENT '研究标题',
    goal TEXT NOT NULL COMMENT '研究目标',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        COMMENT 'DRAFT/PLANNING/RESEARCHING/REVIEWING/SYNTHESIZING/COMPLETED/ARCHIVED/FAILED',
    complexity VARCHAR(20) COMMENT 'SIMPLE/STANDARD/DEEP',
    plan_json JSON COMMENT 'Research Plan（结构化 JSON）',
    result_summary TEXT COMMENT '研究结论摘要',
    result_report LONGTEXT COMMENT '完整研究报告（Markdown）',
    agent_workflow VARCHAR(50) COMMENT '实际执行的 Agent 链路',
    max_iterations INT DEFAULT 3 COMMENT '最大迭代轮数',
    current_iteration INT DEFAULT 0 COMMENT '当前迭代轮数',
    started_at DATETIME,
    completed_at DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究项目表';

-- 研究任务表
CREATE TABLE research_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL COMMENT '任务标题',
    description TEXT COMMENT '任务描述',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/WAITING_USER',
    depends_on BIGINT COMMENT '依赖的前置任务 ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    async_task_id BIGINT COMMENT '关联的异步任务 ID',
    result_summary TEXT COMMENT '任务结果摘要',
    started_at DATETIME,
    completed_at DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究任务表';

-- 研究步骤表（Agent 执行过程记录）
CREATE TABLE research_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    agent_name VARCHAR(50) NOT NULL COMMENT 'Agent 名称',
    step_type VARCHAR(50) NOT NULL COMMENT '步骤类型：THINKING/TOOL_CALL/TOOL_RESULT/PROCESSING/COMPLETED',
    title VARCHAR(200) COMMENT '步骤描述',
    content TEXT COMMENT '步骤内容',
    tool_name VARCHAR(50) COMMENT '使用的 Tool 名称',
    tool_input TEXT COMMENT 'Tool 输入参数（JSON）',
    tool_output TEXT COMMENT 'Tool 输出结果',
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/COMPLETED/FAILED',
    sort_order INT DEFAULT 0,
    duration_ms INT COMMENT '耗时（毫秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_agent_name (agent_name),
    FOREIGN KEY (task_id) REFERENCES research_task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究步骤表（Agent 执行过程）';

-- 研究来源表
CREATE TABLE research_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT,
    title VARCHAR(500) NOT NULL COMMENT '来源标题',
    url VARCHAR(2000) COMMENT '来源 URL',
    source_type VARCHAR(30) NOT NULL COMMENT '来源类型：web_search/official_doc/paper/github/article/internal',
    snippet TEXT COMMENT '内容摘要',
    full_content LONGTEXT COMMENT '完整内容（如已抓取）',
    relevance_score DECIMAL(5,4) COMMENT '相关性评分',
    reliability VARCHAR(20) COMMENT '可靠性：high/medium/low/unverified',
    fetched_at DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    INDEX idx_task_id (task_id),
    INDEX idx_source_type (source_type),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究来源表';

-- 研究结论表
CREATE TABLE research_conclusion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT,
    statement TEXT NOT NULL COMMENT '结论陈述',
    confidence VARCHAR(20) NOT NULL DEFAULT 'medium' COMMENT '可信度：high/medium/low/speculation',
    supporting_sources JSON COMMENT '支持该结论的来源 ID 列表',
    conflicting_sources JSON COMMENT '冲突来源 ID 列表',
    is_key_finding TINYINT DEFAULT 0 COMMENT '是否为关键发现',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    INDEX idx_confidence (confidence),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究结论表';

-- 研究结果（最终输出）
CREATE TABLE research_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    summary TEXT,
    research_questions JSON COMMENT '研究问题列表',
    key_findings JSON COMMENT '关键发现列表',
    knowledge_gaps JSON COMMENT '发现的知识缺口',
    new_knowledge_ids JSON COMMENT '新写入知识库的 knowledge_node ID 列表',
    new_relation_ids JSON COMMENT '新写入知识库的 knowledge_relation ID 列表',
    report_markdown LONGTEXT COMMENT '完整研究报告（Markdown）',
    source_count INT DEFAULT 0,
    conclusion_count INT DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_project_id (project_id),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究结果表';
```

---

## 九、API 设计

### ResearchController REST API

```
POST   /api/research/projects                    # 创建研究项目
GET    /api/research/projects                    # 项目列表（分页，支持状态筛选）
GET    /api/research/projects/{id}               # 项目详情
PUT    /api/research/projects/{id}               # 更新项目（DRAFT 状态可编辑）
DELETE /api/research/projects/{id}               # 删除项目（仅 DRAFT/COMPLETED/FAILED）

POST   /api/research/projects/{id}/execute       # 启动/恢复研究执行
POST   /api/research/projects/{id}/pause         # 暂停研究
POST   /api/research/projects/{id}/retry         # 重试失败的研究

GET    /api/research/projects/{id}/stream        # SSE 流：实时 Agent 执行过程
                                                  # event: step_started, step_progress,
                                                  #         step_completed, task_completed,
                                                  #         conclusion_found, project_completed,
                                                  #         error

GET    /api/research/projects/{id}/tasks          # 项目下的任务列表
GET    /api/research/projects/{id}/tasks/{tid}    # 任务详情（含 steps）
GET    /api/research/projects/{id}/tasks/{tid}/steps  # 任务步骤列表

GET    /api/research/projects/{id}/sources        # 研究来源列表
GET    /api/research/projects/{id}/conclusions    # 研究结论列表
GET    /api/research/projects/{id}/result         # 研究最终结果

POST   /api/research/projects/{id}/write-back     # 将研究结果沉淀到知识库
                                                   # （Knowledge Writer Agent）
GET    /api/research/projects/{id}/preview-write  # 预览待写入的知识点
```

---

## 十、Tool 设计

Agent 通过 Tool 与外部交互。Tool 接口：

```java
public interface Tool {
    String getName();
    String getDescription();  // 用于 LLM tool_choice 描述
    ToolInputSchema getInputSchema();
    ToolResult execute(Map<String, Object> input, AgentContext context);
}
```

### 核心 Tool 列表

| Tool | 功能 | 依赖服务 |
|------|------|----------|
| **KnowledgeSearchTool** | 搜索用户知识库（关键词+语义） | KnowledgeService, ElasticsearchService, VectorSearchService |
| **KnowledgeGraphTool** | 查询图谱关系、发现相邻知识 | KnowledgeGraphService |
| **KnowledgeReadTool** | 读取单个知识点完整内容 | KnowledgeService |
| **WebSearchTool** | 外部网页搜索 | 外部 Search API（可配置） |
| **WebFetchTool** | 抓取网页完整内容 | RestTemplate + Jsoup |
| **ExtractTool** | 从文本中提取结构化知识 | AiService (EXTRACTION scenario) |
| **VerifyTool** | 验证某条陈述是否有来源支撑 | AiService + ResearchSource 表查询 |
| **KnowledgeWriteTool** | 写入新知识点（pending 状态） | KnowledgeService, PendingKnowledgeService |

---

## 十一、前端页面结构

### 新增页面

| 路由 | 页面 | 说明 |
|------|------|------|
| `/research` | Research.vue（已有，重构） | AI 学习研究入口页：快捷操作 + 历史项目列表 |
| `/research/:id` | ResearchProject.vue（新增） | 研究项目详情：实时 Agent 执行过程 + 结果展示 |
| `/research/new` | ResearchCreate.vue（新增） | 创建新研究项目 |

### 新增组件

| 组件 | 说明 |
|------|------|
| `AgentTimeline.vue` | Agent 执行时间线：实时展示每个 Agent 的 THINKING/TOOL_CALL/COMPLETED 状态 |
| `ResearchPlanCard.vue` | 研究计划卡片：展示 Task 列表 + 依赖关系 + 状态 |
| `SourceCard.vue` | 来源卡片：标题/URL/摘要/可信度标签 |

---

## 十二、前后端数据流

### 研究执行流程（SSE 实时推送）

```
前端                          后端                           Agent
  │                             │                              │
  │ POST /projects/{id}/execute │                              │
  │─────────────────────────────→                              │
  │                             │ Orchestrator.start()         │
  │                             │──────────────────────────────→
  │                             │                              │
  │   SSE: event=step_started   │                              │
  │←─────────────────────────────│   PlannerAgent.execute()     │
  │   SSE: event=step_progress  │←─────────────────────────────│
  │←─────────────────────────────│                              │
  │   SSE: event=step_completed │  AgentContext.researchPlan    │
  │←─────────────────────────────│──────────────────────────────│
  │                             │                              │
  │   SSE: event=step_started   │   KnowledgeAgent.execute()   │
  │←─────────────────────────────│──────────────────────────────→
  │   SSE: tool_call=search     │     → KnowledgeSearchTool    │
  │←─────────────────────────────│                              │
  │   SSE: step_completed       │   ← 返回相关知识列表          │
  │←─────────────────────────────│                              │
  │                             │                              │
  │          ... (后续 Agent 步骤类似) ...                      │
  │                             │                              │
  │   SSE: project_completed    │   SynthesizerAgent 完成       │
  │←─────────────────────────────│                              │
  │                             │                              │
  │ GET /projects/{id}/result   │                              │
  │─────────────────────────────→                              │
  │←─────────── result ─────────│                              │
```

### 暂停/恢复流程

```
前端                   后端                         Redis
  │                      │                            │
  │ POST /pause          │                            │
  │──────────────────────→                            │
  │                      │ 保存 AgentContext 快照       │
  │                      │────────────────────────────→
  │                      │ 设置 project.status=PAUSED  │
  │                      │                            │
  │                      │                            │
  │ POST /execute (恢复)  │                            │
  │──────────────────────→                            │
  │                      │ 从 Redis 加载 AgentContext   │
  │                      │←───────────────────────────│
  │                      │ 从中断点继续执行              │
```

---

## 十三、异步任务设计

复用现有 `async_task` 表 + Kafka 机制：

1. 用户点击「开始研究」→ 后端创建 `async_task`（task_type=`RESEARCH_PROJECT`）
2. Orchestrator 在独立线程池执行 Agent 链
3. 每个 Agent Step 更新 `research_step` 表 + 推送 SSE
4. 长任务（如 WebSearchTool 批量搜索）通过 Kafka 分发到 `research-workflow` topic
5. 前端通过 SSE 实时获取进度，无需轮询

---

## 十四、Redis 使用方案

| Key Pattern | 用途 | TTL |
|-------------|------|-----|
| `research:context:{projectId}` | Agent 执行上下文快照（序列化的 AgentContext） | 24h |
| `research:lock:{projectId}` | 项目执行锁（防止重复执行） | 执行期间 |
| `research:rate:{userId}` | 用户研究频率限制 | 1h |
| `research:cache:web:{urlHash}` | 外部网页内容缓存 | 24h |
| `research:stream:{projectId}` | SSE 连接计数 | 连接期间 |

---

## 十五、Kafka 使用方案

新增 topic：`research-workflow`

```
Producer: ResearchOrchestrator
  → 发送 ResearchStepMessage { projectId, taskId, agentName, toolName, input }

Consumer: ResearchStepConsumer
  → 并行执行耗时的 Tool 调用（批量 WebSearch, 大文件抓取）
  → 执行完成后更新 research_step 表
  → 通过 WebSocket 推送完成事件
```

---

## 十六、Elasticsearch 使用方案

复用现有 `ElasticsearchService`：

1. **知识库检索**：Knowledge Agent 使用 ES 多字段搜索检索用户知识库
2. **来源索引**：Research Agent 抓取的外部资料可选地索引到 ES（`research_source_index`）用于后续检索
3. **全文搜索**：用户可在研究结果中全文搜索

---

## 十七、RAG 使用方案

Knowledge Agent 核心就是 RAG：

```
研究问题 → Embedding → Vector Search (knowledge_embedding)
         → 关键词 Search (Elasticsearch)
         → 图谱 Query (knowledge_relation)
    → 综合结果 → 注入 LLM Context → Agent 推理
```

完全复用现有 `RagService` + `VectorSearchService` + `ElasticsearchService`。

---

## 十八、知识图谱使用方案

Gap Agent 依赖知识图谱发现隐性缺口：

1. **相似度分析**：通过 `knowledge_relation` 中的 `related` 关系 + embedding cosine 相似度
2. **知识树遍历**：从已知节点沿 `parent-child` / `prerequisite` 关系向外探索
3. **盲区推断**：如果用户掌握了 A 和 B，且 A→prerequisite→C→prerequisite→B，但 C 缺失 → C 是隐性缺口

复用现有 `KnowledgeGraphService` 的 `auto-generate` 和 `recommend` 方法，新增 gap analysis 逻辑。

---

## 十九、Research Memory 设计

Research Memory 解决 Agent 的"记忆"问题：

```
短期记忆（Redis）：
  - AgentContext：当前研究的完整上下文（研究目标、中间结果、来源列表）
  - AgentChain：已执行的 Agent 序列
  - 上一次 Tool 调用的结果

长期记忆（DB）：
  - research_project：项目元数据
  - research_task：任务分解
  - research_step：每一步的完整记录
  - research_source：所有外部来源
  - research_conclusion：所有结论（含来源引用）
  - research_result：最终输出
```

Agent 不需要自身维护记忆 —— 所有状态存储在 AgentContext（内容结构化的 HashMap），由 Orchestrator 在 Agent 之间传递。

---

## 二十、Agent 可观测性设计

| 维度 | 实现 |
|------|------|
| **步骤追踪** | `research_step` 表记录每个 Agent 的 THINKING/TOOL_CALL/TOOL_RESULT |
| **实时推送** | SSE 推送每个 step_started/step_progress/step_completed 事件 |
| **思考链** | Planner Agent 的推理过程（chain-of-thought）作为 step 内容存储 |
| **Tool 调用日志** | tool_input + tool_output 完整记录 |
| **耗时统计** | 每个 step 的 duration_ms |
| **错误追踪** | 失败 step 记录 error_message，支持重试 |
| **前端展示** | AgentTimeline 组件：时间线 + 状态图标 + 展开查看详情 |

### SSE 事件类型

```json
{"event": "step_started", "data": {"agentName": "PlannerAgent", "title": "正在制定研究计划..."}}
{"event": "step_progress", "data": {"agentName": "PlannerAgent", "content": "分析研究目标..."}}
{"event": "tool_call", "data": {"agentName": "ResearchAgent", "toolName": "WebSearchTool", "input": {"query": "Spring AI Agent"}}}
{"event": "step_completed", "data": {"agentName": "KnowledgeAgent", "result": "找到 12 个相关知识点"}}
{"event": "task_completed", "data": {"taskId": 1, "title": "Spring AI Agent 基础"}}
{"event": "conclusion_found", "data": {"statement": "...", "confidence": "high"}}
{"event": "project_completed", "data": {"projectId": 1}}
{"event": "error", "data": {"agentName": "ResearchAgent", "message": "外部搜索超时"}}
```

---

## 二十一、失败重试机制

```
层级 1: Tool 级重试
  - WebSearchTool 单次失败 → 自动重试 2 次（指数退避 1s, 3s）
  - AiService 调用失败 → 自动重试 1 次

层级 2: Step 级重试
  - 单个 Step 失败 → 标记 FAILED → Orchestrator 决策：跳过/重试/失败

层级 3: Task 级重试
  - Task 中任一步骤失败 → Task 标记 FAILED → 用户可手动 RETRY
  - RETRY 时从该 Task 的第一个 Step 重新开始

层级 4: Project 级重试
  - Project 标记 FAILED → 用户手动 RETRY → 从失败 Task 恢复
```

外部搜索失败**不导致**整个项目失败——Orchestrator 降级为仅使用知识库内容完成研究。

---

## 二十二、长任务恢复机制

1. **检查点**：每个 Task 完成后，将 AgentContext 序列化保存到 Redis（key: `research:context:{projectId}`）
2. **中断检测**：用户关闭页面不影响后端执行。前端重连后通过 `GET /projects/{id}/status` 获取当前状态，重新建立 SSE 连接
3. **恢复执行**：`POST /projects/{id}/execute` 检测 Redis 中是否有 AgentContext，有则从检查点恢复
4. **超时处理**：如果项目在执行中超过 60 分钟无进展，自动标记 FAILED，通知用户

---

## 二十三、权限设计

复用现有 Workspace 权限模型：

| 操作 | 权限要求 |
|------|----------|
| 创建研究项目 | 登录用户（personal space）或 workspace member |
| 查看项目 | 项目所有者 / workspace 成员 |
| 编辑/删除项目 | 项目所有者 / workspace admin |
| 执行研究 | 项目所有者（RLS：每分钟最多 1 次） |
| 知识写入确认 | 项目所有者 |

---

## 二十四、安全设计

| 风险 | 缓解措施 |
|------|----------|
| LLM 注入（通过外部网页内容） | 所有外部内容经过 DOMPurify + prompt sanitization |
| Agent 无限循环 | max_iterations = 3（硬限制），Orchestrator 检测循环 |
| 用户数据泄露到外部搜索 | WebSearchTool 仅发送脱敏后的搜索关键词 |
| 越权访问研究项目 | 所有 API 校验 userId + workspaceId |
| CSRF | Spring Security 已配置（现有基础设施） |
| Rate Limiting | Redis 计数器：每用户每分钟最多 1 次研究执行 |

---

## 关键问题回答

### A. Planner Agent 如何决定是否需要调用其他 Agent？

Planner 接收研究目标文本 + 用户知识概况（知识节点总数、主要标签、图谱连通度），通过 LLM 推理输出结构化 ResearchPlan JSON：

```json
{
  "complexity": "STANDARD",
  "agentChain": ["KnowledgeAgent", "GapAgent", "ResearchAgent", "CriticAgent", "SynthesizerAgent", "KnowledgeWriterAgent"],
  "tasks": [
    {"title": "Spring AI Agent 基础概念", "requiresExternalSearch": false, "dependsOn": null},
    {"title": "Tool Calling 机制", "requiresExternalSearch": true, "dependsOn": 1},
    {"title": "生产级部署考虑", "requiresExternalSearch": true, "dependsOn": [1, 2]}
  ]
}
```

Planner 根据以下因素决定 agentChain：
- 用户知识库中相关知识点数量 < 5 → 必须走 KnowledgeAgent 深度检索
- 用户对目标领域掌握度 < 30% → 必须走 ResearchAgent 外部搜索
- 研究目标包含"对比"/"最新"/"2026"等词 → 必须走 ResearchAgent
- 用户知识库 > 100 个节点 → 启用 GapAgent

### B. Knowledge Agent 如何读取个人知识库？

三步检索（并行执行）：

1. **语义检索**：研究问题 → Embedding → `knowledge_embedding` 表 cosine 相似度 → Top 20
2. **关键词检索**：研究问题 → Elasticsearch 多字段搜索（title, summary, contentMd）→ Top 20
3. **图谱扩展**：取前两步的交集节点 → `knowledge_relation` 查询相邻节点（1-hop）→ 扩展结果

综合排序：语义分数 × 0.5 + 关键词分数 × 0.3 + 图谱距离 × 0.2

返回 `List<KnowledgeReference>`（id, title, summary, snippet, relevanceScore, masteryLevel）。

### C. 如何判断用户已经掌握某个知识？

多维判断（非二元判定）：

| 维度 | 指标 | 权重 |
|------|------|------|
| masteryLevel | knowledge_node.mastery_level (0-5) | 40% |
| reviewLog | 最近 3 次复习结果（easy=高分） | 25% |
| reviewCard | 复习卡片正确率 | 20% |
| chatContext | 用户在对话中讨论该主题的频率 | 10% |
| graphPosition | 该节点在知识图谱中的位置（被多少其他节点引用） | 5% |

综合分数：
- ≥ 80% → "已掌握"
- 50-80% → "部分掌握"
- < 50% → "未掌握"

### D. 如何发现隐性知识盲区？

三步算法：

1. **图谱结构分析**（GapAgent 核心逻辑）：
   - 遍历用户已有知识的标签/类别
   - 对比知识图谱中同类别常见子节点（通过 LLM 已知的知识结构）
   - 例如：用户有 "RAG" 节点，但缺少子节点 "RAG Evaluation"、"Faithfulness 指标"

2. **前置知识推断**（基于 `prerequisite` 关系）：
   - 如果用户标记了目标学习领域 → 按 prerequisite 链反向查找缺失项

3. **社区对比**（Workspace 内）：
   - 对比同 workspace 其他成员的知识结构
   - 发现"其他人都有但你缺少"的知识领域

### E. Research Agent 如何执行外部搜索？

```
ResearchAgent.receive(taskTitle, taskDescription)
    ↓
生成搜索查询（LLM：将研究问题转换为 2-3 个搜索关键词）
    ↓
WebSearchTool.execute(queries)  ← 并行执行
    ├── Search API 1: DuckDuckGo / Brave Search / SerpAPI
    ├── Search API 2: 备用搜索引擎
    └── 超时 10s，使用最先返回的结果
    ↓
取 Top 10 结果 URL
    ↓
WebFetchTool.execute(urls)  ← 并行抓取
    ├── RestTemplate GET
    ├── Jsoup 提取正文
    ├── 超时 15s/页
    └── 失败跳过（不阻塞）
    ↓
ExtractTool.execute(正文)  ← LLM 提取关键信息
    ├── 为每个来源生成摘要
    ├── 标注信息类型（定义/教程/论文/观点/数据）
    └── 关联到研究问题
    ↓
保存到 research_source 表（id, title, url, snippet, fullContent, relevanceScore）
```

### F. 如何保存 Source？

每个外部信息源保存为 `research_source` 记录：

```
{
  id, projectId, taskId,
  title: "Spring AI 官方文档 - Agent 章节",
  url: "https://docs.spring.io/spring-ai/reference/agent.html",
  sourceType: "official_doc",
  snippet: "Spring AI Agent 提供 Tool Calling 能力...",
  fullContent: "...完整抓取内容...",
  relevanceScore: 0.92,
  reliability: "high",
  fetchedAt: "2026-07-28T10:30:00"
}
```

Critic Agent 验证结论时通过 `supporting_sources` / `conflicting_sources` JSON 数组引用 source.id。

### G. Critic Agent 如何验证结论？

每条结论经过 4 项检查：

1. **来源检查**：结论是否有 research_source 支撑？无来源的 statement 标记为 `speculation`
2. **来源可信度**：official_doc > paper > article > github > blog。低可信度来源标记
3. **一致性检查**：多个来源的结论是否一致？如果 3 个来源中 2 个说 A，1 个说 B → 标记冲突
4. **时效性检查**：来源是否过时？（技术类 > 2 年 → 降低可信度）

输出每个结论的 confidence 评级：
- `high`：2+ 个高可信度来源一致支撑
- `medium`：有来源但单一或来源可信度一般
- `low`：仅有间接支撑
- `speculation`：无来源，仅为 LLM 推断

### H. 如何处理冲突信息？

1. **记录冲突**：在 `research_conclusion` 中同时记录 `supporting_sources` 和 `conflicting_sources`
2. **展示冲突**：研究报告中明确标注 "⚠️ 存在冲突观点"，列出双方论据
3. **不下定论**：冲突未解决时，confidence 降级为 `low`，不写入知识库
4. **建议深入**：Synthesizer Agent 将冲突点标记为 "需进一步研究"

### I. 如何保证研究结果可以追溯？

追溯链（完整保留在 DB 中）：

```
ResearchResult（最终输出）
  → research_conclusion（每条结论）
    → supporting_sources [source.id, source.id]
      → research_source（URL + 抓取时间 + 内容快照）
  → research_step（每个 Agent 的完整执行记录）
    → tool_input / tool_output
  → research_task（任务级记录）
  → research_project（项目元数据）
```

研究报告中每个引用使用 `[来源{n}]` 标记，对应 `research_source.id`。

### J. 如何将研究结果沉淀回知识库？

Knowledge Writer Agent 流程：

1. **提取新知识**：从研究结论中提取可作为独立 knowledge_node 的内容
2. **去重**：与用户现有知识库做语义去重（embedding cosine > 0.85 → 合并建议）
3. **生成建议**：为每个新知识生成 title + contentMd + tags + importance
4. **图谱关联**：生成 knowledge_relation（derived_from / supports / relates_to）
5. **用户确认**（关键！）：通过 `POST /projects/{id}/preview-write` 预览 → 用户勾选确认 → `POST /projects/{id}/write-back`

默认规则：
- confidence = high + 2+ 来源 → 默认推荐写入
- confidence = medium → 标记 "待确认"
- confidence = low/speculation → 不推荐写入

### K. 如何防止 Agent 无限循环？

三层防护：

1. **硬限制**：`max_iterations = 3`（可配置），达到上限强制执行 SynthesizerAgent
2. **循环检测**：Orchestrator 检测 Agent 序列中是否出现重复模式（如 A→B→A→B），强制终止循环
3. **Token 限制**：单次研究总 LLM token 消耗不超过 100K tokens（约 $0.50），超出后降级输出

### L. 如何支持任务暂停、恢复和重试？

**暂停**：
- 用户点暂停 → Orchestrator 等待当前 Step 完成 → 序列化 AgentContext 到 Redis → 项目状态设为 PAUSED
- 已在执行中的 Step 不可中断（但 Tool 调用可在下次轮询时取消）

**恢复**：
- 用户点继续 → 从 Redis 加载 AgentContext → 从下一个 PENDING 的 Task/Step 继续

**重试**：
- Task 级重试：从该 Task 的第一个 Step 重新执行
- Project 级重试：从第一个 FAILED 的 Task 开始
- 重试保留之前的 research_source（外部搜索结果缓存），避免重复抓取

### M. 如何支持 Agent 执行过程实时展示？

**SSE 流式推送**（`GET /projects/{id}/stream`）：

前端 `AgentTimeline.vue` 组件：
```
┌─────────────────────────────────────────────┐
│  研究进度                                    │
│                                             │
│  ✅ Planner 制定研究计划 (2.3s)              │
│  ✅ Knowledge Agent 检索知识库 (1.8s)        │
│    找到 12 个相关知识点                      │
│  ✅ Gap Agent 分析知识盲区 (3.1s)             │
│    发现 3 个知识缺口                         │
│  🔄 Research Agent 搜索外部资料...           │
│    ├─ 搜索 "Spring AI Tool Calling" (完成)  │
│    ├─ 抓取 spring.io (完成)                 │
│    └─ 搜索 "Agent Memory Pattern" (进行中)  │
│  ⏳ Critic Agent (等待中)                    │
│  ⏳ Synthesizer Agent (等待中)               │
│                                             │
│  [暂停] [取消]                               │
└─────────────────────────────────────────────┘
```

每个 Step 实时展开：
- `THINKING`：显示 LLM 推理过程（打字机效果）
- `TOOL_CALL`：显示 Tool 名称 + 输入参数 + 加载动画
- `TOOL_RESULT`：显示结果摘要（可展开详情）
- `COMPLETED`：绿色勾 + 耗时

---

## 实施建议

### Phase 1: 基础架构（数据库 + 接口）
1. 执行 migration 创建 6 张新表
2. 实现 ResearchProjectService CRUD
3. 实现 ResearchController 基础端点

### Phase 2: Agent 框架 + 简单 Agent
4. 实现 Agent 接口 + AgentContext
5. 实现 ResearchOrchestrator（状态机 + SSE 推送）
6. 实现 PlannerAgent + KnowledgeAgent（最简单的两个 Agent）

### Phase 3: 完整 Agent 链
7. 实现 GapAgent + ResearchAgent（含 WebSearchTool）
8. 实现 CriticAgent + SynthesizerAgent
9. 实现 KnowledgeWriterAgent

### Phase 4: 前端
10. 创建 ResearchProject.vue（Agent 执行时间线）
11. 创建 ResearchCreate.vue（新建研究项目）
12. 重构 Research.vue（集成项目列表）

### Phase 5: 可靠性
13. 实现 Redis 检查点 + 暂停/恢复
14. 实现多层重试机制
15. 实现循环检测 + 超时处理
