# RAG 问答流式重构 — Product Requirements Document

> 关联需求池：#8
> 日期：2026-07-25
> 状态：可以做了

---

## 1. Executive Summary

### Problem Statement

当前 RAG 知识问答（`POST /rag/answer`）完全同步阻塞，用户提交问题后等待 20-40 秒才能看到完整回答，期间无任何反馈。同时，向量检索采用 Java 内存暴力扫描（加载全量向量 → for 循环算余弦相似度），知识节点越多越慢，且不支持多轮对话上下文。

### Proposed Solution

引入 **Spring AI** 统一 LLM 调用抽象层，使用 `StreamingChatClient` 通过 SSE（Server-Sent Events）将 AI 回答逐字推送到前端。用 **PGVector** 的 HNSW 索引替代内存暴力搜索。改造为对话模式，支持多轮追问。

### Success Criteria

| # | KPI | 目标 |
|---|-----|------|
| 1 | 首 token 时间 | 检索完成后 < 2s 看到第一个字 |
| 2 | 检索延迟（1000 节点） | < 200ms（PGVector HNSW） |
| 3 | 用户体验评分 | 不再有"页面卡住"的投诉 |
| 4 | 向后兼容 | 旧 `POST /rag/answer` 端点继续可用 |
| 5 | 多供应商流式 | Anthropic / OpenAI / Gemini 三家均支持 SSE |

---

## 2. User Experience & Functionality

### User Personas

- **学习者**：在个人空间或工作区中使用 RAG 知识问答，期望像 ChatGPT 一样的逐字输出体验
- **工作区成员**：在工作区知识库中提问，需要引用来源和追问能力

### User Stories

- **US-1**: 作为学习者，我希望提问后能**逐字看到 AI 回答**，而不是盯着转圈等半分钟
- **US-2**: 作为学习者，我希望在第一次回答不满意时能**直接追问**，AI 能记住上下文
- **US-3**: 作为学习者，我希望看到回答时能**知道引用了哪些知识节点**
- **US-4**: 作为工作区成员，我希望 RAG 问答能**限定在工作区知识范围内**

### Acceptance Criteria

**US-1 流式输出**:
- 前端提交问题后，回答区域逐字/逐句渲染，不是一次性显示
- 用户在生成过程中可以看到已输出内容
- 支持手动中止生成
- 生成完成后显示完整回答 + 引用来源

**US-2 多轮对话**:
- 同一 session 内，上下文窗口保留最近 10 轮对话
- AI 能基于前文回答追问（如 "展开讲讲第二点"）
- 用户可以开启新对话、查看历史对话列表

**US-3 引用溯源**:
- 回答末尾列出引用的知识节点标题 + 相关度
- 点击引用可跳转到知识详情页

**US-4 工作区隔离**:
- 检索范围自动限定在当前工作区的知识节点
- 个人空间只检索个人知识节点

### Non-Goals

- **不做** Agent 多步推理研究（那是后续 Phase 3 的事）
- **不做** 知识盲区分析 / 学习路径规划改造（那是另一条线）
- **不做** 图片/多模态 RAG
- **不删除** 现有的 `POST /rag/answer` 同步端点（保留向后兼容，标记 `@Deprecated`）

---

## 3. AI System Requirements

### Streaming Strategy

Spring AI 的 `StreamingChatClient` 返回 `Flux<String>`，三家的流式协议不同但 Spring AI 已统一抽象：

| 供应商 | 流式协议 | Spring AI Starter |
|--------|---------|-------------------|
| OpenAI 兼容（含国内全部供应商） | SSE (`data: [DONE]`) | `spring-ai-openai-spring-boot-starter` |
| Anthropic | SSE (anthropic-version: 2023-06-01, `stream: true`) | `spring-ai-anthropic-spring-boot-starter` |
| Gemini | SSE (generateContentStream) | `spring-ai-vertex-ai-gemini-spring-boot-starter` |

