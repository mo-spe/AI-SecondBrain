> 跟着最经典的「用户登录 → 获取知识点列表」流程走一遍，把前后端各层代码对应起来，建立整体认知。

## 0. 读前地图

先看完整的调用链路，心里有张图再往下读：

```
用户输入账号密码
      │
      ▼
  前端 Login.vue ──点击登录──┐
      │                      │
      ▼                      │
  authAPI.login()            │  前端
      │                      │
      ▼                      │
  request.js (拦截器) ────────┘
      │  baseURL=/api
      │  自动带 Authorization header
      ▼
════════════════════ HTTP 请求 ════════════════════
      │
      ▼
  JwtInterceptor (拦截器)
      │  解析 token → 提取 userId/workspaceId
      ▼
  AuthController (@RestController)
      │  @PostMapping("/auth/login")
      ▼
  AuthService (接口) → AuthServiceImpl (实现)
      │  1. 查用户是否存在
      │  2. BCrypt 比对密码
      │  3. JwtUtil 生成 token
      ▼
  UserMapper (继承 BaseMapper<User>)
      │  selectOne(username)
      ▼
     MySQL
      │
      ▼
  返回 LoginResponseDTO (token + userInfo)
      │
      ▼
  Controller 包装成 Result.success(data)
      │
════════════════════ HTTP 响应 ════════════════════
      │
      ▼
  request.js 响应拦截器
      │  code===200 → 返回 data
      ▼
  Login.vue 拿到结果
      │  存 token 到 localStorage
      │  存 userInfo 到 Pinia
      ▼
  跳转 /dashboard
      │
      ▼
  请求知识点列表
      │  knowledgeAPI.getList({current, size})
      ▼
  KnowledgeController
      │  @GetMapping("/knowledge/list")
      ▼
  KnowledgeServiceImpl
      │  buildBaseWrapper(userId, workspaceId)
      │  selectPage(page, wrapper)
      ▼
  KnowledgeNodeMapper
      │
      ▼
  返回 Page<KnowledgeNodeVO>
      │
      ▼
  前端列表渲染
```

---

## 1. 前端入口：应用启动的第一步

就像开门前先把钥匙、钱包、手机都准备好，前端启动时也要把所有工具初始化完毕。

