# 工作区共享复习卡片 — PRD

> 日期：2026-07-25
> 状态：设计中
> 依赖：需求池 #6（采集入库工作流重构）
> 关联：docs/prd/capture-workflow-refactor-prd.md

---

## 1. Executive Summary

**问题**: 当前复习卡片完全个人化——用户确认知识入库时勾选"生成复习卡片"，卡片只属于操作者一人。在协同工作区场景下，同一个知识点的卡片无法被其他成员复用，每个成员都需要独立触发生成，导致重复的 AI 调用和重复题目。

**方案**: 引入"题目池 + 个人副本"两层架构。确认入库生成的卡片进入工作区级的**题目池**作为模板，成员浏览池子后选择"加入我的复习计划"获取**个人副本**，每人独立追踪复习进度（掌握程度、间隔排期、正确率）。

**成功标准**:
1. 工作区内一个知识点只需生成一次卡片模板，成员通过"加入复习"获得独立副本
2. 每个成员的复习进度（reviewCount, masteryLevel, nextReviewTime）互不影响
3. 池子题目显示社区热度标签（多数已掌握/半数掌握/普遍困难），帮助成员判断是否加入
4. 存量 review_card 数据无感知升级，`pool_id=NULL` 继续可用
5. 上线后 30 天内，有 2 人以上参与的工作区中，至少 40% 的成员加入过他人生成的题目

---

## 2. User Experience & Functionality

### 2.1 用户画像

| 画像 | 描述 | 核心需求 |
|------|------|---------|
| 工作区 owner | 创建协同工作区，导入或确认知识入库 | 一次性为团队生成题目，不用每人重复生成 |
| 普通成员 | 被邀请加入工作区，学习/复习协同知识 | 看到题目池，选择感兴趣的加入，不看不想加入的 |
| 独立学习者 | 个人空间用户 | 不受任何影响，行为与现在完全一致 |

### 2.2 用户故事

**Story 1**: 作为工作区 owner，确认知识入库并生成卡片后，我想卡片自动进入题目池，我直接就能在"我的复习计划"里看到，不用多次操作。

**Acceptance Criteria**:
- 确认入库 + 勾选"生成复习卡片" → 卡片写入 `review_card_pool`（池子模板）
- 操作者自动获得 `user_review_card` 副本（userId=操作者）
- 操作者在"我的复习计划"中可立即看到题目

**Story 2**: 作为工作区成员，我想浏览题目池，查看每道题的社区掌握情况，再决定是否加入复习。

**Acceptance Criteria**:
- 复习中心新增 Tab："我的复习计划" | "工作区题目池"
- 池子列表每道题显示：题目摘要、初始难度星级、社区标签（🟢多数已掌握 / 🟡半数掌握 / 🔴普遍困难）、当前复习人数
- "加入复习"按钮：点击后生成个人副本，跳转到"我的复习计划"
- 已加入的题目按钮置灰显示"已加入"

**Story 3**: 作为成员，我已经掌握了某道题，想重新来一轮。

**Acceptance Criteria**:
- 已加入的题目在池子中显示"重新加入"按钮
- 点击后生成一份全新副本（reviewCount=0，初始进度）
- 旧副本保留在"我的复习计划"中，标注"历史记录"

**Story 4**: 作为工作区 owner，我发现 AI 生成的某道题答案有误，想修正池子中的题目。

**Acceptance Criteria**:
- owner 可在池子中编辑题目内容（question/answer）
- 修改只影响池子模板，不影响已生成的个人副本
- 新加入的成员看到修正后的版本

### 2.3 非目标 (Non-Goals)

- 不在此 PRD 中改 Ebbinghaus 排期算法
- 不提供"批量加入全部题目"功能
- 不提供题目池内的评论/点赞/社交功能
- 不改变个人空间（workspaceId=NULL）的卡片生成流程
- 不在此 PRD 中改变定时任务 ReviewJob 的自动生成逻辑（仍为每人各自生成）

---

## 3. Technical Specifications

### 3.1 数据模型

