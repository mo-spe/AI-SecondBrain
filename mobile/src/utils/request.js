// 统一网络请求封装 —— 语义对齐网页版 request.js 拦截器：
// 1) 自动注入 Bearer token；2) code===200 时解包 data；3) 401 清除登录态并跳登录

// 后端 context-path 为 /api，端口 8080，
// 因此非 H5 平台的 baseURL 必须带上 /api 前缀（H5 由 vite devServer 的 /api 代理补齐）。
// 本地联调指向后端：小程序开发者工具勾选「不校验合法域名」即可用 http://localhost；
// 真机预览时需把 localhost 换成电脑的局域网 IP（如 http://192.168.1.100:8080/api）。
// 通过 VITE_API_BASE_URL 注入不同环境地址；localhost 仅允许开发模式兜底，避免进入线上包。
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL
  || (import.meta.env.DEV ? 'http://localhost:8080/api' : '')

// 网关地址按平台区分：H5 走相对路径由 devServer 代理，App/小程序走绝对地址
function resolveBaseURL() {
  // #ifdef H5
  return '/api'
  // #endif
  // #ifndef H5
  // 上线后替换为正式 API 域名（保留 /api 前缀）
  return API_BASE_URL
  // #endif
}

let redirectingToLogin = false

// 401 统一处理：只弹一次「重新登录」，避免并发请求反复跳转
function handleUnauthorized() {
  if (redirectingToLogin) return
  redirectingToLogin = true
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.removeStorageSync('workspaceSelection')
  uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
  setTimeout(() => {
    uni.reLaunch({ url: '/pages/login/index' })
    redirectingToLogin = false
  }, 800)
}

/**
 * 发起请求。
 *
 * GET 时 params 作为查询串，POST/PUT 时 data 作为请求体，与网页版 axios 语义对齐。
 *
 * @param {object} options 请求配置：url / method / data / params / header
 * @returns {Promise<any>} 后端 Result.data 部分
 */
export function request(options) {
  const token = uni.getStorageSync('token')
  const method = options.method || 'GET'
  const baseURL = resolveBaseURL()
  // uni.request 中 GET 的 data 即查询串，其余方法 data 为请求体
  const payload =
    method === 'GET' ? options.params || options.data || {} : options.data || {}
  if (!baseURL) {
    const error = new Error('移动端 API 地址未配置')
    uni.showToast({
      title: import.meta.env.DEV ? '请用 dev:mp-weixin 预览' : '请配置 VITE_API_BASE_URL',
      icon: 'none',
    })
    return Promise.reject(error)
  }
  return new Promise((resolve, reject) => {
    uni.request({
      url: baseURL + options.url,
      method,
      data: payload,
      timeout: 30000,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.header || {}),
      },
      success: (res) => {
        const { statusCode, data } = res
        if (statusCode === 401) {
          handleUnauthorized()
          reject(new Error('登录已过期，请重新登录'))
          return
        }
        if (data && data.code === 200) {
          resolve(data.data)
        } else {
          const msg = (data && data.message) || '请求失败'
          uni.showToast({ title: msg, icon: 'none' })
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误，请稍后重试', icon: 'none' })
        reject(err)
      },
    })
  })
}

export default request
