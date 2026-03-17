# AI-SecondBrain 从 1.0 升级到 2.0 指南

## 📋 升级说明

### 2.0 版本主要变化

#### 架构优化
- ✅ Docker 多阶段构建（镜像更小）
- ✅ 非 root 用户运行（更安全）
- ✅ 健康检查完善（更稳定）
- ✅ 日志管理优化（更易维护）

#### 配置优化
- ✅ 生产环境配置分离（application-prod.yml）
- ✅ 环境变量统一管理（.env.production）
- ✅ Redis 配置优化（持久化 + 内存管理）
- ✅ JVM 参数调优（G1GC）

#### 部署优化
- ✅ 自动化部署脚本
- ✅ 环境检查工具
- ✅ 一键升级能力
- ✅ 备份恢复支持

---

## 🚀 升级方案选择

### 方案一：完全重新部署（推荐）

**适用场景**：
- 1.0 和 2.0 差异较大
- 希望使用最新架构
- 不介意短暂停机

**优点**：
- ✅ 干净的环境
- ✅ 避免配置冲突
- ✅ 使用最新最佳实践

**缺点**：
- ⚠️ 需要短暂停机
- ⚠️ 需要备份数据

### 方案二：增量升级

**适用场景**：
- 只想更新部分服务
- 希望最小化停机时间

**优点**：
- ✅ 停机时间短
- ✅ 风险较低

**缺点**：
- ⚠️ 配置可能复杂
- ⚠️ 需要手动处理

---

## 🎯 方案一：完全重新部署（推荐）

### 步骤 1：备份数据（重要！）

```bash
# 1. 进入项目目录
cd /root/ai-second-brain

# 2. 创建备份目录
mkdir -p backup/v1_$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=backup/v1_$(date +%Y%m%d_%H%M%S)

# 3. 备份 MySQL 数据库
docker exec ai-second-brain-mysql mysqldump \
  -u root -p${MYSQL_ROOT_PASSWORD} \
  ${MYSQL_DATABASE} > ${BACKUP_DIR}/mysql_backup.sql

# 4. 备份 Redis 数据（可选）
docker exec ai-second-brain-redis redis-cli \
  -a ${REDIS_PASSWORD} BGSAVE
cp -r /var/lib/docker/volumes/ai-second-brain_redis-data/_data \
  ${BACKUP_DIR}/redis-data 2>/dev/null || true

# 5. 备份 Elasticsearch 数据（可选）
cp -r /var/lib/docker/volumes/ai-second-brain_elasticsearch-data/_data \
  ${BACKUP_DIR}/elasticsearch-data 2>/dev/null || true

# 6. 备份环境变量
cp .env ${BACKUP_DIR}/.env.backup 2>/dev/null || true
cp .env.production ${BACKUP_DIR}/.env.production.backup 2>/dev/null || true

echo "备份完成：${BACKUP_DIR}"
ls -lh ${BACKUP_DIR}/
```

### 步骤 2：停止旧服务

```bash
# 停止 1.0 版本的服务
docker-compose down

# 或者如果使用生产配置
docker-compose -f docker-compose.prod.yml down

# 确认所有容器已停止
docker ps -a | grep ai-second-brain
```

### 步骤 3：清理旧容器和镜像

```bash
# 停止所有相关容器
docker stop $(docker ps -aq | xargs docker inspect --format='{{range .Config.Labels}}{{.}}{{end}}' | grep -i second-brain || true)

# 删除所有相关容器
docker rm -f $(docker ps -aq | xargs docker inspect --format='{{range .Config.Labels}}{{.}}{{end}}' | grep -i second-brain || true) 2>/dev/null || true

# 删除旧镜像（可选，会重新构建）
docker rmi $(docker images | grep ai-second-brain | awk '{print $3}') 2>/dev/null || true

# 清理悬空镜像
docker image prune -f

echo "旧容器和镜像已清理"
```

