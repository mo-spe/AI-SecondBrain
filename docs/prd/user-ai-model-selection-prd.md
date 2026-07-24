# 用户级 AI 模型供应商与模型选择 — PRD

> 日期：2026-07-24
> 状态：待审批
> 来源：docs/需求池.md #5

---

## 1. Executive Summary

**问题**: AI-SecondBrain 目前将 AI 服务商和模型在代码/配置中全局写死（`ai.provider` + 硬编码模型名）。用户只有一个 `apiKey` 字段，既不能选择用哪家服务商，也不能为不同场景（对话/知识提取/题目生成等）配置不同的模型。多租户场景下，有的用户习惯用千问、有的用 DeepSeek、有的有 OpenAI 账号——全局配置无法满足差异化需求。

**方案**: 将 AI 服务商和模型选择权完全交还给用户。用户在个人设置页为 5 大 AI 场景（对话、知识提取、题目生成、Embedding 向量化、研究报告）独立配置「服务商 + 模型 + API Key」。管理员通过平台后台维护可选的服务商和模型列表，同时支持用户自定义模型名称。

**成功标准**:
1. 用户能在设置页面完成为每个场景配置服务商、模型、API Key，配置持久化保存
2. 所有 14 个 AI 调用场景均按用户配置执行，100% 不走全局默认值
3. 未配置的用户在使用 AI 功能时收到明确的"请先配置 AI 服务商"引导提示
4. 管理员可在后台增删服务商和模型，无需重启服务或改代码发版
5. 上线后 30 天内，60% 活跃用户完成至少一个场景的 AI 配置

---

## 2. User Experience & Functionality

### 2.1 用户画像

| 画像 | 描述 | 核心需求 |
|------|------|---------|
| 国内学生小李 | 无翻墙环境，使用国内服务商 | 千问/DeepSeek 配置，中文问答质量好 |
| 海外用户 David | 有 OpenAI/Anthropic 账号 | 使用 GPT-4o / Claude，追求回答质量 |
| 精打细算的老张 | 关注 API 费用 | 不同场景用不同模型：RAG 问答用贵的（gpt-4o），知识提取用便宜的（qwen-turbo） |
| 平台管理员 | 负责平台运维 | 维护可选服务商和模型列表，下架过时模型 |

### 2.2 用户故事

**US-1: 个人 AI 配置（核心）**
- 作为用户，我想在个人设置页为每个 AI 场景独立配置服务商和模型，让不同场景使用最合适的模型
- AC:
  - 设置页新增「AI 模型配置」区域，展示 5 个场景卡片：对话、知识提取、题目生成、Embedding 向量化、研究报告
  - 每个场景卡片包含：服务商下拉框 → 模型下拉框（或自定义输入）→ API Key 输入框（带显隐切换）
  - API Key 支持"全局 Key"模式：用户可为同一服务商配置一个全局 Key，所有使用该服务商的场景自动复用，也可为每个场景单独覆盖
  - 点击保存后配置立即生效，下次 AI 调用走新配置
  - 未配置的场景在使用对应 AI 功能时，弹出提示"请先在设置页配置 AI 服务商和模型"

**US-2: 服务商和模型选择**
- 作为用户，我想从平台支持的服务商列表中选择，同时可以自定义输入模型名称
- AC:
  - 服务商下拉框展示管理员启用的所有服务商（带 logo 和名称）
  - 选择服务商后，模型下拉框自动加载该服务商下的预设模型列表
  - 模型下拉框底部有一条分隔线和"自定义模型"选项，选中后可手动输入模型名称
  - 自定义模型名称保存后同样生效

**US-3: 管理员管理服务商**
- 作为管理员，我想在后台管理可选的服务商列表（增删改查），控制用户可见的服务商范围
- AC:
  - 平台管理页新增「AI 服务商管理」Tab
  - 支持添加服务商：填写名称、code、API 地址、API 类型（OpenAI 兼容 / Anthropic / Gemini / 自定义）
  - 支持启用/禁用服务商（禁用的服务商在用户端不可见，但不影响已配置用户）
  - 支持编辑现有服务商信息
  - 删除服务商前需确认，并提示该服务商当前被多少用户使用

