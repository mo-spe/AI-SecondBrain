# AI-SecondBrain 项目结构

## 📁 根目录文件说明

```
AI-SecondBrain/
├── 📄 .dockerignore              # Docker 忽略文件配置
├── 📄 .env.example               # 环境变量模板（复制为 .env 后使用）
├── 📄 .gitignore                 # Git 忽略文件配置
├── 📄 complete_database_schema_verified.sql  # 完整的数据库架构（已验证）
├── 📄 CONTRIBUTING.md            # 贡献指南
├── 📄 docker-compose.yml         # Docker 编排配置文件
├── 📄 Dockerfile                 # 后端 Docker 镜像构建文件
├── 📄 LICENSE                    # MIT 开源协议
├── 📄 pom.xml                    # Maven 项目配置文件
├── 📄 README.md                  # 项目说明文档
├── 📄 start.bat                  # Windows 快速启动脚本
├── 📄 start.sh                   # Linux/macOS快速启动脚本
│
├── 📁 .github/                   # GitHub 配置目录
│   ├── ISSUE_TEMPLATE.md        # Issue 模板
│   ├── PULL_REQUEST_TEMPLATE.md # PR 模板
│   └── workflows/
│       └── ci.yml               # GitHub Actions CI/CD 配置
│
├── 📁 backend/                   # 后端源代码目录
│   └── src/
│       ├── main/java/           # Java 源代码
│       │   └── com/secondbrain/
│       │       ├── common/      # 公共类
│       │       ├── config/      # 配置类
│       │       ├── controller/  # REST API 控制器
│       │       ├── dto/         # 数据传输对象
│       │       ├── elasticsearch/ # ES 文档类
│       │       ├── entity/      # 实体类
│       │       ├── exception/   # 异常处理
│       │       ├── interceptor/ # 拦截器
│       │       ├── kafka/       # Kafka 服务
│       │       ├── mapper/      # 数据访问层
│       │       ├── service/     # 业务逻辑层
│       │       ├── task/        # 定时任务
│       │       ├── util/        # 工具类
│       │       └── vo/          # 视图对象
│       └── resources/           # 资源文件
│           ├── sql/             # SQL 脚本
│           └── application*.yml # 应用配置文件
│
├── 📁 frontend/                  # 前端源代码目录
│   ├── src/
│   │   ├── api/                 # API 封装
│   │   ├── components/          # 组件
│   │   ├── layout/              # 布局
│   │   ├── router/              # 路由
│   │   ├── stores/              # 状态管理
│   │   ├── styles/              # 样式
│   │   ├── utils/               # 工具函数
│   │   └── views/               # 页面视图
│   ├── Dockerfile               # 前端 Docker 镜像
│   ├── index.html               # HTML 入口
│   ├── nginx.conf               # Nginx 配置
│   └── package.json             # Node.js 依赖配置
│
├── 📁 extension/                 # 浏览器插件目录
│   ├── popup/                   # 插件弹窗页面
│   ├── icons/                   # 插件图标
│   ├── config/                  # 配置文件
│   ├── background.js            # 后台脚本
│   ├── content.js               # 内容脚本
│   ├── manifest.json            # 插件清单
│   └── build.sh/build.bat       # 构建脚本
│
├── 📁 deerflow/                  # AI 服务目录
│   ├── app.py                   # FastAPI 应用
│   ├── config.yaml              # 配置文件
│   ├── requirements.txt         # Python 依赖
│   └── *.py                     # Python 服务文件
│
└── 📁 docs/                      # 文档目录
    └── DEERFLOW_INTEGRATION_PLAN.md  # DeerFlow 集成计划
```

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/YOUR_USERNAME/AI-SecondBrain.git
cd AI-SecondBrain
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，配置必需的环境变量
# Windows: notepad .env
# macOS/Linux: vim .env
```

**必须配置的项目**：
- `MYSQL_ROOT_PASSWORD` - MySQL root 密码
- `MYSQL_PASSWORD` - MySQL 用户密码
- `REDIS_PASSWORD` - Redis 密码
- `QWEN_API_KEY` - 通义千问 API 密钥（或其他 AI API 密钥）

### 3. 一键启动

#### Windows:
```bash
start.bat
```

#### Linux/macOS:
```bash
chmod +x start.sh
./start.sh
```

### 4. 访问应用

- **前端界面**: http://localhost
- **API 文档**: http://localhost:8080/api/doc.html
- **健康检查**: http://localhost:8080/api/health

**默认登录凭据**：
- 用户名：`admin`
- 密码：`admin123`

⚠️ **首次登录后请立即修改密码！**

## 📊 技术栈

### 后端
- Java 21 + Spring Boot 3.1.5
- MyBatis-Plus 3.5.3
- MySQL 8.0 + Redis 7
- Kafka 3.6 + Zookeeper 3.8
- Elasticsearch 8.11

### 前端
- Vue 3.4 + Vite 5.0
- Element Plus 2.5
- Pinia 2.1

### AI 服务
- Python FastAPI
- 通义千问/DeepSeek

### 部署
- Docker + Docker Compose

## 📝 重要说明

1. **环境变量**：`.env` 文件包含敏感信息，不应提交到 Git
2. **数据持久化**：Docker 数据卷存储在 `mysql-data/`, `redis-data/` 等目录
3. **日志文件**：所有服务日志存储在 `logs/` 目录
4. **证书文件**：HTTPS 证书应放在 `ssl/` 目录（如启用 HTTPS）

## 🔧 开发指南

### 后端开发

```bash
cd backend
mvn spring-boot:run
```

### 前端开发

```bash
cd frontend
npm install
npm run dev
```

### AI 服务开发

```bash
cd deerflow
pip install -r requirements.txt
python app.py
```

## 📖 更多信息

- [README.md](README.md) - 完整项目说明
- [CONTRIBUTING.md](CONTRIBUTING.md) - 贡献指南
- [LICENSE](LICENSE) - MIT 开源协议
