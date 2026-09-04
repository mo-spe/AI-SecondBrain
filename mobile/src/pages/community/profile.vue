<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { communityAPI } from '@/api/community'
import { useUserStore } from '@/stores/user'

const AV_BG = ['#2b5f4b', '#b8723a', '#5c4a8d', '#2f5d7a', '#a05463', '#6b5a3b']
const avBg   = (n) => AV_BG[String(n || '?').charCodeAt(0) % AV_BG.length]
const avInit = (n) => (String(n || '?').charAt(0) || '?').toUpperCase()

const { userInfo } = useUserStore()
const uid = ref(null)
const loading = ref(true)
const profile = ref(null)
const tab = ref('answers') // answers / questions / knowledge
const actionLoading = ref(false)
const answers = ref([])
const questions = ref([])
const knowledgePosts = ref([])

const load = async () => {
  loading.value = true
  try {
    profile.value = await communityAPI.getUserProfile(uid.value)
    answers.value = profile.value?.recentAnswers || []
    questions.value = profile.value?.recentQuestions || []
    knowledgePosts.value = profile.value?.recentKnowledgePosts || []
  } catch (e) {
    /* request 已 toast */
  } finally {
    loading.value = false
  }
}

const isSelf = computed(() => {
  if (!profile.value) return false
  return (String(profile.value.userId || profile.value.id || '') === String(userInfo.value?.id || ''))
})

const toggleFollow = async () => {
  if (!profile.value || actionLoading.value || profile.value.isSelf || profile.value.isBlocked) return
  actionLoading.value = true
  const was = !!profile.value.isFollowing
  profile.value.isFollowing = !was
  profile.value.followerCount = Math.max(0, (profile.value.followerCount || 0) + (was ? -1 : 1))
  try {
    if (was) await communityAPI.unfollowUser(profile.value.userId)
    else     await communityAPI.followUser(profile.value.userId)
  } catch (e) {
    profile.value.isFollowing = was
    profile.value.followerCount = Math.max(0, (profile.value.followerCount || 0) + (was ? 1 : -1))
  } finally {
    actionLoading.value = false
  }
}

const showBlockedHint = () => {
  uni.showToast({ title: '该主页暂不可互动', icon: 'none' })
}

const openContribution = (item) => {
  if (!item) return
  if (item.type === 'QUESTION' || item.type === 'ANSWER') {
    uni.navigateTo({ url: `/pages/community/detail?id=${item.id}` })
    return
  }
  uni.showToast({ title: '知识文章详情请前往知识页查看', icon: 'none' })
}

onLoad((q) => {
  uid.value = q.id || userInfo.value?.id
  load()
})
</script>