**新增表 `review_card_pool`（题目池）**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO | 主键 |
| node_id | BIGINT NOT NULL | 关联知识点 |
| workspace_id | BIGINT NOT NULL | 所属工作区 |
| question | TEXT | 题目内容 |
| answer | TEXT | 正确答案 |
| card_type | VARCHAR(20) | choice / fill / essay / judge |
| difficulty | INT DEFAULT 1 | 初始难度（生成时定死） |
| generation_type | VARCHAR(20) DEFAULT 'auto' | auto / manual |
| create_user_id | BIGINT NOT NULL | 创建者（操作确认入库的人） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT DEFAULT 0 | 逻辑删除 |

**新增表 `user_review_card`（个人副本）**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO | 主键 |
| pool_id | BIGINT NOT NULL | FK → review_card_pool.id |
| user_id | BIGINT NOT NULL | 所属用户 |
| workspace_id | BIGINT NOT NULL | 工作区 |
| review_count | INT DEFAULT 0 | 复习次数 |
| correct_count | INT DEFAULT 0 | 正确次数 |
| incorrect_count | INT DEFAULT 0 | 错误次数 |
| mastery_level | INT DEFAULT 0 | 掌握程度 0-5 |
| memory_strength | DECIMAL(5,4) DEFAULT 0 | 记忆强度 0-1 |
| last_review_time | DATETIME | 上次复习时间 |
| next_review_time | DATETIME | 下次复习时间 |
| status | INT DEFAULT 0 | 0=待复习, 1=已掌握 |
| is_archived | TINYINT DEFAULT 0 | 是否归档（历史副本，重新加入后旧副本标1） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

**修改现有表 `review_card`**: 不加列，不删列。存量数据继续使用，新生成的卡片不再写入此表。

### 3.2 DDL

```sql
-- V9__add_review_card_pool.sql
-- 题目池表
CREATE TABLE review_card_pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    question TEXT,
    answer TEXT,
    card_type VARCHAR(20) NOT NULL DEFAULT 'choice',
    difficulty INT NOT NULL DEFAULT 1,
    generation_type VARCHAR(20) NOT NULL DEFAULT 'auto',
    create_user_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_pool_ws (workspace_id),
    INDEX idx_pool_node (node_id)
);

-- 个人副本表
CREATE TABLE user_review_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pool_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    review_count INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    incorrect_count INT NOT NULL DEFAULT 0,
    mastery_level INT NOT NULL DEFAULT 0,
    memory_strength DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    last_review_time DATETIME NULL DEFAULT NULL,
    next_review_time DATETIME NULL DEFAULT NULL,
    status INT NOT NULL DEFAULT 0,
    is_archived TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_urc_pool (pool_id),
    INDEX idx_urc_user (user_id),
    INDEX idx_urc_ws (workspace_id),
    INDEX idx_urc_next_review (next_review_time),
    FOREIGN KEY (pool_id) REFERENCES review_card_pool(id)
);
```

### 3.3 社区标签计算

```
pool 统计 SQL (无实时性要求，可 5 分钟缓存):

SELECT pool_id,
       COUNT(*) AS member_count,
       SUM(CASE WHEN mastery_level >= 4 THEN 1 ELSE 0 END) AS mastered_count
FROM user_review_card
WHERE status = 0  -- 待复习状态
GROUP BY pool_id

标签规则:
  mastered_count / member_count  >= 0.6 → 🟢 多数已掌握
  mastered_count / member_count  >= 0.3 → 🟡 半数掌握
  mastered_count / member_count  <  0.3  → 🔴 普遍困难
  member_count = 0 → 不显示标签
```

### 3.4 API 端点

