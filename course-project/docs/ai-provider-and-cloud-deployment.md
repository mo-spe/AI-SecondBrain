# 真实 AI Provider 与云部署运行手册

## 已完成的代码能力

课程版服务端新增了 OpenAI-compatible Provider 适配层，统一调用：

```text
POST {AI_BASE_URL}/chat/completions
```

已接入的业务：

- 费曼复述评价：返回分数、润色文本、优点和改进建议。
- 智能测验：返回四选一题目；正确答案只保存在服务端的临时题目记录中。
- RAG 回答：先由课程版本地检索器找上下文，再让 Provider 在来源约束下组织回答。

未配置真实 Provider 时，上述功能继续使用 Mock 实现。这样不会因为额度、网络或课堂现场环境导致主流程不可演示。

## 本地启用真实 Provider

编辑 `server/.env`，不要把真实值提交到 Git：

```ini
AI_PROVIDER=openai-compatible
AI_BASE_URL=https://api.openai.com/v1
AI_API_KEY=your-secret
AI_CHAT_MODEL=gpt-4o-mini
AI_TIMEOUT_MS=30000
```

如果使用其他兼容服务，只需替换 `AI_BASE_URL`、`AI_API_KEY` 和 `AI_CHAT_MODEL`。重启服务后访问 `/api/health`，应看到 AI `configured: true`。浏览器页面上的回答、复述评价和测验响应也会返回实际 Provider 名称。Provider 请求失败时服务端返回 502 和可读错误，不会回显 API Key。

## Render 单服务部署

仓库已提供：

- `course-project/Dockerfile`：先构建 React，再由 Node 同源托管页面和 API。
- `course-project/render.yaml`：Render Blueprint 配置。
- `course-project/.dockerignore`：避免把本地依赖、构建产物和环境文件放进构建上下文。

部署流程：

1. 将仓库推送到 GitHub/GitLab，并在 Render 创建 Blueprint，选择仓库中的 `course-project/render.yaml`。
2. Render 构建并启动服务后，先访问公网地址的 `/api/health`。
3. 在 Render Environment 中填写 `MONGODB_URI`，保留数据库名 `feynman_course`；`JWT_SECRET` 使用平台生成值。
4. 要启用真实 AI，设置 `AI_PROVIDER=openai-compatible`，填写 `AI_BASE_URL`、`AI_API_KEY` 和 `AI_CHAT_MODEL`。
5. 打开公网首页，注册一个新账号，创建知识点，然后依次测试费曼评价、智能测验和知识问答。

## 外部权限与安全边界

真正发布前还需要用户侧完成两项配置：

- Render 项目创建/部署权限，以及 AI Provider 的 API Key。
- MongoDB Atlas Network Access 允许 Render 服务访问。Render 免费服务通常没有固定出口 IP，需要根据 Atlas 安全策略选择允许范围；更安全的长期方案是使用固定出口 IP 或私网连接能力。

这些配置不需要把密码或 API Key 发给开发者；直接填在 Render 的 Secret Environment Variables 中即可。数据库用户建议只拥有 `feynman_course` 的读写权限，不应继续使用 `atlasAdmin`。

## 本地容器验证

Docker Desktop 引擎启动后，在 `course-project` 目录执行：

```bash
docker build -t feynman-course .
docker run --rm -p 4000:4000 --env-file server/.env feynman-course
```

然后访问 `http://localhost:4000`。本次已在本机完成镜像构建、容器启动、同源首页、Atlas 健康检查和真实 DeepSeek 出题联调；如果本地端口 4000 正被开发服务占用，可以改用 `-p 4001:4000`。
