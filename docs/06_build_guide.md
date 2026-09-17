> 本篇目标：让刚接触项目的新人看懂 AI-SecondBrain 的构建系统，知道怎么编译、怎么运行、出了问题怎么排查。

---

## 0. 构建系统是什么，为什么用它

### 0.1 双构建体系概览

AI-SecondBrain 采用 **Maven + Vite 双构建体系**，后端用 Maven，前端用 Vite，各自独立构建，最后通过 Nginx 或 Docker 整合在一起。

```
┌─────────────────────────────────────────────────────────────┐
│                        项目根目录                            │
│  ┌──────────────────┐          ┌──────────────────┐        │
│  │   pom.xml        │          │  frontend/       │        │
│  │   (Maven 构建)    │          │  package.json    │        │
│  │                  │          │  vite.config.js  │        │
│  │  后端 Java 代码   │          │  (Vite 构建)      │        │
│  │  backend/src/    │          │                  │        │
│  │                  │          │  前端 Vue 代码    │        │
│  │  输出: .jar 文件  │          │  frontend/src/   │        │
│  └────────┬─────────┘          └────────┬─────────┘        │
│           │                            │                   │
│           ▼                            ▼                   │
│  ai-second-brain-1.0.0.jar      dist/ (静态资源)            │
└─────────────────────────────────────────────────────────────┘
```

### 0.2 为什么后端用 Maven 而不是 Gradle

| 对比项 | Maven（本项目选用） | Gradle |
|-------|-------------------|--------|
| 配置方式 | XML，啰嗦但规范 | Groovy/Kotlin DSL，灵活但容易写乱 |
| 学习曲线 | 低，插件生态成熟 | 中，需要学 DSL |
| 构建速度 | 中等 | 快（增量构建） |
| 企业接受度 | 高，Spring 官方默认 | 逐渐上升 |
| 本项目选择理由 | Spring Boot 官方父 POM 开箱即用，团队成员更熟悉 | — |

**一句话总结**：Maven 像"标准菜谱"，步骤固定但可靠；Gradle 像"自由烹饪"，灵活但考验功底。本项目选 Maven，因为 Spring Boot 生态支持最好，团队上手最快。

### 0.3 为什么前端用 Vite 而不是 Webpack

| 对比项 | Vite（本项目选用） | Webpack |
|-------|-------------------|---------|
| 开发启动速度 | 极快（按需编译，秒开） | 慢（全量构建，大项目几分钟） |
| 热更新速度 | 快（HMR 毫秒级） | 中等 |
| 配置复杂度 | 低，开箱即用 | 高，各种 loader/plugin 配置 |
| 生产构建 | Rollup 打包 | Webpack 打包 |
| 本项目选择理由 | Vue 3 官方推荐，开发体验好 | — |

**一句话总结**：Vite 是"电动车"，起步快、加速猛；Webpack 是"燃油车"，成熟稳定但笨重。本项目用 Vite，因为开发效率高，Vue 3 配合最佳。

### 0.4 为什么 pom.xml 在根目录，源码在 backend/

这是本项目一个**特殊的布局**，需要特别注意：

```
项目根目录/
├── pom.xml              ← Maven 配置在这里
├── backend/             ← 但 Java 源码在这里
│   └── src/
│       ├── main/java/
│       └── main/resources/
└── frontend/
    └── ...
```

通常 Maven 项目的源码在 `src/main/java`，和 pom.xml 同级。但本项目因为前后端代码放在同一个仓库里，为了目录整洁，把 Java 源码挪到了 `backend/` 子目录下。

为了让 Maven 能找到源码，pom.xml 里做了自定义配置（后面会详细讲）。

---

## 1. 核心语法速查

### 1.1 Maven 常用标签（pom.xml）

只列本项目里**实际用到**的标签，按出现顺序排列：

