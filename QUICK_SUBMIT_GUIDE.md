# 🚀 快速提交到 GitHub 指南

## 📋 提交前检查清单

在提交之前，请确保：

- ✅ 所有代码已完成并测试通过
- ✅ 没有敏感信息（密码、密钥等）
- ✅ `.gitignore` 配置正确
- ✅ 项目文档完整
- ✅ 本地可以正常运行

---

## 🎯 方法一：使用自动化脚本（推荐）

### 步骤 1：运行提交脚本

**Windows 用户**：
```bash
# 双击运行
submit.bat
```

或在项目根目录打开命令行：
```bash
.\submit.bat
```

**脚本会自动完成**：
1. 检查 Git 安装
2. 初始化 Git 仓库
3. 配置用户信息（如果未配置）
4. 添加所有文件
5. 提交代码
6. 设置默认分支

### 步骤 2：在 GitHub 上创建仓库

1. 访问 https://github.com/new
2. 输入仓库名称：`AI-SecondBrain`
3. **重要**：不要勾选以下选项：
   - ❌ Add a README file
   - ❌ Add .gitignore
   - ❌ Choose a license
4. 点击 "Create repository"
5. 复制仓库地址（例如：`https://github.com/YOUR_USERNAME/AI-SecondBrain.git`）

### 步骤 3：推送到 GitHub

**Windows 用户**：
```bash
# 双击运行
push-to-github.bat
```

脚本会提示您输入仓库地址，然后自动推送。

---

## 🎯 方法二：手动提交（详细步骤）

### 步骤 1：检查 Git 安装

```bash
git --version
```

如果未安装，请访问 https://git-scm.com/download/win 下载安装。

### 步骤 2：配置 Git 用户信息

```bash
# 配置用户名
git config --global user.name "Your Name"

# 配置用户邮箱
git config --global user.email "your.email@example.com"
```

### 步骤 3：初始化 Git 仓库

```bash
cd d:\AI-SecondBrain
git init
```

### 步骤 4：添加所有文件

```bash
git add .
```

### 步骤 5：提交代码

```bash
git commit -m "Initial commit: AI-SecondBrain V2.0

- 完整的智能第二大脑系统
- 基于 Docker 的一键部署
- 支持多平台 AI 对话采集
- 包含知识管理、智能复习、RAG 问答等功能
- 提供 Windows/Linux/macOS 跨平台启动脚本
- 包含浏览器插件和 AI 服务
- 完整的开发和部署文档"
```

### 步骤 6：设置默认分支

```bash
git branch -M main
```

### 步骤 7：关联远程仓库

```bash
# 替换为您的 GitHub 仓库地址
git remote add origin https://github.com/YOUR_USERNAME/AI-SecondBrain.git
```

### 步骤 8：推送到 GitHub

```bash
git push -u origin main
```

---

## 🎯 方法三：使用 Git 图形化工具

### 使用 GitHub Desktop

1. **下载并安装**：https://desktop.github.com/
2. **添加本地仓库**：
   - File → Add Local Repository
   - 选择 `d:\AI-SecondBrain` 文件夹
   - 点击 "Add repository"
3. **提交代码**：
   - 在 Changes 标签页查看所有变更
   - 输入提交信息
   - 点击 "Commit to main"
4. **发布到 GitHub**：
   - 点击右上角 "Publish repository"
   - 输入仓库名称
   - 点击 "Publish"

### 使用 SourceTree

1. **下载并安装**：https://www.sourcetreeapp.com/
2. **添加仓库**：
   - 点击 "Add" → "Create Repository"
   - 选择项目目录
3. **提交和推送**：
   - 选择所有文件
   - 输入提交信息
   - 点击 "Commit"
   - 点击 "Push"

---

## 🔍 常见问题

### Q1: 提示 "nothing to commit, working tree clean"

**原因**：文件已经提交过了

**解决方法**：
```bash
# 查看提交历史
git log

# 如果需要重新提交
git commit --amend -m "新的提交信息"
```

### Q2: 推送失败，提示 "remote: Repository not found"

**原因**：远程仓库不存在或地址错误

**解决方法**：
1. 在 GitHub 上创建新仓库：https://github.com/new
2. 仓库名称：`AI-SecondBrain`
3. **不要**添加 README、.gitignore 或 license
4. 重新运行 `push-to-github.bat`

### Q3: 推送失败，提示 "Authentication failed"

**原因**：GitHub 认证失败

**解决方法**：

**方式 1：使用 Personal Access Token（推荐）**
1. 访问 https://github.com/settings/tokens
2. 点击 "Generate new token"
3. 选择权限：`repo` (Full control of private repositories)
4. 生成 Token 并复制
5. 推送时使用 Token 代替密码