**文件**：[frontend/src/main.js](file:///d:/AI-SecondBrain/frontend/src/main.js)

```javascript
// 引入 Vue 核心
import { createApp } from 'vue'
// 引入 Pinia 状态管理
import { createPinia } from 'pinia'
// 引入 Element Plus UI 组件库
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 引入 Element Plus 图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 引入路由
import router from './router'
// 引入根组件
import App from './App.vue'

// 创建设计系统样式
import './styles/variables.css'
import './styles/shared.css'
import './styles/dark.css'
import './styles/responsive.css'

// 1. 创建 Vue 应用实例
const app = createApp(App)
// 2. 创建 Pinia 实例
const pinia = createPinia()

// 3. 全局注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 4. 依次注册各种插件
app.use(pinia)          // 状态管理
app.use(router)         // 路由
app.use(ElementPlus)    // UI 组件库

// 5. 挂载到页面的 #app 元素上
app.mount('#app')
```

**根组件 App.vue**：[frontend/src/App.vue](file:///d:/AI-SecondBrain/frontend/src/App.vue)

```vue
<template>
  <div id="app">
    <!-- 路由出口：当前路由匹配到哪个组件，就渲染在这里 -->
    <router-view />
  </div>
</template>
```

**理解**：
- `createApp(App)` — 创建应用，就像拿到一张空白画布
- `app.use(xxx)` — 安装各种工具插件，就像往工具箱里放东西
- `app.mount('#app')` — 把应用挂到页面上，正式开始工作

---

## 2. 第一步：用户访问登录页

用户打开浏览器输入网址，路由系统决定显示哪个页面。

**文件**：[frontend/src/router/index.js](file:///d:/AI-SecondBrain/frontend/src/router/index.js)

### 2.1 路由配置

```javascript
const routes = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/Login.vue"),  // 懒加载：访问时才加载
    meta: { title: "登录", requiresAuth: false },  // 不需要登录就能访问
  },
  {
    path: "/",
    component: () => import("@/layout/MainLayout.vue"),
    redirect: "/dashboard",
    meta: { requiresAuth: true },  // 需要登录
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("@/views/Dashboard.vue"),
        meta: { title: "数据统计" },
      },
      {
        path: "knowledge",
        name: "Knowledge",
        component: () => import("@/views/Knowledge.vue"),
        meta: { title: "知识管理" },
      },
      // ... 更多路由
    ],
  },
]
```

### 2.2 路由守卫：门卫检查

```javascript
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title
    ? `${to.meta.title} - AI-SecondBrain`
    : "AI-SecondBrain";

  const userStore = useUserStore();

  // 规则1：需要登录 且 没登录 → 跳登录页
  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn()) {
    next("/login");
  }
  // 规则2：需要管理员 且 不是管理员 → 跳首页
  else if (to.meta.requiresAdmin && userStore.userInfo.role !== "super_admin") {
    next("/dashboard");
  }
  // 规则3：已登录用户访问登录/注册页 → 直接跳首页
  else if (
    (to.path === "/login" || to.path === "/register") &&
    userStore.isLoggedIn()
  ) {
    next("/dashboard");
  }
  // 规则4：其他情况 → 放行
  else {
    next();
  }
});
```

**类比**：路由就像大楼的导航牌，告诉你每个房间怎么走。路由守卫就像前台门卫，检查你有没有门禁卡（token），没有就不让进需要权限的区域。

---

## 3. 第二步：用户点击登录按钮

用户填完用户名密码，点击「登录」按钮，一个完整的前后端交互就开始了。

### 3.1 页面层：Login.vue

**文件**：[frontend/src/views/Login.vue](file:///d:/AI-SecondBrain/frontend/src/views/Login.vue)

关键代码：

```javascript
const handleLogin = async () => {
  if (!loginFormRef.value) return;

  // 1. 表单校验
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        // 2. 调用登录 API
        const response = await authAPI.login(loginForm.value);
        // 3. 保存 token 和用户信息
        userStore.setToken(response.token);
        userStore.setUserInfo(response.userInfo);
        // 4. 记住用户名（可选）
        if (rememberMe.value) {
          localStorage.setItem("rememberedUsername", loginForm.value.username);
        } else {
          localStorage.removeItem("rememberedUsername");
        }
        // 5. 提示成功，跳首页
        ElMessage.success("登录成功");
        router.push("/dashboard");
      } catch (error) {
        ElMessage.error("登录失败：" + error.message);
      } finally {
        loading.value = false;
      }
    }
  });
};
```

### 3.2 API 层：auth.js

**文件**：[frontend/src/api/auth.js](file:///d:/AI-SecondBrain/frontend/src/api/auth.js)

```javascript
import request from '@/utils/request'

export const authAPI = {
  register(data) {
    return request({
      url: '/auth/register',
      method: 'post',
      data
    })
  },

  login(data) {
    return request({
      url: '/auth/login',   // 拼接 baseURL 后变成 /api/auth/login
      method: 'post',
      data
    })
  }
}
```

### 3.3 请求工具：request.js（核心）

**文件**：[frontend/src/utils/request.js](file:///d:/AI-SecondBrain/frontend/src/utils/request.js)

这是前端所有 HTTP 请求的统一入口，就像公司的前台收发室，所有进出的包裹都经过这里。

```javascript
import axios from "axios";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";

// 创建 axios 实例
const request = axios.create({
  baseURL: "/api",      // 所有请求自动加 /api 前缀
  timeout: 300000,      // 超时时间 5 分钟
});

// ========== 请求拦截器：发请求前自动执行 ==========
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore();
    // 如果有 token，自动加到请求头
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

// ========== 响应拦截器：收到响应后自动执行 ==========
request.interceptors.response.use(
  (response) => {
    const res = response.data;

    // 文件下载直接返回
    if (response.config.responseType === "blob") {
      return response.data;
    }

    // 统一处理：code===200 才算成功，直接返回 data
    if (res.code === 200) {
      return res.data;          // 注意：返回的是 res.data，不是整个 res
    } else {
      ElMessage.error(res.message || "请求失败");
      return Promise.reject(new Error(res.message || "请求失败"));
    }
  },
  (error) => {
    // 401 → token 过期，退出登录
    if (error.response && error.response.status === 401) {
      const userStore = useUserStore();
      userStore.logout();
      ElMessage.error("登录已过期，请重新登录");
    } else {
      ElMessage.error(error.message || "网络错误");
    }
    return Promise.reject(error);
  },
);

export default request;
```

**为什么需要拦截器？**

| 没有拦截器 ❌ | 有拦截器 ✅ |
|-------------|------------|
| 每个请求都要手动加 token | 自动加 Authorization header |
| 每个响应都要判断 code | 统一判断 code，直接返回 data |
| 每个请求都要处理 401 | 统一处理 token 过期 |
| 到处都是重复代码 | 代码集中，好维护 |

### 3.4 状态管理：Pinia user store

**文件**：[frontend/src/stores/user.js](file:///d:/AI-SecondBrain/frontend/src/stores/user.js)

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 从 localStorage 初始化，刷新页面不丢失登录状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)    // 持久化存储
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  const logout = () => {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const isLoggedIn = () => {
    return !!token.value   // 有 token 就算登录
  }

  return {
    token, userInfo, setToken, setUserInfo, logout, isLoggedIn
  }
})
```

**小结：前端登录 7 步**

```
① 用户填表单 → ② 点击登录 → ③ authAPI.login()
      ↓
⑦ 跳首页 ← ⑥ 存token/userInfo ← ⑤ 响应拦截器解析 ← ④ request.js 发请求
```

---

## 4. 第三步：后端接收登录请求 — Controller 层

请求穿过网络到达后端，第一层是 Controller。

**Controller 的职责 = HTTP 协议翻译官**
- 接收 HTTP 请求（URL、参数、请求体）
- 校验参数格式
- 调用 Service 处理业务
- 把结果包装成 HTTP 响应返回

**文件**：[backend/src/main/java/com/secondbrain/controller/AuthController.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/controller/AuthController.java)

```java
@RestController
@RequestMapping("/auth")                    // 类级别的路径前缀
@Tag(name = "认证接口", description = "用户登录、注册等认证相关接口")
public class AuthController {

    private final AuthService authService;  // 注入 Service

    // 构造器注入（Spring 推荐方式）
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")                 // 方法级别的路径
    @Operation(summary = "用户登录")
    public Result<LoginResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        // 1. 调用 Service 层处理业务逻辑
        LoginResponseDTO response = authService.login(loginDTO);
        // 2. 包装成统一响应格式返回
        return Result.success(response);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success("注册成功");
    }
}
```

**关键点**：
- `@RestController` = `@Controller` + `@ResponseBody`，返回的对象自动转 JSON
- `@RequestBody` — 把请求体 JSON 转成 Java 对象
- Controller 很"薄"，不写业务逻辑，只做参数校验和调用 Service

---

## 5. 第四步：业务逻辑处理 — Service 层

Service 是业务逻辑的核心，就像厨房的厨师，Controller 只负责点单，真正做菜的是 Service。

### 5.1 Service 接口

**文件**：[backend/src/main/java/com/secondbrain/service/AuthService.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/service/AuthService.java)

```java
public interface AuthService {
    void register(RegisterDTO registerDTO);
    LoginResponseDTO login(LoginDTO loginDTO);
}
```

### 5.2 Service 实现

**文件**：[backend/src/main/java/com/secondbrain/service/impl/AuthServiceImpl.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/service/impl/AuthServiceImpl.java)

```java
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;     // 数据访问层
    private final JwtUtil jwtUtil;           // JWT 工具类
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户登录
     */
    @Override
    public LoginResponseDTO login(LoginDTO loginDTO) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(wrapper);

        // 2. 用户不存在 → 抛异常
        if (user == null) {
            throw new IllegalStateException("没有该用户");
        }

        // 3. 密码比对（BCrypt 是单向哈希，不能解密，只能比对）
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new IllegalStateException("密码错误");
        }

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(
            user.getId(), user.getUsername(), user.getRole(), null
        );

        // 5. 组装响应数据
        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);

        LoginResponseDTO.UserInfo userInfo = new LoginResponseDTO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRole());
        response.setUserInfo(userInfo);

        return response;
    }
}
```

**Service 层的核心原则**：
- 只关心业务逻辑，不关心 HTTP 协议
- 业务校验失败就抛异常，由全局异常处理器统一处理
- 通过 Mapper 操作数据库，自己不写 SQL

---

## 6. 第五步：数据库查询 — Mapper 层

Mapper 是数据访问层，负责和数据库打交道。这个项目用了 MyBatis-Plus，单表 CRUD 不用写代码。

**文件**：[backend/src/main/java/com/secondbrain/mapper/UserMapper.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/mapper/UserMapper.java)

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 空的！继承 BaseMapper 就有了所有单表 CRUD 方法
    // insert、deleteById、updateById、selectById、selectList、selectPage...
}
```

