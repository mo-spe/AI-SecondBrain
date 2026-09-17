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

## 研究工作台 UI/UX 实现基线（2026-09-03）

研究工作台采用“编辑式研究控制台”方向，保留暖色研究书房的视觉识别，不套用通用深色数据大屏。设计参数为中等差异度、克制动效和偏高信息密度，适合长时间阅读与研究状态监控。

### 桌面布局

工作区必须使用明确的三列两行 Grid：

- 左列为研究导航，并跨越内容区和底部操作区。
- 中列第一行为研究正文，第二行为 AI Agent 操作栏。
- 右列为上下文面板，并跨越内容区和底部操作区。
- 中央正文顶部使用粘性研究指挥头，集中呈现项目状态、当前章节、任务进度、有效来源和知识候选数量。

底部操作栏不得作为横向 Flex 的第四列，否则会在中等宽度屏幕上压缩或溢出正文。

### 响应式布局

- `> 1180px`：完整三栏研究工作台。
- `901px–1180px`：收窄左右信息栏，保持中央正文可读宽度。
- `<= 900px`：左右栏变为独立抽屉，由移动工作栏显式打开；正文与操作栏占满可用宽度。
- `<= 600px`：项目列表改为纵向布局，候选操作按钮纵向排列，底部 Agent 操作分层展示。

移动抽屉使用固定的 z-index 层级和遮罩，关闭后必须完全退出视口。页面至少验证 `375×812`、`844×390` 和 `1440×1000` 三种视口，且不允许出现页面级横向滚动。

### 交互与可访问性

- 项目、来源和任务折叠入口必须支持键盘操作。
- 图标按钮必须提供明确的 `aria-label`，当前导航使用 `aria-current`。
- 导航较长时提供“跳到研究内容”的 skip link。
- 所有键盘焦点必须可见，不依赖颜色单独表达状态。
- 异步内容加载使用骨架屏；知识候选提交期间禁用按钮，避免重复操作。
- 所有动效控制在 150–300ms，并支持 `prefers-reduced-motion`。
- 移动端主要按钮触控区域不小于 44px。
- 普通正文颜色与背景对比度应达到 WCAG AA；次要信息也应保持可读，不使用过浅灰色。

### 研究内容排版

- 项目标题使用展示字体，正文和控制元素分别使用阅读字体与 UI 字体。
- 报告、结论、知识候选统一使用受控 Markdown 排版，避免候选内容回退到浏览器默认样式。
- 长正文保持约 45–75 字符的阅读宽度；数据指标紧凑呈现，不堆叠为大量装饰卡片。
- 进行中的研究只使用有目的的状态脉冲和进度动画，不添加持续性的装饰动画。
