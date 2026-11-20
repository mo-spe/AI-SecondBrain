const assert = require('node:assert/strict');
const path = require('node:path');
const os = require('node:os');
const fs = require('node:fs/promises');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

(async () => {
  const userDataDirectory = await fs.mkdtemp(path.join(os.tmpdir(), 'feynman-pwa-'));
  const context = await chromium.launchPersistentContext(userDataDirectory, {
    channel: 'msedge', headless: true,
    viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true
  });
  try {
    const page = await context.newPage();
    const origin = process.env.PWA_TEST_URL || 'http://localhost:8088';
    const errors = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await page.goto(origin);
    await page.getByRole('button', { name: '登录', exact: true }).waitFor();
    await page.evaluate(async () => {
      await navigator.serviceWorker.ready;
      if (!navigator.serviceWorker.controller) await new Promise((resolve) => navigator.serviceWorker.addEventListener('controllerchange', resolve, { once: true }));
    });
    const session = await context.newCDPSession(page);
    const { data, errors: manifestErrors } = await session.send('Page.getAppManifest');
    assert.deepEqual(manifestErrors, []);
    assert.equal(JSON.parse(data).display, 'standalone');
    const installability = await session.send('Page.getInstallabilityErrors');
    assert.deepEqual(installability.installabilityErrors, []);
    const dimensions = await page.evaluate(async () => Promise.all([192, 512].map(async (size) => {
      const image = new Image();
      image.src = `/icons/icon-${size}.png`;
      await image.decode();
      return [image.naturalWidth, image.naturalHeight];
    })));
    assert.deepEqual(dimensions, [[192, 192], [512, 512]]);
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
    // 首次访问中缓存的 API 不得进入离线存储，防止跨账号持久化私有知识。
    await page.evaluate(() => fetch('/api/health'));
    const apiCached = await page.evaluate(async () => {
      const keys = await caches.keys();
      for (const key of keys) {
        const requests = await (await caches.open(key)).keys();
        if (requests.some((request) => new URL(request.url).pathname.startsWith('/api'))) return true;
      }
      return false;
    });
    assert.equal(apiCached, false);
    await context.setOffline(true);
    await page.getByRole('status').filter({ hasText: '当前离线' }).waitFor();
    await page.reload();
    await page.getByRole('button', { name: '登录', exact: true }).waitFor();
    assert.equal(await page.evaluate(async () => {
      try { await fetch(`/api/health?offline-probe=${Date.now()}`, { cache: 'no-store' }); return false; }
      catch { return true; }
    }), true);
    // Chromium 的本机页面刷新会重置 navigator.onLine 模拟状态，重新施加网络状态供提示验收。
    await session.send('Network.overrideNetworkState', { offline: true, latency: 0, downloadThroughput: 0, uploadThroughput: 0 });
    await page.getByRole('status').filter({ hasText: '当前离线' }).waitFor();
    assert.deepEqual(errors, []);
    const output = path.join(__dirname, '../../artifacts/packaging');
    await fs.mkdir(output, { recursive: true });
    await page.screenshot({ path: path.join(output, 'pwa-mobile-offline.png'), fullPage: true });
    console.log('PASS: Chromium安装条件、Manifest图标尺寸、手机布局、Service Worker接管、离线重启、API不落缓存');
  } finally {
    await context.close();
    if (path.dirname(userDataDirectory) === path.resolve(os.tmpdir()) && path.basename(userDataDirectory).startsWith('feynman-pwa-')) {
      await fs.rm(userDataDirectory, { recursive: true, force: true });
    }
  }
})().catch((error) => { console.error(error); process.exitCode = 1; });
