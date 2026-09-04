import { request } from './request'
import { useUserStore } from '@/stores/user'

// 登录态与第三方登录封装 —— 账号密码登录、微信小程序登录（code -> openid -> JWT）
export const authService = {
  /**
   * 账号密码登录，成功后将 token 与用户信息落本地。
   */
  async login(username, password) {
    const userStore = useUserStore()
    const data = await request({
      url: '/auth/login',
      method: 'POST',
      data: { username, password },
    })
    userStore.setToken(data.token)
    userStore.setUserInfo(data.user || data.userInfo || {})
    return data
  },

  /**
   * 微信小程序登录：wx.login 换取 code，交给后端 code2session 换 openid 并签发 JWT。
   */
  // #ifdef MP-WEIXIN
  async wxLogin() {
    const userStore = useUserStore()
    const code = await new Promise((resolve, reject) => {
      uni.login({
        provider: 'weixin',
        success: (r) => resolve(r.code),
        fail: reject,
      })
    })
    const data = await request({
      url: '/auth/wx-login',
      method: 'POST',
      data: { code },
    })
    userStore.setToken(data.token)
    userStore.setUserInfo(data.user || data.userInfo || {})
    return data
  },
  // #endif

  /**
   * 注册并自动登录。
   */
  async register(username, password) {
    const data = await request({
      url: '/auth/register',
      method: 'POST',
      data: { username, password },
    })
    return data
  },
}

export default authService