**对应的实体类**：[backend/src/main/java/com/secondbrain/entity/User.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/entity/User.java)

```java
@Getter
@Setter
@TableName("user")   // 对应数据库表名
public class User {
    @TableId(type = IdType.AUTO)    // 主键自增
    private Long id;
    private String username;
    private String password;        // BCrypt 哈希后的密码
    private String email;
    private String role;            // super_admin / user
    // ... 其他字段
}
```

**BaseMapper 自带的方法（举几个例子）**：

| 方法 | 作用 |
|------|------|
| `insert(entity)` | 插入一条记录 |
| `deleteById(id)` | 根据 ID 删除 |
| `updateById(entity)` | 根据 ID 更新 |
| `selectById(id)` | 根据 ID 查询 |
| `selectOne(wrapper)` | 根据条件查询一条 |
| `selectList(wrapper)` | 根据条件查询列表 |
| `selectPage(page, wrapper)` | 分页查询 |
| `selectCount(wrapper)` | 统计数量 |

**为什么用 Mapper 层？**
- 隔离业务逻辑和数据库操作
- 换数据库不用改 Service，只换 Mapper 实现
- 单表 CRUD 零代码，提高效率

---

## 7. 第六步：JWT 认证机制

登录成功后，后端生成 JWT Token 返回给前端，后续请求前端带上这个 Token，后端通过拦截器解析。

