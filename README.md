# AI-SecondBrain

## 📋 项目简介

AI第二大脑系统 - 智能对话采集、知识管理、学习报告生成平台

## 🎯 核心功能

### 1. 对话采集
- 支持多平台对话采集（ChatGPT、DeepSeek、Kimi）
- 浏览器插件一键采集
- 自动分类和标签管理

### 2. 知识管理
- 智能知识图谱
- 语义搜索（Elasticsearch）
- 知识点关联分析

### 3. 学习报告
- AI生成个性化学习报告
- 学习路径规划
- 知识盲区分析

### 4. 智能推荐
- 基于学习历史的智能推荐
- 个性化学习建议
- 学习资源推荐

## 🏗️ 技术架构

### 后端技术栈
- Spring Boot 3.1.5
- Java 21
- MyBatis-Plus 3.5.3.1
- MySQL 8.0
- Redis 7
- Kafka 3.6
- Elasticsearch 8.11

### 前端技术栈
- Vue.js 3
- Element Plus
- Vue Router 4
- Pinia
- Vite 5

### AI服务
- 通义千问（Qwen Plus）
- DeepSeek
- OpenAI（可选）

### 部署方式
- Docker + Docker Compose
- 一键部署
- 环境变量配置

## 🚀 快速开始

### 环境要求
- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ 内存
- 40GB+ 磁盘空间

### 快速部署

```bash
# 1. 克隆项目
git clone https://github.com/YOUR_USERNAME/AI-SecondBrain.git
cd AI-SecondBrain

# 2. 配置环境变量
cp .env.example .env
# 编辑.env文件，配置API密钥和密码

# 3. 启动服务
docker-compose up -d

# 4. 访问应用
# 浏览器打开：http://localhost
```

## 📖 文档

- [部署指南](DEPLOYMENT_GUIDE.md)
- [项目架构](competition_docs/04_项目架构总览.md)
- [需求分析](competition_docs/01_需求分析.md)
- [概要设计](competition_docs/02_概要设计.md)
- [详细设计](competition_docs/03_详细设计.md)
- [测试报告](competition_docs/05_测试报告.md)
- [安装指南](competition_docs/06_安装及使用指南.md)
- [项目总结](competition_docs/07_项目总结.md)

## 📄 许可证

MIT License

## 👥 贡献

欢迎提交Issue和Pull Request！

## 📞 联系方式

- Email: your-email@example.com
- GitHub: https://github.com/YOUR_USERNAME

## 🙏 致谢

感谢以下开源项目：
- Spring Boot
- Vue.js
- Element Plus
- Elasticsearch
- Redis
- Kafka
- 通义千问
