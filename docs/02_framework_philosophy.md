> 本篇讲清楚 AI-SecondBrain 为什么这样设计，每个设计决策背后的问题、解法和权衡。

---

## 1. 核心哲学

**让 AI 对话中的知识不再流失，通过科学复习转化为长期记忆。**

这是贯穿整个项目的设计北极星。所有功能——对话采集、知识提取、复习卡片、游戏化激励——最终都指向同一个目标：把碎片化的 AI 对话内容，沉淀为用户真正记住、能用的长期知识。

---

## 2. 设计思想详解

### 思想 1：分层架构 —— 关注点分离

#### 问题
如果所有代码混在一起（HTTP 处理、业务逻辑、数据库操作全写在一个类里），改一个功能要翻遍整个项目。新人入职不知道从哪看起，改个 bug 容易牵一发而动全身。

#### 解法
采用经典三层架构，每一层只做自己的事：

```
┌─────────────────────────────────────────────┐
│           Controller 控制层                  │
│   只处理 HTTP 协议：参数校验、响应封装        │
├─────────────────────────────────────────────┤
│           Service 业务层                     │
│   只写业务逻辑：知识创建、复习计算、积分发放   │
│   （先定义接口，再写 Impl 实现类）            │
├─────────────────────────────────────────────┤
│           Mapper 数据层                      │
│   只操作数据库：SQL 编写、单表 CRUD           │
└─────────────────────────────────────────────┘
```

**代码示例 —— Service 接口与实现分离：**

接口定义（只声明"做什么"）：

```java
// file:///D:/AI-SecondBrain/backend/src/main/java/com/secondbrain/service/KnowledgeService.java
public interface KnowledgeService {
    KnowledgeNodeVO create(String title, String summary, String contentMd,
                           Integer importance, Long userId, Long workspaceId);

    void deleteById(Long id, Long userId, Long workspaceId);

    List<KnowledgeNodeVO> search(String keyword, Long userId, Long workspaceId);
    // ... 更多方法
}
```

实现类放在 `service/impl/` 目录下，具体写"怎么做"。

#### 价值
- **新人友好**：看 Controller 就知道有哪些 API，看 Service 就懂业务逻辑
- **修改安全**：改业务逻辑只动 Service 层，换数据库只动 Mapper 层，互不干扰
- **便于测试**：每一层都可以单独写单元测试

#### 代价
多写了一层接口类。对于只有两三个接口的小项目，可能显得"重"。但随着功能增长（这个项目已有知识、复习、对话、工作区等十多个模块），分层的收益会越来越明显。

---

### 思想 2：统一响应格式 —— 前后端契约

#### 问题
如果每个接口返回格式不一样——有的返回 `{status, data}`，有的直接返回数组，有的报错返回纯文本——前端对接每个新接口都要写一套新的判断逻辑，累且容易出错。

#### 解法
所有 API 都返回 `Result<T>` 统一格式，包含三个字段：
- `code`：状态码，200 成功，500 失败
- `message`：响应消息
- `data`：响应数据

**代码示例 —— Result 统一响应类：**

```java
// file:///D:/AI-SecondBrain/backend/src/main/java/com/secondbrain/common/Result.java
@Getter
@Setter
public class Result<T> implements Serializable {
    private Integer code;      // 状态码：200成功，500失败
    private String message;    // 响应消息
    private T data;            // 响应数据

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}
```

前端配合 `request.js` 拦截器统一处理：code=200 直接返回 data，否则弹窗报错。

```javascript
// file:///D:/AI-SecondBrain/frontend/src/utils/request.js
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code === 200) {
      return res.data;          // 成功直接返回数据，调用方省心
    } else {
      ElMessage.error(res.message || "请求失败");
      return Promise.reject(new Error(res.message || "请求失败"));
    }
  },
  // ...
);
```

#### 价值
- **前端零心智负担**：对接新接口不用考虑错误处理，直接拿返回值用
- **统一错误提示**：所有报错走同一套弹窗逻辑，体验一致
- **便于调试**：看 code 和 message 就知道哪里出了问题

