> 本篇目标：让刚接触项目的新同学知道"接口和模块怎么设计才合理"，掌握命名规范、拆分原则、数据格式设计、常见反模式，照着做就能写出和项目风格一致的代码。

---

## 0. 读前须知

### 0.1 为什么需要设计规范

一个项目的代码质量，80% 取决于设计。设计规范的作用：

| 没有规范 | 有规范 |
|---------|-------|
| 每个人写法不一样，读代码像读天书 | 风格统一，看文件名就知道干啥的 |
| 新人接手要猜半天意图 | 按套路出牌，不用猜 |
| 改一个地方牵一发而动全身 | 模块职责清晰，改动范围可控 |
| Bug 藏在角落里找不到 | 结构清晰，问题定位快 |

类比：设计规范就像交通规则——不是为了限制自由，而是为了让所有人都能跑得更快、更安全。

### 0.2 本项目的设计哲学回顾

AI-SecondBrain 遵循三条核心设计原则：

1. **分层清晰**：Controller 只管接请求，Service 管业务，Mapper 管数据
2. **业务域优先**：按业务领域组织模块，不是按技术层
3. **防御式编程**：永远不相信前端传的数据，该校验就校验

---

## 1. 核心决策：何时用 A，何时用 B

做设计之前，先搞清楚"这个功能该用什么方案"。不要一上来就写代码，先对照下面的决策树走一遍。

### 1.1 技术选型决策树

```
要新增一个功能？
├── 只是单表 CRUD？
│   └── MyBatis-Plus BaseMapper + ServiceImpl 就够了
│       （参见 KnowledgeNodeMapper + KnowledgeServiceImpl）
├── 有复杂业务逻辑？
│   └── 写 Service 层，Controller 只做参数校验和返回
│       （参见 EbbinghausServiceImpl 复习算法）
├── 操作很耗时（> 3 秒）？
│   ├── 可以后台跑，用户不用等 → @Async 或 Kafka
│   │   （参见 AsyncTaskServiceImpl 异步任务）
│   └── 用户需要等 → 前端 loading，后端同步
├── 要做搜索？
│   ├── 简单模糊查询 → MySQL LIKE
│   ├── 全文搜索 + 高亮 → Elasticsearch
│   │   （参见 ElasticsearchServiceImpl）
│   └── 语义相似搜索 → 向量嵌入 + 相似度
│       （参见 VectorSearchServiceImpl）
├── 要实时通知？
│   ├── 不太频繁 → 前端轮询
│   └── 频繁且要及时 → WebSocket
│       （参见 WebSocketServiceImpl）
└── 要存文件？
    ├── 小文件（< 10MB）→ 数据库 BLOB（不推荐）
    └── 大文件 → 阿里云 OSS / 本地文件系统
        （参见 FileServiceImpl）
```

### 1.2 同步 vs 异步决策

| 场景 | 同步 | 异步（@Async / Kafka） |
|------|------|----------------------|
| 用户体验 | 要等结果 | 后台跑，用户可以干别的 |
| 耗时 | < 3 秒 | > 3 秒 |
| 失败处理 | 当场报错 | 记日志，重试或通知用户 |
| 示例 | 查询知识点、修改密码 | AI 对话生成、批量导入、报告生成 |

**判断标准：用户能不能接受"等一下"就同步，不能接受就异步。

---

## 2. 命名规范

命名是最基本的尊重——名字起对了，代码就好读了一半。

### 2.1 后端命名