### 7.1 JWT 工具类

**文件**：[backend/src/main/java/com/secondbrain/util/JwtUtil.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/util/JwtUtil.java)

```java
@Component
public class JwtUtil {

    @Value("${jwt.secret:ai-second-brain-secret-key-2024}")
    private String secret;          // 签名密钥

    @Value("${jwt.expiration:86400000}")
    private Long expiration;        // 过期时间（毫秒，默认 24 小时）

    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username, String role, Long currentWsId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        if (role != null) claims.put("role", role);
        if (currentWsId != null) claims.put("currentWsId", currentWsId);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)          // 自定义数据
                .setSubject(subject)        // 主题
                .setIssuedAt(now)           // 签发时间
                .setExpiration(expiryDate)  // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)  // 签名
                .compact();
    }

    /**
     * 从 Token 中提取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }
}
```

### 7.2 JWT 拦截器

**文件**：[backend/src/main/java/com/secondbrain/interceptor/JwtInterceptor.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/interceptor/JwtInterceptor.java)

```java
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头拿 token
        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);  // 去掉 "Bearer " 前缀

            try {
                // 2. 解析 token，提取用户信息
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    // 3. 把用户信息放到 request 属性里，Controller 可以直接拿
                    request.setAttribute("userId", userId);

                    String role = jwtUtil.getRoleFromToken(token);
                    if (role != null) {
                        request.setAttribute("role", role);
                    }

                    Long currentWsId = jwtUtil.getCurrentWsIdFromToken(token);
                    if (currentWsId != null) {
                        request.setAttribute("workspaceId", currentWsId);
                    }
                }
            } catch (Exception e) {
                log.error("JWT 解析失败：{}", e.getMessage());
            }
        }

        return true;  // 始终放行，具体接口自己判断要不要登录
    }
}
```

**JWT 工作原理类比**：
- Token 就像游乐场的手环，进门时工作人员给你一个
- 手环上有你的名字、VIP 等级、有效期（都加密签过名，改不了）
- 你去每个项目都亮出手环，工作人员扫一下就知道你是谁、有没有权限
- 过期了就得重新去门口换

---

## 8. 第七步：统一响应格式 Result

所有接口返回格式统一，前端处理起来才方便。

**文件**：[backend/src/main/java/com/secondbrain/common/Result.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/common/Result.java)

```java
@Getter
@Setter
public class Result<T> implements Serializable {
    private Integer code;       // 状态码：200 成功，500 失败，401 未登录
    private String message;     // 提示消息
    private T data;             // 返回数据

    // 成功响应（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // 失败响应
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}
```

