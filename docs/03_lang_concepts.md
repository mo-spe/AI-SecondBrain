> 本篇讲解 AI-SecondBrain 项目中实际用到的 Java / Vue 语言特性和框架约定，帮你快速读懂代码。

---

## 一、后端 Java 特性

### 特性 1：Lombok 注解 —— 消除样板代码

**项目里在哪用**：所有 Entity / DTO / VO / Result 类都用 `@Getter` `@Setter`，例如 [`Result.java`](../backend/src/main/java/com/secondbrain/common/Result.java)。

**代码示例**（来自 `Result.java`）：

```java
@Getter
@Setter
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;
}
```

**没有这个特性时怎么写**：

```java
// ❌ 没有 Lombok —— 3 个字段要写 6 个方法，几十行模板代码
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
```

**要点速记**：

| 注解 | 作用 | 项目里用在哪 |
|------|------|-------------|
| `@Getter` / `@Setter` | 自动生成 getter / setter 方法 | Entity、DTO、VO、Result |
| `@Slf4j` | 自动生成 `log` 对象，直接 `log.info(...)` | Service、Controller |
| `@Data` | 一次性生成 getter/setter/equals/hashCode/toString | **Entity 禁用**（见下方说明） |

> ⚠️ **Entity 类只用 `@Getter @Setter`，绝对不要用 `@Data`**
> 
> `@Data` 会自动生成 `equals()` 和 `hashCode()`，但 JPA / MyBatis 的实体对象如果用了自动生成的 equals/hashCode，在懒加载、代理对象、集合操作时会出诡异的问题。项目约定 Entity 只加 `@Getter @Setter`。

---

### 特性 2：MyBatis-Plus —— 单表 CRUD 零 SQL

**项目里在哪用**：所有 Mapper 接口继承 `BaseMapper<T>`，所有 Service 实现类继承 `ServiceImpl<Mapper, Entity>`。

**代码示例**（Mapper 层）：

```java
public interface KnowledgeNodeMapper extends BaseMapper<KnowledgeNode> {
    // 什么都不用写，自动拥有 17+ 个基础方法
}
```

**代码示例**（Service 层）：

```java
@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeNodeMapper, KnowledgeNode>
        implements KnowledgeService {
    // 直接用 this.getById()、this.list()、this.save() 等方法
}
```

**没有这个特性时怎么写**：

```
❌ 原生 MyBatis：
  knowledge_node 表 →
    KnowledgeNodeMapper.java  +  5 个 CRUD 方法声明
    KnowledgeNodeMapper.xml   +  5 段 SQL
    KnowledgeService.java     +  5 个方法
  一张表至少 200 行，10 张表就是 2000 行模板代码
```

**要点速记**：

| 层级 | 继承 / 实现 | 获得的能力 |
|------|------------|-----------|
| Mapper | `extends BaseMapper<T>` | `insert` / `deleteById` / `selectById` / `updateById` / `selectList` 等 17+ 方法 |
| ServiceImpl | `extends ServiceImpl<M, T>` | 批量操作、分页、链式查询、`saveOrUpdate` 等高级方法 |

> 💡 **什么时候需要自己写 SQL？**
> 
> 单表操作 → 用 MyBatis-Plus 自带方法 / `QueryWrapper`
> 多表联查、复杂聚合 → 自己写 XML 或 `@Select` 注解

---

### 特性 3：构造器注入 —— Spring 推荐的依赖注入方式

**项目里在哪用**：所有 Service、Controller、Config 都用构造器注入，**没有 `@Autowired` 字段注入**。

**代码示例**（来自 `JwtInterceptor.java`）：

```java
@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
}
```

**没有这个特性时怎么写**（字段注入 —— 不推荐）：

```java
// ❌ 字段注入 - 不推荐
@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;  // 不是 final，可能被意外修改
                                // 看类不知道依赖了什么，得翻遍整个类
}
```

**要点速记**：