#### 代价
即使最简单的接口（比如返回一个布尔值）也要包一层 Result。但这点冗余换来了前后端协作的顺畅，非常划算。

---

### 思想 3：JWT 无状态认证 —— 水平扩展友好

#### 问题
传统 Session 认证需要服务器在内存里存 session 数据。如果部署多台机器，用户请求落到不同机器上就要同步 session，麻烦且容易出问题。移动端 App 也不好带 session cookie。

#### 解法
使用 **JWT（JSON Web Token）** 做无状态认证：

```
用户登录 → 后端生成 token（含 userId、role、workspaceId）→ 返回前端
   ↑                                                    ↓
   └──── 前端存 localStorage，每次请求带在 Header ──────┘
           Authorization: Bearer <token>
```

**代码示例 —— JwtInterceptor 拦截器：**

```java
// file:///D:/AI-SecondBrain/backend/src/main/java/com/secondbrain/interceptor/JwtInterceptor.java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String token = request.getHeader("Authorization");

    if (token != null && token.startsWith("Bearer ")) {
        token = token.substring(7);
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                request.setAttribute("userId", userId);
                // 还会解析 role、currentWsId 并存入 request
            }
        } catch (Exception e) {
            log.error("JWT 解析失败：{}", e.getMessage());
        }
    }

    return true;  // 始终放行，由业务层判断是否需要登录
}
```

注意一个设计细节：**拦截器始终放行，不直接拦截未登录请求**。是否需要登录由具体的 Service 或 Controller 决定，给了业务更大的灵活性（比如有些公开接口不需要登录）。

密码加密用 Spring Security 提供的 BCrypt 算法。

#### 价值
- **后端无状态**：随便加机器扩容，不用考虑 session 同步
- **移动端友好**：App 也能方便地携带 token
- **信息自包含**：token 里自带 userId、role、workspaceId，不用额外查库

#### 代价
token 一旦签发，服务器无法主动让它失效（只能等过期）。如果用户改密码或被踢下线，旧 token 可能还能用一段时间。解决方案是设置较短的过期时间（比如 2 小时）+ 刷新 token 机制。

---

### 思想 4：工作区隔离 —— 多租户数据安全

#### 问题
不同用户、不同团队的知识混在一起，万一 SQL 漏写条件，A 就能看到 B 的数据，这在知识管理产品里是致命问题。

#### 解法
引入 **Workspace（工作区）** 概念，所有业务数据都带 `workspace_id` 字段：

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Workspace A │     │  Workspace B │     │  Workspace C │
│  知识/复习/对话 │     │  知识/复习/对话 │     │  知识/复习/对话 │
└──────────────┘     └──────────────┘     └──────────────┘
       ↑                    ↑                    ↑
       └────────────────────┼────────────────────┘
                      用户可以属于多个工作区
