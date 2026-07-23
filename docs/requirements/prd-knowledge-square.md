# 知识广场社交功能 PRD

> 版本：V1.0
> 日期：2026-07-23
> 状态：待评审
> 来源：`docs/需求池.md` #3 社交功能（评论/点赞）
> 依赖：#1 多用户数据隔离与管理员管控（已就绪）、#2 协作共享知识库（已就绪）

---

## 1. 执行摘要

### 问题陈述

当前 AI-SecondBrain 的知识节点仅在个人或工作区上下文中存在——用户之间无法跨工作区发现优质内容，也无法在平台层面进行互动。知识管理沦为「孤岛」，缺乏社区感和参与动力。

### 解决方案

建立**双层知识广场**体系：
- **全局广场**：所有用户可发布知识节点、浏览、点赞、评论、收藏。内容公开可见。
- **工作区广场**：仅同一工作区成员可见的分享空间，适合团队内部知识沉淀和讨论。

### 设计决策摘要

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 发布机制 | 主动发布 | 知识节点不会自动曝光，用户精选后再分享到广场，保证内容质量 |
| 用户关系 | 无关系模型 | 纯内容驱动，降低冷启动门槛，类似论坛/贴吧 |
| 审核策略 | 混合策略 | 敏感词自动过滤 + 用户举报 → admin 审核队列。正常内容直接上架 |

### 成功标准

| KPI | 目标值 | 衡量方式 |
|-----|--------|----------|
| 广场发布率 | 上线 30 天内 >= 15% 活跃用户发布过内容 | 广场帖子数 / 活跃用户数 |
| 互动参与率 | 人均互动次数 >= 3 次/周（点赞+评论+收藏） | 互动事件计数 / 周活用户 |
| 审核效率 | 举报内容 < 4 小时内处理 | 审核队列平均处理时长 |
| 违规内容拦截率 | 敏感词命中率 >= 90% | 违规内容数 / 总发布数 |

---

## 2. 用户体验与功能

### 用户画像

| 角色 | 场景 | 核心诉求 |
|------|------|----------|
| 知识分享者 | 整理好一篇知识笔记后想分享出去 | 一键发布到广场，获得反馈和认可 |
| 知识浏览者 | 想发现新领域的优质知识内容 | 按热度/时间浏览，搜索感兴趣的内容 |
| 社区互动者 | 看到好内容想表达认可或讨论 | 点赞、评论、收藏，简单直接 |
| 平台管理员 | 需要管控广场内容质量 | 敏感词过滤、举报处理、内容下架 |

### 用户故事

**阶段一：全局知识广场**

| ID | 故事 | 验收标准 |
|----|------|----------|
| US-6 | 作为用户，我要将知识节点发布到全局广场，附带推荐语，让所有人都能看到 | 1. 知识详情页有「发布到广场」按钮 2. 可填写推荐语（≤200字）3. 发布后立即出现在广场列表 4. 可随时取消发布（下架） |
| US-7 | 作为用户，我要在广场浏览所有用户发布的知识内容，按热度或时间排序 | 1. 广场页面展示卡片列表（标题、摘要、作者、点赞数、评论数）2. 支持「最新」和「最热」两种排序 3. 支持关键词搜索 4. 分页加载 |
| US-8 | 作为用户，我要对广场内容点赞表达认可 | 1. 每个帖子显示点赞数和点赞状态 2. 点击点赞/取消点赞即时更新 3. 同一用户对同一帖子只能点赞一次 |
| US-9 | 作为用户，我要评论广场内容参与讨论 | 1. 帖子详情页显示评论列表 2. 支持发表评论（≤500字）3. 评论者可以删除自己的评论 4. 帖子作者可以删除自己帖子下的任何评论 |
| US-10 | 作为用户，我要收藏广场内容以便稍后阅读 | 1. 每个帖子有收藏按钮 2. 收藏/取消收藏即时切换 3. 在个人收藏列表查看所有收藏内容 4. 被下架的内容自动从收藏列表移除 |

**阶段二：工作区知识广场**