**US-4: 管理员管理模型**
- 作为管理员，我想为每个服务商维护预设模型列表，方便用户快速选择
- AC:
  - 服务商详情页展示该服务商下的模型列表
  - 支持添加模型：填写模型名称、显示名称、适用场景（多选：对话/知识提取/题目生成/Embedding/研究报告）
  - 支持启用/禁用模型
  - 支持编辑和删除模型

**US-5: API Key 安全存储**
- 作为用户，我关心的 API Key 被安全存储，不会泄露
- AC:
  - API Key 在数据库中以 AES-256 加密存储
  - 前端展示时默认脱敏（显示前4位+后4位，中间 `****`）
  - 管理后台不展示用户 API Key 明文
  - 日志中不记录 API Key

### 2.3 场景覆盖矩阵

| 大类 | 涵盖的现有场景 | 涉及的 Service / 调用链 |
|------|---------------|----------------------|
| 对话 | AI 普通对话、AI 知识增强对话 | ChatSessionServiceImpl.chat() / chatWithKnowledge() |
| 知识提取 | 对话记录知识提取、采集内容知识提取 | KnowledgeCaptureService / ChatService → AiService.extractKnowledge() |
| 题目生成 | 选择题生成、填空题生成、简答题生成 | QuestionGenerationService → AiService.generateQuestion() |
| Embedding 向量化 | 知识节点向量生成、语义搜索 | EmbeddingService.generateEmbedding() |
| 研究报告 | 深度学习报告、学习路径、知识盲区分析 | DeerFlowResearchService（转发用户 Key 到 DeerFlow） |

### 2.4 非目标 (MVP 排除)

- 不做模型效果对比测评（用户自行判断哪个模型好用）
- 不做 API 用量统计与费用估算面板（可后续版本加）
- 不做服务商维度的速率限制（Rate Limiting）
- 不做 A/B 测试框架（同一场景同时调多个模型对比结果）
- Embedding 场景第一期只支持千问，后续版本扩展
- 不做自定义 API 地址（用户不能添加平台未收录的私有部署服务商）

---

## 3. Technical Specifications

### 3.1 架构总览

```
Vue 3 + Element Plus + Pinia (Frontend)
  │
  ├── Settings.vue (新增「AI 模型配置」区域)
  ├── AdminDashboard.vue (新增「AI 服务商管理」Tab)
  │
  │ HTTP REST (JWT)
  │
Spring Boot 3.1.5 + MyBatis-Plus 3.5.3.1 (Backend)
  │
  ├── AiProviderController     (NEW — 用户端：获取可用服务商/模型列表)
  ├── UserAiConfigController   (NEW — 用户端：保存/读取个人AI配置)
  ├── AdminAiProviderController (NEW — 管理端：服务商 CRUD)
  ├── AdminAiModelController    (NEW — 管理端：模型 CRUD)
  │
  ├── AiService (REFACTOR — 从配置驱动改为数据库驱动)
  ├── ChatSessionServiceImpl (REFACTOR — 收口到 AiService)
  │
  └── 4 张新表 + 4 个 Entity + 4 个 Mapper
```

### 3.2 数据库设计

#### ai_provider — AI 服务商定义

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | 主键 |
| code | VARCHAR(30) UNIQUE | 服务商标识（openai / qwen / deepseek / anthropic / gemini / kimi / zhipu / doubao / minimax） |
| name | VARCHAR(50) | 服务商显示名称（OpenAI / 通义千问 / DeepSeek） |
| base_url | VARCHAR(255) | API 基础地址（如 https://api.openai.com） |
| api_type | VARCHAR(30) | API 协议类型：openai_compatible / anthropic / gemini |
| logo_url | VARCHAR(255) | 服务商 Logo 图标地址 |
| is_enabled | TINYINT | 是否启用（0-禁用，1-启用） |
| sort_order | INT | 排序顺序 |
| create_time / update_time | DATETIME | 时间戳 |

