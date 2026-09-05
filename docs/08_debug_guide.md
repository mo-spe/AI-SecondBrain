> 本篇目标：当项目跑不起来、接口行为不对、不知道从哪下手时，按本篇的步骤一步步排查，快速定位问题根因。

---

## 0. 调试的核心思路

遇到问题别急着改代码，先搞清楚三件事：

1. **问题出在哪一层** —— 前端？后端？数据库？第三方服务？
2. **预期 vs 实际** —— 你期望发生什么？实际发生了什么？
3. **最小复现** —— 能不能用最少的步骤稳定复现这个问题？

一个常用的排查流程：

```
现象描述
   │
   ▼
看前端页面/控制台报错 ──→ 前端问题 ──→ 改前端代码
   │
   │ 前端没问题？
   ▼
看浏览器 Network 面板 ──→ 请求参数不对？响应格式错？
   │
   │ 请求正常？
   ▼
看后端日志（error/warn）──→ 业务异常？参数校验失败？
   │
   │ 后端没报错？
   ▼
开 debug 日志看 SQL ──→ SQL 写错了？参数不对？数据不存在？
   │
   │ SQL 正常？
   ▼
查数据库表/数据 ──→ 表结构错了？数据没插进去？
```

---

## 1. 日志系统

日志是调试的第一抓手。项目用的是 SLF4J + Logback（Spring Boot 默认组合），配置在 `application.yml` 里。

### 1.1 日志级别

项目里用 4 个级别，从高到低：

| 级别 | 用途 | 什么时候看 | 示例场景 |
|------|------|----------|---------|
| error | 系统异常、业务错误 | 出问题必看 | 空指针、数据库连不上、业务校验失败 |
| warn | 警告，不影响主流程 | 排查异常时顺带看 | 配置缺失、重试、降级 |
| info | 关键节点、正常流程 | 了解运行状态 | 用户登录、知识点创建、任务完成 |
| debug | 详细调试信息 | 开发调试专用 | SQL 参数、中间计算结果、请求入参 |

> **怎么理解？** 把日志想象成医院的检查报告：
> - `error` = 病危通知书，必须处理
> - `warn` = 体检异常项，需要关注
> - `info` = 每日体温记录，正常就不细看
> - `debug` = 全身 CT，只有查不出问题时才做

### 1.2 当前日志配置

`application.yml` 里的日志配置（`backend/src/main/resources/application.yml:149-159`）：

```yaml
logging:
  level:
    com.secondbrain: debug      # 项目代码 debug 级别
    org.springframework.web: info
    org.mybatis: debug           # MyBatis SQL 日志
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
  file:
    name: logs/ai-second-brain.log
    max-size: 10MB
    max-history: 30
```

几个关键点：
- 项目代码 `com.secondbrain` 默认就是 **debug 级别**，开发时可以直接用
- `org.mybatis` 也是 debug，**SQL 日志默认就开着**
- 日志文件在 `logs/ai-second-brain.log`，滚动保存 30 天

### 1.3 怎么加日志

每个类顶部加 Logger 对象，有两种写法：

**写法一：手动声明（项目里的标准写法）**

以 `AuthController.java:19` 为例：

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    // ...
}
```

**写法二：Lombok @Slf4j 注解（更简洁）**

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SomeService {
    // 直接用 log 对象，Lombok 自动帮你生成
}
```

> 两种方式等价，项目里目前主要用写法一。

### 1.4 日志使用规范

打日志也是有讲究的，看一下项目里 `GlobalExceptionHandler.java:29` 的正确示范：

```java
// ✅ 正确：占位符 + 异常放最后
log.error("业务异常：{}", e.getMessage());

// ✅ 正确：异常对象放最后一个参数，自动打堆栈
log.error("系统异常，类型：{}，消息：{}", e.getClass().getName(), e.getMessage(), e);

// ✅ 正确：多个参数用占位符，不要拼接字符串
log.debug("查询参数: keyword={}, userId={}", keyword, userId);

// ❌ 错误：字符串拼接，性能差，还可能空指针
log.debug("查询参数: keyword=" + keyword + ", userId=" + userId);

// ❌ 错误：异常不打堆栈，等于白打
log.error("出错了：" + e.getMessage());
```