```

**角色权限体系（从高到低）：**

| 角色 | 权限 |
|------|------|
| OWNER（所有者） | 完全控制，可删除工作区 |
| ADMIN（管理员） | 可增删改内容，管理成员 |
| EDITOR（编辑者） | 可编辑内容，不可管理成员 |
| VIEWER（查看者） | 只能看，不能改 |

**代码示例 —— WorkspaceInterceptor 工作区权限校验：**

```java
// file:///D:/AI-SecondBrain/backend/src/main/java/com/secondbrain/interceptor/WorkspaceInterceptor.java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // 只拦截 /api/workspace/** 路径
    if (!requestUri.startsWith("/api/workspace/")) {
        return true;
    }

    // 1. 校验是否登录
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) { /* 返回401 */ }

    // 2. 校验工作区是否存在
    Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
    if (workspace == null || workspace.getDeleted() == 1) { /* 返回404 */ }

    // 3. 校验是否为工作区成员
    WorkspaceMember member = workspaceService.getMemberByWorkspaceAndUser(workspaceId, userId);
    if (member == null) { /* 返回403 */ }

    // 4. 按 HTTP 方法校验角色权限
    //    GET 请求：所有成员可访问
    //    POST/PUT/DELETE：仅 admin/owner 可访问
    if (isWriteMethod(method) && !WorkspaceRole.isAdmin(member.getRole())) {
        /* 返回403 */
    }

    request.setAttribute("workspaceId", workspaceId);
    request.setAttribute("memberRole", member.getRole());
    return true;
}
```

配合 Service 层：每个方法参数都带 `workspaceId`，查询时必加 `workspace_id` 条件，形成"双保险"。

#### 价值
- **数据天然隔离**：不会出现 A 看到 B 的知识
- **权限统一管控**：所有工作区接口走同一套拦截器，不会漏
- **支持团队协作**：一个用户可加入多个工作区，不同工作区不同角色

#### 代价
每个查询都要带 `workspaceId` 参数，SQL 都要多一个条件。写代码时稍繁琐，但换来了数据安全的底线保障。

---

### 思想 5：异步解耦 —— Kafka + @Async 双机制

#### 问题
AI 对话采集、知识提取、报告生成这些操作很耗时（几秒到几分钟不等）。如果同步等待，用户界面会卡死，体验极差。而且高峰期可能把服务器打满。

#### 解法
分两档异步策略，按需选用：

| 场景 | 机制 | 示例 |
|------|------|------|
| 轻量异步（秒级） | Spring `@Async` | 向量生成、关系推荐 |
| 重量级异步（分钟级） | Kafka 消息队列 | 对话采集、报告生成 |

异步任务统一用 `AsyncTask` 表追踪状态，前端可以轮询查询进度。

```
用户提交任务 → 创建 AsyncTask 记录（状态=PENDING）→ 发消息到 Kafka
                                                           ↓
                                                    消费者慢慢处理
                                                           ↓
                                                更新 AsyncTask 状态=SUCCESS/FAILED
                                                           ↑
用户前端 ←—————————————— 轮询查询任务状态 ————————————————┘
```

**相关文件：**
- `AsyncTask.java` —— 异步任务实体
- `AsyncTaskController.java` —— 任务状态查询接口
- `AsyncTaskConsumerService.java` —— Kafka 消费者

#### 价值
- **用户体验好**：提交后立即返回，不用等 AI 跑完
- **系统弹性高**：高峰期消息堆积在 Kafka，消费者慢慢消费，不会打挂服务器
- **可追溯**：每个任务都有记录，出问题能查到状态和错误日志

#### 代价
调试更复杂——出了问题要查消息有没有发出去、有没有被消费、消费时报了什么错。不能像同步代码那样一步步打断点。

---

### 思想 6：可插拔组件 —— 可选服务不阻塞主流程

#### 问题
Elasticsearch、Kafka、邮件服务这些不是每个部署环境都有。个人用户可能只想装个 MySQL 就跑起来，不能因为缺一个组件整个应用就启动失败。

#### 解法
配置文件里有 `enabled` 开关，配合 Spring 的条件注解实现"有就用，没有就降级"：

```
KAFKA_ENABLED=true/false
ES_ENABLED=true/false
MAIL_ENABLED=true/false
```

**代码示例 —— NoOpElasticsearchService 空实现：**

```java
// file:///D:/AI-SecondBrain/backend/src/main/java/com/secondbrain/service/impl/NoOpElasticsearchService.java
@Service
@ConditionalOnMissingBean(name = "elasticsearchServiceImpl")
public class NoOpElasticsearchService implements ElasticsearchService {

    @Override
    public void syncKnowledgeNode(KnowledgeNode node) {
        log.debug("Elasticsearch未配置，跳过同步知识节点，nodeId：{}", node.getId());
    }

    @Override
    public List<KnowledgeDocument> search(String keyword, Long userId) {
        log.debug("Elasticsearch未配置，返回空搜索结果，keyword：{}，userId：{}", keyword, userId);
        return Collections.emptyList();
    }

