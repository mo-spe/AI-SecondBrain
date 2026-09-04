// 本地存储轻封装 —— 统一 key 语义，避免各页面直接散落 setStorageSync 魔法字符串
const KEYS = {
  token: 'token',
  userInfo: 'userInfo',
}

export const storage = {
  get(key) {
    return uni.getStorageSync(KEYS[key] || key)
  },
  set(key, value) {
    uni.setStorageSync(KEYS[key] || key, value)
  },
  remove(key) {
    uni.removeStorageSync(KEYS[key] || key)
  },
}

export default storage