记住两条：
1. **用占位符 `{}`，不要字符串拼接**
2. **异常对象要放在最后一个参数，自动打印堆栈**

### 1.5 SQL 日志怎么看

MyBatis-Plus 的 SQL 日志是排查"查不出数据"的神器。默认配置下，控制台会输出：

```
==>  Preparing: SELECT id,title,content,user_id,workspace_id FROM knowledge_node WHERE user_id = ? AND deleted = 0 ORDER BY create_time DESC LIMIT ?
==> Parameters: 1(Long), 10(Integer)
<==      Total: 10
```

三行信息各有用处：
- **Preparing**：SQL 模板，`?` 是占位符
- **Parameters**：实际参数值，带类型
- **Total**：返回了几条记录

**典型用法：**
- 查不到数据 → 看 WHERE 条件和参数值对不对
- 数据不对 → 看 SELECT 了哪些字段
- 性能慢 → 看 SQL 是不是太复杂、有没有走索引

### 1.6 日志不输出怎么办

按这个 checklist 排查：

| 序号 | 检查项 | 怎么查 |
|------|--------|--------|
| 1 | 日志级别是不是设太高了 | `application.yml` 里 `com.secondbrain` 是不是 `debug` |
| 2 | Logger 类是不是导错了 | 确认是 `org.slf4j.Logger`，不是别的 |
| 3 | 有没有 `logback-spring.xml` 覆盖配置 | 去 `resources/` 目录看一眼 |
| 4 | 日志文件目录有没有写权限 | `logs/` 目录能不能创建文件 |
| 5 | 是不是被 log 回滚清掉了 | 看 `logs/` 下有没有 `.log.gz` 的历史文件 |

---

## 2. 编译阶段调试

代码写好之后跑不起来，先看编译报不报错。

### 2.1 后端编译常见错误

后端是 Maven 项目，Java 17（`pom.xml:22`），用 `mvn clean compile` 编译。

| 报错信息 | 原因 | 修复方法 |
|---------|------|---------|
| `找不到符号` / `cannot find symbol` | Lombok 没生效，getter/setter 没生成 | 1. IDE 装 Lombok 插件<br>2. 命令行执行 `mvn clean compile`<br>3. 确认 `pom.xml` 里有 lombok 依赖 |
| `程序包xxx不存在` / `package does not exist` | Maven 依赖没下载下来 | 1. `mvn dependency:resolve`<br>2. 检查网络能不能连 Maven 中央仓库<br>3. 试试换国内镜像（阿里云等） |
| `类型不匹配` / `incompatible types` | 方法返回值或参数类型错了 | 看具体行号，检查类型声明 |
| `编码 GBK 不可映射字符` | 文件编码不是 UTF-8 | `pom.xml` 里加 `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` |
| `diamond 运算符不支持` / 语法报错 | Java 版本太低 | 1. 检查 `JAVA_HOME` 是不是 Java 17+<br>2. IDE 里 Project SDK 设对 |
| `类文件具有错误的版本` | 编译用的 Java 和运行用的不一致 | 统一 JDK 版本，编译和运行要一致 |

**看详细错误的命令：**

```bash
# 显示完整的错误堆栈
mvn clean compile -e

# 跳过测试，只编译
mvn clean compile -DskipTests

# 清理之前的编译结果（解决"明明改了还是报错"的玄学问题）
mvn clean
```

### 2.2 前端编译常见错误

前端是 Vue 3 + Vite 项目（`frontend/package.json`），用 `npm run dev` 启动开发服务器。

| 报错信息 | 原因 | 修复方法 |
|---------|------|---------|
| `Module not found: Error: Can't resolve 'xxx'` | import 路径错了，或者依赖没装 | 1. `npm install`<br>2. 检查 import 路径，注意 `@/` 别名 |
| `Parsing error: Unexpected token` / 模板解析错 | Vue 模板语法错了 | 看具体行号，检查标签有没有闭合、指令写对没 |
| `xxx is not defined` | 变量没定义或没 import | 检查 import 语句和变量声明 |
| `Cannot read properties of undefined` | 访问了 null/undefined 的属性 | 加可选链 `?.`，或者判断一下再用 |

