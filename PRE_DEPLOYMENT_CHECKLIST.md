# AI-SecondBrain 部署前检查清单

## 📋 部署前检查（Pre-Deployment Checklist）

在将项目部署到阿里云生产环境之前，请务必完成以下检查项。

---

## ✅ 1. 服务器配置检查

### 1.1 ECS 实例配置
- [ ] CPU：至少 2 核（推荐 4 核或更多）
- [ ] 内存：至少 4GB（推荐 8GB 或更多）
- [ ] 磁盘：至少 20GB 可用空间（推荐 40GB SSD）
- [ ] 操作系统：Ubuntu 20.04 LTS 或 CentOS 7.9

### 1.2 安全组配置
- [ ] 端口 22（SSH）已开放
- [ ] 端口 80（HTTP）已开放
- [ ] 端口 443（HTTPS）已开放（如需启用 HTTPS）
- [ ] 其他端口已限制访问

### 1.3 系统更新
```bash
# Ubuntu
sudo apt update && sudo apt upgrade -y

# CentOS
sudo yum update -y
```
- [ ] 系统已更新到最新版本

---

## ✅ 2. Docker 环境检查

### 2.1 Docker 安装
```bash
docker --version
```
- [ ] Docker 已安装（版本 20.10+）
- [ ] Docker 服务已启动
- [ ] Docker 已设置开机自启

### 2.2 Docker Compose 安装
```bash
docker-compose --version
```
- [ ] Docker Compose 已安装（版本 2.0+）

### 2.3 Docker 配置优化
```bash
# 编辑 Docker 配置
sudo vim /etc/docker/daemon.json
```
- [ ] 配置镜像加速器（中国大陆推荐）
```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://registry.docker-cn.com"
  ]
}
```

---

## ✅ 3. 项目文件检查

### 3.1 必需文件
- [ ] `docker-compose.prod.yml` - 生产环境 Docker 编排文件
- [ ] `.env.production` - 生产环境变量文件
- [ ] `Dockerfile` - 后端 Docker 构建文件
- [ ] `frontend/Dockerfile` - 前端 Docker 构建文件
- [ ] `deploy.sh` - 部署脚本
- [ ] `check_deployment.sh` - 检查脚本
- [ ] `src/main/resources/application-prod.yml` - 生产环境应用配置

### 3.2 配置文件
- [ ] `nginx/nginx.conf` - Nginx 主配置
- [ ] `nginx/conf.d/default.conf` - Nginx 虚拟主机配置
- [ ] `redis/redis.conf` - Redis 配置

### 3.3 数据库脚本
- [ ] `complete_database_schema_verified.sql` - 数据库表结构
- [ ] `src/main/resources/sql/` - SQL 初始化脚本目录

---

## ✅ 4. 环境变量检查

### 4.1 .env.production 文件
```bash
cp .env.example .env.production
vim .env.production
```

#### 必须修改的配置项
- [ ] `MYSQL_ROOT_PASSWORD` - MySQL root 密码（强密码，至少 16 位）
- [ ] `MYSQL_PASSWORD` - MySQL 用户密码（强密码）
- [ ] `REDIS_PASSWORD` - Redis 密码（强密码）
- [ ] `QWEN_API_KEY` - 通义千问 API 密钥

#### 推荐修改的配置项
- [ ] `DOMAIN_NAME` - 域名（如：example.com）
- [ ] `ENABLE_HTTPS` - 是否启用 HTTPS
- [ ] `JAVA_OPTS` - JVM 参数（根据内存调整）
- [ ] `ES_JAVA_OPTS` - Elasticsearch JVM 参数

#### 可选配置项
- [ ] `DEEPSEEK_API_KEY` - DeepSeek API 密钥
- [ ] `OPENAI_API_KEY` - OpenAI API 密钥
- [ ] `ALIYUN_OSS_ACCESS_KEY_ID` - 阿里云 OSS AccessKey
- [ ] `ALIYUN_OSS_ACCESS_KEY_SECRET` - 阿里云 OSS AccessKey Secret
- [ ] `MAIL_ENABLED` - 是否启用邮件通知
- [ ] `MAIL_USERNAME` - 邮箱用户名
- [ ] `MAIL_PASSWORD` - 邮箱密码

### 4.2 密码强度检查
- [ ] MySQL root 密码：至少 16 位，包含大小写字母、数字、特殊字符
- [ ] MySQL 用户密码：至少 16 位，包含大小写字母、数字、特殊字符
- [ ] Redis 密码：至少 16 位，包含大小写字母、数字、特殊字符

