<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const raw = ref('')

// 报告内容可能为结构化 JSON（任务 result）或纯文本（历史 content），统一拍平成可读文本
const content = computed(() => {
  if (!raw.value) return ''
  try {
    const obj = JSON.parse(raw.value)
    if (typeof obj === 'string') return obj
    // 常见字段优先：content / report / markdown / summary
    return obj.content || obj.report || obj.markdown || obj.summary || JSON.stringify(obj, null, 2)
  } catch (e) {
    return raw.value
  }
})

onLoad((query) => {
  if (query.content) {
    raw.value = decodeURIComponent(query.content)
  }
})
</script>

<template>
  <view class="report-page">
    <view v-if="!content" class="empty-state">暂无报告内容</view>
    <view v-else class="card report-body">
      <text class="report-text">{{ content }}</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.report-page {
  padding: 24rpx 32rpx 48rpx;
}

.report-body {
  padding: 32rpx;

  .report-text {
    font-size: 28rpx;
    line-height: 1.8;
    color: var(--text-regular);
    white-space: pre-wrap;
    word-break: break-word;
  }
}
</style>