**Vite 的好处：** 开发模式下，错误会同时显示在：
1. **终端里** —— 启动 Vite 的那个命令行窗口
2. **浏览器页面上** —— 直接在页面弹错误遮罩
3. **浏览器 Console** —— 按 F12 看

---

## 3. 运行阶段调试

编译过了，但跑起来不对，这是最常见的情况。

### 3.1 后端启动失败排查

启动 Spring Boot 后立刻挂掉，看**日志的最后几行**，找 `Caused by:`。

```
... 一堆日志 ...
Caused by: java.net.ConnectException: Connection refused: connect
    at ...
Caused by: org.springframework.data.redis.RedisConnectionFailureException: Unable to connect to Redis
    at ...
```

**从最下面的 Caused by 往上看**，最底层的那个就是根因。

常见启动失败原因：

| 现象 | 根因 | 排查命令/操作 |
|------|------|-------------|
| `Communications link failure` / 连接数据库失败 | MySQL 没启动或配置不对 | 1. 检查 MySQL 服务启动没<br>2. 看 `application.yml` 的 datasource 配置<br>3. 用 MySQL 客户端手动连一下 |
| `RedisConnectionFailureException` | Redis 没启动 | 1. `redis-cli ping` 看通不通<br>2. 检查 `spring.data.redis` 配置 |
| `Port 8080 was already in use` | 端口被占用了 | `netstat -ano \| findstr :8080` 找 PID，杀掉进程<br>或者改 `server.port` 换个端口 |
| `Table 'xxx' doesn't exist` | 数据库表没建 | 执行 `sql/` 目录下的建表脚本 |
| `BeanCreationException` | Spring Bean 创建失败 | 看具体哪个 Bean，往上翻找第一个 Caused by |

> **小技巧：** 启动日志太长找不到重点？用 IDEA 的话，在控制台搜 `Started`，正常启动成功会打印 `Started AiSecondBrainApplication in X.XXX seconds`。如果没看到这句话，就是启动失败了。

### 3.2 接口行为不对怎么查

接口能调通，但返回的数据不对、或者行为不符合预期，按这个顺序查：

**第 1 步：看前端发的请求对不对**

打开浏览器 F12 → Network 面板，找到那个请求，看：
- **Request URL**：路径对不对？参数拼对了吗？
- **Request Method**：GET 还是 POST？
- **Request Headers**：Authorization token 带了吗？Content-Type 对吗？
- **Request Payload / Query String**：参数值对不对？

**第 2 步：看后端收到的请求对不对**

看后端控制台日志，默认 Spring Boot 会打印请求映射。如果看不到，在 Controller 方法入口加一行：

```java
log.debug("收到请求，参数：{}", registerDTO);
```

**第 3 步：看 SQL 执行对不对**

开着 MyBatis SQL 日志（默认就开着），看：
- SQL 语句本身对不对？
- 参数值对不对？
- 返回了几条记录？

**第 4 步：看业务逻辑中间结果**

在 Service 层的关键节点加 `log.debug` 打印中间变量，一步步缩小范围。

### 3.3 前后端联调技巧

项目是前后端分离的，联调时这些技巧能省很多时间：

| 场景 | 工具/方法 | 怎么用 |
|------|---------|-------|
| 看接口请求响应 | Chrome DevTools → Network | 每个请求点进去看 Headers / Payload / Response |
| 看前端状态 | Vue DevTools 浏览器插件 | 看组件 data、Pinia store、路由 |
| 看后端接口文档 | Knife4j / Swagger | 启动后访问 `http://localhost:8080/api/doc.html` |
| 单独测后端接口 | Postman / ApiPost | 绕开前端，直接调接口，确认是哪端的问题 |
| 看请求拦截逻辑 | 在 `request.js` 里打断点 | `frontend/src/utils/request.js` 是统一请求封装 |

**关于 request.js**（`frontend/src/utils/request.js`）：

这是前端统一的 HTTP 请求封装，基于 axios。关键逻辑：
- 第 13-15 行：请求拦截器自动加 `Authorization` header
- 第 31-36 行：响应拦截器判断 `code === 200` 才算成功
- 第 39-42 行：401 自动登出并提示

联调时在这里打断点，能看到所有请求和响应的原始数据。

### 3.4 常见功能异常排查步骤