**前端拿到的 JSON 长这样**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "userInfo": { "id": 1, "username": "alice" }
  }
}
```

因为 request.js 的响应拦截器会判断 `code === 200` 然后直接返回 `data`，所以前端业务代码里拿到的就是 `data` 的内容，不用每次都拆一层。

---

## 9. 第八步：请求知识点列表

登录成功后跳转到知识管理页面，加载知识点列表。流程和登录类似，我们快速过一遍。

### 9.1 前端 API

**文件**：[frontend/src/api/knowledge.js](file:///d:/AI-SecondBrain/frontend/src/api/knowledge.js)

```javascript
export const knowledgeAPI = {
  getList(params) {
    return request({
      url: "/knowledge/list",
      method: "get",
      params,     // GET 请求参数放 params，会拼到 URL 上
    });
  },
  // ... 其他方法
}
```

调用示例：
```javascript
const res = await knowledgeAPI.getList({
  current: 1,
  size: 10,
  keyword: "Java"
})
// res 直接就是 Page<KnowledgeNodeVO> 的内容
```

### 9.2 后端 Controller

**文件**：[backend/src/main/java/com/secondbrain/controller/KnowledgeController.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/controller/KnowledgeController.java)

```java
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/list")
    public Result<Page<KnowledgeNodeVO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer importance,
            @RequestParam(required = false) Integer masteryLevel,
            HttpServletRequest httpRequest) {
        // 从 request 里拿 JWT 拦截器放进去的用户信息
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = (Long) httpRequest.getAttribute("workspaceId");

        Page<KnowledgeNodeVO> page = knowledgeService.list(
            current, size, keyword, userId, importance, masteryLevel, workspaceId
        );
        return Result.success(page);
    }
}
```

### 9.3 后端 Service

**文件**：[backend/src/main/java/com/secondbrain/service/impl/KnowledgeServiceImpl.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/service/impl/KnowledgeServiceImpl.java)

```java
@Override
public Page<KnowledgeNodeVO> list(Integer current, Integer size, String keyword,
                                   Long userId, Integer importance, Integer masteryLevel,
                                   Long workspaceId) {
    // 1. 创建分页对象
    Page<KnowledgeNode> page = new Page<>(current, size);

    // 2. 构建查询条件
    LambdaQueryWrapper<KnowledgeNode> wrapper = buildBaseWrapper(userId, workspaceId);

    // 关键词搜索（标题或摘要匹配）
    if (keyword != null && !keyword.isEmpty()) {
        wrapper.and(w -> w.like(KnowledgeNode::getTitle, keyword)
                .or()
                .like(KnowledgeNode::getSummary, keyword));
    }
    // 按重要程度过滤
    if (importance != null) {
        wrapper.eq(KnowledgeNode::getImportance, importance);
    }
    // 按掌握程度过滤
    if (masteryLevel != null) {
        wrapper.eq(KnowledgeNode::getMasteryLevel, masteryLevel);
    }
    // 按创建时间倒序（最新的在前）
    wrapper.orderByDesc(KnowledgeNode::getCreateTime);

    // 3. 执行分页查询
    Page<KnowledgeNode> resultPage = knowledgeNodeMapper.selectPage(page, wrapper);

    // 4. Entity 转 VO（视图对象，去掉敏感字段、补充展示字段）
    Page<KnowledgeNodeVO> voPage = new Page<>();
    voPage.setCurrent(resultPage.getCurrent());
    voPage.setSize(resultPage.getSize());
    voPage.setTotal(resultPage.getTotal());

    List<KnowledgeNodeVO> voList = resultPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    voPage.setRecords(voList);

    return voPage;
}

// 构建基础查询条件（workspaceId 优先，否则按 userId）
private LambdaQueryWrapper<KnowledgeNode> buildBaseWrapper(Long userId, Long workspaceId) {
    LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<>();
    if (workspaceId != null) {
        wrapper.eq(KnowledgeNode::getWorkspaceId, workspaceId);
    } else {
        wrapper.eq(KnowledgeNode::getUserId, userId);
    }
    return wrapper;
}
```

### 9.4 Mapper 层

**文件**：[backend/src/main/java/com/secondbrain/mapper/KnowledgeNodeMapper.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/mapper/KnowledgeNodeMapper.java)

```java
@Mapper
public interface KnowledgeNodeMapper extends BaseMapper<KnowledgeNode> {
    // 又是空的！单表查询全靠 BaseMapper
}
```

---

## 10. 关键类关系全景图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                          │
│                                                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────┐ │
│  │  views/     │───▶│  api/       │───▶│ utils/request.js│ │
│  │  页面组件    │    │ API 封装     │    │  (拦截器+统一格式)│ │
│  └─────────────┘    └─────────────┘    └────────┬────────┘ │
│                                                 │          │
│  ┌─────────────┐    ┌─────────────┐             │ HTTP     │
│  │ stores/     │◀───│ router/     │             │          │
│  │ Pinia 状态  │    │ 路由+守卫    │             │          │
│  └─────────────┘    └─────────────┘             │          │
└─────────────────────────────────────────────────┼──────────┘
                                                  │
═══════════════════════════════════════════════════╪═══════════
                                                  │
┌─────────────────────────────────────────────────┼──────────┐
│                  后端 (Spring Boot)              │          │
│                                                 │          │
│  ┌─────────────────┐    ┌───────────────────┐  ▼          │
│  │   Controller    │───▶│     Service       │             │
│  │  (HTTP 协议层)   │    │   (业务逻辑层)     │             │
│  │  AuthController │    │  AuthServiceImpl  │             │
│  │  Knowledge...   │    │  Knowledge...Impl │             │
│  └─────────────────┘    └─────────┬─────────┘             │
│                                    │ 调用                   │
│  ┌─────────────────┐    ┌─────────▼─────────┐             │
│  │   Entity        │◀──▶│     Mapper        │             │
│  │  (数据库映射)    │    │   (数据访问层)     │             │
│  │  User           │    │  UserMapper       │             │
│  │  KnowledgeNode  │    │  KnowledgeNode... │             │
│  └─────────────────┘    └─────────┬─────────┘             │
│                                    │                        │
│  ┌─────────────────┐    ┌─────────▼─────────┐             │
│  │  Config / Util  │    │       MySQL       │             │
│  │  JwtUtil        │    └───────────────────┘             │
│  │  JwtInterceptor │                                      │
│  │  Result         │                                      │
│  └─────────────────┘                                      │
└────────────────────────────────────────────────────────────┘
```

