# 开发环境搭建指南

本指南将帮助您在本地快速搭建 AI-SecondBrain 的开发环境。

## 📋 环境要求

### 必需软件

- **Git**: 2.30+
- **Docker**: 20.10+ 和 Docker Compose 2.0+
- **Java**: JDK 21+
- **Maven**: 3.8+
- **Node.js**: 18+ 和 npm
- **Python**: 3.10+（可选，用于 DeerFlow 服务）

### 推荐工具

- **IDE**: IntelliJ IDEA / VS Code
- **数据库管理**: DBeaver / MySQL Workbench
- **API 测试**: Postman / Apifox

## 🚀 快速开始

### 步骤 1：克隆项目

```bash
git clone https://github.com/YOUR_USERNAME/AI-SecondBrain.git
cd AI-SecondBrain
```

### 步骤 2：启动基础服务

```bash
# 启动 MySQL、Redis 和 Elasticsearch
docker-compose up -d mysql redis elasticsearch

# 查看服务状态
docker-compose ps

# 查看日志（可选）
docker-compose logs -f
```

**服务端口**：
- MySQL: 3306
- Redis: 6379
- Elasticsearch: 9200

### 步骤 3：配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置文件
# Windows: notepad .env
# macOS/Linux: vim .env
```

**必须配置的项目**：

```bash
# 数据库配置
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_PASSWORD=your_secure_password
MYSQL_DATABASE=second_brain
MYSQL_USER=secondbrain

# Redis 配置
REDIS_PASSWORD=your_secure_password

# AI 服务配置（至少配置一个）
QWEN_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QWEN_MODEL=qwen-plus

# DeepSeek（可选）
# DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx

# OpenAI（可选）
# OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
```

**获取 API 密钥**：
- **通义千问**：https://dashscope.console.aliyun.com/
- **DeepSeek**：https://platform.deepseek.com/
- **OpenAI**：https://platform.openai.com/api-keys

### 步骤 4：初始化数据库

```bash
# 等待 MySQL 完全启动（约 30 秒）
sleep 30

# 连接 MySQL
docker-compose exec mysql mysql -u root -p

# 在 MySQL 命令行中执行
USE second_brain;
SOURCE /docker-entrypoint-initdb.d/complete_database_schema_verified.sql;
EXIT;
```

或者使用脚本：

```bash
# Windows PowerShell
docker-compose exec mysql mysql -u root -pyour_password -e "source /docker-entrypoint-initdb.d/complete_database_schema_verified.sql"

# Linux/macOS
docker-compose exec mysql mysql -u root -p'your_password' < complete_database_schema_verified.sql
```

### 步骤 5：启动 DeerFlow AI 服务（可选）

DeerFlow 提供 AI 知识提取、学习报告生成、智能问答等功能。

#### 方式一：直接运行（推荐）

```bash
cd deerflow

# 安装 Python 依赖
pip install -r requirements.txt

# 复制配置文件
cp config.yaml config.local.yaml

# 编辑配置文件，添加 API 密钥
# 在 config.local.yaml 中添加：
# qwen_api_key: sk-xxxxxxxxxxxxxxxxxxxxxxxx

# 启动服务
python app.py
```

服务地址：http://localhost:8000
API 文档：http://localhost:8000/docs

#### 方式二：使用 Docker

```bash
docker-compose up -d deerflow
```

### 步骤 6：运行后端服务

```bash
cd backend

# 方式一：使用 Maven（推荐）
mvn spring-boot:run

# 方式二：使用 IDE
# 在 IDEA 中右键运行 AiSecondBrainApplication.java
```

**后端服务地址**：
- API 文档：http://localhost:8080/api/doc.html
- 健康检查：http://localhost:8080/api/health
- Swagger UI：http://localhost:8080/swagger-ui.html

### 步骤 7：运行前端应用

```bash
cd frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev
```

**前端访问地址**：http://localhost:5173

### 步骤 8：验证环境

1. 访问 http://localhost:5173
2. 使用默认账号登录：
   - 用户名：`admin`
   - 密码：`admin123`
3. **⚠️ 首次登录后请立即修改密码！**

## 🔍 服务检查清单

### Docker 服务

```bash
# 检查所有服务状态
docker-compose ps