| 类型 | 规则 | 正确示例 | 错误示例 |
|------|------|---------|---------|
| 包名 | 全小写，按功能分 | `com.secondbrain.service` | `com.secondbrain.Service` |
| Controller 类 | 功能 + Controller | `KnowledgeController` | `KnowledgeAction` |
| Service 接口 | 功能 + Service | `KnowledgeService` | `IKnowledgeService` |
| Service 实现 | 功能 + ServiceImpl | `KnowledgeServiceImpl` | `KnowledgeServiceImp` |
| Mapper 接口 | 表名 + Mapper | `KnowledgeNodeMapper` | `KnowledgeNodeDao` |
| Entity 类 | 表名驼峰 | `KnowledgeNode` | `Knowledge_node` |
| DTO | 功能 + DTO | `LoginDTO` | `loginDto` |
| VO | 功能 + VO | `KnowledgeNodeVO` | `KnowledgeNodeView` |
| 方法名 | 动词 + 名词 | `getById`, `create`, `list` | `find`, `save` |
| 常量 | 全大写下划线 | `DEFAULT_PAGE_SIZE` | `defaultPageSize` |
| 数据库表 | 模块_功能 | `knowledge_node` | `KnowledgeNode` |
| 数据库列 | 下划线 | `user_id`, `create_time` | `userId`, `createTime` |

**项目中的真实例子**（来自 `backend/src/main/java/com/secondbrain/`：

```
controller/KnowledgeController.java
service/KnowledgeService.java
service/impl/KnowledgeServiceImpl.java
mapper/KnowledgeNodeMapper.java
entity/KnowledgeNode.java
dto/LoginDTO.java
vo/KnowledgeNodeVO.java
```

### 2.2 前端命名

| 类型 | 规则 | 正确示例 | 错误示例 |
|------|------|---------|---------|
| 组件文件 | 大驼峰.vue | `Knowledge.vue` | `knowledge.vue` |
| 组件名 | 大驼峰 | `Knowledge` | `knowledge` |
| 变量/函数 | 小驼峰 | `getUserInfo` | `getuserinfo` |
| 常量 | 全大写下划线 | `MAX_COUNT` | `maxCount` |
| 组合式函数 | use + 功能 | `useUserInfo` | `userInfo` |
| Store 文件 | 功能.js | `user.js` | `userStore.js` |
| API 文件 | 模块.js | `knowledge.js` | `knowledge-api.js` |
| CSS 类 | 中划线 | `.knowledge-card` | `.knowledgeCard` |

**项目中的真实例子**来自 `frontend/src/`：

```
components/AchievementToast.vue
components/WorkspaceSwitcher.vue
api/knowledge.js
api/auth.js
stores/user.js
stores/workspace.js
```

### 2.3 API 接口命名（RESTful 风格）

| 操作 | 方法 | 路径示例 |
|------|------|---------|
| 列表 | GET | `/knowledge?page=1&size=10` |
| 详情 | GET | `/knowledge/{id}` |
| 创建 | POST | `/knowledge` |
| 更新 | PUT | `/knowledge/{id}` |
| 删除 | DELETE | `/knowledge/{id}` |
| 动作 | POST | `/knowledge/{id}/sync` |

**项目中的真实例子**来自 `KnowledgeController.java:62-75`：

```java
@GetMapping("/list")          // GET  /knowledge/list
@GetMapping("/{id}")          // GET  /knowledge/{id}
@PostMapping                   // POST /knowledge
@PutMapping("/{id}")           // PUT  /knowledge/{id}
@DeleteMapping("/{id}")        // DELETE /knowledge/{id}
@PostMapping("/{id}/sync")     // POST /knowledge/{id}/sync
```

**命名口诀**：
- 资源用名词（knowledge, user, workspace）
- 动作用动词（list, get, create, update, delete）
- 层级用斜杠（/knowledge/{id}/tags）
- 多个单词用中划线（/knowledge-tag，不是 knowledgeTag）

---

## 3. 拆分原则

代码不是拆得越细越好，也不是越粗越好——拆到"刚好能看懂"的程度就行。

### 3.1 什么时候该拆模块

✅ **该拆的情况：
- 代码超过 500 行，看不过来了
- 一个类干了好几件不相关的事（如既管用户又管知识点）
- 两个功能的改动互不影响，但在同一个文件里
- 新人接手说"这文件太复杂了"

