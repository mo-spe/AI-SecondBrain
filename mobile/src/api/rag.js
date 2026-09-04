import { request } from '@/utils/request'

// RAG 知识问答模块 —— 端点与后端 RagController 对齐
export const ragAPI = {
  // 非流式问答（移动端简化方案，替代 SSE 流式，避免小程序端 SSE 兼容问题）
  ask(data) {
    return request({ url: '/rag/answer', method: 'POST', data })
  },
}

export default ragAPI