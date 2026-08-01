# AGENTS.md

# AI Coding Skill 调用规范

本项目在进行 AI Coding 时，必须根据代码类型调用对应的 Skill 规范。

## 后端代码生成规范

在生成、修改或重构任何后端代码之前，必须先调用并遵循以下 Skill：

```text
D:\AI-SecondBrain\.codex\skills\java-coding-standards
```

该 Skill 用于规范后端 Java 代码的：

- 项目结构
- Java 编码规范
- Spring Boot 开发规范
- 类、方法、变量命名
- Service、Controller、Mapper 等分层规范
- 异常处理
- 数据访问
- Lombok 使用
- 代码可维护性
- 其他后端代码开发规范

### 强制要求

生成后端代码时，必须按照以下顺序执行：

1. 识别当前任务是否涉及后端代码。
2. 如果涉及后端代码，先调用：

```text
D:\AI-SecondBrain\.codex\skills\java-coding-standards
```

3. 读取并理解 Skill 中的相关规范。
4. 根据 Skill 规范生成或修改后端代码。
5. 最终生成的代码必须同时符合本 `CLAUDE.md` 中定义的 Java 编码规范。

禁止在未调用或未遵循 `java-coding-standards` Skill 的情况下直接生成后端代码。

---

## 前端代码生成规范

在生成、修改或重构任何前端代码之前，必须先调用并遵循以下 Skill：

```text
D:\AI-SecondBrain\.codex\skills\taste-skill
```

以及：

```text
D:\AI-SecondBrain\.codex\skills\ui-ux-pro-max
```

这两个 Skill 分别用于规范前端开发过程中的：

- UI 设计
- UX 设计
- 页面布局
- 视觉层次
- 颜色体系
- 字体规范
- 间距规范
- 组件设计
- 交互体验
- 响应式设计
- 页面美观度
- 前端整体设计质量

### 强制要求

生成前端代码时，必须按照以下顺序执行：

1. 识别当前任务是否涉及前端代码。
2. 如果涉及前端代码，先调用：

```text
D:\AI-SecondBrain\.codex\skills\taste-skill
```

3. 再调用：

```text
D:\AI-SecondBrain\.codex\skills\ui-ux-pro-max
```

4. 读取并理解两个 Skill 中与当前任务相关的规范。
5. 根据 Skill 规范完成 UI/UX 设计和前端代码生成。
6. 最终生成的前端代码必须符合项目已有的技术栈、组件规范和视觉规范。

禁止在未调用或未遵循上述两个 Skill 的情况下直接生成前端页面或前端代码。

---

## 代码注释规范

所有生成、修改或重构的代码，其代码注释必须遵循以下 Skill：

```text
D:\AI-SecondBrain\.codex\skills\Code Comment Rules
```

### 强制要求

在生成或修改任何代码之前，如果任务涉及新增或修改代码注释，必须遵循：

```text
D:\AI-SecondBrain\.codex\skills\Code Comment Rules
```

代码注释必须重点说明：

- 为什么这样实现
- 为什么采用当前方案
- 特殊业务规则
- 复杂逻辑的设计原因
- 数据库兼容性处理原因
- 性能优化原因
- 缓存处理原因
- 权限判断原因
- 状态流转原因

禁止生成无意义、重复或仅描述代码行为的注释。

例如，禁止：

```java
// 查询用户
User user = userService.getById(userId);
```

应根据实际业务场景说明必要的原因，例如：

```java
// 这里优先从缓存读取用户信息，避免高频查询直接访问数据库。
User user = userCache.get(userId);
```

### 注释 Skill 优先级

当代码注释规范与其他代码生成规范存在冲突时，代码注释相关内容必须优先遵循：

```text
D:\AI-SecondBrain\.codex\skills\Code Comment Rules
```

---

## Skill 调用决策规则

AI 在执行 Coding 任务时，应根据任务类型自动判断需要调用的 Skill。

### 后端任务

如果任务涉及以下内容：

- Java
- Spring Boot
- Controller
- Service
- Mapper
- Entity
- DTO
- VO
- Domain
- Repository
- MyBatis
- JPA
- 数据库访问
- 后端接口
- 后端业务逻辑