❌ **不该拆的情况：
- 只有一个方法也要拆（过度拆分增加复杂度）
- 两个功能强耦合，改一个必改另一个（拆了反而来回跳）
- 为了"符合规范"而拆，实际没收益

### 3.2 判断标准：一句话说清楚职责

给模块/类起完名字后，试着用一句话说清楚它是干啥的：

| ✅ 好的职责描述 | ❌ 烂的职责描述 |
|----------------|----------------|
| "用户管理：注册、登录、信息修改" | "用户和知识点还有复习的管理" |
| "知识点的 CRUD 和搜索" | "知识相关的东西" |
| "艾宾浩斯复习算法计算" | "复习相关的逻辑" |

说不清楚 = 职责不清，就该拆了。

### 3.3 模块拆分粒度

**按业务领域拆，不是按技术层拆**。

虽然项目有 controller/service/mapper 分层，但**模块是按业务域组织的**：

```
com.secondbrain
├── controller/
│   ├── KnowledgeController.java    ← 知识模块的 Controller
│   ├── UserController.java       ← 用户模块的 Controller
│   └── WorkspaceController.java  ← 工作区模块的 Controller
├── service/
│   ├── KnowledgeService.java     ← 知识模块的 Service
│   ├── UserService.java          ← 用户模块的 Service
│   └── WorkspaceService.java     ← 工作区模块的 Service
├── mapper/
│   ├── KnowledgeNodeMapper.java  ← 知识模块的 Mapper
│   ├── UserMapper.java            ← 用户模块的 Mapper
│   └── WorkspaceMapper.java      ← 工作区模块的 Mapper
└── entity/
    ├── KnowledgeNode.java        ← 知识模块的 Entity
    ├── User.java                ← 用户模块的 Entity
    └── Workspace.java           ← 工作区模块的 Entity
```

**跨模块调用规则**：
- ✅ 通过 Service 接口调用（KnowledgeService 里可以调用 UserService）
- ❌ 不要直接调别人的 Mapper（KnowledgeService 里不要直接注入 UserMapper）
- ❌ 不要循环依赖（A 调 B，B 调 A → 要抽公共层）

---

## 4. 数据格式设计（API 接口）

接口数据格式是前后端的契约，定好了就别乱改。

### 4.1 统一响应格式

所有接口都返回统一的 `Result<T>` 格式（来自 `common/Result.java`）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

| 字段 | 说明 | 示例值 |
|------|------|-------|
| code | 状态码 | 200 成功，500 失败，401 未登录，403 无权限 |
| message | 提示消息 | "操作成功"、"参数错误" |
| data | 响应数据 | 对象、数组、null 都可以 |

**真实代码**（来自 `Result.java:41-46`）：

```java
public static <T> Result<T> success(T data) {
    Result<T> result = new Result<>();
    result.setCode(200);
    result.setMessage("操作成功");
    result.setData(data);
    return result;
}
```

**重要原则**：
- 永远不要返回 `undefined` 或 `null` 的字段，宁可返回空数组 `[]` 或空字符串 `""`
- 日期格式：`yyyy-MM-dd HH:mm:ss`（后端 `LocalDateTime` 自动序列化）

### 4.2 分页接口格式

分页直接用 MyBatis-Plus 的 `Page` 对象，不用自己封装：