> **国内供应商全覆盖**：DeepSeek、千问、Kimi、智谱、豆包、MiniMax 全部提供 OpenAI 兼容端点。
> 只需配置 `spring.ai.openai.base-url` 指向对应地址即可，**一个 Starter 覆盖 6 家国内 + OpenAI 本身**。
> 项目现有的 9 家供应商（3 国外 + 6 国内）全部支持流式输出。

### Prompt Template

```
你是一个专业的知识问答助手。请基于以下知识内容回答用户的问题。

【用户问题】
{question}

【相关知识】
{context}

【回答要求】
1. 只基于提供的知识内容回答，不要编造信息
2. 如果知识内容不足以回答问题，请明确说明
3. 回答要准确、清晰、有条理
4. 在引用知识时标注来源：[来源: 知识标题]
5. 回答末尾列出实际引用的知识来源
```

### Vector Search Enhancement

当前：Java 内存扫描，O(n) 复杂度，每次加载全量向量到内存

改造后：
- PGVector HNSW 索引，O(log n) 近似搜索
- SQL: `SELECT * FROM knowledge_embedding ORDER BY embedding <=> query_vector LIMIT topK`
- 索引选项：`index-type: HNSW, distance-type: COSINE_DISTANCE`

### Evaluation Strategy

| 测试维度 | 方法 | 通过标准 |
|---------|------|---------|
| 流式功能 | 手动测试 3 家供应商的 SSE 输出 | 全部正常逐字输出 |
| 检索质量 | 10 个预设问题，比较 PGVector vs 内存搜索 Top3 结果 | 重叠率 >= 80% |
| 检索性能 | 1000 节点压测 | PGVector < 200ms, 内存 < 2000ms |
| 兼容性 | 旧 `/rag/answer` 端点回归测试 | 全部通过 |

---

## 4. Technical Specifications

### Architecture Overview

```
Frontend (RagQA.vue 改造)
  │
  ├── POST /rag/answer/stream (SSE, 新增)
  │     → RagController.streamAnswer()
  │       → RagService.streamAnswer(userId, question, topK)
  │         1. VectorSearchService.searchSimilar() → PGVector HNSW
  │         2. Spring AI StreamingChatClient.stream(prompt)
  │            → Flux<String> → SSE via SseEmitter / Flux<ServerSentEvent>
  │
  ├── POST /rag/answer (同步, 保留兼容)
  │     → 行为不变, 内部改为复用 Spring AI ChatClient
  │
  └── 新增: CRUD /rag/sessions (对话管理)
        POST /rag/sessions → 创建新会话
        GET /rag/sessions → 会话列表
        GET /rag/sessions/{id}/messages → 消息历史
```

### Data Flow (Streaming)

```
User submits question
  → POST /rag/answer/stream { question, sessionId?, topK, workspaceId }
    → Controller creates SseEmitter (timeout: 5 min)
    → Async: VectorSearch (PGVector HNSW)
      → Build prompt with retrieved context + conversation history
      → Spring AI StreamingChatClient.stream()
        → onToken(token) → SseEmitter.send(token)
        → onComplete() → Save messages to DB → SseEmitter.complete()
        → onError() → SseEmitter.completeWithError()
    → Frontend: EventSource or fetch() with ReadableStream
      → Render tokens progressively
      → On stream end: display references
```

### Dependencies (Maven)

```xml
<!-- Spring AI BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0-M6</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- OpenAI 兼容（覆盖 Qwen/DeepSeek/Kimi/智谱/豆包） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Anthropic -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>

<!-- PGVector Store -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>
```

### Key Files

