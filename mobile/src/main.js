import { createSSRApp } from 'vue'
import * as Pinia from 'pinia'
import App from './App.vue'

// uni-app Vue3 使用 createSSRApp 创建应用实例，便于与服务端渲染共用入口
export function createApp() {
  const app = createSSRApp(App)
  app.use(Pinia.createPinia())
  return {
    app,
    Pinia,
  }
}