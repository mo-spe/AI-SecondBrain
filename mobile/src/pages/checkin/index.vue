<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { gamificationAPI } from '@/api/gamification'

const loading = ref(true)
const checking = ref(false)

const profile = ref({
  totalPoints: 0,
  currentStreak: 0,
  checkedInToday: false,
})

const days = ref([])

const load = async () => {
  loading.value = true
  try {
    const [p, calendar] = await Promise.all([
      gamificationAPI.getProfile().catch(() => null),
      gamificationAPI.getStreakCalendar(3).catch(() => []),
    ])
    if (p) {
      profile.value = {
        totalPoints: Number(p.totalPoints) || 0,
        currentStreak: Number(p.currentStreak) || 0,
        checkedInToday: !!p.checkedInToday,
      }
    }
    days.value = calendar || []
  } catch (e) {
    /* request 已 toast */
  } finally {
    loading.value = false
  }
}

const handleCheckIn = async () => {
  if (profile.value.checkedInToday) return
  checking.value = true
  try {
    const res = await gamificationAPI.checkIn()
    profile.value = {
      totalPoints: Number(res.totalPoints) || profile.value.totalPoints,
      currentStreak: Number(res.currentStreak) || 0,
      checkedInToday: !!res.checkedInToday,
    }
    uni.showToast({ title: `签到成功 +${res.pointsEarned || 0} 积分`, icon: 'none' })
    load()
  } catch (e) {
    /* request 已 toast */
  } finally {
    checking.value = false
  }
}

const cellClass = (day) => {
  const p = day.pointsEarned || 0
  const active = day.hasCheckIn || day.hasReview
  if (p <= 0 && !active) return 'level-0'
  if (p <= 0 && active) return 'level-1'
  if (p <= 8) return 'level-1'
  if (p <= 20) return 'level-2'
  if (p <= 50) return 'level-3'
  return 'level-4'
}

onLoad(load)
</script>

<template>
  <view class="checkin-page">
    <!-- 积分与连续天数 -->
    <view class="hero">
      <view class="hero-stat">
        <text class="stat-num">{{ profile.totalPoints }}</text>
        <text class="stat-label">总积分</text>
      </view>
      <view class="hero-divider" />
      <view class="hero-stat">
        <text class="stat-num">{{ profile.currentStreak }}</text>
        <text class="stat-label">连续天数</text>
      </view>
    </view>

    <!-- 签到按钮 -->
    <button
      class="checkin-btn tap-target"
      :class="{ checked: profile.checkedInToday }"
      :loading="checking"
      :disabled="profile.checkedInToday || checking"
      @tap="handleCheckIn"
    >
      {{ profile.checkedInToday ? '今日已签到' : '立即签到领积分' }}
    </button>

    <!-- 学习热力图 -->
    <view class="card calendar-card">
      <view class="calendar-header">
        <text class="calendar-title">学习热力图</text>
        <text class="calendar-sub">最近 3 个月</text>
      </view>
      <view v-if="days.length > 0" class="calendar-grid">
        <view
          v-for="day in days"
          :key="day.date"
          class="calendar-cell"
          :class="cellClass(day)"
        />
      </view>
      <view v-else-if="loading" class="empty-state">加载中…</view>
      <view v-else class="empty-state">暂无学习记录</view>
      <view class="calendar-legend">
        <text class="legend-label">少</text>
        <view class="legend-dot level-0" />
        <view class="legend-dot level-1" />
        <view class="legend-dot level-2" />
        <view class="legend-dot level-3" />
        <view class="legend-dot level-4" />
        <text class="legend-label">多</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.checkin-page {
  padding: 24rpx 32rpx 48rpx;
}

.hero {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #2b5f4b 0%, #1a3d30 100%);
  border-radius: var(--radius-lg);
  padding: 40rpx 0;
  margin-bottom: 32rpx;

  .hero-stat {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;

    .stat-num {
      font-size: 56rpx;
      font-weight: 600;
      color: #fdfcfa;
    }

    .stat-label {
      margin-top: 8rpx;
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.75);
    }
  }

  .hero-divider {
    width: 1rpx;
    height: 72rpx;
    background: rgba(255, 255, 255, 0.2);
  }
}

.checkin-btn {
  width: 100%;
  height: 96rpx;
  border-radius: 48rpx;
  background: var(--color-accent);
  color: #fdfcfa;
  font-size: 32rpx;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32rpx;

  &.checked {
    background: var(--bg-hover);
    color: var(--text-secondary);
  }
}

.calendar-card {
  padding: 32rpx;

  .calendar-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: 24rpx;

    .calendar-title {
      font-size: 30rpx;
      font-weight: 600;
      color: var(--text-primary);
    }

    .calendar-sub {
      font-size: 24rpx;
      color: var(--text-secondary);
    }
  }

  .calendar-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 8rpx;
    margin-bottom: 20rpx;
  }

  .calendar-cell {
    width: 28rpx;
    height: 28rpx;
    border-radius: 6rpx;
  }

  .calendar-legend {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8rpx;

    .legend-label {
      font-size: 22rpx;
      color: var(--text-secondary);
    }

    .legend-dot {
      width: 24rpx;
      height: 24rpx;
      border-radius: 6rpx;
    }
  }
}

.level-0 { background: var(--border-light); }
.level-1 { background: #b8cdbf; }
.level-2 { background: #7fa894; }
.level-3 { background: #4a8c6e; }
.level-4 { background: #2b5f4b; }
</style>