# 知识标签层级化改造 — PRD

> 日期：2026-07-28
> 状态：部分实现，待真实数据联调
> 来源：需求澄清 — Knowledge.vue 侧边栏「知识体系」硬编码问题

> 实施更新（2026-09-03）：`KnowledgeSystem.vue` 左侧目录也已改为消费 `GET /tags/tree`，显示真实标签层级、标签颜色和知识点数量；点击标签会进入 `/knowledge?tagId={id}` 的筛选视图。此变更不修改标签 API 或数据表。

---

## 1. Executive Summary

**问题**: 知识管理页面（Knowledge.vue）左侧「知识体系」分类侧边栏存在三个问题：

1. **数据源不存在** — 代码尝试从知识点读取 `systemName` 字段做分组，但 `knowledge_node` 表根本没有这个字段,永远走硬编码 fallback（Java 核心技术 85 条、Spring Boot 67 条...全是假数据）
2. **标签系统被闲置** — 后端 `/tags` API 已有完整的标签 CRUD + M:N 节点关联能力，但前端完全没有对接
3. **命名冲突** — 「知识体系」这个词同时出现在侧边导航（指向图谱页）、Knowledge.vue 侧边栏（分类筛选）、KnowledgeSystem.vue（整页标题），三个地方指的不是同一个概念

**方案**: 

1. `knowledge_tag` 表增加 `parent_id` 字段，支持标签层级（如 Java → 多线程 → 线程池）
2. Knowledge.vue 侧边栏改为「知识标签」，对接 `/tags` API，展示标签树
3. 知识点卡片支持打标签/去标签操作
4. AI 在采集入库时自动建议标签，用户在待确认环节确认或修改
5. 统一清理命名冲突

**成功标准**:
1. 用户可在知识管理页面看到真实的标签树（不再有硬编码假数据）
2. 用户可创建/编辑/删除标签，支持父子层级
3. 用户可为知识点打多个标签、移除标签
4. 新建知识点时 AI 自动建议 1-3 个标签，准确率 >= 70%（用户确认即视为准确）
5. 侧边栏筛选标签后知识点列表正确过滤

---

## 2. User Experience & Functionality

### 2.1 用户画像

| 画像 | 描述 | 核心需求 |
|------|------|---------|
| 知识积累者 | 每天采集/导入多条知识，知识点数量增长快 | 快速给知识点分类，不希望手动逐条打标签 |
| 知识检索者 | 知识点多了以后靠搜索找东西 | 通过标签树浏览，按层级缩小范围 |
| 新手用户 | 刚开始用，知识点还不多 | 不需要立即面对复杂的标签管理，AI 自动建议降低上手门槛 |

### 2.2 用户故事

**Story 1 — 标签树浏览与筛选**: 作为知识积累者，我想在知识管理页面左侧看到一个真实的标签树，点击标签后右侧列表只显示该标签及其子标签下的知识点。

**Acceptance Criteria**:
- 侧边栏标题从「知识体系」改为「知识标签」
- 标签树从 `/tags` API 加载真实数据，不再有硬编码 fallback
- 标签按层级缩进展示，父标签可展开/折叠
- 每个标签右侧显示关联的知识点数量
- 点击标签筛选知识点列表，高亮当前选中标签
- 「全部知识」作为默认根节点，显示所有知识点总数
- 空状态：用户尚未创建任何标签时，显示引导文案「创建标签来分类你的知识」
- 图谱入口页（KnowledgeSystem.vue）也复用同一标签树，不再维护第二套“知识体系目录”静态数据

**Story 2 — 标签管理（CRUD + 层级）**: 作为知识积累者，我想创建、编辑、删除标签，并设置父子关系。

**Acceptance Criteria**:
- 侧边栏底部「新建标签」按钮可用（不再是"开发中"）
- 新建标签弹窗：输入标签名称、选择颜色、选择父标签（可选，下拉搜索）
- 右键标签或点击更多菜单：编辑（改名/改色/改父级）、删除
- 删除标签时：如标签下有子标签，提示"该标签下有 N 个子标签，删除后子标签将变为顶级标签"；如标签已被知识点使用，提示"该标签已被 M 个知识点使用，删除后这些知识点将失去此标签"
- 标签名称不能与同级标签重名

**Story 3 — 知识点打标签**: 作为知识积累者，我想在知识点卡片上直接添加或移除标签。

**Acceptance Criteria**:
- 知识点卡片底部展示已有标签（小圆角 chip，带颜色）
- 每个标签 chip 右侧有 × 按钮，点击可移除
- 卡片上有「+ 添加标签」入口，点击弹出下拉搜索框，输入关键词搜索已有标签，选中即添加
- 搜索时如无匹配结果，可快速创建新标签
- 知识点详情弹窗中同样支持标签管理

**Story 4 — AI 自动建议标签**: 作为知识积累者，我希望系统在采集/导入知识点时自动推荐标签，减少手动操作。

