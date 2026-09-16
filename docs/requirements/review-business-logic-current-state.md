# 复习业务当前状态梳理

> 更新时间：2026-09-16  
> 范围：Spring Boot 后端、网页端 Vue、Android Jetpack Compose 端  
> 目的：说明当前复习业务的真实执行逻辑，区分已实现行为、设计文档中的目标行为，以及当前会影响用户体验的缺口。

## 1. 结论先行

当前复习功能不是没有业务逻辑，而是已经形成了一个可运行的核心闭环：知识点可以生成复习卡片，用户可以获取待复习卡片并提交答案，服务端会判定对错、记录复习日志、更新掌握度、安排下一次复习并发放复习积分。

目前系统处在“旧卡片表兼容 + 工作区题目池/个人副本新模型并存”的过渡阶段。网页端和 Android 端都调用同一组 `/review/*` 接口，但两端消费的数据粒度不同：网页端使用复习概览、统计、题目池和沉浸式答题；Android 端只拉取今日卡片并在本地解析题干和选项。结果是核心业务共享，体验和边界行为并不完全一致。

最需要注意的现状是：

1. 认证是复习数据的前置条件。用户 JWT 过期或未带上时，后端无法得到 `userId`，当前代码应返回 401；此前日志中的 `userId=null` 会让查询得到 0 张卡片，表现为“今天没有复习内容”。
2. `review_card` 与 `review_card_pool + user_review_card` 两套模型同时参与查询、提交和统计，所有复习行为都要走兼容分支。
3. `getTodayReviewCards` 当前按 `status=0` 查询，但没有在服务层明确加入 `nextReviewTime <= now` 条件；“今日待复习”的接口语义与实现并不完全一致。
4. 提交答案后，代码先把卡片状态设为 `1`，再写入下一次复习时间。由于今日查询只取 `status=0`，原卡片会从下一次列表中消失；自动卡片还可能依赖 `ReviewJob` 按知识点时间重新生成一张新卡，手动卡片则没有同样的自动再入队路径，当前不是稳定的间隔重复闭环。
5. 网页端在提交后立即重新加载列表，Android 端则把当前列表保留到本轮结束，因此同一个后端状态问题会在两个端表现成不同的复习体验。

## 2. 业务对象和数据来源

### 2.1 旧模型：`review_card`

`review_card` 是早期个人卡片模型，一条记录同时保存题目内容和个人学习进度，主要字段包括：

- `user_id`、`workspace_id`：卡片归属范围；
- `node_id`：关联知识点；
- `question`、`answer`、`card_type`、`difficulty`：题目内容；
- `review_count`、`correct_count`、`incorrect_count`、`mastery_level`、`memory_strength`：学习进度；
- `last_review_time`、`next_review_time`：排期；
- `status`、`deleted`：状态和逻辑删除。

旧数据仍由 `ReviewCardServiceImpl` 查询和提交，目的是兼容存量数据。

### 2.2 新模型：题目池 + 个人副本

新模型拆成两层：

- `review_card_pool`：工作区级题目模板，保存题干、答案、题型、难度和创建者；
- `user_review_card`：用户对题目模板的个人副本，只保存个人复习次数、正确率、掌握度和排期。

工作区中生成卡片时，后端写入题目池，并自动为生成者创建一份个人副本。其他成员通过 `POST /review/pool/{poolId}/join` 创建自己的副本，彼此的进度互不影响。个人空间生成卡片时，可以直接创建 `pool_id = null` 的个人副本。

这与 `docs/prd/workspace-review-card-pool-prd.md` 中的目标设计一致，但当前实现仍保留旧表兼容查询，尚未完成单一数据访问层收口。

## 3. 当前真实业务链路

