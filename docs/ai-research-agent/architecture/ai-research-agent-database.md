# AI Research Agent — 数据库设计文档

> 基于架构设计文档：`ai-research-agent-architecture.md`
> 基于现有数据库：39 张表（完整分析）
> 日期：2026-07-28
> 版本：V1

---

## 一、设计原则

### 1.1 命名规范

遵循现有项目约定：
- 表名：`snake_case`，前缀 `research_`
- 主键：`id BIGINT AUTO_INCREMENT`
- 时间戳：`create_time` / `update_time`（与现有表一致）
- 软删除：`deleted TINYINT NOT NULL DEFAULT 0`
- 工作空间隔离：`workspace_id BIGINT`
- 用户隔离：`user_id BIGINT NOT NULL`

### 1.2 与现有表的差异说明

现有表中有两种时间戳风格：
- `create_time` + `update_time`（knowledge_node、user 等核心表）
- `created_at` + `updated_at`（V3-V9 migration 引入的表）

新增 research 模块统一使用 `create_time` + `update_time` 风格（与核心表保持一致）。

### 1.3 存储分层策略

| 存储层 | 数据内容 | 理由 |
|--------|----------|------|
| **MySQL** | 所有结构化数据：project、task、step、source、finding、conclusion、report、candidate、execution、message、memory | 需要事务、JOIN、外键约束 |
| **Elasticsearch** | 来源全文内容（`research_source.full_content`）、研究报告全文（`research_report.content_md`） | 支持在研究结果中全文搜索 |
| **Redis** | AgentContext 快照、执行状态缓存、搜索缓存、限流计数器、SSE 连接状态 | 低延迟读写、自动过期 |
| **对象存储** | 暂不涉及 | 未来可存储大型抓取文件或导出的 PDF 报告 |

---

## 二、ER 关系图

```
research_project (1) ───── (N) research_task (1) ───── (N) research_step
     │                            │
     │                            ├── (N) research_finding
     │                            ├── (N) research_source
     │                            └── (N) research_conclusion
     │
     ├── (1) research_plan              (Planner 输出，JSON)
     ├── (1) research_report            (最终输出)
     ├── (N) research_source            (项目级来源，不关联具体 task)
     ├── (N) research_finding           (项目级发现)
     ├── (N) research_conclusion        (项目级结论)
     ├── (N) research_knowledge_candidate (待写入知识库)
     ├── (N) agent_execution            (每次执行记录)
     └── (N) research_memory            (Agent 长期记忆)

agent_execution (1) ───── (N) agent_message  (LLM 对话记录)

research_knowledge_candidate ──→ knowledge_node (写入后关联)
research_source ──→ knowledge_node.source_id (来源追溯)
```

### 关系说明

```
research_project
  ├── 1:1 → research_plan          (每个项目一个计划)
  ├── 1:1 → research_report         (每个项目一份报告)
  ├── 1:N → research_task           (CASCADE: 删项目 → 删所有 task)
  ├── 1:N → research_source         (CASCADE)
  ├── 1:N → research_finding        (CASCADE)
  ├── 1:N → research_conclusion     (CASCADE)
  ├── 1:N → research_knowledge_candidate (CASCADE)
  ├── 1:N → agent_execution         (CASCADE)
  └── 1:N → research_memory         (CASCADE)

research_task
  ├── 1:N → research_step           (CASCADE)
  ├── 1:N → research_finding        (SET NULL: 删 task 保留 finding)
  ├── 1:N → research_source         (SET NULL)
  └── 1:N → research_conclusion     (SET NULL)

agent_execution
  └── 1:N → agent_message           (CASCADE)
```

---

## 三、表设计（12 张新表）

### 3.1 research_project（研究项目）

核心表，管理研究项目的完整生命周期。

