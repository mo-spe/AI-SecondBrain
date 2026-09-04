import { request } from '@/utils/request'

// AI 服务配置 —— 端点与网页版 api/ai.js 对齐
export const aiAPI = {
  getProviders() {
    return request({ url: '/ai/providers', method: 'GET' })
  },

  getUserAiConfig() {
    return request({ url: '/user/ai-config', method: 'GET' })
  },

  saveUserAiConfig(configs) {
    return request({ url: '/user/ai-config', method: 'PUT', data: configs })
  },
}

export default aiAPI