```text
登录 / 切换工作区
        ↓
认证拦截器从 JWT 写入 userId、workspaceId
        ↓
知识入库或“纳入复习”
        ↓
生成复习卡片（旧表或题目池 + 个人副本）
        ↓
GET /review/today
        ↓
网页端或 Android 展示题目
        ↓
POST /review/submit
        ↓
判定答案 → 更新卡片进度 → 写 review_log → 发放积分
        ↓
计算 nextReviewTime，并同步知识点掌握度
        ↓
刷新今日列表、统计和下一轮复习
```

### 3.1 卡片生成入口

当前有三类主要入口：

1. **确认知识入库时生成**：`PendingKnowledgeServiceImpl.confirmBatch()` 读取 `generateCards` 或待确认记录中的 `needReview`，为每个知识点生成一张自动卡片。
2. **知识页手动纳入复习**：`KnowledgeServiceImpl.toggleNeedReview()` 将知识点设置为复习目标，并在没有现有卡片时生成两张自动选择题；取消时会删除/归档该知识点的卡片。
3. **复习中心批量生成**：`POST /review/generate-all` 为范围内知识点循环生成卡片，每个知识点当前生成两张手动练习卡。

Quartz 旧任务 `ReviewJob` 还会扫描 `KnowledgeNode.nextReviewTime` 到期的知识点并调用自动生成逻辑。这个任务依赖旧的 `review_card` 查询和通知逻辑，和新题目池模型不是完全同一条链路。

### 3.2 获取今日卡片

Controller `GET /review/today` 读取请求属性中的 `userId` 和当前 `workspaceId`，调用 `ReviewCardServiceImpl.getTodayReviewCards()`，然后同时查询：

- `user_review_card`：用户、工作区、`status=0`、`is_archived=0`；
- `review_card`：用户、工作区、`status=0`、`deleted=0`。

两路结果合并后，Controller 只过滤 `question` 为空的脏数据，并按请求参数进行时间或难度排序。

这里有两个重要边界：

- 当前服务查询没有显式限制 `next_review_time <= 当前时间`。接口名称叫“今日复习”，但实现更接近“所有未完成状态的卡片”。
- 新旧表都可能返回数据，统计接口也会把两边相加。如果迁移期间同一知识点同时存在两种记录，用户可能看到重复题目，统计也可能重复计数。

另外，自动生成任务读取的是知识点的 `nextReviewTime`，不是同一张复习卡片的 `nextReviewTime`。这意味着自动复习实际上是“提交后更新知识点排期，到期时再生成题卡”，而不是“同一张卡片到期后重新入队”。

### 3.3 提交答案

`POST /review/submit` 的请求字段为 `cardId`、`userAnswer`、`duration`。服务端先按 ID 查询 `user_review_card`，查不到再回退到 `review_card`。

对新模型，服务端执行：

1. 校验个人副本的 `userId`；
2. 从题目池读取正确答案和题目内容；
3. 对选择题等内容做字符串忽略大小写、去首尾空格比较；
4. 累加 `reviewCount`，并累加正确或错误次数；
5. 用复习次数和平均正确率计算 `masteryLevel`、`memoryStrength`；
6. 写入 `lastReviewTime`，将 `status` 设为 `1`；
7. 写入 `review_log`；
8. 调用 `GamificationService.awardReviewPoints()` 发放积分；
9. 对 `generationType=auto` 的卡片更新排期，并同步知识点的复习次数、掌握度和下一次时间；
10. 返回 `isCorrect`、`correctAnswer`、`explanation` 和提示消息。

旧模型执行同样的进度更新和日志记录，只是直接写回 `review_card`。

### 3.4 排期算法

默认艾宾浩斯间隔在 `EbbinghausServiceImpl` 中为：

| 本次复习后的阶段 | 正确时默认间隔 |
|---|---:|
| 第 0 次后 | 1 天 |
| 第 1 次后 | 7 天 |
| 第 2 次后 | 14 天 |
| 第 3 次后 | 30 天 |
| 第 4 次及以后 | 60 天 |

