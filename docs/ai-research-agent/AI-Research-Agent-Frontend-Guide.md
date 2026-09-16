现在开始设计 AI Research Workspace 前端。

必须先加载：

D:\AI-SecondBrain\.claude\skills\taste-skill

然后加载：

D:\AI-SecondBrain\.claude\skills\ui-ux-pro-max

代码注释必须遵循：

D:\AI-SecondBrain\.claude\skills\code-comment-rules

技术栈：

Vue 3
Element Plus

设计目标：

不是传统后台管理系统。

不要使用大量传统 Card + Table + Form。

Research Workspace 必须具有：

沉浸式
专业
AI Research
知识探索
研究过程可视化

核心页面：

/ai-research

页面包含：

1. Research Project 列表
2. 新建 Research Project
3. Research Workspace
4. Research Timeline
5. Research Plan
6. Knowledge Context
7. Knowledge Gap
8. Research Sources
9. Research Findings
10. Research Conclusion
11. Research Report
12. Knowledge Candidates

Research Workspace 推荐布局：

左侧：
Research Navigation

中间：
Research Content

右侧：
Context Panel

底部：
AI Agent Interaction

左侧显示：

Research Goal
Knowledge Background
Knowledge Gap
Research Plan
Research Tasks
Sources
Findings
Conclusion
Report

中间显示：

当前研究内容。

右侧显示：

My Knowledge
Related Knowledge
Knowledge Graph
External Sources
Agent Status

底部显示：

AI Research Agent 输入框。

必须支持：

- Agent 实时状态
- Research Progress
- Task Progress
- Tool Calling 状态
- Source 展示
- Citation
- Agent 思考过程的摘要展示
- 用户暂停
- 用户继续
- 用户重新执行
- 用户确认知识沉淀

注意：

不要暴露模型内部 Chain-of-Thought。

只展示：

Agent 当前阶段
Agent 当前任务
Tool 执行状态
研究摘要
研究结果

不要展示模型隐式思维链。

前端设计必须优先考虑：

视觉层次
信息密度
可读性
研究沉浸感
长时间使用体验

完成后保存 UI 设计说明：

docs/design/ai-research-workspace-ui.md

然后实现页面。
