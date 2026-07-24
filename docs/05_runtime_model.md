> 搞清楚程序跑起来后有哪些线程/进程在干活，它们之间怎么配合，哪些地方要注意并发安全。

## 0. 全景图

先把后端 JVM 进程和前端浏览器进程里的所有线程都画出来，建立整体印象：

```
┌──────────────────────────────────────────────────────────────────────┐
│                     后端 JVM 进程（Spring Boot）                      │
│                                                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                │
│  │  Tomcat     │  │  @Async     │  │  Quartz     │                │
│  │  线程池      │  │  线程池x3    │  │  调度线程    │                │
│  │  (200个)     │  │  DeerFlow    │  │  (定时任务)  │                │
│  │             │  │  Report      │  │             │                │
│  │ 处理HTTP请求 │  │  Vector      │  │ 复习/排行榜   │                │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘                │
│         │                │                │                       │
│  ┌──────▼────────────────▼────────────────▼──────┐                │
│  │                                              │                │
│  │         Spring 上下文（所有 Bean 共享）              │                │
│  │                                              │                │
│  └──────┬────────────────┬────────────────┬──────┘                │
│         │                │                │                       │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐                │
│  │ Kafka      │  │ WebSocket  │  │ 连接池线程   │                │
│  │ 消费者线程  │  │ 连接线程    │  │             │                │
│  │ (逐条消费)  │  │ (STOMP)    │  │ Redis(8)    │                │
│  │             │  │             │  │ DB(10)     │                │
│  │ chat-collect│  │ 推送任务进度 │  │ 日志异步写    │                │
│  └─────────────┘  └─────────────┘  └─────────────┘                │
│                                                                    │
└──────────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP / WebSocket
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                     前端浏览器进程                                        │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐             │
│  │  主线程（UI 渲染线程）                             │             │
│  │  - Vue 响应式更新                                 │             │
│  │  - DOM 渲染                                      │             │
│  │  - 用户交互事件                                 │             │
│  └───────────────────┬──────────────────────────────────┘             │
│                  │                                               │
│  ┌───────────────▼───────────────┐  ┌──────────────────────────┐  │
│  │  Web API 线程（浏览器提供）    │  │  WebSocket 连接           │  │
│  │  - setTimeout/setInterval   │  │  - 接收任务进度推送         │  │
│  │  - fetch/XMLHttpRequest     │  │  - 实时通知                 │  │
│  │  - localStorage             │  │  - 自动重连                 │  │
│  └──────────────────────────────┘  └──────────────────────────┘  │
│                                                                    │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 1. 后端线程详解

### 1.1 HTTP 请求线程（Tomcat 线程池）

| 项目 | 说明 |
|------|------|
| **来源** | Spring Boot 内嵌 Tomcat |
| **默认数量** | 200 个线程 |
| **创建时机** | Spring Boot 启动时初始化，请求到来时从池里取 |
| **核心职责** | 处理所有进来的 HTTP 请求 |
| **哪些代码在这跑** | 所有 Controller 方法、Interceptor、Service（同步调用的部分） |

**配置位置**：`application.yml` 里 `server` 段

```yaml
# application.yml:1-8
server:
  port: 8080
  servlet:
    context-path: /api
```

**调用示例**：用户登录请求的整个链路都在 Tomcat 线程里执行：

```
Tomcat 线程（http-nio-8080-exec-1）
    │
    ├── JwtInterceptor.preHandle()      ← 拦截器
    ├── AuthController.login()       ← Controller
    ├── AuthServiceImpl.login()      ← Service
    ├── UserMapper.selectOne()      ← Mapper（借用 DB 连接
    └── 返回 Result.success()
