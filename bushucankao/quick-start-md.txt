# AI-SecondBrain 快速开始指南

## 🚀 5 分钟快速部署

### 前提条件
- 阿里云 ECS 服务器（Ubuntu 20.04 或 CentOS 7.9）
- 4GB+ 内存
- 2 核 + CPU
- 20GB+ 磁盘空间

### 步骤 1：安装 Docker（2 分钟）

#### Ubuntu
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo systemctl start docker
sudo systemctl enable docker
```

#### CentOS
```bash
yum install -y yum-utils
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io
systemctl start docker
systemctl enable docker
```

### 步骤 2：安装 Docker Compose（1 分钟）

```bash
curl -L "https://github.com/docker/compose/releases/download/v2.23.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

### 步骤 3：上传项目（1 分钟）

#### 方法一：Git 克隆
```bash
git clone https://github.com/your-username/ai-second-brain.git
cd ai-second-brain
```

#### 方法二：SCP 上传
```bash
# 本地执行
tar -czf ai-second-brain.tar.gz ai-second-brain
scp ai-second-brain.tar.gz root@服务器 IP:/root/

# 服务器执行
tar -xzf ai-second-brain.tar.gz
cd ai-second-brain
```

### 步骤 4：配置环境变量（1 分钟）

```bash
# 复制配置文件
cp .env.example .env.production

# 编辑配置（至少修改以下 3 项）
vim .env.production
```

**必须修改的配置**：
```bash
MYSQL_ROOT_PASSWORD=你的强密码（至少 16 位）
REDIS_PASSWORD=你的强密码（至少 16 位）
QWEN_API_KEY=你的通义千问 API 密钥
```

### 步骤 5：一键部署

```bash
# 赋予执行权限
chmod +x deploy.sh

# 执行部署
bash deploy.sh
```

等待 3-5 分钟，看到以下提示表示部署成功：

```
🎉 部署完成！
访问地址：http://你的服务器 IP
API 文档：http://你的服务器 IP/api/doc.html
```

---

## ✅ 验证部署

### 1. 检查服务状态

```bash
docker-compose -f docker-compose.prod.yml ps
```

应该看到所有服务都是 `Up` 状态：
- ✅ ai-second-brain-mysql
- ✅ ai-second-brain-redis
- ✅ ai-second-brain-kafka
- ✅ ai-second-brain-elasticsearch
- ✅ ai-second-brain-backend
- ✅ ai-second-brain-nginx

### 2. 访问前端

浏览器打开：`http://你的服务器 IP`

### 3. 访问 API 文档

浏览器打开：`http://你的服务器 IP/api/doc.html`

### 4. 健康检查

```bash
curl http://你的服务器 IP/api/actuator/health
```

应返回：`{"status":"UP"}`

---

## 🔧 常用操作

### 查看日志

```bash
# 查看所有服务日志
docker-compose -f docker-compose.prod.yml logs -f

# 查看后端日志
docker-compose -f docker-compose.prod.yml logs -f backend

# 查看数据库日志
docker-compose -f docker-compose.prod.yml logs -f mysql
```

### 重启服务

```bash
# 重启所有服务
docker-compose -f docker-compose.prod.yml restart

# 重启后端
docker-compose -f docker-compose.prod.yml restart backend
```

### 停止服务

```bash
docker-compose -f docker-compose.prod.yml down
```

### 启动服务

```bash
docker-compose -f docker-compose.prod.yml up -d
```

---

## 📝 下一步配置

### 1. 配置域名（可选）

编辑 `.env.production`：
```bash
DOMAIN_NAME=your-domain.com
ENABLE_HTTPS=true
```

### 2. 配置 SSL 证书（可选）

```bash
# 使用 Let's Encrypt 免费证书
apt install -y certbot
certbot certonly --standalone -d your-domain.com

# 复制证书到 ssl 目录
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem ssl/cert.pem
cp /etc/letsencrypt/live/your-domain.com/privkey.pem ssl/key.pem

# 重启 nginx
docker-compose -f docker-compose.prod.yml restart nginx
```

