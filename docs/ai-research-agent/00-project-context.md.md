# AI Research Agent 项目上下文

## 一、项目背景

当前项目是 AI-SecondBrain，一个基于个人知识库的 AI 第二大脑系统。

项目目标不是简单实现一个 AI Chat，而是构建一个能够理解用户个人知识体系，并基于用户已有知识进行学习、研究、分析和知识沉淀的个人 AI 第二大脑。

当前系统已经具备以下能力：

1. 用户认证
2. 用户管理
3. Chat 对话
4. Markdown 笔记
5. 知识管理
6. Elasticsearch 关键词检索
7. 语义向量检索
8. RAG 问答
9. 知识体系
10. 知识图谱
11. 复习中心
12. 遗忘曲线
13. AI 学习路径规划
14. AI 知识盲区分析
15. AI 学习报告生成
16. DeepFlow Agent 集成或相关能力

当前新增核心目标：

构建「基于个人知识库的自主 AI Research Agent」。

---

# 二、核心产品定位

AI Research Agent 不是普通 AI Chat。

它的核心定位是：

> 一个基于用户个人知识库，能够理解用户已有知识背景、发现知识盲区、拆解研究问题、制定研究计划、检索外部信息、分析和验证信息、形成研究结论，并将研究成果沉淀回个人知识库的 AI 研究智能体。

核心闭环：

用户知识库
    ↓
理解用户已有知识
    ↓
分析知识缺口
    ↓
生成研究目标
    ↓
拆分研究问题
    ↓
制定研究计划
    ↓
检索个人知识库
    ↓
检索外部信息
    ↓
分析与推理
    ↓
信息验证
    ↓
生成研究结论
    ↓
生成研究报告
    ↓
提取新知识
    ↓
更新知识库
    ↓
形成新的知识结构
    ↓
发现新的知识盲区
    ↓
进入下一轮研究

最终形成：

Knowledge → Research → Discovery → Validation → Knowledge

即：

知识 → 研究 → 发现 → 验证 → 知识沉淀

---

# 三、核心产品理念

系统需要实现 Personalized Research。

普通 AI：

用户：
Spring AI 是什么？

AI：
Spring AI 是一个 AI 应用开发框架。

AI Research Agent：

用户：
帮我研究 Spring AI Agent。

Agent：

1. 分析用户个人知识库
2. 发现用户已经掌握 Spring Boot
3. 发现用户已经掌握 RAG
4. 发现用户部分掌握向量数据库
5. 发现用户尚未掌握 Tool Calling
6. 发现用户尚未掌握 MCP
7. 发现用户尚未掌握 Agent Memory

因此 Agent 不应该从 Spring AI 基础开始介绍，而应该针对用户当前知识水平设计研究路径。

系统必须做到：

「研究内容与用户已有知识相关联」。

---

# 四、核心模块

AI Research Agent 包含以下核心模块：

## 1. Research Workspace

研究工作空间。

用户可以创建一个 Research Project。

例如：

Spring AI Agent 架构研究

研究目标：

我要研究如何使用 Spring AI 构建生产级 Agent。

Research Workspace 包含：

- 研究目标
- 用户知识背景
- 知识缺口
- 研究问题
- 研究计划
- 外部资料
- 研究过程
- 实验验证
- 研究结论
- 研究报告
- 新知识
- 研究历史

---

## 2. Knowledge Agent

负责分析用户个人知识库。

核心任务：

- 查询用户已有知识
- 识别用户已掌握知识
- 识别部分掌握知识
- 识别未知知识
- 找到相关知识点
- 找到相关笔记
- 找到历史研究
- 找到知识图谱关联关系

Knowledge Agent 必须优先使用个人知识库。

---

## 3. Knowledge Gap Agent

负责发现知识盲区。

分为：

### 显性知识缺口

用户完全没有相关知识。

### 隐性知识缺口

用户掌握了某个领域，但是缺少关键子领域。

例如：

用户已经掌握 RAG：

RAG
├── Embedding
├── Vector Database
├── Retrieval
├── Context
└── LLM

但是没有：

- RAG Evaluation
- Recall
- Precision
- Faithfulness
- Answer Relevancy

Agent 应该判断：

用户可能存在 RAG Evaluation 相关知识盲区。

---

## 4. Planner Agent

负责：

- 理解研究目标
- 分析用户知识背景
- 生成研究任务
- 拆分研究问题
- 生成研究计划
- 决定研究顺序

输出结构化 Research Plan。

---

## 5. Research Agent

负责：

- 搜索外部资料
- 获取官方文档
- 获取论文
- 获取 GitHub 项目
- 获取技术文章
- 对外部资料进行摘要
- 对资料进行分类
- 对资料进行关联

Research Agent 需要记录来源。

所有研究结论必须尽可能保留 Source。

