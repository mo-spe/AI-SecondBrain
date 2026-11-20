const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { startLocalServer, parseApiOrigin } = require('../local-server.cjs');

test('API 源地址只接受 HTTPS 或 loopback HTTP，拒绝嵌入凭据', () => {
  assert.equal(parseApiOrigin().origin, 'http://127.0.0.1:4000');
  assert.equal(parseApiOrigin('https://course.example').origin, 'https://course.example');
  for (const url of ['file:///tmp', 'http://remote.example', 'https://user:secret@course.example', 'https://course.example/api']) {
    assert.throws(() => parseApiOrigin(url));
  }
});

test('桌面服务支持深层路由、音频上传代理并限制外部来源和目录访问', async (context) => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'feynman-desktop-'));
  await fs.writeFile(path.join(root, 'index.html'), '<html>course shell</html>');
  await fs.writeFile(path.join(root, 'app.js'), 'window.loaded = true');
  const upstream = http.createServer(async (request, response) => {
    const parts = [];
    for await (const part of request) parts.push(part);
    response.setHeader('Content-Type', 'application/json');
    response.end(JSON.stringify({ url: request.url, token: request.headers.authorization, body: Buffer.concat(parts).toString() }));
  });
  await new Promise((resolve) => upstream.listen(0, '127.0.0.1', resolve));
  const { server, origin } = await startLocalServer({ root, apiOrigin: `http://127.0.0.1:${upstream.address().port}`, port: 0 });
  context.after(async () => {
    await Promise.all([new Promise((resolve) => server.close(resolve)), new Promise((resolve) => upstream.close(resolve))]);
    await fs.rm(root, { recursive: true, force: true });
  });
  const shell = await fetch(`${origin}/knowledge/abc`);
  assert.match(await shell.text(), /course shell/);
  assert.match(shell.headers.get('content-security-policy'), /object-src 'none'/);
  assert.match((await fetch(`${origin}/app.js`)).headers.get('content-type'), /javascript/);
  assert.equal((await fetch(`${origin}/missing.js`)).status, 404);
  assert.equal((await fetch(`${origin}/%2e%2e%2fprivate.txt`)).status, 403);
  assert.equal((await fetch(`${origin}/api/health`, { headers: { Origin: 'https://external.example' } })).status, 403);
  const result = await fetch(`${origin}/api/feynman/transcribe?language=zh`, {
    method: 'POST', headers: { 'Content-Type': 'audio/wav', Authorization: 'Bearer sample' }, body: 'recorded audio'
  });
  assert.deepEqual(await result.json(), { url: '/api/feynman/transcribe?language=zh', token: 'Bearer sample', body: 'recorded audio' });
  await new Promise((resolve) => upstream.close(resolve));
  assert.equal((await fetch(`${origin}/api/health`)).status, 502);
});