```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

| 字段 | 说明 |
|------|------|
| records | 当前页数据列表 |
| total | 总记录数 |
| size | 每页大小 |
| current | 当前页码 |
| pages | 总页数 |

**真实代码**（来自 `KnowledgeController.java:64-74`）：

```java
@GetMapping("/list")
public Result<Page<KnowledgeNodeVO>> list(
        @RequestParam(defaultValue = "1") Integer current,
        @RequestParam(defaultValue = "10") Integer size,
        ...) {
    Page<KnowledgeNodeVO> page = knowledgeService.list(...);
    return Result.success(page);
}
```

### 4.3 字段命名

| 位置 | 命名风格 | 示例 |
|------|---------|------|
| 数据库 | 下划线 | `user_id`, `knowledge_id`, `is_read` |
| 后端 Java | 小驼峰 | `userId`, `knowledgeId`, `isRead` |
| 前端/JSON | 小驼峰 | `userId`, `knowledgeId`, `isRead` |

MyBatis-Plus 会自动把数据库下划线转成 Java 驼峰，不用手动转。

**布尔值约定**：
- 用 `is` 前缀：`isRead`, `isDeleted`
- 数据库里：`is_read`, `is_deleted`
- 类型用 `Boolean`，别用 `boolean`（允许 null）

### 4.4 版本兼容

接口改了怎么办？按以下优先级处理：

| 改动类型 | 处理方式 | 示例 |
|---------|---------|------|
| 新增字段 | 直接加，老接口不受影响 | 给用户信息加个 `avatar` 字段 |
| 修改字段 | 不要删老字段，加新字段过渡 | 老字段 `name` 保留，加 `nickname` |
| 大改结构 | 加版本号 `/v2/xxx`，老版本慢慢下线 | `/v2/knowledge |

**原则**：能加不要改，能改不要删。删字段 = 前端崩。

### 4.5 防御性解析

永远不要相信前端传过来的数据。后端必须做三道防线：

```
前端传参 → ① 参数校验（@Valid） → ② 权限校验 → ③ 业务校验
```

**① 参数校验**（JSR-380 注解）：
- `@NotNull` - 不能为 null
- `@NotBlank` - 字符串不能为空白
- `@Size(max = 100)` - 长度限制
- `@Email` - 邮箱格式

**② 权限校验**：
- 这个用户能不能操作这条数据？
- `workspaceId` 必带，做数据隔离
- 每次都校验 `userId + workspaceId`

**③ 业务校验**：
- 状态对不对？（比如已删除的不能再删）
- 数量够不够？（比如积分够不够兑换）

**反面教材**（千万别这么写）：
```java
// ❌ 危险：传个 id 就直接删，别人改 id 就能删别人的数据
@DeleteMapping("/{id}")
public Result<?> delete(@PathVariable Long id) {
    knowledgeService.removeById(id);  // 越权了都不知道
    return Result.success();
}
```

**正确写法**：
```java
// ✅ 安全：校验用户和工作区
@DeleteMapping("/{id}")
public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    Long workspaceId = (Long) request.getAttribute("workspaceId");
    knowledgeService.delete(id, userId, workspaceId);  // Service 里再校验一遍
    return Result.success();
}
```

---

## 5. 常见反模式

这些都是项目里踩过的坑，别再踩一遍。

| 反模式 | 问题 | 正确做法 |
|--------|------|---------|
| Controller 里写业务逻辑 | 代码复用差，难测试 | Controller 只做参数校验和调用 Service |
| 一个 Service 注入几十个 Mapper | 职责不清，难维护 | 按业务拆分 Service，互相调用 |
| 到处写 SQL 拼接 | 容易 SQL 注入，难维护 | 用 MyBatis-Plus QueryWrapper 或 XML |
| 返回 Map 不用 DTO/VO | 不知道有什么字段，前端容易踩坑 | 定义明确的 DTO/VO 类 |
| 接口传 id 就直接操作 | 越权！别人改个 id 就能操作别人的数据 | 每次都校验 userId + workspaceId |
| 同步调用 AI 大模型 | 用户卡死，超时 | 用异步 + Kafka，返回任务 ID，前端轮询 |
| 全局 try-catch 吞异常 | 出问题根本找不到原因 | 捕获后 log.error 再抛，或者交给全局异常处理 |
| 用 session 存用户信息 | 分布式部署有问题 | 用 JWT 无状态认证 |
| Entity 直接返回给前端 | 敏感字段（密码等）泄露 | 用 VO 做数据转换 |
| 复制粘贴代码 | 改一处忘一处，bug 泛滥 | 抽成公共方法/工具类 |

### 5.1 反模式详解：Controller 里写业务逻辑

