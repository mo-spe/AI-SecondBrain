# 采集入库工作流重构 — PRD

> 日期：2026-07-25
> 状态：已实施
> 来源：docs/需求池.md #6

---

## 1. Executive Summary

**问题**: AI-SecondBrain 的"采集对话 → AI 提取知识点 → 生成复习卡片"是一条龙全自动流程，用户无法控制中间环节。引入协同工作区后，采集的知识点 `workspaceId` 丢失，产生了数据隔离问题。用户在以下场景中受阻：
- 对话本身已足够浓缩时，AI 再次提取知识点是多余的噪音
- 知识点该放到哪个工作区（个人空间 vs 协同工作区）没有明确归属
- AI 提取的知识点质量不可控，入库前没有人工检查环节
- 自动生成复习卡片太强制，用户可能只想存档不想复习
- 协同工作区中的知识点无法与其他成员隔离

**方案**: 将一条龙全自动流程拆成三个独立可选动作（采集对话 / AI 提取知识点 / 生成复习卡片），加入人工确认环节（pending_knowledge 中间表），全程适配个人空间/协同工作区的 workspaceId 传递。

**成功标准**:
1. 采集时用户可选择目标工作区（个人空间或任一协同工作区）
2. 采集时可独立开关"提取知识点"和"生成复习卡片"
3. AI 提取的知识点先进入"待确认"列表，用户编辑确认后才正式入库
4. 所有知识点和复习卡片携带正确的 workspaceId，实现工作区间数据隔离
5. 上线后 30 天内，待确认知识点的确认率 >= 60%

---

## 2. User Experience & Functionality

### 2.1 用户画像

| 画像 | 描述 | 核心需求 |
|------|------|---------|
| 知识管理者小王 | 日常用 AI 对话学习，想选择性存档 | 只存档有价值的对话，不想要多余的复习卡片 |
| 团队协作者老张 | 在协同工作区中与团队分享知识 | 知识点能明确归属到协同工作区，与个人空间隔离 |
| 效率追求者小李 | 对话频次高，内容已经足够浓缩 | 跳过 AI 提取，直接保存原始对话 |

### 2.2 用户故事

**Story 1**: 作为知识管理者，我采集对话时想选择目标工作区和是否提取知识点，这样我不会在个人空间里混入协同工作区的内容。

**Acceptance Criteria**:
- 采集对话框中可见工作区下拉列表（含"个人空间"和所有协同工作区）
- 采集对话框中可见"提取知识点"开关（默认开启）
- 采集对话框中可见"生成复习卡片"开关（默认关闭，仅在提取知识点开启时可见）
- 选择"个人空间"时 workspaceId 为 null，知识仅自己可见

**Story 2**: 作为团队协作者，我想在知识点入库前检查和编辑 AI 提取的内容，这样我不会把低质量的知识点分享给团队。

**Acceptance Criteria**:
- 知识库页面新增"待确认"Tab，展示所有待确认知识点
- 每条待确认知识点可编辑标题、摘要、详细内容
- 支持单条删除和批量全部删除
- 支持手动新增知识点到待确认列表
- "确认入库"按钮将选中的知识点一次性写入 knowledge_node 表
- 入库时可勾选是否同时生成复习卡片

**Story 3**: 作为效率追求者，我想关闭 AI 提取直接保存原始对话，这样我的知识库不会产生多余的 AI 生成内容。

**Acceptance Criteria**:
- 关闭"提取知识点"开关后，仅保存 RawChatRecord，不触发 AI 提取
- RawChatRecord 正确记录 workspaceId

### 2.3 非目标 (Non-Goals)

- 不改变已有 knowledge_node 和 review_card 的已有数据
- 不在此 PRD 中实现复习卡片的批量生成/重生成功能（后续需求）
- 不改变 DocumentCapture 和 NoteCapture 的采集流程（保持向后兼容）

---

## 3. Technical Specifications

### 3.1 架构概览

```
[采集对话框] → ChatCollectRequest (含 workspaceId, extractKnowledge, generateCards)
     ↓
[ChatServiceImpl] → 保存 RawChatRecord
     ↓  (if extractKnowledge=true)
[Kafka Producer] → ChatCollectMessage (含 record + workspaceId + generateCards)
     ↓
[Kafka Consumer] → KnowledgeCaptureService.extractKnowledge()
     ↓
[AI 提取 KnowledgeDTO[]] → 遍历写入 pending_knowledge (status=0)
     ↓
[PendingKnowledgeController] → 用户编辑/确认
     ↓
[PendingKnowledgeServiceImpl.confirmBatch()] → 写入 knowledge_node + 可选生成 review_card
```

### 3.2 数据模型

**pending_knowledge 表**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| user_id | BIGINT NOT NULL | 所属用户 |
| workspace_id | BIGINT | 目标工作区（null=个人空间） |
| raw_chat_id | BIGINT | 来源对话记录 |
| title | VARCHAR(500) | 知识点标题 |
| summary | TEXT | 摘要 |
| content | TEXT | 详细内容 |
| status | TINYINT | 0=待确认, 1=已确认入库, 2=已丢弃 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除标记 |

