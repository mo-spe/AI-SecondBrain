import { request } from '@/utils/request'

// 快速记录 / 数据采集 —— 端点与网页版 api/capture.js 对齐
export const captureAPI = {
  captureNote(title, content, userId) {
    return request({ url: '/capture/note', method: 'POST', data: { title, content, userId } })
  },
}

export default captureAPI