> 本篇目标：让刚接触项目的新人在 15 分钟内看懂 AI-SecondBrain 的整体架构，知道代码长什么样、各层负责什么、遇到问题该去哪找。

---

## 0. 读前须知

### 0.1 什么是"第二大脑"

**第二大脑（Second Brain）** 是一个知识管理概念：把人脑不擅长的"记忆、检索、复习"交给计算机，人脑专注于"思考、创造、决策"。

类比一下：

| 人脑擅长 | 第二大脑擅长 |
|---------|------------|
| 联想、推理、创造 | 精确记忆、海量存储、定时提醒 |
| 瞬间理解上下文 | 全文检索、语义搜索、结构化整理 |
| 容易忘、容量有限 | 永不遗忘、容量无限 |

AI-SecondBrain 就是这样一个工具：你和 AI 对话、看书、记笔记，它自动把有价值的内容沉淀成知识卡片，按艾宾浩斯曲线提醒你复习，还能通过 RAG 随时调取。

### 0.2 为什么用前后端分离

前后端分离是现代 Web 应用的标准架构。简单说：

- ❌ **传统模式（不分离）**：后端 Java 直接渲染 HTML 页面，改个按钮颜色都要改 Java 代码
- ✅ **前后端分离**：前端 Vue 负责页面展示，后端 Spring Boot 负责数据和逻辑，通过 HTTP API 通信

好处是：
- 前端工程师和后端工程师可以并行开发
- 前端可以部署到 CDN，后端部署到服务器，各自扩缩容
- 除了 Web 端，还能接 App、小程序、Chrome 插件，共用一套后端 API

---

## 1. 整体分层架构

AI-SecondBrain 是一个典型的**四层架构**，从用户到数据依次是：

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐   │
│  │  Web 页面 │  │ Chrome   │  │  移动端   │  │  第三方    │   │
│  │  (Vue 3) │  │  插件     │  │  (待开发) │  │  集成      │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └─────┬─────┘   │
│       │             │             │              │          │
└───────┼─────────────┼─────────────┼──────────────┼──────────┘
        │  REST API   │  REST API   │  REST API    │  REST API
        ▼             ▼             ▼              ▼