### 3.3 API 接口

**新增接口**（挂在 `/knowledge` 路径下）：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /knowledge/pending | 获取待确认列表（支持 workspaceId 筛选） |
| POST | /knowledge/pending/confirm | 批量确认入库（BatchConfirmRequest） |
| DELETE | /knowledge/pending/{id} | 丢弃单条待确认知识点 |
| POST | /knowledge/pending/add | 手动新增待确认知识点 |

**修改接口**：

| 方法 | 路径 | 变更 |
|------|------|------|
| POST | /chat/collect | ChatCollectRequest 新增 workspaceId, extractKnowledge, generateCards |

### 3.4 新增/修改文件清单

**新建**:

| 文件 | 说明 |
|------|------|
| `sql/V8__add_pending_knowledge.sql` | DDL |
| `entity/PendingKnowledge.java` | 实体类 |
| `mapper/PendingKnowledgeMapper.java` | MyBatis-Plus Mapper |
| `service/PendingKnowledgeService.java` | 服务接口 |
| `service/impl/PendingKnowledgeServiceImpl.java` | 服务实现 |
| `dto/BatchConfirmRequest.java` | 批量确认请求 DTO |
| `dto/PendingKnowledgeItem.java` | 确认项 DTO |
| `dto/ChatCollectMessage.java` | Kafka 消息封装 |

**修改**:

| 文件 | 变更内容 |
|------|---------|
| `dto/ChatCollectRequest.java` | +workspaceId, +extractKnowledge, +generateCards |
| `service/impl/ChatServiceImpl.java` | collectChat 分支逻辑 |
| `kafka/KafkaProducerService.java` | 消息体扩展为 ChatCollectMessage |
| `kafka/KafkaConsumerService.java` | 消费逻辑改为写 pending_knowledge |
| `service/KnowledgeCaptureService.java` | 返回类型 KnowledgeNode → int |
| `service/impl/KnowledgeCaptureServiceImpl.java` | 写 pending_knowledge 代替 knowledge_node |
| `controller/KnowledgeController.java` | +4 个待确认端点 |
| `frontend/src/api/knowledge.js` | +4 个 API 方法 |
| `frontend/src/views/Capture.vue` | 采集对话框新增工作区选择+开关 |
| `frontend/src/views/Chat.vue` | 同上 |
| `frontend/src/views/Knowledge.vue` | +待确认 Tab + 编辑确认区域 |

### 3.5 向后兼容

- `KafkaProducerService.sendChatCollect(RawChatRecord)` 保留为 deprecated 方法，内部包装为 ChatCollectMessage（extractKnowledge=true, generateCards=false, workspaceId 取自 record）
- DocumentCaptureServiceImpl、NoteCaptureServiceImpl 仍使用旧方法，行为不变

---

## 4. Risks & Roadmap

### 4.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Kafka 消息格式变更导致旧消息消费失败 | 采集中断 | ChatCollectMessage 兼容旧字段，consumer 有默认值兜底 |
| pending_knowledge 表数据量增长 | 查询变慢 | 定期清理 status=2（已丢弃）的记录，后续可加 TTL |
| AI 提取服务质量波动 | 待确认列表充斥低质量条目 | 用户可单条删除或批量丢弃，不影响正式知识库 |

### 4.2 分阶段实施

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 1 | 数据基础（SQL + Entity + Mapper + DTO） | 已完成 |
| Phase 2 | 后端服务层（Service + Controller + Kafka 改造） | 已完成 |
| Phase 3 | 前端采集对话框改造（Capture.vue + Chat.vue） | 已完成 |
| Phase 4 | 前端待确认列表（Knowledge.vue 待确认 Tab） | 已完成 |
| Phase 5 | 联调 + 验证（10 个测试场景） | 待进行 |

---

## 5. 验证清单

| # | 场景 | 预期结果 |
|---|------|---------|
| 1 | 采集时关闭"提取知识点" | 仅 RawChatRecord 入库，pending_knowledge 无新记录 |
| 2 | 采集时开启"提取"，选择个人空间 | pending_knowledge 有记录，workspace_id = null |
| 3 | 采集时开启"提取"，选择协同工作区 | pending_knowledge 有记录，workspace_id 正确 |
| 4 | 待确认页编辑标题/摘要/内容 | 修改保留在 pending_knowledge |
| 5 | 待确认页删除某条 | pending_knowledge status=2 |
| 6 | 待确认页新增知识点 | 新 PendingKnowledge 插入（status=0） |
| 7 | 确认入库 + 勾选生成卡片 | KnowledgeNode 创建 + ReviewCard 生成 |
| 8 | 确认入库 + 取消生成卡片 | KnowledgeNode 创建，无 ReviewCard |
| 9 | 不同工作区间隔离 | 只在选定工作区看到对应待确认项 |
| 10 | 后端向后兼容（旧 Kafka 消息） | 旧调用方（Document/Note）正常采集 |