<template>
  <view class="cp-page">

    <view class="cp-hero">
      <view class="cp-hbg"></view>

      <view class="cp-nav">
        <view class="cp-back" @tap="uni.navigateBack()">
          <AppIcon name="chevron-left" :size="18" color="#fdfcfa" />
        </view>
        <view class="cp-spacer"></view>
        <view class="cp-share"><AppIcon name="share" :size="16" color="#fdfcfa" /></view>
      </view>

      <!-- 身份卡：非对称 左头像 右数据 -->
      <view class="cp-id">
        <view class="cp-av-wrap">
          <view class="cp-av" :style="{ background: avBg(profile?.username) }">
            <text>{{ avInit(profile?.username || profile?.nickname || userInfo?.username) }}</text>
          </view>
          <view v-if="profile?.badge" class="cp-badge">{{ profile.badge }}</view>
        </view>

        <view class="cp-meta">
          <text class="cp-name">{{ profile?.username || userInfo?.username || '学者' }}</text>
          <text class="cp-bio">{{ profile?.introduction || '以文会友，以友辅仁' }}</text>
          <view class="cp-tags">
            <view v-for="(item, index) in (profile?.expertiseTags || []).slice(0, 4)" :key="index" class="cp-tag">
              <text>{{ item }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 数据 3 栏：非等分 -- 最大的是 回答/贡献 -->
      <view class="cp-stats">
        <view class="cps cps-big">
          <text class="cps-n">{{ profile?.answerCount || 0 }}</text>
          <text class="cps-l">回答</text>
          <view class="cps-bar"></view>
        </view>
        <view class="cps cps-sml">
          <text class="cps-n">{{ profile?.questionCount || 0 }}</text>
          <text class="cps-l">提问</text>
        </view>
        <view class="cps cps-sml">
          <text class="cps-n">{{ profile?.followerCount || 0 }}</text>
          <text class="cps-l">粉丝</text>
        </view>
        <view class="cps cps-sml">
          <text class="cps-n">{{ profile?.followingCount || 0 }}</text>
          <text class="cps-l">关注</text>
        </view>
      </view>

      <!-- 操作行 -->
      <view class="cp-actions">
        <template v-if="isSelf">
          <view class="cpa cpa-fill" @tap="uni.navigateTo({ url: '/pages/profile/settings' })">
            <AppIcon name="edit" :size="14" color="#fdfcfa" />
            <text>编辑资料</text>
          </view>
          <view class="cpa cpa-ghost" @tap="uni.navigateTo({ url: '/pages/notification/index' })">
            <AppIcon name="bell" :size="14" color="#2b5f4b" />
            <text>通知中心</text>
          </view>
        </template>
        <template v-else>
          <view
            class="cpa"
            :class="profile?.isFollowing ? 'cpa-ghost' : 'cpa-fill'"
            @tap="profile?.isBlocked ? showBlockedHint() : toggleFollow()"
          >
            <AppIcon
              :name="profile?.isFollowing ? 'user-minus' : 'user-plus'"
              :size="14"
              :color="profile?.isFollowing ? '#2b5f4b' : '#fdfcfa'"
            />
            <text>{{ actionLoading ? '处理中' : (profile?.isFollowing ? '已关注' : '关注 TA') }}</text>
          </view>
          <view v-if="profile?.isBlocked" class="cpa cpa-outline" @tap="showBlockedHint">
            <AppIcon name="circle-alert" :size="14" color="#6b6458" />
            <text>互动已隐藏</text>
          </view>
          <view v-else class="cpa cpa-outline" @tap="showBlockedHint">
            <AppIcon name="circle-help" :size="14" color="#2b5f4b" />
            <text>关注后再交流</text>
          </view>
        </template>
      </view>

    </view>

    <!-- Tab bar 3 格（打破等分：收藏最小） -->
    <view class="tabs">
      <view
        class="tab" :class="{ on: tab === 'answers' }"
        style="flex: 1.4"
        @tap="tab = 'answers'"
      >
        <text class="t-name">TA 的回答</text>
        <view v-if="tab === 'answers'" class="t-bar"></view>
      </view>
      <view
        class="tab" :class="{ on: tab === 'questions' }"
        style="flex: 1"
        @tap="tab = 'questions'"
      >
        <text class="t-name">TA 的提问</text>
        <view v-if="tab === 'questions'" class="t-bar"></view>
      </view>
      <view
        class="tab" :class="{ on: tab === 'knowledge' }"
        style="flex: 0.7"
        @tap="tab = 'knowledge'"
      >
        <text class="t-name">知识文章</text>
        <view v-if="tab === 'knowledge'" class="t-bar"></view>
      </view>
    </view>

    <!-- 内容列表 -->
    <view class="content">
      <template v-if="tab === 'answers'">
        <view v-if="answers.length" class="feed">
          <view v-for="(a, i) in answers" :key="i" class="f-item" @tap="openContribution(a)">
            <view class="fi-head">
              <text class="fi-q">{{ a.title || '公开回答' }}</text>
              <text class="fi-time">{{ String(a.createTime || '').slice(0, 10) }}</text>
            </view>
            <text class="fi-c">{{ String(a.summary || '').replace(/<[^>]+>/g, '').slice(0, 120) }}{{ a.summary ? '…' : '' }}</text>
            <view class="fi-foot">
              <view class="fi-metrics">
                <view class="fi-m"><AppIcon name="message-square" :size="11" color="#2b5f4b" /><text>公开回答</text></view>
              </view>
              <text class="fi-link">查看问题 →</text>
            </view>
          </view>
        </view>
        <view v-else class="cp-empty">
          <view class="e-ico"><AppIcon name="message-square" :size="36" color="#bfb29a" /></view>
          <text class="e-t">还没有回答</text>
          <text class="e-s">在社区留下第一个有温度的回答吧</text>
        </view>
      </template>

      <template v-else-if="tab === 'questions'">
        <view v-if="questions.length" class="feed">
          <view v-for="(q, i) in questions" :key="i" class="f-item" @tap="openContribution(q)">
            <view class="fi-head">
              <text class="fi-q">{{ q.title || '公开提问' }}</text>
              <text class="fi-time">{{ String(q.createTime || '').slice(0, 10) }}</text>
            </view>
            <text class="fi-c">{{ String(q.summary || '').replace(/<[^>]+>/g, '').slice(0, 120) }}{{ q.summary ? '…' : '' }}</text>
            <view class="fi-foot">
              <view class="fi-metrics">
                <view class="fi-m"><AppIcon name="newspaper" :size="11" color="#6b6458" /><text>公开问题</text></view>
              </view>
              <view v-if="q.tags && q.tags.length" class="fi-tags">
                <text v-for="(t, ti) in q.tags.slice(0, 3)" :key="ti" class="fi-tag">#{{ t }}</text>
              </view>
            </view>
          </view>
        </view>
        <view v-else class="cp-empty">
          <view class="e-ico"><AppIcon name="layers" :size="36" color="#bfb29a" /></view>
          <text class="e-t">还没有提问</text>
          <text class="e-s">好问题，是好答案的一半</text>
        </view>
      </template>

      <template v-else>
        <view v-if="knowledgePosts.length" class="book-grid">
          <view v-for="(b, i) in knowledgePosts" :key="i" class="b-item" @tap="openContribution(b)">
            <text class="bi-t">{{ b.title || '未命名知识文章' }}</text>
            <text v-if="b.summary" class="bi-summary">{{ b.summary }}</text>
            <view class="bi-foot">
              <text class="bi-from">公开知识文章</text>
              <text class="bi-date">{{ String(b.createTime || '').slice(5, 10) }}</text>
            </view>
          </view>
        </view>
        <view v-else class="cp-empty">
          <view class="e-ico"><AppIcon name="bookmark" :size="36" color="#bfb29a" /></view>
          <text class="e-t">还没有公开知识文章</text>
          <text class="e-s">把值得分享的知识整理出来</text>
        </view>
      </template>
    </view>

  </view>
</template>

<style lang="scss" scoped>
.cp-page {
  min-height: 100vh;
  background: $uni-bg-page;
  padding-bottom: 120rpx;
}

/* ===== Hero ===== */
.cp-hero {
  position: relative;
  padding-bottom: $space-5;
  overflow: hidden;
}

.cp-hbg {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 620rpx;
  background: linear-gradient(170deg, #2b5f4b 0%, #3c7b61 55%, #d1a774 140%);
  z-index: 0;
}
.cp-hbg::after {
  content: '';
  position: absolute;
  right: -80rpx;
  top: -40rpx;
  width: 380rpx;
  height: 380rpx;
  background: radial-gradient(closest-side, rgba(212,149,90,0.5), transparent 70%);
}

.cp-nav {
  position: relative;
  z-index: 2;
  /* uni-status-bar-height 是 H5/App 的 CSS 变量；scss 编译时不认识它，因此用 CSS var 表达。
     小程序顶部由 PageHeader/默认 head 处理，这里给一个稳定 padding 即可。 */
  padding: var(--status-bar-height, 44px) $space-5 $space-2;
  display: flex;
  align-items: center;
}

.cp-back, .cp-share {
  width: 64rpx;
  height: 64rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: $uni-border-radius-sm;
  background: rgba(253, 252, 250, 0.16);
  backdrop-filter: blur(8rpx);
}
.cp-spacer { flex: 1; }

.cp-id {
  position: relative;
  z-index: 2;
  padding: $space-3 $space-5;
  display: grid;
  grid-template-columns: 170rpx 1fr;
  gap: $space-4;
  align-items: center;
  color: #fdfcfa;
}

.cp-av-wrap { position: relative; width: 170rpx; }

.cp-av {
  width: 160rpx;
  height: 160rpx;
  border-radius: 44rpx;
  border: 4rpx solid rgba(253, 252, 250, 0.75);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 64rpx;
  font-weight: $weight-bold;
  color: #fdfcfa;
  box-shadow: 0 18rpx 48rpx rgba(0,0,0,0.22);
}

.cp-badge {
  position: absolute;
  left: 8rpx;
  bottom: -10rpx;
  padding: 4rpx 14rpx;
  background: $uni-color-accent;
  color: #fdfcfa;
  font-size: $uni-font-size-caption;
  font-weight: $weight-bold;
  border-radius: $uni-border-radius-full;
  box-shadow: 0 4rpx 10rpx rgba(141, 87, 40, 0.4);
}

.cp-meta { min-width: 0; }

.cp-name {
  display: block;
  font-size: $uni-font-size-xl;
  font-weight: $weight-bold;
  line-height: 1.2;
  @include truncate;
}
.cp-bio {
  display: block;
  margin-top: $space-2;
  font-size: $uni-font-size-sm;
  opacity: 0.9;
  @include truncate(2);
}

.cp-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
  margin-top: $space-3;
}

.cp-role {
  padding: 4rpx 14rpx;
  background: rgba(253, 252, 250, 0.2);
  color: #fdfcfa;
  font-size: $uni-font-size-caption;
  border-radius: $uni-border-radius-xs;
}
.cp-tag {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
  padding: 4rpx 12rpx;
  background: rgba(253, 252, 250, 0.15);
  color: rgba(253, 252, 250, 0.9);
  font-size: $uni-font-size-caption;
  border-radius: $uni-border-radius-xs;
}

/* ===== Stats ===== */
.cp-stats {
  position: relative;
  z-index: 3;
  margin: $space-3 $space-5 0;
  padding: $space-4;
  background: rgba(253, 252, 250, 0.96);
  border-radius: $uni-border-radius-lg;
  display: grid;
  grid-template-columns: 1.618fr 1fr 1fr 1fr; /* 非等分：主数据 回答 占大头 */
  gap: $space-2;
  box-shadow: $shadow-2;
}

.cps {
  padding: $space-2;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
}

.cps-big {
  background: linear-gradient(135deg, #f3ece0, #e6d4b1);
  border-radius: $uni-border-radius-base;
  padding: $space-3;
}

.cps-n {
  font-size: $uni-font-size-xl;
  font-weight: $weight-bold;
  color: $uni-text-primary;
  line-height: 1.1;
}
.cps-l {
  margin-top: 6rpx;
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
}

.cps-bar {
  position: absolute;
  left: $space-3;
  bottom: 12rpx;
  height: 6rpx;
  width: 60rpx;
  border-radius: 999rpx;
  background: linear-gradient(90deg, $uni-color-primary, $uni-color-accent);
}

/* ===== Actions ===== */
.cp-actions {
  position: relative;
  z-index: 2;
  padding: $space-4 $space-5 0;
  display: flex;
  gap: $space-3;
}

.cpa {
  flex: 1;
  height: 80rpx;
  border-radius: $uni-border-radius-full;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  font-size: $uni-font-size-sm;
  font-weight: $weight-semibold;
}

.cpa-fill {
  background: linear-gradient(135deg, $uni-color-primary-dark, $uni-color-primary);
  color: #fdfcfa;
  box-shadow: $shadow-1;
}

.cpa-ghost {
  background: rgba(43, 95, 75, 0.1);
  color: $uni-color-primary-dark;
  border: $hairline solid $uni-color-primary-soft;
}

.cpa-outline {
  background: rgba(253, 252, 250, 0.95);
  color: $uni-color-primary-dark;
  border: $hairline solid $uni-border-light;
}

/* ===== Tabs ===== */
.tabs {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: stretch;
  padding: $space-2 $space-5;
  background: $uni-bg-page;
  border-bottom: $hairline solid $uni-border-light;
  gap: $space-3;
}

.tab {
  position: relative;
  padding: $space-2 0 $space-3;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.t-name {
  font-size: $uni-font-size-sm;
  color: $uni-text-regular;
  font-weight: $weight-medium;
}

.tab.on .t-name {
  font-size: $uni-font-size-md;
  font-weight: $weight-bold;
  color: $uni-text-primary;
}

.t-bar {
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 40rpx;
  height: 6rpx;
  border-radius: 999rpx;
  background: linear-gradient(90deg, $uni-color-primary, $uni-color-accent);
}

/* ===== Feed ===== */
.content {
  padding: $space-4 $space-5 0;
}

.feed { display: flex; flex-direction: column; gap: $space-4; }

.f-item {
  background: $uni-bg-card;
  border-radius: $uni-border-radius-lg;
  padding: $space-4;
  border: $hairline solid $uni-border-light;
  box-shadow: $shadow-1;
}

.fi-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: $space-3;
  margin-bottom: $space-2;
}

.fi-q {
  flex: 1;
  min-width: 0;
  font-size: $uni-font-size-base;
  font-weight: $weight-semibold;
  color: $uni-text-primary;
  @include truncate;
}

.fi-time {
  flex-shrink: 0;
  font-size: $uni-font-size-caption;
  color: $uni-text-secondary;
}

.fi-c {
  display: block;
  font-size: $uni-font-size-sm;
  color: $uni-text-regular;
  line-height: 1.6;
  @include truncate(2);
}

.fi-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: $space-3;
  gap: $space-3;
  flex-wrap: wrap;
}

.fi-metrics {
  display: inline-flex;
  gap: $space-3;
}

.fi-m {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
}

.fi-link {
  font-size: $uni-font-size-xs;
  color: $uni-color-primary-dark;
  font-weight: $weight-medium;
}

.fi-tags {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6rpx;
}

.fi-tag {
  font-size: $uni-font-size-caption;
  color: $uni-color-primary-dark;
  background: $uni-color-primary-fog;
  padding: 2rpx 12rpx;
  border-radius: $uni-border-radius-full;
}

/* ===== Bookmarks grid ===== */
.book-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $space-3;
}

