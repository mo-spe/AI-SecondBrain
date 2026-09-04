<script setup>
import { ref } from 'vue'
import { authService } from '@/utils/auth'

const mode = ref('login') // login | register
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)

const isLogin = () => mode.value === 'login'

const switchMode = (m) => {
  mode.value = m
}

const goHome = () => {
  uni.switchTab({ url: '/pages/review/index' })
}

// 账号密码登录 / 注册统一入口
const submit = async () => {
  if (!username.value.trim()) {
    uni.showToast({ title: '请输入账号', icon: 'none' })
    return
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  if (!isLogin() && password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
    return
  }
  loading.value = true
  try {
    if (isLogin()) {
      await authService.login(username.value.trim(), password.value)
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(goHome, 500)
    } else {
      await authService.register(username.value.trim(), password.value)
      uni.showToast({ title: '注册成功，请登录', icon: 'none' })
      switchMode('login')
      password.value = ''
      confirmPassword.value = ''
    }
  } catch (e) {
    // request 已 toast 错误信息，这里静默即可
  } finally {
    loading.value = false
  }
}

// 微信一键登录（仅小程序端编译）
// #ifdef MP-WEIXIN
const wxLogin = async () => {
  loading.value = true
  try {
    await authService.wxLogin()
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(goHome, 500)
  } catch (e) {
    /* request 已 toast */
  } finally {
    loading.value = false
  }
}
// #endif
</script>

<template>
  <view class="login-page">
    <view class="brand">
      <view class="brand-mark">墨</view>
      <text class="brand-name">AI · Second Brain</text>
      <text class="brand-slogan">把你的知识，变成不会遗忘的记忆</text>
    </view>

    <view class="card form-card">
      <view class="mode-switch">
        <text
          class="mode-item"
          :class="{ active: isLogin() }"
          @tap="switchMode('login')"
        >登录</text>
        <view class="mode-divider" />
        <text
          class="mode-item"
          :class="{ active: !isLogin() }"
          @tap="switchMode('register')"
        >注册</text>
      </view>

      <view class="field">
        <text class="field-label">账号</text>
        <input
          v-model="username"
          class="field-input"
          placeholder="请输入用户名"
          placeholder-class="field-placeholder"
        />
      </view>

      <view class="field">
        <text class="field-label">密码</text>
        <input
          v-model="password"
          class="field-input"
          password
          placeholder="请输入密码"
          placeholder-class="field-placeholder"
        />
      </view>

      <view v-if="!isLogin()" class="field">
        <text class="field-label">确认密码</text>
        <input
          v-model="confirmPassword"
          class="field-input"
          password
          placeholder="请再次输入密码"
          placeholder-class="field-placeholder"
        />
      </view>

      <button
        class="btn-primary submit-btn tap-target"
        :loading="loading"
        :disabled="loading"
        @tap="submit"
      >{{ isLogin() ? '登 录' : '注 册' }}</button>
    </view>

    <!-- #ifdef MP-WEIXIN -->
    <view class="wx-entry">
      <text class="wx-divider">其他方式登录</text>
      <button class="wx-btn tap-target" :loading="loading" @tap="wxLogin">微信一键登录</button>
    </view>
    <!-- #endif -->
  </view>
</template>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  padding: 0 48rpx;
  padding-top: 160rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 64rpx;

  .brand-mark {
    width: 120rpx;
    height: 120rpx;
    border-radius: 32rpx;
    background: var(--color-primary);
    color: #fdfcfa;
    font-size: 56rpx;
    font-family: 'Noto Serif SC', 'Source Han Serif SC', serif;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 28rpx;
    box-shadow: 0 12rpx 32rpx rgba(43, 95, 75, 0.25);
  }

  .brand-name {
    font-size: 44rpx;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: 2rpx;
  }

  .brand-slogan {
    margin-top: 16rpx;
    font-size: 26rpx;
    color: var(--text-secondary);
  }
}

.form-card {
  padding: 40rpx 36rpx;
}

.mode-switch {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40rpx;

  .mode-item {
    font-size: 32rpx;
    color: var(--text-placeholder);
    padding: 8rpx 24rpx;
    transition: color 0.2s;

    &.active {
      color: var(--color-primary);
      font-weight: 600;
    }
  }

  .mode-divider {
    width: 1rpx;
    height: 28rpx;
    background: var(--border-base);
  }
}

.field {
  margin-bottom: 32rpx;

  .field-label {
    display: block;
    font-size: 26rpx;
    color: var(--text-secondary);
    margin-bottom: 12rpx;
  }

  .field-input {
    height: 88rpx;
    background: var(--bg-input);
    border-radius: var(--radius-md);
    padding: 0 28rpx;
    font-size: 30rpx;
    color: var(--text-primary);
    box-sizing: border-box;
  }
}

.field-placeholder {
  color: var(--text-placeholder);
}

.submit-btn {
  margin-top: 16rpx;
}

.wx-entry {
  margin-top: 56rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  .wx-divider {
    font-size: 24rpx;
    color: var(--text-placeholder);
    margin-bottom: 28rpx;
  }

  .wx-btn {
    width: 100%;
    height: 88rpx;
    border-radius: 44rpx;
    background: transparent;
    border: 1rpx solid var(--border-base);
    color: var(--text-regular);
    font-size: 30rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>