答错时使用当前阶段间隔的一半，但不会短于 1 天。用户可以在网页端设置 3～8 个递增的间隔阶段；`ReviewPreferenceServiceImpl` 会在下一次答题时读取偏好。当前 `ReviewCardServiceImpl` 的提交逻辑仍直接调用 `EbbinghausService`，需要继续核对个人间隔偏好是否已经接入所有卡片分支。

## 4. 网页端当前行为

网页端入口是 `frontend/src/views/Review.vue`，调用 `frontend/src/api/review.js`。

### 4.1 列表页

页面加载时并行请求：

- `GET /review/today`：复习队列；
- `GET /review/overview`：今日待复习、已完成、准确率、连续天数、记忆持久度和分类统计。

页面提供：

- 我的复习计划；
- 工作区题目池；
- 新卡片、学习中、即将遗忘、已掌握筛选；
- 智能、时间、难度排序；
- 复习设置和知识点指定提醒；
- 空列表时的恢复复习题目操作。

### 4.2 答题页

网页端进入沉浸式答题视图后：

- 选择题选项由 `question` 文本本地解析；
- 简答/填空使用文本输入；
- 提交后展示正确/错误、正确答案和服务返回的解析；
- 本地保存已答题状态，支持上一题、下一题和题号跳转；
- 提交成功后会调用 `loadReviewCards()` 重新加载服务端列表。

重新加载会受到后端 `status=0` 查询条件影响：一张卡片提交后状态变为 1，它可能立即从列表中消失，但当前沉浸式页面仍保留旧的 `currentCard`。因此网页端可能出现“结果已显示，但队列长度、题号导航和完成按钮已经变化”的不一致。

### 4.3 网页端的额外能力

网页端已经接入或预留了以下复习周边能力：

- 复习节奏设置；
- 指定知识点提醒；
- 连续复习天数和准确率；
- 复习质量反馈；
- 工作区题目池、加入复习和重新加入；
- 游戏化积分与成长统计。

这些能力不代表所有端都已经消费了相同的接口或统计字段。

## 5. Android 端当前行为

Android 端的复习入口主要由：

- `TodayViewModel`：调用 `GET /review/today`，将空列表显示为“今天没有待复习卡片”；
- `ReviewViewModel`：管理本轮卡片、当前题目、输入答案、提交状态和服务端结果；
- `ReviewSessionScreen`：展示题干、选项、答案解析、记住这一点和查看原知识。

### 5.1 加载和认证

Android 的 `NetworkModule` 从 `SessionStore` 读取 JWT，自动加到 `Authorization: Bearer ...`。收到 401 后，会清除与失败请求匹配的 token。工作区切换接口会返回新的 token 和 `workspaceId`，两者一起保存到 `SessionStore`。

因此 Android 复习数据取决于两件事同时正确：

1. 本地保存的 token 未过期；
2. token 中的当前工作区与用户正在看的空间一致。

如果 JWT 已过期，后端不会得到有效用户身份，复习请求应直接失败为 401。此前日志中的：

```text
JWT expired ...
业务异常：请先登录
```

正是认证失效，不是“数据库没有复习卡片”。早期代码曾把无效身份继续带到查询层，产生 `userId=null` 和 `Total: 0`，这会掩盖真正的登录问题。

### 5.2 答题交互

Android 当前将 `ReviewCard.question` 当作可能混有选项和解析的历史文本，由 `ReviewPrompt.kt` 在本地解析题干、A-H 选项和解析区域。提交请求只发送卡片 ID、用户答案和答题耗时，解析和正确答案依赖后端返回的 `ReviewResult`。

这使 Android 可以在服务返回后展示解析，但它没有使用网页端的 `overview`、`nextReviewTime`、`masteryLevel`、`reviewCount` 等字段，也没有独立的题目池入口。当前 Android 是“轻量今日复习客户端”，不是完整的复习中心。

### 5.3 与网页端的差异