各层职责速查表：

| 层 | 做什么 | 不做什么 |
|----|--------|----------|
| View (页面) | 渲染 UI、用户交互 | 直接调后端、写业务逻辑 |
| API 层 | 封装请求 URL 和参数 | 处理业务状态 |
| Store | 管理全局状态 | 发 HTTP 请求 |
| Controller | 接收 HTTP 请求、参数校验 | 写业务逻辑、直接操作数据库 |
| Service | 业务逻辑处理、事务控制 | 操作 HTTP、直接拼 SQL |
| Mapper | 数据库 CRUD | 写业务逻辑 |
| Entity | 数据库表映射 | 参与前端展示 |
| VO | 前端展示数据 | 直接存数据库 |

---

## 11. 新建一个模块的最小代码框架

想加一个新功能？照着下面的模板抄就行。以"通知模块"为例。

### 11.1 后端文件清单

```
backend/src/main/java/com/secondbrain/
├── entity/
│   └── Notification.java          // 数据库实体
├── mapper/
│   └── NotificationMapper.java    // Mapper 接口
├── dto/
│   └── NotificationDTO.java       // 请求参数
├── vo/
│   └── NotificationVO.java        // 响应视图对象
├── service/
│   ├── NotificationService.java   // Service 接口
│   └── impl/
│       └── NotificationServiceImpl.java  // Service 实现
└── controller/
    └── NotificationController.java       // REST 控制器
```

### 11.2 每个文件的最小内容

**Entity（数据库实体）**
```java
@Getter
@Setter
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Integer status;      // 0-未读，1-已读
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
```

**Mapper（数据访问层）**
```java
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
```

**Service 接口**
```java
public interface NotificationService {
    Page<NotificationVO> list(Integer current, Integer size, Long userId);
    NotificationVO getById(Long id, Long userId);
    void create(NotificationDTO dto, Long userId);
    void deleteById(Long id, Long userId);
}
```

**Service 实现**
```java
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public Page<NotificationVO> list(Integer current, Integer size, Long userId) {
        Page<Notification> page = new Page<>(current, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.orderByDesc(Notification::getCreateTime);

        Page<Notification> resultPage = notificationMapper.selectPage(page, wrapper);

        Page<NotificationVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());
        voPage.setRecords(resultPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList()));
        return voPage;
    }

    private NotificationVO convertToVO(Notification entity) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
```

**Controller**
```java
@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    public Result<Page<NotificationVO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(notificationService.list(current, size, userId));
    }
}
```

### 11.3 前端文件清单

```
frontend/src/
├── api/
│   └── notification.js            // API 请求封装
├── views/
│   └── Notifications.vue          // 页面组件
└── stores/
    └── notification.js            // 状态管理（复杂模块才需要）
```

**API 封装**
```javascript
import request from "@/utils/request";

export const notificationAPI = {
  getList(params) {
    return request({
      url: "/notification/list",
      method: "get",
      params,
    });
  },
};
```

---

## 12. 移动端：从启动到复习答题的完整链路

前面 1~11 节走的是 **Web 端** 的登录流程。这一节换成**移动端微信小程序**，看同一套后端契约如何被 uni-app 消费。重点看 3 件事：① 启动时怎么恢复登录态；② TabBar 切换"复习"时怎么拉取今日卡片；③ 提交一道题的答题结果。

### 12.1 启动：从本地存储恢复 token

`mobile/src/main.js` 用 `createSSRApp` 创建应用后，`App.vue` 的 `onLaunch` 钩子会尝试恢复登录态：

```javascript
// mobile/src/App.vue（核心逻辑）
onLaunch(() => {
  const token = uni.getStorageSync('token');
  const userInfo = uni.getStorageSync('userInfo');
  if (token && userInfo) {
    useUserStore().setToken(token);
    useUserStore().setUserInfo(userInfo);
  } else {
    uni.reLaunch({ url: '/pages/login/index' });  // 没登录跳登录
  }
});
```

> 和 Web 端的差别：Web 端是 `localStorage.getItem`，移动端必须 `uni.getStorageSync`。后端完全不感知差别——同样解析 `Authorization: Bearer <token>`。

### 12.2 切换到"复习"Tab：拉取今日卡片

`mobile/src/pages.json` 里 TabBar 第 3 项是 `pages/review/index`。用户点底部"复习"图标后，uni-app 路由到该页，`onShow` 钩子触发请求：

