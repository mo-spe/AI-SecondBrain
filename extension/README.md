# 插件本地开发环境快速配置指南

## ✅ 已完成的修改

### 1. 修改 `background.js`

```javascript
// ✅ 已修改为本地开发环境
let API_BASE_URL = "http://localhost:8080/api";
let DEBUG = true;  // 开启调试模式
```

### 2. 修改 `manifest.json`

```json
{
  "host_permissions": [
    "http://localhost:8080/*",     // ✅ 已添加
    "http://127.0.0.1:8080/*",     // ✅ 已添加
    "https://chatgpt.com/*",
    // ... 其他域名
  ]
}
```

---

## 🚀 使用步骤

### 步骤 1：启动后端服务

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**验证后端启动成功**：
- 访问：http://localhost:8080/api/doc.html
- 看到 Swagger 文档页面表示成功

### 步骤 2：启动前端服务（可选）

```bash
cd frontend
npm install
npm run dev
```

**验证前端启动成功**：
- 访问：http://localhost:5173
- 看到登录页面表示成功

### 步骤 3：加载浏览器插件

1. **打开 Chrome 浏览器**

2. **访问扩展管理页面**
   ```
   chrome://extensions/
   ```

3. **开启开发者模式**
   - 点击右上角的"开发者模式"开关
   - 开启后会出现"加载已解压的扩展程序"按钮

4. **加载插件**
   - 点击"加载已解压的扩展程序"
   - 选择文件夹：`d:\AI-SecondBrain\extension`
   - 点击"选择文件夹"

5. **验证加载成功**
   - 看到 "AI SecondBrain Collector" 插件
   - 状态显示为"已启用"

### 步骤 4：测试插件功能

1. **访问 AI 平台**
   - 打开任意支持的 AI 平台，例如：
     - https://chatgpt.com
     - https://chat.deepseek.com
     - https://kimi.moonshot.cn

2. **进行对话**
   - 与 AI 进行一段对话

3. **使用插件保存**
   - 点击浏览器右上角的插件图标
   - 点击"保存到知识库"按钮
   - 查看是否有成功提示

4. **检查后端日志**
   ```
   # 在运行后端的终端查看日志
   # 应该能看到类似这样的日志：
   # POST /api/chat/save 200 OK
   ```

5. **验证数据保存**
   - 访问前端：http://localhost:5173
   - 登录（默认账号：admin / admin123）
   - 查看对话列表中是否有刚才保存的内容

---

## 🔧 环境切换

### 切换到生产环境

如果要切换到生产环境，修改 `background.js`：

```javascript
// 注释掉本地配置
// let API_BASE_URL = "http://localhost:8080/api";
// let DEBUG = true;

// 启用生产环境配置
let API_BASE_URL = "https://aisecondbrain.cn/api";
let DEBUG = false;
```

### 切换到测试环境

```javascript
let API_BASE_URL = "http://test-server:8080/api";
let DEBUG = true;
```

---

## ⚠️ 常见问题

### 问题 1：插件图标不显示

**解决方法**：
1. 刷新浏览器页面
2. 重新加载插件
3. 检查浏览器工具栏是否隐藏了插件图标

### 问题 2：保存失败，提示网络错误

**检查清单**：
- [ ] 后端服务是否启动？
- [ ] 后端端口是否为 8080？
- [ ] CORS 配置是否正确？
- [ ] 防火墙是否阻止连接？

**调试步骤**：
```bash
# 1. 检查后端是否运行
curl http://localhost:8080/api/health

# 2. 查看后端日志
docker-compose logs backend

# 3. 检查浏览器控制台
# F12 -> Console -> 查看错误信息
```

### 问题 3：插件无法访问 AI 平台

**解决方法**：
1. 检查 `manifest.json` 中是否包含该平台的域名
2. 重新加载插件
3. 刷新 AI 平台页面

### 问题 4：修改配置后不生效

**解决方法**：
1. 在 `chrome://extensions/` 页面点击插件的"刷新"按钮
2. 关闭并重新打开浏览器
3. 清除浏览器缓存

---

## 📝 配置说明

### background.js 配置项

```javascript
// API 地址
let API_BASE_URL = "http://localhost:8080/api";

// 调试模式（开启后会输出详细日志）
let DEBUG = true;
```

### manifest.json 配置项

```json
{
  "host_permissions": [
    "http://localhost:8080/*",     // 本地后端地址
    "http://127.0.0.1:8080/*",     // 本地 IP 地址
    // ... 其他允许的域名
  ]
}
```

---

## 🎯 开发建议

### 1. 使用调试模式

开发时保持 `DEBUG = true`，可以看到详细的日志输出：

```javascript
let DEBUG = true;
```

### 2. 热重载

修改插件代码后：
1. 在 `chrome://extensions/` 页面点击刷新按钮
2. 不需要重新加载插件

### 3. 查看日志

**后端日志**：
```bash
# 查看实时日志
tail -f logs/ai-second-brain.log

# 或者在 IDEA 中查看 Console 输出
```

**浏览器日志**：
```
# 打开开发者工具
F12 -> Console

# 查看插件日志
chrome://extensions/ -> 插件 -> 检查视图
```

### 4. 快速测试

创建测试脚本快速验证：

```bash
# test-plugin.sh 或 test-plugin.bat
curl -X POST http://localhost:8080/api/chat/save \
  -H "Content-Type: application/json" \
  -d '{
    "platform": "test",
    "content": "测试对话内容"
  }'
```

---

## 📚 相关文档

- [开发环境搭建指南](DEVELOPMENT_GUIDE.md)
- [浏览器插件配置指南](EXTENSION_CONFIGURATION_GUIDE.md)
- [项目完整审查报告](PROJECT_COMPLETE_REVIEW.md)

---

## ✅ 配置完成检查清单

- [ ] 后端服务已启动（端口 8080）
- [ ] 前端服务已启动（端口 5173，可选）
- [ ] `background.js` 已修改为本地地址
- [ ] `manifest.json` 已添加本地权限
- [ ] 插件已重新加载
- [ ] 测试保存功能正常
- [ ] 后端日志显示请求成功
- [ ] 前端可以看到保存的数据

---

**配置完成！现在可以在本地开发环境使用插件了！** 🎉

**最后更新**：2026-03-19  
**版本**：V1.0