┌─────────────────────────────────────────────────────────────┐
│                      网关 / 接入层                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Nginx（反向代理 + 静态资源）              │   │
│  │  前端静态资源  →  /           后端 API  →  /api/*    │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       应用层（后端）                         │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Controller 层  (25个控制器)                           │   │
│  │  接收请求、参数校验、调用Service、返回Result           │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         │                                   │
│  ┌──────────────────────▼───────────────────────────────┐   │
│  │  Service 层  (35+个服务接口 + impl实现)                │   │
│  │  业务逻辑：知识管理、复习算法、AI调用、权限校验          │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         │                                   │
│  ┌──────────────────────▼───────────────────────────────┐   │
│  │  Mapper 层  (MyBatis-Plus, 35+个Mapper)               │   │
│  │  数据库CRUD、分页、条件查询                            │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
│  横切关注点：                                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │  拦截器   │ │  异常处理 │ │  定时任务 │ │  消息队列     │   │
│  │ JWT/WS   │ │  Global  │ │  @Scheduled│ │  Kafka      │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                        数据存储层                            │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐   │
│  │  MySQL   │  │  Redis   │  │Elasticsearch│  Kafka    │   │
│  │  主数据库 │  │  缓存    │  │  全文搜索  │  │  消息队列 │   │
│  │  20+表   │  │  Session│  │  (可选)    │  │  (可选)   │   │
│  └──────────┘  └──────────┘  └──────────┘  └───────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**核心数据流向**：用户在浏览器点按钮 → 前端发 HTTP 请求 → Nginx 转发到后端 → Controller 接收 → Service 处理业务 → Mapper 查数据库 → 原路返回。

---

## 2. 目录结构详解

### 2.1 项目根目录

```
AI-SecondBrain/
├── backend/                # 后端 Spring Boot 项目（Java 17）
├── frontend/               # 前端 Vue 3 项目
├── extension/              # Chrome 浏览器插件
├── deerflow/               # DeerFlow 多智能体服务（Python）
├── sql/                    # 数据库建表脚本
├── nginx/                  # Nginx 配置
├── redis/                  # Redis 配置
├── scripts/                # 运维脚本
├── docs/                   # 项目文档（你正在读的就在这）
├── pom.xml                 # 后端 Maven 父 POM
├── docker-compose.yml      # Docker 一键启动
└── README.md               # 项目说明
```

### 2.2 后端目录（backend/）

所有 Java 代码都在 `com.secondbrain` 包下：

```
backend/src/main/java/com/secondbrain/
├── AiSecondBrainApplication.java    # 启动类（Spring Boot 入口）
│
├── controller/                       # REST API 控制器层（25个）
│   ├── AuthController.java          # 登录注册
│   ├── KnowledgeController.java     # 知识管理
│   ├── ReviewCardController.java    # 复习卡片
│   ├── ChatController.java          # AI对话
│   ├── WorkspaceController.java     # 工作区协作
│   └── ... 共 25 个
│
├── service/                          # 业务逻辑层（接口 + impl）
│   ├── KnowledgeService.java        # 知识服务接口
│   ├── ReviewCardService.java       # 复习服务接口
│   ├── AuthService.java             # 认证服务接口
│   ├── impl/                        # 实现类（约35个）
│   │   ├── KnowledgeServiceImpl.java
│   │   ├── ReviewCardServiceImpl.java
│   │   └── ...
│   └── ... 共 35+ 个
│
├── mapper/                           # 数据访问层（MyBatis-Plus）
│   ├── KnowledgeNodeMapper.java
│   ├── UserMapper.java
│   └── ... 共 35+ 个
│
├── entity/                           # 数据库实体类（20+）
│   ├── KnowledgeNode.java           # 知识节点
│   ├── ReviewCard.java              # 复习卡片
│   ├── User.java                    # 用户
│   ├── Workspace.java               # 工作区
│   └── ...
│
├── dto/                              # 数据传输对象（请求/响应）
│   ├── LoginDTO.java                # 登录请求
│   ├── RegisterDTO.java             # 注册请求
│   ├── LoginResponseDTO.java        # 登录响应
│   └── ... 共 30+ 个
│
├── vo/                               # 视图对象（返回给前端的视图数据）
│   ├── KnowledgeNodeVO.java
│   ├── ReviewCardVO.java
│   └── ... 共 10 个
│
├── config/                           # 配置类（20个）
│   ├── MybatisPlusConfig.java       # MyBatis-Plus配置
│   ├── WebMvcConfig.java            # Web MVC配置（拦截器注册）
│   ├── RedisConfig.java             # Redis配置
│   ├── Knife4jConfig.java           # API文档配置
│   ├── SecurityConfig.java          # 安全配置
│   └── ...
│
├── interceptor/                      # 拦截器
│   ├── JwtInterceptor.java          # JWT令牌解析
│   └── WorkspaceInterceptor.java    # 工作区权限校验
│
├── exception/                        # 异常处理
│   ├── GlobalExceptionHandler.java  # 全局异常处理器
│   └── BusinessException.java       # 业务异常
│
├── kafka/                            # Kafka消息队列（可选）
│   ├── KafkaProducerService.java
│   └── KafkaConsumerService.java
│
├── task/                             # 定时任务
│   ├── ReviewScheduleTask.java      # 复习提醒任务
│   └── GamificationScheduler.java   # 游戏化定时任务
│
├── util/                             # 工具类
│   ├── JwtUtil.java                 # JWT工具
│   └── ...
│
├── common/                           # 公共类
│   └── Result.java                  # 统一响应结果
│
└── elasticsearch/                    # ES文档实体（可选）
    └── KnowledgeDocument.java
```

### 2.3 前端目录（frontend/）

```
frontend/src/
├── main.js                 # 前端入口文件
├── App.vue                 # 根组件
│
├── api/                     # API请求封装（15个模块）
│   ├── knowledge.js        # 知识相关API
│   ├── auth.js             # 认证相关API
│   ├── review.js           # 复习相关API
│   ├── workspace.js        # 工作区相关API
│   └── ... 共 15 个
│
├── views/                   # 页面组件（20+个页面）
│   ├── Login.vue           # 登录页
│   ├── Knowledge.vue       # 知识管理页
│   ├── Review.vue          # 复习页
│   ├── Dashboard.vue       # 仪表盘
│   └── ... 共 20+ 个
│
├── components/              # 公共组件（可复用）
│   ├── WorkspaceSwitcher.vue  # 工作区切换器
│   ├── TaskProgress.vue       # 任务进度条
│   ├── GamificationWidget.vue # 游戏化组件
│   └── ... 共 10 个
│
├── router/                  # 路由配置
│   └── index.js            # vue-router 4
│
├── stores/                  # 状态管理（Pinia）
│   ├── user.js             # 用户状态
│   ├── workspace.js        # 工作区状态
│   ├── theme.js            # 主题状态
│   └── gamification.js     # 游戏化状态
│
├── styles/                  # 全局样式
│   ├── variables.css       # CSS变量（颜色、间距）
│   ├── shared.css          # 公共样式
│   ├── dark.css            # 暗黑模式
│   └── responsive.css      # 响应式适配
│
├── utils/                   # 工具函数
│   ├── request.js          # axios封装（请求拦截器）
│   └── websocket.js        # WebSocket封装
│
└── layout/                  # 布局组件
    └── MainLayout.vue      # 主布局（侧边栏 + 顶部 + 内容区）
```

---

## 3. 核心模块详解

### 3.1 知识管理模块（KnowledgeService）

**提供什么能力**：知识点的增删改查、关键词搜索、语义搜索、同步到 ES、统计数量。

**关键类**：

| 层级 | 类/文件 | 职责 |
|-----|--------|------|
| Controller | KnowledgeController.java | 接收 HTTP 请求，调用 Service |
| Service 接口 | KnowledgeService.java | 定义业务方法 |
| Service 实现 | KnowledgeServiceImpl.java | 业务逻辑实现 |
| Mapper | KnowledgeNodeMapper.java | 数据库操作 |
| Entity | KnowledgeNode.java | 数据库实体映射 |
| VO | KnowledgeNodeVO.java | 返回给前端的视图对象 |

**真实代码示例** —— KnowledgeService 接口（节选）：

```java
// backend/src/main/java/com/secondbrain/service/KnowledgeService.java
public interface KnowledgeService {

    // 分页查询知识点列表
    Page<KnowledgeNodeVO> list(Integer current, Integer size, String keyword, 
        Long userId, Integer importance, Integer masteryLevel, Long workspaceId);

    // 根据ID查询知识点
    KnowledgeNodeVO getById(Long id, Long userId, Long workspaceId);

    // 创建知识点
    KnowledgeNodeVO create(String title, String summary, String contentMd, 
        Integer importance, Long userId, Long workspaceId);

    // 关键词搜索
    List<KnowledgeNodeVO> search(String keyword, Long userId, Long workspaceId);

    // 语义搜索（向量相似度）
    List<KnowledgeNodeVO> semanticSearch(String queryText, Long userId, 
        int topK, Long workspaceId);

    // 同步到 Elasticsearch
    void syncToElasticsearch(Long userId, Long workspaceId);
}
```

**数据库实体** —— KnowledgeNode（`entity/KnowledgeNode.java:16`）：

```java
@Getter
@Setter
@TableName("knowledge_node")
public class KnowledgeNode {
    @TableId(type = IdType.AUTO)
    private Long id;              // 知识点ID
    private Long userId;          // 用户ID
    private Long workspaceId;     // 工作区ID
    private String title;         // 标题
    private String contentMd;     // Markdown内容
    private String summary;       // 摘要
    private Integer importance;   // 重要程度（1-5）
    private Integer masteryLevel; // 掌握程度（0-5）
    private Integer reviewCount;  // 复习次数
    private LocalDateTime nextReviewTime; // 下次复习时间
    @TableLogic
    private Integer deleted;      // 逻辑删除标记
}
```

**前端调用示例** —— 前端通过 `api/knowledge.js` 调用：

```javascript
// frontend/src/api/knowledge.js
import request from "@/utils/request";

export const knowledgeAPI = {
  getList(params) {
    return request({
      url: "/knowledge/list",
      method: "get",
      params,
    });
  },
  createKnowledge(data) {
    return request({
      url: "/knowledge",
      method: "post",
      data,
    });
  },
  semanticSearch(params) {
    return request({
      url: "/knowledge/search/semantic",
      method: "get",
      params,
    });
  },
};
```

### 3.2 复习系统模块（ReviewCardService）

**提供什么能力**：基于艾宾浩斯遗忘曲线的复习卡片生成、每日待复习列表、提交复习结果、连续学习天数统计、准确率计算。

**核心算法**：艾宾浩斯曲线 —— 根据复习时是否答对，动态调整下次复习时间间隔。

**关键类**：

| 层级 | 类/文件 | 职责 |
|-----|--------|------|
| Controller | ReviewCardController.java | 复习相关 API |
| Service 接口 | ReviewCardService.java | 复习业务接口 |
| Service 实现 | ReviewCardServiceImpl.java | 复习逻辑实现 |
| 算法服务 | EbbinghausService.java | 艾宾浩斯算法 |
| Entity | ReviewCard.java | 复习卡片实体 |

**真实代码示例** —— ReviewCardService 接口（节选）：

```java
// backend/src/main/java/com/secondbrain/service/ReviewCardService.java
public interface ReviewCardService {

    // 生成复习卡片
    ReviewCard generateReviewCard(Long nodeId, String cardType, Long userId);

    // 获取今日待复习卡片列表
    List<ReviewCard> getTodayReviewCards(Long userId, Long workspaceId);

    // 提交复习结果
    ReviewResultDTO submitReviewResult(Long cardId, String userAnswer, 
        Integer duration, Long userId);

    // 更新复习计划（答对/答错调整下次时间）
    void updateReviewSchedule(Long cardId, boolean isCorrect);

    // 计算连续复习天数
    int calculateStreakDays(Long userId, Long workspaceId);

    // 获取用户答题准确率
    int getUserAccuracy(Long userId, Long workspaceId);
}
```

**工作原理类比**：复习系统就像一个智能闹钟，你学完一个知识点后，它会：
1. 第 1 天提醒你复习
2. 如果你答对了，隔 2 天再提醒
3. 又答对了，隔 4 天、7 天、15 天……间隔越来越长
4. 如果答错了，间隔重置，重新开始

### 3.3 认证模块（AuthService + JwtInterceptor）

**提供什么能力**：用户注册、登录、JWT 令牌签发与校验。

**认证流程**：

```
用户输入账号密码
      │
      ▼
  AuthController.login()
      │
      ▼
  AuthService.login()
      │  校验密码
      ▼
  JwtUtil.generateToken()  ← 生成 JWT Token
      │
      ▼
  返回 Token 给前端
      │
      ▼
  前端存入 localStorage
      │
      ▼
  后续请求都带 Authorization: Bearer <token>
      │
      ▼
  JwtInterceptor 解析 Token，提取 userId
      │
      ▼
  Service 层通过 request.getAttribute("userId") 获取当前用户
```

**真实代码示例** —— AuthController：

```java
// backend/src/main/java/com/secondbrain/controller/AuthController.java
@RestController
@RequestMapping("/auth")
@Tag(name = "认证接口", description = "用户登录、注册等认证相关接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        LoginResponseDTO response = authService.login(loginDTO);
        return Result.success(response);
    }
}
```

**真实代码示例** —— JWT 拦截器（`interceptor/JwtInterceptor.java:37`）：

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String token = request.getHeader("Authorization");

    if (token != null && token.startsWith("Bearer ")) {
        token = token.substring(7);
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                request.setAttribute("userId", userId);  // 存入请求属性
                
                Long currentWsId = jwtUtil.getCurrentWsIdFromToken(token);
                if (currentWsId != null) {
                    request.setAttribute("workspaceId", currentWsId);
                }
            }
        } catch (Exception e) {
            log.error("JWT 解析失败：{}", e.getMessage());
        }
    }
    return true;  // 始终放行，具体权限由 Service 层校验
}
```

**前端请求封装** —— 自动在请求头加 Token（`frontend/src/utils/request.js:10`）：

```javascript
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);
```

### 3.4 工作区模块（WorkspaceService）

**提供什么能力**：工作区创建、成员管理、角色权限（RBAC）、邀请加入、转让所有权。

**RBAC 模型**：RBAC（Role-Based Access Control，基于角色的访问控制）—— 用户通过角色获得权限，而不是直接给用户分配权限。类比：公司里你是"经理"角色，经理角色本身有"审批请假"的权限。

**工作区角色**：

| 角色 | 权限 |
|-----|------|
| Owner（所有者） | 全部权限，可删除工作区、转让所有权 |
| Admin（管理员） | 管理成员、编辑内容 |
| Member（成员） | 编辑知识内容 |
| Viewer（查看者） | 只读 |

**真实代码示例** —— WorkspaceService 接口（节选）：

```java
// backend/src/main/java/com/secondbrain/service/WorkspaceService.java
public interface WorkspaceService {

    // 创建工作区
    WorkspaceResponse create(String name, String description, Long userId);

    // 查询用户所属的工作区列表
    List<WorkspaceResponse> listByUser(Long userId);

    // 添加成员（邀请）
    void addMember(Long workspaceId, Long targetUserId, String role, Long operatorUserId);

    // 被邀请人确认加入
    void acceptInvitation(Long workspaceId, Long userId);

    // 查询成员列表
    List<MemberResponse> listMembers(Long workspaceId, Long userId);

    // 更新成员角色
    void updateMemberRole(Long workspaceId, Long targetUserId, String newRole, Long operatorUserId);

    // 转让所有权
    void transferOwnership(Long workspaceId, Long newOwnerUserId, Long currentUserId);
}
```

**权限校验机制**：
- `WorkspaceInterceptor` 从 Token 中提取 workspaceId
- Service 层校验用户是否为该工作区成员、角色是否足够
- 不是成员或权限不足时抛出 `WorkspaceAccessDeniedException`

---

## 4. 构建体系

AI-SecondBrain 采用 **Maven + npm 双构建体系**，后端 Java 用 Maven，前端 Vue 用 npm。

### 4.1 整体构建关系

```
项目根目录
├── pom.xml              ← 后端父 POM（Spring Boot 3.1.5 + Java 17）
│
├── backend/
│   └── pom.xml          ← 后端子模块（继承父 POM）
│
└── frontend/
    └── package.json     ← 前端依赖管理（Vue 3.4 + Vite 5）
```

### 4.2 后端构建（Maven）

根 `pom.xml` 定义了父工程和版本（`pom.xml:8`）：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.5</version>
</parent>

<properties>
    <java.version>17</java.version>
    <mybatis-plus.version>3.5.3.1</mybatis-plus.version>
    <knife4j.version>4.4.0</knife4j.version>
    <hutool.version>5.8.24</hutool.version>
</properties>
```

**核心依赖**：

| 依赖 | 版本 | 用途 |
|-----|------|------|
| Spring Boot | 3.1.5 | Web 框架 |
| MyBatis-Plus | 3.5.3.1 | ORM 框架 |
| Knife4j | 4.4.0 | API 文档（Swagger 增强） |
| Spring Security | - | 安全框架 |
| Spring Data Redis | - | Redis 缓存 |
| Spring WebSocket | - | 实时推送 |
| Hutool | 5.8.24 | 工具库 |

**常用命令**：
```bash
mvn clean install    # 编译打包
mvn spring-boot:run  # 启动后端
```

### 4.3 前端构建（npm / Vite）

前端 `package.json`（`frontend/package.json:10`）：

```json
{
  "name": "ai-second-brain-frontend",
  "version": "1.0.0",
  "scripts": {
    "dev": "vite",          // 开发模式
    "build": "vite build",  // 生产构建
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.5",
    "pinia": "^2.1.7",
    "element-plus": "^2.5.0",
    "axios": "^1.6.2",
    "echarts": "^5.6.0",
    "marked": "^17.0.4"
  }
}
```

**核心技术栈**：

| 技术 | 版本 | 用途 |
|-----|------|------|
| Vue | 3.4 | 前端框架 |
| Vite | 5.0 | 构建工具 |
| Vue Router | 4.2 | 路由 |
| Pinia | 2.1 | 状态管理 |
| Element Plus | 2.5 | UI 组件库 |
| Axios | 1.6 | HTTP 客户端 |
| ECharts | 5.6 | 图表库 |

**常用命令**：
```bash
npm install     # 安装依赖
npm run dev     # 启动开发服务器（默认 5173 端口）
npm run build   # 构建生产版本（输出到 dist/）
```

### 4.4 统一响应格式

后端所有 API 都返回统一的 `Result` 格式（`common/Result.java:16`）：

```java
@Getter
@Setter
public class Result<T> implements Serializable {
    private Integer code;     // 状态码：200成功，500失败
    private String message;   // 响应消息
    private T data;           // 响应数据

    public static <T> Result<T> success(T data) { /* ... */ }
    public static <T> Result<T> error(String message) { /* ... */ }
}
```

前端 `request.js` 会自动解析这个格式，成功时直接返回 `data`，失败时弹出错误提示（`frontend/src/utils/request.js:23`）：

```javascript
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code === 200) {
      return res.data;  // 直接返回数据，不用每次都判断 code
    } else {
      ElMessage.error(res.message || "请求失败");
      return Promise.reject(new Error(res.message || "请求失败"));
    }
  }
);
```

---

## 5. 快速定位代码

遇到问题不知道看哪？查这张表：

| 我想了解... | 去哪个目录 | 关键文件 |
|------------|-----------|---------|
| 用户登录接口 | controller/ | AuthController.java |
| 登录业务逻辑 | service/ | AuthService.java + impl/AuthServiceImpl.java |
| 知识点 CRUD | service/ | KnowledgeService.java |
| 知识点数据库表结构 | entity/ | KnowledgeNode.java |
| 数据库操作（SQL） | mapper/ | KnowledgeNodeMapper.java |
| 复习算法 | service/ | EbbinghausService.java |
| 复习卡片 | service/ | ReviewCardService.java |
| 工作区权限 | service/ | WorkspaceService.java |
| JWT 令牌解析 | interceptor/ | JwtInterceptor.java |
| 全局异常处理 | exception/ | GlobalExceptionHandler.java |
| 统一响应格式 | common/ | Result.java |
| 配置项（端口、数据库等） | resources/ | application.yml |
| 前端页面 | views/ | Knowledge.vue、Review.vue 等 |
| 前端 API 请求封装 | api/ | knowledge.js、auth.js 等 |
| 前端 HTTP 封装 | utils/ | request.js |
| 前端路由 | router/ | index.js |
| 前端状态管理 | stores/ | user.js、workspace.js |
| 数据库建表脚本 | sql/ | complete_database_schema_verified.sql |

---

## 速查表

### 后端速查

| 想找什么 | 路径 |
|---------|------|
| 启动类 | `backend/.../AiSecondBrainApplication.java` |
| Controller 层 | `backend/.../controller/*.java`（25个） |
| Service 接口 | `backend/.../service/*.java`（35+个） |
| Service 实现 | `backend/.../service/impl/*.java` |
| 数据库实体 | `backend/.../entity/*.java` |
| 配置类 | `backend/.../config/*.java` |
| 配置文件 | `backend/src/main/resources/application.yml` |
| API 文档地址 | `http://localhost:8080/api/doc.html`（Knife4j） |

### 前端速查

| 想找什么 | 路径 |
|---------|------|
| 入口文件 | `frontend/src/main.js` |
| 页面组件 | `frontend/src/views/*.vue`（20+个） |
| 公共组件 | `frontend/src/components/*.vue` |
| API 封装 | `frontend/src/api/*.js`（15个） |
| 路由配置 | `frontend/src/router/index.js` |
| 状态管理 | `frontend/src/stores/*.js` |
| 全局样式 | `frontend/src/styles/*.css` |

### 端口速查

| 服务 | 默认端口 |
|-----|---------|
| 后端 API | 8080 |
| 前端开发服务器 | 5173 |
| MySQL | 3306 |
| Redis | 6379 |
| Elasticsearch | 9200 |
| Kafka | 9092 |
| Nginx | 80 |

### 启动命令速查

```bash
# 后端
cd backend
mvn spring-boot:run

# 前端
cd frontend
npm install
npm run dev

# Docker 一键启动（推荐）
docker-compose up -d
```
