# AI-SecondBrain 完整性审查报告

**审查时间**：2026-09-15
**审查范围**：backend / frontend / sql / docker-compose / extension
**结论**：**核心业务链路已完整，但存在若干断链导致部分功能不可达、全新部署会失败。**

> **2026-09-15 二轮更新**：P0 三项 + Dashboard 死链已全部修复，详见文末「修复记录」。
> 同时更正两处首轮误判，见「首轮结论更正」。

---

## 首轮结论更正

以下内容为首轮静态分析的错误结论，已作废：

| 原结论 | 更正 |
|--------|------|
| **#9 端口不一致**（`application.yml` 8081 vs compose 8080） | **不存在此问题**。`application-prod.yml:11` 端口就是 8080，Docker 走 prod profile（compose 已设 `SPRING_PROFILES_ACTIVE=prod`），两边一致；本地默认 profile 是 8081，`vite.config.js` 代理也是 8081，也一致。两个环境各自自洽。 |
| **#1 四个核心功能 UI 不可达**（含知识图谱、对话管理） | **夸大了**。「知识体系」页（`KnowledgeSystem.vue:516-554`）已经用 echarts `type:"graph"` 渲染了知识图谱，调的正是 `/knowledge/relation/graph`；对话采集则由已上线的 `Capture.vue`（/capture）承接。真正缺失的只有**学习报告**一项。 |

---

## 一句话结论

后端代码质量高（全仓零 TODO、零空实现占位，业务 Service 全部真查库），
但**前端有 4 个写好的核心页面没注册路由**，**数据库初始化脚本路径是错的**，
这两条拦住了「完整可用」。

---

## P0 — 阻断可用（必须修）

### 1. 页面未注册路由导致跳转失效 ✅ 已修复

`frontend/src/` 下全量搜索 `KnowledgeGraph.vue | RagQA.vue | Chat.vue | Report.vue` → **零引用**。
逐个甄别后结论分化：

| 页面 | 行数 | 判定 | 处置 |
|------|------|------|------|
| `views/Report.vue` | 785 | **真正缺失** —— 学习报告无入口 | ✅ 注册 `/report` + 加入侧边栏 |
| `views/Chat.vue` | 868 | 功能与已上线的 `Capture.vue`（/capture）重复，同是「对话采集」 | ✅ 按用户决定注册 `/chat`，但**不加入侧边栏**（避免两个「数据采集」入口） |
| `views/KnowledgeGraph.vue` | 673 | 与已上线的「知识体系」页重复，该页已渲染 knowledge graph | ❌ 不注册（保留文件，不接线） |
| `views/RagQA.vue` | 369 | 知识体系页已有问答框，调同一个 `/rag/answer` | ❌ 不注册（按用户决定） |

> **新增发现（比原结论更严重）**：`Dashboard.vue` 的快捷卡片本身就有两个死链——
> `:112` `router.push('/chat')`、`:126` `router.push('/report')`，而这两个路由当时都不存在，
> 用户在工作台点击卡片会直接跳空白页。所以这不是「功能没做」，是「入口是坏的」。

### 2. 数据库初始化脚本断裂 ✅ 已修复（全新部署必失败）

| 问题 | 证据 |
|------|------|
| **挂载路径错误** | `docker-compose.yml:20` 写 `./complete_database_schema_verified.sql`，但文件实际在 `sql/` 下（根目录无此文件）→ 容器启动即失败 |
| **主脚本名不副实** | `sql/complete_database_schema_verified.sql` 只有 **12 张** CREATE TABLE，而 Entity 有 **39 个** |
| **无 Flyway** | 全项目无 flyway 配置，`V3~V9` 迁移脚本不会自动执行，必须人工按序跑 |

正确的建库顺序应为：`sql/second_brain.sql`（32 表）→ V3 → V4 → V5 → V6 → V7 → V8 → V9。
即便走完这套，仍缺 7 张表（`ai_model` / `ai_provider` / `user_ai_config` /
`user_ai_provider_key` / `pending_knowledge` / `review_card_pool` / `user_review_card`），
需由 V7/V8/V9 补齐。