```sql
CREATE TABLE research_project (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL                COMMENT '用户 ID',
    workspace_id    BIGINT                         COMMENT '工作空间 ID（NULL=个人空间）',
    title           VARCHAR(300) NOT NULL           COMMENT '研究标题',
    goal            TEXT NOT NULL                   COMMENT '研究目标描述',
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                        COMMENT 'DRAFT/PLANNING/RESEARCHING/REVIEWING/SYNTHESIZING/COMPLETED/ARCHIVED/FAILED/PAUSED',
    complexity      VARCHAR(20)                     COMMENT '复杂度：SIMPLE/STANDARD/DEEP',
    agent_workflow  VARCHAR(200)                    COMMENT '实际执行的 Agent 链路（逗号分隔）',
    max_iterations  INT NOT NULL DEFAULT 3          COMMENT '最大 Agent 迭代轮数',
    current_iteration INT NOT NULL DEFAULT 0        COMMENT '当前迭代轮数',
    plan_json       JSON                            COMMENT 'Research Plan 结构化数据（Planner 输出）',
    result_summary  TEXT                            COMMENT '研究结论摘要',
    result_report   LONGTEXT                        COMMENT '完整研究报告（Markdown）',
    context_snapshot MEDIUMTEXT                     COMMENT 'AgentContext 序列化快照（用于暂停恢复）',
    idempotency_key VARCHAR(64)                     COMMENT '幂等键（执行操作去重）',
    version         INT NOT NULL DEFAULT 1          COMMENT '乐观锁版本号',
    started_at      DATETIME                        COMMENT '首次执行时间',
    paused_at       DATETIME                        COMMENT '最近暂停时间',
    completed_at    DATETIME                        COMMENT '完成时间',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0      COMMENT '逻辑删除',

    INDEX idx_user_id (user_id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_user_status (user_id, status),
    UNIQUE KEY uk_idempotency (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究项目表';
```

**状态流转**：
```
DRAFT → PLANNING → RESEARCHING → REVIEWING → SYNTHESIZING → COMPLETED → ARCHIVED
  │        │            │             │             │             │
  └────────┴────────────┴─────────────┴─────────────┴─────────────┴──→ FAILED
                                                                         │
                                                                    PAUSED (可恢复)
```

### 3.2 research_plan（研究计划）

Planner Agent 输出的结构化研究计划。独立表而非 JSON 字段，便于：
- 版本管理（计划可能更新）
- 单独查询计划状态
- 前端直接展示计划卡片

```sql
CREATE TABLE research_plan (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT NOT NULL                 COMMENT '所属项目 ID',
    version         INT NOT NULL DEFAULT 1          COMMENT '计划版本号',
    complexity      VARCHAR(20) NOT NULL             COMMENT '复杂度评估',
    agent_chain     VARCHAR(200) NOT NULL            COMMENT 'Agent 执行链路（逗号分隔的 agent name）',
    tasks_json      JSON NOT NULL                    COMMENT 'Task 列表 [{title, description, requiresExternalSearch, dependsOn}]',
    rationale       TEXT                             COMMENT 'Planner 推理过程（可观测性）',
    estimated_tokens INT                             COMMENT '预估 token 消耗',
    created_by      VARCHAR(50) NOT NULL             COMMENT '创建者：PLANNER_AGENT / USER',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_project_id (project_id),
    INDEX idx_project_version (project_id, version),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究计划表（Planner 输出）';
```

### 3.3 research_task（研究任务）

项目的可执行子任务，由 Planner 分解产生。

```sql
CREATE TABLE research_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT NOT NULL                 COMMENT '所属项目 ID',
    plan_id         BIGINT                          COMMENT '所属计划 ID',
    title           VARCHAR(300) NOT NULL           COMMENT '任务标题',
    description     TEXT                             COMMENT '任务描述',
    question        TEXT                             COMMENT '该任务要回答的研究问题',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/SKIPPED/WAITING_USER',
    depends_on      BIGINT                          COMMENT '依赖的前置 task_id',
    async_task_id   BIGINT                          COMMENT '关联 async_task 表 ID（复用现有异步任务）',
    requires_external_search TINYINT DEFAULT 0      COMMENT '是否需要外部搜索',
    result_summary  TEXT                             COMMENT '任务结果摘要',
    sort_order      INT DEFAULT 0                   COMMENT '排序序号',
    started_at      DATETIME,
    completed_at    DATETIME,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_project_id (project_id),
    INDEX idx_plan_id (plan_id),
    INDEX idx_status (status),
    INDEX idx_sort_order (sort_order),
    INDEX idx_project_status (project_id, status),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES research_plan(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究任务表';
```