#### ai_model — 预设模型列表

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | 主键 |
| provider_id | BIGINT FK | 关联 ai_provider.id |
| model_name | VARCHAR(100) | 模型标识（gpt-4o / qwen-plus） |
| display_name | VARCHAR(100) | 显示名称（GPT-4o / 通义千问 Plus） |
| is_enabled | TINYINT | 是否启用 |
| supported_scenarios | VARCHAR(200) | 适用场景（JSON 数组：["chat","extraction","question_gen","embedding","research"]） |
| sort_order | INT | 排序 |
| create_time / update_time | DATETIME | 时间戳 |
| UNIQUE(provider_id, model_name) | | 防重 |

#### user_ai_config — 用户 AI 场景配置

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT FK | 用户 ID |
| scenario_code | VARCHAR(30) | 场景代码：chat / extraction / question_gen / embedding / research |
| provider_id | BIGINT FK | 关联 ai_provider.id |
| model_name | VARCHAR(100) | 用户选择的模型（可以是预设模型名或自定义模型名） |
| api_key | VARCHAR(500) | **AES-256 加密存储**的用户 API Key |
| create_time / update_time | DATETIME | 时间戳 |
| UNIQUE(user_id, scenario_code) | | 每个用户每个场景仅一条配置 |

#### user_ai_provider_key — 用户服务商全局 Key（可选）

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT FK | 用户 ID |
| provider_id | BIGINT FK | 关联 ai_provider.id |
| api_key | VARCHAR(500) | **AES-256 加密存储** |
| create_time / update_time | DATETIME | 时间戳 |
| UNIQUE(user_id, provider_id) | | 每个用户每个服务商仅一条 |

> **设计决策**: `user_ai_config.api_key` 和 `user_ai_provider_key.api_key` 同时存在。配置解析时：场景级 Key 优先，若为空则查该服务商的全局 Key，若仍为空则提示用户未配置。

### 3.3 API 设计

#### 用户端 API

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/ai/providers` | 获取可用服务商列表（仅返回 is_enabled=1 的） |
| GET | `/api/ai/providers/{id}/models` | 获取某服务商下的预设模型列表 |
| GET | `/api/user/ai-config` | 获取当前用户的所有场景配置 |
| PUT | `/api/user/ai-config` | 批量保存场景配置 |
| PUT | `/api/user/ai-config/provider-key` | 保存某服务商的全局 API Key |

#### 管理端 API

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/ai/providers` | 服务商列表（含禁用） |
| POST | `/api/admin/ai/providers` | 新增服务商 |
| PUT | `/api/admin/ai/providers/{id}` | 编辑服务商 |
| DELETE | `/api/admin/ai/providers/{id}` | 删除服务商 |
| GET | `/api/admin/ai/providers/{id}/models` | 模型列表（含禁用） |
| POST | `/api/admin/ai/providers/{id}/models` | 新增模型 |
| PUT | `/api/admin/ai/models/{id}` | 编辑模型 |
| DELETE | `/api/admin/ai/models/{id}` | 删除模型 |

### 3.4 AiService 重构方案

#### 当前架构问题

```
当前调用链（分散，不一致）：

RagService ─────────────→ AiService.generateAnswer(prompt, userApiKey)  ✅ 走 AiService
QuestionGenerationService → AiService.generateQuestion(prompt, userApiKey) ✅ 走 AiService
KnowledgeCaptureService ─→ AiService.extractKnowledge(content)           ⚠️ 未传 userApiKey
ChatSessionServiceImpl ──→ 自己的 OpenAiClient                           ❌ 绕过 AiService
```

#### 目标架构

```
目标调用链（统一入口）：

所有调用方 ──→ AiService.xxx(userId, scenarioCode, ...params)
                    │
                    ├── 1. 查 user_ai_config (scenario_code + user_id)
                    ├── 2. 解析 provider_id + model_name + api_key
                    ├── 3. 查 ai_provider 获取 base_url + api_type
                    ├── 4. 创建对应的 Client (OpenAiClient / AnthropicClient / GeminiClient)
                    ├── 5. 执行调用
                    └── 6. 返回结果
```