| 现象 | 排查步骤 |
|------|---------|
| **登录失败** | 1. Network 看传的账号密码对不对<br>2. 看数据库 `user` 表里有没有这个用户<br>3. 看密码加密方式是否一致（项目用的什么加密去 `AuthServiceImpl` 里查） |
| **知识点列表为空** | 1. Network 看 Response 返回了什么<br>2. 看 SQL 日志的 WHERE 条件，`workspaceId`、`userId` 对不对<br>3. 直接去数据库 `knowledge_node` 表查一下 |
| **AI 接口没反应** | 1. `application.yml` 里 API Key 配了没（`ai.qwen.api-key` 等）<br>2. 看日志里 AI 请求和响应的 debug 信息<br>3. 检查网络能不能访问 AI 服务 |
| **复习卡片不生成** | 1. Quartz 定时任务有没有启动（看启动日志）<br>2. 看 `ReviewScheduleTask` 相关日志<br>3. 检查 `review_card` 表有没有数据 |
| **上传文件失败** | 1. 看文件大小有没有超限<br>2. OSS 配置对不对（`aliyun.oss` 相关配置）<br>3. 看后端日志里的文件处理异常 |

---

## 4. 常用调试工具介绍

工欲善其事，必先利其器。这些工具能大幅提升调试效率。

### 4.1 后端调试工具

| 工具 | 适用场景 | 说明 |
|------|---------|------|
| **IDEA Debugger** | 本地开发调试 | 打断点、单步执行、看变量值、表达式求值，最常用 |
| **Arthas** | 线上/测试环境排查 | 不用重启应用，就能看方法耗时、参数、调用栈、改日志级别 |
| **MySQL CLI / Navicat** | 验证 SQL | 把 MyBatis 打出来的 SQL 拼上参数，直接在数据库里执行验证 |
| **Redis-cli** | 查 Redis 数据 | `get key`、`keys *` 看看缓存里有什么 |
| **Knife4j 文档** | 接口自测 | `http://localhost:8080/api/doc.html`，在线调试接口 |

**IDEA Debug 基本用法：**
1. 行号左边点一下，打个红点（断点）
2. 点 Debug 按钮启动（虫子图标）
3. 触发到断点时，程序会停住
4. F8 单步跳过，F7 单步进入，F9 继续运行
5. Variables 面板看当前变量值

### 4.2 前端调试工具

| 工具 | 适用场景 | 说明 |
|------|---------|------|
| **Chrome DevTools** | 前端调试主力 | F12 打开，功能最全 |
| **Vue DevTools** | Vue 组件状态调试 | Chrome 插件，看组件树、Pinia、事件 |
| **Elements 面板** | 样式调试 | 看 DOM 结构、改 CSS、实时预览 |
| **Console 面板** | JS 报错、打印 | 看错误堆栈，执行临时 JS 代码 |
| **Network 面板** | 请求响应调试 | 看每个接口的详细信息 |
| **Sources 面板** | JS 断点调试 | 打断点、单步、看调用栈 |
| **Performance 面板** | 性能分析 | 录屏分析页面卡顿原因 |

**Chrome DevTools 常用快捷键：**

| 快捷键 | 功能 |
|--------|------|
| `F12` / `Ctrl+Shift+I` | 打开开发者工具 |
| `Ctrl+Shift+J` | 直接打开 Console 面板 |
| `Ctrl+Shift+C` | 元素选择器（点哪选哪） |
| `Ctrl+P` | 快速打开文件 |
| `Ctrl+F` | 当前面板搜索 |

---

## 5. 调试技巧速查表

把这个贴在工位边上，遇到问题先对着查一遍。

### 5.1 后端问题速查