| 能力 | 网页端 | Android 端 | 当前影响 |
|---|---|---|---|
| 今日卡片 | `/review/today` + 概览 | `/review/today` | 核心数据源相同，但认证/工作区异常会同时影响两端 |
| 统计 | 概览、准确率、连续天数、分类 | 未消费概览 | Android 首页可能无法解释“为什么是 0 张” |
| 答题解析 | 服务解析 + 网页本地题干解析 | 服务解析 + Android 本地题干解析 | 历史题目文本格式变化时，两端可能解析不同 |
| 提交后刷新 | 提交后立即重新拉列表 | 本轮保留内存列表，重新进入再拉 | 同一后端状态问题会表现成不同队列行为 |
| 题目池 | 已有列表、加入、重新加入 | 未接入 | 工作区复习体验不一致 |
| 复习设置/提醒 | 已有网页入口 | API 模型存在，页面能力较少 | 偏好和提醒的移动端闭环不完整 |
| 原知识回看 | 可从网页题目关联知识点 | 结果后支持查看原知识 | Android 体验相对完整，但依赖 `nodeId` 返回 |

## 6. 当前阶段已确认的问题

### P0：认证失效会被误认为没有复习内容

复习接口依赖 JWT 中的用户身份和当前工作区。过期 token 会返回 401；如果客户端没有正确处理，页面可能落到空态或通用“加载失败”。这解释了日志中 `userId=null`、两张表查询总数为 0 的现象。

### P0：卡片状态与到期时间没有形成一致的队列规则

提交时卡片被设为 `status=1`，但今日查询只取 `status=0`；同时今日查询没有明确按 `nextReviewTime <= now` 过滤。当前字段语义同时承担“是否完成当前轮次”和“是否已经掌握/是否继续进入队列”两种职责，导致下一轮复习可能无法按时间重新出现。

### P1：新旧表并行查询会带来重复和统计口径差异

列表、待复习数量、已完成数量、准确率和连续天数都分别查询新旧表并相加。存量迁移、重复生成或旧任务继续写入时，可能出现重复题目、数量偏大或同一知识点进度不一致。

### P1：自动生成任务和新题目池模型没有完全统一

`ReviewJob` 仍使用旧 `review_card` Mapper 查询每日通知和周报；新模型的个人副本主要由 `ReviewCardServiceImpl` 处理。工作区题目池的生成和定时自动生成并不是同一个来源，后续容易出现“题目池里没有，但个人复习里有”的差异。

### P1：网页端提交后重新加载破坏当前复习会话

网页端提交成功后立即调用 `loadReviewCards()`，而当前列表查询会受卡片状态变化影响。结果展示、题号导航、已答统计和队列数量可能来自不同时间点。

### P1：Android 端缺少复习业务上下文

Android 当前卡片模型只保留 ID、知识点、题目、答案和题型，没有消费掌握度、复习次数、下次时间和概览统计。因此用户无法在移动端判断卡片为什么今天出现、答完后何时再来，也无法看到完整的复习进度。

### P2：题目内容和解析仍依赖文本约定

网页端和 Android 端都需要从 `question` 字符串中解析选项和解析文本。AI 题目格式稍有变化，就可能出现题干为空、选项识别失败或答案解析展示不一致。后端返回的 `explanation` 目前还会从题目文本中抽取，而不是稳定的结构化字段。

### P2：提醒、每日队列和卡片排期是三个相邻但未完全统一的概念

系统同时存在卡片 `nextReviewTime`、知识点 `nextReviewTime`、知识点指定提醒和每日通知任务。它们服务于不同场景，但用户界面容易把它们都理解成“今天要复习的内容”。当前需要明确哪一个是队列真相，哪一个只是通知。

## 7. 目前可以对外说明的业务规则

在不改变代码前，团队可以把当前复习定义为：