❌ **错误示例**：
```java
@PostMapping("/login")
public Result<?> login(@RequestBody LoginDTO dto) {
    // Controller 里写了一堆业务逻辑
    User user = userMapper.selectOne(
        new QueryWrapper<User>().eq("username", dto.getUsername()));
    if (user == null) {
        return Result.error("用户不存在");
    }
    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
        return Result.error("密码错误");
    }
    String token = JwtUtil.generateToken(user.getId());
    return Result.success(token);
}
```

✅ **正确做法**：
```java
@PostMapping("/login")
public Result<LoginResponseDTO> login(@RequestBody @Valid LoginDTO dto) {
    LoginResponseDTO response = authService.login(dto);  // 业务逻辑全在 Service
    return Result.success(response);
}
```

**为什么？**
- Service 里的逻辑可以被其他地方复用
- Controller 很薄，一眼看懂
- 单元测试好写（测 Service 就行，不用启 Web）

### 5.2 反模式详解：Entity 直接返回给前端

❌ **错误示例**：
```java
// 把 User 实体直接返回，password 字段都暴露了
@GetMapping("/{id}")
public Result<User> getById(@PathVariable Long id) {
    User user = userService.getById(id);
    return Result.success(user);  // password、salt 全返回了
}
```

✅ **正确做法**：
```java
// 用 VO 转一道，只返回需要的字段
@GetMapping("/{id}")
public Result<UserVO> getById(@PathVariable Long id) {
    User user = userService.getById(id);
    UserVO vo = new UserVO();
    vo.setId(user.getId());
    vo.setUsername(user.getUsername());
    vo.setNickname(user.getNickname());
    // 敏感字段不返回
    return Result.success(vo);
}
```

---

## 6. 完整设计示例

需求：做一个"收藏知识点"功能。

我们跟着完整走一遍设计流程，看看每一步该干啥。

### 6.1 需求分析

**功能描述**：
- 用户可以收藏/取消收藏知识点
- 用户可以查看自己收藏的知识点列表
- 知识点详情页显示是否已收藏

**关键问题**：收藏状态是每个用户独立的，还是全局的？
→ 每个用户独立的，所以是多对多关系

### 6.2 设计步骤

#### 第 1 步：建表

```sql
CREATE TABLE `favorite` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作区ID',
  `knowledge_id` BIGINT NOT NULL COMMENT '知识点ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user_workspace (user_id, workspace_id),
  INDEX idx_knowledge (knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';
```

**为什么单独建表？**
- 查询独立，不影响知识点主表
- 多对多关系必须中间表
- 以后加字段（收藏时间、备注等）方便

#### 第 2 步：Entity

```java
// entity/Favorite.java
@Getter
@Setter
@TableName("favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long workspaceId;
    private Long knowledgeId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

#### 第 3 步：Mapper

```java
// mapper/FavoriteMapper.java
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
```

就这么简单，BaseMapper 里 CRUD 都有了。

#### 第 4 步：Service 接口

```java
// service/FavoriteService.java
public interface FavoriteService {
    /**
     * 分页查询收藏列表
     */
    Page<KnowledgeNodeVO> list(Integer current, Integer size, Long userId, Long workspaceId);

    /**
     * 添加收藏
     */
    void add(Long knowledgeId, Long userId, Long workspaceId);

    /**
     * 取消收藏
     */
    void remove(Long knowledgeId, Long userId, Long workspaceId);

