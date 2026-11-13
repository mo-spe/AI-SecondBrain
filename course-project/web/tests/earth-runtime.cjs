const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true, args: ['--enable-unsafe-swiftshader'] });
  try {
    const context = await browser.newContext({ serviceWorkers: 'block', viewport: { width: 1280, height: 900 } });
    await context.addInitScript(() => localStorage.setItem('feynman_token', 'earth-runtime-test'));
    const item = { id: 'earth-note', title: '地球测试', content: '测试节点', tags: [], reviewScore: 60, updatedAt: new Date().toISOString() };
    await context.route('**/api/**', async (route) => {
      const path = new URL(route.request().url()).pathname;
      const json = path.endsWith('/auth/me') ? { user: { username: 'performance' } }
        : path.endsWith('/graph/knowledge-map') ? { nodes: [{ id: item.id, label: item.title, excerpt: item.content, tags: [], score: 60 }], links: [], stats: { nodeCount: 1, linkCount: 0, tagCount: 0 } }
        : { items: [item] };
      await route.fulfill({ json });
    });
    const page = await context.newPage();
    const errors = [];
    const runtimeRequests = [];
    page.on('pageerror', (error) => errors.push(error.message));
    page.on('request', (request) => { if (request.url().endsWith('/cesium/Cesium.js')) runtimeRequests.push(request.url()); });
    await page.goto(`${process.env.EARTH_TEST_URL || 'http://127.0.0.1:4175'}/knowledge`);
    await page.getByRole('heading', { name: '我的知识。', exact: true }).waitFor();
    await page.waitForLoadState('networkidle');
    assert.equal(runtimeRequests.length, 0);
    const earthLink = page.getByRole('link', { name: '数字地球', exact: true }).first();
    await earthLink.click();
    await page.getByRole('heading', { name: '把知识放上地球。', exact: true }).waitFor();
    await page.locator('.cesium-viewer canvas').waitFor({ timeout: 30000 });
    assert.equal(runtimeRequests.length, 1);
    assert.equal(await page.evaluate(() => Boolean(window.Cesium?.Viewer)), true);
    await page.getByRole('link', { name: '我的知识', exact: true }).first().click();
    await page.getByRole('heading', { name: '我的知识。', exact: true }).waitFor();
    assert.equal(await page.locator('.cesium-viewer').count(), 0);
    await earthLink.click();
    await page.locator('.cesium-viewer canvas').waitFor();
    assert.equal(runtimeRequests.length, 1);
    assert.deepEqual(errors, []);
    await context.close();
    const failureContext = await browser.newContext({ serviceWorkers: 'block' });
    await failureContext.addInitScript(() => localStorage.setItem('feynman_token', 'earth-runtime-test'));
    await failureContext.route('**/api/**', (route) => route.fulfill({ json: route.request().url().endsWith('/auth/me') ? { user: { username: 'test' } } : { nodes: [], links: [], stats: {} } }));
    await failureContext.route('**/cesium/Cesium.js', (route) => route.abort());
    const failurePage = await failureContext.newPage();
    await failurePage.goto(`${process.env.EARTH_TEST_URL || 'http://127.0.0.1:4175'}/earth`);
    await failurePage.getByRole('heading', { name: '数字地球暂时无法打开' }).waitFor();
    assert.equal(await failurePage.getByRole('link', { name: '我的知识', exact: true }).first().isVisible(), true);
    await failureContext.close();
    console.log('PASS: 普通页面不加载Cesium、数字地球按需加载且正常创建画布、重进复用引擎、资源失败保留导航');
  } finally { await browser.close(); }
})().catch((error) => { console.error(error); process.exitCode = 1; });
