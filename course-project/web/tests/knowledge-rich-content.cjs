const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const example = [
  '# 梯度下降',
  '',
  '**核心思想**：沿梯度反方向更新参数。',
  '',
  '- 计算梯度',
  '- 更新参数',
  '',
  '行内公式 $E=mc^2$。',
  '',
  '$$',
  '\\theta_{t+1}=\\theta_t-\\eta\\nabla J(\\theta_t)',
  '$$',
  '',
  '| 参数 | 含义 |',
  '| --- | --- |',
  '| eta | 学习率 |',
  '',
  '```js',
  'const rate = 0.1;',
  '```',
  '',
  '```mermaid',
  'flowchart LR',
  '  A[计算梯度] --> B[更新参数]',
  '```',
  '',
  '```mermaid',
  'flowchart TD',
  '  C[开始] --> D[结束]',
  '```',
  '',
  '<script>window.markdownUnsafe = true</script>',
  '<img src="x" onerror="window.markdownUnsafe = true">',
  '[危险链接](javascript:alert(1))'
].join('\n');

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, serviceWorkers: 'block' });
    await context.addInitScript(() => localStorage.setItem('feynman_token', 'rich-content-test-token'));
    let item = { id: 'rich-test', title: '富文本测试', content: example, tags: [], updatedAt: '2026-10-30T00:00:00Z' };
    const writes = [];
    await context.route('**/api/**', async (route) => {
      const request = route.request();
      const path = new URL(request.url()).pathname;
      let body = {};
      if (path === '/api/auth/me') body = { user: { id: 'rich-user', name: '渲染测试', email: 'rich@example.invalid' } };
      else if (path.includes('/attempts/')) body = { attempts: [] };
      else if (request.method() === 'PUT' || request.method() === 'POST') {
        writes.push(request.postDataJSON());
        item = { ...item, ...request.postDataJSON() };
        body = { item };
      } else if (path === '/api/knowledge') body = { items: [item] };
      else if (path === '/api/knowledge/rich-test') body = { item };
      await route.fulfill({ json: body });
    });
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await page.goto('http://localhost:5173/knowledge/rich-test');
    await page.waitForLoadState('networkidle');
    const content = page.locator('.detail-content-panel .markdown-content');
    await content.getByRole('heading', { name: '梯度下降' }).waitFor();
    assert.equal(await content.locator('strong').textContent(), '核心思想');
    assert.equal(await content.locator('li').count(), 2);
    assert.equal(await content.locator('table tbody tr').count(), 1);
    assert.equal(await content.locator('pre code.language-js').textContent(), 'const rate = 0.1;\n');
    assert.equal(await content.locator('.katex').count(), 2);
    assert.equal(await content.locator('.katex-display').count(), 1);
    await content.locator('.mermaid-svg').nth(1).waitFor({ timeout: 60000 });
    assert.equal(await content.locator('.mermaid-svg svg').count(), 2);
    assert.equal(await content.locator('.mermaid-svg svg').first().evaluate((svg) => svg.getBoundingClientRect().height > 30), true);
    if (process.env.TEST_SCREENSHOT) await page.screenshot({ path: process.env.TEST_SCREENSHOT, fullPage: true });
    const ids = await content.locator('.mermaid-svg svg').evaluateAll((nodes) => nodes.map((node) => node.id));
    assert.equal(new Set(ids).size, 2);
    assert.equal(await content.locator('script, img[onerror], a[href^="javascript:"]').count(), 0);
    assert.equal(await page.evaluate(() => window.markdownUnsafe), undefined);
    await page.getByRole('button', { name: '编辑', exact: true }).click();
    const dialog = page.getByRole('dialog', { name: '编辑知识点' });
    await dialog.waitFor();
    assert.equal(await dialog.getByLabel('用自己的话解释').inputValue(), example);
    await dialog.getByRole('button', { name: '预览效果' }).click();
    const preview = dialog.getByRole('region', { name: '正文预览', exact: true });
    await preview.locator('.mermaid-svg').nth(1).waitFor({ timeout: 60000 });
    assert.equal(await preview.locator('.katex').count(), 2);
    await dialog.getByLabel('用自己的话解释').fill('```mermaid\nthis is invalid !\n```');
    await preview.getByRole('alert').waitFor({ timeout: 30000 });
    assert.match(await preview.locator('pre').textContent(), /this is invalid/);
    await dialog.getByLabel('用自己的话解释').fill('# 修订内容\n\n公式 $a^2+b^2=c^2$\n\n```mermaid\nflowchart LR\nX[修订] --> Y[保存]\n```');
    await preview.locator('.mermaid-svg svg').waitFor();
    await dialog.getByRole('button', { name: '保存修改' }).click();
    await dialog.waitFor({ state: 'hidden' });
    await content.getByRole('heading', { name: '修订内容' }).waitFor();
    assert.equal(writes.length, 1);
    assert.match(writes[0].content, /\$a\^2\+b\^2=c\^2\$/);
    await page.reload();
    await content.locator('.mermaid-svg svg').waitFor({ timeout: 30000 });
    assert.equal(await content.locator('.katex').count(), 1);
    for (const viewport of [{ width: 375, height: 812 }, { width: 812, height: 375 }]) {
      await page.setViewportSize(viewport);
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), true);
    }
    await page.getByRole('link', { name: '返回知识库' }).click();
    await page.getByRole('button', { name: '新建知识点' }).click();
    await page.getByLabel('标题', { exact: true }).fill('新建排版笔记');
    await page.getByLabel('用自己的话解释').fill(example);
    await page.getByRole('button', { name: '预览效果' }).click();
    await page.getByRole('region', { name: '正文预览' }).locator('.mermaid-svg').nth(1).waitFor();
    await page.getByRole('button', { name: '保存知识点' }).click();
    await page.getByRole('heading', { name: '新建排版笔记', exact: true }).waitFor();
    assert.equal(writes.at(-1).content, example);
    assert.deepEqual(errors, []);
    console.log('PASS: Markdown, GFM table, inline/display KaTeX, multiple Mermaid diagrams, error fallback/recovery, safe HTML/links, edit/create previews, raw-source save/reload, mobile/landscape.');
    await context.close();
  } finally { await browser.close(); }
}

main().catch((error) => { console.error(error); process.exitCode = 1; });