```

> 💡 **理解要点**：Tomcat 线程是"请求驱动"的——来一个请求拿一个线程，请求结束线程还回池子。所以 Controller 里不能做耗时操作，否则会占着线程不还，导致新请求进来没线程可用。

---

### 1.2 @Async 异步线程池（3 个）

Spring 的 `@Async` 注解会把方法扔到专门的线程池里执行，不阻塞 HTTP 请求线程。

项目里配置了 **3 个独立的线程池**，各司其职：

| 线程池名称 | 核心线程数 | 最大线程数 | 队列容量 | 线程名前缀 | 用途 |
|-----------|----------|----------|---------|----------|------|
| `deerFlowTaskExecutor` | 5 | 10 | 100 | `DeerFlowAsync-` | DeerFlow 报告生成 |
| `reportTaskExecutor` | 3 | 5 | 50 | `ReportAsync-` | 报告任务 |
| `vectorTaskExecutor` | 2 | 4 | 50 | `VectorAsync-` | 向量生成任务 |

**配置文件**：`AsyncConfig.java`

```java
// AsyncConfig.java:21-31
@Bean(name = "deerFlowTaskExecutor")
public Executor deerFlowTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("DeerFlowAsync-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

**典型调用流程**：

```
用户发起报告生成请求
    │
    ▼
Tomcat 线程
    ├── Controller 接收请求
    ├── 调用 @Async("reportTaskExecutor") 方法
    ├── 立刻返回 "任务已提交"
    └── 线程结束 ← 快速归还 Tomcat 线程
                             
ReportAsync-1 线程
    ├── 调用大模型 API（耗时几秒~几分钟）
    ├── 生成报告内容
    ├── 保存到数据库
    └── 通过 WebSocket 推送完成通知
```

> 💡 **理解要点**：异步线程池就是"后台干活的工人"。前台（Tomcat）接了活，扔给后台工人去做，自己立刻回去接下一个活。干完了通过 WebSocket 通知用户。

---

### 1.3 Quartz 定时任务线程

| 项目 | 说明 |
|------|------|
| **来源** | Spring Boot Starter Quartz + @Scheduled |
| **创建时机** | Spring Boot 启动时启动调度器 |
| **核心职责** | 按时间表自动执行任务 |

项目里有两套定时任务机制并存：

**① Quartz（功能更强的 Quartz 框架：

```java
// QuartzConfig.java:16-36
@Bean
public org.quartz.JobDetail reviewJobDetail() {
    return org.quartz.JobBuilder.newJob(ReviewJob.class)
            .withIdentity("reviewJob", "group1")
            .storeDurably()
            .build();
}

// 每天早上 8 点执行
@Bean
public org.quartz.Trigger reviewTrigger() {
    return org.quartz.TriggerBuilder.newTrigger()
            .forJob(reviewJobDetail())
            .withIdentity("reviewTrigger", "group1")
            .withSchedule(org.quartz.CronScheduleBuilder.cronSchedule("0 0 8 * * ?"))
            .build();
}
```

**② Spring @Scheduled 注解（简单定时任务）：

```java
// ReviewScheduleTask.java:31-41
// 每天凌晨 2 点扫描待复习知识点
@Scheduled(cron = "0 0 2 * * ?")
public void scanPendingReviews() {
    reviewReminderService.processExpiredReminders();
}

// GamificationScheduler.java:53-54
// 每 15 分钟生成排行榜快照（6:00-23:00）
@Scheduled(cron = "0 */15 6-23 * * *")
public void generateLeaderboardSnapshots() {
```

**定时任务清单**：

| 任务 | 触发方式 | 执行频率 | 干什么 |
|------|---------|--------|--------|
| 复习任务调度 | Quartz | 每天 8:00 | 生成复习卡片 + 每日提醒 + 周报 |
| 扫描待复习 | @Scheduled | 每天 2:00 | 扫描过期复习提醒 |
| 检查提醒 | @Scheduled | 每 5 分钟 | 处理到期提醒 |
| 每日复习提醒 | @Scheduled | 每天 9:00 | 发送每日复习通知 |
| 每周报告 | @Scheduled | 每周一 0:00 | 生成周复习报告 |
| 排行榜快照 | @Scheduled | 每 15 分钟 | 生成日/周/月/总榜快照 |
| 补签卡重置 | @Scheduled | 每月 1 号 0:00 | 重置月补签卡 |

---

### 1.4 Kafka 消费者线程

| 项目 | 说明 |
|------|------|
| **来源** | spring-kafka |
| **创建时机** | Spring Boot 启动时建立连接 |
| **核心职责** | 监听 Kafka 主题，消费消息 |
| **消费速度** | `max-poll-records: 1`，一条一条处理 |

**配置**：

```yaml
# application.yml:41-57
spring:
  kafka:
    enabled: ${KAFKA_ENABLED:true}
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ai-second-brain-group
      max-poll-records: 1        ← 一次只拉一条
      max-poll-interval: 300000  ← 处理超时 5 分钟
```

**消费者服务**：

```java
// KafkaConsumerService.java:44-59
@KafkaListener(topics = "chat-collect", groupId = "chat-collect-group")
public void consumeChatCollect(RawChatRecord record) {
    log.info("收到聊天采集记录，userId：{}，sourceUrl：{}", record.getUserId(), record.getSourceUrl());
    
    try {
        rawChatRecordService.save(record);
        KnowledgeNode node = knowledgeCaptureService.extractKnowledge(record);
        if (node != null) {
            log.info("知识提取成功，nodeId：{}，title：{}", node.getId(), node.getTitle());
        }
    } catch (Exception e) {
        log.error("处理聊天采集记录失败", e);
    }
}
```

**消息流**：

```
聊天采集插件 ──生产──▶ Kafka (chat-collect 主题)
                           │
                           ▼
                   Kafka 消费者线程
                       │
                       ├── 保存原始聊天记录到 DB
                       ├── 调用 AI 提取知识
                       └── 保存知识点
```

> 💡 **理解要点**：`max-poll-records: 1` 是有意为之的设计——宁可慢一点，也不要一次处理太多导致 OOM 或超时。知识提取要调大模型，本身就慢，一条一条来更稳。

---

### 1.5 WebSocket 连接线程

| 项目 | 说明 |
|------|------|
| **来源** | spring-boot-starter-websocket + STOMP over SockJS |
| **创建时机** | 用户连接 `/ws` 端点时创建 |
| **核心职责** | 维护长连接，实时推送任务进度 |

**配置**：

```java
// WebSocketConfig.java:20-37
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");       ← 服务端推消息前缀
    config.setApplicationDestinationPrefixes("/app");  ← 客户端发消息前缀
}

@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
}
```

**推送服务**：

```java
// WebSocketServiceImpl.java:47-60
@Override
public void sendTaskProgress(String userId, String taskNumber, Integer progress) {
    AsyncTaskResponse response = new AsyncTaskResponse();
    response.setTaskId(taskNumber);
    response.setProgress(progress);
    response.setStatus("PROCESSING");
    messagingTemplate.convertAndSend("/topic/tasks/" + userId, response);
}
```

**推送流程**：

```
@Async 线程（干活中...）
    │
    ├── 每完成一步
    ├── 调用 webSocketService.sendTaskProgress()
    └── messagingTemplate.convertAndSend()
                              │
                              ▼
                    WebSocket 连接线程
                              │
                              ▼
                        前端浏览器
                    (websocket.js 收到消息
                    → 更新 UI 进度条
```

---

### 1.6 连接池线程（Redis + DB）

这些不是业务线程，是连接池管理的后台线程：

| 连接池 | 客户端 | 默认大小 | 配置位置 |
|-------|--------|---------|---------|
| Redis 连接池 | Lettuce | max-active: 8 | `spring.data.redis.lettuce.pool` |
| 数据库连接池 | HikariCP | 10（默认） | Spring Boot 默认值 |
| 日志写线程 | Logback | 异步写文件 | `logging.file` |

**Redis 池配置**：

```yaml
# application.yml:34-39
lettuce:
  pool:
    max-active: 8
    max-wait: -1ms
    max-idle: 8
    min-idle: 0
```

> 💡 **理解要点**：连接池不是"干活的线程"，是"管理连接的池子"。业务线程（比如 Tomcat 线程）要用 Redis/DB 时，从池子里借一个连接，用完还回去。池子本身有少量后台线程维护连接健康。

---

## 2. 前端运行时

### 2.1 主线程（UI 渲染线程）

| 项目 | 说明 |
|------|------|
| **数量** | 1 个（单线程） |
| **职责** | 渲染页面、处理用户交互、执行 JS |
| **不能做什么** | 耗时计算（超过 16ms 就会卡） |

**在这条线程跑的东西：
- Vue 响应式更新（ref/reactive 变更 → 虚拟 DOM diff → 真实 DOM 更新）
- 所有事件回调（点击、输入、滚动）
- 定时器回调（setTimeout/setInterval 的回调函数）

```
用户点击按钮
    │
    ▼
 事件队列（入队）
    │
    ▼
 事件循环（Event Loop）
    │
    ▼
 执行回调 → 修改 data → Vue 响应式更新 → DOM 重绘
```

---

### 2.2 Web API 线程（浏览器提供）

这些是浏览器底层提供的，不是 JS 引擎的线程，但 JS 可以调用它们：

| API | 作用 |
|-----|------|
| `setTimeout` / `setInterval` | 定时任务 |
| `fetch` / `XMLHttpRequest` | 发 HTTP 请求 |
| `localStorage` / `sessionStorage` | 本地存储 |
| `requestAnimationFrame` | 动画帧 |

> 它们的回调最终还是回到主线程执行，所以回调里也别做耗时操作。

---

### 2.3 WebSocket 连接

前端 WebSocket 封装在 `websocket.js` 里：

```javascript
// websocket.js:13-60
connect(userId) {
    const wsUrl = `ws://localhost:8080/api/ws`
    this.ws = new WebSocket(wsUrl)
    
    this.ws.onopen = () => { ... }
    this.ws.onmessage = (event) => {
        const data = JSON.parse(event.data)
        this.notifyListeners(data)  ← 回调还是回到主线程
    }
    this.ws.onclose = () => {
        this.attemptReconnect(userId)  ← 自动重连，最多 5 次
    }
}
```

**前端 WebSocket 特性**：
- 自动重连（最多 5 次，间隔 3 秒）
- 发布-订阅模式（`subscribe(eventType, callback)
- 连接成功/失败通知