```javascript
// mobile/src/pages/review/index.vue
import { reviewAPI } from '@/api/review';

async function loadTodayCards() {
  const cards = await reviewAPI.getTodayCards({ workspaceId: currentWorkspaceId });
  cardList.value = cards;
}

onShow(() => {
  // 每次切到这个 tab 都刷新（因为答题后卡片状态会变）
  loadTodayCards();
});
```

后端对应 `ReviewCardController.getTodayReviewCards`，Service 层按 `nextReviewTime <= now` 过滤。

### 12.3 提交答题：答对/答错影响下次复习时间

```javascript
// mobile/src/pages/review/answer.vue
async function submitAnswer(cardId, userAnswer) {
  const result = await reviewAPI.submitReviewResult({
    cardId,
    userAnswer,
    duration: 30,  // 答题耗时（秒）
  });
  // result 里带回：是否答对、下次复习时间、更新后的掌握度
  if (result.isCorrect) {
    uni.showToast({ title: '答对了！', icon: 'success' });
  } else {
    uni.showToast({ title: '答错了，已安排重学', icon: 'none' });
  }
}
```

后端 `ReviewCardServiceImpl.submitReviewResult` 内部：
1. 记录 `ReviewLog`
2. 调 `EbbinghausService.calculateNextReviewInterval(reviewCount, isCorrect)` 算下次间隔
3. 更新 `ReviewCard.nextReviewTime`
4. 如果答对次数达阈值，`masteryLevel++`
5. 同时给用户加积分（`GamificationService`）—— 这就是"复习即赚积分"的闭环

整条链路涉及的真实文件：

| 层 | 文件 |
|---|------|
| 移动端页面 | `mobile/src/pages/review/index.vue`、`answer.vue` |
| 移动端 API | `mobile/src/api/review.js` |
| 后端 Controller | `ReviewCardController.java` |
| 后端 Service | `ReviewCardServiceImpl.java` + `EbbinghausServiceImpl.java` |
| 数据库 | `review_card`、`review_log`、`points_log` |

---

## 13. 浏览器扩展：一键采集 AI 对话到知识库

扩展的"采集"流程是整个项目里**唯一不走 Nginx、直接打后端**的链路，也是连接"外部 AI 平台"和"我的知识库"的关键桥梁。

### 13.1 content.js 在 AI 平台页面注入按钮

扩展声明（`extension/manifest.json`）让 `content.js` 注入到 `chatgpt.com`、`kimi.com`、`doubao.com` 等 9 个域名。脚本 `document_idle` 时执行，找到对话容器后插入一个"采集到 AI-SecondBrain"浮动按钮：

```javascript
// extension/content.js（核心思路）
const btn = document.createElement('button');
btn.textContent = '采集';
btn.onclick = async () => {
  const conversation = parseConversationFromDOM();  // 从页面 DOM 抽取用户/AI 消息
  chrome.runtime.sendMessage({
    type: 'CAPTURE',
    payload: { platform: 'chatgpt', content: conversation }
  });
};
document.body.appendChild(btn);
```

### 13.2 background.js 转发到后端

content.js 不能直接发网络请求到跨域后端（受 CSP 限制），所以把消息丢给 Service Worker 转发：

```javascript
// extension/background.js
chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if (msg.type === 'CAPTURE') {
    chrome.storage.local.get(['apiBase', 'token'], async (cfg) => {
      await fetch(`${cfg.apiBase}/api/capture`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${cfg.token}`
        },
        body: JSON.stringify(msg.payload)
      });
      sendResponse({ ok: true });
    });
    return true;  // 异步响应必须 return true
  }
});
```

### 13.3 后端：先落 pending_knowledge，不直接入库

`CaptureController` 接收后调 `KnowledgeCaptureService`，AI 抽摘要、估重要性、打标签建议，**全部存进 `pending_knowledge` 表**（不写 `knowledge_node`），等用户在 Web 端"待确认"列表里二次处理。

```
ChatGPT 网页 (content.js)
    │  解析 DOM 抽取对话
    ▼
background.js
    │  fetch POST /api/capture  (带 JWT)
    ▼
CaptureController
    │
    ▼
KnowledgeCaptureService.createPending()
    │  1. AI 抽摘要/标签/重要性
    │  2. 写 pending_knowledge 表（status=PENDING）
    ▼
返回 pendingId + 推荐的标题/标签
    │
    │  用户在 Web 端"待确认"页：
    │    确认 → 转 knowledge_node + 发积分
    │    丢弃 → 删 pending 记录
