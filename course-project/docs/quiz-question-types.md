# 智能测验：选择题与解答题

更新日期：2026-11-13

## 功能与操作

智能测验支持“基础／进阶／挑战”三档难度，以及“选择题／解答题”两种题型。用户先选择自己的知识点、难度和题型，再生成一道题。

- 选择题：四选一，答对100分、答错0分，提交后展示正确答案与解析。
- 解答题：输入1至5000字符的非空解答，由真实 AI 评阅，返回0至100整数分、评价、参考答案、评分要点与解析。60分及以上显示本题达标。
- 切换知识点、难度或题型时清空旧题、答案和结果；出题与判分期间锁定设置，防止串题和重复点击。
- 空白解答不能提交。评分失败保留输入，可重新提交；成功后本页锁定答案，点击“再来一题”开始新练习。

选择题与解答题完成评分并保存后，都会同步更新对应知识点的学习状态：60分及以上为“已学习且已掌握”，低于60分为“已学习但未掌握”。选择题答对100分、答错0分，分别对应这两种状态。测验和费曼复述共用最近掌握度，以最近保存的评价记录时间为准，后续低分练习可使已掌握状态变为待巩固。

测验结果区直接展示服务端返回的知识点状态。列表、详情、概览及图谱相关缓存同时失效，切换页面即可读取新数据。尚未完成评分或评分／答题记录保存失败时，不改变知识点状态。

## 实现与数据流

复用现有 `QuizPage`、测验路由、测验 Service、共享 AI Client、`QuizQuestion` 和 `QuizAttempt`，没有另建一套出题模块。

出题时，服务端根据知识点和难度请求 AI，验证结果后保存题目、参考答案与评分要点。前端只收到题干、题型、难度和选择题选项，不提前收到正确答案、参考答案、解析或评分要点。

提交时，服务端根据题目ID重新查询当前用户的题目，检查30分钟有效期及知识点归属。选择题按保存的索引判分；解答题按出题时保存的题干、参考答案与评分要点评阅，避免知识点后续编辑改变判分依据。成功后先保存答题记录，再用记录分数和创建时间更新知识点的 `reviewScore`、`lastReviewedAt`，最后重新读取知识点并返回 `item` 与 `saved: true`。

同一道题使用题目ID作为答题记录ID，复用 MongoDB 现有主键唯一约束，不新增索引。同答案重试复用已存分数与评价；已提交其他答案则返回409，需生成新题。记录已保存但状态更新失败时返回错误，同答案重试可补写状态，无需再次AI评分。状态更新带复习时间条件，旧记录重试不会覆盖较新的测验或费曼复述结果。两次写入没有使用数据库事务，失败恢复依靠幂等重试；失败后需重试提交才能完成状态同步。

解答题的题干、参考答案、评价与解析复用安全的 Markdown 渲染组件，支持已有公式渲染能力。AI 输出以 JSON 解析并校验必填字段、分数范围和类型；错误输出不会写成答题记录。提示词要求只依据给定内容、允许同义表达与部分得分，并将学生答案视为数据。

## 配置与兼容

使用现有 `server/.env` 中的 `AI_PROVIDER`、`AI_BASE_URL`、`AI_API_KEY`、`AI_CHAT_MODEL`，无需增加密钥配置。解答题调用真实模型出题和评分；未配置时返回503，不使用规则假装 AI 主观评分。选择题保留既有 Mock 兜底。

数据模型新增 `questionType`，默认 `multiple-choice`，旧选择题记录无需迁移。原有数字型 `userAnswer` 保留，解答题文本另存 `userTextAnswer`；新增参考答案、要点、分数、反馈和评分Provider字段。知识点状态继续由已有分数派生，没有增加独立状态列。本次没有批量改写旧数据、删除集合或新增索引；状态同步从更新后的新提交开始生效，不追溯历史答题记录。

更新部署时需要同步前后端：本地 `npm run dev` 后端通常会自动重载；生产单容器、云服务或 Electron 中内置的前端需要重新构建并部署。

## API

接口前缀 `/api`，请求头使用 `Authorization: Bearer <token>` 与 `Content-Type: application/json`。错误响应为 `{ "message": "错误说明" }`。现有 Apifox 导入文件 `feynman-course-apifox-openapi.json` 已同步题型和答案类型。

### 生成题目

