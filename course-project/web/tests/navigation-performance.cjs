const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  let releaseHistories;
  let releaseGraph;
  try {
    const context = await browser.newContext({ serviceWorkers: 'block', viewport: { width: 1280, height: 900 } });
    await context.addInitScript(() => localStorage.setItem('feynman_token', 'performance-test'));
    const historyGate = new Promise((resolve) => { releaseHistories = resolve; });
    const graphGate = new Promise((resolve) => { releaseGraph = resolve; });
    let historyFinished = false;
    let graphImportStarted;
    const graphImport = new Promise((resolve) => { graphImportStarted = resolve; });
    let graphReads = 0;
    const item = { id: 'perf-note', title: '先读正文', content: '正文应该独立于历史请求显示。', tags: [], reviewScore: null, updatedAt: new Date().toISOString() };
    await context.route('**/api/**', async (route) => {
      const path = new URL(route.request().url()).pathname;
      let json = {};
      if (path.endsWith('/auth/me')) json = { user: { username: 'performance' } };
      else if (path.endsWith('/knowledge/perf-note')) json = { item };
      else if (path.endsWith('/knowledge')) json = { items: [item] };
      else if (path.includes('/attempts/')) {
        await historyGate;
        historyFinished = true;
        if (path.includes('/feynman/')) {
          await route.fulfill({ status: 500, json: { message: '历史暂不可用' } });
          return;
        }
        json = { attempts: [] };
      } else if (path.endsWith('/graph/knowledge-map')) {
        graphReads++;
        json = { nodes: [], links: [], stats: {} };
      }
      await route.fulfill({ json });
    });
    await context.route('**/src/pages/GraphPage.jsx*', async (route) => {
      graphImportStarted();
      await graphGate;
      await route.continue();
    });
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await page.goto(`${process.env.NAVIGATION_TEST_URL || 'http://127.0.0.1:5175'}/knowledge/perf-note`);
    await page.getByRole('heading', { name: item.title, exact: true }).waitFor();
    assert.equal(historyFinished, false);
    await page.getByText(item.content, { exact: true }).waitFor();
    assert.equal(await page.getByText('还没有复述记录。', { exact: true }).count(), 0);
    releaseHistories();
    await page.getByRole('alert').filter({ hasText: '历史记录加载失败' }).waitFor();
    await page.getByText('还没有测验记录。', { exact: true }).waitFor();
    await page.getByRole('heading', { name: item.title, exact: true }).waitFor();
    const graphResponse = page.waitForResponse((response) => response.url().endsWith('/api/graph/knowledge-map'));
    const graphLink = page.getByRole('link', { name: '知识图谱', exact: true }).first();
    await graphLink.hover();
    await Promise.all([graphResponse, graphImport]);
    assert.equal(graphReads, 1);
    await graphLink.click();
    await page.getByText('正在打开学习模块…', { exact: true }).waitFor();
    assert.equal(await graphLink.isVisible(), true);
    const knowledgeLink = page.getByRole('link', { name: '我的知识', exact: true }).first();
    assert.equal(await knowledgeLink.isVisible(), true);
    await knowledgeLink.click();
    await page.getByRole('heading', { name: '我的知识。', exact: true }).waitFor();
    releaseGraph();
    await graphLink.click();
    await page.getByRole('heading', { name: '看见知识之间。', exact: true }).waitFor();
    assert.equal(graphReads, 1);
    assert.deepEqual(errors, []);
    console.log('PASS: 历史未返回时正文可读、历史失败独立提示、路由代码延迟期间导航可用、意图预取并行且请求去重');
    await context.close();
  } finally {
    releaseHistories?.();
    releaseGraph?.();
    await browser.close();
  }
})().catch((error) => { console.error(error); process.exitCode = 1; });