**Acceptance Criteria**:
- 对话采集时，如开启「提取知识点」，AI 在提取知识点的同时返回 1-3 个建议标签
- 建议标签出现在待确认知识点的编辑卡片中，以「建议标签」样式展示（虚线边框 + AI 图标，区别于已确认标签）
- 用户可点击建议标签将其转为正式标签（变为实线 chip），或点击 × 忽略
- 用户可手动添加 AI 未建议的标签
- 「确认入库」时，所有已确认的标签随知识点一起保存
- 手动创建知识点时，输入标题和内容后，可点击「AI 推荐标签」按钮触发建议

**Story 5 — 命名清理（非功能）**: 消除「知识体系」一词的多义使用。

**Acceptance Criteria**:
- Knowledge.vue 侧边栏标题：「知识体系」→「知识标签」
- Knowledge.vue 筛选下拉：「全部体系」→「全部标签」
- 代码变量 `knowledgeSystems` → `tagTree` 或 `knowledgeTags`
- 代码变量 `filterSystem` → `filterTag`
- `/knowledge-system` 路由和导航项「知识体系」保持不变（那是独立的图谱+问答页，不同概念）
- 数据库 `knowledge_tag` 表名不变

### 2.3 非目标 (Non-Goals)

- 不重构 KnowledgeSystem.vue 的图谱探索或 RAG 问答功能；仅将其左侧目录替换为真实标签导航
- 不在此 PRD 中实现标签的跨工作区共享（标签暂时用户级隔离）
- 不在此 PRD 中实现基于标签的复习范围筛选（后续需求）
- 不改变采集入库的核心流程，只在其基础上增加标签建议

---

## 3. Technical Specifications

### 3.1 数据模型变更

**knowledge_tag 表 — 新增字段**:

```sql
ALTER TABLE knowledge_tag ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '父标签ID，NULL表示顶级标签';
ALTER TABLE knowledge_tag ADD INDEX idx_parent_id (parent_id);
```

**KnowledgeTag 实体 — 新增字段**:

```java
/**
 * 父标签ID，NULL表示顶级标签
 */
private Long parentId;

/**
 * 子标签列表（非数据库字段，仅用于树形返回）
 */
@TableField(exist = false)
private List<KnowledgeTag> children;

/**
 * 关联的知识点数量（非数据库字段，仅用于展示）
 */
@TableField(exist = false)
private Integer nodeCount;
```

### 3.2 API 变更

#### 修改已有接口

**`GET /tags` — 返回结构改为树形**:

```
当前: [{ id, userId, tagName, tagColor, createTime }]
改为: [{ id, userId, tagName, tagColor, parentId, children: [...], nodeCount, createTime }]
```

按 `parentId` 组装树形结构。`parentId = null` 为根节点。每个节点携带 `nodeCount`（关联知识点数，含子标签下的知识点）。

**`POST /tags` — 增加 parentId 参数**:

```
当前: tagName, tagColor
改为: tagName, tagColor, parentId(optional)
```

**`PUT /tags/{id}` — 新增接口**: 修改标签名称、颜色、父级。

#### 新增接口

**`PUT /tags/{id}`** — 更新标签信息:

```
Request: { tagName?, tagColor?, parentId? }
Response: KnowledgeTag
```

**`POST /tags/ai-suggest`** — AI 建议标签:

```
Request: { title, summary, content? }
Response: [{ tagName, tagColor, confidence }]
```

后端调用已配置的 AI 服务（复用 `AiService`），Prompt 要求返回 1-3 个最适合的标签名。匹配已有标签（相似度 > 80% 则复用已有标签），否则建议新建。

约束：此接口不修改数据库，仅返回建议列表。标签的实际创建和关联由前端在确认入库时通过 `/tags` + `/tags/node/{nodeId}/tag/{tagId}` 完成。

### 3.3 前端变更

#### Knowledge.vue — 侧边栏重构

| 项目 | 当前 | 改为 |
|------|------|------|
| 侧边栏标题 | 「知识体系」 | 「知识标签」 |
| 数据源 | `knowledgeAPI.getList()` 提取 systemName | `tagsAPI.getTree()` 直接获取标签树 |
| fallback 数据 | 8 条硬编码分类 | 删除，API 失败时展示空状态 + 重试按钮 |
| 「新建体系」按钮 | 弹"开发中" | 弹出新建标签对话框 |
| 标签项交互 | 仅点击筛选 | 左键点击筛选 + 右键菜单（编辑/删除） |
| 筛选下拉 | 「全部体系」 | 「全部标签」 |

#### 新增标签管理组件

- **TagCreateDialog.vue**: 新建/编辑标签弹窗（名称、颜色选择器、父标签下拉）
- **TagChips.vue**: 知识点卡片上的标签 chip 列表（含添加/移除交互）
- **TagSuggest.vue**: AI 建议标签展示（虚线 chip + 点击确认/忽略）

#### 知识点卡片增加标签区域

在 `card-meta` 下方、`card-progress` 上方插入标签行，复用 `TagChips` 组件。

#### 待确认知识点增加标签区域

