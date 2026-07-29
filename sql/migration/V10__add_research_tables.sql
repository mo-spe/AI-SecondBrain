-- ============================================================
-- AI Research Agent — 数据库迁移
-- 文件：V10__add_research_tables.sql
-- 描述：AI Research Agent 12 张核心表 + knowledge_node 扩展
-- 基于：现有 39 张表分析
-- 日期：2026-07-28
-- ============================================================
-- 执行前提：
-- 1. MySQL 8.0+
-- 2. 数据库 second_brain 已存在
-- 3. 已执行 V1-V9 migration
-- ============================================================

USE second_brain;

-- ============================================================
-- 0. 扩展现有 knowledge_node 表
-- ============================================================
ALTER TABLE knowledge_node
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) DEFAULT NULL
        COMMENT '来源类型：research/manual/rag_extraction/document_capture',
    ADD COLUMN IF NOT EXISTS source_id BIGINT DEFAULT NULL
        COMMENT '来源 ID（关联 research_knowledge_candidate.id 或 research_source.id）';

-- ============================================================
-- 1. research_project（研究项目）
-- ============================================================
DROP TABLE IF EXISTS research_project;
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
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT NOT NULL DEFAULT 0      COMMENT '逻辑删除 0=正常 1=删除',

    INDEX idx_rp_user_id (user_id),
    INDEX idx_rp_workspace_id (workspace_id),
    INDEX idx_rp_status (status),
    INDEX idx_rp_create_time (create_time),
    INDEX idx_rp_user_status (user_id, status),
    UNIQUE KEY uk_rp_idempotency (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究项目表';

-- ============================================================
-- 2. research_plan（研究计划）
-- ============================================================
DROP TABLE IF EXISTS research_plan;
CREATE TABLE research_plan (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT NOT NULL                 COMMENT '所属项目 ID',
    version         INT NOT NULL DEFAULT 1          COMMENT '计划版本号',
    complexity      VARCHAR(20) NOT NULL             COMMENT '复杂度评估',
    agent_chain     VARCHAR(200) NOT NULL            COMMENT 'Agent 执行链路（逗号分隔）',
    tasks_json      JSON NOT NULL                    COMMENT 'Task 列表 [{title,description,requiresExternalSearch,dependsOn}]',
    rationale       TEXT                             COMMENT 'Planner 推理过程',
    estimated_tokens INT                             COMMENT '预估 token 消耗',
    created_by      VARCHAR(50) NOT NULL             COMMENT '创建者：PLANNER_AGENT/USER',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_rplan_project_id (project_id),
    INDEX idx_rplan_project_version (project_id, version),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究计划表（Planner 输出）';

-- ============================================================
-- 3. research_task（研究任务）
-- ============================================================
DROP TABLE IF EXISTS research_task;
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
    async_task_id   BIGINT                          COMMENT '关联 async_task 表 ID',
    requires_external_search TINYINT DEFAULT 0      COMMENT '是否需要外部搜索',
    result_summary  TEXT                             COMMENT '任务结果摘要',
    sort_order      INT DEFAULT 0                   COMMENT '排序序号',
    started_at      DATETIME                        COMMENT '开始时间',
    completed_at    DATETIME                        COMMENT '完成时间',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_rtask_project_id (project_id),
    INDEX idx_rtask_plan_id (plan_id),
    INDEX idx_rtask_status (status),
    INDEX idx_rtask_sort_order (sort_order),
    INDEX idx_rtask_project_status (project_id, status),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES research_plan(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究任务表';

-- ============================================================
-- 4. agent_execution（Agent 执行会话）
-- ============================================================
DROP TABLE IF EXISTS agent_execution;
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
    started_at          DATETIME                      COMMENT '开始时间',
    completed_at        DATETIME                      COMMENT '完成时间',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_aexec_project_id (project_id),
    INDEX idx_aexec_execution_key (execution_key),
    INDEX idx_aexec_status (status),
    INDEX idx_aexec_project_status (project_id, status),
    UNIQUE KEY uk_aexec_execution_key (execution_key),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 执行会话表';

-- ============================================================
-- 5. research_step（Agent 执行步骤 — append-only 日志）
-- ============================================================
DROP TABLE IF EXISTS research_step;
CREATE TABLE research_step (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT NOT NULL                 COMMENT '所属任务 ID',
    execution_id    BIGINT                          COMMENT '所属执行会话 ID',
    agent_name      VARCHAR(50) NOT NULL             COMMENT 'Agent 标识',
    step_type       VARCHAR(30) NOT NULL             COMMENT '步骤类型：THINKING/TOOL_CALL/TOOL_RESULT/LLM_CALL/PROCESSING',
    title           VARCHAR(200)                     COMMENT '步骤描述（前端展示）',
    content         MEDIUMTEXT                       COMMENT '步骤内容',
    tool_name       VARCHAR(50)                      COMMENT 'Tool 名称',
    tool_input      JSON                             COMMENT 'Tool 入参',
    tool_output     MEDIUMTEXT                       COMMENT 'Tool 结果',
    token_usage     JSON                             COMMENT 'Token 消耗 {"prompt":N,"completion":N}',
    status          VARCHAR(20) NOT NULL DEFAULT 'RUNNING'
                        COMMENT 'RUNNING/COMPLETED/FAILED/SKIPPED',
    error_message   TEXT                             COMMENT '错误信息',
    sort_order      INT DEFAULT 0                   COMMENT '步骤序号',
    duration_ms     INT                              COMMENT '耗时（毫秒）',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_rstep_task_id (task_id),
    INDEX idx_rstep_execution_id (execution_id),
    INDEX idx_rstep_agent_name (agent_name),
    INDEX idx_rstep_status (status),
    INDEX idx_rstep_task_agent (task_id, agent_name, sort_order),
    FOREIGN KEY (task_id) REFERENCES research_task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究步骤表（Agent 执行日志）';

-- ============================================================
-- 6. research_source（研究来源）
-- ============================================================
DROP TABLE IF EXISTS research_source;
CREATE TABLE research_source (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT NOT NULL                 COMMENT '所属项目 ID',
    task_id         BIGINT                          COMMENT '关联任务 ID',
    title           VARCHAR(500) NOT NULL           COMMENT '来源标题',
    url             VARCHAR(2000)                    COMMENT '来源 URL',
    source_type     VARCHAR(30) NOT NULL             COMMENT '来源类型：web_search/official_doc/paper/github/article/internal',
    snippet         TEXT                             COMMENT '内容摘要',
    full_content    LONGTEXT                         COMMENT '完整内容',
    relevance_score DECIMAL(5,4)                     COMMENT '与研究的关联度 0-1',
    reliability     VARCHAR(20) DEFAULT 'unverified' COMMENT '可靠性：high/medium/low/unverified',
    content_hash    VARCHAR(64)                      COMMENT '内容 SHA-256 哈希（去重）',
    fetch_status    VARCHAR(20) DEFAULT 'success'    COMMENT '抓取状态：success/failed/timeout/skipped',
    fetched_at      DATETIME                        COMMENT '抓取时间',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_rsrc_project_id (project_id),
    INDEX idx_rsrc_task_id (task_id),
    INDEX idx_rsrc_source_type (source_type),
    INDEX idx_rsrc_reliability (reliability),
    INDEX idx_rsrc_content_hash (content_hash),
    FULLTEXT INDEX ft_rsrc_content (title, snippet),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES research_task(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究来源表';

-- ============================================================
-- 7. research_finding（研究中间发现）
-- ============================================================
DROP TABLE IF EXISTS research_finding;
CREATE TABLE research_finding (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT NOT NULL                 COMMENT '所属项目 ID',
    task_id         BIGINT                          COMMENT '关联任务 ID',
    agent_name      VARCHAR(50) NOT NULL             COMMENT '产生该发现的 Agent',
    statement       TEXT NOT NULL                    COMMENT '发现内容',
    category        VARCHAR(30)                      COMMENT '分类：fact/insight/question/contradiction/gap',
    source_ids      JSON                             COMMENT '支撑来源 ID 列表 [1,2,3]',
    confidence      VARCHAR(20) DEFAULT 'medium'     COMMENT '可信度：high/medium/low/speculation',
    is_promoted     TINYINT DEFAULT 0               COMMENT '是否已升级为 conclusion',
    promoted_to_id  BIGINT                           COMMENT '升级后的 conclusion_id',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_rfnd_project_id (project_id),
    INDEX idx_rfnd_task_id (task_id),
    INDEX idx_rfnd_agent_name (agent_name),
    INDEX idx_rfnd_category (category),
    INDEX idx_rfnd_is_promoted (is_promoted),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES research_task(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究中间发现表';

-- ============================================================
-- 8. research_conclusion（研究结论）
-- ============================================================
DROP TABLE IF EXISTS research_conclusion;
CREATE TABLE research_conclusion (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT NOT NULL             COMMENT '所属项目 ID',
    task_id             BIGINT                      COMMENT '关联任务 ID',
    statement           TEXT NOT NULL                COMMENT '结论陈述',
    confidence          VARCHAR(20) NOT NULL DEFAULT 'medium'
                            COMMENT 'high/medium/low/speculation',
    supporting_sources   JSON                        COMMENT '支持来源 [{sourceId,quote,relevance}]',
    conflicting_sources  JSON                        COMMENT '冲突来源 [{sourceId,quote}]',
    is_key_finding      TINYINT DEFAULT 0            COMMENT '是否为关键发现',
    is_controversial    TINYINT DEFAULT 0            COMMENT '是否存在冲突信息',
    critic_notes        TEXT                         COMMENT 'Critic Agent 验证备注',
    version             INT NOT NULL DEFAULT 1      COMMENT '结论版本号',
    previous_version_id BIGINT                       COMMENT '上一版本 conclusion_id',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_rcon_project_id (project_id),
    INDEX idx_rcon_task_id (task_id),
    INDEX idx_rcon_confidence (confidence),
    INDEX idx_rcon_is_key_finding (is_key_finding),
    INDEX idx_rcon_project_confidence (project_id, confidence),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES research_task(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究结论表';

-- ============================================================
-- 9. research_report（研究报告 — 最终输出）
-- ============================================================
DROP TABLE IF EXISTS research_report;
CREATE TABLE research_report (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT NOT NULL             COMMENT '所属项目 ID',
    version             INT NOT NULL DEFAULT 1      COMMENT '报告版本号',
    title               VARCHAR(300) NOT NULL        COMMENT '报告标题',
    summary             TEXT                         COMMENT '报告摘要',
    content_md          LONGTEXT                     COMMENT '报告正文（Markdown）',
    research_questions  JSON                         COMMENT '研究问题列表',
    key_findings        JSON                         COMMENT '关键发现 [{statement,confidence,sourceIds}]',
    knowledge_gaps      JSON                         COMMENT '发现的知识缺口 [{topic,description,priority}]',
    new_knowledge_ids   JSON                         COMMENT '新写入的 knowledge_node ID 列表',
    new_relation_ids    JSON                         COMMENT '新写入的 knowledge_relation ID 列表',
    source_count        INT DEFAULT 0               COMMENT '引用来源总数',
    conclusion_count    INT DEFAULT 0                COMMENT '结论总数',
    token_usage_total   JSON                         COMMENT '总 Token 消耗 {"prompt":N,"completion":N}',
    duration_total_ms   BIGINT                       COMMENT '总耗时（毫秒）',
    generated_by        VARCHAR(50) DEFAULT 'SYNTHESIZER_AGENT' COMMENT '生成方',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_rrpt_project_id (project_id),
    INDEX idx_rrpt_project_version (project_id, version),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究报告表（最终输出）';

-- ============================================================
-- 10. research_knowledge_candidate（知识候选 — 待用户确认）
-- ============================================================
DROP TABLE IF EXISTS research_knowledge_candidate;
CREATE TABLE research_knowledge_candidate (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT NOT NULL             COMMENT '所属项目 ID',
    source_ids          JSON                         COMMENT '支撑来源 ID 列表',
    conclusion_ids      JSON                         COMMENT '支撑结论 ID 列表',
    title               VARCHAR(200) NOT NULL        COMMENT '建议标题',
    content_md          TEXT                         COMMENT '建议正文（Markdown）',
    summary             TEXT                         COMMENT '摘要',
    tags                JSON                         COMMENT '建议标签 ["tag1","tag2"]',
    importance          TINYINT DEFAULT 3            COMMENT '建议重要程度 1-5',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            COMMENT 'PENDING/ACCEPTED/REJECTED/MODIFIED/WRITTEN',
    written_node_id     BIGINT                       COMMENT '写入后的 knowledge_node.id',
    user_feedback       TEXT                         COMMENT '用户修改意见',
    reviewed_at         DATETIME                     COMMENT '用户审核时间',
    written_at          DATETIME                     COMMENT '写入知识库时间',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_rkc_project_id (project_id),
    INDEX idx_rkc_status (status),
    INDEX idx_rkc_written_node_id (written_node_id),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究知识候选表（待用户确认）';

-- ============================================================
-- 11. agent_message（Agent LLM 对话记录 — 审计日志）
-- ============================================================
DROP TABLE IF EXISTS agent_message;
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
    metadata_json       JSON                          COMMENT '额外元数据',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_amsg_execution_id (execution_id),
    INDEX idx_amsg_step_id (step_id),
    INDEX idx_amsg_agent_name (agent_name),
    INDEX idx_amsg_execution_agent (execution_id, agent_name, create_time),
    FOREIGN KEY (execution_id) REFERENCES agent_execution(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent LLM 对话记录表';

-- ============================================================
-- 12. research_memory（Agent 长期记忆）
-- ============================================================
DROP TABLE IF EXISTS research_memory;
CREATE TABLE research_memory (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT NOT NULL             COMMENT '所属项目 ID',
    user_id             BIGINT NOT NULL              COMMENT '用户 ID',
    memory_key          VARCHAR(100) NOT NULL        COMMENT '记忆键（唯一标识）',
    memory_type         VARCHAR(30) NOT NULL         COMMENT '类型：knowledges_state/gap_found/search_result/user_preference/decision',
    content             LONGTEXT NOT NULL             COMMENT '记忆内容（Markdown 文本，见 V12 迁移）',
    last_accessed_at    DATETIME                     COMMENT '上次访问时间',
    expires_at          DATETIME                     COMMENT '过期时间（NULL=永不过期）',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_rmem_project_id (project_id),
    INDEX idx_rmem_user_id (user_id),
    UNIQUE KEY uk_rmem_project_memory (project_id, memory_key),
    INDEX idx_rmem_expires_at (expires_at),
    FOREIGN KEY (project_id) REFERENCES research_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 长期记忆表';

-- ============================================================
-- 迁移完成
-- ============================================================
-- 共新增：
-- - 12 张表
-- - 1 个 ALTER（knowledge_node 新增 2 列）
-- ============================================================
