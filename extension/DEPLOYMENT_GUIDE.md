# AI-SecondBrain 浏览器插件生产环境部署指南

## 📋 生产环境配置总结

### ✅ 已完成的修改

1. **API地址配置**
   - ✅ `background.js`: `https://aisecondbrain.cn/api`
   - ✅ `content.js`: `https://aisecondbrain.cn/api`

2. **host_permissions配置**
   - ✅ 添加了 `https://aisecondbrain.cn/*`
   - ✅ 添加了 `https://www.aisecondbrain.cn/*`
   - ✅ 移除了 `http://localhost:8080/*`

3. **调试日志优化**
   - ✅ 移除了大量调试日志
   - ✅ 添加了 `DEBUG` 控制开关
   - ✅ 保留了必要的错误日志

4. **配置文件**
   - ✅ 创建了 `config/production.json`

---

## 📦 插件文件清单

### 核心文件

| 文件名 | 说明 | 状态 |
|--------|------|------|
| `manifest.json` | 插件配置文件 | ✅ 已修改 |
| `background.js` | 后台服务脚本 | ✅ 已修改 |
| `content.js` | 内容脚本 | ✅ 已修改 |
| `popup.html` | 弹窗页面 | ✅ 无需修改 |
| `popup.js` | 弹窗脚本 | ✅ 无需修改 |

### 配置文件

| 文件名 | 说明 | 状态 |
|--------|------|------|
| `config/production.json` | 生产环境配置 | ✅ 已创建 |

### 资源文件

| 文件名 | 说明 | 状态 |
|--------|------|------|
| `icons/icon.svg` | 插件图标 | ✅ 无需修改 |

---

## 🚀 部署步骤

### 步骤1：打包插件

```bash
# 进入插件目录
cd d:\AI-SecondBrain\extension

# 打包插件（Windows）
# 方法1：使用zip压缩
# 选择所有文件，压缩为zip文件
# 注意：不要包含config/production.json

# 方法2：使用命令行
powershell Compress-Archive -Path * -DestinationPath ai-secondbrain-extension.zip -Force
```

### 步骤2：测试插件

```bash
# 1. 打开Chrome浏览器
# 2. 访问 chrome://extensions/
# 3. 开启"开发者模式"
# 4. 点击"加载已解压的扩展程序"
# 5. 选择 extension 文件夹
```

### 步骤3：验证功能

```bash
# 1. 访问支持的AI平台
#    - https://chatgpt.com
#    - https://chat.deepseek.com
#    - https://www.kimi.com

# 2. 检查是否出现采集按钮
#    - 按钮位置：右上角
#    - 按钮文本："📥 采集到知识库"

# 3. 点击插件图标
#    - 检查是否能正常登录
#    - 检查是否能正常保存Token

# 4. 测试采集功能
#    - 点击采集按钮
#    - 检查是否能成功采集对话
```

---

## 📝 发布到应用商店

### Chrome Web Store

#### 1. 准备发布材料

**必需材料**：
- ✅ 插件zip文件
- ✅ 应用图标（128x128）
- ✅ 屏幕截图（1280x800 或 640x400）
- ✅ 宣传图片（440x280）
- ✅ 隐私政策URL
- ✅ 商店列表信息

**商店列表信息**：
```
名称：AI SecondBrain Collector
简短描述：一键采集AI对话到个人知识库
详细描述：
AI SecondBrain Collector 是一款浏览器扩展，帮助您一键采集各大AI平台的对话内容到个人知识库。

支持的平台：
- ChatGPT
- DeepSeek
- Kimi
- 豆包
- 智谱清言
- 通义千问

主要功能：
✅ 一键采集对话内容
✅ 自动识别对话平台
✅ 支持多种AI平台
✅ 安全的数据传输
✅ 简单易用的界面

使用方法：
1. 访问支持的AI平台
2. 点击右上角的"采集到知识库"按钮
3. 登录您的AI SecondBrain账号
4. 对话内容将自动保存到您的知识库

隐私政策：https://aisecondbrain.cn/privacy
支持邮箱：support@aisecondbrain.cn
```

#### 2. 提交审核

```bash
# 1. 访问 Chrome Web Store Developer Dashboard
#    https://chrome.google.com/webstore/devconsole

# 2. 创建新项目
#    - 点击"新建项目"
#    - 上传zip文件
#    - 填写商店信息
#    - 上传图标和截图

# 3. 提交审核
#    - 检查所有必填项
#    - 支付5美元注册费（首次）
#    - 提交审核

# 4. 等待审核
#    - 审核时间：1-3天
#    - 审核通过后自动发布
```

#### 3. 审核注意事项

**常见拒绝原因**：
- ❌ 插件描述不够详细
- ❌ 缺少隐私政策
- ❌ 图标尺寸不符合要求
- ❌ 功能描述与实际不符
- ❌ 包含恶意代码

**建议**：
- ✅ 提供详细的隐私政策
- ✅ 确保图标尺寸正确
- ✅ 提供清晰的功能说明
- ✅ 提供多个截图展示功能

---

### Edge Add-ons

#### 1. 准备发布材料

**必需材料**：
- ✅ 插件zip文件
- ✅ 应用图标（128x128）
- ✅ 屏幕截图（1280x800 或 640x400）
- ✅ 商店列表信息

#### 2. 提交审核