| 问题现象 | 第一步操作 | 常见原因 |
|---------|-----------|---------|
| 启动失败，立刻退出 | 看日志最后一个 `Caused by:` | 数据库/Redis 连不上、端口占用、表不存在 |
| 接口 404 | 看 Controller 的 `@RequestMapping` 路径 | 路径写错、方法注解（`@GetMapping`/`@PostMapping`）错了、context-path 没加 |
| 接口 401 | 看请求头有没有 `Authorization` | token 没传、token 过期、token 格式不对 |
| 接口 500 | 看后端 error 日志 + 堆栈 | 空指针、数组越界、数据库异常 |
| 接口返回数据不对 | 开 debug 日志，看 SQL 和参数 | WHERE 条件错、参数传错、业务逻辑 bug |
| 查不到数据 | 看 MyBatis SQL 日志的 `Total` | 数据不存在、过滤条件错、逻辑删除没注意 |
| 插入/更新没生效 | 看 SQL 的 `Updates` 行数 | 事务没提交、主键冲突、字段约束不满足 |
| 配置不生效 | 看启动日志里的配置值 | 配错文件了、环境变量覆盖了、有多个配置文件 |
| 内存溢出 OOM | 看 heap dump 文件 | 数据量太大、有内存泄漏、缓存没限制 |

### 5.2 前端问题速查

| 问题现象 | 第一步操作 | 常见原因 |
|---------|-----------|---------|
| 页面空白 | F12 看 Console 报错 | JS 报错、路由不对、import 路径错 |
| 接口请求没发出去 | 看 Network 面板 | 地址写错、跨域拦截、网络断了 |
| 数据没更新到页面 | Vue DevTools 看 store/组件 data | 响应式没写对、没触发重新渲染 |
| 样式不对 | Elements 面板看 CSS | 选择器优先级不够、被覆盖了、选择器写错 |
| 点击按钮没反应 | Console 看有没有报错，Network 看有没有请求 | 事件没绑定、方法名写错、接口报错 |
| 图片加载失败 | Network 看图片请求的状态 | 路径错了、文件不存在、权限问题 |
| 页面卡顿 | Performance 面板录屏 | JS 执行太久、渲染太频繁、接口太慢 |

### 5.3 移动端（微信小程序）问题速查

| 问题现象 | 第一步操作 | 常见原因 |
|---------|-----------|---------|
| 小程序白屏 | 微信开发者工具看 Console | main.js 用了 `createApp` 而非 `createSSRApp`、JS 语法不兼容 ES5 |
| 接口 401 | Network 面板看请求头 | token 没存进 `uni.setStorageSync`、请求封装没加 Authorization |
| `navigateTo:fail page not found` | 检查 pages.json | 页面路径没在 pages.json 注册、路径拼写错 |
| TabBar 不显示 | 检查 pages.json 的 tabBar | list 少于 2 项或多于 5 项、iconPath 路径错 |
| `uni.request` 跨域（H5 端） | 看 vite.config.js proxy | H5 端浏览器 CORS，需要配代理；小程序端无此问题 |
| 图片不显示 | 看 Network | 路径是相对还是绝对、小程序不能直接用本地 file:// 路径 |
| 真机预览报"不在以下 request 合法域名" | 微信公众平台后台 | 没把后端域名加到"服务器域名"白名单 |
| 数据不更新 | Vue DevTools（HBuilderX 自带） | 小程序的 setData 机制，大列表要手动 `this.$forceUpdate()` |

### 5.4 浏览器扩展问题速查

| 问题现象 | 第一步操作 | 常见原因 |
|---------|-----------|---------|
| 扩展图标灰了 / 点不开 | chrome://extensions 看错误 | manifest.json 语法错、权限声明不全 |
| content.js 没注入 | 目标页面 F12 → Sources → Content scripts | manifest.json 的 `matches` 不匹配当前域名 |
| 点"采集"没反应 | 目标页面 F12 Console | content.js 报错（DOM 选择器失效）、消息没发到 background |
| background.js 报错 | chrome://extensions → 点"Service Worker"打开 DevTools | `chrome.runtime.onMessage` 回调没 `return true` 导致异步失败 |
| 调后端报 CORS | background DevTools Network | manifest.json `host_permissions` 没加后端域名 |
| 改了代码不生效 | chrome://extensions 刷新按钮 | 必须手动重新加载扩展，热更新不生效 |
| popup 打开白屏 | 右键 popup → 检查 | popup.html 里的 `<script src>` 路径错 |

### 5.5 DeerFlow 问题速查