### 3. 配置邮件通知（可选）

编辑 `.env.production`：
```bash
MAIL_ENABLED=true
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your-email@qq.com
MAIL_PASSWORD=your-email-password
```

### 4. 配置阿里云 OSS（可选）

编辑 `.env.production`：
```bash
ALIYUN_OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
ALIYUN_OSS_ACCESS_KEY_ID=你的 AccessKey ID
ALIYUN_OSS_ACCESS_KEY_SECRET=你的 AccessKey Secret
ALIYUN_OSS_BUCKET_NAME=your-bucket-name
```

---

## ❓ 故障排查

### 问题 1：服务启动失败

```bash
# 查看错误日志
docker-compose -f docker-compose.prod.yml logs backend

# 检查配置
docker-compose -f docker-compose.prod.yml config
```

### 问题 2：无法访问前端

```bash
# 检查 nginx 状态
docker-compose -f docker-compose.prod.yml ps nginx

# 检查 nginx 日志
docker-compose -f docker-compose.prod.yml logs nginx
```

### 问题 3：数据库连接失败

```bash
# 等待 MySQL 完全启动（首次启动需要 1-2 分钟）
sleep 60

# 重启后端
docker-compose -f docker-compose.prod.yml restart backend
```

### 问题 4：内存不足

```bash
# 查看资源使用
docker stats

# 调整 JVM 内存（编辑 .env.production）
JAVA_OPTS=-Xms256m -Xmx512m

# 调整 ES 内存（编辑 .env.production）
ES_JAVA_OPTS=-Xms256m -Xmx512m

# 重启服务
docker-compose -f docker-compose.prod.yml restart
```

---

## 📊 系统要求

### 最低配置
- CPU：2 核
- 内存：4GB
- 磁盘：20GB

### 推荐配置
- CPU：4 核
- 内存：8GB
- 磁盘：40GB SSD

### 生产配置
- CPU：8 核
- 内存：16GB
- 磁盘：100GB SSD

---

## 🔒 安全建议

1. **修改默认密码**
   - MySQL root 密码
   - Redis 密码
   - 使用强密码（至少 16 位，包含大小写字母、数字、特殊字符）

2. **配置防火墙**
   ```bash
   # 只开放必要端口
   ufw allow 22/tcp    # SSH
   ufw allow 80/tcp    # HTTP
   ufw allow 443/tcp   # HTTPS
   ufw enable
   ```

3. **启用 HTTPS**
   - 使用 Let's Encrypt 免费证书
   - 配置自动续期

4. **定期备份**
   ```bash
   # 备份 MySQL
   docker exec ai-second-brain-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} ${MYSQL_DATABASE} > backup_$(date +%Y%m%d).sql
   ```

5. **监控日志**
   ```bash
   # 实时查看日志
   tail -f logs/backend/application.log
   ```

---

## 📞 获取帮助

### 检查脚本

```bash
# 运行配置检查
chmod +x check_deployment.sh
bash check_deployment.sh
```

### 查看文档

- 详细部署文档：[DEPLOYMENT.md](DEPLOYMENT.md)
- 系统优化指南：[SYSTEM_OPTIMIZATION_GUIDE.md](SYSTEM_OPTIMIZATION_GUIDE.md)
- 本地测试指南：[LOCAL_TESTING_GUIDE.md](LOCAL_TESTING_GUIDE.md)

### 技术支持

如遇到问题：
1. 查看日志文件
2. 运行检查脚本
3. 查看常见问题部分
4. 联系技术支持

---

## 🎉 部署完成！

现在你可以：
- ✅ 访问前端：`http://你的服务器 IP`
- ✅ 访问 API 文档：`http://你的服务器 IP/api/doc.html`
- ✅ 开始使用 AI 对话功能
- ✅ 导入对话记录
- ✅ 生成知识卡片
- ✅ 进行智能复习

**祝你使用愉快！** 🚀
