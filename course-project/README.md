# 费曼学习平台（课程版）

课程版是独立于 AI-SecondBrain 主项目的 React + Node.js + MongoDB 学习平台，用于完成课程要求和最终展示。

## 目录

```text
course-project/
├─ server/      Node.js + Express + MongoDB API
├─ web/         React + Vite Web/PWA 前端
└─ desktop/     Electron 桌面端壳
```

## 当前进度

- 已完成课程版基础工程边界。
- 已完成注册、登录、JWT 登录态保持。
- 已完成知识点的列表、创建、编辑和删除 API 基础。
- 已完成仪表盘基础数据展示。
- 已完成费曼复述第一版：浏览器录音、音频上传、Mock 转录、结构化评价和复述记录保存。
- 已完成课程版独立 RAG 第一版：知识内容分块、本地向量化、相似度检索、来源展示和无相关内容兜底。
- 已完成智能测验第一版：按知识点和难度生成单选题、提交判分、展示正确答案与解析，并将答题记录保存到 MongoDB Atlas。
- 已接入可配置的真实 AI Provider：兼容 OpenAI Chat Completions 协议，可用于费曼评价、智能出题和有来源 RAG 回答；未配置密钥时自动保持 Mock 演示模式。
- 已增加单容器云部署方案：Node API 同源托管 React 构建产物，提供根目录 Dockerfile 和 Render Blueprint。
- 已使用真实 DeepSeek Provider 完成本地进程和 Docker 容器双路径联调；单容器首页、Atlas 连接、真实出题接口均验证通过。
- 已完成二维知识图谱第一版：按共享标签生成节点和关系，支持节点选择、详情查看和缩放。
- 已完成 Three.js 3D 知识宇宙第一版：复用二维图谱数据，支持轨道旋转、滚轮缩放、节点点击和掌握度视觉编码。
- 已接入 CesiumJS 数字地球页面：复用知识图谱节点和关系，提供无 Ion Token 的课程展示模式。
- 已增加当前账号的演示数据初始化接口，按标题去重，不覆盖已有知识点。
- 已补齐 Electron 本地连接适配，打包版通过 `http://localhost:4000/api` 访问课程 API。
- 已生成 Windows x64 Electron NSIS 安装包；当前为未签名构建，适合课程演示和本地安装。
- 真实 AI Provider 和云部署将在后续阶段接入。

## 启动

### API

```bash
cd server
copy .env.example .env
# 编辑 .env，填入 MongoDB Atlas 连接串、数据库用户和密码
npm install
npm run dev
```

### Web

```bash
cd web
npm install
npm run dev
```

默认 API 地址为 `http://localhost:4000/api`，前端开发地址为 `http://localhost:5173`。

当前可演示的核心路径：登录 → 创建知识点 → 费曼复述 → 上传转录 → 结构化评价 → 保存记录 → 智能测验 → 知识问答。RAG 当前使用课程版独立的本地向量检索器，不读取主项目的 MySQL、Elasticsearch 或 RAG 索引；出题和评价当前使用可替换的 Mock Provider，后续可在保持接口不变的前提下替换为真实 Embedding、LLM 和题目 Provider。

主要 API：

- `POST /api/feynman/transcribe`：上传音频并获取转录文本（当前为 Mock Provider）。
- `POST /api/feynman/evaluate`：对复述文本进行结构化评价。
- `POST /api/feynman/attempts`：保存复述记录并更新知识点掌握度。
- `POST /api/rag/ask`：检索课程版知识库并返回回答与来源。
- `POST /api/quiz/generate`：按知识点和难度生成单选题（当前为 Mock Provider，不返回正确答案索引）。
- `POST /api/quiz/grade`：提交答案、返回判分与解析，并保存答题记录。
- `GET /api/quiz/attempts/:knowledgeId`：查询当前用户某个知识点最近的答题记录。
- `GET /api/health`：返回服务状态及 AI Provider 是否已配置，不返回密钥。
- `GET /api/graph/knowledge-map`：返回当前用户的知识节点、共享标签关系和统计信息。
- `POST /api/demo/seed`：为当前登录账号加入课程演示数据，重复调用会跳过已有标题。

