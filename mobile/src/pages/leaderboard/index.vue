<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { gamificationAPI } from '@/api/gamification'

const loading = ref(true)
const entries = ref([])
const currentUser = ref(null)
const period = ref('daily')
const domain = ref('all')

const PERIODS = [
  { value: 'daily', label: '今日' },
  { value: 'weekly', label: '本周' },
  { value: 'monthly', label: '本月' },
]

const load = async () => {
  loading.value = true
  try {
    const data = await gamificationAPI.getLeaderboard({ period: period.value, domain: domain.value, size: 100 })
    entries.value = data?.entries || []
    currentUser.value = data?.currentUser || null
  } catch (e) {
    entries.value = []
    currentUser.value = null
  } finally {
    loading.value = false
  }
}

const switchPeriod = (p) => {
  period.value = p
  load()
}

const rankColor = (rank) => ({ 1: '#c9793f', 2: '#7f918d', 3: '#a87554' }[rank] || '#7f918d')

onLoad(load)
</script>

<template>
  <view class="leaderboard-page">
    <view class="period-tabs">
      <view
        v-for="p in PERIODS"
        :key="p.value"
        class="period-tab tap-target"
        :class="{ active: period === p.value }"
        @tap="switchPeriod(p.value)"
      >{{ p.label }}</view>
    </view>

    <view v-if="currentUser" class="current-row card">
      <text class="current-rank">#{{ currentUser.rank || '-' }}</text>
      <text class="current-label">我的排名</text>
      <text class="current-score">{{ currentUser.score || 0 }} 分</text>
    </view>

    <view v-if="loading" class="empty-state">加载中…</view>
    <view v-else-if="entries.length === 0" class="empty-state">暂无排行数据</view>
    <view v-else class="rank-list card">
      <view
        v-for="entry in entries"
        :key="entry.userId || entry.username"
        class="rank-item"
        :class="{ me: currentUser && entry.rank === currentUser.rank }"
      >
        <view class="rank-position">
          <view v-if="entry.rank <= 3" class="rank-medal" :class="'rank-' + entry.rank">
            <AppIcon name="trophy" :size="22" :color="rankColor(entry.rank)" />
          </view>
          <text v-else class="rank-number">#{{ entry.rank }}</text>
        </view>
        <view class="rank-avatar">{{ (entry.username || '?').charAt(0) }}</view>
        <text class="rank-username ellipsis">{{ entry.username }}</text>
        <text class="rank-score">{{ entry.score }} 分</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.leaderboard-page {
  padding: 24rpx 32rpx 48rpx;
}

.period-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;

  .period-tab {
    flex: 1;
    height: 72rpx;
    border-radius: 36rpx;
    background: var(--bg-card);
    border: 1rpx solid var(--border-light);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    color: var(--text-secondary);

    &.active {
      background: var(--color-primary);
      color: #fdfcfa;
      border-color: var(--color-primary);
    }
  }
}

.current-row {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  margin-bottom: 24rpx;

  .current-rank {
    font-size: 36rpx;
    font-weight: 600;
    color: var(--color-primary);
  }

  .current-label {
    margin-left: 16rpx;
    font-size: 26rpx;
    color: var(--text-secondary);
  }

  .current-score {
    margin-left: auto;
    font-size: 30rpx;
    font-weight: 600;
    color: var(--color-accent);
  }
}

.rank-list {
  overflow: hidden;
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid var(--border-light);

  &:last-child {
    border-bottom: none;
  }

  &.me {
    background: rgba(43, 95, 75, 0.04);
  }

  .rank-position {
    width: 72rpx;
    flex-shrink: 0;

    .rank-medal {
      width: 44rpx;
      height: 44rpx;
      border-radius: 14rpx;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: #f4ede7;
    }

    .rank-medal.rank-2 { background: #eef2f1; }
    .rank-medal.rank-3 { background: #f2ebe6; }

    .rank-number {
      font-size: 26rpx;
      color: var(--text-secondary);
    }
  }

  .rank-avatar {
    width: 56rpx;
    height: 56rpx;
    border-radius: 50%;
    background: var(--color-accent);
    color: #fdfcfa;
    font-size: 26rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
    flex-shrink: 0;
  }

  .rank-username {
    flex: 1;
    font-size: 28rpx;
    color: var(--text-regular);
  }

  .rank-score {
    font-size: 28rpx;
    font-weight: 600;
    color: var(--text-primary);
  }
}
</style>