`POST /api/quiz/generate`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| knowledgeId | string | 是 | 当前用户知识点ID |
| difficulty | string | 否 | easy、medium、hard，默认medium |
| questionType | string | 否 | multiple-choice、short-answer，默认multiple-choice |

```json
{
  "knowledgeId": "当前知识点ID",
  "difficulty": "medium",
  "questionType": "short-answer"
}
```

成功返回 `{ "quiz": { ... }, "provider": "..." }`，其中 `quiz` 包括 `quizId`、`questionType`、`question`、`options`、`difficulty`、`difficultyLabel`、`knowledgeId`、`knowledgeTitle`、`provider`。解答题 `options` 为空数组。

### 提交答案

`POST /api/quiz/grade`

```json
{
  "quizId": "上一步返回的quizId",
  "userAnswer": "闭包允许函数在创建它的词法环境之外继续访问该环境中的变量。例如返回的内部函数仍能读取外部函数的局部变量。"
}
```

选择题的 `userAnswer` 必须是0至3整数；解答题必须是文本。客户端无需提交题型、参考答案或自评分数，服务端按保存的题目决定评分方式。

成功返回 `{ "result": { ... }, "attempt": { ... }, "provider": "...", "item": { ... }, "saved": true }`。`item` 是重新读取的知识点，含 `reviewScore`、`lastReviewedAt`、`isLearned`、`isMastered` 和 `learningStatus`。若这是旧题重试且知识点已有更新的复习记录，`result.score` 保留本题分数，`item` 保留最新知识点状态。

| result字段 | 说明 |
| --- | --- |
| questionType | 实际题型 |
| isCorrect | 选择题是否正确；解答题是否达到60分 |
| score | 本题得分 |
| userAnswer | 实际提交的数字选项或去首尾空白后的文本 |
| correctAnswer | 正确选项文本或参考答案 |
| explanation | 解析 |
| correctIndex | 仅选择题：正确选项下标 |
| feedback、keyPoints | 仅解答题：AI反馈与评分要点 |
| provider | 实际判分Provider |

`GET /api/quiz/attempts/:knowledgeId` 继续返回当前用户该知识点最近20条测验记录。新记录包含题型与分数；解答题另含文本答案和反馈，旧记录保持可读取。

### 错误处理

| HTTP状态 | 场景 |
| --- | --- |
| 400 | 不支持的难度或题型、答案类型错误、空白或超长解答 |
| 401 | 未登录或登录态无效 |
| 404 | 题目或知识点不存在，或不属于当前用户 |
| 409 | 同一道题已提交不同答案，需重新生成题目 |
| 410 | 题目已超过有效期，需重新生成 |
| 502 | AI请求失败、格式缺失或分数无效，可保留输入重试 |
| 503 | 解答题所需的真实AI未配置 |

## 验证

后端测试 `server/tests/quiz-types.test.js` 通过真实 Express 路由与 Mongoose 模型验证，隔离数据库和上游 AI。覆盖两种题型×三档难度、隐藏答案、历史选择题兼容、空白及超长输入、无效JSON结构、无效分数、0／59／60／61／100分状态、AI失败重试、过期题目和归属检查。新增验证同题重试及并发只保存一条记录、换答案409、状态写入失败后修复、客户端伪造分数无效，以及旧记录不覆盖新复习状态。费曼流程回归测试同时通过。

浏览器测试 `web/tests/quiz-types.cjs` 使用 Edge 与隔离 API，覆盖六种生成组合、实际点击选项、文本作答、空白拦截、评分失败重试、结果与参考答案展示、切换设置重置、请求期间锁定，以及390像素移动端无横向溢出。新增验证0／59／60／100分的状态标签，预先缓存页面后完成测验，列表、详情与图谱重新读取成绩，刷新后仍显示最新分数。

前端生产构建通过。以上自动验证不调用真实 AI、不写入 Atlas；实际模型的出题质量、分数一致性仍需要用已有 Provider 配置人工试题确认。

复验命令：

```powershell
# 在 course-project/server 下
node --test tests/quiz-types.test.js

# 在 course-project/web 下，另一个终端启动临时开发服务
node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5175 --strictPort
# 已安装Playwright时，在web目录运行；否则将PLAYWRIGHT_MODULE设为可用模块的绝对路径
node tests/quiz-types.cjs
node node_modules/vite/bin/vite.js build
```