    /**
     * 是否已收藏
     */
    boolean isFavorited(Long knowledgeId, Long userId, Long workspaceId);
}
```

**方法命名要见名知意，别用 `save`、`delete` 这种太泛的词。

#### 第 5 步：ServiceImpl

```java
// service/impl/FavoriteServiceImpl.java
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final KnowledgeService knowledgeService;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper,
                                KnowledgeService knowledgeService) {
        this.favoriteMapper = favoriteMapper;
        this.knowledgeService = knowledgeService;
    }

    @Override
    public Page<KnowledgeNodeVO> list(Integer current, Integer size,
                                      Long userId, Long workspaceId) {
        // 1. 查收藏的 knowledgeId 列表
        Page<Favorite> favoritePage = favoriteMapper.selectPage(
            new Page<>(current, size),
            new QueryWrapper<Favorite>()
                .eq("user_id", userId)
                .eq("workspace_id", workspaceId)
                .orderByDesc("create_time")
        );

        // 2. 批量查知识点详情
        List<Long> knowledgeIds = favoritePage.getRecords().stream()
            .map(Favorite::getKnowledgeId)
            .collect(Collectors.toList());

        if (knowledgeIds.isEmpty()) {
            return new Page<>();
        }

        // 3. 调用 KnowledgeService 查详情（不直接调 Mapper）
        List<KnowledgeNodeVO> knowledgeList = knowledgeService.getByIds(knowledgeIds);

        // 4. 组装返回
        Page<KnowledgeNodeVO> result = new Page<>();
        result.setRecords(knowledgeList);
        result.setTotal(favoritePage.getTotal());
        return result;
    }

    @Override
    public void add(Long knowledgeId, Long userId, Long workspaceId) {
        // 校验：知识点存在且用户有权限
        KnowledgeNode node = knowledgeService.getById(knowledgeId, userId, workspaceId);
        if (node == null) {
            throw new BusinessException("知识点不存在");
        }

        // 防重复收藏
        Long count = favoriteMapper.selectCount(
            new QueryWrapper<Favorite>()
                .eq("user_id", userId)
                .eq("workspace_id", workspaceId)
                .eq("knowledge_id", knowledgeId)
        );
        if (count > 0) {
            return;  // 已经收藏过了，直接返回
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setWorkspaceId(workspaceId);
        favorite.setKnowledgeId(knowledgeId);
        favoriteMapper.insert(favorite);
    }

    @Override
    public void remove(Long knowledgeId, Long userId, Long workspaceId) {
        favoriteMapper.delete(
            new QueryWrapper<Favorite>()
                .eq("user_id", userId)
                .eq("workspace_id", workspaceId)
                .eq("knowledge_id", knowledgeId)
        );
    }

    @Override
    public boolean isFavorited(Long knowledgeId, Long userId, Long workspaceId) {
        Long count = favoriteMapper.selectCount(
            new QueryWrapper<Favorite>()
                .eq("user_id", userId)
                .eq("workspace_id", workspaceId)
                .eq("knowledge_id", knowledgeId)
        );
        return count > 0;
    }
}
```

**关键点**：
- 跨模块调用用 Service，不用 Mapper
- 参数校验要做（知识点存在吗？有权限吗？）
- 防重复收藏

#### 第 6 步：Controller

```java
// controller/FavoriteController.java
@RestController
@RequestMapping("/favorite")
@Tag(name = "收藏管理", description = "知识点收藏相关接口")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/list")
    @Operation(summary = "收藏列表")
    public Result<Page<KnowledgeNodeVO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long workspaceId = (Long) request.getAttribute("workspaceId");
        Page<KnowledgeNodeVO> page = favoriteService.list(current, size, userId, workspaceId);
        return Result.success(page);
    }

    @PostMapping("/{knowledgeId}")
    @Operation(summary = "添加收藏")
    public Result<?> add(@PathVariable Long knowledgeId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long workspaceId = (Long) request.getAttribute("workspaceId");
        favoriteService.add(knowledgeId, userId, workspaceId);
        return Result.success("收藏成功");
    }

    @DeleteMapping("/{knowledgeId}")
    @Operation(summary = "取消收藏")
    public Result<?> remove(@PathVariable Long knowledgeId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long workspaceId = (Long) request.getAttribute("workspaceId");
        favoriteService.remove(knowledgeId, userId, workspaceId);
        return Result.success("取消收藏成功");
    }

    @GetMapping("/{knowledgeId}/status")
    @Operation(summary = "是否已收藏")
    public Result<Boolean> isFavorited(@PathVariable Long knowledgeId,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long workspaceId = (Long) request.getAttribute("workspaceId");
        boolean favorited = favoriteService.isFavorited(knowledgeId, userId, workspaceId);
        return Result.success(favorited);
    }
}
```

**为什么用 POST/DELETE 而不是 PUT？**
- 收藏是操作关系，不是修改资源
- POST = 创建收藏关系
- DELETE = 删除收藏关系
- 符合 RESTful 语义

#### 第 7 步：前端 API

```javascript
// frontend/src/api/favorite.js
import request from "@/utils/request";