### 3. 批量删除必崩 ✅ 已修复（运行时 TypeError）

- `frontend/src/views/Knowledge.vue:1068` 调用 `knowledgeAPI.batchDelete(...)`
- 但 `frontend/src/api/knowledge.js` **未定义** `batchDelete`（全 src 搜索 `batchDelete` 仅此一处）
- 进一步排查发现：**后端根本没有批量删除接口**，`KnowledgeController` 只有 `@DeleteMapping("/{id}")` 单条删除

即前端在调一个前后端都不存在的方法，用户点「批量删除」必然报错。

---

## P1 — 功能半成品（后端齐活，前端没接线）

### 4. 导出功能

- 后端：`ExportController` 已实现 5 个端点（markdown / pdf / word / json / csv）
- 前端封装：`api/export.js` 5 个函数齐全
- **前端入口：无**。`api/export.js` 整模块零引用；`Knowledge.vue:1084` 只弹一句
  `ElMessage.info("批量导出功能开发中")`

即：能力全在，只差把按钮接到已封装好的 API 上。

### 5. 学习报告

- `api/report.js`（`generateReport` / `getReportList` / `getReportDetail` / `deleteReport`）**整模块零引用**
- 原因同 #1：`Report.vue` 没注册路由

### 6. 复习提醒链路空转（唯一真正的后端空实现）

`backend/.../service/impl/ReviewReminderServiceImpl.java` 三个方法**只有日志，无任何逻辑**：

| 行号 | 方法 | 声明要做 | 实际 |
|------|------|---------|------|
| `:83-89` | `processExpiredReminders()` | 处理过期复习提醒 | 仅 2 行 log，无扫描、无通知 |
| `:97-101` | `sendDailyReviewNotification()` | 发送每日复习提醒 | 仅 2 行 log，不发任何通知 |
| `:109-113` | `generateWeeklyReport()` | 生成每周复习报告 | 仅 2 行 log，不生成不落库 |

后果：艾宾浩斯排期（`EbbinghausServiceImpl`）算出了下次复习时间，但提醒投递环节被完全吞掉。
这是全仓唯一确凿的「半成品」。

---

## P2 — 占位提示与配置风险

### 7. 六处「功能开发中」占位

| 位置 | 占位功能 |
|------|---------|
| `Knowledge.vue:1011` | 导入 |
| `Knowledge.vue:1015` | 新建体系 |
| `Knowledge.vue:1019` | 高级筛选 |
| `Knowledge.vue:1084` | 批量导出（← 后端已就绪，见 #4） |
| `Capture.vue:564` | 采集编辑 |
| `Review.vue:1198` | 复习设置 |
| `Chat.vue:521` | 对话编辑（页面本就无路由） |

### 8. ES / Kafka 静默降级风险

- `application.yml:42` `KAFKA_ENABLED` 默认 `true`、`:60` `ES_ENABLED` 默认 `true`
- 未部署 ES/Kafka 时走 `NoOpElasticsearchService:51-81`，`search` / `multiFieldSearch` /
  `semanticSearch` **无条件返回 emptyList**
- 结果：搜索功能「不报错但永远空」，属无声故障，建议改为显式提示或关闭开关

### 9. ~~端口不一致~~ —— 结论作废，不是问题

原误判已更正（见顶部「首轮结论更正」）。两套配置各自自洽：

| 环境 | profile | 后端端口 | 配套配置 |
|------|---------|---------|---------|
| 本地开发 | 默认 `application.yml` | **8081** | `vite.config.js:16` 代理 8081 ✅ |
| Docker | prod（`application-prod.yml`） | **8080** | compose `:218` 映射 8080、`:233` 健康检查 8080 ✅ |

仅 README 与部分文档里写的 8080 与本地实际端口不符，属文档瑕疵，不影响运行。

### 10. 其他配置占位