### 3.4 research_step（Agent 执行步骤）

每个 Agent 的执行过程以 step 为单位记录。append-only（不可变日志）。

```sql
CREATE TABLE research_step (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT NOT NULL                 COMMENT '所属任务 ID',
    execution_id    BIGINT                          COMMENT '所属执行会话 ID',
    agent_name      VARCHAR(50) NOT NULL             COMMENT 'Agent 标识：PlannerAgent/KnowledgeAgent/...',
    step_type       VARCHAR(30) NOT NULL             COMMENT '步骤类型：THINKING/TOOL_CALL/TOOL_RESULT/LLM_CALL/PROCESSING',
    title           VARCHAR(200)                     COMMENT '步骤描述（前端展示）',
    content         MEDIUMTEXT                       COMMENT '步骤内容（思考过程/工具输入/LLM 响应）',
    tool_name       VARCHAR(50)                      COMMENT 'Tool 名称（如 KnowledgeSearchTool）',
    tool_input      JSON                             COMMENT 'Tool 入参',
    tool_output     MEDIUMTEXT                       COMMENT 'Tool 结果',
    token_usage     JSON                             COMMENT 'Token 消耗 {"prompt": N, "completion": N}',
    status          VARCHAR(20) NOT NULL DEFAULT 'RUNNING'
                        COMMENT 'RUNNING/COMPLETED/FAILED/SKIPPED',
    error_message   TEXT                             COMMENT '错误信息',
    sort_order      INT DEFAULT 0                   COMMENT '步骤序号',
    duration_ms     INT                              COMMENT '耗时（毫秒）',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_task_id (task_id),
    INDEX idx_execution_id (execution_id),
    INDEX idx_agent_name (agent_name),
    INDEX idx_status (status),
    INDEX idx_task_agent (task_id, agent_name, sort_order),
    FOREIGN KEY (task_id) REFERENCES research_task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究步骤表（Agent 执行日志，append-only）';
```

### 3.5 research_source（研究来源）

外部信息源。按可靠性分级，支持全文检索。

```sql
CREATE TABLE research_source (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT NOT NULL                 COMMENT '所属项目 ID',
    task_id         BIGINT                          COMMENT '关联任务 ID（可选）',
    title           VARCHAR(500) NOT NULL           COMMENT '来源标题',
    url             VARCHAR(2000)                    COMMENT '来源 URL',
    source_type     VARCHAR(30) NOT NULL             COMMENT '来源类型：web_search/official_doc/paper/github/article/internal',
    snippet         TEXT                             COMMENT '内容摘要',
    full_content    LONGTEXT                         COMMENT '完整内容（抓取后存储）',
    relevance_score DECIMAL(5,4)                     COMMENT '与研究的关联度 0-1',
    reliability     VARCHAR(20) DEFAULT 'unverified' COMMENT '可靠性：high/medium/low/unverified',
    content_hash    VARCHAR(64)                      COMMENT '内容 SHA256 哈希（去重）',
    fetch_status    VARCHAR(20) DEFAULT 'success'    COMMENT '抓取状态：success/failed/timeout/skipped',
    fetched_at      DATETIME,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_project_id (project_id),
    INDEX idx_task_id (task_id),
    INDEX idx_source_type (source_type),
    INDEX idx_reliability (reliability),
    INDEX idx_content_hash (content_hash),
    FULLTEXT INDEX ft_source_content (title, snippet),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES research_task(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究来源表';
```

### 3.6 research_finding（研究中间发现）

研究过程中产生的中间观察和发现。比 conclusion 更轻量、更早期。

```sql
CREATE TABLE research_finding (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT NOT NULL                 COMMENT '所属项目 ID',
    task_id         BIGINT                          COMMENT '关联任务 ID（可选）',
    agent_name      VARCHAR(50) NOT NULL             COMMENT '产生该发现的 Agent',
    statement       TEXT NOT NULL                    COMMENT '发现内容',
    category        VARCHAR(30)                      COMMENT '分类：fact/insight/question/contradiction/gap',
    source_ids      JSON                             COMMENT '支撑来源 ID 列表 [1, 2, 3]',
    confidence      VARCHAR(20) DEFAULT 'medium'     COMMENT '可信度：high/medium/low/speculation',
    is_promoted     TINYINT DEFAULT 0               COMMENT '是否已升级为 conclusion',
    promoted_to_id  BIGINT                           COMMENT '升级后的 conclusion ID',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_project_id (project_id),
    INDEX idx_task_id (task_id),
    INDEX idx_agent_name (agent_name),
    INDEX idx_category (category),
    INDEX idx_is_promoted (is_promoted),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES research_task(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究中间发现表';
```

