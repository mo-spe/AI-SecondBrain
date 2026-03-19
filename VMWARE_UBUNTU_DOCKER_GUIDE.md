# VMware Ubuntu Docker 使用指南

## 📋 您的环境

- **主机系统**：Windows
- **虚拟机**：VMware + Ubuntu
- **Docker**：安装在 Ubuntu 上

---

## ✅ 可以使用，但需要配置

### 方案一：纯本地开发（推荐）

**不使用 Docker**，直接在 Windows 上运行所有服务。

#### 1. 安装必要软件

```bash
# Windows 上安装
# 1. Java 21: https://adoptium.net/
# 2. MySQL 8.0: https://dev.mysql.com/downloads/
# 3. Redis: https://github.com/microsoftarchive/redis/releases
# 4. Node.js 18+: https://nodejs.org/
# 5. Python 3.10+: https://www.python.org/
```

#### 2. 启动服务

```bash
# 1. 启动 MySQL（Windows 服务）
# 2. 启动 Redis（Windows 服务）

# 3. 启动后端（Windows）
cd backend
mvn spring-boot:run

# 4. 启动前端（Windows）
cd frontend
npm install
npm run dev

# 5. 启动 DeerFlow（Windows）
cd deerflow
pip install -r requirements.txt
python app.py

# 6. 加载插件（浏览器）
# chrome://extensions/ -> 加载 extension/
```

**优点**：
- ✅ 无需配置网络
- ✅ 调试方便
- ✅ 性能最好

**缺点**：
- ❌ 需要安装多个软件
- ❌ Windows 和 Linux 环境可能有差异

---

### 方案二：使用虚拟机 Docker（推荐）

**在 Ubuntu 虚拟机上运行 Docker，Windows 访问**。

#### 1. 配置 Ubuntu 网络

**VMware 网络设置**：
1. 打开 VMware
2. 右键 Ubuntu 虚拟机 → 设置
3. 网络适配器 → 选择"桥接模式"或"NAT 模式"

**推荐：桥接模式**
- Ubuntu 会获得独立的 IP 地址
- Windows 可以直接访问

**查看 Ubuntu IP**：
```bash
# 在 Ubuntu 终端执行
ip addr show
# 或
hostname -I
```

假设 Ubuntu IP 为：`192.168.1.100`

#### 2. 修改 Docker Compose 配置

编辑 `.env` 文件（在 Windows 上）：

```bash
# 数据库配置
MYSQL_HOST=192.168.1.100
MYSQL_PORT=3306

# Redis 配置
REDIS_HOST=192.168.1.100
REDIS_PORT=6379

# 后端配置
BACKEND_HOST=192.168.1.100
BACKEND_PORT=8080

# 前端配置
FRONTEND_HOST=192.168.1.100
FRONTEND_PORT=80
```

#### 3. 在 Ubuntu 上启动 Docker

```bash
# SSH 连接到 Ubuntu（从 Windows）
ssh your_username@192.168.1.100

# 或在 Ubuntu 虚拟机终端执行
cd /path/to/AI-SecondBrain

# 启动所有服务
docker-compose up -d

# 查看状态
docker-compose ps
```

#### 4. 在 Windows 上访问

```bash
# 访问前端
http://192.168.1.100

# 访问后端 API
http://192.168.1.100:8080

# 访问 API 文档
http://192.168.1.100:8080/api/doc.html
```

#### 5. 配置插件

修改 `extension/background.js`：

```javascript
// 改为 Ubuntu 的 IP 地址
let API_BASE_URL = "http://192.168.1.100:8080/api";
let DEBUG = true;
```

修改 `extension/manifest.json`：

```json
{
  "host_permissions": [
    "http://192.168.1.100:8080/*",
    "http://localhost:8080/*",
    // ... 其他域名
  ]
}
```

**优点**：
- ✅ 使用 Docker 的便利性
- ✅ 环境一致（生产环境也是 Linux）
- ✅ 不需要在 Windows 安装太多软件

**缺点**：
- ❌ 需要配置网络
- ❌ 依赖虚拟机的稳定性

---

### 方案三：混合模式

**数据库和缓存用 Docker，后端前端在 Windows**。

#### 1. 在 Ubuntu 上只启动基础服务

```bash
# SSH 连接到 Ubuntu
ssh your_username@192.168.1.100

# 只启动 MySQL 和 Redis
docker-compose up -d mysql redis
```

#### 2. 在 Windows 上配置连接

编辑 `.env` 文件：

```bash
# 连接到 Ubuntu 上的 Docker 服务
MYSQL_HOST=192.168.1.100
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=your_password

REDIS_HOST=192.168.1.100
REDIS_PORT=6379
REDIS_PASSWORD=your_password
```

#### 3. 在 Windows 上运行后端和前端

```bash
# Windows PowerShell 或 CMD

# 启动后端
cd backend
mvn spring-boot:run

# 启动前端
cd frontend
npm run dev
```

**优点**：
- ✅ 数据库用 Docker（环境一致）
- ✅ 后端前端本地运行（调试方便）
- ✅ 灵活配置

**缺点**：
- ❌ 需要配置网络
- ❌ 配置相对复杂

---

## 🔧 网络配置详解

### VMware 网络模式

#### 1. 桥接模式（推荐）

**配置**：
- VMware 设置 → Network Adapter → Bridged

**特点**：
- Ubuntu 获得独立 IP（与 Windows 同网段）
- Windows 可以直接访问
- 外部网络也可以访问

**示例**：
```
Windows IP:     192.168.1.50
Ubuntu IP:      192.168.1.100
网关：          192.168.1.1
```

#### 2. NAT 模式

**配置**：
- VMware 设置 → Network Adapter → NAT