必须调用：

```text
D:\AI-SecondBrain\.codex\skills\java-coding-standards
```

---

### 前端任务

如果任务涉及以下内容：

- Vue
- React
- HTML
- CSS
- JavaScript
- TypeScript
- 页面
- 组件
- UI
- UX
- 前端交互
- 页面布局
- 页面视觉设计

必须调用：

```text
D:\AI-SecondBrain\.codex\skills\taste-skill
D:\AI-SecondBrain\.codex\skills\ui-ux-pro-max
```

---

### 全栈任务

如果一个任务同时涉及前端和后端，例如：

- 新增一个完整业务功能
- 新增一个前后端联调功能
- 新增一个完整模块
- 新增一个 CRUD 功能
- 新增一个页面及其后端接口

必须同时调用：

```text
D:\AI-SecondBrain\.codex\skills\java-coding-standards
D:\AI-SecondBrain\.codex\skills\taste-skill
D:\AI-SecondBrain\.codex\skills\ui-ux-pro-max
```

如果涉及代码注释，则必须同时遵循：

```text
D:\AI-SecondBrain\.codex\skills\Code Comment Rules
```

---

## Skill 执行优先级

执行 Coding 任务时，必须遵循以下优先级：

```text
用户明确要求
    ↓
项目 CLAUDE.md 规范
    ↓
对应 Coding Skill
    ↓
代码注释 Skill
    ↓
项目现有代码风格
    ↓
AI 默认编码习惯
```

如果用户明确要求与项目规范冲突，应优先按照用户明确要求执行。

如果用户未明确指定，则必须遵循本 `CLAUDE.md` 和对应 Skill 的规范。

---

## Coding 任务执行流程

AI 在执行代码生成任务时，必须遵循以下流程：

```text
1. 分析用户需求
        ↓
2. 判断任务类型
        ↓
3. 判断涉及后端 / 前端 / 全栈
        ↓
4. 调用对应 Skill
        ↓
5. 读取相关规范
        ↓
6. 分析现有项目代码结构
        ↓
7. 确定修改范围
        ↓
8. 生成或修改代码
        ↓
9. 检查代码规范
        ↓
10. 检查代码注释规范
        ↓
11. 检查是否符合项目现有代码风格
        ↓
12. 输出最终结果
```

### 重要要求

AI 不应跳过 Skill 调用流程直接生成代码。

在生成代码之前，必须优先加载当前任务相关的 Skill，并以 Skill 中定义的规范作为代码生成的重要依据。

对于同时涉及多个技术领域的任务，应同时调用所有相关 Skill，而不是只调用其中一个。

---

# Java 编码规范

以下规范适用于本项目所有 Java 代码。

## 核心原则

- 清晰优于巧妙
- 默认不可变；最小化共享可变状态
- 快速失败并提供有意义的异常
- 一致的命名和包结构

## 命名

```java
// Classes/Records: PascalCase
public class MarketService {}
public record Money(BigDecimal amount, Currency currency) {}

// Methods/fields: camelCase
private final MarketRepository marketRepository;
public Market findBySlug(String slug) {}

// Constants: UPPER_SNAKE_CASE
private static final int MAX_PAGE_SIZE = 100;
```

## 不可变性

```java
// 优先使用 record 和 final 字段
public record PostDto(Long id, String content, Integer rating) {}

public class Post {
  private final Long id;
  private final String content;
  // getters only, no setters
}
```

## Optional 使用

```java
// find* 方法返回 Optional
Optional<Post> post = postRepository.findById(id);

// 使用 map/flatMap 替代 get()
return post
    .map(PostResponse::from)
    .orElseThrow(() -> new EntityNotFoundException("Post not found"));
```

## Streams 最佳实践

```java
// 用于转换，保持 pipeline 简短
List<String> names = posts.stream()
    .map(Post::getTitle)
    .filter(Objects::nonNull)
    .toList();

// 避免复杂嵌套 stream；复杂逻辑用 for 循环更清晰
```

