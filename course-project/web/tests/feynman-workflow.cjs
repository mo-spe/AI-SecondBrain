const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true, args: ['--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream'] });
  try {
    const context = await browser.newContext({ serviceWorkers: 'block', viewport: { width: 1280, height: 1000 } });
    await context.addInitScript(() => localStorage.setItem('feynman_token', 'workflow-test'));
    const item = { id: 'feynman-test', title: '闭包', content: '词法作用域', tags: [], reviewScore: null, updatedAt: new Date().toISOString() };
    let score = 59;
    let failPolish = true;
    const evaluationBodies = [];
    await context.route('**/api/**', async (route) => {
      const path = new URL(route.request().url()).pathname;
      let json = {};
      if (path.endsWith('/auth/me')) json = { user: { username: 'workflow' } };
      else if (path.endsWith('/knowledge')) json = { items: [item] };
      else if (path.endsWith('/knowledge/feynman-test')) json = { item };
      else if (path.endsWith('/transcribe')) json = { transcript: '嗯，闭包就是记着外面的变量', provider: 'baidu-asr' };
      else if (path.endsWith('/polish')) {
        if (failPolish) { failPolish = false; await route.fulfill({ status: 502, json: { message: '润色服务暂时不可用' } }); return; }
        json = { polishedText: '闭包允许函数保留对词法作用域的访问。', provider: 'openai-compatible' };
      } else if (path.endsWith('/evaluate')) {
        const body = route.request().postDataJSON();
        evaluationBodies.push(body);
        assert.ok(body.polishedText.startsWith('闭包允许函数'));
        assert.ok(body.transcript.startsWith('嗯'));
        assert.ok(body.clientAttemptId);
        assert.equal(body.score, undefined);
        item.reviewScore = score;
        json = { score, evaluation: '反馈内容', strengths: ['结构清楚'], weaknesses: ['补充例子'], provider: 'openai-compatible', saved: true, item };
      } else if (path.includes('/attempts/')) json = { attempts: [] };
      await route.fulfill({ json });
    });
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await page.goto('http://127.0.0.1:5175/feynman?knowledgeId=feynman-test');
    await page.getByRole('button', { name: '开始录音', exact: true }).click();
    await page.getByText(/正在聆听… 1s/).waitFor();
    assert.equal(await page.getByLabel('今天要讲什么？').isDisabled(), true);
    await page.getByRole('button', { name: '停止录音' }).click();
    await page.getByRole('button', { name: '上传并转成文字' }).click();
    const original = page.getByLabel('识别后的复述原文');
    await original.waitFor();
    await page.getByRole('button', { name: 'AI 专业润色' }).click();
    await page.getByRole('alert').filter({ hasText: '润色服务暂时不可用' }).waitFor();
    assert.match(await original.inputValue(), /外面的变量/);
    await page.getByRole('button', { name: 'AI 专业润色' }).click();
    const polished = page.getByLabel('润色后的复述');
    await polished.waitFor();
    for (const value of [59, 60, 61]) {
      score = value;
      await polished.fill(`闭包允许函数保留对词法作用域的访问。版本${value}`);
      await page.getByRole('button', { name: 'AI 评价润色后的复述' }).click();
      const status = page.getByRole('status').filter({ hasText: '复述记录和知识点学习状态已保存' });
      await status.waitFor();
      await status.getByText(value >= 60 ? '已学习且已掌握' : '已学习但未掌握', { exact: true }).waitFor();
      assert.equal(await page.getByRole('button', { name: '本次评价已保存' }).isDisabled(), true);
    }
    assert.equal(new Set(evaluationBodies.map((body) => body.clientAttemptId)).size, 3);
    await original.fill('嗯，我重新解释一下');
    assert.equal(await polished.count(), 0);
    assert.equal(await page.getByRole('button', { name: '本次评价已保存' }).count(), 0);
    await page.getByRole('link', { name: '我的知识', exact: true }).first().click();
    await page.getByText('已学习且已掌握', { exact: true }).waitFor();
    await page.reload();
    await page.getByText('已学习且已掌握', { exact: true }).waitFor();
    await page.getByRole('link', { name: '查看详情', exact: true }).click();
    await page.getByText('已学习且已掌握', { exact: true }).waitFor();
    assert.deepEqual(errors, []);
    console.log('PASS: fake-device录音、转录、润色失败重试、评价润色文本、59/60/61状态、编辑失效、不同评价编号、列表详情和刷新状态');
    await context.close();
  } finally { await browser.close(); }
})().catch((error) => { console.error(error); process.exitCode = 1; });