| 标签 | 作用 | 本项目值 |
|------|------|---------|
| `<parent>` | 继承父 POM，省去大量重复配置 | spring-boot-starter-parent 3.1.5 |
| `<groupId>` | 组织唯一标识（反向域名） | com.secondbrain |
| `<artifactId>` | 项目唯一标识 | ai-second-brain |
| `<version>` | 版本号 | 1.0.0 |
| `<properties>` | 定义属性变量，类似常量 | java.version=17 等 |
| `<dependencies>` | 依赖列表 | — |
| `<dependency>` | 单个依赖 | — |
| `<groupId>/<artifactId>` | 依赖坐标 | 见依赖表 |
| `<version>` | 依赖版本 | 可使用 `${变量名}` 引用 properties |
| `<scope>` | 依赖范围 | compile/test/runtime/provided |
| `<optional>` | 是否可选（不传递） | true（Lombok） |
| `<build>` | 构建配置 | — |
| `<sourceDirectory>` | 源码目录（自定义） | backend/src/main/java |
| `<testSourceDirectory>` | 测试源码目录 | backend/src/test/java |
| `<resources>` | 资源文件目录 | backend/src/main/resources |
| `<plugins>` | 构建插件列表 | — |

**依赖范围（scope）说明**：

| scope | 编译时 | 测试时 | 运行时 | 打包进 jar | 例子 |
|-------|--------|--------|--------|-----------|------|
| compile（默认） | ✅ | ✅ | ✅ | ✅ | spring-boot-starter-web |
| test | ❌ | ✅ | ❌ | ❌ | spring-boot-starter-test |
| runtime | ❌ | ✅ | ✅ | ✅ | mysql-connector-java |
| provided | ✅ | ✅ | ❌ | ❌ | lombok（编译时用，运行时不需要） |

### 1.2 npm 常用命令

| 命令 | 作用 | 等价于 |
|------|------|--------|
| `npm install` | 安装 package.json 里所有依赖 | `npm i` |
| `npm run dev` | 启动开发服务器（带热更新） | `vite` |
| `npm run build` | 构建生产版本到 dist/ | `vite build` |
| `npm run preview` | 本地预览构建结果 | `vite preview` |
| `npm install <包名>` | 安装指定包到 dependencies | `npm i <包名>` |
| `npm install -D <包名>` | 安装指定包到 devDependencies | `npm i -D <包名>` |

**dependencies vs devDependencies**：

| 类型 | 含义 | 例子 |
|------|------|------|
| dependencies | 生产环境也需要的依赖 | vue, axios, element-plus |
| devDependencies | 只在开发/构建时需要 | vite, @vitejs/plugin-vue |

### 1.3 Docker Compose 常用命令

| 命令 | 作用 |
|------|------|
| `docker-compose up -d` | 后台启动所有服务 |
| `docker-compose down` | 停止并删除所有容器 |
| `docker-compose ps` | 查看服务状态 |
| `docker-compose logs -f` | 实时查看所有日志 |
| `docker-compose logs -f backend` | 查看指定服务日志 |
| `docker-compose restart backend` | 重启指定服务 |

---

## 2. 逐文件注释

### 2.1 pom.xml 关键部分注释

完整文件见 `d:\AI-SecondBrain\pom.xml`，这里只讲关键部分。

#### 2.1.1 父 POM 与项目基本信息

```xml
<!-- pom.xml:8-19 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.5</version>
    <relativePath/>         <!-- 从 Maven 仓库查找父 POM，不在本地 -->
</parent>

<groupId>com.secondbrain</groupId>    <!-- 组织名，反向域名风格 -->
<artifactId>ai-second-brain</artifactId>  <!-- 项目名，jar 包的名字 -->
<version>1.0.0</version>              <!-- 版本号 -->
<name>AI-SecondBrain</name>
<description>AI对话知识沉淀系统</description>
```

> **为什么要继承 spring-boot-starter-parent？**
> 
> 父 POM 里已经帮你配置好了：
> - Java 版本默认值
> - 常用依赖的版本号（不用自己写 version）
> - 各种插件的默认配置
> - 资源文件过滤规则
> 
> 相当于"装修好的房子"，你直接拎包入住就行。

#### 2.1.2 属性定义

