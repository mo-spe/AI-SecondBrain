import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import cesium from 'vite-plugin-cesium';

export default defineConfig({
  // 保留地球资源的服务与复制，取消插件向所有页面注入阻塞式6MB脚本。
  plugins: [react(), { ...cesium({ devMinifyCesium: true }), transformIndexHtml: undefined }],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:4000' }
  }
});