| 问题现象 | 第一步操作 | 常见原因 |
|---------|-----------|---------|
| Java 调 DeerFlow 报 Connection refused | `curl http://localhost:8000/health` | DeerFlow 没启动、`DEERFLOW_API_URL` 配错 |
| `/health` 返回 healthy 但业务接口 500 | 看 DeerFlow 控制台日志 | 通义千问 API Key 无效、model 名错、网络不通 |
| 返回内容为 null | 看 DeerFlow 日志的 print | `requests.post` 超时（300s）或 API 返回非 200 |
| 响应很慢 | DeerFlow 日志看调用耗时 | 通义千问本身慢、prompt 太长导致 max_tokens 不够 |
| Docker 里 DeerFlow 反复重启 | `docker-compose logs deerflow` | `.env` 没配 `QWEN_API_KEY` 导致启动后立即报错 |

### 5.6 Kafka 问题速查

| 问题现象 | 第一步操作 | 常见原因 |
|---------|-----------|---------|
| 消息发不出去 | 看 `KafkaProducerService` 日志 | Kafka 没启动、`bootstrap-servers` 配错、topic 不存在 |
| 消息发出去了但没消费 | `docker-compose logs kafka` 看 topic 列表 | 消费者组没起来、`max-poll-records` 配错、消息被路由到没消费者的 partition |
| 消费报错"被踢出消费组" | 看消费者处理时长 | 单条消息处理超过 `max.poll.interval.ms`（默认 5 分钟） |
| 重复消费 | 看 async_task 表状态 | 消费者处理成功但 offset 没提交、应用异常退出 |
| Kafka 启动失败 | `docker-compose logs kafka` | Zookeeper 没启动、磁盘满了、端口占用 |

> **调试 Kafka 的神器**：`kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic async-task-topic --from-beginning` 直接看 topic 里有什么消息。

### 5.7 Elasticsearch 问题速查

| 问题现象 | 第一步操作 | 常见原因 |
|---------|-----------|---------|
| ES 搜索返回空 | `curl http://localhost:9200/_cat/indices` | 索引不存在、数据没同步、查询 DSL 写错 |
| 同步知识到 ES 失败 | 看 `ElasticsearchServiceImpl` 日志 | ES 没启动、mapping 不匹配、字段超长 |
| ES 启动失败 | `docker-compose logs elasticsearch` | 内存不够（`ES_JAVA_OPTS` 太小）、磁盘空间不足、vm.max_map_count 太小 |
| 搜索结果不准 | 看 ES 查询 DSL | 分词器不对、应该用 match 却用了 term、boost 权重不合理 |

> **ES 没启用时**：项目会自动降级到 MySQL 模糊搜索（`NoOpElasticsearchService`），所以搜索功能"能用但不准"不一定是 bug，可能是 ES 关了。

### 5.8 Redis 问题速查

| 问题现象 | 第一步操作 | 常见原因 |
|---------|-----------|---------|
| 缓存不生效 | `redis-cli get <key>` 看有没有 | 缓存 key 拼错、TTL 过期、Redis 连不上 |
| Redis 连不上 | `redis-cli -a <password> ping` | Redis 没启动、密码错、`spring.redis.host` 配错 |
| 缓存和 DB 数据不一致 | 看代码有没有删缓存 | 更新 DB 后没 `redisTemplate.delete(key)`、分布式并发问题 |
| 验证码/登录态丢失 | `redis-cli keys "session:*"` | Redis 重启丢了内存数据、TTL 设太短 |

---

### 5.9 通用调试心法

最后送你几条调试经验：

1. **复现是第一步** —— 不能稳定复现的 bug 没法修，先找到稳定复现路径
2. **二分法缩小范围** —— 不知道哪错了？注释一半代码看看还错不错，反复缩小范围
3. **对比法找差异** —— 同样的功能 A 好用 B 不好用？对比一下两者有什么不一样
4. **别猜，要验证** —— "我觉得应该是这个问题" → 打日志/打断点/改一下试试，确认了再说
5. **回退到上一个好用的版本** —— 不知道改坏了什么？`git diff` 看看改了啥，或者 `git bisect` 二分找 commit
6. **休息一下** —— 卡了一小时还没思路？起来喝杯水，有时候一眼就看到了

> 调试的本质是**假设 → 验证 → 再假设 → 再验证**的循环。
> 关键是每一步都要有明确的依据，不要瞎改代码瞎试。
