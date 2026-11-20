import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import cesium from 'vite-plugin-cesium';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';

function pwaShell() {
  return {
    name: 'feynman-pwa-shell',
    apply: 'build',
    generateBundle(_options, bundle) {
      const files = new Set(['/', '/manifest.json', '/icons/icon-192.png', '/icons/icon-512.png']);
      const includeChunk = (file) => {
        if (files.has(`/${file}`)) return;
        files.add(`/${file}`);
        const chunk = bundle[file];
        if (chunk?.type !== 'chunk') return;
        chunk.imports.forEach(includeChunk);
        chunk.viteMetadata?.importedCss.forEach((css) => files.add(`/${css}`));
      };
      Object.values(bundle).filter((entry) => entry.type === 'chunk' && (entry.isEntry || entry.name === 'LoginPage'))
        .forEach((entry) => includeChunk(entry.fileName));
      // 缓存名随构建变化，预缓存入口及登录页依赖，使首次访问后即可离线重新打开。
      const version = createHash('sha256').update([...files].join('|')).digest('hex').slice(0, 12);
      const source = readFileSync(new URL('./public/sw.js', import.meta.url), 'utf8')
        .replace('__BUILD_VERSION__', version)
        .replace(/const SHELL_FILES = .*;/, `const SHELL_FILES = ${JSON.stringify([...files])};`);
      this.emitFile({ type: 'asset', fileName: 'sw.js', source });
    }
  };
}

export default defineConfig({
  // 保留地球资源的服务与复制，取消插件向所有页面注入阻塞式6MB脚本。
  plugins: [react(), { ...cesium({ devMinifyCesium: true }), transformIndexHtml: undefined }, pwaShell()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:4000' }
  }
});