### 步骤 4：更新代码

```bash
# 方法一：Git 拉取最新代码
git pull origin main

# 方法二：重新克隆
# cd /root
# rm -rf ai-second-brain
# git clone https://github.com/your-username/ai-second-brain.git
# cd ai-second-brain

# 方法三：SCP 上传新代码
# 在本地执行：
# scp -r ai-second-brain root@服务器 IP:/root/
```

### 步骤 5：迁移配置

```bash
# 1. 如果已有 .env.production，备份并创建新的
if [ -f ".env.production" ]; then
  cp .env.production .env.production.old
fi

# 2. 复制新的环境配置
cp .env.example .env.production

# 3. 从旧配置迁移关键变量（如果有备份）
if [ -f "backup/v1_*/.env.production.backup" ]; then
  echo "发现旧配置备份，开始迁移..."
  
  # 提取关键配置
  grep "MYSQL_ROOT_PASSWORD=" backup/v1_*/.env.production.backup | tail -1 >> .env.production
  grep "MYSQL_PASSWORD=" backup/v1_*/.env.production.backup | tail -1 >> .env.production
  grep "REDIS_PASSWORD=" backup/v1_*/.env.production.backup | tail -1 >> .env.production
  grep "QWEN_API_KEY=" backup/v1_*/.env.production.backup | tail -1 >> .env.production
  grep "DOMAIN_NAME=" backup/v1_*/.env.production.backup | tail -1 >> .env.production
  
  echo "配置迁移完成"
fi

# 4. 编辑配置（必须修改）
vim .env.production
```

**必须配置的项目**：
```bash
# 数据库密码（使用旧密码或新密码）
MYSQL_ROOT_PASSWORD=你的强密码
MYSQL_PASSWORD=你的强密码

# Redis 密码
REDIS_PASSWORD=你的强密码

# API 密钥
QWEN_API_KEY=你的 API 密钥

# 域名（可选）
DOMAIN_NAME=your-domain.com
```

### 步骤 6：运行环境检查

```bash
chmod +x check_deployment.sh
bash check_deployment.sh
```

**检查输出**：
- ✓ 确保所有检查项通过（无 FAIL）
- ⚠ 警告项可以忽略或修复

### 步骤 7：重新部署

```bash
# 执行部署
chmod +x deploy.sh
bash deploy.sh
```

**部署过程**：
1. 检查 Docker 环境
2. 检查配置文件
3. 创建必要目录
4. 构建新镜像
5. 启动新服务
6. 检查服务状态

等待 3-5 分钟，看到以下提示表示成功：
```
🎉 部署完成！
访问地址：http://你的服务器 IP
API 文档：http://你的服务器 IP/api/doc.html
```

### 步骤 8：恢复数据

```bash
# 1. 找到备份文件
BACKUP_FILE=$(ls -t backup/v1_*/mysql_backup.sql | head -1)
echo "使用备份文件：${BACKUP_FILE}"

# 2. 等待 MySQL 完全启动
sleep 30

# 3. 恢复数据库
docker exec -i ai-second-brain-mysql mysql \
  -u root -p${MYSQL_ROOT_PASSWORD} \
  ${MYSQL_DATABASE} < ${BACKUP_FILE}

echo "数据库恢复完成"

# 4. 验证数据
docker exec -it ai-second-brain-mysql mysql \
  -u root -p${MYSQL_ROOT_PASSWORD} \
  -e "USE ${MYSQL_DATABASE}; SELECT COUNT(*) FROM user;"
```

### 步骤 9：验证升级

```bash
# 1. 检查所有服务状态
docker-compose -f docker-compose.prod.yml ps

# 应该看到所有服务都是 Up 状态

# 2. 查看后端日志
docker-compose -f docker-compose.prod.yml logs backend | tail -50

# 3. 访问前端
# 浏览器打开：http://你的服务器 IP

# 4. 访问 API 文档
# 浏览器打开：http://你的服务器 IP/api/doc.html

# 5. 健康检查
curl http://你的服务器 IP/api/actuator/health

# 6. 测试功能
# - 用户登录
# - 对话导入
# - 知识管理
```