| 项 | 位置 | 说明 |
|----|------|------|
| AI Key 全空 | `application.yml:119/123/131/159` | 未配则 AI 提取/出题/RAG 全部不可用 |
| OSS 凭据空 | `application.yml:146-147` | 文件上传不可用 |
| JWT secret 硬编码 | `application.yml:140` | 生产环境应改 |
| AI 加密 key 硬编码 | `application.yml:111` | 生产环境应改 |
| `application-local.yml:33` | 硬编码一个 sk- 密钥 | **疑似泄露，建议轮换并加 .gitignore** |

### 11. 文档状态滞后（不影响运行，但会误导）

| PRD | 文档标注 | 代码实际 |
|-----|---------|---------|
| `prd-multi-tenant-rbac.md` | 草稿 | 已完整实现（AdminController 10 端点 + Workspace 体系 + v1.0-rbac-migration.sql） |
| `prd-knowledge-square.md` | 待评审 | 已完整实现（SquareController 12 端点，真分页真点赞） |
| `prd-collaborative-knowledge-sharing.md` | 待评审 | 已完整实现（ShareController，SHA-256 token） |
| `需求池.md` #1/#2/#3 | 可以做了 / 需要想想 | 均已落地，状态未更新 |

---

## 已核实为「真实完整实现」的模块

以下模块经代码级核查，确认是真查库、真落表，非空壳：

| 模块 | 证据 |
|------|------|
| 多租户 RBAC 工作区 | `WorkspaceServiceImpl:88-104` 真实成员过滤与计数；`AdminController` 10 端点 |
| 知识广场 | `SquareServiceImpl:207-222` 真实 `selectPage`；`:250-260` 真实点赞增删与计数刷新 |
| 协作分享 | `ShareServiceImpl:170` 真实 SHA-256 token 生成 |
| 游戏化 | `GamificationServiceImpl:700-740` 真实成就进度三表联查 |
| 通知中心 | `NotificationController:46/56/68/79` 真实分页与已读写入 |
| 采集入库工作流 | `Knowledge.vue` 待确认 Tab + `PendingKnowledgeServiceImpl.confirmBatch` 已落地 |
| 工作区题目池 | 后端 `ReviewCardController:271-332` pool CRUD 齐全；`ReviewCardServiceImpl:112-114` 写 pool + 自动建个人副本；前端 `Review.vue:111/314/1240/1251` 双 Tab + 加入复习 **全链路打通** |
| 用户级 AI 模型选择 | 9 服务商 + 5 场景配置已上线 |
| 知识图谱 / RAG | 后端算法与检索真实（余弦相似度 `VectorSearchServiceImpl:90-100`），**仅缺前端入口** |

> 备注：多处 `emptyList()` / `return null` 经逐条核查均为「空结果短路保护」与「守卫分支」，
> 前序都有真实 mapper 查询，**不是假实现**。

---

## 剩余待办优先级（第一轮已完成）

**✅ 已完成**：数据库初始化链路、批量删除、路由断链 → 见「修复记录」

**第二轮（补完剩余的半成品）**
1. `ReviewReminderServiceImpl:83/97/109` 三个空方法 —— 复习提醒链路目前是断的
2. 把「批量导出」接到已有的 `api/export.js`（后端 5 个导出端点早已就绪）
3. 六处「功能开发中」占位按需升级成真实功能，或干脆移除按钮避免误导

**第三轮（安全与健壮性）**
4. 轮换 `application-local.yml:33` 疑似泄露的 sk- 密钥，并加入 `.gitignore`
5. 处理 ES/Kafka 静默降级：未部署时不要静默返回空结果，应显式报错或关闭开关
6. 清理孤儿文件：`KnowledgeGraph.vue`、`RagQA.vue`、`AsyncTaskDemo.vue` 若确定不用可直接删除

**文档同步**
7. README / 需求池的状态标记普遍滞后于代码，建议一次性校正

---

---

## 修复记录（2026-09-15 第二轮）

按「先保证完整运转，不做优化」原则，只处理 P0 阻断项。共改动 **8 个文件**。