Three.js 页面地址为 `http://localhost:5173/universe`。3D 场景仅使用图谱接口返回的数据，节点关系不会在前端单独维护。

CesiumJS 页面地址为 `http://localhost:5173/earth`。页面使用椭球地球和本地实体，不要求 Cesium Ion Token；真实底图和地形服务可以在部署阶段按需要配置。

Electron 开发/打包前需要先构建 Web：

```bash
cd desktop
npm install
npm run dev
npm run dist
```

打包后的桌面端默认请求本机的 `http://localhost:4000/api`，因此本地演示时需要同时启动 `server`；云部署完成后，可在 Web 构建时设置 `VITE_API_BASE_URL` 指向公网 API。

当前安装包：`desktop/dist/费曼学习平台 Setup 0.1.0.exe`。Windows 可能因为安装包未配置代码签名而显示安全提示，这是课程版当前的预期状态。

课程版使用 MongoDB Atlas 云数据库。启动前需要在 Atlas 中创建 Database User，并在 Network Access 中允许当前开发环境访问；数据库名使用 `feynman_course`。连接串只保存在本地 `server/.env`，不要提交到 Git。

使用 Docker 启动完整课程版时：

```bash
copy server\\.env.example server\\.env
# 编辑 server\\.env 后执行
docker compose up --build -d
```

Docker Compose 只启动课程版 Web 和 API，不启动本地 MongoDB 容器。

课程版只连接自己的 MongoDB Atlas 数据库，不连接主项目 MySQL、Elasticsearch 或主项目 RAG 索引。

更完整的真实 AI Provider、Render 部署、Atlas 网络权限和 Docker 说明见 [云部署运行手册](docs/ai-provider-and-cloud-deployment.md)。

## 真实 AI Provider

服务端使用 OpenAI-compatible 的 `POST {AI_BASE_URL}/chat/completions` 接口。可以在 `server/.env` 中配置：

```ini
AI_PROVIDER=openai-compatible
AI_BASE_URL=https://api.openai.com/v1
AI_API_KEY=只保存在本机或云平台密钥管理中的值
AI_CHAT_MODEL=gpt-4o-mini
AI_TIMEOUT_MS=30000
```

`AI_PROVIDER=mock` 或没有 `AI_API_KEY` 时，费曼评价、测验和有检索上下文的 RAG 会使用稳定的课程演示实现。配置真实 Provider 后，接口返回中的 `provider` 会显示实际 Provider 名称；真实 Provider 请求失败会返回可理解的 502 错误，不会把 API Key 写入日志或响应。

## 云部署

课程版已准备 Render 单服务部署配置：`render.yaml` 会构建根目录 `Dockerfile`，由 Node 服务同时提供 React 页面和 `/api` 接口。部署时在 Render 的 Environment 中填写 `MONGODB_URI` 和 `AI_API_KEY`，`JWT_SECRET` 可使用 Render 自动生成的值。

部署前还需要在 MongoDB Atlas 的 Network Access 中允许云服务访问。Render 免费服务通常没有固定出口 IP，实际部署时需要根据安全要求选择 Atlas 的临时开放范围，或使用具备固定出口 IP 的部署方案；不要把数据库密码提交到仓库。

本地验证单容器镜像：

```bash
docker build -t feynman-course .
docker run --rm -p 4000:4000 --env-file server/.env feynman-course
```

此时将通过 `http://localhost:4000` 同时访问页面和 API。真正上线还需要一个 Render 账号/项目，以及 MongoDB Atlas 对云服务出口的访问配置；这些属于外部账号权限，当前没有代替用户发布到云平台。
