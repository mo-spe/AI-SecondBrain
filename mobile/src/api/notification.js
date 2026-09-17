import { request } from '@/utils/request'

// 通知中心 —— 端点与网页版 api/notification.js 对齐
export const notificationAPI = {
  getList(params) {
    return request({ url: '/notification/list', method: 'GET', params })
  },

  getUnreadCount() {
    return request({ url: '/notification/unread-count', method: 'GET' })
  },

  markAsRead(id) {
    return request({ url: `/notification/${id}/read`, method: 'PUT' })
  },

  markAllAsRead() {
    return request({ url: '/notification/read-all', method: 'PUT' })
  },
}

export default notificationAPI