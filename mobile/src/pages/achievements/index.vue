<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { gamificationAPI } from '@/api/gamification'

const loading = ref(true)
const list = ref([])

const load = async () => {
  loading.value = true
  try {
    list.value = (await gamificationAPI.getAchievements({})) || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

const tierClass = (tier) => `tier-${tier || 'bronze'}`

const formatDate = (s) => {
  if (!s) return ''
  const d = new Date(s)
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}

onLoad(load)
</script>

<template>
  <view class="achievements-page">
    <view v-if="loading" class="empty-state">加载中…</view>
    <view v-else-if="list.length === 0" class="empty-state">暂无成就</view>
    <view v-else class="achievements-grid">
      <view
        v-for="ach in list"
        :key="ach.id || ach.name"
        class="card achievement-card"
        :class="[ach.unlocked ? 'unlocked' : 'locked', tierClass(ach.tier)]"
      >
        <view class="ach-icon">{{ ach.icon || '🏅' }}</view>
        <text class="ach-name">{{ ach.name }}</text>
        <text class="ach-desc ellipsis-2">{{ ach.description }}</text>
        <view v-if="ach.unlocked" class="ach-meta">
          <text class="ach-date">{{ formatDate(ach.unlockedAt) }}</text>
          <text v-if="ach.pointsReward" class="ach-reward">+{{ ach.pointsReward }}积分</text>
        </view>
        <view v-else class="ach-locked-tag">未解锁</view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.achievements-page {
  padding: 24rpx 32rpx 48rpx;
}

.achievements-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.achievement-card {
  width: calc(50% - 10rpx);
  box-sizing: border-box;
  padding: 32rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;

  &.locked {
    opacity: 0.55;
  }

  &.unlocked.tier-platinum {
    border-color: #b8c4d0;
  }

  &.unlocked.tier-gold {
    border-color: #d4a94f;
  }

  .ach-icon {
    font-size: 64rpx;
    margin-bottom: 16rpx;
  }

  .ach-name {
    font-size: 28rpx;
    font-weight: 600;
    color: var(--text-primary);
  }

  .ach-desc {
    margin-top: 8rpx;
    font-size: 24rpx;
    line-height: 1.5;
    color: var(--text-secondary);
  }

  .ach-meta {
    margin-top: 12rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4rpx;

    .ach-date {
      font-size: 20rpx;
      color: var(--text-placeholder);
    }

    .ach-reward {
      font-size: 22rpx;
      color: var(--color-accent);
    }
  }

  .ach-locked-tag {
    margin-top: 12rpx;
    font-size: 20rpx;
    color: var(--text-placeholder);
    background: var(--bg-input);
    padding: 4rpx 16rpx;
    border-radius: 8rpx;
  }
}
</style>