**方式 2：配置 SSH 密钥**
```bash
# 生成 SSH 密钥
ssh-keygen -t ed25519 -C "your.email@example.com"

# 添加公钥到 GitHub
# 访问 https://github.com/settings/keys
# 点击 New SSH key，粘贴 ~/.ssh/id_ed25519.pub 的内容

# 修改远程仓库为 SSH 地址
git remote set-url origin git@github.com:YOUR_USERNAME/AI-SecondBrain.git

# 重新推送
git push -u origin main
```

### Q4: 文件太大，无法推送

**原因**：某些文件超过了 GitHub 的大小限制（100MB）

**解决方法**：
```bash
# 查看大文件
git rev-list --objects --all | grep "$(git verify-pack -v .git/objects/pack/*.idx | sort -k 3 -n | tail -5 | awk '{print$1}')"

# 如果是不需要的大文件，从 Git 历史中删除
# 然后重新推送
```

### Q5: 不小心提交了敏感信息

**解决方法**：
```bash
# 使用 BFG Repo-Cleaner 清理敏感文件
# 1. 下载 BFG：https://rtyley.github.io/bfg-repo-cleaner/
# 2. 运行清理
java -jar bfg.jar --delete-files '*password*' .
java -jar bfg.jar --delete-files '*secret*' .

# 3. 强制推送
git push --force
```

---

## 📊 提交后的检查

### 1. 查看 GitHub 仓库

访问您的仓库页面，确认：
- ✅ 所有文件都已上传
- ✅ README.md 正确显示
- ✅ 项目结构清晰

### 2. 配置仓库设置

**仓库设置**：
1. Settings → General
2. 添加描述：`AI-SecondBrain - 基于 AI 大模型的智能第二大脑系统`
3. 添加主题标签：
   - `ai`
   - `knowledge-management`
   - `second-brain`
   - `vue3`
   - `spring-boot`
   - `docker`
   - `chrome-extension`

**分支设置**：
1. Settings → Branches
2. 添加分支保护规则：
   - Branch name pattern: `main`
   - Require pull request reviews before merging

### 3. 启用 GitHub Actions

1. 访问 https://github.com/YOUR_USERNAME/AI-SecondBrain/actions
2. 点击 "Enable GitHub Actions"
3. CI 工作流会自动运行

---

## 🎯 最佳实践

### 1. 提交信息规范

```bash
# 格式：<type>: <subject>
# 空行
# <body>

# 示例
feat: 添加用户认证功能

- 实现 JWT token 生成和验证
- 添加用户注册和登录接口
- 配置 Spring Security

Closes #123
```

### 2. 定期同步远程仓库

```bash
# 拉取远程更新
git pull origin main

# 查看远程分支
git branch -r

# 查看所有分支
git branch -a
```

### 3. 使用 .gitignore

确保 `.gitignore` 包含：
```gitignore
# IDE
.idea/
.vscode/
*.iml

# 编译产物
target/
build/
dist/
*.class

# 依赖
node_modules/
vendor/

# 环境配置
.env
.env.local
.env.production

# 日志
logs/
*.log

# 系统文件
.DS_Store
Thumbs.db
```

### 4. 使用 Release 管理版本

```bash
# 创建标签
git tag -a v1.0.0 -m "Release version 1.0.0"

# 推送标签
git push origin v1.0.0
```

然后在 GitHub 上创建 Release：
1. 访问 https://github.com/YOUR_USERNAME/AI-SecondBrain/releases
2. 点击 "Draft a new release"
3. 选择标签 `v1.0.0`
4. 填写发布说明
5. 点击 "Publish release"

---

## 📚 相关文档

- [README.md](README.md) - 项目主文档
- [CONTRIBUTING.md](CONTRIBUTING.md) - 贡献指南
- [LICENSE](LICENSE) - MIT 开源协议
- [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) - 开发环境搭建
- [PROJECT_COMPLETE_REVIEW.md](PROJECT_COMPLETE_REVIEW.md) - 项目完整审查

---

## ✅ 提交完成检查清单

- [ ] Git 仓库已初始化
- [ ] 所有文件已添加并提交
- [ ] GitHub 仓库已创建
- [ ] 远程仓库已关联
- [ ] 代码已推送到 GitHub
- [ ] README.md 正确显示
- [ ] 仓库描述和标签已配置
- [ ] GitHub Actions 已启用
- [ ] 分支保护已设置（可选）
- [ ] 第一个 Release 已创建（可选）

---

**恭喜！您的项目已成功提交到 GitHub！** 🎉

**最后更新**：2026-03-19  
**版本**：V1.0
