<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onUnload } from '@dcloudio/uni-app'
import { deerFlowAPI } from '@/api/deerflow'

const loading = ref(true)
const history = ref([])
const showCompose = ref(false)
const submitting = ref(false)

const form = ref({ topic: '', depth: 'intermediate' })

const DEPTHS = [
  { value: 'beginner', label: '入门' },
  { value: 'intermediate', label: '进阶' },
  { value: 'advanced', label: '深入' },
]

let pollTimer = null

const load = async () => {
  loading.value = true
  try {
    const data = await deerFlowAPI.getHistoryList({ current: 1, size: 20 })
    history.value = data.records || []
  } catch (e) {
    history.value = []
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

const openCompose = () => {
  form.value = { topic: '', depth: 'intermediate' }
  showCompose.value = true
}

const submit = async () => {
  const topic = form.value.topic.trim()
  if (!topic) {
    uni.showToast({ title: '请输入研究主题', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const task = await deerFlowAPI.generateLearningReport({
      topic,
      depth: form.value.depth,
    })
    showCompose.value = false
    uni.showLoading({ title: '研究中…' })
    // 轮询任务状态，完成后跳转报告页
    pollStatus(task.taskId || task.taskNumber)
  } catch (e) {
    /* request 已 toast */
  } finally {
    submitting.value = false
  }
}

const pollStatus = async (taskId) => {
  if (!taskId) {
    uni.hideLoading()
    load()
    return
  }
  try {
    const task = await deerFlowAPI.getTaskStatus(taskId)
    if (task.status === 'COMPLETED') {
      uni.hideLoading()
      uni.navigateTo({ url: `/pages/ai/report?content=${encodeURIComponent(JSON.stringify(task.result || {}))}` })
      load()
      return
    }
    if (task.status === 'FAILED') {
      uni.hideLoading()
      uni.showToast({ title: task.errorMessage || '研究失败', icon: 'none' })
      load()
      return
    }
  } catch (e) {
    /* 轮询失败忽略，继续 */
  }
  pollTimer = setTimeout(() => pollStatus(taskId), 3000)
}

const goReport = (item) => {
  uni.navigateTo({ url: `/pages/ai/report?content=${encodeURIComponent(item.content || '')}` })
}

onLoad(load)
onPullDownRefresh(load)
onUnload(() => {
  if (pollTimer) clearTimeout(pollTimer)
})
</script>

<template>
  <view class="research-page">
    <view v-if="loading && history.length === 0" class="empty-state">加载中…</view>
    <view v-else-if="history.length === 0" class="empty-state">暂无研究记录，发起一次研究吧</view>
    <view v-else class="history-list">
      <view
        v-for="item in history"
        :key="item.id"
        class="card history-card tap-target"
        @tap="goReport(item)"
      >
        <text class="history-topic">{{ item.topic }}</text>
        <text class="history-meta">{{ item.type }} · {{ item.createTime }}</text>
      </view>
    </view>

    <view class="fab tap-target" @tap="openCompose">
      <text class="fab-icon">＋</text>
    </view>

    <view v-if="showCompose" class="compose-mask" @tap.self="showCompose = false">
      <view class="compose-panel card">
        <text class="compose-title">发起研究</text>
        <input
          v-model="form.topic"
          class="compose-input"
          placeholder="输入研究主题"
          placeholder-class="compose-placeholder"
        />
        <view class="depth-row">
          <view
            v-for="d in DEPTHS"
            :key="d.value"
            class="depth-chip tap-target"
            :class="{ active: form.depth === d.value }"
            @tap="form.depth = d.value"
          >{{ d.label }}</view>
        </view>
        <button class="btn-primary compose-submit" :loading="submitting" :disabled="submitting" @tap="submit">
          开始研究
        </button>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.research-page {
  padding: 24rpx 32rpx 140rpx;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.history-card {
  padding: 28rpx 32rpx;

  .history-topic {
    display: block;
    font-size: 30rpx;
    font-weight: 600;
    color: var(--text-primary);
  }

  .history-meta {
    display: block;
    margin-top: 12rpx;
    font-size: 22rpx;
    color: var(--text-placeholder);
  }
}

.fab {
  position: fixed;
  right: 48rpx;
  bottom: calc(48rpx + env(safe-area-inset-bottom));
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  background: var(--color-primary);
  box-shadow: 0 12rpx 32rpx rgba(43, 95, 75, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;

  .fab-icon {
    font-size: 52rpx;
    color: #fdfcfa;
    line-height: 1;
  }
}

.compose-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;

  .compose-panel {
    width: 100%;
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    padding: 40rpx 36rpx;
    padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
    box-sizing: border-box;

    .compose-title {
      display: block;
      font-size: 34rpx;
      font-weight: 600;
      color: var(--text-primary);
      margin-bottom: 28rpx;
    }

    .compose-input {
      height: 88rpx;
      background: var(--bg-input);
      border-radius: var(--radius-md);
      padding: 0 28rpx;
      font-size: 30rpx;
      margin-bottom: 24rpx;
      box-sizing: border-box;
    }

    .depth-row {
      display: flex;
      gap: 16rpx;
      margin-bottom: 28rpx;

      .depth-chip {
        flex: 1;
        height: 72rpx;
        border-radius: 36rpx;
        border: 1rpx solid var(--border-base);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26rpx;
        color: var(--text-secondary);

        &.active {
          background: var(--color-primary);
          color: #fdfcfa;
          border-color: var(--color-primary);
        }
      }
    }

    .compose-submit {
      width: 100%;
    }
  }
}

.compose-placeholder {
  color: var(--text-placeholder);
}
</style>