### 3.7 research_conclusion（研究结论）

经过 Critic Agent 验证后的正式结论。有来源支撑和冲突标注。

```sql
CREATE TABLE research_conclusion (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT NOT NULL             COMMENT '所属项目 ID',
    task_id             BIGINT                      COMMENT '关联任务 ID',
    statement           TEXT NOT NULL                COMMENT '结论陈述',
    confidence          VARCHAR(20) NOT NULL DEFAULT 'medium'
                            COMMENT 'high/medium/low/speculation',
    supporting_sources   JSON                        COMMENT '支持来源 ID 列表 [{sourceId, quote, relevance}]',
    conflicting_sources  JSON                        COMMENT '冲突来源 ID 列表 [{sourceId, quote}]',
    is_key_finding      TINYINT DEFAULT 0            COMMENT '是否为关键发现',
    is_controversial    TINYINT DEFAULT 0            COMMENT '是否存在冲突信息',
    critic_notes        TEXT                         COMMENT 'Critic Agent 验证备注',
    version             INT NOT NULL DEFAULT 1      COMMENT '结论版本（更新后递增）',
    previous_version_id BIGINT                       COMMENT '上一版本 conclusion ID',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_project_id (project_id),
    INDEX idx_task_id (task_id),
    INDEX idx_confidence (confidence),
    INDEX idx_is_key_finding (is_key_finding),
    INDEX idx_project_confidence (project_id, confidence),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES research_task(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究结论表';
```

### 3.8 research_report（研究报告）

研究的最终输出。一个项目可有多版报告（通过 version 区分）。

```sql
CREATE TABLE research_report (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT NOT NULL             COMMENT '所属项目 ID',
    version             INT NOT NULL DEFAULT 1      COMMENT '报告版本号',
    title               VARCHAR(300) NOT NULL        COMMENT '报告标题',
    summary             TEXT                         COMMENT '报告摘要',
    content_md          LONGTEXT                     COMMENT '报告正文（Markdown）',
    research_questions  JSON                         COMMENT '研究问题列表',
    key_findings        JSON                         COMMENT '关键发现列表 [{statement, confidence, sourceIds}]',
    knowledge_gaps      JSON                         COMMENT '发现的知识缺口 [{topic, description, priority}]',
    new_knowledge_ids   JSON                         COMMENT '新写入的 knowledge_node ID 列表',
    new_relation_ids    JSON                         COMMENT '新写入的 knowledge_relation ID 列表',
    source_count        INT DEFAULT 0               COMMENT '引用来源总数',
    conclusion_count    INT DEFAULT 0                COMMENT '结论总数',
    token_usage_total   JSON                         COMMENT '总 token 消耗 {"prompt": N, "completion": N}',
    duration_total_ms   BIGINT                       COMMENT '总耗时（毫秒）',
    generated_by        VARCHAR(50) DEFAULT 'SYNTHESIZER_AGENT' COMMENT '生成方',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_project_id (project_id),
    INDEX idx_project_version (project_id, version),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究报告表（最终输出）';
```

### 3.9 research_knowledge_candidate（知识候选）

Knowledge Writer Agent 生成的待写入知识。用户确认后写入 `knowledge_node` 表。