```xml
<!-- pom.xml:21-30 -->
<properties>
    <java.version>17</java.version>           <!-- Java 版本 -->
    <mybatis-plus.version>3.5.3.1</mybatis-plus.version>
    <knife4j.version>4.4.0</knife4j.version>
    <hutool.version>5.8.24</hutool.version>
    <fastjson2.version>2.0.43</fastjson2.version>
    <aliyun-oss.version>3.17.4</aliyun-oss.version>
    <poi.version>5.2.5</poi.version>
    <itextpdf.version>8.0.2</itextpdf.version>
</properties>
```

> **properties 像常量定义**：用 `${mybatis-plus.version}` 引用，改一处全生效。
> 
> 注意：Spring Boot 父 POM 里已经定义了很多依赖的版本（如 spring-boot-starter-web），所以那些依赖不用写 version。

#### 2.1.3 自定义源码目录（重点！）

```xml
<!-- pom.xml:248-255 -->
<build>
    <!-- 指定 Java 源码目录，不是默认的 src/main/java -->
    <sourceDirectory>backend/src/main/java</sourceDirectory>
    <!-- 指定测试源码目录 -->
    <testSourceDirectory>backend/src/test/java</testSourceDirectory>
    <!-- 指定资源文件目录 -->
    <resources>
        <resource>
            <directory>backend/src/main/resources</directory>
        </resource>
    </resources>
    ...
</build>
```

> ⚠️ **这是本项目最容易踩坑的地方**
> 
> 标准 Maven 项目源码在 `src/main/java`，本项目挪到了 `backend/src/main/java`。
> 
> 如果你用 IDE 打开项目，可能会遇到：
> - 找不到类
> - 报"程序包不存在"
> - 源码目录没有被标记为蓝色（IDEA）
> 
> **解决方法**：在 IDE 里重新导入 Maven 项目，或者直接用命令行 `mvn compile`（命令行一定是对的）。

#### 2.1.4 构建插件

```xml
<!-- pom.xml:256-277 -->
<plugins>
    <!-- Spring Boot 打包插件：把依赖打进 jar，变成可执行 jar -->
    <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
            <excludes>
                <!-- 打包时排除 Lombok（编译时用，运行时不需要） -->
                <exclude>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                </exclude>
            </excludes>
        </configuration>
    </plugin>
    <!-- 编译器插件 -->
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
            <!-- 源代码兼容版本 -->
            <source>15</source>
            <!-- 目标字节码版本 -->
            <target>15</target>
        </configuration>
    </plugin>
</plugins>
```

> ⚠️ **注意一个不一致**：
> - `java.version` 设的是 17
> - 但 `maven-compiler-plugin` 的 source/target 设的是 15
> 
> 这意味着代码用 Java 17 编译，但编译出来的字节码兼容 Java 15。一般建议保持一致，这里可能是历史遗留问题。

### 2.2 package.json 注释

文件路径：`d:\AI-SecondBrain\frontend\package.json`

```json
{
  "name": "ai-second-brain-frontend",    // 项目名
  "version": "1.0.0",                    // 版本号
  "type": "module",                      // 使用 ES 模块（import/export），不是 CommonJS
  "scripts": {                           // 可执行的脚本命令
    "dev": "vite",                       // 开发模式：npm run dev
    "build": "vite build",               // 生产构建：npm run build
    "preview": "vite preview"            // 预览构建结果：npm run preview
  },
  "dependencies": {                      // 生产环境依赖
    "@element-plus/icons-vue": "^2.3.1", // Element Plus 图标库
    "axios": "^1.6.2",                   // HTTP 请求库
    "dompurify": "^3.3.3",               // XSS 防护，净化 HTML
    "echarts": "^5.6.0",                 // 图表库
    "element-plus": "^2.5.0",            // UI 组件库
    "marked": "^17.0.4",                 // Markdown 渲染
    "pinia": "^2.1.7",                   // 状态管理（Vuex 的替代品）
    "vue": "^3.4.0",                     // Vue 3 框架
    "vue-router": "^4.2.5"               // 路由
  },
  "devDependencies": {                   // 开发环境依赖
    "@vitejs/plugin-vue": "^5.0.0",      // Vite 的 Vue 3 插件
    "vite": "^5.0.0"                     // 构建工具本身
  }
}
```

