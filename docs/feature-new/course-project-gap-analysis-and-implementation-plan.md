# 《大前端与 AI 实战》课程项目差异分析与实施方案（审核稿）

> 版本：v0.3
>
> 状态：核心技术决策已确认；课程版已进入实施，阶段 1—4 的最小闭环已完成
>
> 分析范围：`docs/feature-new` 下 00—16 共 17 份课程材料，以及当前仓库的后端、前端、移动端、数据库脚本和部署配置。

## 1. 文档目的

当前仓库是此前独立开发的 AI-SecondBrain 项目；课程材料要求完成一个功能相近、但技术架构和教学目标不同的“费曼学习平台”。两者不能简单按照“在原项目上补几个页面”处理。

本方案用于在动手开发前回答以下问题：

1. 老师的课程项目到底要求完成哪些功能和技术点。
2. 当前项目哪些能力可以复用，哪些只能作为参考。
3. MongoDB、Node.js/Express、前端框架等架构差异应如何处理。
4. 如何在不破坏现有 AI-SecondBrain 的前提下完成课程项目。
5. 如何拆分开发阶段、定义验收标准并控制风险。

本稿保留为课程版的范围与实施基线；具体接口和当前实现状态以 `course-project/README.md` 及课程版代码为准。由于老师未提供独立任务书/评分表，后续按已确认决策和课程材料完成验收。

RAG 的详细原理、当前实现审计、MongoDB Atlas Vector Search 目标架构、评测指标和分阶段升级方案见：[RAG 架构深度研究与课程版落地方案](../../course-project/docs/rag-architecture-deep-dive.md)。

## 2. 结论摘要

### 2.1 推荐方案

根据当前已确认的决策，采用“课程版独立项目 + 课程版独立 RAG + 现有项目经验参考”的方案：

- 保留现有 `backend/`、`frontend/`、`mobile/` 和现有数据库体系，不迁移、不重构、不替换。
- 新建一个边界清晰的课程项目目录，使用课程要求的 `Node.js + Express + MongoDB + Mongoose` 完成课程版后端。
- 课程版前端使用 `React + Vite`，因为 04—16 课的示例、组件和交互均以 React 为主。
- 课程版独立实现老师要求的 RAG；该实现只放在课程版目录中，不改造或替换当前主项目的 RAG。
- 课程版只实现老师需要展示的最小闭环，不复制当前系统已有的社区、工作区、研究 Agent、权限管理、复杂复习体系等生产级功能。
- 现有项目的知识模型、交互经验、AI 调用经验和部署经验只作为参考，不共享主数据库，也不让课程版自动修改主知识库。
- CesiumJS、Electron 安装包和真实云部署暂按正式交付范围纳入计划；如果老师评分表另有口径，再在阶段 0 调整优先级。

### 2.2 不推荐方案

不建议直接把现有项目从 `Spring Boot + MySQL` 改造成 `Node.js + MongoDB`。这会导致：

- 破坏已经完成的大量功能和数据模型。
- 让课程交付与原项目产品演进相互牵制。
- 引入一次大规模迁移，风险远超课程作业的合理范围。
- 很难证明哪些代码是课程新完成的，哪些是旧项目遗留的。

也不建议在当前 `backend/` 中再添加一套与现有 `RagService`、`KnowledgeService`、AI 客户端同名但实现不同的课程模块，这会形成重复模块和长期维护冲突。

## 3. 需求来源与课程要求梳理

### 3.1 课程总体目标

课程大纲定义的项目是“费曼学习平台”，核心用户流程为：

1. 用户注册、登录并获得 JWT。
2. 创建或阅读一个知识点。
3. 用户用自己的话对知识点进行录音复述。
4. 系统把录音上传并转成文字。
5. AI 对复述文本进行润色、评价和打分。
6. 系统根据评价或测评结果把薄弱知识点纳入复习范围。
7. AI 根据知识点生成题目，用户答题并获得反馈。
8. 用户可以向私有知识库提问，系统通过 RAG 返回回答。
9. 知识点及其关系可以用二维或三维方式展示。
10. Web 应用可以封装成桌面端，并具备基础 PWA 能力。

### 3.2 课程技术基线

课程材料反复出现的目标技术如下：