**特点**：
- Ubuntu 在虚拟网络中
- Windows 可以访问 Ubuntu
- 外部网络无法访问

**示例**：
```
Windows IP:     192.168.1.50 (物理网络)
Ubuntu IP:      192.168.152.128 (虚拟网络)
```

#### 3. Host-Only 模式

**配置**：
- VMware 设置 → Network Adapter → Host-Only

**特点**：
- 只有 Windows 和 Ubuntu 可以互访
- 无法访问外网
- 最安全

---

## 📝 具体配置步骤

### 步骤 1：设置桥接网络

1. 打开 VMware Workstation
2. 右键 Ubuntu 虚拟机 → 设置
3. 网络适配器 → 桥接模式
4. 确定保存

### 步骤 2：获取 Ubuntu IP

```bash
# 在 Ubuntu 终端
ip addr show eth0
# 或
hostname -I
```

记录 IP 地址，例如：`192.168.1.100`

### 步骤 3：测试连接

```bash
# 在 Windows PowerShell 测试
ping 192.168.1.100

# 测试端口（如果安装了 telnet）
telnet 192.168.1.100 3306
```

### 步骤 4：配置防火墙（如果需要）

```bash
# 在 Ubuntu 上
sudo ufw allow 3306/tcp    # MySQL
sudo ufw allow 6379/tcp    # Redis
sudo ufw allow 8080/tcp    # 后端
sudo ufw allow 80/tcp      # 前端
```

### 步骤 5：修改项目配置

编辑 `.env` 文件：

```bash
# 所有 HOST 都改为 Ubuntu IP
MYSQL_HOST=192.168.1.100
REDIS_HOST=192.168.1.100
BACKEND_HOST=192.168.1.100
FRONTEND_HOST=192.168.1.100
```

### 步骤 6：启动服务

```bash
# 在 Ubuntu 上
cd /path/to/AI-SecondBrain
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### 步骤 7：访问应用

```bash
# 在 Windows 浏览器访问
http://192.168.1.100           # 前端
http://192.168.1.100:8080      # 后端 API
http://192.168.1.100:8080/api/doc.html  # API 文档
```

---

## 🔍 常见问题

### Q1: Windows 无法 ping 通 Ubuntu？

**A**: 检查网络模式：
1. VMware 设置为桥接模式
2. 检查 Ubuntu 防火墙：`sudo ufw status`
3. 临时关闭防火墙测试：`sudo ufw disable`

### Q2: Docker 服务无法访问？

**A**: 检查端口是否开放：
```bash
# Ubuntu 上检查
sudo netstat -tlnp | grep 3306
sudo netstat -tlnp | grep 6379
sudo netstat -tlnp | grep 8080

# Windows 上检查
telnet 192.168.1.100 3306
```

### Q3: 插件无法连接后端？

**A**: 修改插件配置：
```javascript
// extension/background.js
let API_BASE_URL = "http://192.168.1.100:8080/api";

// extension/manifest.json
"host_permissions": [
  "http://192.168.1.100:8080/*"
]
```

### Q4: 性能会不会很差？

**A**: 
- **桥接模式**：性能接近原生（推荐）
- **NAT 模式**：有轻微损耗
- **纯本地**：性能最好

### Q5: 每次重启 IP 都变怎么办？

**A**: 设置静态 IP：
```bash
# Ubuntu 上编辑网络配置
sudo nano /etc/netplan/01-netcfg.yaml

# 添加静态 IP 配置
network:
  version: 2
  renderer: networkd
  ethernets:
    ens33:
      addresses:
        - 192.168.1.100/24
      gateway4: 192.168.1.1
      nameservers:
        addresses: [8.8.8.8, 8.8.4.4]

# 应用配置
sudo netplan apply
```

---

## 🎯 推荐方案

### 开发环境

**推荐：方案二（虚拟机 Docker）**

```
Windows (主机)
  ├── 浏览器 + 插件
  ├── IDE (IntelliJ IDEA / VS Code)
  └── 访问 http://192.168.1.100

Ubuntu (VMware 虚拟机)
  ├── Docker
  │   ├── MySQL
  │   ├── Redis
  │   ├── Elasticsearch
  │   ├── 后端 (Spring Boot)
  │   └── 前端 (Nginx)
  └── DeerFlow (Python)
```

**配置步骤**：
1. VMware 设置桥接网络
2. 获取 Ubuntu IP
3. 修改 `.env` 配置
4. Ubuntu 启动 Docker
5. Windows 访问服务

### 生产环境

**推荐：纯 Docker Compose**

```bash
# 在服务器上
docker-compose up -d
```

---

## 📚 相关文档

- [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) - 开发环境搭建
- [CONFIGURATION.md](CONFIGURATION.md) - 配置文件说明
- [FRONTEND_EXTENSION_DEPLOYMENT.md](FRONTEND_EXTENSION_DEPLOYMENT.md) - 前端和插件部署

---

## ✅ 总结

### 您的情况

- ✅ **可以使用 Docker**
- ✅ **需要配置网络**（桥接模式）
- ✅ **修改配置文件**（使用 Ubuntu IP）

### 推荐方案

**方案二：虚拟机 Docker**
- 在 Ubuntu 上运行所有 Docker 服务
- Windows 通过 IP 访问
- 插件配置为 Ubuntu IP

### 快速开始

```bash
# 1. VMware 设置桥接网络
# 2. 获取 Ubuntu IP
ip addr show

# 3. 修改 .env 文件
MYSQL_HOST=192.168.1.100

# 4. Ubuntu 启动 Docker
docker-compose up -d

# 5. Windows 访问
http://192.168.1.100
```

---

**最后更新**：2026-03-19  
**版本**：V1.0