| 文件 | 操作 | 说明 |
|------|------|------|
| `pom.xml` | MODIFY | 添加 Spring AI 依赖 |
| `application.yml` | MODIFY | Spring AI + PGVector 配置 |
| `config/SpringAiConfig.java` | NEW | ChatClient / StreamingChatClient Bean 配置，根据用户 AI 配置动态切换服务商 |
| `config/PgVectorConfig.java` | NEW | PGVector Store 配置 |
| `service/RagService.java` | MODIFY | 新增 `streamAnswer()` 方法 |
| `service/impl/RagServiceImpl.java` | MODIFY | 流式实现 + 改为 Spring AI ChatClient |
| `service/impl/AiServiceImpl.java` | MODIFY | 新增流式 chat 方法，或逐步用 Spring AI 替换自建客户端 |
| `controller/RagController.java` | MODIFY | 新增流式端点 + 会话管理端点 |
| `entity/RagSession.java` | NEW | 对话会话实体 |
| `entity/RagMessage.java` | NEW | 对话消息实体 |
| `sql/V10__rag_streaming.sql` | NEW | RagSession + RagMessage DDL, PGVector 索引 DDL |
| `frontend/src/views/RagQA.vue` | MODIFY | 流式渲染 UI + 对话模式 |

### Security & Privacy

- API Key 继续使用现有的 `AiServiceImpl.resolveConfig()` 体系，不在前端暴露
- 流式端点使用 SSE（单向 server→client），敏感数据走后端的 LLM 调用，前端只接收 token 文本
- 会话消息存储时敏感信息脱敏

---

## 5. Risks & Roadmap

### Phased Rollout

**Phase 1: 流式 MVP（P0，必须做）**

| 任务 | 文件 |
|------|------|
| 引入 Spring AI 依赖 | `pom.xml` |
| Spring AI 配置（动态多供应商） | `config/SpringAiConfig.java` |
| `AiService` 新增流式方法 | `AiServiceImpl.java` |
| RAG `streamAnswer()` 实现 | `RagServiceImpl.java` |
| 新增 `/rag/answer/stream` SSE 端点 | `RagController.java` |
| 前端流式渲染 | `RagQA.vue` |

**Phase 2: 检索 + 对话升级（P1）**

| 任务 | 文件 |
|------|------|
| PGVector 集成 & HNSW 索引 | `config/PgVectorConfig.java`, `V10__rag_streaming.sql` |
| 替换内存暴力搜索 | `VectorSearchServiceImpl.java` |
| 引入 Spring AI PromptTemplate | `RagServiceImpl.java` |
| 对话管理（session + messages） | 新增 entity/mapper/controller |
| 前端对话模式 UI | `RagQA.vue` |

**Phase 3: Agent 能力（P2，后续迭代）**

- Function Calling 注册知识检索/复习数据工具
- Agent Loop: 检索 → 验证 → 补充检索 → 回答

### Technical Risks

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| Spring AI 版本不稳定（1.0.0-M6 是里程碑版） | 中 | 核心 LLM 调用保留一键回退到自建客户端的开关 |
| 动态多供应商切换（用户可能在设置页切换服务商） | 中 | SpringAiConfig 设计为请求级 ChatClient，每次调用时根据 resolveConfig 动态构建 |
| PGVector 需要 PostgreSQL 扩展 | 低 | 项目已用 PostgreSQL，只需 `CREATE EXTENSION vector` |
| SSE 连接超时/中断 | 低 | SseEmitter 设 5 分钟超时，前端实现断线重连提示 |

### 会话标题智能化（2026-09-03 已实现）

- 新会话仍以“新对话”作为短暂占位名，首轮 RAG 回答保存后由当前用户配置的聊天模型生成 8—20 字标题。
- 命名上下文只截取首轮问题和回答各 600 字，避免标题生成产生不必要的 token 消耗。
- 模型不可用、超时或返回空标题时，使用首个问题的精简摘要降级，问答结果不受命名失败影响。
- 自动命名仅处理空标题或“新对话”；写入前再次检查标题，避免覆盖用户并发执行的手动重命名。
- 历史会话列表提供重命名入口，调用 `PUT /sessions/{id}/title`，并校验会话访问范围、空标题和 100 字长度上限。

### Rollback Plan

在 `application.yml` 中保留开关：

```yaml
spring:
  ai:
    enabled: true  # 设为 false 回退到自建 AiService 客户端
```

回退后，`/rag/answer/stream` 端点返回 503 并提示用户使用旧端点。
