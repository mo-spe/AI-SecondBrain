import { request } from '@/utils/request'

// 知识广场 —— 端点与网页版 api/square.js 对齐
export const squareAPI = {
  list(params) {
    return this.getList(params)
  },

  getList(params) {
    return request({ url: '/square/list', method: 'GET', params })
  },

  getDetail(id) {
    return request({ url: `/square/${id}`, method: 'GET' })
  },

  publish(data) {
    return request({ url: '/square/publish', method: 'POST', data })
  },

  toggleLike(id) {
    return request({ url: `/square/${id}/like`, method: 'POST' })
  },

  like(id) {
    return this.toggleLike(id)
  },

  toggleBookmark(id) {
    return request({ url: `/square/${id}/bookmark`, method: 'POST' })
  },

  bookmark(id) {
    return this.toggleBookmark(id)
  },
}

export default squareAPI