```

**为什么不直接入库**：这是思想 11（PendingKnowledge 中间态）的体现——AI 推荐的内容必须经用户确认，否则知识库会被污染。

---

## 14. DeerFlow 研究报告：Java → Python → 通义千问 的跨语言调用

DeerFlow 是项目里**唯一的 Python 服务**，专门处理"长文生成"这类 Java 不擅长的活。这里走一遍"生成学习报告"的完整链路。

### 14.1 前端发起

`frontend/src/api/deerflow.js` 里封装：

```javascript
// frontend/src/api/deerflow.js
export const deerflowAPI = {
  generateLearningReport(data) {
    return request({
      url: '/deerflow/learning-report',
      method: 'post',
      data,  // { prompt, apiKey? }
    });
  },
};
```

### 14.2 Java 后端做协议适配 + 鉴权透传

`DeerFlowResearchController` → `DeerFlowResearchService` 用 RestTemplate 调 `DEERFLOW_API_URL`：

```java
// DeerFlowResearchServiceImpl（核心思路）
String url = deerflowApiUrl + "/api/research/learning-report";
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
return resp.getBody();
```

> **为什么不直接让前端调 deerflow:8000？** 因为 deerflow 在内网不暴露，且它本身不处理登录鉴权。由 Java 后端做鉴权网关，既安全又能统一日志。

### 14.3 DeerFlow 调通义千问

`deerflow/app.py` 的 `call_qwen_api(prompt, user_api_key)`：

```python
def call_qwen_api(prompt, user_api_key=None):
    api_key = user_api_key if user_api_key else QWEN_API_KEY
    url = f"{QWEN_BASE_URL}/chat/completions"
    data = {'model': QWEN_MODEL, 'messages': [{'role': 'user', 'content': prompt}],
            'temperature': 0.7, 'max_tokens': 4000}
    response = requests.post(url, headers={'Authorization': f'Bearer {api_key}'},
                             json=data, timeout=300)
    return response.json()['choices'][0]['message']['content']
```

返回的 Markdown 报告沿原路回到前端展示。

### 14.4 整条链路的文件清单

| 步骤 | 文件 |
|-----|------|
| 前端调用 | `frontend/src/api/deerflow.js` |
| 后端控制器 | `DeerFlowResearchController.java` |
| 后端服务 | `DeerFlowResearchServiceImpl.java`（RestTemplate） |
| Python 入口 | `deerflow/app.py` |
| Python 报告服务 | `deerflow/deerflow_report.py` |

---

## 15. 代码阅读路线建议

按这个顺序读，循序渐进不迷路：

| 顺序 | 推荐阅读路径 | 目的 |
|------|------------|------|
| 1 | [main.js](file:///d:/AI-SecondBrain/frontend/src/main.js) + [App.vue](file:///d:/AI-SecondBrain/frontend/src/App.vue) + [router/index.js](file:///d:/AI-SecondBrain/frontend/src/router/index.js) | 了解前端入口和页面结构 |
| 2 | [utils/request.js](file:///d:/AI-SecondBrain/frontend/src/utils/request.js) + [stores/user.js](file:///d:/AI-SecondBrain/frontend/src/stores/user.js) | 理解 API 请求机制和认证逻辑 |
| 3 | AiSecondBrainApplication.java | 找到后端入口 |
| 4 | [Result.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/common/Result.java) + GlobalExceptionHandler.java | 理解统一响应格式 |
| 5 | [JwtInterceptor.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/interceptor/JwtInterceptor.java) + [JwtUtil.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/util/JwtUtil.java) | 理解认证机制 |
| 6 | [AuthController.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/controller/AuthController.java) → [AuthService.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/service/AuthService.java) → [AuthServiceImpl.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/service/impl/AuthServiceImpl.java) | 跟着登录流程走通三层架构 |
| 7 | [KnowledgeController.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/controller/KnowledgeController.java) → [KnowledgeServiceImpl.java](file:///d:/AI-SecondBrain/backend/src/main/java/com/secondbrain/service/impl/KnowledgeServiceImpl.java) | 看一个完整 CRUD 模块 |
| 8 | application.yml | 了解所有配置项 |

---

## 速查表：常见问题定位

| 现象 | 先查哪里 |
|------|---------|
| 前端报 401 | request.js 拦截器、JwtInterceptor、token 是否过期 |
| 前端报 500 | 后端日志、Service 层异常、GlobalExceptionHandler |
| 查不到数据 | Mapper、QueryWrapper 条件、workspaceId 是否正确 |
| 登录失败 | AuthServiceImpl、密码 BCrypt 比对、用户是否存在 |
| 接口返回 null | Controller 是否调对 Service、Service 是否返回正确 |
| 页面跳不动 | router/index.js 路由配置、路由守卫逻辑 |