.b-item {
  background: $uni-bg-card;
  border-radius: $uni-border-radius-base;
  padding: $space-4;
  border: $hairline solid $uni-border-light;
  box-shadow: $shadow-1;
  min-height: 180rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.b-item:nth-child(3n + 1) {
  grid-column: span 2;
  background: linear-gradient(135deg, #fdf3e4, #e8d6ae);
}

.bi-t {
  font-size: $uni-font-size-sm;
  font-weight: $weight-semibold;
  color: $uni-text-primary;
  line-height: 1.4;
  @include truncate(3);
}
.bi-summary {
  display: block;
  margin-top: $space-2;
  color: $uni-text-secondary;
  font-size: $uni-font-size-xs;
  line-height: 1.5;
  @include truncate(2);
}

.bi-foot {
  margin-top: $space-3;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.bi-from { font-size: $uni-font-size-caption; color: $uni-text-secondary; }
.bi-date { font-size: $uni-font-size-caption; color: $uni-color-accent-dark; font-weight: $weight-medium; }

/* ===== Empty ===== */
.cp-empty {
  padding: $space-8 $space-5;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-2;
}
.e-ico {
  width: 120rpx;
  height: 120rpx;
  border-radius: $uni-border-radius-lg;
  background: $uni-bg-card;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: $hairline solid $uni-border-light;
  margin-bottom: $space-2;
}
.e-t { font-size: $uni-font-size-md; color: $uni-text-primary; font-weight: $weight-semibold; }
.e-s { font-size: $uni-font-size-sm; color: $uni-text-secondary; }
</style>