| 领域 | 课程基线 |
|---|---|
| 后端 | Node.js、Express、RESTful API、路由、中间件、错误处理 |
| 主数据库 | MongoDB / MongoDB Atlas、Mongoose、Schema |
| 认证 | JWT、Bearer Token、密码哈希（`bcryptjs`） |
| 前端 | React、Vite、React Router、Axios、Context API；大纲允许 Vue，但课件以 React 为主 |
| 内容 | Markdown、GFM、LaTeX、Mermaid；课件还演示了 ReactQuill |
| 语音 | `MediaRecorder`、`FormData`、`multer`、百度语音识别 |
| 大模型 | 百度千帆/文心类 API，用于润色、评价、出题和评分 |
| RAG | 文本分块、Embedding、LangChain.js、HNSWLib/FAISS、本地向量存储、检索增强生成 |
| 图谱 | ECharts 或 G6 的二维关系图 |
| 3D | Three.js、`d3-force-3d`、Raycaster；第 14 课正文实际是 Three.js 进阶 |
| 跨平台 | Electron、`electron-builder`、PWA、Manifest、Service Worker |
| 部署 | Docker、Nginx、云服务器；第 16 课更偏理论和展示 |

### 3.3 必须完成、挑战项与不确定项

课程大纲将 16 次课内容全部列为项目实践，但各课件又将部分能力标记为“挑战”“选做”或“理论掌握”。结合当前确认，暂按下面的优先级理解：

> 说明：当前尚未收到老师单独的原始任务书或评分表正文。以下基线以 `docs/feature-new` 下 17 份课程材料为依据；收到正式评分材料后，应优先按评分表修订。

#### P0：最终项目应具备的核心闭环

- Node.js + Express 服务可以启动。
- MongoDB 数据可以实际读写。
- 注册、登录、JWT 保护路由可用。
- 知识点 CRUD 可用。
- 前端可以登录、查看和编辑知识点。
- 至少完成“录音 → 上传 → 转文字 → 页面展示”。
- 至少完成“知识点/复述文本 → AI 评价 → 分数和反馈展示”。
- 至少完成“选择难度 → AI 出单选题 → 答题 → 评分 → 薄弱知识点进入复习”的闭环。
- 课程版独立实现的 RAG 问答可以演示，并能说明上下文来源或知识库边界。
- 二维知识图谱可以展示节点、关系和点击交互。
- Three.js 3D 知识宇宙至少能完成基础场景和知识点节点展示。
- CesiumJS 数字地球或老师指定的 CesiumJS 交互场景可以运行。
- Electron 可以生成当前目标平台的可执行安装包。
- PWA 可以安装，并能在断网时打开缓存的基础页面。
- 课程版可以部署到真实云服务器，并通过域名或公网地址访问。
- 课程版前端至少完成一次可运行的生产构建。
- 提供源代码、演示视频和项目报告，并准备 8—10 分钟展示。

#### P1：在 P0 完成后继续增强的项目

- 简答题和 AI 评分 API。
- RAG 引用来源展示。
- RAG 流式输出。
- 复述记录持久化、历史记录和趋势统计。
- 知识图谱按掌握状态/复习状态着色。
- Three.js 3D 知识宇宙、力导向布局、Raycaster 高亮。
- Electron 原生菜单和更完整的桌面端体验。
- PWA 离线缓存优化。

#### 已确认但仍需评分表约束的实现细节

- React 已确定为课程版前端框架。
- RAG 已确定为课程版独立实现，拟采用 LangChain.js + HNSWLib/其他经确认的向量存储。
- CesiumJS、Electron 安装包和真实云部署已暂按需要纳入 P0；具体验收深度仍需以评分表为准。
- 第 14 课标题写的是“CesiumJS”，但正文、目标、作业全部是 Three.js 进阶；需要向老师确认是“补充 CesiumJS”还是以正文为准。
- 百度千帆是否为唯一 AI Provider、云服务器厂商、域名和部署平台仍需确认。

## 4. 当前仓库状态

### 4.1 当前项目实际技术架构

当前仓库不是课件中的 Node.js 项目，主要结构如下：