---

## 6. Critic Agent

负责验证研究结论。

需要判断：

- 信息是否有来源
- 来源是否可靠
- 多个来源是否一致
- 是否存在冲突观点
- 结论是否属于推测
- 是否存在证据不足

输出：

- 可信度
- 证据
- 支持来源
- 冲突来源
- 风险提示

---

## 7. Synthesizer Agent

负责将研究过程转换为：

- 研究结论
- 研究摘要
- 技术分析
- 对比分析
- 学习建议
- 实践建议
- 研究报告

---

## 8. Knowledge Writer Agent

负责将研究结果沉淀回知识库。

可以生成：

- Knowledge Point
- Markdown Note
- Knowledge Graph Entity
- Knowledge Graph Relation

但必须：

默认「建议写入」，不能直接覆盖用户原始知识。

应该：

Agent 生成新知识
    ↓
用户确认
    ↓
写入知识库

---

# 五、Research Workflow

一次完整研究流程：

用户：

研究 Spring AI Agent。

系统：

Step 1：
理解研究目标。

Step 2：
读取用户知识库。

Step 3：
构建 Knowledge Context。

Step 4：
分析 Knowledge Gap。

Step 5：
生成 Research Plan。

Step 6：
拆分 Research Tasks。

Step 7：
执行知识库检索。

Step 8：
执行外部信息检索。

Step 9：
分析资料。

Step 10：
验证关键结论。

Step 11：
发现冲突。

Step 12：
生成研究结论。

Step 13：
生成研究报告。

Step 14：
提取新知识。

Step 15：
生成知识沉淀建议。

Step 16：
用户确认。

Step 17：
写入知识库。

---

# 六、Agent Workflow

推荐架构：

Planner
    ↓
Knowledge Agent
    ↓
Gap Agent
    ↓
Research Agent
    ↓
Critic Agent
    ↓
Synthesizer Agent
    ↓
Knowledge Writer Agent

但不要强制所有任务都执行全部 Agent。

应该根据任务动态决定。

简单任务：

User
 ↓
Knowledge Agent
 ↓
Research Agent
 ↓
Answer

复杂任务：

User
 ↓
Planner
 ↓
Knowledge Agent
 ↓
Gap Agent
 ↓
Research Agent
 ↓
Critic
 ↓
Synthesizer
 ↓
Knowledge Writer

---

# 七、Research Task

每一个研究项目可以拆分成多个 Task。

例如：

Research Project：

Spring AI Agent 架构研究

Tasks：

1. Spring AI Agent 基础
2. Tool Calling
3. Agent Memory
4. MCP
5. Multi-Agent
6. Spring AI 与 LangChain 对比

Task 状态：

PENDING
RUNNING
COMPLETED
FAILED
WAITING_USER

---

# 八、Research Project 状态

DRAFT
PLANNING
RESEARCHING
REVIEWING
SYNTHESIZING
COMPLETED
ARCHIVED

---

# 九、研究结果

Research Result 应包含：

- 标题
- 摘要
- 研究问题
- 研究结论
- 关键发现
- 用户已有知识
- 用户知识缺口
- 外部来源
- 证据
- 可信度
- 冲突观点
- 推荐学习路径
- 推荐实践
- 新知识建议

---

# 十、核心原则

1. 优先使用用户个人知识库。
2. 不要重复用户已经掌握的内容。
3. 研究必须结合用户知识背景。
4. 研究结论必须尽可能有来源。
5. 不能把推测当成事实。
6. 研究过程必须可追踪。
7. Agent 每一步必须可观察。
8. 长任务必须支持中断和恢复。
9. Agent 失败必须支持重试。
10. 外部搜索失败不能导致整个任务直接失败。
11. 所有 Agent 输出尽可能结构化。
12. 不允许 Agent 随意修改用户知识库。
13. 知识写入必须支持用户确认。
14. 研究结果必须可以回溯到来源。
15. 尽量复用已有系统能力，不重复实现已有 RAG、知识搜索、知识图谱等功能。

---

# 十一、技术原则

当前项目技术栈：

Backend：

- Java 17
- Spring Boot 3.1.5
- Spring AI 或现有 AI Agent 能力
- MySQL 8.0
- Redis 7
- Elasticsearch 8.11
- Kafka 3.6

Frontend：

- Vue 3
- Element Plus

AI：

- Qwen
- DeepSeek
- OpenAI
- DeerFlow

知识库：

- Elasticsearch
- Vector Search
- RAG

必须优先复用现有项目架构。

不要在不了解现有代码结构的情况下重新设计整个项目。

不要重复创建已有的：

- 用户模块
- 知识模块
- RAG 模块
- Elasticsearch 模块
- AI 调用模块
- 知识图谱模块

新增 AI Research Agent 应作为已有系统上的扩展模块。