> **版本号里的 `^` 是什么意思？**
> 
> `^1.6.2` 表示：>= 1.6.2 且 < 2.0.0
> 
> 即兼容同一主版本号下的所有更新。如果想锁死版本，去掉 `^` 直接写 `1.6.2`。

### 2.3 vite.config.js 注释

文件路径：`d:\AI-SecondBrain\frontend\vite.config.js`

```javascript
// 从 vite 导入配置定义函数（有类型提示）
import { defineConfig } from 'vite'
// 导入 Vue 3 插件
import vue from '@vitejs/plugin-vue'
// 导入 Node.js 的 path 模块，用于处理路径
import { resolve } from 'path'

// 导出 Vite 配置
export default defineConfig({
  plugins: [vue()],    // 启用 Vue 插件，支持 .vue 单文件组件
  
  resolve: {
    alias: {
      // 路径别名：@ 指向 src 目录
      // 这样 import xxx from '@/utils/request' 就不用写相对路径了
      '@': resolve(__dirname, 'src')
    }
  },
  
  server: {
    port: 3000,    // 开发服务器端口
    proxy: {
      // 代理配置：以 /api 开头的请求转发到后端
      '/api': {
        target: 'http://localhost:8080',  // 后端地址
        changeOrigin: true,               // 改变请求头的 origin，避免跨域
        timeout: 300000,                  // 超时时间 5 分钟（AI 对话可能很慢）
        // 自定义代理日志，方便调试
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('代理请求:', req.method, req.url);
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('代理响应:', proxyRes.statusCode, req.url);
          });
        }
      }
    }
  }
})
```

> **为什么需要代理？**
> 
> 前端开发服务器跑在 3000 端口，后端跑在 8080 端口。
> 浏览器直接请求 8080 会有**跨域问题**（CORS）。
> 
> 代理的作用：
> ```
> 浏览器 → localhost:3000/api/xxx  →  Vite 代理  →  localhost:8080/api/xxx
> ```
> 
> 对浏览器来说，请求的是同一个域名，就没有跨域问题了。

> ⚠️ **注意**：文档里说端口是 5173，但实际配置是 **3000**，以实际配置为准。

---

## 3. 五端构建对比

| 对比维度 | 后端（Maven） | Web 前端（Vite） | 移动端（uni-app） | 浏览器扩展 | DeerFlow（Python） |
|---------|-------------|-----------------|------------------|----------|------------------|
| 配置文件 | pom.xml | package.json + vite.config.js | package.json + vite.config.js + pages.json | manifest.json | requirements.txt |
| 配置语言 | XML | JSON/JS | JSON/JS | JSON | pip 文本 |
| 构建产物 | .jar（fat jar） | dist/ 静态资源 | dist/build/{h5,mp-weixin,app}/ | 目录 + zip | Docker 镜像 |
| 运行方式 | `java -jar` / `mvn spring-boot:run` | Nginx 托管 / `npm run dev` | 微信开发者工具 / HBuilderX | Chrome 加载已解压 | `python app.py` / Docker |
| 依赖管理 | Maven 中央仓库 | npm registry | npm registry | 无（纯 JS） | PyPI |
| 入口 | AiSecondBrainApplication.java | main.js | main.js（createSSRApp） | manifest.json → background.js | app.py |
| 端口 | 8080 | 5173（dev） | 取决于端 | 无 | 8000 |
| 构建命令 | `mvn package -DskipTests` | `npm run build` | `npm run build:mp-weixin` | `build.sh`（打 zip） | `docker build` |
| 热更新 | 无（需 JRebel） | HMR 毫秒级 | 保存即重编译 | 需手动"重新加载扩展" | 需重启进程 |

---

## 4. 本地开发完整步骤（从 0 到跑起来）

### 前置条件

先确保你电脑上装了这些：

