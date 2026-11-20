const assert = require('node:assert/strict');
const http = require('node:http');
const path = require('node:path');
const fs = require('node:fs/promises');
const os = require('node:os');
const { _electron } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

(async () => {
  const fixture = http.createServer((request, response) => {
    response.setHeader('Content-Type', 'application/json');
    const item = { id: 'packaging-note', title: '桌面打包验收知识点', content: '用于验证安装包路由与资源加载。', tags: ['验收'], reviewScore: null, updatedAt: new Date().toISOString() };
    response.end(JSON.stringify(request.url.endsWith('/auth/me') ? { user: { username: 'desktop-check' } }
      : request.url.includes('/graph/') ? { nodes: [{ id: item.id, label: item.title, excerpt: item.content, tags: [], score: null }], links: [], stats: {} }
        : request.url.startsWith('/api/knowledge/') ? { item }
          : request.url.includes('/attempts/') ? { attempts: [] }
        : { items: [item] }));
  });
  await new Promise((resolve) => fixture.listen(0, '127.0.0.1', resolve));
  const environment = { ...process.env, FEYNMAN_API_ORIGIN: `http://127.0.0.1:${fixture.address().port}` };
  delete environment.ELECTRON_RUN_AS_NODE;
  const executablePath = process.env.ELECTRON_TEST_EXE || path.join(__dirname, '../release', require('../package.json').version, 'win-unpacked/费曼学习平台.exe');
  let application;
  const profile = await fs.mkdtemp(path.join(os.tmpdir(), 'feynman-desktop-test-'));
  try {
    application = await _electron.launch({ executablePath, env: environment,
      args: [`--user-data-dir=${profile}`, '--use-fake-device-for-media-stream', '--enable-unsafe-swiftshader'], timeout: 30000 });
    const page = await application.firstWindow();
    const errors = [];
    page.on('pageerror', (error) => { errors.push(error.message); console.error('Renderer error:', error.message); });
    page.on('console', (message) => { if (message.type() === 'error') console.error('Renderer console:', message.text()); });
    await page.getByRole('button', { name: '登录', exact: true }).waitFor();
    const preferences = await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].webContents.getLastWebPreferences());
    assert.equal(preferences.nodeIntegration, false);
    assert.equal(preferences.contextIsolation, true);
    assert.equal(preferences.sandbox, true);
    assert.equal(await page.evaluate(() => typeof window.require), 'undefined');
    assert.equal(await page.evaluate(() => window.feynmanDesktop.platform), process.platform);
    assert.equal(await page.evaluate(() => window.isSecureContext), true);
    const media = await page.evaluate(async () => {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const audioTracks = stream.getAudioTracks().length;
      stream.getTracks().forEach((track) => track.stop());
      try {
        const camera = await navigator.mediaDevices.getUserMedia({ video: true });
        camera.getTracks().forEach((track) => track.stop());
        return { audioTracks, cameraDenied: false };
      } catch { return { audioTracks, cameraDenied: true }; }
    });
    assert.deepEqual(media, { audioTracks: 1, cameraDenied: true });
    await page.evaluate(() => localStorage.setItem('feynman_token', 'packaging-test'));
    await page.goto(`${new URL(page.url()).origin}/knowledge`);
    await page.getByRole('heading', { name: '我的知识。', exact: true }).waitFor();
    await page.getByText('桌面打包验收知识点', { exact: true }).waitFor();
    await page.reload();
    await page.getByRole('heading', { name: '我的知识。', exact: true }).waitFor();
    await page.goto(`${new URL(page.url()).origin}/knowledge/packaging-note`);
    await page.getByRole('heading', { name: '桌面打包验收知识点', exact: true }).waitFor();
    await page.reload();
    await page.getByRole('heading', { name: '桌面打包验收知识点', exact: true }).waitFor();
    await page.getByRole('link', { name: '数字地球', exact: true }).first().click();
    try { await page.locator('.cesium-viewer canvas').waitFor({ timeout: 30000 }); }
    catch (error) { console.error('Earth page:', (await page.locator('body').innerText()).slice(-1700)); throw error; }
    assert.equal(await page.evaluate(async () => (await navigator.serviceWorker.getRegistrations()).length), 0);
    const output = path.join(__dirname, '../../artifacts/packaging');
    await fs.mkdir(output, { recursive: true });
    await page.screenshot({ path: path.join(output, 'desktop-earth.png') });
    assert.deepEqual(errors, []);
    await page.evaluate(() => localStorage.removeItem('feynman_token'));
    console.log('PASS: 打包后可执行文件启动、隔离设置、麦克风/摄像头权限、API代理、深层路由刷新、Cesium画布、无桌面SW');
  } finally {
    if (application) await application.close();
    await new Promise((resolve) => fixture.close(resolve));
    if (path.dirname(profile) === path.resolve(os.tmpdir()) && path.basename(profile).startsWith('feynman-desktop-test-')) {
      await fs.rm(profile, { recursive: true, force: true });
    }
  }
})().catch((error) => { console.error(error); process.exitCode = 1; });
