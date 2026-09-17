import { defineStore } from 'pinia'
import { ref } from 'vue'

// 移动端登录态 —— 与网页版 user store 语义一致，存储介质由 localStorage 换成 uni Storage
export const useUserStore = defineStore('user', () => {
  const token = ref(uni.getStorageSync('token') || '')
  const userInfo = ref(uni.getStorageSync('userInfo') || {})

  const setToken = (newToken) => {
    token.value = newToken
    uni.setStorageSync('token', newToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    uni.setStorageSync('userInfo', info)
  }

  const logout = () => {
    token.value = ''
    userInfo.value = {}
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    // 工作区选择与登录用户绑定，退出时清除可避免下一账号继承旧工作区上下文。
    uni.removeStorageSync('workspaceSelection')
  }

  const isLoggedIn = () => !!token.value

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    logout,
    isLoggedIn,
  }
})