---

## 3. 线程交互注意事项

### 3.1 需要加锁/注意并发的场景

| 场景 | 处理方式 | 相关代码 |
|------|---------|--------|
| 多人同时编辑同一个知识点 | 编辑锁（EditingLockService） | 15 分钟超时，自动续期 |
| 积分变更（PointsLog） | 数据库事务 + 幂等性 | 防止重复计算 |
| 排行榜 | 定时快照 | 不实时算，每 15 分钟生成一次 |
| Kafka 消费 | max-poll-records=1 | 一条一条处理，避免并发 |

**编辑锁服务接口**：

```java
// EditingLockService.java:19-28
EditingLock acquireLock(Long nodeId, Long userId);
// 若锁未被持有或已过期则获取成功
// 若被他人持有则返回 null
// 若自己已持有则续期

void releaseLock(Long nodeId, Long userId);
```

---

### 3.2 哪些操作不能在哪条线程做

| 线程类型 | ❌ 不能做 | ✅ 应该做 |
|---------|---------|---------|
| **Tomcat 请求线程 | 调大模型、生成报告、向量计算 | 快速接收请求，扔给 @Async |
| **Kafka 消费者线程 | 太耗时的操作（超过 5 分钟会被踢出组） | 快速处理单条消息 |
| **WebSocket 推送线程 | 大计算、DB 操作 | 只负责发消息 |
| **前端主线程 | 大循环、复杂计算 | 扔给后端做 |
| **@Async 线程 | — | 就是设计来干耗时活的 |