| ID | 故事 | 验收标准 |
|----|------|----------|
| US-11 | 作为工作区成员，我要将知识节点发布到工作区广场，仅工作区成员可见 | 1. 发布时可选择「全局广场」或「工作区广场」2. 工作区成员可见该内容 3. 非成员无法看到 |
| US-12 | 作为工作区成员，我要在工作区广场内浏览、点赞、评论、收藏 | 与全局广场完全相同的交互，但范围限定在工作区内 |

**阶段三：内容审核与通知**

| ID | 故事 | 验收标准 |
|----|------|----------|
| US-13 | 作为平台管理员，我要审核被举报的内容并决定是否下架 | 1. 管理后台有「举报管理」模块 2. 列表显示举报原因、时间、举报人 3. 可查看被举报内容 4. 支持「忽略举报」和「下架内容」两种操作 |
| US-14 | 作为用户，我要收到互动通知（被点赞、被评论、内容被举报结果） | 1. 顶部导航栏有通知图标+未读数角标 2. 点击展开通知列表 3. 通知类型：点赞、评论、举报处理结果 4. 点击通知跳转到对应内容 |
| US-15 | 作为用户，如果我的发布内容命中敏感词，发布时给出提示 | 1. 发布时前端+后端双重校验敏感词 2. 命中时提示具体哪个词违规 3. 修改后可重新提交 |

### 非目标（本 PRD 不做）

- 关注/好友关系模型（决策为无关系模型）
- 私信/即时通讯功能
- 内容推荐算法（基于用户画像的个性化推荐）
- 富文本评论（评论仅支持纯文本）
- 评论的评论（仅支持一级评论，不支持嵌套回复——V1.2 补充）

---

## 3. 技术规格

### 架构概览

```
前端                                 后端                                  数据层
───────                              ──────                                ──────
广场页面 ──[浏览帖子]──▶  SquareController  ──▶  square_post 表
   │                                  │
   ├──[点赞]──────▶  SquareController  ──▶  square_like 表
   │                                  │
   ├──[评论]──────▶  SquareController  ──▶  square_comment 表
   │                                  │
   ├──[收藏]──────▶  SquareController  ──▶  square_bookmark 表
   │                                  │
   ├──[发布]──────▶  SquareController  ──▶  square_post 表
   │                      │                       │
   │                  [敏感词过滤]            knowledge_node (关联)
   │
通知组件 ◀──[WebSocket]──  NotificationService  ──▶  notification 表
   │                                  │
管理后台 ──[举报处理]──▶  AdminController  ──▶  square_report 表
```

### 新增数据表

#### square_post — 广场帖子

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| node_id | BIGINT | 关联的知识节点 ID |
| author_id | BIGINT INDEX | 发布者用户 ID |
| scope | VARCHAR(20) | 发布范围：`global` / `workspace` |
| workspace_id | BIGINT | 工作区 ID（scope=workspace 时必填，scope=global 时 null） |
| recommend_text | VARCHAR(200) | 推荐语/分享理由 |
| like_count | INT DEFAULT 0 | 点赞数（冗余计数器，避免 COUNT 查询） |
| comment_count | INT DEFAULT 0 | 评论数（冗余计数器） |
| bookmark_count | INT DEFAULT 0 | 收藏数（冗余计数器） |
| status | VARCHAR(20) DEFAULT 'published' | 状态：`published` / `removed` |
| created_at | DATETIME | 发布时间 |
| updated_at | DATETIME | 更新时间 |

- INDEX：`idx_scope_created` (scope, created_at DESC)、`idx_scope_like` (scope, like_count DESC)、`idx_author` (author_id)、`idx_workspace` (workspace_id)

#### square_like — 点赞记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| post_id | BIGINT | 帖子 ID |
| user_id | BIGINT | 点赞用户 ID |
| created_at | DATETIME | 点赞时间 |

- UNIQUE KEY：`uk_post_user` (post_id, user_id) — 一人对一帖只能点赞一次
- INDEX：`idx_user` (user_id)

