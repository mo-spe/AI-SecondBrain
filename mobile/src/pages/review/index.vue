<template>
  <view class="review-page">
    <!-- 页头：把工作区切换放到标题旁边，加签到与通知的操作按钮 -->
    <PageHeader
      title="今日复习"
      :subtitle="overview.greeting || '保持节奏，一次只学一张卡'"
      :actions="headerActions"
      @openWorkspace="wsVisible = true"
      @action="onHeaderAction"
    />

    <!-- ========== Hero 区：打破 4 等分卡片 ============= -->
    <!-- 布局：左大卡（进度圆环 + CTA），右侧两小卡堆叠（连胜 / 今日已完成） -->
    <view class="hero">
      <view class="hero-primary">
        <view class="hero-progress" :style="{ background: heroBg }">
          <!-- 环形进度：SVG 画 -->
          <view class="hero-ring-wrap">
            <svg class="hero-ring" viewBox="0 0 120 120" aria-hidden="true">
              <circle cx="60" cy="60" r="50" stroke="rgba(253,252,250,0.22)" stroke-width="8" fill="none" />
              <circle
                class="ring-active"
                cx="60" cy="60" r="50"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="ringOffset"
                stroke-linecap="round"
                stroke="#fdfcfa"
                stroke-width="8"
                fill="none"
              />
            </svg>
            <view class="hero-ring-text">
              <text class="pct">{{ progressPct }}%</text>
              <text class="label">完成率</text>
            </view>
          </view>

          <view class="hero-cta">
            <text class="hero-title">{{ todayPending }} 张卡片等你复习</text>
            <text class="hero-sub">新卡 {{ overview.newCardCount || 0 }} · 待巩固 {{ todayPending - (overview.newCardCount || 0) }}</text>
            <view class="hero-btn" :class="{ disabled: todayPending === 0 }" @tap="goReview">
              <text>开始复习</text>
              <AppIcon name="arrow-right" :size="16" color="#0f3430" />
            </view>
          </view>
        </view>
      </view>

      <view class="hero-side">
        <!-- 连胜卡 —— 黄铜色，质感浮起 -->
        <view class="side-card streak">
          <view class="side-top">
            <view class="side-ico">
              <AppIcon name="fire" :size="18" color="#945225" />
            </view>
            <text class="side-label">连续学习</text>
          </view>
          <view class="side-value">
            <text class="num">{{ streak }}</text>
            <text class="unit">天</text>
          </view>
          <view class="side-bar">
            <view class="side-bar-fill" :style="{ width: Math.min(100, streak * 6) + '%' }"></view>
          </view>
        </view>

        <!-- 今日已完成卡 —— 墨绿 -->
        <view class="side-card done">
          <view class="side-top">
            <view class="side-ico">
              <AppIcon name="circle-check" :size="18" color="#0f3430" />
            </view>
            <text class="side-label">今日已完成</text>
          </view>
          <view class="side-value">
            <text class="num">{{ todayCompleted }}</text>
            <text class="unit">张</text>
          </view>
          <view class="done-mini-list">
            <view
              v-for="(s, i) in masteryDistribution"
              :key="i"
              class="done-chip"
              :class="'lvl-' + s.level"
            >
              <text>L{{ s.level }}</text>
              <text class="chip-count">{{ s.count }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- ========== 功能快捷入口（打破等分：2大 + 2小） ========== -->
    <view class="quick-grid">
      <view class="q-card q-big" @tap="go('checkin')">
        <view class="q-ico gold">
          <AppIcon name="cal-grid" :size="22" color="#945225" />
        </view>
        <view class="q-meta">
          <text class="q-title">每日签到</text>
          <text class="q-sub">连续{{ streak }}天 · 领取今日经验</text>
        </view>
        <AppIcon name="chevron-right" :size="14" color="#bfb29a" />
      </view>

      <view class="q-card q-big" @tap="go('today-cards')">
        <view class="q-ico green">
          <AppIcon name="book-open" :size="22" color="#0f3430" />
        </view>
        <view class="q-meta">
          <text class="q-title">今日待学</text>
          <text class="q-sub">{{ todayPending }}张 · 预计 {{ spend }}分钟</text>
        </view>
        <AppIcon name="chevron-right" :size="14" color="#bfb29a" />
      </view>

      <view class="q-card q-small" @tap="go('achievements')">
        <view class="q-ico purple">
          <AppIcon name="trophy" :size="18" color="#5c4a8d" />
        </view>
        <text class="q-title-sm">成就</text>
      </view>

      <view class="q-card q-small" @tap="go('leaderboard')">
        <view class="q-ico blue">
          <AppIcon name="podium" :size="18" color="#2f5d7a" />
        </view>
        <text class="q-title-sm">排行榜</text>
      </view>
    </view>

    <!-- ========== 推荐任务卡（非对称：左大标题区 + 右视觉芯片） ========== -->
    <view class="section-head">
      <text class="s-title">下阶段任务</text>
      <text class="s-more" @tap="go('review-all')">查看全部</text>
    </view>

    <view class="task-list" v-if="upcomingCards.length">
      <view
        v-for="c in upcomingCards.slice(0, 3)"
        :key="c.cardId"
        class="task-item"
        @tap="goAnswer(c.cardId)"
      >
        <view class="task-text">
          <view class="task-pill" :class="'lvl-' + (c.masteryLevel || 0)">
            L{{ c.masteryLevel || 0 }}
          </view>
          <text class="task-title">{{ strip(c.questionText) }}</text>
          <text class="task-sub">
            {{ formatPool(c.poolName) }} · 下次到期 {{ formatDue(c.dueTime) }}
          </text>
        </view>
        <view class="task-go">
          <AppIcon name="chevron-right" :size="16" color="#6b6458" />
        </view>
      </view>
    </view>

    <view v-else class="empty-soft">
      <AppIcon name="sparkles" :size="22" color="#c9793f" />
      <text>暂无更多卡片，明天再来～</text>
    </view>

    <WorkspaceDrawer
      v-model:visible="wsVisible"
      @changed="onWsChanged"
      @create="onWsCreate"
    />
  </view>
</template>

<script>
import { onMounted, computed, ref } from 'vue'
import { reviewAPI, gamificationAPI } from '@/api'
import { useWorkspaceStore } from '@/stores/workspace'
import { stripQuestionMarkdown, formatPoolName } from '@/utils/review-parser'

export default {
  setup() {
    return { workspaceStore: useWorkspaceStore() }
  },
  data() {
    return {
      wsVisible: false,
      overview: {},
      streak: 0,
      upcomingCards: [],
      loading: true,
    }
  },
  computed: {
    todayPending()  { return Number(this.overview.todayPending || 0) },
    todayCompleted(){ return Number(this.overview.todayCompleted || 0) },
    totalToday()    { return this.todayPending + this.todayCompleted || 1 },
    progressPct()   {
      const v = Math.round((this.todayCompleted / this.totalToday) * 100)
      return Math.min(100, Math.max(0, v))
    },
    circumference() { return 2 * Math.PI * 50 },
    ringOffset()    {
      return this.circumference * (1 - this.progressPct / 100)
    },
    heroBg() {
      // 墨绿 → 墨绿深渐变 + 右上角小角纹，带黄铜光点
      return `linear-gradient(138deg, #174a45 0%, #0f3430 76%, #245850 100%)`
    },
    masteryDistribution() {
      return (this.overview.masteryDistribution || [
        { level: 1, count: 0 }, { level: 3, count: 0 }, { level: 5, count: 0 },
      ]).slice(0, 3)
    },
    spend() {
      // 平均 0.8 min / 卡
      return Math.max(1, Math.round(this.todayPending * 0.8))
    },
    headerActions() {
      return [
        { key: 'notification', icon: 'bell', color: '#3d3832' },
        { key: 'checkin',      icon: 'cal-grid',  color: '#3d3832' },
      ]
    },
  },
  onShow() {
    this.refresh()
  },
  onMounted() {
    this.workspaceStore.fetchWorkspaces()
    this.refresh()
  },
  methods: {
    async refresh() {
      this.loading = true
      try {
        const [o, s, p] = await Promise.allSettled([
          reviewAPI.getOverview(),
          gamificationAPI.getStreak(),
          reviewAPI.getUpcoming({ limit: 10 }),
        ])
        if (o.status === 'fulfilled') this.overview = o.value || {}
        if (s.status === 'fulfilled') this.streak  = (s.value && s.value.currentStreak) || 0
        if (p.status === 'fulfilled') this.upcomingCards = p.value || []
      } finally {
        this.loading = false
      }
    },
    onHeaderAction(k) {
      if (k === 'checkin')      return uni.navigateTo({ url: '/pages/checkin/index' })
      if (k === 'notification') return uni.navigateTo({ url: '/pages/notification/index' })
    },
    onWsChanged() { this.refresh() },
    onWsCreate() {
      uni.showToast({ title: '请在网页版创建工作区', icon: 'none' })
      this.wsVisible = false
    },
    go(p) {
      const map = {
        checkin: '/pages/checkin/index',
        'today-cards': null,
        achievements: '/pages/achievements/index',
        leaderboard:  '/pages/leaderboard/index',
        'review-all': null,
      }
      const url = map[p]
      if (url) uni.navigateTo({ url })
      else if (p === 'today-cards' || p === 'review-all') {
        if (this.todayPending > 0) this.goReview()
      }
    },
    goReview() {
      if (!this.todayPending) {
        uni.showToast({ title: '今天没有待复习的卡了', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/review/answer' })
    },
    goAnswer(id) {
      uni.navigateTo({ url: `/pages/review/answer?cardId=${id}` })
    },
    strip(text)    { return stripQuestionMarkdown(text).slice(0, 50) },
    formatPool(p)  { return formatPoolName(p) },
    formatDue(t)   {
      if (!t) return ''
      const now = Date.now()
      const d   = new Date(t).getTime()
      const dh  = Math.max(0, Math.round((d - now) / 3600000))
      if (dh === 0) return '今天内'
      if (dh < 24) return dh + '小时后'
      return Math.round(dh / 24) + '天后'
    },
  },
}
</script>

<style lang="scss" scoped>
.review-page {
  min-height: 100vh;
  background: $uni-bg-page;
  padding-bottom: 120rpx;
}

/* ============= Hero ============= */
.hero {
  display: grid;
  /* 拒绝等分：左 1.618 : 右 1 */
  grid-template-columns: 1.618fr 1fr;
  gap: $space-4;
  padding: $space-5 $space-5 $space-3;
}

.hero-progress {
  border-radius: $uni-border-radius-lg;
  padding: $space-5;
  box-shadow: $shadow-primary;
  color: $uni-text-inverse;
  position: relative;
  overflow: hidden;
}

/* 黄铜光点装饰（模拟光线，非 AI 渐变） */
.hero-progress::after {
  content: '';
  position: absolute;
  width: 200rpx;
  height: 200rpx;
  background: radial-gradient(closest-side, rgba(212, 149, 90, 0.35), transparent 70%);
  top: -60rpx;
  right: -40rpx;
}

.hero-ring-wrap {
  position: relative;
  width: 200rpx;
  height: 200rpx;
  margin: 0 auto $space-4;
}

.hero-ring {
  width: 200rpx;
  height: 200rpx;
  transform: rotate(-90deg);
}

.ring-active {
  transition: stroke-dashoffset 0.6s ease;
}

.hero-ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  .pct   { font-size: $uni-font-size-display; font-weight: $weight-bold; line-height: 1; }
  .label { font-size: $uni-font-size-xs; opacity: 0.82; margin-top: 4rpx; }
}

.hero-cta {
  position: relative;
  z-index: 1;
  text-align: center;
}

.hero-title {
  display: block;
  font-size: $uni-font-size-md;
  font-weight: $weight-semibold;
  margin-bottom: 4rpx;
}

.hero-sub {
  display: block;
  font-size: $uni-font-size-xs;
  opacity: 0.82;
  margin-bottom: $space-4;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  padding: $space-3 $space-5;
  border-radius: $uni-border-radius-full;
  background: $uni-color-accent-soft;
  color: $uni-color-primary-dark;
  font-weight: $weight-semibold;
  font-size: $uni-font-size-sm;
  box-shadow: $shadow-accent;
}

.hero-btn.disabled {
  background: rgba(253, 252, 250, 0.22);
  color: $uni-text-inverse;
  box-shadow: none;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: $space-4;
}

.side-card {
  border-radius: $uni-border-radius-lg;
  padding: $space-4;
  box-shadow: $shadow-2;
  flex: 1;
}

.side-card.streak {
  background: linear-gradient(135deg, #fbf1e4, #f7e2c7);
}

.side-card.done {
  background: linear-gradient(145deg, #eaf1ec, #dce8df);
}

.side-top {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-bottom: $space-3;
}

.side-ico {
  width: 48rpx;
  height: 48rpx;
  border-radius: $uni-border-radius-xs;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.7);
}

.side-label {
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
  font-weight: $weight-medium;
}

.side-value {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
  margin-bottom: $space-3;
  .num  { font-size: $uni-font-size-display; font-weight: $weight-bold; color: $uni-text-primary; line-height: 1; }
  .unit { font-size: $uni-font-size-sm; color: $uni-text-secondary; }
}

.side-bar {
  height: 10rpx;
  background: rgba(0,0,0,0.06);
  border-radius: 999rpx;
  overflow: hidden;
}

.side-bar-fill {
  height: 100%;
  background: $uni-color-accent;
  border-radius: 999rpx;
  transition: width 0.4s ease;
}

.done-mini-list {
  display: flex;
  gap: $space-2;
}

.done-chip {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
  padding: 4rpx 12rpx;
  border-radius: $uni-border-radius-xs;
  background: rgba(253, 252, 250, 0.75);
  font-size: $uni-font-size-caption;
  color: $uni-text-secondary;
  font-weight: $weight-semibold;

  .chip-count { color: $uni-text-primary; }
}

/* ============= Quick ============= */
.quick-grid {
  padding: $space-4 $space-5 $space-5;
  display: grid;
  grid-template-columns: 1.618fr 1fr;
  grid-auto-rows: minmax(120rpx, auto);
  gap: $space-4;
}

.q-card {
  background: $uni-bg-card;
  border-radius: $uni-border-radius-base;
  padding: $space-4;
  display: flex;
  align-items: center;
  gap: $space-3;
  border: $hairline solid $uni-border-light;
  box-shadow: $shadow-1;
}

.q-big { grid-column: span 1; }

.q-small {
  flex-direction: column;
  justify-content: center;
  padding: $space-3;
  text-align: center;
  gap: $space-2;
}

.q-ico {
  width: 72rpx;
  height: 72rpx;
  border-radius: $uni-border-radius-sm;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.q-ico.gold   { background: $uni-color-accent-fog; }
.q-ico.green  { background: $uni-color-primary-fog; }
.q-ico.purple { background: rgba(92, 74, 141, 0.14); }
.q-ico.blue   { background: rgba(47, 93, 122, 0.14); }

.q-meta {
  flex: 1;
  min-width: 0;
}

.q-title {
  display: block;
  font-size: $uni-font-size-md;
  font-weight: $weight-semibold;
  color: $uni-text-primary;
  @include truncate;
}

.q-title-sm {
  font-size: $uni-font-size-sm;
  font-weight: $weight-medium;
  color: $uni-text-primary;
}

.q-sub {
  display: block;
  margin-top: 4rpx;
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
  @include truncate;
}

/* ============= Section Head ============= */
.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: $space-4 $space-5 $space-3;
}

.s-title {
  font-size: $uni-font-size-lg;
  font-weight: $weight-bold;
  color: $uni-text-primary;
  position: relative;
  padding-left: $space-3;
}

.s-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12%;
  height: 76%;
  width: 6rpx;
  border-radius: 4rpx;
  background: $uni-color-accent;
}

.s-more {
  font-size: $uni-font-size-xs;
  color: $uni-color-primary;
  font-weight: $weight-medium;
}

/* ============= Task List ============= */
.task-list {
  padding: 0 $space-5 $space-4;
  display: flex;
  flex-direction: column;
  gap: $space-3;
}

.task-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-4;
  background: $uni-bg-card;
  border-radius: $uni-border-radius-base;
  border: $hairline solid $uni-border-light;
  box-shadow: $shadow-1;
}

.task-text { flex: 1; min-width: 0; }

.task-pill {
  display: inline-flex;
  padding: 2rpx 12rpx;
  border-radius: $uni-border-radius-xs;
  font-size: $uni-font-size-caption;
  font-weight: $weight-bold;
  margin-bottom: $space-2;
}

.task-pill.lvl-0 { background: rgba(184,68,58, 0.14); color: #b8443a; }
.task-pill.lvl-1 { background: rgba(201,121,63,0.13); color: #a95e2c; }
.task-pill.lvl-2 { background: rgba(201,121,63,0.13); color: #a95e2c; }
.task-pill.lvl-3 { background: rgba(62,107,139,0.14); color: #2f5d7a; }
.task-pill.lvl-4,.task-pill.lvl-5 { background: $uni-color-primary-fog; color: $uni-color-primary-dark; }

.task-title {
  display: block;
  font-size: $uni-font-size-base;
  font-weight: $weight-medium;
  color: $uni-text-primary;
  @include truncate-2;
}

.task-sub {
  display: block;
  margin-top: 6rpx;
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
  @include truncate;
}

.task-go {
  width: 56rpx;
  height: 56rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: $uni-bg-input;
  flex-shrink: 0;
}

.empty-soft {
  margin: $space-6 $space-5;
  padding: $space-6;
  border-radius: $uni-border-radius-base;
  background: $uni-bg-card;
  border: $hairline dashed $uni-border-base;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-3;
  color: $uni-text-secondary;
  font-size: $uni-font-size-sm;
}
</style>