**新增**（挂在 `/review` 路径下）:

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/review/pool?workspaceId=` | 获取题目池列表（含社区标签） |
| GET | `/review/pool/{poolId}` | 池子题目详情 |
| POST | `/review/pool/{poolId}/join` | 加入复习（生成个人副本） |
| DELETE | `/review/pool/{poolId}` | owner 删除池子题目 |
| PUT | `/review/pool/{poolId}` | owner 编辑池子题目 |

**修改**:

| 方法 | 路径 | 变更 |
|------|------|------|
| GET | `/review/today` | 数据源从 review_card 切到 user_review_card |
| POST | `/review/submit` | 更新 user_review_card 的进度字段 |
| POST | `/review/generate` | 改为写入 pool + 自动生成操作者副本；个人空间时直接写 user_review_card(pool_id=NULL) |
| POST | `/review/generate-all` | 同上，批量版本 |
| GET | `/review/streak-days` | 统计源切到 user_review_card |
| GET | `/review/accuracy` | 统计源切到 user_review_card |

### 3.5 关键流程

**生成卡片流程（新）**:

```
PendingKnowledgeServiceImpl.confirmBatch()
  if (generateCards):
    for each KnowledgeNode:
      1. insert review_card_pool (workspaceId=node.workspaceId)
      2. insert user_review_card (poolId=pool.id, userId=currentUser) ← 操作者自动加入
```

**加入复习流程**:

```
POST /review/pool/{poolId}/join
  → 检查是否已有非归档副本 → 有则提示"已加入"
  → insert user_review_card (poolId, userId, 初始进度=0)
  → 返回新副本
```

**重新加入流程**:

```
POST /review/pool/{poolId}/join
  → 检查是否已有副本
  → 已有副本 → 标记旧副本 is_archived=1
  → insert 新副本 (poolId, userId, 初始进度=0)
  → 返回新副本
```

### 3.6 向后兼容

- 个人空间（workspaceId=NULL）：generateReviewCard 直接写 `user_review_card(pool_id=NULL)`，行为与改造前一致
- 存量 `review_card` 数据：通过兼容视图或查询适配层读取，`pool_id=NULL` 的 `user_review_card` 即为原地不动的旧数据
- `ReviewJob` 定时任务：暂时不改，仍为每用户独立生成卡片

---

## 4. Risks & Roadmap

### 4.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 新表写入 + 旧表查询共存的过渡期代码复杂 | 查询遗漏、数据不一致 | 封装 ReviewCardRepository 统一查询层，隐藏新旧表切换 |
| 社区标签需聚合查询 | 题目池列表加载变慢 | 缓存 5 分钟，或通过定时任务预计算存入 pool 表冗余字段 |
| ReviewJob 仍按旧逻辑生成 card | 定时生成的卡片不进池子 | 明确定义 ReviewJob 改为本 PRD 的下一阶段处理，当前维持不变 |

### 4.2 分阶段实施

| 阶段 | 内容 | 预计 |
|------|------|------|
| Phase 1 | SQL 迁移（V9）+ Entity/Mapper/DTO 新建 | 1 天 |
| Phase 2 | 后端 Service 层：pool CRUD + join/logout + 社区标签计算 | 2 天 |
| Phase 3 | 后端 API 层：ReviewCardController 新增 + 现有端点适配 | 1.5 天 |
| Phase 4 | 前端：复习中心双 Tab + 题目池 + 加入/重新加入 | 2 天 |
| Phase 5 | confirmBatch 改接入 pool 表 | 0.5 天 |
| Phase 6 | 联调 + 验证 | 1 天 |

---

## 5. 验证清单

| # | 场景 | 预期 |
|---|------|------|
| 1 | 个人空间确认入库 + 生成卡片 | 直接生成 user_review_card(pool_id=NULL)，与旧行为一致 |
| 2 | 工作区确认入库 + 生成卡片 | 生成 pool 记录 + 操作者自动获得 user_review_card |
| 3 | 成员浏览题目池 | 看到池子题目 + 社区标签 + 复习人数 |
| 4 | 成员点击"加入复习" | 生成个人副本，跳转到"我的复习计划" |
| 5 | 已加入的成员再次点击 | 按钮置灰显示"已加入" |
| 6 | 成员点击"重新加入" | 旧副本归档，新副本生成 |
| 7 | owner 编辑池子题目 | 只改 pool，已有副本不变 |
| 8 | owner 删除池子题目 | 只删 pool，已有副本不受影响 |
| 9 | 成员提交答案 | 更新 user_review_card 进度（reviewCount/mastery/nextReviewTime） |
| 10 | 存量 review_card | pool_id=NULL，查询正常，答题正常 |
