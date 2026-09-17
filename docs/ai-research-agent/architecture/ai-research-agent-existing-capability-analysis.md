# AI Research Agent 现有系统能力分析与重构决策

## 1. 结论

当前阶段应继续在原有 Java/Spring Boot 模块上优化重构，不应将 AI Research Agent 整体改写为 Python。

研究质量的主要瓶颈不是 Java 缺少 Agent 能力，而是证据链、任务隔离、质量门禁、失败语义和评测体系尚未形成闭环。整体迁移到 Python 不会自然解决这些问题，反而会增加双运行时部署、用户与 Workspace 隔离、事务一致性、鉴权、监控和数据模型同步成本。

Python 适合作为后续离线评测、模型实验、论文解析或特殊数据处理的可替换 Worker，但不应替代当前研究领域的主编排与业务边界。

## 2. 已有能力

项目已经具备可继续演进的基础设施：

- Spring Boot 研究项目、任务、计划、来源、步骤、结论和报告数据模型。
- `PlannerAgent`、`KnowledgeAgent`、`GapAgent`、`ExternalResearchAgent`、`CriticAgent`、`SynthesizerAgent` 和 `KnowledgeWriterAgent` 链路。
- 现有 `AiService` 模型调用能力，避免重复建设 AI Model Client。
- 现有知识检索、RAG、Elasticsearch、知识图谱和研究记忆能力。
- `AgentContext`、预算对象、研究步骤记录以及基本暂停语义。
- 前端研究工作区和已有 Research API。

这些能力与用户、Workspace、数据库和知识库已经处于同一业务事务边界，是保留 Java 主架构的重要原因。

## 3. 质量问题根因

### 3.1 伪来源污染证据链

原实现会在网页搜索无结果时要求 LLM 生成“模拟网页来源”，随后把这些内容作为来源和发现传给 Critic 与 Synthesizer。即使 URL 被替换成真实站点的入口或搜索页，也不能证明页面支持对应结论。

这会产生最严重的质量错误：模型知识被包装成外部证据，用户无法区分已验证事实和模型推测。

### 3.2 跨任务来源串扰

原实现把所有任务来源累积在同一个列表中，并在每个任务提取发现时传入整个列表。后执行任务可能引用前面任务的资料，LLM 返回的任务内 `sourceIndices` 也会被 Critic 当作全局索引解释，导致引用错位。

### 3.3 抓取正文未用于发现提取

系统虽然调用 `WebFetchTool` 抓取正文并写入数据库，但发现提取提示词只包含搜索结果标题和摘要。研究深度事实上受限于搜索摘要，而不是抓取后的原始材料。

### 3.4 质量门禁不足

- Agent 失败后编排器仍将项目和全部任务标记为 `COMPLETED`。
- Critic 的高置信度主要由来源类型权重决定，尚未充分约束来源数量、独立性、时效和原文支持。
- `maxIterations` 已进入上下文，但尚未形成基于 Critic 反馈的有限补充研究循环。
- 缺少固定问题集和可量化的引用正确率、来源覆盖率、结论支持率评测。

## 4. 本轮已实施改造

### 4.1 禁止生成模拟来源

外部搜索没有获得可验证网页时：

- 不再调用 LLM 生成来源或发现。
- 输出 `evidenceStatus=UNAVAILABLE`。
- 输出明确的降级说明。
- 将执行上下文标记为降级状态，供后续编排和界面表达使用。

### 4.2 按研究任务隔离证据

每个任务只使用本任务搜索并抓取的来源提取发现。任务完成后再合并进入全局来源列表，避免不同研究问题之间互相污染。

### 4.3 强制引用有效性

新增引用规范化逻辑：

- 校验 `sourceIndices` 必须落在当前任务来源范围内。
- 去除重复和越界引用。
- 将任务内索引转换成全局索引。
- 没有有效来源引用的发现不得进入 Critic 和报告链路。

### 4.4 使用抓取正文

发现提取现在会使用每个来源最多 3000 字符的正文摘录，而不再只依赖搜索摘要。完整正文仍保存在 `research_source` 中，以便追溯。

### 4.5 消除重复来源写入

来源在成功抓取时已经携带 `taskId` 和正文落库，因此移除了后续再次批量插入简化来源的逻辑，避免重复记录以及任务关联丢失。

### 4.6 Critic 确定性质量门禁

Critic 不再只输出一组置信度标签，还会输出结构化 `qualityGate`：

- `passed`：当前证据是否达到进入最终报告的标准。
- `retryRequired`：是否需要进行补充检索。
- `reasons`：未通过的确定性原因。
- `acceptableConfidenceRatio`：中高置信结论占比。
- `followUpQueries`：下一轮需要补证的问题。