```sql
CREATE TABLE research_knowledge_candidate (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT NOT NULL             COMMENT '所属项目 ID',
    source_ids          JSON                         COMMENT '支撑该知识的来源 ID 列表',
    conclusion_ids      JSON                         COMMENT '支撑该知识的结论 ID 列表',
    title               VARCHAR(200) NOT NULL        COMMENT '建议标题',
    content_md          TEXT                         COMMENT '建议正文（Markdown）',
    summary             TEXT                         COMMENT '摘要',
    tags                JSON                         COMMENT '建议标签 ["tag1", "tag2"]',
    importance          TINYINT DEFAULT 3            COMMENT '建议重要程度 1-5',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            COMMENT 'PENDING/ACCEPTED/REJECTED/MODIFIED/WRITTEN',
    written_node_id     BIGINT                       COMMENT '写入后的 knowledge_node.id',
    user_feedback       TEXT                         COMMENT '用户修改意见（status=MODIFIED 时）',
    reviewed_at         DATETIME                     COMMENT '用户审核时间',
    written_at          DATETIME                     COMMENT '写入知识库时间',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    INDEX idx_written_node_id (written_node_id),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究知识候选表（待用户确认）';
```

### 3.10 agent_execution（Agent 执行会话）

每次用户触发"开始研究"，创建一个执行会话。用于追踪和恢复。

```sql
CREATE TABLE agent_execution (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT NOT NULL             COMMENT '所属项目 ID',
    execution_key       VARCHAR(64) NOT NULL         COMMENT '执行唯一键（幂等）',
    status              VARCHAR(20) NOT NULL DEFAULT 'RUNNING'
                            COMMENT 'RUNNING/COMPLETED/FAILED/PAUSED/CANCELLED',
    agent_chain_executed VARCHAR(200)                 COMMENT '实际执行的 Agent 序列',
    current_agent       VARCHAR(50)                   COMMENT '当前/最后执行的 Agent',
    current_task_id     BIGINT                        COMMENT '当前/最后执行的任务 ID',
    context_snapshot    MEDIUMTEXT                    COMMENT '执行上下文快照（用于恢复）',
    checkpoint_at       DATETIME                      COMMENT '最近检查点时间',
    total_duration_ms   BIGINT                        COMMENT '总执行时间（毫秒）',
    token_usage_total   JSON                          COMMENT '总 token 消耗',
    error_message       TEXT                          COMMENT '失败原因',
    retry_count         INT DEFAULT 0                COMMENT '重试次数',
    started_at          DATETIME,
    completed_at        DATETIME,
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_project_id (project_id),
    INDEX idx_execution_key (execution_key),
    INDEX idx_status (status),
    INDEX idx_project_status (project_id, status),
    UNIQUE KEY uk_execution_key (execution_key),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 执行会话表';
```

### 3.11 agent_message（Agent LLM 对话记录）

每次 Agent 调用 LLM 的完整 prompt/response 记录。用于审计、调试和可观测性。

```sql
CREATE TABLE agent_message (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id        BIGINT NOT NULL             COMMENT '所属执行会话 ID',
    step_id             BIGINT                       COMMENT '关联 research_step ID',
    agent_name          VARCHAR(50) NOT NULL         COMMENT '调用 LLM 的 Agent',
    message_role        VARCHAR(20) NOT NULL         COMMENT 'system/user/assistant/tool',
    model               VARCHAR(100)                  COMMENT '使用的模型名称',
    provider_code       VARCHAR(30)                   COMMENT 'AI Provider code',
    prompt_tokens       INT DEFAULT 0                COMMENT 'prompt token 数',
    completion_tokens   INT DEFAULT 0                COMMENT 'completion token 数',
    content             MEDIUMTEXT NOT NULL           COMMENT '消息内容',
    metadata_json       JSON                          COMMENT '额外元数据（temperature, top_p 等）',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_execution_id (execution_id),
    INDEX idx_step_id (step_id),
    INDEX idx_agent_name (agent_name),
    INDEX idx_execution_agent (execution_id, agent_name, create_time),
    FOREIGN KEY (execution_id) REFERENCES agent_execution(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent LLM 对话记录表';
```

### 3.12 research_memory（Agent 长期记忆）

Agent 在研究过程中积累的持久记忆项。跨执行会话共享。

