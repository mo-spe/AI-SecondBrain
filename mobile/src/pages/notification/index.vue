<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { notificationAPI } from '@/api/notification'

const list = ref([])
const loading = ref(false)
const current = ref(1)
const size = 15
const total = ref(0)
const hasMore = ref(true)

const load = async (reset) => {
  if (loading.value) return
  if (reset) {
    current.value = 1
    hasMore.value = true
  }
  loading.value = true
  try {
    const data = await notificationAPI.getList({ current: current.value, size })
    const records = data.records || []
    total.value = data.total || 0
    list.value = reset ? records : list.value.concat(records)
    hasMore.value = list.value.length < total.value
  } catch (e) {
    /* request 已 toast */
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

const markRead = async (item) => {
  if (item.isRead === 0) {
    try {
      await notificationAPI.markAsRead(item.id)
      item.isRead = 1
    } catch (e) {
      /* request 已 toast */
    }
  }
}

const markAll = async () => {
  try {
    await notificationAPI.markAllAsRead()
    list.value.forEach((item) => (item.isRead = 1))
    uni.showToast({ title: '已全部标记为已读', icon: 'none' })
  } catch (e) {
    /* request 已 toast */
  }
}

const formatDate = (s) => {
  if (!s) return ''
  const d = new Date(s)
  return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onLoad(() => load(true))
onPullDownRefresh(() => load(true))
onReachBottom(() => {
  if (hasMore.value && !loading.value) {
    current.value += 1
    load(false)
  }
})
</script>

<template>
  <view class="notification-page">
    <view v-if="list.length > 0" class="toolbar">
      <text class="toolbar-btn tap-target" @tap="markAll">全部已读</text>
    </view>

    <view v-if="loading && list.length === 0" class="empty-state">加载中…</view>
    <view v-else-if="list.length === 0" class="empty-state">暂无通知</view>
    <view v-else class="notif-list">
      <view
        v-for="item in list"
        :key="item.id"
        class="card notif-card tap-target"
        :class="{ unread: item.isRead === 0 }"
        @tap="markRead(item)"
      >
        <view class="notif-dot" v-if="item.isRead === 0" />
        <view class="notif-body">
          <text class="notif-title">{{ item.title }}</text>
          <text v-if="item.content" class="notif-content ellipsis-2">{{ item.content }}</text>
          <text class="notif-time">{{ formatDate(item.createdAt) }}</text>
        </view>
      </view>
    </view>

    <view v-if="list.length > 0 && !hasMore" class="list-end">没有更多了</view>
  </view>
</template>

<style lang="scss" scoped>
.notification-page {
  padding: 24rpx 32rpx 48rpx;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16rpx;

  .toolbar-btn {
    font-size: 24rpx;
    color: var(--color-primary);
  }
}

.notif-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.notif-card {
  display: flex;
  align-items: flex-start;
  padding: 24rpx 28rpx;
  position: relative;

  &.unread {
    background: rgba(43, 95, 75, 0.03);
  }

  .notif-dot {
    width: 16rpx;
    height: 16rpx;
    border-radius: 50%;
    background: var(--color-danger);
    margin-top: 12rpx;
    margin-right: 16rpx;
    flex-shrink: 0;
  }

  .notif-body {
    flex: 1;
    min-width: 0;

    .notif-title {
      display: block;
      font-size: 30rpx;
      font-weight: 500;
      color: var(--text-primary);
    }

    .notif-content {
      display: block;
      margin-top: 8rpx;
      font-size: 26rpx;
      line-height: 1.5;
      color: var(--text-secondary);
    }

    .notif-time {
      display: block;
      margin-top: 12rpx;
      font-size: 22rpx;
      color: var(--text-placeholder);
    }
  }
}

.list-end {
  text-align: center;
  padding: 32rpx 0;
  font-size: 24rpx;
  color: var(--text-placeholder);
}
</style>