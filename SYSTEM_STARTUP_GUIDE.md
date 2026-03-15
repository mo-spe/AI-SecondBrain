# AI-SecondBrain 系统启动指南

## 📊 当前状态

### ✅ 已成功启动
- **Kafka**: 端口9092，运行正常
  - 消费者已连接
  - 主题 `async-task-topic-0` 已创建

### ❌ 需要手动启动
- **Elasticsearch**: 权限问题
- **后端应用**: Maven权限问题

## 🔧 解决步骤

### 1. 启动Elasticsearch

由于权限问题，需要手动启动：

```powershell
# 以管理员身份运行PowerShell
cd D:\elasticsearch-8.11.0\elasticsearch-8.11.0\bin
.\elasticsearch.bat
```

或者使用服务安装：

```powershell
# 安装为Windows服务
.\elasticsearch-service.bat install

# 启动服务
.\elasticsearch-service.bat start
```

### 2. 启动后端应用

```powershell
cd D:\AI-SecondBrain
mvn spring-boot:run
```

如果Maven权限有问题，尝试：

```powershell
# 以管理员身份运行
# 或者使用IDE直接运行主类
# 主类：com.secondbrain.AiSecondBrainApplication
```

### 3. 启动前端应用

```powershell
cd D:\AI-SecondBrain\frontend
npm run dev
```

## 📝 配置说明

### Kafka配置
- **端口**: 9092
- **配置文件**: `d:\AI-SecondBrain\kafka-server-fixed.properties`
- **数据目录**: `D:\kafka_2.13-4.2.0\data\kafka-logs`

### Elasticsearch配置
- **端口**: 9200
- **用户名**: elastic
- **密码**: elastic123
- **数据目录**: `D:\elasticsearch-8.11.0\elasticsearch-8.11.0\data`

### 应用配置
- **后端端口**: 8080
- **前端端口**: 3001
- **API路径**: /api

## 🚀 验证服务状态

### 检查Kafka
```powershell
netstat -ano | findstr :9092
```

### 检查Elasticsearch
```powershell
curl http://localhost:9200
```

### 检查后端
```powershell
netstat -ano | findstr :8080
```

### 检查前端
```powershell
netstat -ano | findstr :3001
```

## 🐛 常见问题

### Elasticsearch启动失败
**问题**: 权限不足
**解决**:
1. 以管理员身份运行
2. 检查数据目录权限
3. 查看日志文件：`D:\elasticsearch-8.11.0\elasticsearch-8.11.0\logs\ai-second-brain.log`

### Maven编译失败
**问题**: 仓库权限不足
**解决**:
1. 以管理员身份运行
2. 检查 `D:\Maven\repository` 权限
3. 清理Maven缓存：`mvn clean`

### Kafka连接失败
**问题**: 端口被占用或服务未启动
**解决**:
1. 检查Kafka进程：`Get-Process -Name java | Where-Object { $_.CommandLine -like "*kafka*" }`
2. 重启Kafka服务
3. 检查配置文件中的端口设置

## 📋 功能说明

### Kafka异步任务处理
- **生产者**: `KafkaProducerService`
- **消费者**: `AsyncTaskConsumerService`
- **主题**: `async-task-topic`
- **功能**: 实现任务与用户会话解耦，用户可以切换页面而不中断任务

### Elasticsearch语义搜索
- **服务**: `ElasticsearchService`
- **实现**: `ElasticsearchServiceImpl`
- **功能**: 提供关键词搜索、多字段搜索和语义搜索
- **当前状态**: 接口已定义，需要Elasticsearch服务运行

### WebSocket实时通信
- **配置**: `WebSocketConfig`
- **服务**: `WebSocketService`
- **功能**: 实时推送任务进度和状态更新

## 🎯 下一步

1. ✅ 启动Elasticsearch（需要管理员权限）
2. ✅ 启动后端应用
3. ✅ 测试异步任务处理
4. ✅ 测试语义搜索功能
5. ✅ 测试WebSocket实时更新

## 📞 技术支持

如果遇到问题，请检查：
1. 服务日志文件
2. 系统事件查看器
3. 端口占用情况
4. 权限设置