```text
客户端：Vue 3 + Vite + Element Plus + Pinia
    │
    ├── Web 前端：frontend/
    ├── 移动端：mobile/（uni-app，可产出 App/H5/小程序）
    └── 浏览器扩展：extension/
    │
后端：Spring Boot 3 + Java + MyBatis-Plus
    │
    ├── MySQL：业务主数据
    ├── Redis：缓存和状态
    ├── Elasticsearch：搜索/向量相关能力
    ├── Kafka、Quartz、WebSocket：异步、调度和实时能力
    └── DeerFlow：独立 Python AI Research 服务
```

证据包括：根目录 `pom.xml` 使用 Spring Boot 和 MyBatis-Plus；`application.yml` 配置了 MySQL、Redis、Kafka、Elasticsearch；`frontend/package.json` 使用 Vue、Pinia、Element Plus；前端路由和页面也是 Vue 组件。

### 4.2 当前已经具备的能力

| 课程能力 | 当前状态 | 现有实现/证据 | 结论 |
|---|---|---|---|
| 用户注册、登录、JWT | 已有 | `AuthController`、`AuthService`、`JwtUtil`、前端用户 Store | 可复用业务经验，不能直接满足 Node/Mongoose 架构要求 |
| 知识点 CRUD | 已有 | `KnowledgeController`、`KnowledgeService`、`KnowledgeNode`、Vue 知识管理页面 | 业务模型和交互可参考 |
| Markdown 内容 | 已有 | `marked`、`DOMPurify`、知识详情/编辑页面 | 可参考安全渲染策略；课件中的 React 插件需另行适配 |
| 复习机制 | 已有且更复杂 | `ReviewCardController`、`ReviewCard`、复习页面、复习调度 | 课程版只需保留简单的 need-review 语义 |
| AI 客户端/AI 配置 | 已有 | `AiService`、Provider/Model/User AI Config 相关服务 | 不应重复创建一套通用 AI Client |
| AI 出题 | 已有较强能力 | `QuestionGenerationService`、复习题卡逻辑 | 可转化为课程展示能力，但 API/数据格式不同 |
| RAG | 已有且更完整 | `RagController`、`RagService`、Embedding、向量搜索、SSE 流式问答 | 主项目保持不变；课程版按已确认决策独立实现 |
| 知识图谱 | 已有 | `KnowledgeGraphController`、`KnowledgeGraphService`、前端 `KnowledgeGraph.vue`、ECharts | 二维图谱能力基本具备 |
| Docker/Nginx | 已有 | `docker-compose.yml`、根目录 Dockerfile、前端 Dockerfile、Nginx 配置 | 可作为部署经验，但不是课程版架构证明 |
| 移动端 | 已有另一种形态 | `mobile/` 是 uni-app，可产出 App/H5/小程序 | 不能直接等同于课程要求的 PWA |
| Node.js/Express/MongoDB | 当前主项目没有 | 未发现课程版 Node 服务、Mongoose Schema 或 MongoDB 配置 | 在课程版中独立实现 |
| React/React Router/Context | 当前 Web 前端没有 | 当前使用 Vue Router、Pinia | 课程版已确定使用 React |
| 浏览器录音与百度 ASR | 未发现课程闭环 | 未发现 `MediaRecorder`、上传音频和转录接口 | 需要新增课程能力或接入适配器 |
| Electron/PWA | 当前 Web 前端没有 | 未发现 Electron 主进程、Service Worker、Vite PWA 配置 | 课程版增加，并纳入交付验证 |

### 4.3 关键差异

最重要的差异不是某几个接口名称，而是架构边界：

| 维度 | 当前 AI-SecondBrain | 课程版目标 |
|---|---|---|
| 后端语言 | Java | JavaScript/Node.js |
| Web 框架 | Spring Boot | Express |
| 数据访问 | MyBatis-Plus + Mapper | Mongoose + Schema/Model |
| 主数据库 | MySQL | MongoDB |
| 前端框架 | Vue 3 | 课件以 React 为主，大纲允许 Vue |
| 状态管理 | Pinia | 课件以 Context API 为主 |
| 向量/RAG | Elasticsearch + 既有 Embedding/RAG 服务 | 课件以 LangChain.js + HNSWLib/FAISS 为例 |
| 复习语义 | 复习卡、艾宾浩斯、计划、日志 | 课程样例中的 `reviewList`/简单复习标记 |
| 产品复杂度 | 多工作区、社区、研究 Agent、扩展、移动端 | 64 学时内可展示的教学项目 |

## 5. 课程版目标边界