    // ... 其他方法全是空实现
}
```

`@ConditionalOnMissingBean` 的意思是：如果已经有了 `elasticsearchServiceImpl` 这个 Bean（说明 ES 启用了），就不创建我；如果没有（ES 没启用），就用我这个空实现顶上去。

**降级策略：**

| 组件 | 启用时 | 关闭时（降级） |
|------|--------|---------------|
| Elasticsearch | ES 全文搜索 | 只用 MySQL 模糊搜索 |
| Kafka | 异步消息处理 | 同步处理（慢但能用） |
| Mail | 邮件通知 | 不发邮件，不影响主功能 |

#### 价值
- **最小化部署依赖**：最简版只需要 MySQL 就能跑起来
- **渐进式增强**：需要更强功能时再开启对应组件
- **避免单点故障**：某个组件挂了不影响整个系统

#### 代价
多写了一套空实现，代码量稍增。而且降级后的功能体验会打折扣（比如 MySQL 搜索不如 ES 精准），需要用户有预期。

---

### 思想 7：艾宾浩斯复习 —— 科学记忆

#### 问题
学了就忘是人的天性。知识沉淀了一大堆，但用户从来不复习，最后还是记不住，等于白沉淀。

#### 解法
基于**艾宾浩斯遗忘曲线**算法，动态调整复习间隔：
- 答对了 → 间隔拉长（记得更牢了，晚点再复习）
- 答错了 → 间隔缩短（还没记住，得赶紧复习）

**代码示例 —— EbbinghausServiceImpl 复习间隔计算：**

```java
// file:///D:/AI-SecondBrain/backend/src/main/java/com/secondbrain/service/impl/EbbinghausServiceImpl.java
private static final long[] REVIEW_INTERVALS = {
    1440,      // 第0次复习后：1天（1440分钟）
    10080,     // 第1次复习后：7天
    20160,     // 第2次复习后：14天
    43200,     // 第3次复习后：30天
    86400      // 第4次复习后：60天
};

