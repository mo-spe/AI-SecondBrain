<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { communityAPI } from '@/api/community'

const questionId = ref(null)
const loading = ref(true)
const detail = ref(null)

const authorInitial = computed(() => {
  const name = detail.value?.authorName || '学者'
  return String(name).charAt(0).toUpperCase()
})

const plainText = (value) => String(value || '').replace(/<[^>]+>/g, '')

const formatDate = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

const avatarColor = (value) => {
  const palette = ['#2b5f4b', '#b8723a', '#3e6b8b', '#5c4a8d', '#a05463']
  const code = String(value || '?').charCodeAt(0) || 0
  return palette[code % palette.length]
}

const goProfile = (userId) => {
  if (userId) uni.navigateTo({ url: `/pages/community/profile?id=${userId}` })
}

const load = async () => {
  loading.value = true
  try {
    detail.value = await communityAPI.getQuestion(questionId.value)
  } catch (_) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

onLoad((query) => {
  questionId.value = query.id
  if (!questionId.value) {
    uni.showToast({ title: '问题不存在', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 450)
    return
  }
  load()
})
</script>

<template>
  <view class="detail-page">
    <view class="detail-nav">
      <view class="nav-back tap-target" @tap="uni.navigateBack()">
        <AppIcon name="chevron-left" :size="20" color="#2b5f4b" />
      </view>
      <text class="nav-title">问题详情</text>
      <view class="nav-spacer" />
    </view>

    <view v-if="loading" class="state-card">
      <AppIcon name="refresh-cw" :size="24" color="#b8723a" />
      <text>正在展开问题…</text>
    </view>

    <template v-else-if="detail">
      <view class="question-card">
        <view class="author-row" @tap="goProfile(detail.authorId)">
          <view class="avatar" :style="{ background: avatarColor(detail.authorName) }">
            <image v-if="detail.authorAvatar" :src="detail.authorAvatar" mode="aspectFill" />
            <text v-else>{{ authorInitial }}</text>
          </view>
          <view class="author-meta">
            <text class="author-name">{{ detail.authorName || '匿名学者' }}</text>
            <text class="author-time">{{ formatDate(detail.createTime) }} · {{ detail.viewCount || 0 }} 阅读</text>
          </view>
          <AppIcon name="chevron-right" :size="16" color="#bfb29a" />
        </view>

        <text class="question-title">{{ detail.title }}</text>
        <text class="question-content">{{ detail.content }}</text>

        <view v-if="detail.tags && detail.tags.length" class="tag-row">
          <text v-for="tag in detail.tags" :key="tag" class="tag">#{{ tag }}</text>
        </view>

        <view class="question-meta">
          <view class="meta-item"><AppIcon name="message-square" :size="15" color="#6b6458" /><text>{{ detail.answerCount || 0 }} 个回答</text></view>
          <view v-if="detail.status === 'CLOSED'" class="closed-label">已结束</view>
        </view>
      </view>

      <view class="section-heading">
        <text class="section-title">回答</text>
        <text class="section-count">{{ detail.answerCount || 0 }}</text>
      </view>

      <view v-if="detail.answers && detail.answers.length" class="answer-list">
        <view v-for="answer in detail.answers" :key="answer.id" class="answer-card" :class="{ accepted: answer.accepted }">
          <view class="answer-head">
            <view class="author-row" @tap="goProfile(answer.authorId)">
              <view class="avatar small" :style="{ background: avatarColor(answer.authorName) }">
                <image v-if="answer.authorAvatar" :src="answer.authorAvatar" mode="aspectFill" />
                <text v-else>{{ String(answer.authorName || '学者').charAt(0).toUpperCase() }}</text>
              </view>
              <view class="author-meta">
                <text class="author-name">{{ answer.authorName || '匿名学者' }}</text>
                <text class="author-time">{{ formatDate(answer.createTime) }}</text>
              </view>
            </view>
            <view v-if="answer.accepted" class="accepted-label"><AppIcon name="check" :size="13" color="#2b5f4b" />已采纳</view>
          </view>

          <text class="answer-content">{{ plainText(answer.content) }}</text>

          <view v-if="answer.knowledgeSnapshots && answer.knowledgeSnapshots.length" class="snapshot-list">
            <view v-for="snapshot in answer.knowledgeSnapshots" :key="snapshot.sourceId" class="snapshot">
              <AppIcon name="book-open" :size="15" color="#b8723a" />
              <view class="snapshot-copy">
                <text class="snapshot-title">{{ snapshot.title }}</text>
                <text v-if="snapshot.summary" class="snapshot-summary">{{ snapshot.summary }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="state-card empty">
        <view class="empty-icon"><AppIcon name="message-square" :size="24" color="#bfb29a" /></view>
        <text>还没有回答</text>
        <text class="state-hint">如果你知道答案，欢迎在网页版参与回答</text>
      </view>
    </template>

    <view v-else class="state-card empty">
      <view class="empty-icon"><AppIcon name="circle-alert" :size="24" color="#b8443a" /></view>
      <text>问题暂时无法查看</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  padding: 0 $space-5 calc(48rpx + env(safe-area-inset-bottom));
  background: $uni-bg-page;
}

.detail-nav {
  height: 112rpx;
  display: flex;
  align-items: center;
  gap: $space-3;
}

.nav-back {
  width: 72rpx;
  height: 72rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: $uni-border-radius-sm;
  background: $uni-bg-card;
  border: $hairline solid $uni-border-light;
}

.nav-title { color: $uni-text-primary; font-size: $uni-font-size-md; font-weight: $weight-bold; }
.nav-spacer { flex: 1; }

.question-card, .answer-card, .state-card {
  background: $uni-bg-card;
  border: $hairline solid $uni-border-light;
  border-radius: $uni-border-radius-lg;
  box-shadow: $shadow-1;
}

.question-card { padding: $space-5; }

.author-row { display: flex; align-items: center; gap: $space-3; }
.author-row:active { opacity: .78; }
.avatar {
  width: 76rpx;
  height: 76rpx;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 26rpx;
  color: #fdfcfa;
  font-size: $uni-font-size-lg;
  font-weight: $weight-bold;
}
.avatar.small { width: 58rpx; height: 58rpx; border-radius: 20rpx; font-size: $uni-font-size-sm; }
.avatar image { width: 100%; height: 100%; }
.author-meta { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 4rpx; }
.author-name { color: $uni-text-primary; font-size: $uni-font-size-sm; font-weight: $weight-semibold; }
.author-time { color: $uni-text-secondary; font-size: $uni-font-size-caption; }
.question-title { display: block; margin-top: $space-5; color: $uni-text-primary; font-size: 42rpx; font-weight: $weight-bold; line-height: 1.3; }
.question-content { display: block; margin-top: $space-4; color: $uni-text-regular; font-size: $uni-font-size-base; line-height: 1.75; white-space: pre-wrap; word-break: break-word; }
.tag-row { display: flex; flex-wrap: wrap; gap: $space-2; margin-top: $space-4; }
.tag { padding: 6rpx 14rpx; border-radius: $uni-border-radius-full; color: $uni-color-primary-dark; background: $uni-color-primary-fog; font-size: $uni-font-size-xs; }
.question-meta { display: flex; align-items: center; gap: $space-4; margin-top: $space-5; padding-top: $space-4; border-top: $hairline solid $uni-border-light; }
.meta-item { display: inline-flex; align-items: center; gap: 8rpx; color: $uni-text-secondary; font-size: $uni-font-size-xs; }
.closed-label { margin-left: auto; color: $uni-color-accent-dark; font-size: $uni-font-size-xs; }

.section-heading { display: flex; align-items: baseline; gap: $space-2; margin: $space-6 0 $space-3; }
.section-title { color: $uni-text-primary; font-size: $uni-font-size-lg; font-weight: $weight-bold; }
.section-count { color: $uni-text-secondary; font-size: $uni-font-size-sm; }
.answer-list { display: flex; flex-direction: column; gap: $space-3; }
.answer-card { padding: $space-4; }
.answer-card.accepted { border-color: rgba(43, 95, 75, .38); box-shadow: 0 8rpx 28rpx rgba(43, 95, 75, .1); }
.answer-head { display: flex; align-items: center; justify-content: space-between; gap: $space-3; }
.accepted-label { display: inline-flex; align-items: center; gap: 4rpx; padding: 6rpx 12rpx; border-radius: $uni-border-radius-full; color: $uni-color-primary-dark; background: $uni-color-primary-fog; font-size: $uni-font-size-caption; font-weight: $weight-semibold; }
.answer-content { display: block; margin-top: $space-4; color: $uni-text-regular; font-size: $uni-font-size-sm; line-height: 1.75; white-space: pre-wrap; word-break: break-word; }
.snapshot-list { display: flex; flex-direction: column; gap: $space-2; margin-top: $space-4; padding-top: $space-3; border-top: $hairline solid $uni-border-light; }
.snapshot { display: flex; gap: $space-2; padding: $space-3; border-radius: $uni-border-radius-sm; background: $uni-color-accent-fog; }
.snapshot-copy { min-width: 0; display: flex; flex-direction: column; gap: 4rpx; }
.snapshot-title { color: $uni-text-primary; font-size: $uni-font-size-xs; font-weight: $weight-semibold; }
.snapshot-summary { color: $uni-text-secondary; font-size: $uni-font-size-caption; line-height: 1.5; @include truncate(2); }

.state-card { min-height: 220rpx; padding: $space-6; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: $space-3; color: $uni-text-secondary; font-size: $uni-font-size-sm; }
.state-card.empty { margin-top: $space-3; box-shadow: none; }
.empty-icon { width: 88rpx; height: 88rpx; display: inline-flex; align-items: center; justify-content: center; border-radius: 28rpx; background: $uni-bg-input; }
.state-hint { color: $uni-text-placeholder; font-size: $uni-font-size-xs; }
</style>
