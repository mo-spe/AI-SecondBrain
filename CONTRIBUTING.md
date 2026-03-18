# 贡献指南

感谢您对 AI-SecondBrain 项目的关注！我们欢迎各种形式的贡献，包括代码提交、问题反馈、文档改进等。

## 🤝 如何贡献

### 1. 报告问题

发现 bug 或有功能建议？请创建 [Issue](https://github.com/YOUR_USERNAME/AI-SecondBrain/issues)：

- **Bug 报告**：请提供详细的复现步骤、环境信息、错误日志
- **功能建议**：请说明使用场景、期望功能、实现思路
- **文档改进**：请指出不清晰或需要补充的部分

### 2. 提交代码

#### 步骤 1：Fork 项目

在 GitHub 页面点击右上角的 "Fork" 按钮

#### 步骤 2：克隆项目

```bash
git clone https://github.com/YOUR_USERNAME/AI-SecondBrain.git
cd AI-SecondBrain
```

#### 步骤 3：创建分支

```bash
# 从 main 分支创建新分支
git checkout -b feature/your-feature-name
```

**分支命名规范**：
- `feature/xxx` - 新功能
- `bugfix/xxx` - Bug 修复
- `docs/xxx` - 文档更新
- `refactor/xxx` - 代码重构
- `test/xxx` - 测试相关

#### 步骤 4：开发和测试

```bash
# 安装依赖
cd frontend
npm install

# 运行测试
cd ../src
mvn test

# 本地运行项目
docker-compose up -d
```

#### 步骤 5：提交更改

```bash
# 添加文件
git add .

# 提交（遵循 Commit Message 规范）
git commit -m "feat: add new feature"

# 推送到远程
git push origin feature/your-feature-name
```

#### 步骤 6：创建 Pull Request

1. 访问您的 Fork 项目
2. 点击 "Compare & pull request"
3. 填写 PR 描述
4. 提交 PR

### 3. 代码审查标准

#### Commit Message 规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型**：
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具/配置

**示例**：
```
feat(review): add Ebbinghaus review algorithm

- Implement review interval calculation
- Add review scheduling task
- Add review statistics

Closes #123
```

#### 代码规范

**后端 Java**：
- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 使用 Checkstyle 进行代码检查
- 添加必要的注释和文档

**前端 Vue**：
- 遵循 [Vue.js Style Guide](https://vuejs.org/style-guide/)
- 使用 ESLint 进行代码检查
- 组件添加必要的注释

#### 测试要求

- 新功能必须包含单元测试
- Bug 修复应包含回归测试
- 测试覆盖率不应降低

**运行测试**：
```bash
# 后端测试
mvn test

# 前端测试
npm test

# 生成测试覆盖率报告
mvn jacoco:report
```

---

## 📋 开发环境搭建

### 必需工具

- **JDK**: 21+
- **Node.js**: 18+
- **Maven**: 3.8+
- **Docker**: 20.10+
- **Git**: 2.30+

### 推荐工具

- **IDE**: IntelliJ IDEA / VS Code
- **数据库工具**: Navicat / DBeaver
- **API 测试**: Postman / Apifox

### 本地开发环境

```bash
# 1. 启动基础服务
docker-compose up -d mysql redis elasticsearch

# 2. 配置开发环境
cp .env.example .env.dev
# 编辑 .env.dev 配置

# 3. 运行后端
cd src
mvn spring-boot:run

# 4. 运行前端
cd frontend
npm install
npm run dev
```

---

## 🎯 开发任务

### 当前需要帮助的功能

- [ ] 移动端 App 开发
- [ ] 多人协作功能
- [ ] 知识分享社区
- [ ] 更多 AI 模型支持
- [ ] 性能优化
- [ ] 国际化支持

查看完整的 [Issue 列表](https://github.com/YOUR_USERNAME/AI-SecondBrain/issues)

### 文档改进

- [ ] 英文文档翻译
- [ ] 视频教程制作
- [ ] 最佳实践文档
- [ ] API 文档完善

---

## 📖 资源链接

- [项目架构](competition_docs/04_项目架构总览.md)
- [部署指南](DEPLOYMENT.md)
- [用户手册](用户手册.md)
- [API 文档](http://localhost:8080/api/doc.html)

---

## ❓ 常见问题

### Q: 如何获取 API 密钥？

A: 访问各平台官网：
- [通义千问](https://dashscope.console.aliyun.com/)
- [DeepSeek](https://platform.deepseek.com/)
- [OpenAI](https://platform.openai.com/api-keys)

### Q: 本地开发需要启动所有服务吗？

A: 不需要。可以只启动 MySQL 和 Redis，其他服务按需启动。

### Q: 提交代码后多久会被合并？

A: 我们会尽快审查代码，通常在 1-3 个工作日内回复。

---

## 🙏 致谢

感谢所有贡献者！

[![Contributors](https://contrib.rocks/image?repo=YOUR_USERNAME/AI-SecondBrain)](https://github.com/YOUR_USERNAME/AI-SecondBrain/graphs/contributors)

---

## 📄 许可证

本项目采用 [MIT](LICENSE) 协议开源。

---

**再次感谢您的贡献！** 🎉
