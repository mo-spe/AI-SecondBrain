# 配置文件说明

本文档详细说明 AI-SecondBrain 项目的配置文件结构和使用方法。

## 📁 配置文件位置

### 后端配置文件

所有配置文件位于 `backend/src/main/resources/` 目录：

```
backend/src/main/resources/
├── application.yml          # 主配置文件（通用配置）
├── application-local.yml    # 本地开发环境配置
└── application-prod.yml     # 生产环境配置
```

### 环境变量文件

- `.env.example` - 环境变量模板（提交到代码库）
- `.env` - 实际环境变量文件（不提交到代码库）

## 🔧 配置说明

### application.yml（主配置文件）

包含所有环境的通用配置，包括：

- **服务器配置**：端口、上下文路径、编码
- **数据库配置**：MySQL 连接（使用环境变量）
- **Redis 配置**：缓存服务连接（使用环境变量）
- **Kafka 配置**：消息队列（使用环境变量）
- **Elasticsearch 配置**：搜索引擎（使用环境变量）
- **邮件服务配置**：SMTP 服务（使用环境变量）
- **MyBatis-Plus 配置**：ORM 框架
- **AI 服务配置**：通义千问、DeepSeek、OpenAI
- **JWT 配置**：认证密钥（使用环境变量）
- **阿里云 OSS 配置**：对象存储（使用环境变量）
- **DeerFlow 配置**：AI 服务集成
- **日志配置**：日志级别和路径

### application-local.yml（本地开发）

本地开发环境的配置，特点：

- 使用环境变量，但有合理的默认值
- 排除了 Kafka 和 Elasticsearch 的自动配置（可选）
- 开启了详细的 SQL 日志
- 使用 localhost 连接所有服务

**使用方式**：

```bash
# 启动时使用 local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### application-prod.yml（生产环境）

生产环境的配置，特点：

- **所有敏感配置必须使用环境变量**
- 性能优化（连接池、压缩等）
- 关闭了详细的错误信息
- 开启了响应压缩
- 生产环境关闭 SQL 日志

**使用方式**：

```bash
# 启动时使用 prod profile
java -jar -Dspring.profiles.active=prod ai-second-brain.jar
```

## 🔐 环境变量说明

### 数据库配置

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `MYSQL_HOST` | MySQL 主机地址 | localhost | 否 |
| `MYSQL_PORT` | MySQL 端口 | 3306 | 否 |
| `MYSQL_DATABASE` | 数据库名称 | second_brain | 否 |
| `MYSQL_USER` | 数据库用户名 | root | 否 |
| `MYSQL_PASSWORD` | 数据库密码 | (空) | **是** |

### Redis 配置

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `REDIS_HOST` | Redis 主机地址 | localhost | 否 |
| `REDIS_PORT` | Redis 端口 | 6379 | 否 |
| `REDIS_PASSWORD` | Redis 密码 | (空) | **是** |

### Kafka 配置

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `KAFKA_ENABLED` | 是否启用 Kafka | true | 否 |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka 服务器地址 | localhost:9092 | 否 |

### Elasticsearch 配置

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `ES_ENABLED` | 是否启用 ES | true | 否 |
| `ES_USERNAME` | ES 用户名 | elastic | 否 |
| `ES_PASSWORD` | ES 密码 | elastic123 | 否 |

### AI 服务配置

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `AI_PROVIDER` | AI 提供商 | qwen | 否 |
| `QWEN_API_KEY` | 通义千问 API 密钥 | (空) | **是** |
| `QWEN_BASE_URL` | 通义千问 API 地址 | https://dashscope.aliyuncs.com/compatible-mode/v1 | 否 |
| `QWEN_MODEL` | 通义千问模型 | qwen-plus | 否 |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | (空) | 可选 |
| `OPENAI_API_KEY` | OpenAI API 密钥 | (空) | 可选 |

### JWT 配置

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `JWT_SECRET` | JWT 密钥 | (长密钥) | **生产环境必须修改** |
| `JWT_EXPIRATION` | JWT 过期时间（毫秒） | 86400000 | 否 |

### DeerFlow 配置

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `DEERFLOW_API_URL` | DeerFlow API 地址 | http://localhost:8000 | 否 |
| `DEERFLOW_API_TIMEOUT` | API 超时时间（秒） | 300 | 否 |

### 阿里云 OSS 配置（可选）

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `ALIYUN_OSS_ENDPOINT` | OSS 端点 | oss-cn-hangzhou.aliyuncs.com | 否 |
| `ALIYUN_OSS_ACCESS_KEY_ID` | AccessKey ID | (空) | 使用 OSS 时必填 |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | AccessKey Secret | (空) | 使用 OSS 时必填 |
| `ALIYUN_OSS_BUCKET_NAME` | Bucket 名称 | thesecondbrain | 否 |

### 邮件服务配置（可选）

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `MAIL_ENABLED` | 是否启用邮件服务 | false | 否 |
| `MAIL_HOST` | SMTP 服务器 | smtp.qq.com | 使用邮件时必填 |
| `MAIL_PORT` | SMTP 端口 | 587 | 使用邮件时必填 |
| `MAIL_USERNAME` | SMTP 用户名 | (空) | 使用邮件时必填 |
| `MAIL_PASSWORD` | SMTP 密码 | (空) | 使用邮件时必填 |

## 🚀 使用方式

### 本地开发

1. 复制环境变量文件：

```bash
cp .env.example .env
```

2. 编辑 `.env` 文件，配置必要的变量：

```bash
# 数据库配置
MYSQL_PASSWORD=your_password