在 `pending-card-body` 中增加标签编辑区域：
- AI 建议标签以虚线 chip 展示（带 AI 图标），点击确认 → 变为实线
- 「添加标签」入口
- 确认入库时随知识点一起保存

### 3.4 标签树构建逻辑

```java
// KnowledgeTagServiceImpl.listByUser(Long userId)
// 1. 查询用户所有标签
// 2. 查询每个标签的知识点数量（含子标签递归汇总）
// 3. 组装树形结构
//    - parentId == null 的标签作为根节点
//    - 递归挂载 children
// 4. 在列表前插入虚拟根节点"全部知识"（nodeCount = 用户知识点总数）
```

### 3.5 AI 标签建议 Prompt 设计

```
你是一个知识分类专家。给定以下知识点的标题和内容，请推荐 1-3 个最合适的分类标签。

要求：
1. 标签应简洁（2-6个字）、准确、具有概括性
2. 优先使用常见的技术领域分类（如"Java""数据库""分布式系统""前端"）
3. 标签层级用 → 表示（如"后端开发 → Java → 多线程"）
4. 返回 JSON 数组格式：[{"tagName": "...", "level": 1}, ...]
   level 1 为顶级标签，level 2 为二级标签

知识点标题：{title}
知识点内容：{summary}

请只返回 JSON 数组，不要有其他内容。
```

---

## 4. Implementation Plan

### Phase 1: 数据库 + 后端基础（1-2天）

| 任务 | 文件 | 说明 |
|------|------|------|
| DDL | migration SQL | `knowledge_tag` 加 `parent_id` 列 |
| Entity 更新 | `KnowledgeTag.java` | 加 `parentId`、`children`、`nodeCount` |
| 标签树接口 | `KnowledgeTagServiceImpl.java` | `listByUser` 返回树形结构 + nodeCount |
| 更新标签接口 | `KnowledgeTagController.java` | 新增 `PUT /tags/{id}` |
| AI 建议接口 | `KnowledgeTagController.java` | 新增 `POST /tags/ai-suggest` |
| 创建标签扩展 | `KnowledgeTagController.java` | `POST /tags` 增加 `parentId` 参数 |

### Phase 2: 前端标签树 + 管理（2-3天）

| 任务 | 文件 | 说明 |
|------|------|------|
| API 层 | `api/tags.js`（新建） | 封装标签相关 API 调用 |
| 侧边栏重构 | `Knowledge.vue` | 对接标签树 API，删除硬编码 fallback |
| 新建/编辑标签弹窗 | `TagCreateDialog.vue` | 名称 + 颜色 + 父标签选择 |
| 右键菜单 | `Knowledge.vue` | 标签项右键菜单（编辑/删除） |

### Phase 3: 知识点打标签（1-2天）

| 任务 | 文件 | 说明 |
|------|------|------|
| 标签 chip 组件 | `TagChips.vue` | 展示 + 添加 + 移除标签 |
| 知识点卡片集成 | `Knowledge.vue` | 卡片中嵌入 TagChips |
| 详情弹窗集成 | `Knowledge.vue` | 详情弹窗中增加标签管理 |

### Phase 4: AI 标签建议（1-2天）

| 任务 | 文件 | 说明 |
|------|------|------|
| AI 建议组件 | `TagSuggest.vue` | 虚线 chip + 确认/忽略交互 |
| 待确认页集成 | `Knowledge.vue` | pending-card 中增加标签区域 + AI 建议 |
| 手动创建集成 | `Knowledge.vue` | 编辑弹窗中增加"AI 推荐标签"按钮 |

---

## 5. Risks & Mitigations

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| 存量知识点没有标签，标签树为空，用户体验差 | 中 | AI 建议降低打标签门槛；首屏引导提示"创建你的第一个标签"；后续可做批量 AI 打标签 |
| 标签层级过深导致树形展示混乱 | 低 | 前端限制展示层级（最多 3 层），超出部分用"更多"折叠 |
| AI 建议标签不准确，用户不信任 | 中 | 建议标签以虚线样式展示（明确标识为 AI 建议），用户一键忽略；后续收集反馈数据优化 prompt |
| parent_id 循环引用（A 的父是 B，B 的父是 A） | 低 | 更新 parentId 时后端校验：目标父级不能是自己的子孙节点 |

---

## 6. Naming Cleanup Summary

| 位置 | 当前文案 | 改为 |
|------|---------|------|
| Knowledge.vue 侧边栏标题 | `知识体系` | `知识标签` |
| Knowledge.vue `filterSystem` 下拉 placeholder | `全部体系` | `全部标签` |
| Knowledge.vue 变量 `knowledgeSystems` | — | `tagTree` |
| Knowledge.vue 变量 `filterSystem` | — | `filterTag` |
| Knowledge.vue 变量 `selectedSystem` | — | `selectedTagId` |
| `/knowledge-system` 路由 | `知识体系` | 不变（独立概念） |
| 侧边导航「知识体系」 | `知识体系` | 不变（指向图谱页） |