---

## 🔄 方案二：增量升级

### 步骤 1：备份数据

与方案一相同，先备份所有数据。

### 步骤 2：更新配置文件

```bash
# 1. 备份旧配置
cp docker-compose.yml docker-compose.yml.backup
cp .env .env.backup

# 2. 复制新配置
cp docker-compose.prod.yml docker-compose.yml.new

# 3. 对比配置差异
diff docker-compose.yml.backup docker-compose.yml.new
```

### 步骤 3：逐个更新服务

```bash
# 1. 更新后端服务
docker-compose build backend
docker-compose up -d backend

# 等待后端启动
sleep 30

# 2. 更新前端服务
docker-compose build frontend-builder
docker-compose restart nginx

# 3. 更新其他服务（按需）
docker-compose up -d mysql redis elasticsearch kafka
```

### 步骤 4：验证功能

测试关键功能是否正常。

---

## ⚠️ 常见问题处理

### 问题 1：数据库表结构不兼容

**症状**：启动后报错表不存在或字段错误

**解决方案**：
```bash
# 1. 备份当前数据
docker exec ai-second-brain-mysql mysqldump \
  -u root -p${MYSQL_ROOT_PASSWORD} \
  ${MYSQL_DATABASE} > current_backup.sql

# 2. 使用新的表结构
docker exec -i ai-second-brain-mysql mysql \
  -u root -p${MYSQL_ROOT_PASSWORD} \
  < complete_database_schema_verified.sql

# 3. 恢复数据（只恢复数据，不恢复表结构）
# 手动导入需要的数据
```

### 问题 2：端口冲突

**症状**：服务启动失败，提示端口被占用

**解决方案**：
```bash
# 1. 查看端口占用
netstat -tulpn | grep :8080
netstat -tulpn | grep :3306
netstat -tulpn | grep :6379

# 2. 停止占用端口的旧容器
docker ps -a | grep ai-second-brain
docker stop <容器 ID>
docker rm <容器 ID>

# 3. 或者修改端口（编辑 .env.production）
BACKEND_PORT=8081
MYSQL_PORT=3307
REDIS_PORT=6380
```

### 问题 3：镜像构建失败

**症状**：Docker 构建报错

**解决方案**：
```bash
# 1. 清理 Docker 缓存
docker system prune -a -f

# 2. 重新构建
docker-compose -f docker-compose.prod.yml build --no-cache

# 3. 检查 Docker 版本
docker --version
docker-compose --version

# 4. 更新 Docker（如果需要）
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

### 问题 4：环境变量未生效

**症状**：服务使用默认配置

**解决方案**：
```bash
# 1. 检查 .env.production 文件
cat .env.production | grep -v "^#" | grep -v "^$"

# 2. 重新加载环境变量
set -a
source .env.production
set +a

# 3. 重启服务
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d
```

### 问题 5：数据丢失

**症状**：升级后发现数据不见了

**解决方案**：
```bash
# 1. 立即停止服务
docker-compose -f docker-compose.prod.yml down

# 2. 检查数据卷
docker volume ls | grep ai-second-brain

# 3. 从备份恢复
BACKUP_FILE=backup/v1_*/mysql_backup.sql
docker exec -i ai-second-brain-mysql mysql \
  -u root -p${MYSQL_ROOT_PASSWORD} \
  ${MYSQL_DATABASE} < ${BACKUP_FILE}