### 5.1 课程版产品定位

课程版不是现有 AI-SecondBrain 的替代品，而是一个能够独立运行、能完整体现课程知识点的教学交付物：

> 用户维护自己的知识点，通过费曼复述得到 AI 反馈，再通过 AI 出题和 RAG 问答完成学习闭环，并用知识图谱观察知识之间的关系。

### 5.2 建议的最小数据模型

课程版 MongoDB 只保留与演示闭环直接相关的集合，避免照搬当前几十张 MySQL 表。

#### `users`

- `_id`
- `username`
- `email`（唯一索引）
- `passwordHash`
- `createdAt`
- `updatedAt`

#### `knowledge_points`

- `_id`
- `ownerId`
- `title`
- `summary`
- `contentMd`
- `status`：例如 `NEW`、`LEARNING`、`MASTERED`
- `needReview`
- `relatedKnowledgePointIds`（可选）
- `createdAt`
- `updatedAt`

#### `feynman_attempts`

- `_id`
- `userId`
- `knowledgePointId`
- `audioUrl` 或本地临时文件标识（可选）
- `transcript`
- `polishedText`
- `score`
- `evaluation`
- `strengths`
- `weaknesses`
- `createdAt`

原始音频不建议默认直接存 MongoDB。课程演示只需保存转录结果和评价结果，音频可以采用内存处理中转或本地受控目录；如果老师要求回放，再增加文件存储方案。

#### `quiz_attempts`

- `_id`
- `userId`
- `knowledgePointId`
- `difficulty`
- `questionType`
- `questionSnapshot`
- `userAnswer`
- `isCorrect`
- `explanation`
- `createdAt`

#### RAG 相关数据

RAG 数据的最终形态取决于“复用现有 RAG”还是“课程版独立 RAG”的决策。课程版不应在未确认前提前设计第二套完整向量数据模型。

### 5.3 建议的课程版 API 契约

以下是便于后续实现和验收的统一接口，不机械照搬课件中多次变化的路径：

| 模块 | 方法 | 路径 | 用途 |
|---|---|---|---|
| 健康检查 | GET | `/api/health` | 检查服务是否启动 |
| 认证 | POST | `/api/auth/register` | 注册 |
| 认证 | POST | `/api/auth/login` | 登录并返回 JWT |
| 认证 | GET | `/api/auth/me` | 获取当前用户 |
| 知识点 | GET | `/api/knowledge-points` | 列表 |
| 知识点 | GET | `/api/knowledge-points/:id` | 详情 |
| 知识点 | POST | `/api/knowledge-points` | 创建 |
| 知识点 | PUT | `/api/knowledge-points/:id` | 更新 |
| 知识点 | DELETE | `/api/knowledge-points/:id` | 删除 |
| 费曼复述 | POST | `/api/feynman/transcribe` | 上传音频并转录 |
| 费曼复述 | POST | `/api/feynman/evaluate` | AI 润色、评价、打分 |
| 费曼复述 | POST | `/api/feynman/attempts` | 保存复述结果 |
| AI 出题 | POST | `/api/quiz/generate` | 按知识点和难度出题 |
| AI 答题 | POST | `/api/quiz/grade` | 评分并更新复习状态 |
| RAG | POST | `/api/rag/ask` | 知识库问答 |
| 图谱 | GET | `/api/graph/knowledge-map` | 获取 nodes/links |

所有受保护接口统一使用 `Authorization: Bearer <token>`，不要同时混用课程课件中的 `x-auth-token` 和 Bearer Token。

## 6. 复用与重新实现策略

### 6.1 可以直接复用的内容

以下内容可以作为课程版的参考、素材或演示经验：

- 现有知识点的业务字段和知识管理交互。
- 现有 JWT 登录流程的安全原则。
- 当前 Markdown 清洗和预览的安全思路。
- 当前复习中心的“答题后更新复习状态”业务语义。
- 当前 ECharts 知识图谱的节点、边和交互经验。
- 当前 RAG 的检索、引用、SSE 流式输出经验。
- 当前 Docker、Nginx、环境变量和部署经验。
- 当前项目已有设计系统和页面视觉经验，但课程版需要根据课程演示目标重新收敛界面。

### 6.2 不应直接复制的内容