- ✅ JDK 17 或更高
- ✅ Maven 3.6+
- ✅ Node.js 18+
- ✅ MySQL 8.0
- ✅ Redis 7.x
- ✅ （可选）Kafka + Elasticsearch（缺了也能跑部分功能）

### 第 1 步：克隆项目

```bash
git clone <仓库地址>
cd AI-SecondBrain
```

### 第 2 步：配置数据库

1. 创建数据库：

```sql
CREATE DATABASE second_brain 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

2. 导入初始化脚本：

```bash
mysql -u root -p second_brain < sql/complete_database_schema_verified.sql
```

### 第 3 步：启动 Redis

确保 Redis 服务在运行，默认端口 6379。

### 第 4 步：配置后端

编辑 `backend/src/main/resources/application.yml`，修改数据库和 Redis 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/second_brain?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
  data:
    redis:
      host: localhost
      port: 6379
      password: 你的密码（如果设了的话）
```

### 第 5 步：启动后端

在**项目根目录**执行（因为 pom.xml 在根目录）：

```bash
# 编译
mvn compile

# 运行（开发模式）
mvn spring-boot:run
```

看到 `Started AiSecondBrainApplication in X.XXX seconds` 就说明启动成功了。

验证一下：浏览器打开 http://localhost:8080/api/health ，应该返回 OK。

### 第 6 步：安装前端依赖

```bash
cd frontend
npm install
```

> 如果 npm install 很慢，换淘宝镜像：
> ```bash
> npm config set registry https://registry.npmmirror.com
> ```

### 第 7 步：启动前端

```bash
# 在 frontend 目录下
npm run dev
```

看到 `Local: http://localhost:3000/` 就说明启动成功了。

### 第 8 步：打开浏览器

访问 http://localhost:3000

默认账号：admin / admin123

### 完整启动流程图

```
┌─────────────────────────────────────────────────────┐
│  启动顺序                                           │
│                                                     │
│  1. MySQL ──────────┐                               │
│  2. Redis ──────────┤                               │
│  3. (可选) Kafka ───┤                               │
│  4. (可选) ES ──────┤                               │
│  5. DeerFlow ───────┤  (Python :8000)               │
│                     ▼                               │
│  6. 后端 Spring Boot (端口 8080)                    │
│                     │                               │
│         ┌───────────┼───────────┐                   │
│         ▼           ▼           ▼                   │
│  Web 前端 Vite   移动端       浏览器扩展             │
│  (:3000 或 5173)  (微信开发者工具)  (Chrome 加载)     │
│         │           │           │                   │
│         ▼           ▼           ▼                   │
│  浏览器 / 微信 / Chrome 访问                        │
└─────────────────────────────────────────────────────┘
```

### 一键启动（Docker 方式）

如果你不想装这么多环境，可以直接用 Docker：

```bash
# Windows
start.bat

# Linux/macOS
./start.sh
```

Docker 方式会自动启动 **7 个服务**：MySQL、Redis、Zookeeper、Kafka、Elasticsearch、DeerFlow、后端 Spring Boot、前端构建器、Nginx（见 `docker-compose.yml`）。

---

### 移动端开发（微信小程序）

前置：安装**微信开发者工具**（stable 版）。

```bash
cd mobile
npm install
npm run dev:mp-weixin
# 产物目录：mobile/dist/dev/mp-weixin
# 打开微信开发者工具 → 导入项目 → 选 mobile/dist/dev/mp-weixin 目录
```

> 小程序的 AppID 在 `mobile/src/manifest.json` 里配置，没有 AppID 也能用"测试号"跑，但无法真机预览。

### 浏览器扩展开发

```
Chrome → 地址栏输入 chrome://extensions → 打开"开发者模式"
→ 点击"加载已解压的扩展程序" → 选 d:\AI-SecondBrain\extension 目录
→ 扩展图标会出现在工具栏
```

改了 content.js / background.js 后，需要在扩展管理页点**刷新按钮**（或 Ctrl+R）重新加载。

### DeerFlow 本地开发

