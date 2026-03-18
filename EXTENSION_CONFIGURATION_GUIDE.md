# 浏览器插件配置指南

本文档详细说明如何配置浏览器插件以支持不同环境的部署。

## 📋 目录

- [问题说明](#问题说明)
- [解决方案](#解决方案)
- [本地开发配置](#本地开发配置)
- [测试环境配置](#测试环境配置)
- [生产环境配置](#生产环境配置)
- [常见问题](#常见问题)

---

## 🔍 问题说明

### 当前状态

浏览器插件（`extension/`）中硬编码了生产环境的 API 地址：

**`extension/background.js`**:
```javascript
let API_BASE_URL = "https://aisecondbrain.cn/api";  // ❌ 硬编码
```

**`extension/config/production.json`**:
```json
{
  "api": {
    "baseUrl": "https://aisecondbrain.cn/api"  // ❌ 硬编码
  }
}
```

**`extension/manifest.json`**:
```json
"host_permissions": [
  "https://aisecondbrain.cn/*"  // ❌ 只有生产域名
]
```

### 导致的问题

1. ❌ **本地部署时无法使用** - 插件只能访问生产环境
2. ❌ **开发调试困难** - 无法测试本地后端
3. ❌ **缺乏灵活性** - 无法切换到测试环境

---

## ✅ 解决方案

### 方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **方案 1：修改源代码** | 简单直接 | 每次切换需修改代码 | ⭐⭐⭐ |
| **方案 2：配置文件切换** | 灵活，易维护 | 需要额外配置文件 | ⭐⭐⭐⭐⭐ |
| **方案 3：Storage 配置** | 最灵活，用户友好 | 实现复杂 | ⭐⭐⭐⭐ |

---

## 🛠️ 方案 1：修改源代码（简单直接）

### 步骤

#### 1. 修改 `background.js`

```javascript
// 根据环境选择 API 地址
// 本地开发环境
let API_BASE_URL = "http://localhost:8080/api";
let DEBUG = true;

// 生产环境（注释掉上面的，取消注释下面的）
// let API_BASE_URL = "https://aisecondbrain.cn/api";
// let DEBUG = false;

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === "saveChat") {
    // 使用 API_BASE_URL 发送请求
    fetch(API_BASE_URL + '/chat/save', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request.data)
    }).then(response => {
      if (DEBUG) {
        console.log('保存成功:', response);
      }
    });
  }
});
```

#### 2. 修改 `manifest.json`

```json
{
  "manifest_version": 3,
  "name": "AI SecondBrain Collector",
  "version": "1.0.0",
  "host_permissions": [
    "http://localhost:8080/*",     // ✅ 添加本地地址
    "http://127.0.0.1:8080/*",     // ✅ 添加本地 IP
    "https://chatgpt.com/*",
    "https://chat.openai.com/*",
    "https://chat.deepseek.com/*",
    "https://www.kimi.com/*",
    "https://kimi.moonshot.cn/*",
    "https://kimi.ai/*",
    "https://www.doubao.com/*",
    "https://www.zhipuai.cn/*",
    "https://www.qianwen.com/*",
    "https://aisecondbrain.cn/*"
  ],
  // ... 其他配置
}
```

#### 3. 重新加载插件

1. 打开 Chrome 浏览器
2. 访问 `chrome://extensions/`
3. 开启"开发者模式"
4. 点击"刷新"按钮重新加载插件

---

## 🛠️ 方案 2：配置文件切换（推荐）

### 步骤

#### 1. 创建多环境配置文件

**`extension/config/local.json`**:
```json
{
  "environment": "local",
  "api": {
    "baseUrl": "http://localhost:8080/api",
    "timeout": 30000,
    "retryCount": 3
  },
  "websocket": {
    "url": "ws://localhost:8080/ws",
    "reconnectInterval": 5000
  },
  "features": {
    "autoSave": false,
    "debugMode": true,
    "logLevel": "debug"
  }
}
```

**`extension/config/test.json`**:
```json
{
  "environment": "test",
  "api": {
    "baseUrl": "http://test-server:8080/api",
    "timeout": 30000,
    "retryCount": 3
  },
  "websocket": {
    "url": "ws://test-server:8080/ws",
    "reconnectInterval": 5000
  },
  "features": {
    "autoSave": true,
    "debugMode": true,
    "logLevel": "info"
  }
}
```

**保留 `extension/config/production.json`**:
```json
{
  "environment": "production",
  "api": {
    "baseUrl": "https://aisecondbrain.cn/api",
    "timeout": 30000,
    "retryCount": 3
  },
  "websocket": {
    "url": "wss://aisecondbrain.cn/ws",
    "reconnectInterval": 5000
  },
  "features": {
    "autoSave": true,
    "debugMode": false,
    "logLevel": "error"
  }
}
```

#### 2. 修改 `background.js` 支持配置加载

```javascript
let config = {
  api: {
    baseUrl: "http://localhost:8080/api",  // 默认本地
    timeout: 30000,
    retryCount: 3
  },
  websocket: {
    url: "ws://localhost:8080/ws",
    reconnectInterval: 5000
  },
  features: {
    autoSave: false,
    debugMode: true,
    logLevel: "debug"
  }
};

// 从 Chrome Storage 读取配置
chrome.storage.local.get(['environmentConfig'], (result) => {
  if (result.environmentConfig) {
    config = result.environmentConfig;
    console.log('使用自定义配置:', config);
  } else {
    console.log('使用默认配置（本地开发）');
  }
});

// 使用配置
const API_BASE_URL = config.api.baseUrl;
const DEBUG = config.features.debugMode;

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === "saveChat") {
    fetch(API_BASE_URL + '/chat/save', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request.data),
      timeout: config.api.timeout
    }).then(response => {
      if (DEBUG) {
        console.log('保存成功:', response);
      }
    });
  }
});
```

#### 3. 创建配置切换页面（可选）

**`extension/popup/settings.html`**:
```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>插件设置</title>
  <link rel="stylesheet" href="settings.css">
</head>
<body>
  <div class="settings-container">
    <h2>环境配置</h2>
    
    <div class="form-group">
      <label for="environment">选择环境：</label>
      <select id="environment">
        <option value="local">本地开发</option>
        <option value="test">测试环境</option>
        <option value="production">生产环境</option>
      </select>
    </div>
    
    <div class="form-group">
      <label for="apiUrl">API 地址：</label>
      <input type="text" id="apiUrl" placeholder="http://localhost:8080/api">
    </div>
    
    <div class="form-group">
      <label>
        <input type="checkbox" id="debugMode">
        调试模式
      </label>
    </div>
    
    <button id="saveBtn">保存配置</button>
    <button id="resetBtn">重置</button>
  </div>
  
  <script src="settings.js"></script>
</body>
</html>
```

**`extension/popup/settings.js`**:
```javascript
// 加载当前配置
chrome.storage.local.get(['environmentConfig'], (result) => {
  if (result.environmentConfig) {
    document.getElementById('apiUrl').value = result.environmentConfig.api.baseUrl;
    document.getElementById('debugMode').checked = result.environmentConfig.features.debugMode;
  }
});

// 保存配置
document.getElementById('saveBtn').addEventListener('click', () => {
  const config = {
    api: {
      baseUrl: document.getElementById('apiUrl').value,
      timeout: 30000,
      retryCount: 3
    },
    features: {
      debugMode: document.getElementById('debugMode').checked
    }
  };
  
  chrome.storage.local.set({ environmentConfig: config }, () => {
    alert('配置已保存！请重新加载插件。');
    chrome.runtime.reload();
  });
});

// 重置配置
document.getElementById('resetBtn').addEventListener('click', () => {
  chrome.storage.local.remove(['environmentConfig'], () => {
    alert('配置已重置为默认值！');
    chrome.runtime.reload();
  });
});
```

#### 4. 在 `popup.html` 中添加设置入口

```html
<div class="popup-container">
  <h2>AI SecondBrain</h2>
  <button id="saveBtn">保存到知识库</button>
  <button id="settingsBtn">⚙️ 设置</button>
</div>

<script>
  document.getElementById('settingsBtn').addEventListener('click', () => {
    chrome.tabs.create({ url: chrome.runtime.getURL('popup/settings.html') });
  });
</script>
```

#### 5. 修改 `manifest.json`

```json
{
  "manifest_version": 3,
  "name": "AI SecondBrain Collector",
  "version": "1.0.1",
  "permissions": [
    "activeTab",
    "storage",
    "scripting"
  ],
  "host_permissions": [
    "http://localhost:8080/*",
    "http://127.0.0.1:8080/*",
    "http://test-server:8080/*",
    "https://chatgpt.com/*",
    "https://chat.openai.com/*",
    "https://chat.deepseek.com/*",
    "https://www.kimi.com/*",
    "https://kimi.moonshot.cn/*",
    "https://kimi.ai/*",
    "https://www.doubao.com/*",
    "https://www.zhipuai.cn/*",
    "https://www.qianwen.com/*",
    "https://aisecondbrain.cn/*"
  ],
  "action": {
    "default_popup": "popup/popup.html",
    "default_title": "AI SecondBrain"
  },
  "background": {
    "service_worker": "background.js"
  },
  "web_accessible_resources": [
    {
      "resources": [
        "config/*.json",
        "icons/*"
      ],
      "matches": ["<all_urls>"]
    }
  ]
}
```

---

## 🎯 不同环境的快速配置

### 本地开发环境

**配置文件**：`config/local.json`

```json
{
  "api": {
    "baseUrl": "http://localhost:8080/api"
  }
}
```

**启动命令**：
```bash
# 1. 启动后端
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 2. 启动前端
cd frontend
npm run dev

# 3. 加载插件（已配置为本地）
# chrome://extensions/ -> 加载 extension/
```

### 测试环境

**配置文件**：`config/test.json`

```json
{
  "api": {
    "baseUrl": "http://test-server:8080/api"
  }
}
```

### 生产环境

**配置文件**：`config/production.json`（默认）

```json
{
  "api": {
    "baseUrl": "https://aisecondbrain.cn/api"
  }
}
```

---

## 📋 配置检查清单

### 本地开发配置检查

- [ ] 修改 `background.js` 中的 API 地址为 `http://localhost:8080/api`
- [ ] 在 `manifest.json` 中添加 `http://localhost:8080/*` 权限
- [ ] 后端服务已启动（端口 8080）
- [ ] 前端服务已启动（端口 5173）
- [ ] CORS 配置允许跨域
- [ ] 插件已重新加载

### 测试配置

- [ ] 创建 `config/test.json`
- [ ] 配置测试服务器地址
- [ ] 测试连接是否正常
- [ ] 验证功能完整性

### 生产配置

- [ ] 使用默认的 `production.json`
- [ ] 验证 HTTPS 证书
- [ ] 测试所有功能
- [ ] 检查性能

---

## 🔍 常见问题

### Q1: 插件无法访问本地 API？

**A**: 检查以下几点：
1. 确保 `manifest.json` 中包含 `http://localhost:8080/*`
2. 确保后端服务已启动
3. 检查 CORS 配置
4. 查看浏览器控制台错误信息

### Q2: 如何切换环境？

**A**: 
- **方案 1**：修改 `background.js` 中的 API 地址
- **方案 2**：使用配置页面切换（推荐）
- **方案 3**：修改 Chrome Storage 中的配置

### Q3: 插件保存失败？

**A**: 检查：
1. Token 是否有效
2. API 地址是否正确
3. 网络连接是否正常
4. 后端日志

### Q4: 如何调试插件？

**A**:
1. 打开 `chrome://extensions/`
2. 找到插件，点击"检查视图"
3. 查看 Console 和 Network
4. 使用 `console.log()` 输出调试信息

### Q5: 多人协作时如何配置？

**A**:
1. 使用统一的测试环境配置
2. 或者每人使用本地开发环境
3. 通过配置文件分发配置

---

## 📚 相关文档

- [开发环境搭建指南](DEVELOPMENT_GUIDE.md)
- [配置文件说明](CONFIGURATION.md)
- [项目完整审查报告](PROJECT_COMPLETE_REVIEW.md)

---

## 🎯 最佳实践

### 1. 使用配置文件

```javascript
// ✅ 推荐：从配置读取
const config = loadConfig();
const apiUrl = config.api.baseUrl;

// ❌ 不推荐：硬编码
const apiUrl = "https://aisecondbrain.cn/api";
```

### 2. 支持环境切换

```javascript
// ✅ 推荐：支持多环境
const environments = {
  local: 'http://localhost:8080/api',
  test: 'http://test-server:8080/api',
  production: 'https://aisecondbrain.cn/api'
};

// ❌ 不推荐：固定一个环境
```

### 3. 添加错误处理

```javascript
// ✅ 推荐：完整的错误处理
fetch(apiUrl + '/save', options)
  .then(response => {
    if (!response.ok) throw new Error('Network error');
    return response.json();
  })
  .catch(error => {
    console.error('保存失败:', error);
    // 显示错误提示
  });
```

### 4. 日志分级

```javascript
// ✅ 推荐：根据配置控制日志级别
if (config.features.debugMode) {
  console.log('调试信息');
}
if (config.features.logLevel === 'error') {
  console.error('错误信息');
}
```

---

**配置完成！** 🎉

**最后更新**：2026-03-19  
**版本**：V1.0