#### square_comment — 评论

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| post_id | BIGINT INDEX | 帖子 ID |
| user_id | BIGINT | 评论者用户 ID |
| content | VARCHAR(500) | 评论内容 |
| created_at | DATETIME | 评论时间 |
| deleted | TINYINT DEFAULT 0 | 删除标记（逻辑删除） |

#### square_bookmark — 收藏

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| post_id | BIGINT | 帖子 ID |
| user_id | BIGINT | 收藏用户 ID |
| created_at | DATETIME | 收藏时间 |

- UNIQUE KEY：`uk_post_user` (post_id, user_id) — 一人对一帖只能收藏一次

#### square_report — 举报记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| post_id | BIGINT | 被举报帖子 ID |
| reporter_id | BIGINT | 举报人用户 ID |
| reason | VARCHAR(500) | 举报原因 |
| status | VARCHAR(20) DEFAULT 'pending' | 处理状态：`pending` / `ignored` / `removed` |
| handler_id | BIGINT | 处理人 ID（admin） |
| handle_note | VARCHAR(500) | 处理备注 |
| created_at | DATETIME | 举报时间 |
| handled_at | DATETIME | 处理时间 |

#### notification — 通知

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT INDEX | 接收通知的用户 ID |
| type | VARCHAR(30) | 通知类型：`like` / `comment` / `report_result` |
| title | VARCHAR(200) | 通知标题 |
| content | VARCHAR(500) | 通知内容 |
| target_type | VARCHAR(30) | 关联目标类型：`post` / `comment` |
| target_id | BIGINT | 关联目标 ID（点击通知可跳转） |
| is_read | TINYINT DEFAULT 0 | 是否已读 |
| created_at | DATETIME | 通知时间 |

#### sensitive_word — 敏感词库

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| word | VARCHAR(100) UK | 敏感词 |
| created_at | DATETIME | 添加时间 |

### API 设计

#### 广场帖子

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/square/post` | 发布到广场 `{ nodeId, scope, workspaceId?, recommendText }` |
| DELETE | `/square/post/{id}` | 下架自己的帖子 |
| GET | `/square/list` | 广场列表 `?scope=global&sort=newest&page=1&size=20&keyword=` |
| GET | `/square/post/{id}` | 帖子详情（含知识节点内容 + 评论列表） |

#### 互动

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/square/post/{id}/like` | 点赞（再次调用取消点赞） |
| GET | `/square/post/{id}/comments` | 评论列表 `?page=1&size=20` |
| POST | `/square/post/{id}/comment` | 发表评论 `{ content }` |
| DELETE | `/square/comment/{id}` | 删除评论（本人或帖子作者） |
| POST | `/square/post/{id}/bookmark` | 收藏（再次调用取消收藏） |
| GET | `/square/bookmarks` | 我的收藏列表 |
| POST | `/square/post/{id}/report` | 举报帖子 `{ reason }` |

#### 通知

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/notification/list` | 通知列表 |
| GET | `/notification/unread-count` | 未读通知数 |
| PUT | `/notification/{id}/read` | 标记已读 |
| PUT | `/notification/read-all` | 全部标记已读 |

#### 管理端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/reports` | 举报列表（pending 优先） |
| PUT | `/admin/reports/{id}/handle` | 处理举报 `{ action: ignore/remove, note }` |
| GET | `/admin/sensitive-words` | 敏感词列表 |
| POST | `/admin/sensitive-words` | 添加敏感词 |
| DELETE | `/admin/sensitive-words/{id}` | 删除敏感词 |

### 通知推送架构

```
互动事件发生（点赞/评论/举报处理）
  → 写入 notification 表
  → 通过 WebSocket 推送到 /topic/notifications/{userId}
  → 前端 notification store 更新
  → UI 角标 + 弹窗提示
```

复用现有 STOMP-over-WebSocket 基础设施（`/ws` 端点），新增 `/topic/notifications/{userId}` 主题。前端需统一为 STOMP 协议（当前 raw WebSocket 与后端 STOMP 不兼容）。

### 敏感词过滤