```bash
cd deerflow
pip install -r requirements.txt
# 复制 .env.example 为 .env，填入 QWEN_API_KEY
python app.py
# 验证：curl http://localhost:8000/health → {"status":"healthy"}
```

启动后，后端通过 `application.yml` 的 `deerflow.api-url` 或环境变量 `DEERFLOW_API_URL` 找到它。

---

## 5. 常用命令速查表

### 5.1 后端 Maven 命令（在项目根目录执行）

| 命令 | 作用 | 常用场景 |
|------|------|---------|
| `mvn compile` | 编译源码 | 检查代码有没有语法错误 |
| `mvn test` | 运行测试 | 跑单元测试 |
| `mvn package -DskipTests` | 打包（跳过测试） | 生成可执行 jar |
| `mvn clean package -DskipTests` | 清理后重新打包 | 发布前构建 |
| `mvn spring-boot:run` | 启动应用 | 开发调试 |
| `mvn clean` | 清理 target 目录 | 重新构建前清场 |
| `mvn dependency:tree` | 查看依赖树 | 排查依赖冲突 |
| `mvn install` | 安装到本地仓库 | 多模块项目时用 |

> **-DskipTests vs -Dmaven.test.skip=true**
> - `-DskipTests`：不运行测试，但编译测试代码
> - `-Dmaven.test.skip=true`：连测试代码都不编译
> 
> 一般用 `-DskipTests` 就够了。

### 5.2 前端 npm 命令（在 frontend 目录执行）

| 命令 | 作用 | 常用场景 |
|------|------|---------|
| `npm install` | 安装所有依赖 | 第一次拉代码后 |
| `npm run dev` | 启动开发服务器 | 日常开发 |
| `npm run build` | 构建生产版本 | 部署前 |
| `npm run preview` | 预览构建结果 | 验证构建产物 |
| `npm update` | 更新所有依赖 | 定期升级 |

### 5.3 移动端 npm 命令（在 mobile 目录执行）

| 命令 | 作用 | 产物 |
|------|------|------|
| `npm run dev:h5` | H5 开发模式 | `mobile/dist/dev/h5` |
| `npm run dev:mp-weixin` | 微信小程序开发 | `mobile/dist/dev/mp-weixin` |
| `npm run dev:app` | App 开发（需 HBuilderX） | - |
| `npm run build:h5` | H5 生产构建 | `mobile/dist/build/h5` |
| `npm run build:mp-weixin` | 微信小程序生产构建 | `mobile/dist/build/mp-weixin` |
| `npm run build:app` | App 打包（需 HBuilderX） | - |

### 5.4 DeerFlow 命令（在 deerflow 目录执行）

| 命令 | 作用 |
|------|------|
| `pip install -r requirements.txt` | 安装依赖 |
| `python app.py` | 本地启动（监听 8000） |
| `curl http://localhost:8000/health` | 健康检查 |
| `docker build -t deerflow .` | 构建镜像 |

### 5.5 浏览器扩展

无构建命令。改完代码在 `chrome://extensions` 点刷新按钮即可。打包用 `extension/build.sh` 产出 zip。

### 5.6 Docker Compose 命令（在项目根目录执行）

| 命令 | 作用 |
|------|------|
| `docker-compose up -d` | 后台启动所有服务（MySQL/Redis/ZK/Kafka/ES/DeerFlow/Backend/Frontend/Nginx） |
| `docker-compose down` | 停止并删除所有容器 |
| `docker-compose ps` | 查看服务状态 |
| `docker-compose logs -f` | 实时查看所有日志 |
| `docker-compose logs -f backend` | 查看后端日志 |
| `docker-compose logs -f deerflow` | 查看 DeerFlow 日志 |
| `docker-compose logs -f mysql` | 查看 MySQL 日志 |
| `docker-compose restart backend` | 重启后端服务 |
| `docker-compose build backend` | 重新构建后端镜像 |
| `docker-compose up -d deerflow` | 只启动 DeerFlow（按需） |

---

## 6. 常见错误与修复

### 6.1 后端相关