```sql
CREATE TABLE research_memory (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT NOT NULL             COMMENT '所属项目 ID',
    user_id             BIGINT NOT NULL              COMMENT '用户 ID',
    memory_key          VARCHAR(100) NOT NULL        COMMENT '记忆键（唯一标识，如 "knowledge_state.spring_boot"）',
    memory_type         VARCHAR(30) NOT NULL         COMMENT '类型：knowledges_state/gap_found/search_result/user_preference/decision',
    content             JSON NOT NULL                 COMMENT '记忆内容（结构化 JSON）',
    last_accessed_at    DATETIME                     COMMENT '上次访问时间',
    expires_at          DATETIME                     COMMENT '过期时间（NULL = 永不过期）',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_project_id (project_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_project_memory (project_id, memory_key),
    INDEX idx_expires_at (expires_at),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 长期记忆表';
```

---

## 四、Redis Key 设计

| Key Pattern | 类型 | 内容 | TTL |
|-------------|------|------|-----|
| `research:context:{projectId}` | String (JSON) | AgentContext 序列化快照 | 24h |
| `research:exec:{executionId}` | Hash | 执行会话实时状态 | 执行期间 + 1h |
| `research:lock:{projectId}` | String | 项目执行分布式锁（value=executionId） | 60min（续期） |
| `research:rate:{userId}` | String | 执行频率计数器 | 1h |
| `research:stream:{projectId}` | Set | SSE 连接的 sessionId 集合 | 连接期间 |
| `research:cache:web:{urlHash}` | String (JSON) | 网页抓取缓存 | 24h |
| `research:cache:search:{queryHash}` | String (JSON) | 搜索结果缓存 | 1h |
| `research:sse:{projectId}:{sessionId}` | String | SSE 断线重连 offset | 5min |

---

## 五、Elasticsearch 索引设计

### 5.1 research_source 索引

```
Index: research_source_{env}

Mapping:
  project_id:    long
  title:         text (analyzer: ik_max_word)
  snippet:       text (analyzer: ik_max_word)
  full_content:  text (analyzer: ik_max_word)
  source_type:   keyword
  url:           keyword
  reliability:   keyword
  relevance_score: float
  create_time:   date
```

### 5.2 research_report 索引

```
Index: research_report_{env}

Mapping:
  project_id:    long
  title:         text (analyzer: ik_max_word)
  summary:       text (analyzer: ik_max_word)
  content_md:    text (analyzer: ik_max_word)
  key_findings:  text (analyzer: ik_max_word)
  knowledge_gaps: text (analyzer: ik_max_word)
  create_time:   date
```

---

## 六、关键设计决策

### 6.1 多用户隔离

- 所有表包含 `user_id`（research_project 直接存，其他表通过 project_id 间接关联）
- Service 层所有查询增加 `userId` 过滤条件
- API 层从 JWT 中提取 userId

### 6.2 Workspace 隔离

- `research_project.workspace_id` 可为 NULL（个人空间）或有值（workspace 共享项目）
- workspace 成员均可查看 workspace 内的研究项目
- 个人空间项目仅本人可见
- 其他表通过 project_id CASCADE 关联，无需冗余 workspace_id

### 6.3 项目生命周期管理

状态流转由 `ResearchOrchestrator` 驱动：
- 非法状态转换在 Service 层校验
- `context_snapshot` 字段存储序列化的 AgentContext（JSON），用于暂停恢复
- 从 PAUSED 恢复时从 `context_snapshot` 反序列化，定位到 `current_task_id` + `agent_name`

### 6.4 任务生命周期管理

- `research_task.status` 与 `async_task.status` 通过 `async_task_id` 关联
- `depends_on` 字段表达 task 间依赖（DAG），Orchestrator 在依赖满足后才启动 task
- `WAITING_USER` 状态用于需要用户决策的 task（如确认是否继续搜索）

### 6.5 Agent 执行记录

三层记录粒度：
1. **agent_execution**：执行会话级（一次点击"开始研究"）
2. **research_step**：Agent 步骤级（每个 Agent 的每步操作，append-only）
3. **agent_message**：LLM 调用级（每次 LLM 请求/响应，用于审计）

### 6.6 来源可追溯

完整的追溯链：
```
research_report.key_findings[i].sourceIds
    → research_conclusion.supporting_sources[].sourceId
        → research_source.id → url + fetched_at + full_content
```

所有结论必须能追溯到 `research_source`。Speculation 类结论标记 `confidence='speculation'` 且 `supporting_sources=null`。

### 6.7 任务恢复机制