- 前端发布时预校验（加载敏感词列表到本地缓存，减少网络请求）
- 后端保存时最终校验（读取 `sensitive_word` 表，遍历匹配）
- 命中敏感词 → 返回 400 + 具体违规词，不创建帖子
- 敏感词库由 super_admin 通过管理后台维护

### 安全性

| 层 | 措施 |
|------|------|
| 发布 | 仅已登录用户可发布；需关联自己的知识节点 |
| 下架 | 仅帖子作者本人或 super_admin 可下架 |
| 评论删除 | 评论者本人或帖子作者可删除 |
| 举报 | 已登录用户可举报；重复举报同一帖子去重 |
| 工作区广场 | scope=workspace 时校验用户是否为该工作区成员（accepted 状态） |
| 敏感词 | 前后端双重校验，后端为最终防线 |

---

## 4. 风险与路线图

### 分阶段交付

```
V2.0 MVP — 全局知识广场
├── 发布到全局广场 + 下架
├── 广场列表（最新/最热）+ 搜索
├── 点赞/取消点赞
├── 一级评论（发表/删除）
├── 收藏/取消收藏 + 收藏列表
├── 敏感词过滤（前后端）
└── 举报 + 管理后台处理

V2.1 — 工作区广场 + 通知
├── 工作区广场（scope=workspace）
├── 通知系统（数据库 + WebSocket 推送）
├── 顶部导航通知角标 + 通知列表页
└── 互动事件自动创建通知

V2.2 — 体验增强
├── 评论嵌套回复（二级评论）
├── 帖子详情页 Markdown 完整渲染
├── 个人主页（查看某用户发布的所有帖子）
└── 广场内容数据统计（admin dashboard）
```

### 技术风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 评论区被灌水/垃圾内容 | 高 | 中 | 敏感词过滤 + 举报机制 + 频率限制（单用户每分钟最多 5 条评论） |
| 计数器（like_count 等）与实际数据不一致 | 中 | 低 | 定时任务（每日凌晨）全量校准计数器 |
| WebSocket 通知推送失败导致通知丢失 | 中 | 低 | 通知持久化为数据库优先，WebSocket 推送为增强体验，用户刷新页面即可看到 |
| square_post 表随用户增长查询变慢 | 低 | 中 | 联合索引覆盖排序场景；后续可引入 ES 做搜索 |

### 与现有系统的兼容性

| 现有系统 | 影响 |
|------|------|
| knowledge_node 表 | 无需改表；square_post.node_id 外键关联 |
| 工作区 RBAC | 复用；工作区广场校验成员身份 |
| share_link 表 | 不冲突；广场是「公开可见」，share_link 是「令牌链接」——两个独立渠道 |
| WebSocket /ws | 复用端点；新增 `/topic/notifications/{userId}` 主题 |
| AdminController | 新增 3 个管理端点 |

---

## 5. 验收测试场景

1. **发布流程**：用户 A 创建知识节点 → 点击「发布到广场」→ 选择「全局广场」→ 填写推荐语 → 发布成功 → 广场列表可见
2. **敏感词拦截**：用户 A 的推荐语包含敏感词 → 发布时提示「内容包含违规词：xxx」→ 修改后重新发布成功
3. **点赞**：用户 B 在广场看到帖子 → 点击点赞 → 点赞数+1 → 再次点击 → 取消点赞，点赞数-1
4. **评论**：用户 B 发表评论 → 帖子评论数+1 → 用户 A（帖子作者）可删除该评论 → 评论删除，评论数-1
5. **收藏**：用户 B 收藏帖子 → 个人收藏列表可见 → 取消收藏 → 收藏列表移除
6. **举报与审核**：用户 C 举报帖子 → admin 在后台看到举报 → 处理为「下架」→ 帖子状态变为 removed → 广场不可见
7. **工作区隔离**：用户 A 发布到工作区广场 → 工作区成员用户 B 可见 → 非成员用户 C 不可见
8. **通知**：用户 B 点赞用户 A 的帖子 → 用户 A 收到通知 → 通知角标+1 → 点击查看 → 标记已读