| 报错信息 | 原因 | 修复方法 |
|---------|------|---------|
| `程序包 com.secondbrain.mapper 不存在` | pom.xml 在根目录，但源码在 backend/，IDE 没识别到 sourceDirectory | 在 IDE 中重新导入 Maven 项目，或用命令行 `mvn compile` |
| `找不到符号: 变量 log` / `找不到符号: 方法 getter/setter` | Lombok 没生效，IDE 没识别注解 | 1. IDE 安装 Lombok 插件<br>2. 开启注解处理（Annotation Processing）<br>3. 确保 pom.xml 有 lombok 依赖 |
| `Table 'second_brain.knowledge_node' doesn't exist` | 数据库没初始化，表不存在 | 执行 `sql/complete_database_schema_verified.sql` 建表 |
| `Connection refused: connect` 连不上 Redis | Redis 没启动，或地址/端口/密码错了 | 1. 启动 Redis 服务<br>2. 检查 application.yml 里的 Redis 配置 |
| `Communications link failure` 连不上 MySQL | MySQL 没启动，或 URL/用户名/密码错了 | 1. 启动 MySQL<br>2. 检查 application.yml 里的 datasource 配置 |
| `BeanCreationException: Error creating bean with name 'xxx'` | Spring 容器创建 Bean 失败 | 看 Caused by 后面的具体异常，通常是依赖缺失或配置错误 |
| `java.lang.OutOfMemoryError: Java heap space` | 内存不够 | 调大 JVM 内存：`mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m"` |
| mvn download 很慢，卡死 | Maven 中央仓库在国外 | 配置阿里云镜像，见下面补充 |

**Maven 阿里云镜像配置**：

在 `~/.m2/settings.xml` 里加：

```xml
<mirrors>
    <mirror>
        <id>aliyunmaven</id>
        <mirrorOf>*</mirrorOf>
        <name>阿里云公共仓库</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

### 6.2 前端相关

| 报错信息 | 原因 | 修复方法 |
|---------|------|---------|
| `404 Not Found` 访问 `/api/xxx` 报错 | 后端没启动，或代理配置不对 | 1. 确保后端在 8080 端口运行<br>2. 检查 vite.config.js 的 proxy 配置<br>3. 看控制台的代理日志 |
| `Cannot find module 'xxx'` | 依赖没装全 | 重新执行 `npm install` |
| `npm install` 很慢 / 超时 | 网络问题，npm 官方源在国外 | 换淘宝镜像：`npm config set registry https://registry.npmmirror.com` |
| `SyntaxError: Unexpected token` | 代码语法错误 | 看报错行号，检查语法 |
| 页面空白，控制台有 `Uncaught Error` | JS 运行时错误 | 打开浏览器开发者工具，看 Console 面板的报错 |
| 修改代码后页面不更新 | 热更新失效 | 刷新页面，或重启 `npm run dev` |
| `Port 3000 is already in use` | 3000 端口被占了 | 1. 关掉占用端口的程序<br>2. 或修改 vite.config.js 的 port 为其他值 |

### 6.3 移动端（uni-app）相关

| 报错信息 | 原因 | 修复方法 |
|---------|------|---------|
| `not found module '@dcloudio/uni-app'` | 依赖没装或版本不对 | `cd mobile && npm install`，确保用的是项目锁定的版本 |
| 微信开发者工具打开后白屏 | main.js 用了 `createApp` 而不是 `createSSRApp` | 改回 `createSSRApp`（见 03 特性 11） |
| `navigateTo:fail page not found` | pages.json 没注册该页面 | 在 `mobile/src/pages.json` 的 pages 数组里加上 |
| `uni.request` 跨域报错（H5 端） | H5 端浏览器 CORS | 开发环境在 `vite.config.js` 配 proxy；生产走 Nginx 反代 |
| 小程序端 `storage` 读不到 token | 用了 `localStorage` 而不是 `uni.getStorageSync` | 全局替换为 `uni.setStorageSync` / `uni.getStorageSync` |
| TabBar 图标不显示 | pages.json 里 iconPath 路径错 | 检查图标文件是否存在、路径是否相对 pages.json |

