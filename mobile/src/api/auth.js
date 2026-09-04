import { request } from '@/utils/request'

export const authAPI = {
  login(data) {
    return request({ url: '/auth/login', method: 'POST', data })
  },

  register(data) {
    return request({ url: '/auth/register', method: 'POST', data })
  },

  // 微信小程序登录：code -> openid -> JWT（后端 AuthController#wx-login）
  wxLogin(code) {
    return request({ url: '/auth/wx-login', method: 'POST', data: { code } })
  },
}

export default authAPI