**暂停流程**：
1. Orchestrator 等待当前 Agent Step 完成
2. 序列化 AgentContext → `research_project.context_snapshot`
3. 记录当前 agent_execution 状态 → `agent_execution.context_snapshot`
4. 保存到 Redis → `research:context:{projectId}`
5. 设置 project.status = PAUSED

**恢复流程**：
1. 从 Redis 加载 AgentContext（fallback：从 DB context_snapshot 加载）
2. 定位 `current_task_id` + `current_agent` → 从下一个 Step 继续
3. 创建新的 agent_execution 记录（retry_count = previous + 1）

### 6.8 幂等性保障

- `research_project.idempotency_key`：前端生成 UUID，唯一约束确保同一请求不被重复处理
- `agent_execution.execution_key`：每次 POST execute 生成新 key，唯一约束
- Tool 调用幂等：`research_source.content_hash` 去重相同的网页内容

### 6.9 删除策略

遵循现有项目软删除约定：
- 所有表 `deleted` 字段：0 = 正常，1 = 已删除
- 删除 project → 级联软删除所有子表记录
- 实际 DELETE 操作：MyBatis-Plus 逻辑删除自动处理
- 已写入知识库的 candidate（status=WRITTEN）删除 project 时不回滚

### 6.10 数据版本控制

- **research_plan.version**：每次 Planner 重新规划递增
- **research_conclusion.version**：结论更新后递增，`previous_version_id` 链式追溯
- **research_report.version**：每次 Synthesizer 重新生成递增
- **research_step**：append-only，不可变（无 version 字段）
- **agent_message**：append-only，不可变

---

## 七、与现有表的关联

### 7.1 扩展 knowledge_node（需要 ALTER）

研究来源追溯需要 `knowledge_node` 新增两个可选字段：

```sql
ALTER TABLE knowledge_node
    ADD COLUMN source_type VARCHAR(20) DEFAULT NULL
        COMMENT '来源类型：research/manual/rag_extraction/document_capture',
    ADD COLUMN source_id BIGINT DEFAULT NULL
        COMMENT '来源 ID（关联 research_knowledge_candidate.id 或 research_source.id）';
```

### 7.2 扩展 knowledge_relation（无 ALTER 需要）

现有 `relation_type` 字段（VARCHAR(50)）已支持新类型值，无需改表结构。
新增合法值：`derived_from`（研究推导）、`supports`（证据支持）、`contradicts`（证据矛盾）。

### 7.3 扩展 async_task（无 ALTER 需要）

现有 `task_type` 字段（VARCHAR(50)）已支持新类型值，无需改表结构。
新增合法值：`RESEARCH_PROJECT`、`RESEARCH_TASK`。

---

## 八、迁移计划

### SQL 文件清单

| 序号 | 文件 | 内容 |
|------|------|------|
| 1 | `sql/migration/V10__add_research_tables.sql` | 12 张新表 + knowledge_node ALTER |

### 执行顺序

```
1. knowledge_node ALTER (2 columns)
2. research_project
3. research_plan (FK → project)
4. research_task (FK → project, plan)
5. agent_execution (FK → project)
6. research_step (FK → task, execution)
7. research_source (FK → project, task)
8. research_finding (FK → project, task)
9. research_conclusion (FK → project, task)
10. research_report (FK → project)
11. research_knowledge_candidate (FK → project)
12. agent_message (FK → execution, step)
13. research_memory (FK → project)
```

### 回滚

```sql
DROP TABLE IF EXISTS research_memory;
DROP TABLE IF EXISTS agent_message;
DROP TABLE IF EXISTS research_knowledge_candidate;
DROP TABLE IF EXISTS research_report;
DROP TABLE IF EXISTS research_conclusion;
DROP TABLE IF EXISTS research_finding;
DROP TABLE IF EXISTS research_source;
DROP TABLE IF EXISTS research_step;
DROP TABLE IF EXISTS agent_execution;
DROP TABLE IF EXISTS research_task;
DROP TABLE IF EXISTS research_plan;
DROP TABLE IF EXISTS research_project;
-- 可选：回滚 knowledge_node ALTER
ALTER TABLE knowledge_node DROP COLUMN source_type;
ALTER TABLE knowledge_node DROP COLUMN source_id;
```