#### AiService 接口重构

```java
// 旧接口（废弃）
String generateAnswer(String prompt, String userApiKey);

// 新接口（统一使用 userId + scenarioCode）
String generateAnswer(Long userId, String scenarioCode, String prompt);
String generateQuestion(Long userId, String scenarioCode, String prompt);
List<KnowledgeDTO> extractKnowledge(Long userId, String scenarioCode, String content);
String chat(Long userId, String scenarioCode, List<Message> messages);

// 新增：根据 userId + scenarioCode 解析配置
AiCallConfig resolveConfig(Long userId, String scenarioCode);
```

> **场景常量化**: `scenarioCode` 使用枚举 `AiScenario`：`CHAT`, `EXTRACTION`, `QUESTION_GEN`, `EMBEDDING`, `RESEARCH`

### 3.5 多 API 协议支持

| api_type | 客户端/SDK | 覆盖服务商 |
|----------|-----------|----------|
| `openai_compatible` | 现有 `OpenAiClient`（unfbx/chatgpt 库） | OpenAI、千问、DeepSeek、Kimi、智谱、豆包、MiniMax |
| `anthropic` | 新增 `AnthropicClient`（基于 OkHttp + Anthropic Messages API） | Anthropic |
| `gemini` | 新增 `GeminiClient`（基于 OkHttp + Google Gemini API） | Gemini |

> OpenAI 兼容协议覆盖 9 家中 7 家，Anthropic 和 Gemini 需独立适配。第一期全部 9 家都支持。

### 3.6 API Key 加密方案

```
加密算法: AES-256-GCM
密钥来源: ${AI_CONFIG_ENCRYPTION_KEY} 环境变量（32字节）
加密时机: 写入 user_ai_config / user_ai_provider_key 前
解密时机: AiService.resolveConfig() 读取 api_key 时

Entity 层:
  - @TableField 存储加密后的密文
  - Service 层提供 encryptKey() / decryptKey() 方法
  - 使用 MyBatis-Plus TypeHandler 自动加解密（可选方案）
```

### 3.7 Embedding 场景特殊处理

第一期 Embedding 仅支持千问。`user_ai_config` 中 scenario_code=embedding 的 provider_id 限定为千问。

`EmbeddingService.generateEmbedding()` 调用链改造：
```
旧: generateEmbedding(text, model, userApiKey)
新: generateEmbedding(text, userId)
     └── 内部调用 AiService.resolveConfig(userId, "embedding")
     └── 仅千问有效，其他 provider 返回配置错误提示
```

### 3.8 种子数据

迁移文件 `V7__add_ai_provider_tables.sql` 预置 9 家服务商和常用模型：

```sql
-- 9 家服务商种子数据
INSERT INTO ai_provider (code, name, base_url, api_type, is_enabled, sort_order) VALUES
('openai',     'OpenAI',       'https://api.openai.com',                          'openai_compatible', 1, 1),
('qwen',       '通义千问',      'https://dashscope.aliyuncs.com/compatible-mode',    'openai_compatible', 1, 2),
('deepseek',   'DeepSeek',     'https://api.deepseek.com',                        'openai_compatible', 1, 3),
('anthropic',  'Anthropic',    'https://api.anthropic.com',                       'anthropic',         1, 4),
('gemini',     'Google Gemini','https://generativelanguage.googleapis.com',        'gemini',            1, 5),
('kimi',       'Kimi (Moonshot)','https://api.moonshot.cn',                       'openai_compatible', 1, 6),
('zhipu',      '智谱 AI',       'https://open.bigmodel.cn/api/paas/v4',           'openai_compatible', 1, 7),
('doubao',     '豆包 (字节)',     'https://ark.cn-beijing.volces.com/api/v3',        'openai_compatible', 1, 8),
('minimax',    'MiniMax',      'https://api.minimax.chat/v1',                     'openai_compatible', 1, 9);

-- 预置常用模型（以 OpenAI 和千问为例）
INSERT INTO ai_model (provider_id, model_name, display_name, is_enabled, supported_scenarios, sort_order) VALUES
(1, 'gpt-4o',         'GPT-4o',         1, '["chat","extraction","question_gen","research"]', 1),
(1, 'gpt-4o-mini',    'GPT-4o Mini',    1, '["chat","extraction","question_gen","research"]', 2),
(1, 'gpt-4-turbo',    'GPT-4 Turbo',    1, '["chat","extraction","question_gen","research"]', 3),
(2, 'qwen-plus',      '通义千问 Plus',   1, '["chat","extraction","question_gen","research"]', 1),
(2, 'qwen-max',       '通义千问 Max',    1, '["chat","extraction","question_gen","research"]', 2),
(2, 'qwen-turbo',     '通义千问 Turbo',  1, '["chat","extraction","question_gen"]',           3),
(2, 'text-embedding-v2', 'Embedding V2', 1, '["embedding"]',                                   4);
```