export const favoriteAPI = {
  getList(params) {
    return request({
      url: "/favorite/list",
      method: "get",
      params,
    });
  },

  add(knowledgeId) {
    return request({
      url: `/favorite/${knowledgeId}`,
      method: "post",
    });
  },

  remove(knowledgeId) {
    return request({
      url: `/favorite/${knowledgeId}`,
      method: "delete",
    });
  },

  getStatus(knowledgeId) {
    return request({
      url: `/favorite/${knowledgeId}/status`,
      method: "get",
    });
  },
};
```

#### 第 8 步：前端页面

在知识点详情页加个收藏按钮，在"我的"页面加收藏列表。这里就不展开了。

### 6.3 设计决策回顾

| 决策 | 为什么这么选 |
|------|-------------|
| 单独建 favorite 表 | 多对多关系，查询独立 |
| 不用 knowledge_node 加 is_favorite 字段 | 每个用户收藏状态不一样，不是全局的 |
| 用 POST/DELETE | 操作收藏关系，符合 RESTful 语义 |
| Service 里调用 KnowledgeService | 跨模块用 Service，不用 Mapper |
| 防重复收藏 | 重复收藏不报错，幂等性 |

---

---

## 7. 多端设计规范

后端只有一套，但前端有 5 个形态。以下规范保证"同一功能在不同端体验一致但不互相绑死"。

### 7.1 前后端契约规范（5 端共用）

所有端共享同一套后端 API，契约一旦定下来不能随便改（改了 5 端都要跟着改）。

| 规范 | 说明 | 反面例子 |
|-----|------|---------|
| 路径统一 `/api/xxx` | 所有接口前缀 `/api`，Nginx 按此前缀转发 | 有的接口 `/xxx` 有的 `/api/xxx` |
| 统一 `Result<T>` 响应 | `{code, message, data}` | 有的接口直接返回数组 |
| 分页参数统一 | `current`（页号，从 1 开始）+ `size`（每页条数） | 有的用 `pageNo/pageSize` 有的用 `offset/limit` |
| 时间格式统一 | `yyyy-MM-dd HH:mm:ss`（后端 LocalDateTime 序列化） | 有的返回时间戳有的返回字符串 |
| 鉴权方式统一 | `Authorization: Bearer <token>` | 有的用 token 查询参数 |
| 工作区参数统一 | 所有业务接口带 `workspaceId`（从 token 或请求体传） | 有的接口漏 workspaceId 导致跨工作区 |

> **改契约 = 5 端联调**：如果必须改响应字段，优先"加字段不删字段"，让旧端继续能用。

### 7.2 uni-app 移动端规范

| 规范 | 说明 |
|-----|------|
| 页面路由只走 pages.json | 不能动态注册路由，所有页面必须静态声明 |
| 存储只用 `uni.*` API | 禁止用 `localStorage`、`sessionStorage`，小程序没有 |
| HTTP 只用 `uni.request` | 不能用 axios（小程序无 XMLHttpRequest） |
| 跳转不超 10 层 | `navigateTo` 栈深上限 10，深链路用 `redirectTo` 或 `reLaunch` |
| 图片用网络路径或 base64 | 小程序不能直接引用本地 `file://` 路径 |
| TabBar 页面放一级功能 | TabBar 是高频入口，只放 AI/知识/复习/广场/我的 |
| 样式用 rpx 单位 | rpx 是小程序响应式单位（750rpx = 屏幕宽） |

