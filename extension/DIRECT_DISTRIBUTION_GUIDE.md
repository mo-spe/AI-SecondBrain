# AI-SecondBrain 浏览器插件直接分发指南

## 📋 分发方式概述

### 为什么选择直接分发？

**优势**：
- ✅ 无需等待应用商店审核
- ✅ 可以快速发布新版本
- ✅ 用户可以直接下载安装
- ✅ 适合内部测试和推广

**劣势**：
- ❌ 需要用户手动安装
- ❌ 需要提供安装说明
- ❌ 无法自动更新
- ❌ 需要自己管理版本

---

## 🚀 打包步骤

### Windows系统

```bash
# 1. 进入插件目录
cd d:\AI-SecondBrain\extension

# 2. 运行打包脚本
build.bat

# 3. 检查输出
#    - dist/ai-secondbrain-extension-v1.0.0.zip
```

### Linux/Mac系统

```bash
# 1. 进入插件目录
cd /path/to/AI-SecondBrain/extension

# 2. 给脚本添加执行权限
chmod +x build.sh

# 3. 运行打包脚本
./build.sh

# 4. 检查输出
#    - dist/ai-secondbrain-extension-v1.0.0.zip
```

### 手动打包（备用方案）

```bash
# 1. 创建build目录
mkdir build

# 2. 复制以下文件到build目录
#    - manifest.json
#    - background.js
#    - content.js
#    - popup.html
#    - popup.js
#    - icons/ (整个目录）

# 3. 压缩build目录
#    Windows: 右键 -> 发送到 -> 压缩文件夹
#    Linux/Mac: zip -r ai-secondbrain-extension.zip build/*
```

---

## 📤 上传到服务器

### 1. 准备服务器目录

```bash
# SSH连接到服务器
ssh user@your-server.com

# 创建插件目录
mkdir -p /var/www/aisecondbrain.cn/extension
cd /var/www/aisecondbrain.cn/extension

# 创建版本目录
mkdir -p versions
```

### 2. 上传插件文件

```bash
# 使用SCP上传（Windows）
scp dist/ai-secondbrain-extension-v1.0.0.zip user@your-server.com:/var/www/aisecondbrain.cn/extension/versions/

# 使用SCP上传（Linux/Mac）
scp dist/ai-secondbrain-extension-v1.0.0.zip user@your-server.com:/var/www/aisecondbrain.cn/extension/versions/

# 使用SFTP上传（推荐工具：FileZilla、WinSCP）
#    连接到服务器
#    上传到 /var/www/aisecondbrain.cn/extension/versions/
```

### 3. 创建下载链接

```bash
# 创建最新版本的符号链接
ln -sf versions/ai-secondbrain-extension-v1.0.0.zip ai-secondbrain-extension-latest.zip

# 创建版本信息文件
cat > versions.json << EOF
{
  "latest": "1.0.0",
  "versions": [
    {
      "version": "1.0.0",
      "fileName": "ai-secondbrain-extension-v1.0.0.zip",
      "releaseDate": "2026-03-14",
      "changelog": "初始版本发布",
      "downloadUrl": "https://aisecondbrain.cn/extension/versions/ai-secondbrain-extension-v1.0.0.zip",
      "size": "1.2MB"
    }
  ]
}
EOF
```

---

## 🌐 创建下载页面

