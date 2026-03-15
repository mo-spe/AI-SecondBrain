# 本地测试指南

## 📋 前置条件

### 必需服务（最小配置）
1. **MySQL 8.0+** - 数据库
2. **Java 17+** - 运行后端
3. **Node.js 18+** - 运行前端

### 可选服务（用于完整功能）
4. **Redis 7+** - 缓存（可选，但推荐）
5. **Elasticsearch 8+** - 语义搜索（RAG功能需要）
6. **Kafka 3+** - 消息队列（可选）
7. **Python 3.8+** - DeerFlow服务（可选）

## 🚀 快速开始（最小配置）

### 1. 安装MySQL

**Windows:**
```bash
# 下载并安装MySQL 8.0
# https://dev.mysql.com/downloads/mysql/
```

**Linux:**
```bash
sudo apt install mysql-server
sudo systemctl start mysql
```

### 2. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS second_brain 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE second_brain;

-- 执行数据库初始化脚本
SOURCE d:/AI-SecondBrain/database_schema_complete.sql;
```

### 3. 配置环境变量

创建 `.env` 文件在项目根目录：

```env
# AI配置（必需）
QWEN_API_KEY=your_qwen_api_key_here

# 数据库配置
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=second_brain
MYSQL_USER=root
MYSQL_PASSWORD=123456

# Redis配置（可选）
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Elasticsearch配置（RAG功能需要）
ES_ENABLED=true
ES_HOST=localhost
ES_PORT=9200
ES_USERNAME=
ES_PASSWORD=

# Kafka配置（可选）
KAFKA_ENABLED=false
KAFKA_HOST=localhost
KAFKA_PORT=9092

# 邮件配置（可选）
MAIL_ENABLED=false
```

### 4. 启动后端

```bash
cd d:/AI-SecondBrain

# 使用本地配置启动
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 5. 启动前端

```bash
cd d:/AI-SecondBrain/frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev
```

### 6. 访问应用

- 前端地址：http://localhost:5173
- 后端API：http://localhost:8080/api
- API文档：http://localhost:8080/api/doc.html

## 🎯 完整配置（推荐）

### 1. 使用Docker Compose启动所有服务

如果你想使用完整功能，可以使用Docker Compose启动所有依赖服务：

```bash
cd d:/AI-SecondBrain

# 启动所有服务（MySQL, Redis, Elasticsearch, Kafka, DeerFlow）
docker-compose up -d mysql redis elasticsearch kafka zookeeper deerflow

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 2. 初始化数据库

```bash
# 等待MySQL启动完成（约30秒）
docker-compose exec mysql mysql -uroot -p123456 second_brain < database_schema_complete.sql
```

### 3. 启动后端和前端

```bash
# 终端1：启动后端
cd d:/AI-SecondBrain
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 终端2：启动前端
cd d:/AI-SecondBrain/frontend
npm run dev
```

## 🔧 配置说明

### application-local.yml 配置说明

这个配置文件是为本地测试优化的：

```yaml
# 数据库配置 - 使用本地MySQL
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/second_brain
    username: root
    password: 123456

  # Redis配置 - 使用本地Redis
  data:
    redis:
      host: localhost
      port: 6379
      password: 

  # Kafka配置 - 默认禁用
  kafka:
    enabled: false

  # Elasticsearch配置 - 默认禁用（需要时启用）
  elasticsearch:
    enabled: false

# DeerFlow配置 - 使用本地服务
deerflow:
  api:
    url: http://localhost:8000
```

### 启用/禁用功能

**启用RAG功能（需要Elasticsearch）：**
```bash
# 1. 启动Elasticsearch
docker-compose up -d elasticsearch

# 2. 修改配置
# 在application-local.yml中设置：
spring:
  elasticsearch:
    enabled: true

# 3. 重启后端
```

**启用Kafka消息队列：**
```bash
# 1. 启动Kafka和Zookeeper
docker-compose up -d kafka zookeeper

# 2. 修改配置
# 在application-local.yml中设置：
spring:
  kafka:
    enabled: true

# 3. 重启后端
```

## 🧪 测试新功能

### 测试RAG知识问答

1. 确保Elasticsearch已启动并启用
2. 确保有知识节点且包含向量数据
3. 访问：http://localhost:5173/rag-qa
4. 输入问题并提问

### 测试知识图谱

1. 确保数据库中有知识节点
2. 访问：http://localhost:5173/knowledge-graph
3. 点击"自动生成关系"
4. 查看知识网络图

## 🐛 常见问题

### 1. 数据库连接失败

**问题：** `Communications link failure`

**解决：**
- 检查MySQL是否启动：`mysql -uroot -p123456`
- 检查端口是否正确：默认3306
- 检查用户名密码是否正确

### 2. Redis连接失败

**问题：** `Unable to connect to Redis`

**解决：**
- 检查Redis是否启动：`redis-cli ping`
- 或者禁用Redis功能：设置`spring.data.redis.enabled=false`

### 3. Elasticsearch连接失败

**问题：** `Connection refused`

**解决：**
- 检查Elasticsearch是否启动：`curl http://localhost:9200`
- 或者禁用Elasticsearch：设置`spring.elasticsearch.enabled=false`

### 4. AI API调用失败

**问题：** `AI API调用失败`

**解决：**
- 检查API Key是否正确
- 检查网络连接
- 查看日志中的详细错误信息

### 5. 前端无法连接后端

**问题：** `Network Error`

**解决：**
- 检查后端是否启动：访问 http://localhost:8080/api/actuator/health
- 检查前端代理配置：`frontend/vite.config.js`
- 检查CORS配置

## 📝 开发建议

### 推荐的开发流程

1. **最小配置启动**：只启动MySQL，禁用其他服务
2. **逐步启用**：根据需要启用Redis、Elasticsearch等
3. **使用Docker**：对于复杂服务（Elasticsearch、Kafka），推荐使用Docker
4. **本地开发**：后端和前端在本地运行，方便调试

### 性能优化

1. **关闭不必要的日志**：修改`logging.level`配置
2. **调整JVM参数**：在启动命令中添加`-Xms512m -Xmx1024m`
3. **使用连接池**：MySQL和Redis已配置连接池

## 🎉 总结

现在你的项目支持两种运行方式：

1. **Docker部署**：使用`docker-compose up`启动所有服务
2. **本地测试**：使用`application-local.yml`配置，部分服务本地运行

选择适合你的方式即可！
