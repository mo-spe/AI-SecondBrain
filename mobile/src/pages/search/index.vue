<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { knowledgeAPI } from '@/api/knowledge'

const keyword = ref('')
const loading = ref(false)
const searched = ref(false)
const results = ref([])

const doSearch = async () => {
  const kw = keyword.value.trim()
  if (!kw) {
    uni.showToast({ title: '请输入搜索关键词', icon: 'none' })
    return
  }
  loading.value = true
  searched.value = true
  try {
    results.value = (await knowledgeAPI.search({ keyword: kw })) || []
  } catch (e) {
    results.value = []
  } finally {
    loading.value = false
  }
}

const onConfirm = () => doSearch()

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/knowledge/detail?id=${id}` })
}

onLoad(() => {
  // 自动聚焦由搜索框 confirm 触发
})
</script>

<template>
  <view class="search-page">
    <view class="search-bar card">
      <input
        v-model="keyword"
        class="search-input"
        placeholder="输入关键词搜索知识点"
        placeholder-class="search-placeholder"
        confirm-type="search"
        @confirm="onConfirm"
      />
      <text class="search-btn tap-target" @tap="doSearch">搜索</text>
    </view>

    <view v-if="loading" class="empty-state">搜索中…</view>
    <view v-else-if="searched && results.length === 0" class="empty-state">未找到相关知识点</view>
    <view v-else class="result-list">
      <view
        v-for="item in results"
        :key="item.id"
        class="card result-card tap-target"
        @tap="goDetail(item.id)"
      >
        <text class="result-title ellipsis">{{ item.title }}</text>
        <text class="result-summary ellipsis-2">{{ item.summary || item.content || '' }}</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.search-page {
  padding: 24rpx 32rpx;
}

.search-bar {
  display: flex;
  align-items: center;
  padding: 12rpx 12rpx 12rpx 28rpx;
  margin-bottom: 28rpx;

  .search-input {
    flex: 1;
    height: 64rpx;
    font-size: 30rpx;
    color: var(--text-primary);
  }

  .search-btn {
    padding: 12rpx 32rpx;
    background: var(--color-primary);
    color: #fdfcfa;
    font-size: 28rpx;
    border-radius: 32rpx;
  }
}

.search-placeholder {
  color: var(--text-placeholder);
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.result-card {
  padding: 28rpx 32rpx;

  .result-title {
    display: block;
    font-size: 30rpx;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 12rpx;
  }

  .result-summary {
    display: block;
    font-size: 26rpx;
    line-height: 1.6;
    color: var(--text-secondary);
  }
}
</style>