const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const context = await browser.newContext({ serviceWorkers: 'block', viewport: { width: 1280, height: 1000 } });
    await context.addInitScript(() => localStorage.setItem('feynman_token', 'quiz-test'));
    const items = [{ id: 'quiz-note', title: '闭包' }, { id: 'quiz-note-2', title: '词法作用域' }].map((item) => ({ ...item, content: '函数保留词法环境。', tags: [], reviewScore: null, updatedAt: new Date().toISOString() }));
    const generated = [];
    const submissions = [];
    const quizzes = new Map();
    let failGrade = false;
    let releaseGeneration;
    let generationEntered;
    let holdGeneration = false;
    let gradingScore;
    const reads = new Map();
    await context.route('**/api/**', async (route) => {
      const path = new URL(route.request().url()).pathname;
      if (route.request().method() === 'GET') reads.set(path, (reads.get(path) || 0) + 1);
      let json = {};
      if (path.endsWith('/auth/me')) json = { user: { username: 'quiz-test' } };
      else if (path.endsWith('/knowledge')) json = { items };
      else if (path.endsWith('/knowledge/quiz-note')) json = { item: items[0] };
      else if (path.includes('/attempts/')) json = { attempts: [] };
      else if (path.endsWith('/graph/knowledge-map')) json = {
        nodes: items.map((item) => ({ id: item.id, label: item.title, excerpt: item.content, tags: item.tags, score: item.reviewScore })),
        links: [], stats: { nodeCount: 2, linkCount: 0, tagCount: 0 }
      };
      else if (path.endsWith('/quiz/generate')) {
        const body = route.request().postDataJSON();
        generated.push(body);
        const quizId = `q${generated.length}`;
        const quiz = {
          quizId, ...body, question: body.questionType === 'short-answer' ? '**解释闭包**，并举例。' : '以下哪项描述闭包？',
          options: body.questionType === 'short-answer' ? [] : ['保留词法环境', '图片', '数据库', '网络'],
          difficultyLabel: { easy: '基础', medium: '进阶', hard: '挑战' }[body.difficulty], provider: 'openai-compatible'
        };
        quizzes.set(quizId, quiz);
        if (holdGeneration) await new Promise((resolve) => { releaseGeneration = resolve; generationEntered(); });
        json = { quiz, provider: quiz.provider };
      } else if (path.endsWith('/quiz/grade')) {
        const body = route.request().postDataJSON();
        submissions.push(body);
        if (failGrade) {
          failGrade = false;
          await route.fulfill({ status: 502, json: { message: '评分服务暂时不可用' } });
          return;
        }
        const quiz = quizzes.get(body.quizId);
        const subjective = quiz.questionType === 'short-answer';
        const score = gradingScore ?? (subjective ? 80 : 100);
        const item = items.find((entry) => entry.id === quiz.knowledgeId);
        Object.assign(item, { reviewScore: score, lastReviewedAt: new Date().toISOString() });
        json = { result: {
          questionType: quiz.questionType, userAnswer: body.userAnswer, isCorrect: score >= 60, score,
          correctAnswer: '函数保留对词法环境的访问。', explanation: '结合定义与实例理解闭包。',
          ...(subjective ? { feedback: '核心概念准确，请补充例子。', keyPoints: ['词法环境', '举例说明'] } : { correctIndex: 0 })
        }, item, saved: true };
      }
      await route.fulfill({ json });
    });
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await page.goto(`${process.env.QUIZ_TEST_URL || 'http://127.0.0.1:5175'}/knowledge/quiz-note`);
    await page.getByText('未学习', { exact: true }).waitFor();
    await page.getByRole('link', { name: '知识图谱', exact: true }).first().click();
    await page.getByRole('button', { name: '查看 闭包', exact: true }).waitFor();
    await page.getByRole('link', { name: '智能测验', exact: true }).first().click();
    const type = page.getByLabel('题目类型');
    const knowledge = page.getByLabel('练习哪个知识点？');
    const generate = page.getByRole('button', { name: '生成一道题' });
    const submit = page.getByRole('button', { name: '提交答案' });
    for (const questionType of ['multiple-choice', 'short-answer']) {
      await type.selectOption(questionType);
      for (const difficulty of ['easy', 'medium', 'hard']) {
        await page.locator(`input[name="difficulty"][value="${difficulty}"]`).check();
        assert.equal(await page.getByRole('status').count(), 0);
        await generate.click();
        await submit.waitFor();
        assert.deepEqual(generated.at(-1), { knowledgeId: 'quiz-note', difficulty, questionType });
        assert.equal(await page.getByRole('heading', { name: '参考答案', exact: true }).count(), 0);
        assert.equal(await submit.isDisabled(), true);
        if (questionType === 'short-answer') {
          const answer = page.getByLabel('你的解答');
          await answer.fill('   ');
          assert.equal(await submit.isDisabled(), true);
          await answer.fill('闭包保留词法环境，例如内部函数访问外部变量。');
          if (difficulty === 'easy') {
            failGrade = true;
            await submit.click();
            await page.getByRole('alert').filter({ hasText: '评分服务暂时不可用' }).waitFor();
            assert.match(await answer.inputValue(), /词法环境/);
          }
        } else {
          assert.equal(await page.getByRole('textbox').count(), 0);
          await page.locator('.quiz-option').filter({ hasText: '保留词法环境' }).click();
          assert.equal(await page.getByRole('radio', { name: 'A 保留词法环境' }).isChecked(), true);
        }
        await submit.click();
        const result = page.getByRole('status');
        await result.waitFor();
        await result.getByText('测验记录和知识点学习状态已保存。', { exact: true }).waitFor();
        await result.getByText('已学习且已掌握', { exact: true }).waitFor();
        await result.getByText(questionType === 'short-answer' ? '本题得分 80 / 100' : '本题得分 100 / 100', { exact: true }).waitFor();
        assert.equal(typeof submissions.at(-1).userAnswer, questionType === 'short-answer' ? 'string' : 'number');
        if (questionType === 'short-answer') {
          await result.getByRole('heading', { name: 'AI 评价' }).waitFor();
          await result.getByRole('heading', { name: '参考答案' }).waitFor();
          assert.equal(await page.getByLabel('你的解答').isDisabled(), true);
        }
      }
    }
    await type.selectOption('multiple-choice');
    assert.equal(await page.getByRole('status').count(), 0);
    assert.equal(await page.getByLabel('你的解答').count(), 0);
    await generate.click();
    await submit.waitFor();
    await knowledge.selectOption('quiz-note-2');
    assert.equal(await page.getByRole('radiogroup', { name: '题目选项' }).count(), 0);
    holdGeneration = true;
    const generationStarted = new Promise((resolve) => { generationEntered = resolve; });
    const before = generated.length;
    await generate.click();
    await page.getByRole('button', { name: '出题中…' }).waitFor();
    assert.equal(await type.isDisabled(), true);
    assert.equal(await knowledge.isDisabled(), true);
    assert.equal(await page.locator('input[name="difficulty"]').first().isDisabled(), true);
    await generationStarted;
    releaseGeneration();
    await submit.waitFor();
    assert.equal(generated.length, before + 1);
    holdGeneration = false;
    await type.selectOption('short-answer');
    await page.setViewportSize({ width: 390, height: 844 });
    await generate.click();
    await page.getByLabel('你的解答').fill('闭包使内部函数访问外部词法环境。');
    await submit.click();
    await page.getByRole('status').waitFor();
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true);
    await page.setViewportSize({ width: 1280, height: 1000 });
    for (const [questionType, score] of [['multiple-choice', 0], ['multiple-choice', 100], ['short-answer', 59], ['short-answer', 60]]) {
      const detailReads = reads.get('/api/knowledge/quiz-note') || 0;
      const graphReads = reads.get('/api/graph/knowledge-map') || 0;
      gradingScore = score;
      await page.getByRole('link', { name: '我的知识', exact: true }).first().click();
      await page.getByRole('link', { name: '查看 闭包 的详情', exact: true }).waitFor();
      await page.getByRole('link', { name: '智能测验', exact: true }).first().click();
      await type.selectOption(questionType);
      await generate.click();
      if (questionType === 'short-answer') await page.getByLabel('你的解答').fill('函数可持续访问创建时的词法环境。');
      else await page.locator('.quiz-option').first().click();
      await submit.click();
      const status = score >= 60 ? '已学习且已掌握' : '已学习但未掌握';
      await page.getByRole('status').getByText(status, { exact: true }).waitFor();
      await page.getByRole('link', { name: '我的知识', exact: true }).first().click();
      const note = page.getByRole('link', { name: '查看 闭包 的详情', exact: true });
      await note.getByText(status, { exact: true }).waitFor();
      await note.click();
      await page.getByText(status, { exact: true }).waitFor();
      assert.ok(reads.get('/api/knowledge/quiz-note') > detailReads);
      await page.getByRole('link', { name: '知识图谱', exact: true }).first().click();
      await page.getByRole('button', { name: '查看 闭包', exact: true }).click();
      await page.locator('.graph-detail-card .detail-score strong').getByText(String(score), { exact: true }).waitFor();
      assert.ok(reads.get('/api/graph/knowledge-map') > graphReads);
    }
    await page.reload();
    await page.getByRole('button', { name: '查看 闭包', exact: true }).click();
    await page.locator('.graph-detail-card .detail-score strong').getByText('60', { exact: true }).waitFor();
    assert.deepEqual(errors, []);
    console.log('PASS: 六种题型难度组合、失败重试、设置锁定、移动布局、0/59/60/100学习状态、列表详情图谱缓存刷新及重新加载');
    await context.close();
  } finally { await browser.close(); }
})().catch((error) => { console.error(error); process.exitCode = 1; });