```bash
# 1. 访问 Edge Add-ons Developer Dashboard
#    https://partner.microsoft.com/dashboard/microsoftedge/extension/listing

# 2. 创建新项目
#    - 点击"提交新扩展"
#    - 上传zip文件
#    - 填写商店信息
#    - 上传图标和截图

# 3. 提交审核
#    - 检查所有必填项
#    - 提交审核

# 4. 等待审核
#    - 审核时间：1-2天
#    - 审核通过后自动发布
```

---

### Firefox Add-ons

#### 1. 适配Firefox

**需要修改的地方**：

```javascript
// manifest.json
{
  "manifest_version": 2,  // Firefox使用v2
  "applications": {
    "gecko": {
      "id": "ai-secondbrain@aisecondbrain.cn",
      "strict_min_version": "78.0"
    }
  }
}
```

#### 2. 提交审核

```bash
# 1. 访问 Firefox Add-ons Developer Hub
#    https://addons.mozilla.org/developers/

# 2. 创建新项目
#    - 点击"提交新扩展"
#    - 上传zip文件
#    - 填写商店信息
#    - 上传图标和截图

# 3. 提交审核
#    - 检查所有必填项
#    - 提交审核

# 4. 等待审核
#    - 审核时间：1-3天
#    - 审核通过后自动发布
```

---

## 📦 直接分发（备用方案）

### 方法1：提供crx文件下载

```bash
# 1. 打包为crx文件
#    - 使用Chrome扩展程序打包工具
#    - 或使用在线工具

# 2. 上传到服务器
#    - 上传到 https://aisecondbrain.cn/extension/ai-secondbrain.crx

# 3. 提供下载链接
#    - 在网站上提供下载链接
#    - 提供安装说明
```

### 方法2：提供zip文件下载

```bash
# 1. 打包为zip文件
#    - 压缩所有插件文件

# 2. 上传到服务器
#    - 上传到 https://aisecondbrain.cn/extension/ai-secondbrain.zip

# 3. 提供下载链接
#    - 在网站上提供下载链接
#    - 提供安装说明
```

---

## 🔒 安全配置

### 1. Content Security Policy

```json
{
  "content_security_policy": {
    "extension_pages": "script-src 'self'; object-src 'self'; connect-src 'self' https://aisecondbrain.cn",
    "content_scripts": "script-src 'self'"
  }
}
```

### 2. 权限最小化

```json
{
  "permissions": [
    "activeTab",
    "storage",
    "scripting"
  ]
}
```

### 3. HTTPS强制

```javascript
// 确保所有API请求使用HTTPS
const API_BASE_URL = "https://aisecondbrain.cn/api";

// 检查HTTPS
if (location.protocol !== 'https:') {
  console.warn('建议使用HTTPS访问');
}
```

---

## 📊 监控和日志

### 1. 错误监控

```javascript
// background.js
chrome.runtime.onError.addListener((error) => {
  console.error('插件错误:', error);
  
  // 发送错误到服务器
  fetch('https://aisecondbrain.cn/api/extension/error', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      error: error.message,
      stack: error.stack,
      timestamp: new Date().toISOString(),
    }),
  });
});
```

### 2. 使用统计

```javascript
// background.js
chrome.runtime.onInstalled.addListener(() => {
  // 发送安装统计
  fetch('https://aisecondbrain.cn/api/extension/install', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      version: chrome.runtime.getManifest().version,
      timestamp: new Date().toISOString(),
    }),
  });
});
```

---

## 🎯 测试清单

### 功能测试

- [ ] 插件能正常加载
- [ ] 采集按钮能正常显示
- [ ] 能正常登录
- [ ] 能正常采集对话
- [ ] Token能正常保存
- [ ] 错误处理正常

### 兼容性测试

- [ ] Chrome浏览器
- [ ] Edge浏览器
- [ ] Firefox浏览器
- [ ] ChatGPT平台
- [ ] DeepSeek平台
- [ ] Kimi平台
- [ ] 豆包平台
- [ ] 智谱清言平台
- [ ] 通义千问平台

### 安全测试

- [ ] HTTPS通信正常
- [ ] Token存储安全
- [ ] 权限最小化
- [ ] CSP策略正确

---

## 📞 技术支持

### 常见问题

**Q1: 插件无法加载？**
A: 检查manifest.json格式是否正确，确保所有必需字段都存在。

**Q2: 采集按钮不显示？**
A: 检查是否在支持的网站上，检查content.js是否正常加载。

**Q3: 无法登录？**
A: 检查API地址是否正确，检查网络连接是否正常。

**Q4: 采集失败？**
A: 检查Token是否有效，检查API是否正常响应。

### 联系方式

- 技术支持邮箱：support@aisecondbrain.cn
- 问题反馈：https://aisecondbrain.cn/feedback
- 文档地址：https://aisecondbrain.cn/docs

---

## 🎉 总结

### ✅ 已完成的配置

1. **API地址配置**：`https://aisecondbrain.cn/api`
2. **host_permissions配置**：添加了生产域名
3. **调试日志优化**：移除了大量调试日志
4. **配置文件创建**：创建了生产环境配置

### 📋 后续步骤

1. **打包插件**：压缩为zip文件
2. **测试插件**：在浏览器中测试功能
3. **提交审核**：提交到应用商店
4. **发布上线**：审核通过后发布

### 🎯 目标

- ✅ Chrome Web Store发布
- ✅ Edge Add-ons发布
- ✅ Firefox Add-ons发布（可选）
- ✅ 提供直接下载（备用）

---

**插件生产环境配置已完成！可以开始打包和发布流程了！**