当前门禁要求：不存在未解决冲突，中高置信结论占比不低于 70%，并且至少存在一条高置信结论或两条中置信结论。单一普通网页最多只能形成中置信结论；高置信结论至少需要两个不同域名的独立来源。

### 4.7 有限补充研究循环

编排器在 Critic 未通过门禁时执行 `ResearchAgent -> CriticAgent` 补证循环：

- 初始研究计为第 1 轮。
- 后续检索只使用 Critic 输出的 `followUpQueries`。
- 新来源和原有来源合并，重复 URL 不再抓取。
- 当前轮数会同步更新到 `research_project.current_iteration`。
- 总轮数严格不超过项目 `maxIterations`。
- 达到上限后仍不通过时，执行上下文保持降级状态。

### 4.8 报告结论过滤

Synthesizer 只允许 `high`、`medium` 且没有冲突标记的结论进入“已验证结论”。`low`、`speculation` 或存在冲突的内容不再被包装为确定事实。

### 4.9 预算控制与低收益熔断

三类预算已加入 `AgentContext` 并进入补证循环的实际判断：

- `TokenBudget`：Agent 返回精确 `tokensUsed` 时优先使用；当前 `AiService` 未返回供应商 usage 的调用按输出字符数估算。该值用于工程限流，不等同于供应商账单 Token。
- `ToolCallBudget`：每次真实 `web_search` 和 `web_fetch` 执行前预占额度；相同工具和参数的重复调用会直接跳过，不重复消耗额度。
- `TimeBudget`：从项目开始执行时计时，在每轮补证开始前以及 Research Agent 返回后检查总时间。

默认配置位于 `application.yml`：

```yaml
research:
  budget:
    max-tokens: ${RESEARCH_MAX_TOKENS:60000}
    max-tool-calls: ${RESEARCH_MAX_TOOL_CALLS:60}
    project-timeout-ms: ${RESEARCH_PROJECT_TIMEOUT_MS:600000}
```

任一预算耗尽都会终止补证、进入降级模式，并在 `research_step` 中以 `QualityGate/BUDGET_CHECK` 记录停止原因：

- `TOKEN_BUDGET_EXHAUSTED`
- `TOOL_CALL_BUDGET_EXHAUSTED`
- `TIME_BUDGET_EXHAUSTED`

补证循环同时比较每轮 Research Agent 返回的去重来源数量。如果连续两轮没有新增来源，提前以 `NO_NEW_SOURCES_TWO_ROUNDS` 停止，避免继续消耗模型和搜索调用。

### 4.10 可信终态语义

新增项目终态 `PARTIAL`，表示系统已经生成可查看的研究材料，但 Agent 失败、预算耗尽或质量门禁未通过，不能视为完整成功：

- 全部要求满足：项目 `COMPLETED`，任务 `COMPLETED`。
- 有部分结果但处于降级模式：项目 `PARTIAL`，未完成质量目标的任务 `FAILED`。
- Planner 无法生成有效 Agent 链：项目与任务均为 `FAILED`。

`PARTIAL` 支持重新执行和归档。前端将其显示为黄色“部分完成”，视为终态并停止轮询，但保留重新执行入口。该状态使用现有 `VARCHAR` 字段，无需修改数据库表结构。

编排器增加纯 Mockito 测试，验证成功链路会形成 `COMPLETED`，降级链路会形成 `PARTIAL`，同时校验项目与任务状态一致。

### 4.11 前端轮询与研究产物展示修复

研究完成后持续请求的原因是 `watch(currentProject)` 与轮询回调互相触发：轮询获取新项目对象后更新 `currentProject`，watcher 随即重新创建定时器并加载任务、步骤、记忆、报告等数据，导致终态清理无法稳定生效。

修复后的约束：

- 只有 `RESEARCHING`、`REVIEWING`、`SYNTHESIZING` 会启动轮询。
- 同一项目的普通对象刷新不会重新创建定时器或重复全量加载。
- 轮询请求未完成时不允许下一轮重入。
- `COMPLETED`、`PARTIAL`、`FAILED`、`ARCHIVED` 到达后立即停止轮询并只加载一次最终数据。

“研究结论”为空来自 `research_memory` 的唯一约束 `(project_id, memory_key)`。FINDING 与 CONCLUSION 原先使用相同陈述作为 key，保存结论时实际更新了 FINDING 内容，却没有改变其 `memoryType`。为避免未经确认修改已有数据库表，各类型记忆改用物理键命名空间：

```text
FINDING:<logical-key>
CONCLUSION:<logical-key>
CANDIDATE:<logical-key>
```

前端仅展示冒号后的逻辑 key，因此不会暴露内部存储前缀。重新执行研究后，新生成的发现、结论和候选可以同时保留。