### 3.9 前端改动

| 文件 | 改动 |
|------|------|
| `src/views/Settings.vue` | 新增「AI 模型配置」区域：5 张场景卡片，每张含服务商下拉 + 模型下拉（含自定义入力） + API Key 输入。保存按钮。 |
| `src/views/AdminDashboard.vue` | 新增 Tab「AI 服务商管理」：服务商列表 + 新增/编辑弹窗；服务商详情子页：模型列表管理 |
| `src/api/ai.js` | 新增 API 封装：`getProviders`, `getModels`, `getUserAiConfig`, `saveUserAiConfig`, `saveProviderKey` |
| `src/api/admin.js` | 新增管理端 API：`getAiProviders`, `saveAiProvider`, `deleteAiProvider`, `getAiModels`, `saveAiModel`, `deleteAiModel` |
| `src/stores/aiConfig.js` | 新增 Pinia Store：缓存用户 AI 配置，前端使用 |

### 3.10 后端新建/修改文件清单

**新建文件 (~18 个)**:

| 文件 | 说明 |
|------|------|
| `sql/V7__add_ai_provider_tables.sql` | DDL + 种子数据 |
| `entity/AiProvider.java` | 服务商实体 |
| `entity/AiModel.java` | 模型实体 |
| `entity/UserAiConfig.java` | 用户 AI 配置实体 |
| `entity/UserAiProviderKey.java` | 用户服务商全局 Key 实体 |
| `mapper/AiProviderMapper.java` | 服务商 Mapper |
| `mapper/AiModelMapper.java` | 模型 Mapper |
| `mapper/UserAiConfigMapper.java` | 用户配置 Mapper |
| `mapper/UserAiProviderKeyMapper.java` | 用户 Key Mapper |
| `service/AiProviderService.java` + impl | 服务商管理服务 |
| `service/UserAiConfigService.java` + impl | 用户 AI 配置服务 |
| `service/ApiKeyEncryptionService.java` | AES-256 加解密服务 |
| `controller/AiProviderController.java` | 用户端：服务商/模型查询 |
| `controller/UserAiConfigController.java` | 用户端：个人配置 CRUD |
| `controller/AdminAiProviderController.java` | 管理端：服务商/模型管理 |
| `enums/AiScenario.java` | 场景枚举 |
| `client/AnthropicClient.java` | Anthropic API 客户端 |
| `client/GeminiClient.java` | Gemini API 客户端 |

**修改文件 (~6 个)**:

| 文件 | 改动 |
|------|------|
| `service/AiService.java` | 接口改为 userId + scenarioCode 驱动 |
| `service/impl/AiServiceImpl.java` | 实现从数据库读取配置，支持多 API 协议分发 |
| `service/impl/ChatSessionServiceImpl.java` | 移除自有 OpenAiClient，收口到 AiService |
| `service/impl/EmbeddingServiceImpl.java` | 改为通过 AiService 解析配置 |
| `config/WebMvcConfig.java` | JWT 拦截器新增 `/api/ai/**`、`/api/admin/ai/**` |
| `service/impl/RagServiceImpl.java` | 接口改为传 userId + scenarioCode |