### 已修复

**① 批量删除全链路打通**（后端本来就没这接口）

| 文件 | 改动 |
|------|------|
| `backend/.../service/KnowledgeService.java:63` | 新增接口方法 `int deleteBatchByIds(List<Long>, Long, Long)` |
| `backend/.../service/impl/KnowledgeServiceImpl.java:160-197` | 新增实现：**先逐个校验存在性与权限，全部通过后才执行删除**，避免删到一半才报错；`@Transactional(rollbackFor = Exception.class)` 保证事务；同步清理 ES 文档 |
| `backend/.../controller/KnowledgeController.java:130-144` | 新增 `@DeleteMapping("/batch")`，返回实际删除数量 |
| `frontend/src/api/knowledge.js:50-56` | 补齐 `batchDelete(ids)`，至此 `Knowledge.vue:1068` 的调用成立 |

采用「事务 + 全校验后删」而非简单循环调用单条删除，是依用户选择的方案 A。

**② 数据库初始化链路**

`docker-compose.yml` 与 `db-compose.yml` 两处均改为挂载完整且**有序**的脚本：

```
01-second_brain.sql              ← sql/second_brain.sql（32 张基础表）
02-V7__add_ai_provider_tables.sql ← AI 服务商/模型/用户配置（4 表 + 种子数据）
03-V8__add_pending_knowledge.sql  ← 待确认知识点（1 表）
04-V9__add_review_card_pool.sql   ← 题目池 + 个人副本（2 表）
```

MySQL 的 initdb 按文件名字典序执行，故用数字前缀锁定顺序（V3~V6 的表已合并进 `second_brain.sql`，无需重复执行）。

> ⚠️ **注意**：此改动只对**全新数据库**生效。已存在的 `mysql-data` 卷不会重跑 initdb，
> 老环境请手动执行 `02/03/04` 三个脚本，或删卷重建。

**③ 路由断链修复**

| 文件 | 改动 |
|------|------|
| `frontend/src/router/index.js:101-114` | 注册 `/report` → Report.vue、`/chat` → Chat.vue（均在 MainLayout 下，受登录保护） |
| `frontend/src/layout/MainLayout.vue:131` | 侧边栏新增「学习报告」（Document 图标）。Chat 未加入侧边栏——它与「数据采集」重复，只保留工作台卡片入口 |

至此 `Dashboard.vue:112` 和 `:126` 两张快捷卡片不再跳空白。

**④ 顺带修出一个隐藏的编译错误**

`frontend/src/views/Chat.vue:346-347` 存在重复的
`import { useWorkspaceStore } from "@/stores/workspace";`（重复声明同一标识符）。
因为这个页面从未被任何模块引用，Vite 也从没编译过它 —— **一注册路由就构建失败**。
已删除重复行。这也解释了「孤儿页面」为什么能一直藏着不出声。

### 验证情况

| 项目 | 结果 |
|------|------|
| 前端构建 `vite build` | ✅ 通过，2137 modules，`Report`/`Chat` 均正常产出 chunk（`Report--d0ydOfo.js` 7.55 kB、`Chat-bFVL2udN.js` 11.05 kB） |
| 前端 JS 语法 `node --check` | ✅ `api/knowledge.js`、`router/index.js` 均通过 |
| 后端 Java 编译 | ⚠️ **未验证** —— 本机 PATH 无 JDK/Maven。改动已人工复核（`hasAccess` 签名一致、`List`/`ArrayList`/`Transactional` 均已导入、接口仅一个实现类），请本地执行一次 `mvn clean compile` 确认 |

### 未处理（按约定不做）

- P1：导出功能前端接线、`ReviewReminderServiceImpl` 三个空方法、学习报告页并不需要额外后端开发
- P2：六处「功能开发中」占位、ES/Kafka 静默降级、`application-local.yml` 疑似泄露的 sk- 密钥

---

**审查人**：WorkBuddy
**审查方法**：静态代码核查（Grep/Read）+ 前端实际构建验证；未启动后端服务