## 异常

- 领域错误使用非受检异常；包装技术异常时提供上下文
- 创建领域特定异常（如 `PostNotFoundException`）
- 避免宽泛的 `catch (Exception ex)`，除非在中心位置重新抛出/记录

```java
throw new PostNotFoundException(postId);
```

## 泛型和类型安全

- 避免原始类型；声明泛型参数
- 对于可复用的工具类，优先使用有界泛型

```java
public <T extends Identifiable> Map<Long, T> indexById(Collection<T> items) { ... }
```

## 项目结构

```
src/main/java/com/example/campusfood/
  config/
  controller/
  service/
  mapper/
  domain/
  dto/
  util/
src/main/resources/
  application.yml
src/test/java/... (mirrors main)
```

## 格式化和风格

- 统一使用 4 个空格缩进
- 每个文件一个公共顶级类型
- 保持方法简短且专注；提取辅助方法
- 成员顺序：常量 → 字段 → 构造函数 → 公共方法 → 受保护方法 → 私有方法

## 需要避免的代码坏味道

- 长参数列表 → 使用 DTO/构建器
- 深度嵌套 → 提前返回
- 魔法数字 → 命名常量
- 静态可变状态 → 优先使用依赖注入
- 静默捕获块 → 记录日志并处理或重新抛出

## 日志记录

```java
private static final Logger log = LoggerFactory.getLogger(PostService.class);
log.info("fetch_post id={}", postId);
log.error("failed_fetch_post id={}", postId, ex);
```

## Null 处理

- 仅在不可避免时接受 `@Nullable`；否则使用 `@NonNull`
- 在 Controller 入参上使用 Bean Validation（`@NotNull`, `@NotBlank`）

## 测试

- 使用 JUnit 5 + AssertJ 进行流畅的断言
- 使用 Mockito 进行模拟；避免部分模拟
- 倾向于确定性测试；不使用 sleep

# Code Comment Rules

所有新生成或修改的代码必须遵循以下注释规范。

## 通用原则

- 注释应解释"为什么"，而不是简单重复代码做了什么。
- 避免无意义注释，例如：
  - 获取用户
  - 查询数据
  - 返回结果

- 保持注释与代码同步，修改代码时同步更新注释。
- 删除已经失效的注释。

---

## 类注释

所有 Controller、Service、Mapper、Configuration、Component 等公共类必须添加类注释。

示例：

```java
/**
 * 用户管理服务
 *
 * 负责用户信息维护、状态管理及权限相关业务。
 *
 * @author AI
 */
```

---

## 方法注释

所有 public 方法必须使用 JavaDoc。

必须包含：

- 方法作用
- 参数说明
- 返回值说明
- 重要业务说明（如有）

示例：

```java
/**
 * 根据用户ID查询用户信息。
 *
 * @param userId 用户ID
 * @return 用户信息
 */
```

不要生成：

```java
/**
 * 查询
 */
```

---

## 字段注释

Entity、DTO、VO 的字段必须添加说明。

例如：

```java
/**
 * 用户名称
 */
private String username;
```

---

## 复杂业务

满足以下情况必须添加行内注释：

- 复杂计算
- 特殊业务规则
- SQL优化
- 缓存逻辑
- 权限判断
- 状态流转
- 多线程
- 达梦数据库兼容处理

注释重点说明：

为什么这样做。

不要仅描述代码本身。

---

## 禁止事项

禁止生成：

```java
// TODO

// 临时处理

// magic

// fix bug

// 这里不知道为什么

// 以后再改
```

除非用户明确要求。

---

## 输出要求

生成代码时：

- 注释应简洁准确。
- 不允许为了增加注释而增加注释。
- 不要生成模板化、重复、无意义的说明。

## Lombok 使用规范

为了保持代码简洁，项目统一使用 Lombok。

### Entity、DTO、VO、Domain

默认使用：

```java
@Getter
@Setter
```

禁止手动编写 getter() 和 setter() 方法。

示例：

```java
@Getter
@Setter
public class UserDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;
}
```

---

### 禁止事项

不要生成：