| 好处 | 说明 |
|------|------|
| **依赖不可变** | 字段用 `final` 修饰，注入后不会被改 |
| **依赖关系可见** | 看构造器参数就知道这个类依赖了什么 |
| **方便单元测试** | 直接 `new JwtInterceptor(mockJwtUtil)`，不需要启动 Spring |
| **循环依赖早发现** | 构造器注入在启动时就报错，而不是运行时才发现 |

> 💡 **小技巧**：如果依赖很多，可以用 Lombok 的 `@RequiredArgsConstructor` 自动生成构造器（项目里用得不多，但知道有这个东西就行）。
>
> ```java
> @Service
> @RequiredArgsConstructor
> public class KnowledgeServiceImpl implements KnowledgeService {
>     private final KnowledgeNodeMapper knowledgeNodeMapper;
>     private final UserService userService;
>     // 自动生成带所有 final 字段的构造器
> }
> ```

---

### 特性 4：接口与实现分离 —— 面向接口编程

**项目里在哪用**：所有 Service 都是「接口 + Impl 实现类」的模式，例如 [`KnowledgeService.java`](../backend/src/main/java/com/secondbrain/service/KnowledgeService.java) + `KnowledgeServiceImpl.java`。

**代码示例**：

```java
// 接口 - 定义"能做什么"
public interface KnowledgeService {
    Page<KnowledgeNodeVO> list(Integer current, Integer size, Long userId, Long workspaceId);
    KnowledgeNodeVO getById(Long id, Long userId, Long workspaceId);
    KnowledgeNodeVO create(KnowledgeNodeCreateDTO dto, Long userId, Long workspaceId);
    void delete(Long id, Long userId, Long workspaceId);
}
```

```java
// 实现类 - 定义"怎么做"
@Service
public class KnowledgeServiceImpl implements KnowledgeService {
    // 具体实现逻辑...
}
```

**Controller 只依赖接口**：

```java
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;  // 依赖接口，不依赖具体类

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }
}
```

**没有这个特性时怎么写**：

```
❌ Controller 直接依赖 KnowledgeServiceImpl
   → 哪天想换成 ElasticsearchKnowledgeServiceImpl
   → 所有引用的地方都要改
   → 单元测试没法 mock 接口
```

**要点速记**：

| 好处 | 说明 |
|------|------|
| **解耦** | 调用方只关心"能做什么"，不关心"怎么做" |
| **方便 Mock 测试** | 测试 Controller 时可以给个假的 Service 实现 |
| **多实现支持** | 比如 `SearchService` 可以有 `ElasticsearchSearchServiceImpl` 和 `NoOpSearchServiceImpl` |

> 💡 **什么时候不需要接口？**
> 
> 如果确定不会有第二个实现（比如工具类、配置类），可以不用接口，直接写实现类。为了接口而接口是过度设计。

---

### 特性 5：Java 时间 API（LocalDateTime）—— 告别 Date

**项目里在哪用**：所有时间字段都用 `LocalDateTime`，不用 `Date`。

**代码示例**（Entity 中的时间字段）：

```java
@Getter
@Setter
public class KnowledgeNode {
    private Long id;
    private String title;
    private LocalDateTime createTime;  // ✅ 用 LocalDateTime
    private LocalDateTime updateTime;  // ✅ 用 LocalDateTime
}
```

**没有这个特性时怎么写**：

```java
// ❌ 用 Date 的老写法
private Date createTime;
// 问题：
// - Date 是可变的，线程不安全
// - 格式化要写 SimpleDateFormat（也线程不安全）
// - 时区处理麻烦
// - API 难用（月份从 0 开始？！）
```

**要点速记**：

| 类 | 用途 | 不可变 | 线程安全 |
|----|------|--------|---------|
| `LocalDateTime` | 日期 + 时间（无时区） | ✅ | ✅ |
| `LocalDate` | 仅日期 | ✅ | ✅ |
| `LocalTime` | 仅时间 | ✅ | ✅ |
| ~~`Date`~~ | 老 API，别用 | ❌ | ❌ |