---

## 4. Risks & Roadmap

### 4.1 分阶段交付

| 阶段 | 内容 | 估时 |
|------|------|------|
| **Phase 1: 数据基础** | V7 迁移 + 4 Entity + 4 Mapper + 种子数据 + ApiKeyEncryptionService | 3 天 |
| **Phase 2: 管理后台** | AdminAiProviderController + AdminAiModelController + AiProviderService + 管理端前端 Tab | 3 天 |
| **Phase 3: 用户配置** | UserAiConfigController + UserAiProviderKey + 设置页前端（5 张场景卡片） | 4 天 |
| **Phase 4: AiService 重构** | 接口改为 userId+scenarioCode 驱动 + AnthropicClient + GeminiClient + ChatSessionServiceImpl 收口 | 5 天 |
| **Phase 5: 接入层改造** | RagService / QuestionGenerationService / EmbeddingService / KnowledgeCaptureService 全部走新接口 | 3 天 |
| **Phase 6: 打磨** | API Key 脱敏展示、未配置引导提示、兼容性测试、安全审计 | 2 天 |

> 总计估算：**约 4 周**（前端 1 人 + 后端 1 人）

### 4.2 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Anthropic / Gemini API 协议差异 | 客户端适配工作量大 | Phase 4 先行做 PoC，验证两个 SDK 的可集成性 |
| API Key 加密密钥泄露 | 所有用户 Key 暴露 | 环境变量注入，不写入配置文件；密钥轮换机制预留 |
| 旧接口调用方遗漏 | 部分场景仍走全局默认值 | Phase 5 全量 grep AiService 调用点，逐个改造 |
| 用户配置错误（自己填的模型名无效） | AI 调用失败 | resolveConfig 时不做模型名校验，调用失败时将错误信息透传给前端展示 |
| ChatSessionServiceImpl 收口时破坏现有聊天功能 | 聊天功能不可用 | Phase 4 先保留旧代码路径，用 feature flag 切换；充分回归测试 |
| OpenAI 兼容协议不一致 | 某些国产服务商的 "OpenAI 兼容" 有细微差异 | 每个服务商上线前用 `generateAnswer` 做一个简单的冒烟测试 |

### 4.3 向后兼容

- `User.apiKey` 字段 **保留但不使用**。迁移脚本可将其作为历史数据保留，新系统不再读取该字段
- 旧的 `application.yml` 中 `ai.provider` 和 `ai.*.api-key` 配置保留，作为**系统级兜底**：当用户未配置某场景时，不走兜底，直接返回"请配置"提示（除非管理员配置了"允许使用系统默认值"开关）

---

## 5. Verification

1. **配置保存**: 用户在设置页选择千问 → qwen-plus → 输入 Key → 保存 → 刷新页面 → 配置仍在
2. **配置生效**: 配置对话场景为 DeepSeek → 发送 RAG 问答 → 后台日志打印"使用服务商：DeepSeek，模型：deepseek-chat"
3. **未配置拦截**: 清空用户所有场景配置 → 尝试 RAG 问答 → 前端弹提示"请先在设置页配置 AI 服务商和模型"
4. **管理员增删模型**: 管理员新增模型 `gpt-5` → 用户端下拉出现 `gpt-5` → 管理员禁用 `gpt-5` → 已配置用户不受影响，新用户不可见
5. **API Key 加密**: 直接查数据库 `user_ai_config` 表 → api_key 字段为密文 → 无法直接读取明文
6. **多场景独立配置**: 对话用 OpenAI gpt-4o，题目生成用千问 qwen-plus → 分别调用 AI → 日志确认不同服务商被调用
7. **ChatSessionServiceImpl 收口**: 聊天功能 → 确认调用走 `AiService.chat()` 而非自有 OpenAiClient
