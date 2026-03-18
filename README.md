# AI-SecondBrain 🧠

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4-green.svg)](https://vuejs.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**智能第二大脑系统** - 基于 AI 大模型的知识管理平台，帮助您高效采集、整理、复习 AI 对话中的宝贵知识。

![AI-SecondBrain Banner](docs/images/banner.png)

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

---

## 🏗️ 技术架构

### 系统架构图

```
┌─────────────────────────────────────────────────────────┐
│                    客户端层                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  Web 应用    │  │ 浏览器插件  │  │  移动端     │     │
│  │  (Vue 3)    │  │ (Extension) │  │ (响应式)    │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                    网关层                                │
│                  Nginx 反向代理                          │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                    应用层                                │
│  ┌─────────────────────┐  ┌─────────────────────┐      │
│  │  Spring Boot 后端   │  │   DeerFlow AI 服务   │      │
│  │   (Java 21)         │  │   (Python FastAPI)  │      │
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

| 技术          | 版本  | 用途         |
| ------------- | ----- | ------------ |
| Java          | 21    | 主要开发语言 |
| Spring Boot   | 3.1.5 | 应用框架     |
| MyBatis-Plus  | 3.5.3 | ORM 框架     |
| MySQL         | 8.0   | 关系数据库   |
| Redis         | 7     | 缓存         |
| Kafka         | 3.6   | 消息队列     |
| Elasticsearch | 8.11  | 搜索引擎     |
| Quartz        | 2.3   | 任务调度     |

#### 前端技术

| 技术         | 版本 | 用途        |
| ------------ | ---- | ----------- |
| Vue.js       | 3.4  | 前端框架    |
| Vite         | 5.0  | 构建工具    |
| Element Plus | 2.5  | UI 组件库   |
| Pinia        | 2.1  | 状态管理    |
| Vue Router   | 4.2  | 路由管理    |
| Axios        | 1.6  | HTTP 客户端 |
| ECharts      | 5.6  | 图表库      |

#### AI 服务

| 服务              | 提供商   | 用途               |
| ----------------- | -------- | ------------------ |
| Qwen Plus         | 阿里云   | 知识提取、报告生成 |
| DeepSeek          | 深度求索 | 题目生成、RAG 问答 |
| text-embedding-v2 | 阿里云   | 向量嵌入生成       |

---

## 🚀 快速开始

### 环境要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **内存**: 最少 4GB，推荐 8GB+
- **磁盘**: 40GB+ 可用空间
- **操作系统**: Windows / macOS / Linux

### 5 分钟快速部署

#### 步骤 1：克隆项目

```bash
git clone https://github.com/YOUR_USERNAME/AI-SecondBrain.git
cd AI-SecondBrain
```

#### 步骤 2：配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置文件（必填）
# Windows: notepad .env
# macOS/Linux: vim .env
```

**必须配置的项目**：

```bash
# 数据库密码（请修改为强密码）
MYSQL_ROOT_PASSWORD=your_secure_password_here
MYSQL_PASSWORD=your_secure_password_here

# Redis 密码
REDIS_PASSWORD=your_secure_password_here

# AI API 密钥（至少配置一个）
QWEN_API_KEY=your_qwen_api_key_here
# DEEPSEEK_API_KEY=your_deepseek_api_key_here
```

#### 步骤 3：一键启动

```bash
# 启动所有服务
docker-compose up -d

# 查看日志（可选）
docker-compose logs -f

# 检查服务状态
docker-compose ps
```

#### 步骤 4：访问应用

- **前端界面**: http://localhost
- **API 文档**: http://localhost:8080/api/doc.html
- **后端健康检查**: http://localhost:8080/api/health

#### 步骤 5：默认登录

```
用户名：admin
密码：admin123
```

**⚠️ 首次登录后请立即修改密码！**

---

## 📦 项目结构

```
AI-SecondBrain/
├── 📁 src/                      # 后端源代码
│   ├── main/java/
│   │   └── com/secondbrain/
│   │       ├── controller/      # REST API 控制器
│   │       ├── service/         # 业务逻辑层
│   │       ├── mapper/          # 数据访问层
│   │       ├── entity/          # 实体类
│   │       ├── dto/             # 数据传输对象
│   │       ├── config/          # 配置类
│   │       └── util/            # 工具类
│   └── resources/
│       ├── sql/                 # SQL 脚本
│       └── application*.yml     # 配置文件
│
├── 📁 frontend/                 # 前端源代码
│   ├── src/
│   │   ├── api/                 # API 封装
│   │   ├── views/               # 页面组件
│   │   ├── components/          # 公共组件
│   │   ├── router/              # 路由配置
│   │   └── stores/              # 状态管理
│   └── package.json
│
├── 📁 extension/                # 浏览器插件
│   ├── popup/                   # 弹窗界面
│   ├── background.js            # 后台脚本
│   ├── content.js               # 内容脚本
│   └── manifest.json            # 插件配置
│
├── 📁 deerflow/                 # AI 服务
│   ├── app.py                   # FastAPI 应用
│   ├── config.yaml              # 配置文件
│   └── *.py                     # Python 服务
│
├── 📁 nginx/                    # Nginx 配置
├── 📁 redis/                    # Redis 配置
├── 📁 scripts/                  # 辅助脚本
│
├── docker-compose.yml           # Docker 编排配置
├── Dockerfile                   # 后端 Docker 配置
├── frontend/Dockerfile          # 前端 Docker 配置
├── .env.example                 # 环境变量模板
├── .gitignore                   # Git 忽略配置
├── pom.xml                      # Maven 配置
└── README.md                    # 项目说明
```

---

## 🎯 核心功能使用

### 1️⃣ 采集 AI 对话

#### 方法一：浏览器插件（推荐）

1. **安装插件**

   ```bash
   # Chrome/Edge 浏览器
   # 访问 chrome://extensions/
   # 启用"开发者模式"
   # 点击"加载已解压的扩展程序"
   # 选择 extension/ 目录
   ```

2. **使用插件**
   - 访问 ChatGPT/DeepSeek/Kimi 网站
   - 点击浏览器插件图标
   - 点击"采集对话"按钮
   - 对话自动保存到系统

#### 方法二：手动导入

1. 复制 AI 对话内容
2. 在系统中点击"新建对话"
3. 粘贴内容并保存

### 2️⃣ 管理知识点

1. **查看知识列表**
   - 访问"知识管理"页面
   - 浏览所有知识点

2. **编辑知识点**
   - 点击知识点标题
   - 修改内容、标签、重要程度
   - 保存更改

3. **关联知识**
   - 在知识点详情页
   - 点击"添加关联"
   - 选择关联的知识点的类型

### 3️⃣ 智能复习

1. **访问复习中心**
   - 点击"复习"菜单
   - 查看今日待复习卡片

2. **开始复习**
   - 点击"开始复习"
   - 回答 AI 生成的题目
   - 查看正确答案和解析

3. **查看进度**
   - 复习完成率
   - 掌握程度统计
   - 连续学习天数

### 4️⃣ RAG 知识问答

1. **打开问答界面**
   - 点击"AI 问答"菜单

2. **提问**
   - 输入问题（如："什么是艾宾浩斯遗忘曲线？"）
   - 点击"发送"

3. **查看答案**
   - AI 基于您的知识库生成答案
   - 显示参考知识点来源

---

## 🔧 高级配置

### 自定义端口

编辑 `.env` 文件：

```bash
# 修改端口
BACKEND_PORT=8081
FRONTEND_PORT=3000
MYSQL_PORT=3307
```

### 配置多个 AI 提供商

编辑 `.env` 文件：

```bash
# 主提供商
AI_PROVIDER=qwen
QWEN_API_KEY=your_key
QWEN_MODEL=qwen-plus

# 备用提供商
DEEPSEEK_API_KEY=your_key
DEEPSEEK_MODEL=deepseek-chat

# OpenAI（可选）
OPENAI_API_KEY=your_key
OPENAI_MODEL=gpt-4
```

### 启用 HTTPS

1. **准备证书**

   ```bash
   # 将证书文件放到 ssl/ 目录
   ssl/cert.pem
   ssl/key.pem
   ```

2. **修改配置**

   ```bash
   # .env 文件
   ENABLE_HTTPS=true
   DOMAIN_NAME=yourdomain.com
   ```

3. **重启服务**
   ```bash
   docker-compose down
   docker-compose up -d
   ```

---

## 📊 性能优化建议

### 内存优化

对于内存有限的服务器，编辑 `.env`：

```bash
# 降低 JVM 内存
JAVA_OPTS=-Xms256m -Xmx512m

# 降低 ES 内存
ES_JAVA_OPTS=-Xms256m -Xmx256m
```

### 禁用不需要的服务

编辑 `docker-compose.yml`，注释掉不需要的服务：

```yaml
# 如不需要 Kafka，可以注释掉
# kafka:
#   image: bitnami/kafka:3.6
#   ...
```

---

## 🧪 测试

### 运行后端测试

```bash
cd src
mvn test
```

### 运行前端测试

```bash
cd frontend
npm test
```

### 健康检查

```bash
# 检查所有服务
docker-compose ps

# 查看后端日志
docker-compose logs backend

# 查看前端日志
docker-compose logs frontend

# 检查数据库连接
docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"
```

---

## 📖 相关文档

- [贡献指南](CONTRIBUTING.md) - 如何参与项目开发
- [LICENSE](LICENSE) - MIT 开源协议

---

## 🤝 贡献指南

### 开发环境搭建

1. **克隆项目**

   ```bash
   git clone https://github.com/YOUR_USERNAME/AI-SecondBrain.git
   cd AI-SecondBrain
   ```

2. **启动开发环境**

   ```bash
   docker-compose up -d mysql redis
   ```

3. **配置本地环境**

   ```bash
   cp .env.example .env.dev
   # 编辑 .env.dev 配置开发环境
   ```

4. **运行后端**

   ```bash
   cd src
   mvn spring-boot:run
   ```

5. **运行前端**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

### 提交 PR

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## ❓ 常见问题

### 1. 服务启动失败

**问题**：`docker-compose up -d` 后服务无法启动

**解决**：

```bash
# 查看日志
docker-compose logs

# 检查端口占用
netstat -ano | findstr :8080
netstat -ano | findstr :3306

# 重启服务
docker-compose down
docker-compose up -d
```

### 2. 数据库连接失败

**问题**：后端无法连接 MySQL

**解决**：

```bash
# 等待 MySQL 完全启动（约 30 秒）
docker-compose logs mysql

# 检查网络
docker-compose exec backend ping mysql

# 重启后端
docker-compose restart backend
```

### 3. 前端页面空白

**问题**：访问 http://localhost 显示空白

**解决**：

```bash
# 检查前端日志
docker-compose logs frontend

# 清除浏览器缓存
# Ctrl+Shift+Delete

# 检查后端连接
docker-compose exec frontend wget http://backend:8080/api/health
```

### 4. API 密钥配置

**问题**：不知道如何获取 API 密钥

**解决**：

- **通义千问**：访问 https://dashscope.console.aliyun.com/
- **DeepSeek**：访问 https://platform.deepseek.com/
- **OpenAI**：访问 https://platform.openai.com/api-keys

---

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源。

### 使用的开源框架

| 框架          | 协议       | 用途       |
| ------------- | ---------- | ---------- |
| Spring Boot   | Apache 2.0 | 后端框架   |
| Vue.js        | MIT        | 前端框架   |
| Element Plus  | MIT        | UI 组件库  |
| MyBatis-Plus  | Apache 2.0 | ORM 框架   |
| Elasticsearch | Apache 2.0 | 搜索引擎   |
| Redis         | BSD        | 缓存数据库 |
| Kafka         | Apache 2.0 | 消息队列   |

---

## 👥 开发团队

- **项目负责人**：[您的姓名]
- **后端开发**：[团队成员]
- **前端开发**：[团队成员]
- **AI 算法**：[团队成员]

---

## 📞 联系方式

- **项目主页**：https://github.com/YOUR_USERNAME/AI-SecondBrain
- **问题反馈**：https://github.com/YOUR_USERNAME/AI-SecondBrain/issues
- **邮箱**：your-email@example.com

---

## 🙏 致谢

感谢以下开源项目和支持者：

- Spring 社区
- Vue.js 社区
- Element Plus 团队
- 阿里云 DashScope
- DeepSeek 团队
- 所有贡献者

---

## 📈 项目统计

![GitHub stars](https://img.shields.io/github/stars/YOUR_USERNAME/AI-SecondBrain?style=social)
![GitHub forks](https://img.shields.io/github/forks/YOUR_USERNAME/AI-SecondBrain?style=social)
![GitHub issues](https://img.shields.io/github/issues/YOUR_USERNAME/AI-SecondBrain)
![GitHub license](https://img.shields.io/github/license/YOUR_USERNAME/AI-SecondBrain)

**代码统计**：

- **文件数**：470+
- **代码行数**：35,738+
- **贡献者**：[数字]
- **提交次数**：[数字]

---

## 🎯 路线图

### V2.0（当前版本）

- ✅ 多平台对话采集
- ✅ AI 知识提取
- ✅ 智能复习系统
- ✅ RAG 知识问答
- ✅ 学习报告生成

### V3.0（计划中）

- 🔄 移动端 App
- 🔄 多人协作功能
- 🔄 知识分享社区
- 🔄 更多 AI 模型支持

---

**⭐ 如果这个项目对您有帮助，请给一个 Star 支持一下！**

**🚀 立即开始构建您的第二大脑！**
