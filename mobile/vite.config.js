import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// uni-app CLI 模式，@dcloudio/vite-plugin-uni 会自动按 -p 目标平台编译
export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5173,
    // H5 端 request.js 使用相对路径 /api，由 devServer 代理到本地后端（context-path=/api）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})