1. 只有已登录用户才能查询和提交复习；工作区由当前 JWT 上下文决定。
2. 知识点是否纳入复习由 `needReview` 控制，但它不是复习队列本身；真正决定答题记录的是复习卡片或个人副本。
3. 工作区卡片优先采用题目池模板 + 用户个人副本；个人空间可以直接使用个人副本；历史卡片仍从旧表兼容读取。
4. 复习提交是一次不可忽略的业务事件，会更新进度、写日志、触发积分，并尝试更新下一次复习时间。
5. “今天待复习”应该按用户、工作区、未归档/未删除、已到期的个人卡片计算；当前代码还没有把这条规则在所有入口统一落实。
6. 复习统计应以 `review_log` 为主要事实来源，卡片上的累计字段用于当前进度和兜底；当前代码已经部分采用这个方向，但新旧表相加仍需治理。

## 8. 后续重构时的优先顺序

这次梳理不直接修改实现，建议后续按下面顺序收口：

1. **先定义唯一队列规则**：明确 `status`、`nextReviewTime`、`isArchived/deleted` 的职责，确定“到期卡片”的唯一 SQL 条件。
2. **再统一数据访问层**：由一个 ReviewCardRepository/查询服务隐藏旧表兼容逻辑，避免 Controller、统计、定时任务各自拼新旧查询。
3. **统一提交结果契约**：返回结构化题型、正确答案、解析、下一次复习时间、掌握度和本次统计，网页和 Android 不再自行猜测文本格式。
4. **把复习会话与队列快照分开**：开始一轮复习时固定卡片 ID 列表，提交后只更新单卡结果；本轮结束或重新进入时再刷新队列。
5. **补齐认证和工作区失败态**：401 必须显示“登录已过期并引导重新登录”，工作区切换后必须等待新 token 生效再拉复习数据。
6. **补齐跨端验收**：至少验证个人空间、工作区、旧卡片、新副本、答对、答错、到期重新出现、重复提交、token 过期和切换工作区。

## 9. 建议的验收场景

| 场景 | 应验证的事实 |
|---|---|
| 新用户登录后打开复习 | token 有效，返回当前空间的卡片或明确空态 |
| token 过期后打开复习 | 两端均显示登录失效，不显示“没有卡片” |
| 个人空间生成卡片 | 个人副本可查询、可提交、可再次排期 |
| 工作区生成卡片 | 题目池有模板，创建者有个人副本，成员加入后有独立进度 |
| 答对一次 | 日志、次数、正确数、掌握度、下一次时间均更新 |
| 答错一次 | 错误数增加，下一次时间按缩短规则计算 |
| 到期后再次查询 | 卡片按唯一到期规则重新进入队列 |
| 网页提交后继续本轮 | 当前题目结果和队列快照不跳变 |
| Android 提交后重新进入 | 与网页看到同一张卡片的状态和排期 |
| 多端交替复习 | 统计以服务端日志为准，不重复计数 |

## 10. 证据文件

- 后端接口与兼容分支：`backend/src/main/java/com/secondbrain/controller/ReviewCardController.java`、`backend/src/main/java/com/secondbrain/service/impl/ReviewCardServiceImpl.java`
- 排期算法：`backend/src/main/java/com/secondbrain/service/impl/EbbinghausServiceImpl.java`
- 题目池：`backend/src/main/java/com/secondbrain/service/impl/ReviewCardPoolServiceImpl.java`
- 知识点纳入复习：`backend/src/main/java/com/secondbrain/service/impl/KnowledgeServiceImpl.java`
- 知识入库生成卡片：`backend/src/main/java/com/secondbrain/service/impl/PendingKnowledgeServiceImpl.java`
- 网页端：`frontend/src/views/Review.vue`、`frontend/src/api/review.js`
- Android 端：`android/app/src/main/java/com/secondbrain/android/review/ReviewViewModel.kt`、`android/app/src/main/java/com/secondbrain/android/ui/ReviewScreen.kt`、`android/app/src/main/java/com/secondbrain/android/data/remote/SecondBrainApi.kt`
- 设计目标参考：`docs/prd/workspace-review-card-pool-prd.md`、`docs/04_code_walkthrough.md`、`docs/API_REFERENCE.md`