# 4. 重启服务
docker-compose -f docker-compose.prod.yml up -d
```

---

## 📊 升级检查清单

### 升级前
- [ ] 备份所有数据（MySQL、Redis、Elasticsearch）
- [ ] 备份环境变量（.env、.env.production）
- [ ] 记录当前配置（端口、密码等）
- [ ] 通知用户即将停机升级
- [ ] 准备回滚方案

### 升级中
- [ ] 停止旧服务
- [ ] 清理旧容器
- [ ] 更新代码
- [ ] 迁移配置
- [ ] 运行环境检查
- [ ] 执行部署脚本
- [ ] 恢复数据

### 升级后
- [ ] 检查所有服务状态（docker-compose ps）
- [ ] 查看后端日志（无 ERROR）
- [ ] 访问前端页面
- [ ] 访问 API 文档
- [ ] 执行健康检查
- [ ] 测试核心功能
- [ ] 验证数据完整性
- [ ] 通知用户升级完成

---

## 🎯 推荐升级流程

**对于 1.0 到 2.0 的升级，强烈推荐使用方案一（完全重新部署）**：

```bash
# 完整升级命令（复制执行）

# 1. 备份数据
cd /root/ai-second-brain
mkdir -p backup/v2_upgrade_$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=backup/v2_upgrade_$(date +%Y%m%d_%H%M%S)
docker exec ai-second-brain-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} ${MYSQL_DATABASE} > ${BACKUP_DIR}/mysql_backup.sql
cp .env ${BACKUP_DIR}/ 2>/dev/null || true
cp .env.production ${BACKUP_DIR}/ 2>/dev/null || true
echo "✓ 数据备份完成：${BACKUP_DIR}"

# 2. 停止旧服务
docker-compose -f docker-compose.prod.yml down
echo "✓ 旧服务已停止"

# 3. 更新代码
git pull origin main
echo "✓ 代码已更新"

# 4. 配置环境
cp .env.example .env.production
vim .env.production  # 编辑配置
echo "✓ 配置已更新"

# 5. 运行检查
bash check_deployment.sh
echo "✓ 环境检查完成"

# 6. 执行部署
bash deploy.sh
echo "✓ 部署完成"

# 7. 恢复数据
BACKUP_FILE=$(ls -t backup/v2_upgrade_*/mysql_backup.sql | head -1)
docker exec -i ai-second-brain-mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} ${MYSQL_DATABASE} < ${BACKUP_FILE}
echo "✓ 数据恢复完成"

# 8. 验证
docker-compose -f docker-compose.prod.yml ps
echo "✓ 升级完成！"
```

---

## 📞 升级支持

### 日志查看
```bash
# 查看升级日志
docker-compose -f docker-compose.prod.yml logs backend | tail -100

# 实时查看日志
docker-compose -f docker-compose.prod.yml logs -f
```

### 故障排查
```bash
# 运行诊断
bash check_deployment.sh

# 查看 Docker 状态
docker stats

# 查看磁盘使用
docker system df
```

### 回滚操作
```bash
# 如果需要回滚到 1.0
cd /root/ai-second-brain
git checkout <1.0 版本号>
docker-compose -f docker-compose.yml up -d
```

---

## ✅ 升级完成确认

升级成功后，您应该看到：

### 服务状态
```
NAME                          STATUS
ai-second-brain-mysql         Up (healthy)
ai-second-brain-redis         Up (healthy)
ai-second-brain-kafka         Up (healthy)
ai-second-brain-elasticsearch Up (healthy)
ai-second-brain-backend       Up (healthy)
ai-second-brain-nginx         Up (healthy)
```

### 功能测试
- ✅ 前端页面正常访问
- ✅ API 文档正常访问
- ✅ 用户登录正常
- ✅ 数据完整无丢失
- ✅ 核心功能正常

### 性能提升
- ✅ 启动速度更快
- ✅ 内存使用更优
- ✅ 日志管理更完善
- ✅ 健康检查更可靠

---

**升级过程中如有任何问题，请查看日志或联系技术支持！** 🚀

**预计升级时间**：10-15 分钟
**预计停机时间**：5-10 分钟

**祝升级顺利！** 🎉