> 💡 **MyBatis-Plus 自动映射**
> 
> 数据库的 `datetime` 类型会自动映射到 Java 的 `LocalDateTime`，不需要额外配置。

---

## 二、前端 Vue 3 特性

### 特性 6：Composition API + `<script setup>` —— Vue 3 推荐写法

**项目里在哪用**：所有 Vue 组件都用 `<script setup>` 语法糖。

**代码示例**（Pinia Store 的 Setup 写法，来自 [`stores/user.js`](../frontend/src/stores/user.js)）：

```javascript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
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

  const isLoggedIn = computed(() => !!token.value)

  return { token, userInfo, setToken, setUserInfo, logout, isLoggedIn }
})
```

**没有这个特性时怎么写**（Options API）：

```javascript
// ❌ Options API - 一个功能的代码分散在不同选项里
export default defineStore('user', {
  state: () => ({
    token: '',
    userInfo: {}
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    setToken(newToken) {
      this.token = newToken
      localStorage.setItem('token', newToken)
    },
    // ...
  }
})
```

**要点速记**：

| 概念 | 说明 |
|------|------|
| `<script setup>` | Vue 3 编译期语法糖，写 Composition API 更简洁 |
| `ref()` | 定义基本类型的响应式数据，访问用 `.value` |
| `reactive()` | 定义对象类型的响应式数据，直接访问属性 |
| `computed()` | 计算属性，依赖变化时自动更新 |
| `return { ... }` | Setup 函数里 return 的变量/函数，模板里直接用 |

> 💡 **为什么 Composition API 更好？**
> 
> ```
> Options API：
>   data()     { ... 用户相关 ... }
>   methods:   { ... 用户相关 ... 搜索相关 ... }
>   computed:  { ... 搜索相关 ... 用户相关 ... }
>   → 一个功能的代码被拆得七零八落
>
> Composition API：
>   // 用户相关逻辑放一起
>   const token = ref('')
>   const login = () => { ... }
>
>   // 搜索相关逻辑放一起
>   const keywords = ref('')
>   const search = () => { ... }
>   → 代码按功能组织，读起来更顺
> ```

---

### 特性 7：Pinia —— Vue 3 官方状态管理

**项目里在哪用**：用户状态、工作区状态、主题、游戏化数据都存在 Pinia 里。

**代码示例**（定义 Store，来自 [`stores/user.js`](../frontend/src/stores/user.js)）：

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const setToken = (val) => { token.value = val }
  return { token, setToken }
})
```

**代码示例**（在组件中使用）：

```vue
<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 读
console.log(userStore.token)

// 写
userStore.setToken('abc123')
</script>

<template>
  <div v-if="userStore.isLoggedIn">已登录</div>
</template>
```

**没有这个特性时怎么写**：

```
❌ 只用 props / emit：
  爷爷组件 → 爸爸组件 → 儿子组件 → 孙子组件
  每层都要传一遍 token（"prop drilling"）
  孙子要改 token 得 emit 一路传上去
  10 层嵌套就是地狱
```

**要点速记**：

| 概念 | 说明 |
|------|------|
| `defineStore('id', () => {...})` | 定义一个 Store，第一个参数是唯一 id |
| `useXxxStore()` | 获取 Store 实例 |
| Setup 风格 | 项目用的写法，像写 Composition API 一样写 Store |
| Options 风格 | 类似 Vuex 的写法（state / getters / actions），项目里没用到 |

> 💡 **Pinia vs Vuex**
> 
> Pinia 是 Vue 3 官方推荐的状态管理，是 Vuex 的"继任者"。
> 区别：Pinia 没有 mutations、更简单、TypeScript 支持更好、体积更小。

---

### 特性 8：Axios 拦截器 —— 统一请求处理

**项目里在哪用**：[`utils/request.js`](../frontend/src/utils/request.js) 封装了 axios，所有 API 调用都用它。

**代码示例**（来自 `request.js`）：

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: "/api",
  timeout: 300000,
})

// 请求拦截器：自动加 token
request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data  // ✅ 直接返回数据，不用每次都 .data.data
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
```

