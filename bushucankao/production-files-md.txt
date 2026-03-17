# AI-SecondBrain 生产环境文件结构

## 📁 完整文件列表

### 根目录文件
```
AI-SecondBrain/
├── .env.example                      # 环境变量示例
├── .env.production                   # 生产环境变量（需创建，不提交到 Git）
├── .gitignore                        # Git 忽略文件配置
├── docker-compose.yml                # 开发环境 Docker 编排
├── docker-compose.prod.yml           # 生产环境 Docker 编排 ✓
├── Dockerfile                        # 后端 Docker 构建文件 ✓
├── deploy.sh                         # Linux/Mac 部署脚本 ✓
├── deploy.bat                        # Windows 部署脚本 ✓
├── check_deployment.sh               # 部署检查脚本 ✓
├── DEPLOYMENT.md                     # 详细部署文档 ✓
├── QUICKSTART.md                     # 快速开始指南 ✓
├── PRE_DEPLOYMENT_CHECKLIST.md       # 部署前检查清单 ✓
├── README.md                         # 项目说明文档
├── pom.xml                           # Maven 项目配置
└── complete_database_schema_verified.sql  # 数据库表结构 ✓
```

### 后端目录
```
src/
├── main/
│   ├── java/
│   │   └── com/secondbrain/
│   │       ├── AiSecondBrainApplication.java
│   │       ├── config/               # 配置类
│   │       ├── controller/           # 控制器
│   │       ├── service/              # 服务层
│   │       ├── repository/           # 数据访问层
│   │       ├── entity/               # 实体类
│   │       ├── dto/                  # 数据传输对象
│   │       └── util/                 # 工具类
│   └── resources/
│       ├── application.yml           # 主配置文件
│       ├── application-prod.yml      # 生产环境配置 ✓
│       ├── application-local.yml     # 本地环境配置（可选）
│       ├── mapper/                   # MyBatis 映射文件
│       └── sql/                      # SQL 脚本
└── test/                             # 测试代码
```

### 前端目录
```
frontend/
├── Dockerfile                        # 前端 Docker 构建文件 ✓
├── nginx.conf                        # 前端 Nginx 配置 ✓
├── package.json                      # Node.js 依赖配置
├── vite.config.js                    # Vite 构建配置
├── index.html                        # HTML 入口
├── public/                           # 静态资源
└── src/
    ├── main.js                       # Vue 入口
    ├── App.vue                       # 根组件
    ├── views/                        # 页面组件
    ├── components/                   # 组件
    ├── router/                       # 路由配置
    ├── store/                        # 状态管理
    ├── api/                          # API 调用
    └── utils/                        # 工具函数
```

### Nginx 目录
```
nginx/
├── nginx.conf                        # Nginx 主配置 ✓
└── conf.d/
    └── default.conf                  # Nginx 虚拟主机配置 ✓
```

### Redis 目录
```
redis/
└── redis.conf                        # Redis 生产环境配置 ✓
```

### SSL 目录（不提交到 Git）
```
ssl/
├── cert.pem                          # SSL 证书
└── key.pem                          # SSL 私钥
```

### 日志目录（不提交到 Git）
```
logs/
├── mysql/                           # MySQL 日志
├── backend/                         # 后端日志
└── nginx/                           # Nginx 日志
```

### DeerFlow 目录
```
deerflow/
├── Dockerfile                        # DeerFlow Docker 构建文件
├── requirements.txt                  # Python 依赖
├── api_server.py                     # API 服务
└── ...                               # 其他 DeerFlow 文件
```

---

## ✅ 生产环境必需文件

### 核心配置文件
- [x] `docker-compose.prod.yml` - 生产环境 Docker 编排
- [x] `.env.production` - 生产环境变量（需手动创建）
- [x] `Dockerfile` - 后端 Docker 构建
- [x] `frontend/Dockerfile` - 前端 Docker 构建
- [x] `src/main/resources/application-prod.yml` - 生产环境应用配置

### Nginx 配置
- [x] `nginx/nginx.conf` - Nginx 主配置
- [x] `nginx/conf.d/default.conf` - Nginx 虚拟主机配置

### Redis 配置
- [x] `redis/redis.conf` - Redis 生产配置

### 部署脚本
- [x] `deploy.sh` - Linux/Mac 部署脚本
- [x] `deploy.bat` - Windows 部署脚本
- [x] `check_deployment.sh` - 部署检查脚本

### 文档
- [x] `DEPLOYMENT.md` - 详细部署文档
- [x] `QUICKSTART.md` - 快速开始指南
- [x] `PRE_DEPLOYMENT_CHECKLIST.md` - 部署前检查清单

### 数据库
- [x] `complete_database_schema_verified.sql` - 验证过的数据库表结构

---

## 📝 文件说明

### 1. docker-compose.prod.yml
生产环境 Docker 编排文件，包含：
- MySQL 8.0 数据库
- Redis 7 缓存
- Zookeeper + Kafka 消息队列
- Elasticsearch 8.11 搜索引擎
- DeerFlow AI 服务
- Spring Boot 后端
- Vue.js 前端（构建阶段）
- Nginx Web 服务器