- 不把 Java Entity、Mapper、Service 直接搬到 Node 项目中。
- 不把 MySQL 表结构原样翻译成几十个 MongoDB 集合。
- 不复制当前工作区、社区、研究 Agent、后台管理等与课程闭环无关的模块。
- 不在主项目中新增第二套 RAG、第二套通用 AI Client 或第二套知识图谱服务。
- 不让课程版程序直接读写现有用户的主知识库。
- 不把现有移动端 uni-app 当作已经完成了 PWA。

### 6.3 RAG 的特殊处理

现有仓库已经拥有 RAG，但课程版已确定独立实现。两套 RAG 必须保持物理和数据边界隔离：

1. 课程版在 `course-project/server` 内独立完成文本分块、Embedding、向量保存、相似度检索、Prompt 组装和问答 API。
2. 课程版优先采用课件中的 LangChain.js + HNSWLib；如果原生依赖在目标环境无法稳定安装，再根据老师允许的范围选择替代向量存储。
3. 课程版 RAG 只读取课程版 MongoDB 中的知识点，不读取当前 AI-SecondBrain 的 MySQL、Elasticsearch 或用户知识库。
4. 当前主项目的 RAG 保持不变；课程版独立 RAG 是课程交付物，不回写主项目，也不与主项目共享索引文件。
5. 必须增加“无相关上下文时明确说明不知道”的验收用例，避免把模型自由生成误认为知识库问答。

## 7. 推荐架构

### 7.1 课程版独立架构

```text
课程版 Web / Electron / PWA
        │ HTTP/HTTPS + Bearer JWT
        ▼
Node.js + Express
  ├── auth middleware
  ├── knowledge routes/service
  ├── feynman routes/service
  ├── quiz routes/service
  ├── rag service/provider（课程版独立实现）
  └── graph service
        │
        ├── MongoDB Atlas：用户、知识点、复述记录、测评记录
        ├── Vector Store：课程版独立索引文件或独立向量服务
        └── AI Provider：ASR、LLM、Embedding
```

### 7.2 目录建议

为避免与当前 `backend/`、`frontend/` 混淆，课程版建议采用独立目录，例如：

```text
course-project/
  server/
    src/
      config/
      middleware/
      models/
      routes/
      controllers/
      services/
      providers/
      utils/
    tests/
    package.json
    .env.example
  web/
    src/
      api/
      components/
      contexts/
      pages/
      router/
      styles/
    package.json
  desktop/
    main.js
  docs/
    project-report.md
    demo-script.md
  docker-compose.yml
```

目录名称只是当前建议，待审核时可以改成用户希望的命名。关键原则是课程版与现有产品代码边界清晰、可独立启动、可独立展示。

## 8. 实施阶段与交付物

### 阶段 0：确认评分口径和技术决策

交付物：

- 老师原始任务书/评分表摘录（当前尚未提供，暂以 17 份课程材料为依据）。
- React 已确认。
- RAG 已确认由课程版独立实现。
- CesiumJS、Electron 安装包、真实云部署已暂按需要纳入范围。
- 课程版采用 `course-project/server`、`course-project/web`、`course-project/desktop` 目录。

未完成阶段 0，不开始大规模编码。

### 阶段 1：课程版骨架与 MongoDB

交付物：

- Node.js + Express 可启动。
- `/api/health` 可访问。
- `.env.example`、`.gitignore` 和 MongoDB 连接配置。
- 用户 Schema、知识点 Schema。
- 注册、登录、JWT 中间件。
- Postman 或 Bruno 测试集合。

验收：新环境只需安装依赖、配置环境变量即可完成注册和登录。

### 阶段 2：知识点学习模块

交付物：

- 知识点列表、详情、创建、编辑、删除页面。
- Markdown 安全渲染。
- 表格、公式、Mermaid 的最小可用示例。
- 前端请求封装和路由守卫。

验收：用户只能看到和修改自己的知识点，刷新后登录态仍然有效。

### 阶段 3：费曼复述闭环

交付物：

- 浏览器录音、停止、回放。
- `multipart/form-data` 上传。
- 音频格式、大小和超时错误处理。
- ASR Provider 接口及真实/模拟实现。
- 转录文本页面展示。

验收：至少可以稳定完成“录音 → 上传 → 转录 → 展示”；没有 API Key 时也能用模拟 Provider 做离线演示，不把密钥写入仓库。