# 应该看到以下服务：
# - mysql (healthy)
# - redis (healthy)
# - elasticsearch (healthy)
```

### 后端服务

```bash
# 检查健康状态
curl http://localhost:8080/api/health

# 预期响应：
# {"code": 200, "message": "success", "data": "OK"}
```

### 前端服务

访问 http://localhost:5173，应该能看到登录页面

### DeerFlow 服务（如果启动）

```bash
# 检查健康状态
curl http://localhost:8000/health

# 访问 API 文档
# http://localhost:8000/docs
```

## 🛠️ 常见问题

### 1. 端口被占用

**问题**：启动时提示端口已被占用

**解决**：

```bash
# Windows - 查看端口占用
netstat -ano | findstr :8080
netstat -ano | findstr :3306
netstat -ano | findstr :6379

# 杀死占用端口的进程
taskkill /F /PID <进程 ID>

# macOS/Linux
lsof -i :8080
kill -9 <进程 ID>
```

### 2. Docker 服务启动失败

**问题**：`docker-compose up -d` 后服务无法启动

**解决**：

```bash
# 查看日志
docker-compose logs mysql
docker-compose logs redis
docker-compose logs elasticsearch

# 重启服务
docker-compose down
docker-compose up -d

# 检查资源
docker system df
docker volume prune
```

### 3. 数据库连接失败

**问题**：后端无法连接 MySQL

**解决**：

```bash
# 等待 MySQL 完全启动（约 30 秒）
sleep 30

# 检查网络连接
docker-compose exec backend ping mysql

# 查看 MySQL 日志
docker-compose logs mysql

# 重启后端
docker-compose restart backend
```

### 4. 前端页面空白

**问题**：访问 http://localhost:5173 显示空白

**解决**：

```bash
# 清除浏览器缓存
# Ctrl+Shift+Delete (Windows)
# Cmd+Shift+Delete (macOS)

# 检查前端日志
# 浏览器开发者工具 -> Console

# 检查后端连接
curl http://localhost:8080/api/health
```

### 5. Python 依赖安装失败

**问题**：`pip install -r requirements.txt` 失败

**解决**：

```bash
# 升级 pip
python -m pip install --upgrade pip

# 使用国内镜像
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 或使用 conda
conda create -n deerflow python=3.10
conda activate deerflow
pip install -r requirements.txt
```

## 📝 开发建议

### 1. 使用 Docker 管理数据

```bash
# 停止所有服务
docker-compose down

# 清除数据（谨慎使用）
docker-compose down -v

# 备份数据
docker-compose exec mysql mysqldump -u root -p second_brain > backup.sql
```

### 2. 热重载开发

**后端**：
```bash
# 使用 spring-boot-devtools
# 修改代码后自动重启
mvn spring-boot:run
```

**前端**：
```bash
# Vite 默认支持热重载
npm run dev
```

### 3. 调试模式

**后端调试**：
```bash
# 在 IDEA 中配置 Remote Debug
# 端口：5005
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

**前端调试**：
```bash
# 开启 source map
npm run dev
# 浏览器开发者工具 -> Sources
```

## 📚 下一步

环境搭建完成后，您可以：

1. **阅读 API 文档**：http://localhost:8080/api/doc.html
2. **测试功能**：访问 http://localhost:5173
3. **查看源代码**：
   - 后端：`backend/src/main/java/com/secondbrain/`
   - 前端：`frontend/src/`
   - DeerFlow：`deerflow/`

## 🆘 获取帮助

- [README.md](README.md) - 项目说明
- [CONTRIBUTING.md](CONTRIBUTING.md) - 贡献指南
- [GitHub Issues](https://github.com/YOUR_USERNAME/AI-SecondBrain/issues) - 问题反馈

---

**祝您开发顺利！** 🎉