```java
public Long getId() {
    return id;
}

public void setId(Long id) {
    this.id = id;
}
```

除非用户明确要求不使用 Lombok。

---

### 其他 Lombok 注解

根据业务需要合理使用：

- `@NoArgsConstructor`
- `@AllArgsConstructor`
- `@Builder`
- `@EqualsAndHashCode`
- `@ToString`

不要为了省事直接使用 `@Data`。

优先使用：

```java
@Getter
@Setter
```

仅在确有需要时再添加其他 Lombok 注解，避免引入不必要的 `equals()`、`hashCode()`、`toString()` 等方法。

# Java 编码规范

以下规范适用于本项目所有 Java 代码。

## 核心原则

- 清晰优于巧妙
- 默认不可变；最小化共享可变状态
- 快速失败并提供有意义的异常
- 一致的命名和包结构

## 命名

```java
// Classes/Records: PascalCase
public class MarketService {}
public record Money(BigDecimal amount, Currency currency) {}

// Methods/fields: camelCase
private final MarketRepository marketRepository;
public Market findBySlug(String slug) {}

// Constants: UPPER_SNAKE_CASE
private static final int MAX_PAGE_SIZE = 100;
```

## 不可变性

```java
// 优先使用 record 和 final 字段
public record PostDto(Long id, String content, Integer rating) {}

public class Post {
  private final Long id;
  private final String content;
  // getters only, no setters
}
```

## Optional 使用

```java
// find* 方法返回 Optional
Optional<Post> post = postRepository.findById(id);

// 使用 map/flatMap 替代 get()
return post
    .map(PostResponse::from)
    .orElseThrow(() -> new EntityNotFoundException("Post not found"));
```

## Streams 最佳实践

```java
// 用于转换，保持 pipeline 简短
List<String> names = posts.stream()
    .map(Post::getTitle)
    .filter(Objects::nonNull)
    .toList();

// 避免复杂嵌套 stream；复杂逻辑用 for 循环更清晰
```

## 异常

- 领域错误使用非受检异常；包装技术异常时提供上下文
- 创建领域特定异常（如 `PostNotFoundException`）
- 避免宽泛的 `catch (Exception ex)`，除非在中心位置重新抛出/记录

```java
throw new PostNotFoundException(postId);
```

## 泛型和类型安全

- 避免原始类型；声明泛型参数
- 对于可复用的工具类，优先使用有界泛型

```java
public <T extends Identifiable> Map<Long, T> indexById(Collection<T> items) { ... }
```

## 项目结构

```
src/main/java/com/example/campusfood/
  config/
  controller/
  service/
  mapper/
  domain/
  dto/
  util/
src/main/resources/
  application.yml
src/test/java/... (mirrors main)
```

## 格式化和风格

- 统一使用 4 个空格缩进
- 每个文件一个公共顶级类型
- 保持方法简短且专注；提取辅助方法
- 成员顺序：常量 → 字段 → 构造函数 → 公共方法 → 受保护方法 → 私有方法

## 需要避免的代码坏味道

- 长参数列表 → 使用 DTO/构建器
- 深度嵌套 → 提前返回
- 魔法数字 → 命名常量
- 静态可变状态 → 优先使用依赖注入
- 静默捕获块 → 记录日志并处理或重新抛出

## 日志记录

```java
private static final Logger log = LoggerFactory.getLogger(PostService.class);
log.info("fetch_post id={}", postId);
log.error("failed_fetch_post id={}", postId, ex);
```

## Null 处理

- 仅在不可避免时接受 `@Nullable`；否则使用 `@NonNull`
- 在 Controller 入参上使用 Bean Validation（`@NotNull`, `@NotBlank`）

## 测试

- 使用 JUnit 5 + AssertJ 进行流畅的断言
- 使用 Mockito 进行模拟；避免部分模拟
- 倾向于确定性测试；不使用 sleep

# Code Comment Rules

所有新生成或修改的代码必须遵循以下注释规范。

## 通用原则

- 注释应解释"为什么"，而不是简单重复代码做了什么。
- 避免无意义注释，例如：
  - 获取用户
  - 查询数据
  - 返回结果