### 阶段 4：AI 评价与智能测评

交付物：

- AI 润色、准确性/完整性/流畅性评价。
- 结构化 JSON 输出校验和异常兜底。
- 单选题出题、难度选择、答题和解释。
- 错题或低分知识点进入复习范围。
- `QuizAttempt` 答题历史记录，以及按用户和知识点隔离的查询接口。

当前状态：已完成第一版。课程版使用 Mock Provider 保证课堂演示稳定；题目接口不把正确答案索引返回给前端，判分时由服务端根据知识点快照重新计算，并将结果写入 MongoDB Atlas。

验收：已验证生成 4 个选项、错误答案返回 0 分、正确答案返回 100 分、解析正常返回、答题记录可查询；AI Provider 替换时保持前端接口不变。

### 阶段 5：RAG 问答

按已确认决策，课程版独立完成：

- 知识点内容索引、文本分块和 Embedding。
- 课程版独立向量存储及索引生命周期管理。
- 相似度检索、Prompt 组装和问答 API。
- RAG 问答页面、上下文来源展示和知识库边界提示。

验收：

- 能回答知识库内问题。
- 能处理跨知识点问题。
- 对知识库外问题明确说明没有相关资料。
- 能显示来源或至少记录检索片段。

### 阶段 6：图谱和 3D 展示

交付物：

- 二维知识图谱：节点、边、拖拽/缩放、点击详情。
- Three.js 3D 基础场景、知识点节点、连线和基础交互。
- CesiumJS 数字地球场景，以及知识点实体/标签的展示方式。
- `d3-force-3d`、Raycaster 高亮、信息浮层作为 3D 交互增强项。

注意：第 14 课标题写的是“CesiumJS”，但正文实际讲授 Three.js 进阶。当前先按“Three.js + CesiumJS 均纳入课程版”规划；若老师最终只要求其中一个，再缩减实现范围。

### 阶段 7：Electron、PWA 和容器化

交付物：

- Electron 开发模式可运行。
- 生成当前目标平台的 Electron 可执行安装包，并保留构建说明。
- PWA Manifest、Service Worker、基础页面缓存。
- 前后端 Dockerfile 和最小 `docker-compose.yml`。
- 云服务器部署、域名或公网访问配置、HTTPS/反向代理说明。
- 课程版启动和回滚说明。

验收：

- Web 生产构建成功。
- Electron 能打开前端。
- PWA 能安装并在断网时打开基础壳页面；AI、登录和数据库操作不承诺离线可用。
- 容器启动后健康检查可访问。

### 阶段 8：测试、报告与展示

交付物：

- API 测试记录。
- 关键流程测试记录。
- 项目报告：背景、需求、架构、数据库、关键技术、问题与改进。
- 演示视频。
- 8—10 分钟展示稿和备用录屏。

推荐展示顺序：登录 → 创建知识点 → 录音转文字 → AI 评价 → AI 出题/复习 → RAG 问答 → 图谱/3D 亮点 → 架构与复盘。

## 9. 测试与验收基线

### 9.1 后端

- 未登录访问受保护接口返回 401。
- 用户 A 不能读取、修改或删除用户 B 的知识点。
- 邮箱和用户名唯一性有明确错误响应。
- 密码只保存哈希，不返回密码字段。
- 无效 JSON、缺少字段、非法 ID 有 4xx 响应。
- 第三方 AI 超时和格式错误有可追踪日志和稳定响应。
- 关键服务不使用无限重试或无限 Agent Loop。

### 9.2 前端

- 登录、退出、刷新、路由守卫流程正常。
- 列表空状态、加载状态、错误状态完整。
- 录音权限被拒绝时有清晰提示。
- AI 请求进行中不能重复提交。
- 智能测验选项支持键盘焦点、提交前选择校验、判分反馈和解析展示。
- Markdown/HTML 内容经过安全处理。
- RAG 回答和来源区域能区分“模型回答”和“检索依据”。
- 移动窄屏下核心页面可用。

### 9.3 交付和展示

- 新机器按 README 能启动。
- `.env`、API Key、数据库密码不提交。
- 录屏准备真实凭证不可用时的备用演示数据。
- 现场展示不依赖不稳定的第三方接口才能完成主流程。
- 报告中明确哪些能力是课程版新增、哪些能力是已有项目复用或适配。

