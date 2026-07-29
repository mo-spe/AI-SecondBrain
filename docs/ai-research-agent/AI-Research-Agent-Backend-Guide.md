现在开始实现 AI Research Agent Backend。

请先读取：

1. CLAUDE.md
2. docs/ai-research-agent/architecture/ai-research-agent-architecture.md
3. docs/ai-research-agent/architecture/ai-research-agent-database.md
4. docs/ai-research-agent/architecture/ai-research-agent-workflow.md
5. docs/ai-research-agent/architecture/ai-research-agent-tools.md

同时必须严格遵循项目 Skills。

Backend 开发前必须加载：

D:\AI-SecondBrain\.claude\skills\java-coding-standards

代码注释必须遵循：

D:\AI-SecondBrain\.claude\skills\code-comment-rules

项目已有规范优先于你自己的默认规范。

请先分析现有 Backend。

不要直接生成大量代码。

第一步只实现：

Research Project 模块。

包含：

Entity
DTO
VO
Mapper
Service
ServiceImpl
Controller

实现：

1. 创建研究项目
2. 查询研究项目
3. 查询项目详情
4. 更新研究项目
5. 删除研究项目
6. 启动研究
7. 暂停研究
8. 恢复研究
9. 归档研究

必须：

- 遵循已有项目分层结构
- 遵循已有统一返回结构
- 遵循已有异常处理
- 遵循已有权限体系
- 遵循已有用户/Workspace 隔离
- 遵循已有分页规范

不要自行引入新的框架。

Entity 必须使用 Lombok：

@Getter
@Setter

禁止手写 Getter / Setter。

完成后：

1. 编译
2. 运行测试
3. 检查 SQL
4. 检查 API
5. 检查代码规范

然后暂停。

不要继续开发下一个模块。