### 1. 创建HTML页面

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI SecondBrain 浏览器插件下载</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.6;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            background: #f5f5f5;
        }
        .container {
            background: white;
            padding: 40px;
            border-radius: 12px;
            box-shadow: 0 2px 12px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            margin-bottom: 30px;
        }
        .download-section {
            display: flex;
            gap: 20px;
            margin: 30px 0;
        }
        .download-btn {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 15px 30px;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
            transition: all 0.3s ease;
        }
        .download-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 16px rgba(102, 126, 234, 0.5);
        }
        .version-info {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
        }
        .supported-browsers {
            display: flex;
            gap: 20px;
            margin: 20px 0;
        }
        .browser {
            text-align: center;
            padding: 15px;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .install-steps {
            background: #e9ecef;
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
        }
        .install-steps ol {
            margin: 0;
            padding-left: 20px;
        }
        .install-steps li {
            margin: 10px 0;
        }
        .changelog {
            background: #fff3cd;
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>📥 AI SecondBrain 浏览器插件</h1>
        
        <p>AI SecondBrain 浏览器插件帮助您一键采集各大AI平台的对话内容到个人知识库。</p>
        
        <div class="download-section">
            <a href="/extension/ai-secondbrain-extension-latest.zip" class="download-btn" download>
                📥 下载插件 (v1.0.0)
            </a>
        </div>
        
        <div class="version-info">
            <h3>📋 版本信息</h3>
            <ul>
                <li>版本：1.0.0</li>
                <li>发布日期：2026-03-14</li>
                <li>文件大小：1.2MB</li>
                <li>支持平台：Windows、Linux、Mac</li>
            </ul>
        </div>
        
        <div class="supported-browsers">
            <div class="browser">
                <h3>🌐 Chrome</h3>
                <p>✅ 完全支持</p>
            </div>
            <div class="browser">
                <h3>🦊 Edge</h3>
                <p>✅ 完全支持</p>
            </div>
            <div class="browser">
                <h3>🦊 Firefox</h3>
                <p>⚠️ 需要适配</p>
            </div>
        </div>
        
        <div class="install-steps">
            <h3>📖 安装步骤</h3>
            <ol>
                <li>下载插件压缩包</li>
                <li>解压到任意目录</li>
                <li>打开Chrome浏览器</li>
                <li>访问 <code>chrome://extensions/</code></li>
                <li>开启"开发者模式"</li>
                <li>点击"加载已解压的扩展程序"</li>
                <li>选择解压后的目录</li>
            </ol>
        </div>
        
        <div class="changelog">
            <h3>📝 更新日志</h3>
            <h4>v1.0.0 (2026-03-14)</h4>
            <ul>
                <li>✅ 初始版本发布</li>
                <li>✅ 支持ChatGPT、DeepSeek、Kimi等平台</li>
                <li>✅ 一键采集对话内容</li>
                <li>✅ 自动识别对话平台</li>
            </ul>
        </div>
    </div>
</body>
</html>
```

### 2. 保存下载页面

```bash
# 保存到服务器
cat > /var/www/aisecondbrain.cn/extension/index.html << 'EOF'
# [上面的HTML内容]
EOF

# 设置正确的权限
chown -R www-data:www-data /var/www/aisecondbrain.cn/extension
chmod -R 755 /var/www/aisecondbrain.cn/extension
```

---

## 📋 版本管理

### 1. 版本号规则

```
主版本号.次版本号.修订号

例如：
1.0.0 - 初始版本
1.0.1 - Bug修复
1.1.0 - 新功能
2.0.0 - 重大更新
```

### 2. 发布新版本

```bash
# 1. 修改版本号
#    编辑 manifest.json
#    修改 build.bat 和 build.sh

# 2. 运行打包脚本
./build.sh

# 3. 上传新版本
scp dist/ai-secondbrain-extension-v1.0.1.zip user@server:/var/www/aisecondbrain.cn/extension/versions/

# 4. 更新符号链接
ln -sf versions/ai-secondbrain-extension-v1.0.1.zip ai-secondbrain-extension-latest.zip

# 5. 更新版本信息
#    编辑 versions.json
#    添加新版本信息
```

### 3. 版本回滚

```bash
# 回滚到上一个版本
ln -sf versions/ai-secondbrain-extension-v1.0.0.zip ai-secondbrain-extension-latest.zip
```

---

## 📊 下载统计

### 1. 添加下载统计

```javascript
// 在下载页面添加统计代码
<script>
  document.querySelector('.download-btn').addEventListener('click', function() {
    // 发送下载统计
    fetch('https://aisecondbrain.cn/api/extension/download', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        version: '1.0.0',
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
      }),
    });
  });
</script>
```

### 2. 查看下载统计

```bash
# 在后端添加下载统计API
#    POST /api/extension/download
#    GET /api/extension/download/stats
```

---

## 🔒 安全配置

### 1. HTTPS强制

```nginx
# Nginx配置
server {
    listen 443 ssl;
    server_name aisecondbrain.cn;
    
    # SSL证书配置
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    # 强制HTTPS
    if ($scheme != "https") {
        return 301 https://$host$request_uri;
    }
    
    location /extension/ {
        alias /var/www/aisecondbrain.cn/extension;
        autoindex on;
    }
}
```

### 2. 文件完整性校验

```bash
# 生成SHA256校验和
sha256sum dist/ai-secondbrain-extension-v1.0.0.zip > dist/ai-secondbrain-extension-v1.0.0.zip.sha256

# 在下载页面显示校验和
echo "SHA256: $(cat dist/ai-secondbrain-extension-v1.0.0.zip.sha256)"
```

---

## 📞 技术支持

### 常见问题

**Q1: 插件无法加载？**
A: 检查manifest.json格式是否正确，确保所有必需字段都存在。

**Q2: 采集按钮不显示？**
A: 检查是否在支持的网站上，检查content.js是否正常加载。

**Q3: 无法登录？**
A: 检查API地址是否正确，检查网络连接是否正常。

**Q4: 如何更新插件？**
A: 下载新版本，删除旧版本，重新安装新版本。

### 联系方式

- 技术支持邮箱：support@aisecondbrain.cn
- 问题反馈：https://aisecondbrain.cn/feedback
- 文档地址：https://aisecondbrain.cn/docs

---

## 🎯 分发清单

### 发布前检查

- [ ] 插件功能正常
- [ ] 所有配置正确
- [ ] 测试所有支持的平台
- [ ] 检查错误处理
- [ ] 准备下载页面
- [ ] 准备安装说明
- [ ] 准备版本信息

### 发布后检查

- [ ] 下载链接可访问
- [ ] 安装说明清晰
- [ ] 版本信息正确
- [ ] 下载统计正常
- [ ] 用户反馈渠道畅通

---

## 🎉 总结

### ✅ 直接分发优势

1. **快速发布**：无需等待审核
2. **完全控制**：自己管理版本
3. **灵活更新**：随时发布新版本
4. **用户友好**：直接下载安装

### 📋 分发流程

1. **打包插件**：使用打包脚本
2. **上传服务器**：上传到指定目录
3. **创建页面**：创建下载页面
4. **测试下载**：确保下载链接正常
5. **发布通知**：通知用户更新

### 🚀 下一步

1. **运行打包脚本**：build.bat 或 build.sh
2. **上传到服务器**：使用SCP或SFTP
3. **创建下载页面**：提供友好的下载界面
4. **测试下载**：确保一切正常

---

**直接分发准备完成！现在可以开始打包和发布了！**
