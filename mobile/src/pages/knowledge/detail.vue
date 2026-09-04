<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { knowledgeAPI } from '@/api/knowledge'

const id = ref(null)
const loading = ref(true)
const knowledge = ref(null)

const load = async () => {
  loading.value = true
  try {
    knowledge.value = await knowledgeAPI.getById(id.value)
  } catch (e) {
    /* request 已 toast */
  } finally {
    loading.value = false
  }
}

const toggleReview = async () => {
  if (!knowledge.value) return
  const target = knowledge.value.needReview === 1 ? 0 : 1
  try {
    await knowledgeAPI.toggleNeedReview(id.value, target)
    knowledge.value.needReview = target
    uni.showToast({
      title: target === 1 ? '已纳入复习计划' : '已取消复习',
      icon: 'none',
    })
  } catch (e) {
    /* request 已 toast */
  }
}

const goEdit = () => {
  uni.navigateTo({ url: `/pages/knowledge/create?id=${id.value}` })
}

onLoad((query) => {
  id.value = query.id
  load()
})
</script>

<template>
  <view class="detail-page">
    <view v-if="loading" class="empty-state">加载中…</view>

    <template v-else-if="knowledge">
      <view class="head card">
        <text class="detail-title">{{ knowledge.title }}</text>
        <view class="meta-row">
          <text class="meta-item">重要度 {{ knowledge.importance || 0 }}</text>
          <text v-if="knowledge.category" class="meta-item">{{ knowledge.category }}</text>
        </view>
        <view class="tag-row">
          <text
            v-for="tag in (knowledge.tags || [])"
            :key="tag.id || tag.tagName"
            class="tag-chip"
            :style="{ color: tag.tagColor || '#4a8c6e', borderColor: tag.tagColor || '#4a8c6e' }"
          >{{ tag.tagName }}</text>
        </view>
      </view>

      <view class="card body-card">
        <text class="summary-text">{{ knowledge.summary }}</text>
        <view class="divider" />
        <text class="content-text">{{ knowledge.contentMd || knowledge.content }}</text>
      </view>

      <view class="action-bar">
        <button class="ghost-btn tap-target" @tap="goEdit">编辑</button>
        <button class="primary-btn tap-target" @tap="toggleReview">
          {{ knowledge.needReview === 1 ? '取消复习计划' : '纳入复习计划' }}
        </button>
      </view>
    </template>
  </view>
</template>

<style lang="scss" scoped>
.detail-page {
  padding: 24rpx 32rpx 48rpx;
}

.head {
  padding: 32rpx;

  .detail-title {
    display: block;
    font-size: 40rpx;
    font-weight: 600;
    color: var(--text-primary);
    line-height: 1.4;
    margin-bottom: 20rpx;
  }

  .meta-row {
    display: flex;
    gap: 32rpx;
    margin-bottom: 20rpx;

    .meta-item {
      font-size: 24rpx;
      color: var(--text-secondary);
    }
  }

  .tag-row {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;

    .tag-chip {
      font-size: 22rpx;
      padding: 4rpx 14rpx;
      border: 1rpx solid;
      border-radius: 8rpx;
    }
  }
}

.body-card {
  margin-top: 24rpx;
  padding: 32rpx;

  .summary-text {
    display: block;
    font-size: 28rpx;
    line-height: 1.6;
    color: var(--text-regular);
  }

  .divider {
    height: 1rpx;
    background: var(--border-light);
    margin: 28rpx 0;
  }

  .content-text {
    display: block;
    font-size: 28rpx;
    line-height: 1.8;
    color: var(--text-regular);
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: var(--bg-page);
  display: flex;
  gap: 24rpx;

  .ghost-btn {
    flex: 1;
    height: 88rpx;
    border-radius: 44rpx;
    background: var(--bg-card);
    border: 1rpx solid var(--border-base);
    color: var(--text-regular);
    font-size: 30rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .primary-btn {
    flex: 1;
    height: 88rpx;
    border-radius: 44rpx;
    background: var(--color-primary);
    color: #fdfcfa;
    font-size: 30rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>