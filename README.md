# AI-SecondBrain 🧠

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4-green.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**智能第二大脑系统** - 基于 AI 大模型的知识管理平台，帮助您高效采集、整理、复习 AI 对话中的宝贵知识，通过科学记忆法将短期记忆转化为长期记忆。

---

## 📋 目录

- [✨ 核心特性](#-核心特性)
- [🚀 快速开始](#-快速开始)
- [🏗️ 技术架构](#-技术架构)
- [📁 项目结构](#-项目结构)
- [📖 新人文档](#-新人文档)
- [🔧 配置指南](#-配置指南)
- [🧪 测试](#-测试)
- [🤝 贡献指南](#-贡献指南)
- [📄 许可证](#-许可证)

---

## ✨ 核心特性

### 🎯 智能采集
- **多平台支持**：一键采集 ChatGPT、DeepSeek、Kimi、通义千问等主流 AI 平台对话
- **浏览器插件**：Chrome/Edge 插件，无需切换窗口即可保存对话
- **自动分类**：AI 智能识别对话主题，自动添加标签

### 🧠 知识管理
- **结构化存储**：将散乱的对话转换为结构化知识点
- **知识图谱**：可视化展示知识点之间的关联关系
- **语义搜索**：基于 Elasticsearch 的智能搜索，支持关键词高亮

### 📚 科学复习
- **艾宾浩斯记忆曲线**：智能规划复习时间，对抗遗忘
- **自动出题**：AI 根据知识点自动生成复习题目
- **进度追踪**：实时统计学习进度和掌握程度

### 🤖 AI 增强
- **RAG 知识问答**：基于检索增强生成的智能问答系统
- **学习报告**：AI 深度分析学习数据，生成个性化报告
- **智能推荐**：根据学习历史推荐相关知识

### 👥 协作与分享
- **工作区管理**：支持多工作区、多角色权限控制（RBAC）
- **知识广场**：分享、点赞、评论知识点
- **游戏化激励**：积分、成就、排行榜系统

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | 后端运行环境 |
| Maven | 3.8+ | 后端构建工具 |
| Node.js | 18+ | 前端运行环境 |
| MySQL | 8.0+ | 关系数据库 |
| Redis | 7+ | 缓存数据库 |

### 步骤 1：克隆项目

```bash
git clone https://github.com/mo-spe/AI-SecondBrain.git
cd AI-SecondBrain
```

### 步骤 2：初始化数据库

**方式一：**

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE secondbrain CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 执行初始化脚本
USE secondbrain;
source second_brain.sql;
```

**方式二：**

```java
# 本地测试时可以连接我的本机数据库
  # IP：10.65.59.104
  #端口：3306
  #账号：dev
  #密码：123456
```

### 步骤 3：配置后端

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/second_brain?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

# AI API 配置（暂时不需要，现在我们要开发的是偏协作社交的功能，暂时不关注这个）
ai:
  qwen:
    api-key: your_qwen_api_key
    base-url: https://dashscope.aliyuncs.com/compatible-mode
```

### 步骤 4：启动后端

```java
//编译
mvn clean install -DskipTests
//运行
三角键启动
```

### 步骤 5：启动前端

```bash
cd frontend
npm install
npm run dev
```

前端访问地址：http://localhost:5173

### 步骤 6：登录系统

```
方式1：连接我的
用户名：newuser
密码：123456
方式二2自己建表
自己注册账号

```

---

## 🏗️ 技术架构

### 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    客户端层                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  Web 应用    │  │ 浏览器插件  │  │   移动端     │     │
│  │  (Vue 3)    │  │ (Extension) │  │  (开发中)    │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                    应用层                                │
│  ┌─────────────────────┐  ┌─────────────────────┐      │
│  │  Spring Boot 后端   │  │   DeerFlow AI 服务（暂不关注）|
│  │   (Java 17)         │  │   (Python FastAPI)  │      │
│  └─────────────────────┘  └─────────────────────┘      │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                    数据层                                │
│  ┌──────────┐ ┌────────┐ ┌──────────┐ ┌────────────┐   │
│  │  MySQL   │ │ Redis  │ │ Kafka    │ │Elasticsearch│  │
│  │  8.0     │ │  7     │ │  3.6     │ │    8.11    │  │
│  └──────────┘ └────────┘ └──────────┘ └────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 技术栈详情

#### 后端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.1.5 | 应用框架 |
| MyBatis-Plus | 3.5.3 | ORM 框架 |
| Spring Security | 3.1 | 安全框架 |
| Spring Kafka | 3.1 | 消息队列 |
| Spring Data Elasticsearch | 5.1 | 搜索引擎 |
| Spring Data Redis | 3.1 | 缓存 |
| Quartz | 2.3 | 定时任务 |
| JWT (jjwt) | 0.11.5 | 认证令牌 |
| Knife4j | 4.4.0 | API 文档 |
| Hutool | 5.8.24 | 工具库 |

#### 前端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue.js | 3.4 | 前端框架 |
| Vite | 5.0 | 构建工具 |
| Element Plus | 2.5 | UI 组件库 |
| Pinia | 2.1 | 状态管理 |
| Vue Router | 4.2 | 路由管理 |
| Axios | 1.6 | HTTP 客户端 |
| ECharts | 5.6 | 图表库 |
| Marked | 17 | Markdown 渲染 |

#### AI 服务

| 服务 | 提供商 | 用途 |
|------|--------|------|
| Qwen (目前阶段) | 阿里云 | 知识提取、报告生成 |
| Qwen（目前阶段） | 阿里云 | 题目生成、RAG 问答 |
| text-embedding-v2 | 阿里云 | 向量嵌入生成 |

---

## 📁 项目结构

```
AI-SecondBrain/
├── 📁 backend/                   # 后端源代码
│   ├── src/main/java/com/secondbrain/
│   │   ├── controller/           # REST API 控制器 (25+)
│   │   ├── service/              # 业务逻辑层 (接口 + impl/)
│   │   ├── mapper/               # 数据访问层 (MyBatis-Plus)
│   │   ├── entity/               # 数据库实体 (20+)
│   │   ├── dto/                  # 请求/响应传输对象
│   │   ├── vo/                   # 视图对象
│   │   ├── config/               # 配置类 (Async/Kafka/WebSocket等)
│   │   ├── interceptor/          # 拦截器 (JWT/Workspace)
│   │   ├── exception/            # 异常处理
│   │   ├── kafka/                # Kafka 生产者/消费者
│   │   ├── elasticsearch/        # ES 文档实体
│   │   ├── task/                 # 定时任务
│   │   └── util/                 # 工具类
│   └── src/main/resources/
│       ├── sql/                  # SQL 脚本
│       └── application.yml       # 配置文件
│
├── 📁 frontend/                  # 前端源代码
│   ├── src/
│   │   ├── api/                  # API 请求封装
│   │   ├── views/                # 页面组件 (20+)
│   │   ├── components/           # 公共组件
│   │   ├── router/               # 路由配置
│   │   ├── stores/               # Pinia 状态管理
│   │   ├── styles/               # 全局样式
│   │   ├── utils/                # 工具 (request/websocket)
│   │   ├── layout/               # 布局组件
│   │   ├── App.vue               # 根组件
│   │   └── main.js               # 入口文件
│   └── package.json
│
├── 📁 docs/                      # 项目文档（新人必读）
│   ├── 01_framework_architecture.md     # 架构指南
│   ├── 02_framework_philosophy.md       # 设计思想
│   ├── 03_lang_concepts.md              # 语言特性
│   ├── 04_code_walkthrough.md           # 代码导读
│   ├── 05_runtime_model.md             # 运行时模型
│   ├── 06_build_guide.md               # 构建指南
│   ├── 07_integration_guide.md         # 对接指南
│   ├── 08_debug_guide.md               # 调试指南
│   └── 09_design_conventions.md        # 设计规范
│
├── 📁 extension/                 # 浏览器插件
├── 📁 deerflow/                  # AI 服务 (Python)
├── 📁 scripts/                   # 辅助脚本
│
├── pom.xml                       # Maven 配置
├── complete_database_schema_verified.sql  # 数据库初始化
├── start.bat                     # Windows 启动脚本
├── start.sh                      # Linux/macOS 启动脚本
├── README.md                     # 项目主文档
└── LICENSE                       # MIT 开源协议
```

---

## 📖 新人文档

项目提供了完整的新人文档体系，建议按以下顺序阅读：

| 顺序 | 文档 | 内容概要 |
|------|------|---------|
| 1 | [01_framework_architecture.md](docs/01_framework_architecture.md) | 整体架构 + 目录结构 + 核心模块 |
| 2 | [02_framework_philosophy.md](docs/02_framework_philosophy.md) | 8 大设计思想（分层、JWT、工作区隔离等） |
| 3 | [03_lang_concepts.md](docs/03_lang_concepts.md) | Java/Vue 语言特性 + 命名规范 |
| 4 | [04_code_walkthrough.md](docs/04_code_walkthrough.md) | 登录→获取知识点列表完整代码走读 |
| 5 | [05_runtime_model.md](docs/05_runtime_model.md) | 线程全景图 + 启动顺序 + 线程安全 |
| 6 | [06_build_guide.md](docs/06_build_guide.md) | Maven + Vite 构建指南 + 常见错误 |
| 7 | [07_integration_guide.md](docs/07_integration_guide.md) | 新模块接入十步法（完整示例） |
| 8 | [08_debug_guide.md](docs/08_debug_guide.md) | 日志系统 + 调试技巧速查 |
| 9 | [09_design_conventions.md](docs/09_design_conventions.md) | 命名规范 + 拆分原则 + 反模式 |

---

## 🔧 配置指南

### AI API 配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
ai:
  provider: qwen  # 默认 AI 提供商
  qwen:
    api-key: ${QWEN_API_KEY:}
    base-url: https://dashscope.aliyuncs.com/compatible-mode
    model: qwen-plus
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:}
    base-url: https://api.deepseek.com
    model: deepseek-chat
```

### 可选服务配置

```yaml
spring:
  kafka:
    enabled: ${KAFKA_ENABLED:true}
    bootstrap-servers: localhost:9092

  elasticsearch:
    enabled: ${ES_ENABLED:true}
    uris: http://localhost:9200
    username: elastic
    password: elastic123
```

### 自定义端口

```yaml
server:
  port: 8080
```

---

## 🧪 测试

### 运行测试

```bash
# 后端测试
mvn test

# 前端测试
cd frontend
npm test
```

### 健康检查

```bash
curl http://localhost:8080/api/health
```

---

## 🤝 贡献指南

### 开发流程

1. 拉取代码，切到dev分支，更新项目
2. 开发
3. 在推送代码到远程前测试编译运行能否通过
4. 通过的话可以推送，否则禁止推送

### 代码规范

- 后端：遵循 Java 编码规范，使用 Lombok 的 `@Getter/@Setter`（禁用 `@Data`）
- 前端：Vue 3 Composition API + `<script setup>`
- 提交信息：遵循 Conventional Commits 规范

### 新功能开发

参考 [07_integration_guide.md](docs/07_integration_guide.md) 中的十步法：
1. 理解三层架构
2. 数据库表设计
3. 创建 Entity
4. 创建 Mapper
5. 定义 DTO/VO
6. 创建 Service 接口 + 实现
7. 创建 Controller
8. 前端 API 封装
9. 前端页面开发
10. 注册路由

---

## 📄 许可证

本项目采用 [MIT](LICENSE) 开源协议。

### 使用的开源框架许可

| 框架 | 协议 |
|------|------|
| Spring Boot | Apache 2.0 |
| Vue.js | MIT |
| Element Plus | MIT |
| MyBatis-Plus | Apache 2.0 |
| Elasticsearch | Apache 2.0 |
| Redis | BSD |
| Kafka | Apache 2.0 |

---

## 📞 联系方式

- **项目主页**：https://github.com/mo-spe/AI-SecondBrain
- **问题反馈**：https://github.com/mo-spe/AI-SecondBrain/issues
- **邮箱**：walliamharric@gmail.com

---

## 🎯 路线图

### V2.0（当前版本）

- ✅ 多平台对话采集
- ✅ AI 知识提取
- ✅ 智能复习系统（艾宾浩斯）
- ✅ RAG 知识问答
- ✅ 学习报告生成
- ✅ 工作区管理（RBAC）
- ✅ 知识广场
- ✅ 游戏化激励（积分/成就/排行榜）

### V3.0（计划中）

- 🚧 移动端 App
- 🚧 多人实时协作
- 🚧 知识分享社区
- 🚧 更多 AI 模型支持
- 🚧 离线模式

---

**⭐ 如果这个项目对您有帮助，请给一个 Star 支持一下！**