### 6.4 浏览器扩展相关

| 报错信息 | 原因 | 修复方法 |
|---------|------|---------|
| 扩展加载后点图标没反应 | popup.html 里的 JS 路径错 | 检查 manifest.json 的 `action.default_popup` 路径 |
| content.js 没注入到目标页面 | manifest.json 的 `matches` 不匹配当前域名 | 在 `content_scripts[0].matches` 加上目标域名 |
| background.js 发请求报 CORS | host_permissions 没加后端域名 | 在 manifest.json 的 `host_permissions` 加 `http://localhost:8080/*` |
| `chrome is not defined` | 代码跑在了 content.js 里误用了 chrome.* API 的同步版本 | content.js 里用 `chrome.runtime.sendMessage` 异步发消息给 background |
| 改了代码扩展不生效 | 没重新加载扩展 | chrome://extensions → 点扩展卡片的刷新按钮 |

### 6.5 DeerFlow 相关

| 报错信息 | 原因 | 修复方法 |
|---------|------|---------|
| `ModuleNotFoundError: No module named 'flask'` | 依赖没装 | `pip install -r requirements.txt` |
| `/health` 返回 500 或连不上 | 8000 端口被占或进程没起来 | `netstat -ano | findstr 8000` 查占用；`python app.py` 看启动日志 |
| 调用通义千问超时 | QWEN_API_KEY 没配或网络不通 | 检查 `.env` 里的 `QWEN_API_KEY`；确认能访问 `dashscope.aliyuncs.com` |
| Java 后端调 DeerFlow 报 Connection refused | DEERFLOW_API_URL 配错或 DeerFlow 没启动 | 检查 `application.yml` 的 `deerflow.api-url`，确保指向运行中的 DeerFlow |
| 返回内容为 null | 通义千问接口报错 | 看 DeerFlow 控制台日志，通常是 API Key 无效或 model 名错 |

### 6.6 Docker 相关

| 报错信息 | 原因 | 修复方法 |
|---------|------|---------|
| `ERROR: ... .env: no such file or directory` | 缺少 .env 文件 | 复制 `.env.example` 为 `.env`，并配置必填项 |
| `Container unhealthy` 服务启动失败 | 健康检查不通过 | `docker-compose logs 服务名` 看具体日志 |
| `bind: address already in use` 端口被占用 | 端口冲突 | 修改 .env 里的端口配置，或关掉占用端口的程序 |
| 构建镜像时网络超时 | Docker Hub 访问慢 | 配置 Docker 镜像加速器 |

---

## 7. 构建速查卡

打印出来贴工位上，随时看：

```
╔══════════════════════════════════════════════════════════════╗
║           AI-SecondBrain 构建速查卡                           ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  后端 (项目根目录)          前端 (frontend 目录)              ║
║  ─────────────────          ───────────────────              ║
║  编译:  mvn compile         安装依赖: npm install            ║
║  运行:  mvn spring-boot:run 开发:   npm run dev              ║
║  打包:  mvn package         构建:   npm run build            ║
║         -DskipTests         预览:   npm run preview          ║
║  清理:  mvn clean                                          ║
║                                                              ║
║  访问地址                                                    ║
║  ────────                                                    ║
║  前端界面:     http://localhost:3000                         ║
║  后端 API:     http://localhost:8080/api                     ║
║  API 文档:     http://localhost:8080/api/doc.html            ║
║  健康检查:     http://localhost:8080/api/health              ║
║                                                              ║
║  默认账号: admin / admin123                                  ║
║                                                              ║
║  Docker 一键启动                                             ║
║  ───────────────                                             ║
║  Windows: start.bat                                          ║
║  Linux:   ./start.sh                                         ║
║  看日志:  docker-compose logs -f                             ║
║  停止:    docker-compose down                                ║
║                                                              ║
║  ⚠️  注意: pom.xml 在根目录，源码在 backend/                  ║
║     IDE 报找不到类就重新导入 Maven 项目                       ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```