**没有这个特性时怎么写**：

```javascript
// ❌ 每个 API 调用都要手动处理
async function login(username, password) {
  try {
    const res = await axios.post('/api/auth/login', {
      username,
      password
    }, {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`  // 每次都要加
      }
    })
    
    if (res.data.code === 200) {  // 每次都要判断
      return res.data.data
    } else {
      ElMessage.error(res.data.message)  // 每次都要弹错
    }
  } catch (e) {
    if (e.response?.status === 401) {  // 每次都要处理 401
      // 跳登录...
    }
  }
}
```

**要点速记**：

| 拦截器 | 作用 |
|--------|------|
| **请求拦截器** | 发请求前自动加 `Authorization: Bearer <token>` |
| **响应拦截器** | `code === 200` 直接返回 `data`；非 200 自动弹错误；401 自动跳登录 |

> 💡 **调用 API 有多爽？**
> 
> ```javascript
> import request from '@/utils/request'
> 
> // 不用加 token，不用判断 code，不用弹错误
> // 成功直接拿数据，失败自动提示
> export function login(username, password) {
>   return request.post('/auth/login', { username, password })
> }
> ```

---

## 三、命名规范速查表

### 后端命名

| 类型 | 规则 | 例子 |
|------|------|------|
| 包名 | 全小写，点分隔 | `com.secondbrain.controller` |
| 类名 | 大驼峰（PascalCase） | `KnowledgeNode`、`AuthController` |
| 接口名 | 大驼峰，描述能力 | `KnowledgeService` |
| 实现类名 | 接口名 + `Impl` | `KnowledgeServiceImpl` |
| 方法名 | 小驼峰（camelCase） | `getById`、`create`、`deleteById` |
| 常量 | 全大写下划线分隔 | `DEFAULT_MODEL` |
| 变量名 | 小驼峰 | `userId`、`workspaceId` |
| DTO 后缀 | 入参对象 | `LoginDTO`、`KnowledgeNodeCreateDTO` |
| VO 后缀 | 出参对象 | `KnowledgeNodeVO`、`UserVO` |
| 数据库表名 | 下划线分隔 | `knowledge_node`、`review_card` |
| 数据库字段 | 下划线分隔 | `create_time`、`workspace_id` |

### 前端命名

| 类型 | 规则 | 例子 |
|------|------|------|
| 组件文件名 | 大驼峰 | `Knowledge.vue`、`MainLayout.vue` |
| 组件名 | 大驼峰（与文件名一致） | `Knowledge` |
| 变量 / 函数 | 小驼峰 | `token`、`setToken`、`isLoggedIn` |
| 常量 | 全大写下划线 | `BASE_URL` |
| Store 文件名 | 功能名（小驼峰） | `user.js`、`workspace.js` |
| Store 函数名 | `use` + 功能 + `Store` | `useUserStore`、`useWorkspaceStore` |
| API 文件名 | 模块名 | `auth.js`、`knowledge.js` |
| CSS 类名 | kebab-case（中划线） | `.main-container`、`.knowledge-tree` |

---

### 检查清单 ✅

读代码前，确认你知道这些：

- [ ] 看到 `@Getter @Setter` 知道是 Lombok，不用找 getter/setter 方法
- [ ] 看到 `extends BaseMapper<T>` 知道自带 CRUD 方法
- [ ] 看到构造器里一堆 `private final Xxx xxx` 知道是 Spring 注入
- [ ] 看到 `interface XxxService` + `class XxxServiceImpl` 知道是接口分离
- [ ] 看到 `LocalDateTime` 知道是新时间 API，别用 `Date`
- [ ] 看到 `<script setup>` 知道是 Vue 3 组合式 API
- [ ] 看到 `useXxxStore()` 知道是 Pinia 状态管理
- [ ] 看到 `import request from '@/utils/request'` 知道是封装好的 axios