- 保持注释与代码同步，修改代码时同步更新注释。
- 删除已经失效的注释。

---

## 类注释

所有 Controller、Service、Mapper、Configuration、Component 等公共类必须添加类注释。

示例：

```java
/**
 * 用户管理服务
 *
 * 负责用户信息维护、状态管理及权限相关业务。
 *
 * @author AI
 */
```

---

## 方法注释

所有 public 方法必须使用 JavaDoc。

必须包含：

- 方法作用
- 参数说明
- 返回值说明
- 重要业务说明（如有）

示例：

```java
/**
 * 根据用户ID查询用户信息。
 *
 * @param userId 用户ID
 * @return 用户信息
 */
```

不要生成：

```java
/**
 * 查询
 */
```

---

## 字段注释

Entity、DTO、VO 的字段必须添加说明。

例如：

```java
/**
 * 用户名称
 */
private String username;
```

---

## 复杂业务

满足以下情况必须添加行内注释：

- 复杂计算
- 特殊业务规则
- SQL优化
- 缓存逻辑
- 权限判断
- 状态流转
- 多线程
- 达梦数据库兼容处理

注释重点说明：

为什么这样做。

不要仅描述代码本身。

---

## 禁止事项

禁止生成：

```java
// TODO

// 临时处理

// magic

// fix bug

// 这里不知道为什么

// 以后再改
```

除非用户明确要求。

---

## 输出要求

生成代码时：

- 注释应简洁准确。
- 不允许为了增加注释而增加注释。
- 不要生成模板化、重复、无意义的说明。

## Lombok 使用规范

为了保持代码简洁，项目统一使用 Lombok。

### Entity、DTO、VO、Domain

默认使用：

```java
@Getter
@Setter
```

禁止手动编写 getter() 和 setter() 方法。

示例：

```java
@Getter
@Setter
public class UserDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;
}
```

---

### 禁止事项

不要生成：

```java
public Long getId() {
    return id;
}

public void setId(Long id) {
    this.id = id;
}
```

除非用户明确要求不使用 Lombok。

---

### 其他 Lombok 注解

根据业务需要合理使用：

- `@NoArgsConstructor`
- `@AllArgsConstructor`
- `@Builder`
- `@EqualsAndHashCode`
- `@ToString`

不要为了省事直接使用 `@Data`。

优先使用：

```java
@Getter
@Setter
```

仅在确有需要时再添加其他 Lombok 注解，避免引入不必要的 `equals()`、`hashCode()`、`toString()` 等方法。

# AI Research Agent 开发约束

## 禁止事项

1. 禁止在未分析现有代码的情况下创建新模块。

2. 禁止重复实现已有的 RAG。

3. 禁止重复实现已有的 Elasticsearch 搜索。

4. 禁止重复实现已有的知识图谱。

5. 禁止重复实现已有的 AI Model Client。

6. 禁止 Research Agent 直接修改用户知识库。

7. 禁止 Agent 自动删除用户知识。

8. 禁止没有 Source 的研究结论被标记为确定事实。

9. 禁止展示模型 Chain-of-Thought。

10. 禁止无限 Agent Loop。

11. 禁止单个 Agent 无限调用 Tool。

12. 禁止单个 Research Task 无限重试。

13. 禁止一次性修改大量无关代码。

14. 禁止未经确认修改数据库已有表。

15. 禁止为了实现新功能重构整个旧系统。

16. 禁止创建重复 DTO。

17. 禁止创建重复 Service。

18. 禁止创建重复 Tool。

19. 禁止在 Controller 中编写业务逻辑。

20. 禁止在 Service 中直接编写复杂 SQL。

21. 禁止 Entity 手写 Getter / Setter。

22. 必须使用 Lombok @Getter / @Setter。

23. 所有最终设计、架构、需求、测试和开发总结必须保存为 Markdown 文档。

24. 发现已有同主题文档时，必须优先更新已有文档。

25. 禁止创建 final-final、new、temp、test2、copy 等无意义重复文件。