**密码生成工具**：
```bash
# 生成 16 位随机密码
openssl rand -base64 16

# 生成 32 位随机密码
openssl rand -base64 32
```

---

## ✅ 5. 安全检查

### 5.1 .gitignore 配置
- [ ] `.env` 文件已添加到 `.gitignore`
- [ ] `.env.production` 文件已添加到 `.gitignore`
- [ ] `ssl/` 目录已添加到 `.gitignore`
- [ ] `logs/` 目录已添加到 `.gitignore`

### 5.2 敏感信息保护
- [ ] API 密钥未硬编码在代码中
- [ ] 数据库密码未硬编码在代码中
- [ ] SSL 证书未提交到代码仓库

### 5.3 防火墙配置
```bash
# Ubuntu
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# CentOS
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```
- [ ] 防火墙已配置并启用

### 5.4 SSH 安全
- [ ] 已禁用 root 用户密码登录（推荐使用 SSH 密钥）
- [ ] 已修改 SSH 默认端口（可选，建议改为其他端口）
- [ ] 已安装 fail2ban 防止暴力破解

---

## ✅ 6. 运行检查脚本

### 6.1 执行自动检查
```bash
chmod +x check_deployment.sh
bash check_deployment.sh
```
- [ ] 所有检查项通过（无 FAIL）
- [ ] 警告项已审查（WARN）

### 6.2 手动验证
```bash
# 验证 Docker Compose 配置
docker-compose -f docker-compose.prod.yml config
```
- [ ] Docker Compose 配置无语法错误

---

## ✅ 7. 数据库准备

### 7.1 数据库脚本
- [ ] `complete_database_schema_verified.sql` 文件存在
- [ ] 数据库脚本语法正确

### 7.2 数据库初始化（可选）
如果不想使用 Docker 自动初始化：
```bash
# 手动创建数据库
mysql -u root -p
CREATE DATABASE second_brain DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE second_brain;
SOURCE complete_database_schema_verified.sql;
```
- [ ] 数据库已创建（或使用 Docker 自动创建）

---

## ✅ 8. SSL 证书准备（如启用 HTTPS）

### 8.1 申请证书
#### 方法一：Let's Encrypt（免费）
```bash
sudo apt install -y certbot
certbot certonly --standalone -d your-domain.com
```
- [ ] 证书已申请
- [ ] 证书文件位置：
  - `/etc/letsencrypt/live/your-domain.com/fullchain.pem`
  - `/etc/letsencrypt/live/your-domain.com/privkey.pem`

#### 方法二：阿里云 SSL 证书
- [ ] 从阿里云控制台下载证书
- [ ] 证书格式转换为 PEM

### 8.2 复制证书到项目
```bash
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem ssl/cert.pem
cp /etc/letsencrypt/live/your-domain.com/privkey.pem ssl/key.pem
```
- [ ] 证书已复制到 `ssl/` 目录
- [ ] 证书文件权限正确（644）

---

## ✅ 9. 系统资源检查

### 9.1 内存检查
```bash
free -h
```
- [ ] 可用内存 ≥ 4GB

### 9.2 磁盘空间检查
```bash
df -h
```
- [ ] 可用磁盘空间 ≥ 20GB

### 9.3 CPU 检查
```bash
nproc
```
- [ ] CPU 核心数 ≥ 2

### 9.4 系统配置优化
```bash
# 增加文件描述符限制
ulimit -n 65536

# 增加虚拟内存（swap）
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```
- [ ] 系统参数已优化

---

## ✅ 10. 部署测试

### 10.1 执行部署
```bash
chmod +x deploy.sh
bash deploy.sh
```
- [ ] 部署脚本执行成功
- [ ] 所有服务启动成功

### 10.2 服务状态检查
```bash
docker-compose -f docker-compose.prod.yml ps
```
- [ ] 所有服务状态为 `Up`

### 10.3 功能验证
- [ ] 前端可以访问：`http://服务器 IP`
- [ ] API 文档可以访问：`http://服务器 IP/api/doc.html`
- [ ] 健康检查通过：`curl http://服务器 IP/api/actuator/health`
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] Elasticsearch 连接正常（如启用）

---

## ✅ 11. 日志和监控

