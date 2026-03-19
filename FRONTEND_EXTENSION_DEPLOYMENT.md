# 前端和插件部署说明

## 📦 前端部署

### 方法一：使用 Docker（推荐）

前端**有** Dockerfile，位于 `frontend/Dockerfile`。

#### 1. 单独构建前端镜像

```bash
cd frontend
docker build -t ai-secondbrain-frontend .
```

#### 2. 使用 Docker Compose 启动

```bash
# 在项目根目录
docker-compose up -d frontend
```

#### 3. 访问前端

- **开发环境**：http://localhost:5173
- **生产环境（Docker）**：http://localhost

### 方法二：本地开发

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

---

## 🔌 浏览器插件使用

### 当前状态

插件**已经配置为本地开发环境**，可以直接使用！

**已完成的配置**：

1. ✅ `background.js` - API 地址已设置为 `http://localhost:8080/api`
2. ✅ `manifest.json` - 已添加本地权限 `http://localhost:8080/*`
3. ✅ 开启调试模式 `DEBUG = true`

### 使用步骤

#### 1. 启动后端服务

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**验证**：访问 http://localhost:8080/api/doc.html

#### 2. 加载插件

1. 打开 Chrome 浏览器
2. 访问 `chrome://extensions/`
3. 开启"开发者模式"
4. 点击"加载已解压的扩展程序"
5. 选择 `extension/` 文件夹

#### 3. 使用插件

1. 访问 AI 平台（如 https://chatgpt.com）
2. 进行对话
3. 点击插件图标
4. 点击"保存到知识库"

#### 4. 验证结果

- 查看后端日志：`POST /api/chat/save 200 OK`
- 访问前端：http://localhost:5173
- 登录后查看保存的对话

---

## 🎯 不同环境配置

### 本地开发环境（当前配置）

**插件配置**：
```javascript
// extension/background.js
let API_BASE_URL = "http://localhost:8080/api";
let DEBUG = true;
```

**使用场景**：
- ✅ 本地开发和调试
- ✅ 功能测试
- ✅ 快速验证

**优点**：
- ✅ 配置简单
- ✅ 调试方便
- ✅ 响应快速

**缺点**：
- ❌ 需要手动启动服务
- ❌ 只能本机访问

### Docker 部署环境

**启动命令**：
```bash
# 启动所有服务
docker-compose up -d

# 查看状态
docker-compose ps
```

**访问地址**：
- 前端：http://localhost
- 后端：http://localhost:8080
- API 文档：http://localhost:8080/api/doc.html

**使用场景**：
- ✅ 团队协作
- ✅ 稳定测试
- ✅ 演示展示

**优点**：
- ✅ 一键启动
- ✅ 环境一致
- ✅ 易于管理

**缺点**：
- ❌ 资源占用较多
- ❌ 调试相对复杂

### 生产环境部署

**插件配置**：
```javascript
// extension/background.js
let API_BASE_URL = "https://aisecondbrain.cn/api";
let DEBUG = false;
```

**使用场景**：
- ✅ 正式使用
- ✅ 多人协作
- ✅ 对外服务

**优点**：
- ✅ 性能优化
- ✅ 可多人访问
- ✅ 数据持久化

**缺点**：
- ❌ 需要服务器
- ❌ 需要域名和证书

---

## 📝 插件在不同环境的使用

### 本地开发环境

**配置**：
- 后端运行在：http://localhost:8080
- 插件已配置为访问本地地址
- 可以直接使用

**步骤**：
1. 启动后端：`mvn spring-boot:run`
2. 加载插件：`chrome://extensions/`
3. 使用插件保存对话

### 测试环境

**配置**：
```javascript
// 修改 background.js
let API_BASE_URL = "http://test-server:8080/api";
let DEBUG = true;
```

**步骤**：
1. 修改插件配置
2. 重新加载插件
3. 连接到测试服务器

### 生产环境

**配置**：
```javascript
// 修改 background.js
let API_BASE_URL = "https://aisecondbrain.cn/api";
let DEBUG = false;
```

**步骤**：
1. 修改插件配置
2. 重新加载插件
3. 连接到生产服务器

---

## 🔧 插件快速切换环境

### 方法一：修改源代码

**切换到本地开发**：
```javascript
// extension/background.js
let API_BASE_URL = "http://localhost:8080/api";
let DEBUG = true;
```

**切换到生产环境**：
```javascript
// extension/background.js
let API_BASE_URL = "https://aisecondbrain.cn/api";
let DEBUG = false;
```

**重新加载插件**：
1. `chrome://extensions/`
2. 点击刷新按钮

### 方法二：使用构建脚本

**构建本地版本**：
```bash
cd extension
build.bat  # Windows
# 或
./build.sh  # Linux/macOS
```

**构建生产版本**：
```bash
# 修改配置后运行构建脚本
```

---

## ✅ 常见问题

### Q1: 前端 Dockerfile 在哪里？

**A**: 在 `frontend/Dockerfile`，用于构建前端生产环境镜像。

### Q2: 插件可以直接使用吗？

**A**: 可以！插件已经配置为本地开发环境：
- ✅ API 地址：`http://localhost:8080/api`
- ✅ 权限配置：包含 `localhost:8080`
- ✅ 调试模式：已开启

### Q3: 如何切换插件到生产环境？

**A**: 修改 `extension/background.js`：
```javascript
let API_BASE_URL = "https://aisecondbrain.cn/api";
let DEBUG = false;
```

### Q4: Docker 部署后插件无法访问？

**A**: 确保插件配置为正确的后端地址：
- Docker 部署：`http://localhost:8080/api`
- 服务器部署：`https://your-domain.com/api`

### Q5: 前端和后端都需要用 Docker 吗？

**A**: 不是必须：
- **开发环境**：可以只使用 Docker 启动数据库和缓存
- **生产环境**：建议使用 Docker 部署所有服务

---

## 📚 相关文档

- [README.md](README.md) - 项目主文档
- [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) - 开发环境搭建
- [EXTENSION_CONFIGURATION_GUIDE.md](EXTENSION_CONFIGURATION_GUIDE.md) - 插件配置指南
- [extension/README.md](extension/README.md) - 插件本地开发指南

---

## 🎯 总结

### 前端部署

- ✅ **有 Dockerfile**：`frontend/Dockerfile`
- ✅ **支持 Docker 部署**：`docker-compose up -d frontend`
- ✅ **支持本地开发**：`npm run dev`

### 插件使用

- ✅ **可以直接使用**：已配置本地开发环境
- ✅ **支持多环境切换**：修改配置即可切换
- ✅ **开箱即用**：加载即可使用

### 推荐方案

**本地开发**：
1. Docker 启动数据库和缓存
2. 本地运行后端（IDE 或 Maven）
3. 本地运行前端（npm run dev）
4. 加载插件到浏览器

**生产部署**：
1. Docker Compose 启动所有服务
2. 配置域名和 HTTPS
3. 修改插件配置为生产地址
4. 分发给用户使用

---

**最后更新**：2026-03-19  
**版本**：V1.0