“知识候选”为空还包含一个独立错误：`Map.of` 不允许 value 为 `null`，而候选去重元数据把 `mostSimilarNodeId` 初始化为 null，导致候选解析成功后仍抛出异常并退化为空列表。现已改为允许 null 的 `LinkedHashMap`，并增加候选持久化回归测试。

### 4.12 知识候选确认与忽略持久化

知识候选页面原先的“保存到知识库”和“忽略”只修改浏览器内的 `memories` 数组，并直接显示成功提示，没有调用后端。因此保存不会生成 `knowledge_node`，忽略也不会删除 `research_memory`，刷新后候选必然恢复。

本次沿用现有 `KnowledgeService`，没有重复实现知识库写入能力，也没有修改数据库表：

- `POST /research/projects/{projectId}/memory/{memoryId}/accept`：校验项目归属、用户归属及 `CANDIDATE` 类型，在同一事务中调用 `KnowledgeService.create`，并仅在创建成功后删除候选记忆。
- `DELETE /research/projects/{projectId}/memory/{memoryId}/candidate`：完成相同的访问与类型校验后，持久化删除候选记忆。
- 知识节点继承研究项目的 `workspaceId`，避免保存到错误的个人空间或工作区。
- 候选标题从带命名空间的 `memoryKey` 提取，正文继续使用候选的 Markdown 内容。
- 前端请求期间禁用当前候选的两个操作按钮；只有接口成功后才移除本地候选并提示成功，请求失败时保留候选供用户重试。

回归测试覆盖知识创建成功后删除候选、知识创建失败时保留候选，以及忽略候选不触发知识创建三条关键路径。

## 5. 推荐目标架构

保持 Java 作为控制面和业务面：

1. Spring Boot 负责鉴权、Workspace 隔离、计划、状态机、预算、持久化和 API。
2. 现有知识库、RAG、Elasticsearch 和知识图谱继续通过既有 Service/Tool 复用。
3. 研究工具通过稳定接口接入；某个工具确实依赖 Python 生态时，才拆成无状态 Worker。
4. 所有发现采用 `Claim -> Evidence Span -> Source` 结构，不允许仅关联一个 URL。
5. Critic 输出机器可判定的质量门禁；未通过时只允许在 `maxIterations` 内生成补充查询。
6. Synthesizer 只能消费通过门禁的结论，低证据内容必须进入“待验证问题”，不能进入确定结论。

## 6. 后续优先级

### P0：证据可信度

- 为来源保存正文证据片段、发布时间、作者、域名和内容哈希。
- 高置信结论至少需要一个高权威直接来源，或两个相互独立的普通来源。
- 报告引用必须能回到具体原文片段，而不只是网页 URL。
- 无证据、抓取失败和证据冲突必须作为正式状态展示。

### P1：细化反思循环

- 在现有 `followUpQueries` 基础上增加结构化 `missingEvidence` 和冲突消解策略。
- 改造 `AiService` 返回统一 usage，使 TokenBudget 从字符估算升级为供应商实际 Token。
- 根据任务复杂度生成不同预算档位，而不是所有项目使用相同默认值。

### P1：研究评测

建立固定评测集并持续记录：

- 引用正确率：引用页面是否真实支持结论。
- 引用覆盖率：确定性结论中带有效引用的比例。
- 来源独立性：支撑同一结论的不同来源是否实际独立。
- 来源权威度和时效性。
- 个性化增益：报告是否利用了用户已有知识与知识缺口。
- 降级诚实度：无证据时是否明确表达不确定性。

### P2：可选 Python Worker

只有出现以下需求时再引入 Python，并保持为可替换工具服务：

- 复杂 PDF/论文版面解析。
- 本地模型推理或特定 Python-only ML 库。
- 离线研究评测与数据分析。
- 大规模爬取后的专用文本处理。

Python Worker 不直接修改用户知识库，不拥有研究状态机，也不绕过 Java 的鉴权、预算和审计。

## 7. 验收标准

- 搜索失败时数据库和报告中不出现 AI 生成的伪来源。
- 任一发现的来源索引都指向当前任务实际抓取成功的来源。
- 无有效来源引用的发现不会成为研究结论。
- 抓取正文被用于发现提取，来源正文仍可追溯。
- 引用规范化单元测试通过。
- 后续引入反思循环时，执行次数永远不超过 `maxIterations`。
- 保存知识候选后生成真实知识节点，且刷新研究页面后候选不再出现。
- 忽略知识候选后刷新页面仍保持移除状态。
- 知识库写入失败时不删除研究候选，也不向前端返回假成功。

## 8. 风险与边界

本轮改造优先保证“宁缺毋滥”，因此网络不可用时报告内容可能减少。这是有意的可信度取舍。下一步应通过更可靠的搜索 Provider、来源分级和有限补充检索提升召回率，而不是恢复无证据的模型回退。