### 2. .env.production
生产环境变量文件（需手动创建），包含：
- 数据库密码
- Redis 密码
- API 密钥
- 域名配置
- JVM 参数
- 其他敏感信息

**注意**：此文件包含敏感信息，不应提交到 Git

### 3. application-prod.yml
Spring Boot 生产环境配置，包含：
- 数据库连接池配置
- Redis 连接配置
- Kafka 配置
- Elasticsearch 配置
- 日志配置
- 监控端点配置

### 4. Dockerfile（后端）
后端 Docker 构建文件，特点：
- 多阶段构建（减小镜像大小）
- 使用 OpenJDK 21 JRE
- 非 root 用户运行（安全）
- JVM 参数优化
- 健康检查配置

### 5. Dockerfile（前端）
前端 Docker 构建文件，特点：
- 多阶段构建
- Node.js 18 构建
- Nginx Alpine 运行
- Gzip 压缩
- 静态资源缓存

### 6. nginx.conf
Nginx 主配置，包含：
- Gzip 压缩
- 日志格式
- 性能优化
- MIME 类型

### 7. default.conf
Nginx 虚拟主机配置，包含：
- 前端静态文件服务
- 后端 API 代理
- WebSocket 支持
- HTTPS 配置（可选）
- 健康检查端点

### 8. redis.conf
Redis 生产配置，包含：
- 密码认证
- 持久化配置（RDB + AOF）
- 内存管理
- 慢查询日志
- 性能优化

### 9. deploy.sh
自动化部署脚本，功能：
- 检查 Docker 环境
- 检查配置文件
- 创建必要目录
- 停止旧服务
- 清理旧容器
- 构建新镜像
- 启动新服务
- 检查服务状态

### 10. check_deployment.sh
部署前检查脚本，检查：
- Docker 环境
- 配置文件
- 目录结构
- 安全配置
- 系统资源
- 端口占用
- 服务状态

---

## 🔒 安全文件（不提交到 Git）

以下文件已添加到 `.gitignore`，不应提交到代码仓库：

### 环境变量
- `.env`
- `.env.production`
- `.env.local`
- `.env.*.local`

### SSL 证书
- `ssl/cert.pem`
- `ssl/key.pem`
- `ssl/*.pem`

### 日志文件
- `logs/`
- `*.log`

### 构建产物
- `target/`
- `frontend/dist/`
- `frontend/node_modules/`

### 其他敏感文件
- `*.key`
- `*.crt`
- `*.p12`
- `*.jks`

---

## 📊 文件大小估算

### Docker 镜像大小（估算）
- 后端镜像：~300MB
- 前端镜像：~50MB
- MySQL 镜像：~500MB
- Redis 镜像：~30MB
- Elasticsearch 镜像：~700MB
- Kafka 镜像：~400MB
- Nginx 镜像：~25MB

**总镜像大小**：~2GB

### 磁盘空间需求
- Docker 镜像：~2GB
- Docker 数据卷：~5GB（初始）
- 日志文件：~500MB
- 项目文件：~200MB

**总磁盘需求**：~8GB（初始）

**建议预留**：20GB+ 可用空间

---

## 🎯 文件完整性检查

### 使用检查脚本
```bash
# 运行文件结构检查
find . -type f -name "*.yml" | head -20
find . -type f -name "Dockerfile" | head -10
find . -type f -name "*.sh" | head -10
```

### 手动检查
```bash
# 检查必需文件
ls -la docker-compose.prod.yml
ls -la Dockerfile
ls -la frontend/Dockerfile
ls -la deploy.sh
ls -la check_deployment.sh
ls -la src/main/resources/application-prod.yml
ls -la nginx/nginx.conf
ls -la nginx/conf.d/default.conf
ls -la redis/redis.conf
```

---

## 📞 文件相关的问题

### 如果文件缺失
1. 检查 Git 仓库是否完整克隆
2. 查看 `.gitignore` 是否误删
3. 从备份恢复
4. 联系项目管理员

### 如果文件损坏
1. 从 Git 恢复：`git checkout <文件路径>`
2. 从备份恢复
3. 重新创建文件

### 如果需要更新
1. 从主分支拉取：`git pull origin main`
2. 合并冲突（如有）
3. 重新部署

---

## ✅ 检查清单

在部署前，请确保以下文件存在且正确：

- [ ] `docker-compose.prod.yml`
- [ ] `.env.production`（需手动创建）
- [ ] `Dockerfile`
- [ ] `frontend/Dockerfile`
- [ ] `src/main/resources/application-prod.yml`
- [ ] `nginx/nginx.conf`
- [ ] `nginx/conf.d/default.conf`
- [ ] `redis/redis.conf`
- [ ] `deploy.sh`
- [ ] `check_deployment.sh`
- [ ] `complete_database_schema_verified.sql`
- [ ] `DEPLOYMENT.md`
- [ ] `QUICKSTART.md`

---

**所有文件就绪后，即可开始部署！** 🚀
