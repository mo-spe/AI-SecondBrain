# AI-SecondBrain 阿里云部署指南

## 📋 目录

- [部署前准备](#部署前准备)
- [服务器配置要求](#服务器配置要求)
- [快速部署（推荐）](#快速部署推荐)
- [手动部署](#手动部署)
- [配置说明](#配置说明)
- [常见问题](#常见问题)
- [运维管理](#运维管理)

---

## 🚀 部署前准备

### 1. 阿里云服务器准备

#### 1.1 购买 ECS 实例
- **推荐配置**：
  - CPU：4 核
  - 内存：8GB
  - 系统：Ubuntu 20.04 LTS 或 CentOS 7.9
  - 存储：40GB SSD（至少）
  
- **最低配置**：
  - CPU：2 核
  - 内存：4GB
  - 系统：Ubuntu 20.04 LTS 或 CentOS 7.9
  - 存储：20GB SSD

#### 1.2 配置安全组
登录阿里云控制台，配置安全组规则：

| 端口 | 协议 | 授权对象 | 说明 |
|------|------|----------|------|
| 22 | TCP | 0.0.0.0/0 | SSH 连接 |
| 80 | TCP | 0.0.0.0/0 | HTTP 访问 |
| 443 | TCP | 0.0.0.0/0 | HTTPS 访问（可选） |

#### 1.3 连接服务器
```bash
# 使用 SSH 连接
ssh root@你的服务器 IP
```

### 2. 安装 Docker

#### Ubuntu 系统
```bash
# 更新系统
apt update && apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# 安装 Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.23.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version

# 启动 Docker
systemctl start docker
systemctl enable docker
```

#### CentOS 系统
```bash
# 安装 yum 工具包
yum install -y yum-utils

# 添加 Docker 仓库
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 安装 Docker
yum install -y docker-ce docker-ce-cli containerd.io

# 安装 Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.23.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version

# 启动 Docker
systemctl start docker
systemctl enable docker
```

### 3. 上传项目代码

#### 方法一：使用 Git
```bash
# 安装 Git
yum install -y git  # CentOS
apt install -y git  # Ubuntu

# 克隆项目
git clone https://github.com/your-username/ai-second-brain.git
cd ai-second-brain
```

#### 方法二：使用 SCP 上传
```bash
# 在本地执行
tar -czf ai-second-brain.tar.gz ai-second-brain
scp ai-second-brain.tar.gz root@你的服务器 IP:/root/

# 在服务器执行
tar -xzf ai-second-brain.tar.gz
cd ai-second-brain
```

---

## ⚡ 快速部署（推荐）

### 步骤 1：配置环境变量

```bash
# 复制环境配置文件
cp .env.example .env.production

# 编辑配置文件
vim .env.production
```

**必须修改的配置项**：
- `MYSQL_ROOT_PASSWORD` - MySQL root 密码（强密码）
- `MYSQL_PASSWORD` - MySQL 用户密码（强密码）
- `REDIS_PASSWORD` - Redis 密码（强密码）
- `QWEN_API_KEY` - 通义千问 API 密钥
- `DOMAIN_NAME` - 你的域名（可选）
- `ENABLE_HTTPS` - 是否启用 HTTPS

### 步骤 2：执行部署脚本

```bash
# 赋予执行权限
chmod +x deploy.sh

# 执行部署
bash deploy.sh
```

部署脚本会自动完成以下操作：
1. ✅ 检查 Docker 安装
2. ✅ 检查配置文件
3. ✅ 创建必要目录
4. ✅ 停止旧服务
5. ✅ 清理旧容器
6. ✅ 构建所有服务
7. ✅ 启动所有服务
8. ✅ 检查服务状态

### 步骤 3：验证部署

```bash
# 查看服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看后端日志
docker-compose -f docker-compose.prod.yml logs backend

# 查看前端日志
docker-compose -f docker-compose.prod.yml logs nginx
```

**访问地址**：
- 前端：`http://你的服务器 IP`
- API 文档：`http://你的服务器 IP/api/doc.html`
- 后端健康检查：`http://你的服务器 IP/api/actuator/health`

---

## 🔧 手动部署

### 步骤 1：准备环境

```bash
# 创建必要的目录
mkdir -p logs/mysql logs/backend logs/nginx ssl redis

# 复制配置文件
cp .env.example .env.production
```

### 步骤 2：修改配置

编辑 `.env.production` 文件，修改必要的配置项。

### 步骤 3：构建并启动

```bash
# 构建所有服务
docker-compose -f docker-compose.prod.yml build

# 启动所有服务
docker-compose -f docker-compose.prod.yml up -d

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f
```

### 步骤 4：初始化数据库

```bash
# 进入 MySQL 容器
docker exec -it ai-second-brain-mysql mysql -u root -p

# 创建数据库（如果未自动创建）
CREATE DATABASE IF NOT EXISTS second_brain DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE second_brain;

# 导入表结构
source /docker-entrypoint-initdb.d/your_schema.sql;

# 退出
exit;
```

---

## ⚙️ 配置说明

### 环境变量配置

#### 数据库配置
```bash
MYSQL_ROOT_PASSWORD=强密码    # MySQL root 密码
MYSQL_DATABASE=second_brain   # 数据库名称
MYSQL_USER=secondbrain        # 数据库用户名
MYSQL_PASSWORD=强密码         # 数据库用户密码
MYSQL_PORT=3306              # MySQL 端口
```

#### Redis 配置
```bash
REDIS_PASSWORD=强密码    # Redis 密码
REDIS_PORT=6379          # Redis 端口
```

#### AI 服务配置
```bash
AI_PROVIDER=qwen                    # AI 提供商：qwen/deepseek/openai
QWEN_API_KEY=你的密钥               # 通义千问 API 密钥
QWEN_MODEL=qwen-plus                # 通义千问模型
QWEN_EMBEDDING_MODEL=text-embedding-v2  # Embedding 模型
```

#### 阿里云 OSS 配置
```bash
ALIYUN_OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com  # OSS Endpoint
ALIYUN_OSS_ACCESS_KEY_ID=你的密钥                 # AccessKey ID
ALIYUN_OSS_ACCESS_KEY_SECRET=你的密钥             # AccessKey Secret
ALIYUN_OSS_BUCKET_NAME=thesecondbrain             # Bucket 名称
```

#### JVM 配置
```bash
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

根据服务器内存调整：
- 4GB 内存：`-Xms512m -Xmx1024m`
- 8GB 内存：`-Xms1g -Xmx2g`
- 16GB 内存：`-Xms2g -Xmx4g`

---

## 🔒 HTTPS 配置（可选）

### 申请 SSL 证书

#### 使用 Let's Encrypt（免费）
```bash
# 安装 certbot
apt install -y certbot  # Ubuntu
yum install -y certbot  # CentOS

# 申请证书
certbot certonly --standalone -d your-domain.com
```

证书文件位置：
- 证书：`/etc/letsencrypt/live/your-domain.com/fullchain.pem`
- 私钥：`/etc/letsencrypt/live/your-domain.com/privkey.pem`

### 配置 Nginx

修改 `nginx/conf.d/default.conf`：

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # ... 其他配置
}
```

### 复制证书到 SSL 目录

```bash
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem ssl/cert.pem
cp /etc/letsencrypt/live/your-domain.com/privkey.pem ssl/key.pem
```

### 重启服务

```bash
docker-compose -f docker-compose.prod.yml restart nginx
```

---

## ❓ 常见问题

### 1. 服务启动失败

**问题**：容器启动后立即退出

**解决方案**：
```bash
# 查看日志
docker-compose -f docker-compose.prod.yml logs 服务名

# 检查配置文件
docker-compose -f docker-compose.prod.yml config

# 重新构建
docker-compose -f docker-compose.prod.yml build --no-cache
```

### 2. 数据库连接失败

**问题**：后端无法连接 MySQL

**解决方案**：
```bash
# 检查 MySQL 是否启动
docker-compose -f docker-compose.prod.yml ps mysql

# 查看 MySQL 日志
docker-compose -f docker-compose.prod.yml logs mysql

# 等待 MySQL 完全启动（首次启动需要 1-2 分钟）
docker-compose -f docker-compose.prod.yml restart backend
```

### 3. 端口冲突

**问题**：端口已被占用

**解决方案**：
修改 `.env.production` 中的端口配置：
```bash
MYSQL_PORT=3307    # 改为其他端口
BACKEND_PORT=8081  # 改为其他端口
```

### 4. 内存不足

**问题**：OOM Killer 杀死容器

**解决方案**：
```bash
# 调整 JVM 内存
JAVA_OPTS=-Xms256m -Xmx512m

# 调整 ES 内存
ES_JAVA_OPTS=-Xms256m -Xmx512m

# 或者增加服务器内存
```

### 5. Elasticsearch 启动失败

**问题**：ES 启动失败

**解决方案**：
```bash
# 检查系统配置
sysctl -w vm.max_map_count=262144

# 永久生效
echo "vm.max_map_count=262144" >> /etc/sysctl.conf
sysctl -p

# 重启 ES
docker-compose -f docker-compose.prod.yml restart elasticsearch
```

---

## 🛠️ 运维管理

### 查看服务状态

```bash
# 查看所有服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看特定服务状态
docker-compose -f docker-compose.prod.yml ps backend

# 查看资源使用
docker stats
```

### 查看日志

```bash
# 查看所有服务日志
docker-compose -f docker-compose.prod.yml logs

# 查看特定服务日志
docker-compose -f docker-compose.prod.yml logs backend

# 实时查看日志
docker-compose -f docker-compose.prod.yml logs -f

# 查看最近 100 行
docker-compose -f docker-compose.prod.yml logs --tail=100 backend
```

### 重启服务

```bash
# 重启所有服务
docker-compose -f docker-compose.prod.yml restart

# 重启特定服务
docker-compose -f docker-compose.prod.yml restart backend

# 先停止再启动
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d
```

### 停止服务

```bash
# 停止所有服务
docker-compose -f docker-compose.prod.yml down

# 停止并删除容器
docker-compose -f docker-compose.prod.yml down --remove-orphans

# 停止并删除数据卷（危险！会删除数据库数据）
docker-compose -f docker-compose.prod.yml down -v
```

### 备份数据

```bash
# 备份 MySQL 数据库
docker exec ai-second-brain-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} ${MYSQL_DATABASE} > backup_$(date +%Y%m%d).sql

# 备份 Redis 数据
docker exec ai-second-brain-redis redis-cli -a ${REDIS_PASSWORD} BGSAVE

# 复制数据卷
cp -r /var/lib/docker/volumes/ai-second-brain_mysql-data/_data /backup/mysql
```

### 恢复数据

```bash
# 恢复 MySQL 数据库
cat backup_20240101.sql | docker exec -i ai-second-brain-mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} ${MYSQL_DATABASE}
```

### 更新版本

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose -f docker-compose.prod.yml build
docker-compose -f docker-compose.prod.yml up -d

# 清理旧镜像
docker image prune -f
```

### 监控服务

```bash
# 查看容器资源使用
docker stats

# 查看磁盘使用
docker system df

# 查看网络
docker network ls
docker network inspect ai-second-brain-network
```

### 清理空间

```bash
# 清理停止的容器
docker container prune

# 清理未使用的镜像
docker image prune

# 清理未使用的数据卷
docker volume prune

# 清理所有未使用的资源
docker system prune -a
```

---

## 📊 性能优化建议

### 1. 数据库优化

在 `docker-compose.prod.yml` 中调整 MySQL 配置：

```yaml
command: >
  --innodb-buffer-pool-size=1G
  --innodb-log-file-size=256M
  --max-connections=300
  --query-cache-size=64M
```

### 2. Redis 优化

在 `redis/redis.conf` 中调整：

```bash
maxmemory 512mb
maxmemory-policy allkeys-lru
```

### 3. Elasticsearch 优化

在 `docker-compose.prod.yml` 中调整：

```yaml
environment:
  ES_JAVA_OPTS: "-Xms2g -Xmx2g"
```

### 4. JVM 优化

在 `.env.production` 中调整：

```bash
JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100
```

---

## 📞 技术支持

如遇到问题，请：
1. 查看日志文件
2. 检查配置文件
3. 查看常见问题部分
4. 联系技术支持

**日志文件位置**：
- 后端：`logs/backend/`
- MySQL：`logs/mysql/`
- Nginx：`logs/nginx/`

---

## ✅ 部署检查清单

- [ ] 服务器配置满足要求
- [ ] Docker 和 Docker Compose 已安装
- [ ] 安全组规则已配置
- [ ] 项目代码已上传
- [ ] `.env.production` 已配置
- [ ] MySQL 密码已修改为强密码
- [ ] Redis 密码已修改为强密码
- [ ] API 密钥已配置
- [ ] 服务已全部启动
- [ ] 前端可以正常访问
- [ ] API 文档可以正常访问
- [ ] 后端健康检查通过
- [ ] 数据库连接正常
- [ ] 数据备份策略已制定

---

**部署完成后，请妥善保管：**
1. `.env.production` 文件（包含敏感信息）
2. 数据库密码
3. API 密钥
4. SSL 证书（如果启用）

**建议定期：**
1. 备份数据库
2. 更新系统补丁
3. 检查日志文件
4. 监控资源使用