@Override
public long calculateNextReviewInterval(int reviewCount, boolean isCorrect) {
    int index = Math.min(reviewCount, REVIEW_INTERVALS.length - 1);

    if (isCorrect) {
        return REVIEW_INTERVALS[index];           // 答对了：按计划推进
    } else {
        return Math.max(REVIEW_INTERVALS[0], REVIEW_INTERVALS[index] / 2);
    }                                             // 答错了：间隔减半，退回去重学
}
```

配合定时任务，每天凌晨自动生成当天待复习的卡片，用户打开 App 就能看到"今天需要复习 X 张卡片"。

#### 价值
- **科学高效**：用最小的复习量达到最好的记忆效果，不浪费时间
- **个性化**：每个人、每张卡片的复习节奏都不一样，因材施教
- **被动受益**：用户只要跟着系统安排走就行，不用自己规划复习

#### 代价
复习算法的参数调优需要经验——间隔太密用户嫌烦，太松又记不住。目前用的是经典艾宾浩斯参数，后续可以根据用户数据持续优化。

---

### 思想 8：游戏化激励 —— 让学习上瘾

#### 问题
纯工具型产品用户粘性低。用户装了 App，新鲜劲过了就不用了，知识沉淀和复习都无从谈起。

#### 解法
引入一套游戏化激励体系，让学习像打怪升级一样有成就感：

| 模块 | 作用 | 相关实体 |
|------|------|---------|
| 积分系统 | 学习、复习、分享都赚积分 | `PointsLog` |
| 成就系统 | 完成特定目标解锁徽章 | `Achievement` / `UserAchievement` |
| 排行榜 | 每日/每周排名，激发竞争欲 | - |
| 连续签到 | 培养每日使用习惯 | `DailyCheckIn` |

**相关实体文件：**
- `PointsLog.java` —— 积分流水记录
- `Achievement.java` —— 成就定义
- `DailyCheckIn.java` —— 每日签到记录

设计思路是：用短期反馈（积分+1、成就解锁）驱动长期行为（持续学习、持续复习）。

#### 价值
- **提升留存**：用户有动力每天打开 App
- **增强参与感**：解锁成就、上榜排名带来成就感
- **形成习惯**：连续签到机制培养使用惯性

#### 代价
游戏化设计要把握好平衡。如果激励过度，用户可能会为了刷积分而"假学习"（比如快速乱点复习卡片），反而偏离了"真正记住知识"的初衷。所以积分规则设计要引导正确行为，而不是鼓励刷量。

---

### 思想 9：多端一致但不强行复用 —— 一次后端，五端个性体验

#### 问题
知识管理是跨场景的：PC 端适合深度整理知识、手机端适合碎片复习和快速记笔记、微信小程序适合分享和低频使用、浏览器扩展适合在 AI 对话页面"顺手采集"。如果只做 Web 端，很多场景覆盖不到；如果每端都重写一套 UI 和 API，维护成本爆炸。

#### 解法
**后端只写一套**（所有端共享 `/api/*` 契约、JWT、工作区权限），但**每端的交互独立设计**，不强行复用前端组件。类比：同一家餐厅，堂食有菜单和服务员、外卖有打包盒和 App 点餐——菜是同一份（后端），餐具和点餐流程按场景适配。

```
               ┌──────────────────────────────────────────┐
               │        统一后端 /api/*（Spring Boot）      │
               │  JWT 鉴权 · 工作区 RBAC · Result<T> 格式  │
               └──────────┬──────────┬──────────┬──────────┘
                          │          │          │
         深度整理/研究     │          │          │   AI平台顺手采集
     ┌──────────────┐     │          │          │  ┌──────────────┐
     │  Web Vue 3   │◄────┘          │          └─►│ Chrome 扩展   │
     │  30+ 页面    │                │             │ content.js   │
     │  侧边栏+仪表板│                │             │ popup 面板   │
     └──────────────┘                │             └──────────────┘
                                     │
                          碎片复习/快速笔记        Python 微服务
         ┌──────────────────────────┐  │     ┌───────────────────┐
         │  uni-app 多端             │  └────►│  DeerFlow (Flask) │
         │  pages.json  TabBar       │        │  长文报告生成     │
         │  mobile/src/pages/ 25+页  │◄───────┤  深度研究         │
         │  产物：H5/小程序/App      │  用户API_key 透传        │
         └──────────────────────────┘         └───────────────────┘
```

**关键设计细节：**
1. **Token 三载体**：Web 端存 `localStorage`，移动端用 `uni.setStorageSync`，扩展用 `chrome.storage.local`（因为扩展 Service Worker 无 localStorage）。
2. **工作区在移动端的呈现形式不同**：Web 端是顶栏 `WorkspaceSwitcher` 下拉；移动端屏幕窄，换成 `WorkspaceDrawer` 抽屉滑出。
3. **扩展不直连 Nginx**：它是 Chrome 里的 JS，直接请求 `http://localhost:8080` 或生产域名，走 `host_permissions` 白名单（`extension/manifest.json:7`）。
4. **DeerFlow 走内部 HTTP**：不对外直接暴露，用户请求先到 Java 后端 → 后端鉴权后调 `http://deerflow:8000`（docker-compose 内网），这样 DeerFlow 不需要独立处理登录。

#### 价值
- **一套后端，五端复用**：新功能只需写一次后端 + 每端写一次 UI 适配，不用重复写业务逻辑
- **体验不妥协**：每端按自己的交互习惯设计，不会出现"把 Web 端硬塞进手机"的违和感
- **安全边界清晰**：扩展和 DeerFlow 都通过后端鉴权网关，不直接暴露核心 API

#### 代价
每端都要独立做 UI 联调和回归测试，5 端 × 新功能 = 工作量约等于 2 端 Web 开发。如果功能非常简单，可能"重写一套 UI"比"抽出跨端组件"更划算。

---

### 思想 10：Research Agent 预算硬约束 —— 防止 AI 无限循环和烧钱

#### 问题
Agent 自主研究一旦放开约束，有两种典型故障：
1. **无限自循环**：Planner → KnowledgeAgent → GapAgent → Planner… 无休止往复，用户等到天荒地老。
2. **token 爆炸**：WebFetch 抓了 50 篇长文全部塞给 LLM，单次调用上万美元账单。

这不是"bug"，是 Agent 的**天然行为倾向**——缺了边界约束它就会乱跑。`AGENTS.md` 中"AI Research Agent 开发约束"第 10~12 条（禁止无限 Agent Loop、禁止单 Agent 无限调 Tool、禁止单 Research Task 无限重试）就是为了管住它。

#### 解法
在 `ResearchOrchestrator` 里挂三条**独立预算**，任何一条超了立即终止，并把状态写回 `ResearchProject`：

```
  ResearchOrchestrator.start()
       │
       ├─► AgentContext 初始化 3 条预算：
       │    ├── TokenBudget       max 60000 token（总消耗）
       │    ├── ToolCallBudget    max 60 次工具调用
       │    └── TimeBudget        max 600000 ms（10 分钟超时）
       │
       ▼  每次 Agent / Tool 执行后扣减
       │
       │  if (任一预算超支) {
       │      标记项目 status = FAILED_BUDGET_EXCEEDED;
       │      写 ResearchHistory 记录超支点;
       │      return;
       │  }
```

**真实代码（research/orchestrator/ResearchOrchestrator.java:55）**：

```java
public ResearchOrchestrator(...,
    @Value("${research.budget.max-tokens:60000}") long maxTokens,
    @Value("${research.budget.max-tool-calls:60}") int maxToolCalls,
    @Value("${research.budget.project-timeout-ms:600000}") long projectTimeoutMs) { ... }
```

配合 `ResearchQualityGate` 评审：如果 CriticAgent 发现输出质量不达标，允许**有限次数**重试，但不能超过预算。

#### 价值
- **稳定性有兜底**：无论 LLM 怎么胡来，项目一定在 10 分钟/60000 token 内结束
- **成本可控**：单用户的一次研究账单有上限，不会出现"月底吓死"的情况
- **可观测**：预算消耗写入 ResearchHistory，事后可以调参数（比如让重度研究放宽到 120k token）

#### 代价
约束过严会让深度研究"意犹未尽"——比如还没查到核心资料就因 token 用尽被切了。需要通过分档（普通研究/深度研究）给用户选择权，而不是一套参数套所有场景。

---

### 思想 11：知识分级 + PendingKnowledge 中间态 —— 不替用户做决定

#### 问题
AI 自动采集对话 → 直接写入 knowledge_node 表 → 用户回来发现一堆自己不想留的内容，还得逐条删除。信任一旦被破坏，用户就再也不敢开自动采集了。

#### 解法
所有"AI 推荐的知识"（对话采集、DeerFlow 提炼、RAG 关联推荐）**先落到 `pending_knowledge` 表**，给用户一个"确认 / 修改 / 丢弃"的过程：

```
 extension content.js 采集对话
         │ POST /capture
         ▼
 KnowledgeCaptureService.createPending()
         │  AI 抽摘要、重要性、标签建议
         ▼
 pending_knowledge 表（中间态）
         │
         │  用户在"待确认"列表里操作：
         │    ✅ 确认入库 → 转 knowledge_node + 发积分
         │    ✏️ 编辑后确认 → 同上
         │    🗑 丢弃 → 仅删除 pending 记录
         ▼
 knowledge_node（正式知识）
```

这和邮件"垃圾邮件夹"、微信"好友申请"是同一个道理——**推荐类操作必须经用户确认**，AI 只能建议不能代替决定。

**相关文件：**
- 表结构：`sql/V8__add_pending_knowledge.sql`
- Entity：`entity/PendingKnowledge.java`
- Service：`PendingKnowledgeService.java`
- Controller：`CaptureController.java`（采集入口）+ `KnowledgeController` 的 pending 接口

#### 价值
- **用户掌控感强**：知识库是"我的"，不是"AI 随便塞的"
- **AI 可以更激进**：反正走中间态，AI 多推荐几条也不会污染主库
- **采集场景体验完整**：从 Chrome 里一键采集 → 回到 Web/移动端"待确认"二次处理 → 入库，形成闭环

#### 代价
采集到入库多了一步确认，部分用户嫌麻烦。后续可以加"工作区级信任阈值"：如果 AI 推荐最近 N 条用户都没改，就允许它自动入库一小部分，但必须保留"一键撤销"。

---

### 思想 12：知识广场与问答社区 —— 公私双空间，信任第一

#### 问题
知识如果只存私人工作区，用户只能"闭门造车"，没法借别人的知识、没法提问、也没动力产出精品内容。但如果公开和私有混在同一张表，很容易出现"把私有笔记误发公开区"的事故。

#### 解法
**物理分表**，而非"加一个 is_public 标记"——公开内容和私有内容是两种完全不同的业务：

| 空间 | 表 / 实体 | 权限模型 | 信任机制 |
|-----|----------|---------|---------|
| 私人空间 | knowledge_node（workspace 隔离） | 工作区 RBAC | 仅成员可见 |
| 知识广场 | square_post + square_post_node | 发布后任何登录用户可看 | 点赞/收藏/评论/举报 + 敏感词过滤 + 管理员处理 |
| 问答社区 | community_question + community_answer | 提问者采纳、关注、用户资料 | 关注关系 + CommunityUserProfile 声誉 |

**物理分表的好处**：即使 SQL 写错，`SELECT * FROM square_post` 也绝不会查出某人私人知识。这和"工作区双保险"思路一脉相承——安全永远靠架构不靠"记得加条件"。

**相关文件：**
- 广场：`SquareService` / `SquarePost` / `SquareController`
- 社区：`CommunityQuestionService` / `CommunityUserService` / `CommunityQuestionController`
- 举报/审核：`SquareReport` / `SensitiveWord` / `controller/admin/AdminAiProviderController.java`（管理员入口）

#### 价值
- **数据安全底线**：公有/私有物理分离，杜绝"私有笔记被公开"
- **社交属性驱动内容**：公开区让用户有动力写精品、回答问题
- **信任机制可迭代**：举报、敏感词、积分激励都可以单独调，不影响私有功能

#### 代价
同一份知识从"私人笔记→公开广场"需要显式"发布"流程，用户要走 SquarePublishRequest；广场内容无法自动反向同步回私人知识库（因为担心版本分叉），后续可以做"一键另存为我的笔记"补齐。

---

## 3. 设计思想汇总表

| 序号 | 设计思想 | 解决的问题 | 核心手段 | 主要代价 |
|------|---------|-----------|---------|---------|
| 1 | 分层架构 | 代码混乱，新人难上手 | Controller → Service → Mapper 三层分离 | 多写一层接口类 |
| 2 | 统一响应格式 | 前后端对接繁琐 | Result\<T\> 统一封装 + 前端拦截器 | 简单接口也要包一层 |
| 3 | JWT 无状态认证 | 多机部署 session 同步难 | 令牌自包含，服务端无状态 | token 无法主动失效 |
| 4 | 工作区隔离 | 多租户数据安全 | workspace_id + 拦截器 + 角色权限 | 每个查询都要带 workspaceId |
| 5 | 异步解耦 | AI 操作耗时阻塞 | @Async 轻量 + Kafka 重量级 | 调试复杂度上升 |
| 6 | 可插拔组件 | 部署环境依赖不统一 | enabled 开关 + 空实现降级 | 多写一套空实现 |
| 7 | 艾宾浩斯复习 | 学了就忘 | 遗忘曲线算法 + 动态间隔 | 参数调优需要经验 |
| 8 | 游戏化激励 | 用户粘性低 | 积分 + 成就 + 排行榜 + 签到 | 过度激励导致刷量 |
| 9 | 多端一致不强行复用 | 场景覆盖不足+维护爆炸 | 一套后端 / 每端独立 UI / 共享契约 | 多端 UI 联调成本 |
| 10 | Agent 预算硬约束 | 无限循环 / token 爆炸 | Token/Time/ToolCall 三条独立预算 | 深度研究可能"意犹未尽" |
| 11 | PendingKnowledge 中间态 | AI 乱塞污染知识库 | pending_knowledge 表 + 用户二次确认 | 用户多一步操作 |
| 12 | 公私双空间物理分表 | 私人笔记被公开的安全风险 | square_post / community_question 独立表 | 发布流程稍繁琐 |

---

> **速记口诀**：三层清晰响应省，JWT 扩容工作区安；异步解耦可插拔，艾宾浩斯记又玩；五端个性预算兜，待确中间双空间。
