<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const total = ref(0)
const answered = ref(0)

onLoad((query) => {
  total.value = Number(query.total) || 0
  answered.value = Number(query.answered) || 0
})

const goBack = () => {
  uni.switchTab({ url: '/pages/review/index' })
}

const goCheckin = () => {
  uni.navigateTo({ url: '/pages/checkin/index' })
}
</script>

<template>
  <view class="done-page">
    <view class="done-mark">✓</view>
    <text class="done-title">复习完成</text>
    <text class="done-sub">本次共完成 {{ answered }} / {{ total }} 张卡片</text>
    <text class="done-tip">记忆的种子已经种下，记得按时回来浇水</text>

    <button class="btn-primary action-btn tap-target" @tap="goCheckin">去签到领积分</button>
    <button class="ghost-btn tap-target" @tap="goBack">返回今日复习</button>
  </view>
</template>

<style lang="scss" scoped>
.done-page {
  min-height: 100vh;
  padding: 0 48rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.done-mark {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fdfcfa;
  font-size: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40rpx;
  box-shadow: 0 16rpx 40rpx rgba(43, 95, 75, 0.25);
}

.done-title {
  font-size: 44rpx;
  font-weight: 600;
  color: var(--text-primary);
}

.done-sub {
  margin-top: 20rpx;
  font-size: 30rpx;
  color: var(--text-regular);
}

.done-tip {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
  text-align: center;
  margin-bottom: 80rpx;
}

.action-btn {
  width: 100%;
  margin-bottom: 24rpx;
}

.ghost-btn {
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
</style>