### 7.3 Chrome 扩展规范

| 规范 | 说明 |
|-----|------|
| 不用 npm 包 | 扩展直接跑在 Chrome 里，不能打包 node_modules，所有逻辑手写原生 JS |
| DOM 解析必须容错 | 平台随时改版，选择器失效要 try/catch 并提示用户 |
| 状态只存 `chrome.storage.local` | Service Worker 随时休眠，不能用全局变量当持久状态 |
| 异步消息要 `return true` | `onMessage` 回调里有异步操作必须返回 true，否则 Chrome 会提前结束 |
| 权限最小化 | manifest.json 的 host_permissions 只加实际要用的域名 |
| content.js 不直接发网络请求 | 跨域受限，统一交给 background.js 转发 |

### 7.4 DeerFlow Python 规范

| 规范 | 说明 |
|-----|------|
| 配置只走环境变量 | 不写死 API Key、Base URL，用 `os.getenv()` |
| 用户 key 优先 | `api_key = user_api_key or QWEN_API_KEY`，不强制用户用平台 key |
| 异常返回 None，不抛 500 | `requests` 异常捕获后返回 None，由 Java 侧判断 |
| 路由以 `/api/research/` 开头 | 和 Java 后端的 `/api/deerflow/**` 代理路径对应 |
| 必须有 `/health` 端点 | docker-compose healthcheck 依赖它 |
| 不引入重型框架 | Flask 够用，不上 FastAPI/Django，保持单文件可读懂 |

---

## 8. 代码审查检查清单

写完代码后，对照这个清单自查一遍。能过这 20 条，代码质量就有保障了。

### 8.1 命名检查

- [ ] 类名、方法名、变量名见名知意，不用缩写（除了大家都懂的（id、vo、dto）
- [ ] 后端命名符合规范（Controller/Service/Mapper/Entity/DTO/VO）
- [ ] 前端命名符合规范（组件大驼峰、变量小驼峰、CSS 中划线）
- [ ] API 路径是 RESTful 风格（名词复数，GET/POST/PUT/DELETE）
- [ ] 数据库表名/列名是下划线

### 8.2 分层检查

- [ ] Controller 只做参数校验和调用 Service，不写业务逻辑
- [ ] Service 里不直接返回 Entity 给前端，用 VO 转一道
- [ ] 跨模块调用用 Service 接口，不直接调别人的 Mapper
- [ ] 没有循环依赖（A 调 B，B 又调 A）
- [ ] 一个类/文件职责单一，一句话能说清楚

### 8.3 安全检查

- [ ] 接口都校验了 userId + workspaceId，不会越权
- [ ] 参数都加了校验注解（@NotNull、@NotBlank 等）
- [ ] 敏感字段（密码等）没返回给前端
- [ ] 没有 SQL 拼接（用 QueryWrapper 或 XML）
- [ ] 异常都记录了日志，没有吞异常

### 8.4 数据格式检查

- [ ] 接口返回统一的 Result 格式
- [ ] 分页用 MyBatis-Plus 的 Page 对象
- [ ] 没有返回 null 的字段，空的用空数组/空字符串
- [ ] 日期格式是 yyyy-MM-dd HH:mm:ss
- [ ] 布尔值用 is 前缀（isRead、isDeleted）

### 8.5 多端检查

- [ ] 如果改了后端契约，Web/移动/扩展三端是否都同步改了？
- [ ] 移动端有没有误用 `localStorage` / axios？
- [ ] 扩展有没有在 content.js 里直接发网络请求？
- [ ] DeerFlow 有没有把 API Key 写死在代码里？
- [ ] 新增的 Research Agent/Tool 有没有考虑预算约束（Token/Time/ToolCall）？

---

**记住**：规范不是束缚，是为了让你和未来的你（以及同事）都能轻松读懂代码。先写对，再写好，最后写快。