## 10. 主要风险与缓解方案

| 风险 | 等级 | 缓解方案 |
|---|---|---|
| 老师有未提供的硬性评分表 | 高 | 阶段 0 先获取并锁定评分口径；报告按评分项组织 |
| 直接迁移现有项目导致范围失控 | 高 | 采用独立课程版；主项目只读参考/受控适配 |
| 课程版独立 RAG 工作量大或课程版数据与索引不一致 | 高 | 只实现课程必需的最小索引链路；MongoDB 数据和向量索引均限定在课程版；主项目保持不动 |
| 课件接口路径、字段名和实现细节不一致 | 中 | 以统一 API 契约为准，不照抄课件中的临时代码 |
| 百度 ASR 对浏览器 WebM/WAV 格式有要求 | 高 | 在 Provider 层做格式检测/转码预案，先用模拟 Provider 验证页面闭环 |
| HNSWLib 原生依赖安装失败 | 中 | 预留现有 RAG 适配模式或纯 JavaScript 最小向量存储作为备选 |
| AI API Key、额度或网络不稳定 | 高 | 所有 AI Provider 可替换；增加 mock 数据和录屏备用路径 |
| React 与现有 Vue 并存增加维护成本 | 中 | 课程版独立目录，不修改现有 Web 构建链 |
| “CesiumJS”标题与正文不一致 | 中 | 当前按 Three.js + CesiumJS 双范围规划；收到评分表后按老师实际要求调整 |
| PWA 离线被误解为 AI 全离线 | 中 | 报告明确只缓存应用壳和静态资源，在线 AI 功能需要网络 |
| Electron 打包耗时或跨平台限制 | 中 | 先完成当前操作系统安装包，其他平台保留构建说明；现场演示准备录屏 |
| 真实云部署受云主机、域名、端口或 HTTPS 条件限制 | 高 | 阶段 1 即确定部署平台；先完成最小公网环境，再逐步接入 AI 和静态资源 |

## 11. 已确认决策与剩余输入

### 11.1 已确认决策

| 决策项 | 当前结论 | 对实施的影响 |
|---|---|---|
| 课程版前端 | React + Vite | 不改造现有 Vue 前端，课程版独立建立 React 工程 |
| RAG 方案 | 课程版独立实现 | 使用课程版 MongoDB 数据建立独立索引，不调用主项目 RAG，不共享主项目索引 |
| CesiumJS | 纳入范围 | 除 Three.js 3D 知识宇宙外，补充 CesiumJS 场景；具体展示内容按评分表收敛 |
| Electron | 需要安装包 | 至少生成当前目标操作系统安装包，并准备构建记录 |
| 云部署 | 暂按需要 | 纳入真实云服务器、公网访问、反向代理和 HTTPS 的实施计划 |
| 课程版目录 | 采用建议方案 | 使用 `course-project/server`、`course-project/web`、`course-project/desktop` |

### 11.2 仍需补充的外部材料

- 老师原始任务书、评分表或提交要求正文。目前用户只确认了技术决策，尚未提供该材料。
- 云服务器厂商、域名、是否已有账号以及可用的部署预算。
- 百度千帆、语音识别、Embedding 等服务的账号和额度。
- 第 14 课是要求完整 CesiumJS 数字地球，还是只需要一个 CesiumJS 技术展示页。
- Electron 安装包的目标操作系统（当前建议先以 Windows 为第一目标）。

## 12. 最终建议

当前已确认可以进入课程版正式设计，但仍不建议迁移现有数据库、改造现有 Java 后端或向主项目添加课程版重复模块。老师原始任务书/评分表和云部署信息补齐后，再锁定最终验收范围。

最稳妥的执行顺序是：

```text
补充任务书/评分表与部署信息
  → 锁定课程版边界
  → 建立 Node/Express/Mongo 基础
  → 完成知识点和费曼核心闭环
  → 独立实现 AI 与 RAG
  → 补齐图谱、Three.js、CesiumJS、Electron、PWA
  → 完成真实云部署
  → 测试、报告、录屏和展示
```

这样既能满足老师对新架构和课程知识点的考察，又能保留现有 AI-SecondBrain 的开发成果，不把一次课程作业变成对成熟项目的整体重构。
