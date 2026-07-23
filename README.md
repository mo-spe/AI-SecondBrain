# AI-SecondBrain 🧠

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4-green.svg)](https://vuejs.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**智能第二大脑系统** - 基于 AI 大模型的知识管理平台，帮助您高效采集、整理、复习 AI 对话中的宝贵知识。

![AI-SecondBrain Banner](docs/images/banner.png)

**注意：这个readme不是最新的，所以不要完全参考**

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

## 🚀 本地部署（推荐）

### 环境要求

- **JDK**: 17
- **Maven**: 3.8+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Redis**: 7+
- **操作系统**: Windows / macOS / Linux

### 步骤 1：克隆项目

```bash
git clone https://github.com/mo-spe/AI-SecondBrain.git
cd AI-SecondBrain
```

### 步骤 2：配置数据库

**方式一（根据sql目录创建）**

#### 创建数据库

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE secondbrain CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 执行初始化脚本

```bash
# 在 MySQL 中执行、
USE secondbrain;
然后执行sql目录下sql语句

```

**方式二：直接连我的本机数据库（推荐）**

```java
# 这里host在本地测试时可以连接我的本机数据库
  # IP：10.65.59.104
  #端口：3306
  #账号：dev
  #密码：123456
```

### 步骤 3：配置后端

#### 修改配置文件

编辑 `backend/src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/secondbrain?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_mysql_password
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password

# AI API 配置（至少配置一个，可先不配置）
ai:
  openai:
    api-key: your_api_key
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
```

### 步骤 4：启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务地址：
- API 文档：http://localhost:8080/api/doc.html
- 健康检查：http://localhost:8080/api/health

### 步骤 5：启动前端

```bash
cd frontend
npm install
npm run dev
```

前端访问地址：http://localhost:5173

### 步骤 6：登录（如果连的是我的本机数据库，否则跳过自己创建）

```
用户名：newuser
密码：123456
```

---

## 🐳 Docker 部署（可选，这个不太确定还对不对了，不推荐）

### 环境要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **内存**: 最少 4GB，推荐 8GB+
- **磁盘**: 40GB+ 可用空间
- **操作系统**: Windows / macOS / Linux

### 步骤 1：配置环境变量

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

### 步骤 2：一键启动

```bash
# 启动所有服务
docker-compose up -d

# 查看日志（可选）
docker-compose logs -f

# 检查服务状态
docker-compose ps
```

### 步骤 3：访问应用

- **前端界面**: http://localhost
- **API 文档**: http://localhost:8080/api/doc.html
- **后端健康检查**: http://localhost:8080/api/health

### 步骤 4：默认登录

```
用户名：admin
密码：admin123
```

**⚠️ 首次登录后请立即修改密码！**

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

| 技术          | 版本  | 用途         |
| ------------- | ----- | ------------ |
| Java          | 17    | 主要开发语言 |
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

## 📦 项目结构

```
AI-SecondBrain/
├── 📁 backend/                  # 后端源代码
│   ├── src/main/java/
│   │   └── com/secondbrain/
│   │       ├── controller/      # REST API 控制器
│   │       ├── service/         # 业务逻辑层
│   │       ├── mapper/          # 数据访问层
│   │       ├── entity/          # 实体类
│   │       ├── dto/             # 数据传输对象
│   │       ├── config/          # 配置类
│   │       └── util/            # 工具类
│   └── src/main/resources/
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
├── complete_database_schema_verified.sql  # 数据库初始化脚本
├── start.bat                    # Windows 启动脚本
├── start.sh                     # Linux/macOS 启动脚本
├── README.md                    # 项目主文档
├── DEVELOPMENT_GUIDE.md         # 开发环境搭建指南
├── CONFIGURATION.md             # 配置文件说明
├── CONTRIBUTING.md              # 贡献指南
└── LICENSE                      # MIT 开源协议
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

编辑 `backend/src/main/resources/application.yml`：

```yaml
server:
  port: 8081
```

编辑 `frontend/vite.config.js`：

```javascript
server: {
  port: 3000
}
```

### 配置多个 AI 提供商

编辑 `backend/src/main/resources/application.yml`：

```yaml
ai:
  openai:
    api-key: your_key
    base-url: https://api.openai.com/v1
  qwen:
    api-key: your_key
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
  deepseek:
    api-key: your_key
    base-url: https://api.deepseek.com/v1
```

### 启用 HTTPS

```yaml
server:
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: your_password
    key-store-type: PKCS12
```

---

## 📊 性能优化建议

### 内存优化

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

### 禁用不需要的服务

编辑 `backend/src/main/resources/application.yml`，注释掉不需要的配置：

```yaml
# spring:
#   kafka:
#     bootstrap-servers: localhost:9092
```

---

## 🧪 测试

### 运行后端测试

```bash
cd backend
mvn test
```

### 运行前端测试

```bash
cd frontend
npm test
```

### 健康检查

```bash
# 检查后端
curl http://localhost:8080/api/health

# 检查数据库连接
mysql -u root -p -e "SELECT 1;"
```

---

## 📖 相关文档

- [开发环境搭建指南](DEVELOPMENT_GUIDE.md) - 详细的本地开发环境配置步骤
- [贡献指南](CONTRIBUTING.md) - 如何参与项目开发
- [LICENSE](LICENSE) - MIT 开源协议

---

## 🤝 贡献指南

### 开发环境搭建

#### 1. 克隆项目

```bash
git clone https://github.com/mo-spe/AI-SecondBrain.git
cd AI-SecondBrain
```

#### 2. 启动基础服务

```bash
# 启动 MySQL、Redis 和 Elasticsearch
docker-compose up -d mysql redis elasticsearch
```

#### 3. 配置本地环境

编辑 `backend/src/main/resources/application.yml`，配置数据库连接和 API 密钥。

#### 4. 启动 DeerFlow AI 服务（可选）

如果需要使用 AI 功能（知识提取、报告生成等）：

```bash
cd deerflow
pip install -r requirements.txt
python app.py
```

#### 5. 运行后端

```bash
cd backend
mvn spring-boot:run
```

#### 6. 运行前端

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

**问题**：后端无法启动

**解决**：

```bash
# 检查端口占用
netstat -ano | findstr :8080

# 查看日志
cd backend
mvn spring-boot:run 2>&1 | tail -50
```

### 2. 数据库连接失败

**问题**：后端无法连接 MySQL

**解决**：

```bash
# 检查 MySQL 服务状态
# Windows: services.msc
# Linux: systemctl status mysql

# 测试连接
mysql -u root -p -h localhost -P 3306
```

### 3. 前端页面空白

**问题**：访问 http://localhost:5173 显示空白

**解决**：

```bash
# 检查前端日志
cd frontend
npm run dev

# 清除浏览器缓存
# Ctrl+Shift+Delete
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

- **项目负责人**：mo-spe
- **后端开发**：mo-spe
- **前端开发**：mo-spe
- **AI 算法**：mo-spe

---

## 📞 联系方式

- **项目主页**：https://github.com/mo-spe/AI-SecondBrain
- **问题反馈**：https://github.com/mo-spe/AI-SecondBrain/issues
- **邮箱**：walliamharric@gmail.com

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

![GitHub stars](https://img.shields.io/github/stars/mo-spe/AI-SecondBrain?style=social)
![GitHub forks](https://img.shields.io/github/forks/mo-spe/AI-SecondBrain?style=social)
![GitHub issues](https://img.shields.io/github/issues/mo-spe/AI-SecondBrain)
![GitHub license](https://img.shields.io/github/license/mo-spe/AI-SecondBrain)

**代码统计**：
- **文件数**：262+
- **代码行数**：35,738+
- **贡献者**：1
- **提交次数**：多次提交

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
