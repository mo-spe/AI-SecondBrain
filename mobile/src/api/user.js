import { request } from '@/utils/request'

// 用户信息 —— 端点与网页版 api/user.js 对齐
export const userAPI = {
  getUserInfo() {
    return request({ url: '/user/info', method: 'GET' })
  },

  updateUser(data) {
    return request({ url: '/user/update', method: 'PUT', data })
  },

  updatePassword(data) {
    return request({ url: '/user/password', method: 'PUT', data })
  },
}

export default userAPI