> 💡 **口诀**：前台接活快还回，后台干活慢慢来，消费别做重活。

---

## 4. 启动顺序全流程

### 4.1 后端启动（Spring Boot）

```
1. 读取配置文件（application.yml）
    │
    ├── 环境变量覆盖（MYSQL_HOST、REDIS_HOST 等）
    ▼
2. 初始化数据源（按顺序
    │
    ├── DataSource（HikariCP 连接池，10 个连接）
    ├── Redis（Lettuce 连接池，8 个连接）
    ├── MyBatis-Plus 扫描 Mapper
    ▼
3. Spring 上下文初始化
    │
    ├── 扫描 @Component / @Service / @Controller
    ├── 注册 Bean（AsyncConfig 三个线程池）
    ├── 初始化 AOP 代理
    ▼
4. 启动内嵌 Tomcat
    │
    ├── 监听 8080 端口
    ├── 初始化 200 个请求处理线程
    ▼
5. 启动调度器
    │
    ├── Quartz 调度器启动（复习任务）
    ├── @Scheduled 任务注册（排行榜、补签卡）
    ▼
6. 建立外部连接
    │
    ├── Kafka 消费者连接（如果启用）
    ├── Elasticsearch 连接（如果启用）
    ├── WebSocket（STOMP over SockJS）
    ▼
7. 启动完成，准备接收请求
```

### 4.2 前端启动（开发模式）

```
1. Vite 开发服务器启动（5173 端口）
    │
    ▼
2. 浏览器加载 index.html
    │
    ▼
3. 加载 main.js，创建 Vue 应用
    │
    ├── 创建 Pinia store
    ├── 注册路由 router
    ├── 注册 Element Plus
    ├── 注册全局组件
    ▼
4. 路由守卫检查登录状态
    │
    ├── 有 token → 放行
    └── 没 token → 跳登录页
    ▼
5. 渲染第一个页面
    │
    ├── Dashboard 或 Login
    └── WebSocket 连接建立
```

---

## 5. 一句话速查表

| 你想知道 | 看哪里 |
|---------|-------|
| HTTP 请求在哪个线程跑？ | Tomcat 线程池，默认 200 个 |
| 异步任务扔去哪？ | 看 `@Async("xxxTaskExecutor")` 注解里的名字 |
| 定时任务在哪配的？ | `QuartzConfig.java` + `@Scheduled` 注解的类 |
| Kafka 消费几条？ | `max-poll-records: 1`，一条一条来 |
| WebSocket 推送到哪？ | `/topic/tasks/{userId} |
| 编辑冲突怎么办？ | `EditingLockService`，15 分钟超时 |
| 排行榜怎么算的？ | 每 15 分钟快照一次，不是实时算 |
| 前端卡了怎么办？ | 检查是不是在主线程做了耗时操作 |
| Kafka 消费慢怎么办？ | 别在消费者里做太耗时的事 |
| 任务进度怎么推？ | @Async 里调 webSocketService.sendXXX() |
