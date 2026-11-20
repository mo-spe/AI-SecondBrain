const http = require('node:http');
const https = require('node:https');
const fs = require('node:fs');
const path = require('node:path');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css', '.json': 'application/json', '.webmanifest': 'application/manifest+json',
  '.png': 'image/png', '.svg': 'image/svg+xml', '.ico': 'image/x-icon',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.wasm': 'application/wasm', '.jpg': 'image/jpeg'
};

function parseApiOrigin(value = 'http://127.0.0.1:4000') {
  const url = new URL(value);
  const local = ['127.0.0.1', 'localhost', '[::1]'].includes(url.hostname);
  if (url.username || url.password || url.pathname !== '/' || url.search || url.hash
    || !(url.protocol === 'https:' || (url.protocol === 'http:' && local))) {
    throw new Error('API 地址必须是 HTTPS 服务源地址，或本机 HTTP 地址；不要填写 /api 或密钥。');
  }
  return url;
}

async function startLocalServer({ root, apiOrigin, port = 17463 }) {
  const upstream = parseApiOrigin(apiOrigin);
  const directory = path.resolve(root);
  const server = http.createServer(async (request, response) => {
    const address = server.address();
    const origin = `http://127.0.0.1:${address.port}`;
    // 固定 loopback Host 并检查请求来源，避免其他网页借桌面代理访问本机 API。
    if (request.headers.host !== `127.0.0.1:${address.port}`
      || (request.headers.origin && request.headers.origin !== origin)) {
      response.writeHead(403).end();
      return;
    }
    if (request.url === '/api' || request.url.startsWith('/api/')) {
      const headers = { ...request.headers, host: upstream.host };
      delete headers.origin;
      delete headers.referer;
      const proxy = (upstream.protocol === 'https:' ? https : http).request({
        hostname: upstream.hostname, port: upstream.port, protocol: upstream.protocol,
        path: request.url, method: request.method, headers, timeout: 120000
      }, (result) => {
        response.writeHead(result.statusCode, result.headers);
        result.pipe(response);
      });
      proxy.on('timeout', () => proxy.destroy(new Error('API timeout')));
      proxy.on('error', () => {
        if (response.headersSent) return response.destroy();
        response.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
        response.end(JSON.stringify({ message: '课程 API 暂时无法连接，请启动后端或检查桌面 API 地址。' }));
      });
      request.on('aborted', () => proxy.destroy());
      request.pipe(proxy);
      return;
    }
    if (!['GET', 'HEAD'].includes(request.method)) return response.writeHead(405).end();
    try {
      const pathname = decodeURIComponent(new URL(request.url, origin).pathname);
      let file = path.resolve(directory, `.${pathname}`);
      if (file !== directory && !file.startsWith(`${directory}${path.sep}`)) return response.writeHead(403).end();
      let stat = await fs.promises.stat(file).catch(() => null);
      if (!stat?.isFile()) {
        // History 路由回退只处理页面，缺失的 JS/图片必须保持 404，避免把 HTML 当脚本执行。
        if (path.extname(pathname)) return response.writeHead(404).end();
        file = path.join(directory, 'index.html');
        stat = await fs.promises.stat(file);
      }
      response.writeHead(200, {
        'Content-Type': MIME_TYPES[path.extname(file)] || 'application/octet-stream',
        'Content-Length': stat.size, 'X-Content-Type-Options': 'nosniff',
        'Cache-Control': 'no-cache',
        // Cesium Widgets 的 Knockout 绑定依赖 Function 构造器；脚本仍仅能来自安装包同源资源。
        'Content-Security-Policy': "default-src 'self'; script-src 'self' 'unsafe-eval' blob:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' data: https://fonts.gstatic.com; img-src 'self' data: blob:; connect-src 'self' blob:; worker-src 'self' blob:; media-src 'self' blob:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'"
      });
      if (request.method === 'HEAD') return response.end();
      fs.createReadStream(file).on('error', () => response.destroy()).pipe(response);
    } catch {
      if (!response.headersSent) response.writeHead(404);
      response.end();
    }
  });
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(port, '127.0.0.1', resolve);
  });
  return { server, origin: `http://127.0.0.1:${server.address().port}` };
}

module.exports = { startLocalServer, parseApiOrigin };