# Redis 配置
REDIS_PASSWORD=your_password

# AI 配置（至少配置一个）
QWEN_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
```

3. 启动后端：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Docker 部署

1. 编辑 `.env` 文件：

```bash
# 数据库配置
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_PASSWORD=your_secure_password

# Redis 配置
REDIS_PASSWORD=your_secure_password

# AI 配置
QWEN_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
```

2. 启动所有服务：

```bash
docker-compose up -d
```

### 生产环境部署

1. 设置环境变量（不要使用 .env 文件）：

```bash
# Linux/macOS
export MYSQL_HOST=prod-mysql.example.com
export MYSQL_PASSWORD=very_secure_password
export REDIS_HOST=prod-redis.example.com
export REDIS_PASSWORD=very_secure_password
export JWT_SECRET=very-long-secure-secret-key
export QWEN_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx

# 或者使用 .env 文件（确保不提交到 Git）
echo "MYSQL_PASSWORD=very_secure_password" >> .env
```

2. 启动应用：

```bash
java -Dspring.profiles.active=prod -jar ai-second-brain.jar
```

## ⚠️ 安全注意事项

### 1. 保护敏感信息

- ✅ **永远不要**将 `.env` 文件提交到 Git
- ✅ **永远不要**在代码中硬编码密码
- ✅ 生产环境使用强密码（至少 16 位）
- ✅ 定期更新 API 密钥和密码

### 2. JWT 密钥安全

```bash
# 生成安全的 JWT 密钥
openssl rand -base64 32
# 输出：xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# 设置为环境变量
export JWT_SECRET=生成的密钥
```

### 3. 数据库安全

- 使用强密码
- 限制数据库用户权限
- 使用 SSL 连接（生产环境）
- 定期备份数据

### 4. Redis 安全

- 设置密码
- 绑定到内网 IP
- 禁用危险命令（CONFIG、FLUSHALL 等）

## 🔍 配置优先级

Spring Boot 配置加载优先级（从高到低）：

1. 命令行参数
2. SPRING_APPLICATION_JSON 中的属性
3. ServletConfig 初始化参数
4. ServletContext 初始化参数
5. JNDI 属性
6. Java 系统属性（-D）
7. 操作系统环境变量
8. RandomValuePropertySource
9. jar 包外部的 application-{profile}.properties/yml
10. jar 包内部的 application-{profile}.properties/yml
11. jar 包外部的 application.properties/yml
12. jar 包内部的 application.properties/yml
13. @PropertySource 注解
14. 默认属性

**本项目使用**：
- `application.yml` - 基础配置（优先级 11/12）
- `application-local.yml` - 本地配置（优先级 9/10）
- `application-prod.yml` - 生产配置（优先级 9/10）
- 环境变量 - 覆盖所有配置（优先级 7）

## 📝 最佳实践

### 1. 使用环境变量

```yaml
# ✅ 推荐
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:second_brain}
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:}

# ❌ 不推荐
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/second_brain
    username: root
    password: 123456
```

### 2. 为环境变量提供默认值

```yaml
# ✅ 推荐 - 提供合理的默认值
redis:
  host: ${REDIS_HOST:localhost}
  port: ${REDIS_PORT:6379}

# ❌ 不推荐 - 没有默认值
redis:
  host: ${REDIS_HOST}
  port: ${REDIS_PORT}
```

### 3. 敏感信息必须使用环境变量

```yaml
# ✅ 推荐
jwt:
  secret: ${JWT_SECRET:}
  
# ❌ 不推荐
jwt:
  secret: my-secret-key
```

### 4. 使用 Profile 分离环境

```bash
# 本地开发
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 生产环境
java -Dspring.profiles.active=prod -jar app.jar
```

## 🆘 故障排查

### 问题 1：无法连接数据库

**检查**：
```bash
# 1. 检查环境变量
echo $MYSQL_HOST
echo $MYSQL_PASSWORD

# 2. 检查网络
ping $MYSQL_HOST

# 3. 检查端口
telnet $MYSQL_HOST $MYSQL_PORT
```

### 问题 2：Redis 连接失败

**检查**：
```bash
# 1. 检查 Redis 是否运行
docker-compose ps redis

# 2. 检查 Redis 日志
docker-compose logs redis

# 3. 测试连接
redis-cli -h $REDIS_HOST -a $REDIS_PASSWORD ping
```

### 问题 3：配置未生效

**检查**：
```bash
# 1. 查看当前使用的 profile
java -jar app.jar --debug

# 2. 查看加载的配置
curl http://localhost:8080/api/actuator/env

# 3. 检查环境变量是否设置
printenv | grep MYSQL
```

## 📚 相关文档

- [Spring Boot 配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12 因素应用](https://12factor.net/zh_cn/)
- [开发环境搭建指南](DEVELOPMENT_GUIDE.md)
- [部署指南](GITHUB_SUBMISSION_GUIDE.md)

---

*最后更新：2026-03-19*
