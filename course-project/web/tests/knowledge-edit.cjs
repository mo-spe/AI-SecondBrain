const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, serviceWorkers: 'block' });
    await context.addInitScript(() => localStorage.setItem('feynman_token', 'editor-test-token'));
    let item = { id: 'edit-test-note', title: '原始标题', content: '原始理解', tags: ['React'], updatedAt: '2026-10-01T00:00:00Z', reviewScore: 80 };
    let putCount = 0;
    let failNext = false;
    let releaseSave;
    let signalSave;
    let holdSave = false;
    await context.route('**/api/**', async (route) => {
      const request = route.request();
      const path = new URL(request.url()).pathname;
      let body;
      if (path === '/api/auth/me') body = { user: { id: 'editor-test-user', name: '编辑测试', email: 'editor@example.invalid' } };
      else if (path === '/api/knowledge') body = { items: [item] };
      else if (path === '/api/knowledge/edit-test-note' && request.method() === 'PUT') {
        putCount += 1;
        if (holdSave) await new Promise((resolve) => { releaseSave = resolve; signalSave(); });
        if (failNext) {
          failNext = false;
          await route.fulfill({ status: 503, json: { message: '测试保存失败，请重试' } });
          return;
        }
        item = { ...item, ...request.postDataJSON(), updatedAt: '2026-10-30T00:00:00Z' };
        body = { item };
      } else if (path === '/api/knowledge/edit-test-note') body = { item };
      else if (path.includes('/attempts/')) body = { attempts: [] };
      else body = {};
      await route.fulfill({ json: body });
    });
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await page.goto(process.env.TEST_BASE_URL || 'http://localhost:5173/knowledge');
    await page.waitForLoadState('networkidle');
    await page.getByRole('link', { name: '编辑 原始标题', exact: true }).click();
    const dialog = page.getByRole('dialog', { name: '编辑知识点' });
    await dialog.waitFor();
    assert.equal(await dialog.getByLabel('标题', { exact: true }).inputValue(), '原始标题');
    assert.equal(await dialog.getByLabel('用自己的话解释').inputValue(), '原始理解');
    assert.equal(await dialog.getByRole('button', { name: '保存修改' }).isDisabled(), true);
    await dialog.getByLabel('标题', { exact: true }).fill('   ');
    await dialog.getByRole('button', { name: '保存修改' }).click();
    await dialog.getByRole('alert').filter({ hasText: '标题和内容不能为空' }).waitFor();
    assert.equal(putCount, 0);
    await dialog.getByLabel('标题', { exact: true }).fill('更新后的标题');
    await dialog.getByLabel('用自己的话解释').fill('补充后的完整理解');
    await dialog.getByLabel('标签', { exact: true }).fill('1,2,3,4,5,6,7,8,9');
    await dialog.getByRole('button', { name: '保存修改' }).click();
    await dialog.getByRole('alert').filter({ hasText: '最多添加 8 个标签' }).waitFor();
    assert.equal(putCount, 0);
    await dialog.getByLabel('标签', { exact: true }).fill('React， 缓存,React');
    failNext = true;
    await dialog.getByRole('button', { name: '保存修改' }).click();
    await dialog.getByRole('alert').filter({ hasText: '测试保存失败' }).waitFor();
    assert.equal(await dialog.getByLabel('标题', { exact: true }).inputValue(), '更新后的标题');
    holdSave = true;
    const saveStarted = new Promise((resolve) => { signalSave = resolve; });
    await dialog.getByRole('button', { name: '保存修改' }).click();
    await page.waitForFunction(() => document.querySelector('form[aria-busy="true"]'));
    assert.equal(await dialog.getByLabel('标题', { exact: true }).isDisabled(), true);
    assert.equal(await dialog.getByRole('button', { name: '取消' }).first().isDisabled(), true);
    await dialog.locator('form').evaluate((form) => {
      form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    });
    await saveStarted;
    releaseSave();
    await dialog.waitFor({ state: 'hidden' });
    assert.equal(putCount, 2);
    assert.deepEqual(item.tags, ['React', '缓存']);
    await page.getByRole('status').filter({ hasText: '知识点已更新' }).waitFor();
    assert.equal(await page.getByRole('heading', { level: 1 }).textContent(), '更新后的标题');
    await page.getByRole('link', { name: '返回知识库' }).click();
    await page.getByRole('heading', { name: '更新后的标题' }).waitFor();
    await page.getByRole('link', { name: '编辑 更新后的标题', exact: true }).click();
    await dialog.waitFor();
    await dialog.getByLabel('用自己的话解释').fill('准备放弃的内容');
    page.once('dialog', (prompt) => prompt.dismiss());
    await page.keyboard.press('Escape');
    assert.equal(await dialog.isVisible(), true);
    page.once('dialog', (prompt) => prompt.accept());
    await page.keyboard.press('Escape');
    await dialog.waitFor({ state: 'hidden' });
    await page.getByRole('button', { name: '编辑', exact: true }).click();
    await dialog.waitFor();
    await dialog.getByLabel('标签', { exact: true }).fill('');
    holdSave = false;
    await dialog.getByRole('button', { name: '保存修改' }).click();
    await dialog.waitFor({ state: 'hidden' });
    assert.deepEqual(item.tags, []);
    await page.reload();
    await page.getByRole('heading', { name: '更新后的标题', exact: true }).waitFor();
    for (const viewport of [{ width: 375, height: 812 }, { width: 812, height: 375 }]) {
      await page.setViewportSize(viewport);
      await page.getByRole('button', { name: '编辑', exact: true }).click();
      await dialog.waitFor();
      assert.equal(await dialog.evaluate((element) => element.scrollWidth <= element.clientWidth + 1), true);
      await dialog.getByRole('button', { name: '取消' }).first().click();
    }
    assert.deepEqual(errors, []);
    let releaseOld;
    let signalOld;
    const oldStarted = new Promise((resolve) => { signalOld = resolve; });
    let cacheRequests = 0;
    await page.route('**/api/cache-regression', async (route) => {
      cacheRequests += 1;
      if (cacheRequests === 1) {
        await new Promise((resolve) => { releaseOld = resolve; signalOld(); });
        await route.fulfill({ json: { version: 'old' } });
      } else await route.fulfill({ json: { version: 'new' } });
    });
    await page.evaluate(async () => {
      window.editorCacheTest = await import('/src/lib/dataCache.js');
      window.oldCacheRequest = window.editorCacheTest.cachedRequest('edit-cache-regression', '/cache-regression');
    });
    await oldStarted;
    assert.equal(await page.evaluate(async () => {
      window.editorCacheTest.invalidateApiCache('edit-cache-regression');
      return (await window.editorCacheTest.cachedRequest('edit-cache-regression', '/cache-regression')).version;
    }), 'new');
    releaseOld();
    assert.equal(await page.evaluate(async () => {
      await window.oldCacheRequest;
      return (await window.editorCacheTest.cachedRequest('edit-cache-regression', '/cache-regression')).version;
    }), 'new');
    assert.equal(cacheRequests, 2);
    console.log('PASS: prefill, validation, error/retry, duplicate-submit guard, save, list refresh, cancel, empty tags, reload, mobile/landscape.');
    console.log('PASS: invalidated in-flight request cannot overwrite fresh cached content.');
    await context.close();
  } finally {
    await browser.close();
  }
}

main().catch((error) => { console.error(error); process.exitCode = 1; });