### 11.1 日志配置
- [ ] 日志目录已创建：`logs/mysql`, `logs/backend`, `logs/nginx`
- [ ] 日志级别配置正确（INFO 或 WARN）

### 11.2 监控配置
- [ ] Actuator 端点可访问
- [ ] 健康检查配置正确
- [ ] 监控指标可获取

---

## ✅ 12. 备份策略

### 12.1 数据备份
```bash
# 创建备份脚本
cat > backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="backup/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"

# 备份 MySQL
docker exec ai-second-brain-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} ${MYSQL_DATABASE} > "$BACKUP_DIR/mysql_backup.sql"

# 备份 Redis
docker exec ai-second-brain-redis redis-cli -a ${REDIS_PASSWORD} BGSAVE
cp -r redis-data "$BACKUP_DIR/"

echo "备份完成：$BACKUP_DIR"
EOF
chmod +x backup.sh
```
- [ ] 备份脚本已创建
- [ ] 定时任务已配置（crontab）

### 12.2 日志轮转
- [ ] 日志文件大小限制已配置（100MB）
- [ ] 日志文件保留天数已配置（30 天）

---

## ✅ 13. 性能优化

### 13.1 JVM 优化
根据服务器内存调整 `.env.production` 中的 `JAVA_OPTS`：

- 4GB 内存：`JAVA_OPTS=-Xms512m -Xmx1024m`
- 8GB 内存：`JAVA_OPTS=-Xms1g -Xmx2g`
- 16GB 内存：`JAVA_OPTS=-Xms2g -Xmx4g`

- [ ] JVM 参数已根据内存调整

### 13.2 Elasticsearch 优化
根据服务器内存调整 `.env.production` 中的 `ES_JAVA_OPTS`：

- 4GB 内存：`ES_JAVA_OPTS=-Xms256m -Xmx512m`
- 8GB 内存：`ES_JAVA_OPTS=-Xms512m -Xmx1g`
- 16GB 内存：`ES_JAVA_OPTS=-Xms1g -Xmx2g`

- [ ] ES JVM 参数已调整

### 13.3 MySQL 优化
在 `docker-compose.prod.yml` 中调整：
```yaml
command: >
  --innodb-buffer-pool-size=512M
  --innodb-log-file-size=128M
  --max-connections=200
```
- [ ] MySQL 参数已优化

### 13.4 Redis 优化
在 `redis/redis.conf` 中调整：
```bash
maxmemory 256mb
maxmemory-policy allkeys-lru
```
- [ ] Redis 内存限制已配置

---

## ✅ 14. 文档准备

- [ ] `DEPLOYMENT.md` - 详细部署文档
- [ ] `QUICKSTART.md` - 快速开始指南
- [ ] `check_deployment.sh` - 检查脚本
- [ ] `deploy.sh` - 部署脚本
- [ ] `backup.sh` - 备份脚本（可选）

---

## ✅ 15. 应急计划

### 15.1 回滚方案
- [ ] 旧版本备份（如有）
- [ ] 数据库备份
- [ ] 回滚脚本已准备

### 15.2 故障恢复
- [ ] 故障排查文档已准备
- [ ] 紧急联系人已确定
- [ ] 监控告警已配置

---

## 📊 检查总结

### 完成情况统计
- 总检查项：15 大类
- 必须完成项：10 项（1-10）
- 推荐完成项：5 项（11-15）

### 最终验证
```bash
# 运行完整检查
bash check_deployment.sh

# 如果所有检查通过，执行部署
bash deploy.sh
```

---

## 🎯 部署后检查

部署完成后，请执行以下检查：

### 服务状态
- [ ] 所有容器运行正常
- [ ] CPU 和内存使用率正常
- [ ] 磁盘 I/O 正常

### 功能测试
- [ ] 用户注册/登录正常
- [ ] 对话导入功能正常
- [ ] 知识提取功能正常
- [ ] AI 问答功能正常
- [ ] 复习卡片功能正常

### 性能测试
- [ ] 页面加载时间 < 3 秒
- [ ] API 响应时间 < 1 秒
- [ ] 并发用户支持正常

### 安全检查
- [ ] HTTPS 正常工作（如启用）
- [ ] 防火墙规则生效
- [ ] 敏感信息未泄露

---

## 📞 获取帮助

如果检查过程中遇到问题：

1. 查看日志文件
2. 运行故障排查脚本
3. 查阅文档
4. 联系技术支持

---

**完成所有检查项后，即可安心部署到生产环境！** 🚀
