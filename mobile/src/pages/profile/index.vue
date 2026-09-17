<template>
  <view class="profile-page">
    <!-- 自定义头（取消系统导航栏视觉），把「我的」身份做视觉焦点 -->
    <view class="identity">
      <view class="identity-bg"></view>

      <view class="identity-head">
        <PageHeader
          title=""
          subtitle=""
          :showWorkspace="true"
          :actions="headerActions"
          @openWorkspace="wsVisible = true"
          @action="onHeaderAction"
        />
      </view>

      <view class="identity-body">
        <view class="av-group">
          <view class="av-wrap">
            <view class="avatar" :style="avatarStyle">
              <text class="av-init">{{ avatarInit }}</text>
            </view>
            <view class="level-badge">Lv.{{ user.level || 1 }}</view>
          </view>
          <view class="id-meta">
            <text class="id-name">{{ user.username || userInfo.username || '未登录' }}</text>
            <text class="id-bio">{{ user.bio || userInfo.bio || '让知识和记忆，都有处可去' }}</text>
            <view class="id-tag-row">
              <view class="id-tag">
                <AppIcon name="fire" :size="11" color="#c9793f" />
                <text>连续 {{ streak }} 天</text>
              </view>
              <view class="id-tag" v-if="user.role">
                <AppIcon name="badge" :size="11" color="#174a45" />
                <text>{{ roleText(user.role) }}</text>
              </view>
            </view>
          </view>
          <view class="id-edit" @tap="goEdit">
            <AppIcon name="edit" :size="14" color="#6b6458" />
          </view>
        </view>

        <!-- 非等分三数：左边大（XP 进度条），右边两格小数据 -->
        <view class="stats">
          <view class="stat-main">
            <view class="stat-label-row">
              <text class="stat-label">经验值</text>
              <text class="stat-val">{{ user.exp || 0 }}<text class="unit"> / {{ nextLevelExp }} XP</text></text>
            </view>
            <view class="xp-bar">
              <view class="xp-fill" :style="{ width: xpPct + '%' }"></view>
            </view>
            <text class="stat-hint">再学 {{ Math.max(0, nextLevelExp - (user.exp||0)) }} XP 升级到 Lv.{{ (user.level||1) + 1 }}</text>
          </view>
          <view class="stat-side">
            <view class="s-card s-card-a">
              <text class="s-num">{{ stats.reviewedTotal || 0 }}</text>
              <text class="s-lab">累计复习</text>
            </view>
            <view class="s-card s-card-b">
              <text class="s-num">{{ stats.notesCount || 0 }}</text>
              <text class="s-lab">知识笔记</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 成就徽章条 -->
    <view class="badges">
      <view class="b-head">
        <text class="b-title">成就徽章</text>
        <text class="b-more" @tap="go('achievements')">全部 {{ (badges || []).length }} 枚</text>
      </view>
      <scroll-view
        class="b-row"
        scroll-x="true"
        :enhanced="true"
        :show-scrollbar="false"
      >
        <view v-if="badgeList.length === 0" class="b-empty">完成一次复习或签到后，这里会显示你的真实成就</view>
        <view v-for="(b, i) in badgeList" :key="i" class="b-item" :class="['tier-' + (b.tier || 1), { off: !b.unlocked }]">
          <view class="b-ico">
            <AppIcon :name="b.icon || 'trophy'" :size="22" :color="b.unlocked ? '#fdfcfa' : '#bfb29a'" />
          </view>
          <text class="b-name">{{ b.name }}</text>
        </view>
      </scroll-view>
    </view>

    <!-- 学习数据（mini 柱状图） -->
    <view class="chart-card">
      <view class="cc-head">
        <text class="cc-title">近 7 天学习时长</text>
        <view class="cc-chip">
          <AppIcon name="chart-bar" :size="12" color="#174a45" />
          <text>统计</text>
        </view>
      </view>
      <view class="bars">
        <view
          v-for="(d, i) in weekChart"
          :key="i"
          class="bar-col"
        >
          <view class="bar-wrap">
            <view
              class="bar-fill"
              :style="{ height: d.pct + '%' }"
              :class="{ hot: d.hot }"
            ></view>
          </view>
          <text class="bar-label">{{ d.label }}</text>
          <text class="bar-val">{{ d.min }}m</text>
        </view>
      </view>
    </view>

    <!-- 功能入口：2 行 非等分卡片网格（打破等分 3 图标样板） -->
    <view class="entry-grid">
      <view class="e-big e-checkin" @tap="go('checkin')">
        <view class="e-ico yellow"><AppIcon name="cal-grid" :size="24" color="#945225" /></view>
        <view class="e-meta">
          <text class="e-title">每日签到</text>
          <text class="e-sub">连续{{ streak }}天 · 今日奖励待领取</text>
        </view>
        <view class="e-badge">
          <AppIcon name="fire" :size="14" color="#b8443a" />
        </view>
      </view>

      <view class="e-big e-notif" @tap="go('notification')">
        <view class="e-ico green"><AppIcon name="bell" :size="24" color="#0f3430" /></view>
        <view class="e-meta">
          <text class="e-title">消息通知</text>
          <text class="e-sub">{{ unreadCount }} 条未读</text>
        </view>
        <view v-if="unreadCount" class="e-dot">{{ unreadCount > 99 ? '99+' : unreadCount }}</view>
      </view>

      <view class="e-small" @tap="go('ai-chat')">
        <view class="e-ico purple"><AppIcon name="bot" :size="20" color="#5c4a8d" /></view>
        <text class="e-st">AI 问答</text>
      </view>

      <view class="e-small" @tap="go('research')">
        <view class="e-ico red"><AppIcon name="lab" :size="20" color="#b8443a" /></view>
        <text class="e-st">AI 研究</text>
      </view>

      <view class="e-small" @tap="go('community')">
        <view class="e-ico blue"><AppIcon name="users" :size="20" color="#2f5d7a" /></view>
        <text class="e-st">我的主页</text>
      </view>

      <view class="e-small" @tap="go('settings')">
        <view class="e-ico sand"><AppIcon name="settings" :size="20" color="#6b5a3b" /></view>
        <text class="e-st">设置</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-row">
      <button class="logout-btn" @tap="onLogout">
        <AppIcon name="logout" :size="16" color="#b8443a" />
        <text>退出登录</text>
      </button>
    </view>

    <WorkspaceDrawer
      v-model:visible="wsVisible"
      @changed="onWsChanged"
      @create="onWsCreate"
    />
  </view>
</template>

<script>
import { computed, onMounted } from 'vue'
import { userAPI, gamificationAPI, notificationAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { useWorkspaceStore } from '@/stores/workspace'

const AVATAR_COLORS = [
  ['#174a45', '#ffffff'], ['#c9793f', '#ffffff'], ['#5c4a8d', '#ffffff'],
  ['#3e6b8b', '#fdfcfa'], ['#a05463', '#fdfcfa'],
]

export default {
  setup() {
    return {
      userStore: useUserStore(),
      workspaceStore: useWorkspaceStore(),
    }
  },
  data() {
    return {
      wsVisible: false,
      user: {},
      stats: {},
      badges: [],
      streak: 0,
      unreadCount: 0,
      weekMinutes: [],
    }
  },
  computed: {
    userInfo() { return this.userStore.userInfo || {} },
    headerActions() {
      return [
        { key: 'share',    icon: 'share',       color: '#6b6458' },
        { key: 'settings', icon: 'settings',    color: '#3d3832' },
      ]
    },
    avatarInit() {
      const n = this.user.username || this.userInfo.username || 'U'
      return (n || '?').charAt(0).toUpperCase()
    },
    avatarStyle() {
      const id = Number(this.user.id || this.userInfo.id || 1) || 1
      const [bg, fg] = AVATAR_COLORS[id % AVATAR_COLORS.length]
      return `background:${bg};color:${fg};`
    },
    nextLevelExp() {
      const lv = this.user.level || 1
      return 200 * lv * lv + 200
    },
    xpPct() {
      const v = Math.round(((this.user.exp || 0) / this.nextLevelExp) * 100)
      return Math.min(100, Math.max(0, v))
    },
    badgeList() {
      return Array.isArray(this.badges) ? this.badges : []
    },
    weekChart() {
      const labels = ['一', '二', '三', '四', '五', '六', '日']
      const now = new Date()
      const today = (now.getDay() + 6) % 7
      const mins = (this.weekMinutes && this.weekMinutes.length === 7)
        ? this.weekMinutes.map((value) => Number(value) || 0)
        : [0, 0, 0, 0, 0, 0, 0]
      const max = Math.max(60, ...mins)
      return labels.map((label, i) => {
        const min = mins[i]
        const isToday = i === today
        return {
          label: '周' + label,
          min,
          pct: Math.round((min / max) * 100),
          hot: isToday,
        }
      })
    },
  },
  onShow() { this.refresh() },
  onMounted() {
    this.workspaceStore.fetchWorkspaces()
    this.refresh()
  },
  methods: {
    async refresh() {
      const p = await Promise.allSettled([
        userAPI.getUserInfo(),
        gamificationAPI.getStreak(),
        gamificationAPI.getBadges && gamificationAPI.getBadges(),
        gamificationAPI.getUserStats && gamificationAPI.getUserStats(),
        notificationAPI.getUnreadCount && notificationAPI.getUnreadCount(),
      ])
      if (p[0].status === 'fulfilled') this.user  = p[0].value || {}
      if (p[1].status === 'fulfilled') this.streak = (p[1].value && p[1].value.currentStreak) || 0
      if (p[2].status === 'fulfilled' && p[2].value) this.badges = Array.isArray(p[2].value) ? p[2].value : (p[2].value.records || [])
      if (p[3].status === 'fulfilled' && p[3].value) {
        this.stats = p[3].value || {}
        this.weekMinutes = this.stats.weekMinutes || this.stats.dailyMinutes || []
      }
      if (p[4].status === 'fulfilled' && p[4].value != null) this.unreadCount = Number(p[4].value) || 0
    },
    onHeaderAction(k) {
      if (k === 'settings') return uni.navigateTo({ url: '/pages/profile/settings' })
      if (k === 'share')    return uni.showToast({ title: '分享个人主页：待接入', icon: 'none' })
    },
    onWsChanged() { this.refresh() },
    onWsCreate() {
      uni.showToast({ title: '请在网页版创建工作区', icon: 'none' })
      this.wsVisible = false
    },
    go(p) {
      const map = {
        achievements:   '/pages/achievements/index',
        checkin:        '/pages/checkin/index',
        notification:   '/pages/notification/index',
        'ai-chat':      '/pages/ai/chat',
        research:       '/pages/ai/research',
        community:      '/pages/community/profile',
        settings:       '/pages/profile/settings',
      }
      if (map[p]) uni.navigateTo({ url: map[p] })
    },
    goEdit() {
      uni.navigateTo({ url: '/pages/profile/settings' })
    },
    roleText(r) {
      return ({ SUPER_ADMIN: '超级管理员', ADMIN: '管理员', USER: '用户' })[r] || '用户'
    },
    async onLogout() {
      try {
        await uni.showModal({ title: '退出登录', content: '确定要退出当前账号吗？' })
        this.userStore.logout()
        uni.reLaunch({ url: '/pages/login/index' })
      } catch (_) { /* cancel */ }
    },
  },
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: $uni-bg-page;
  padding-bottom: 160rpx;
  position: relative;
}

/* ===== Identity ===== */
.identity { position: relative; }

.identity-bg {
  position: absolute;
  inset: 0 0 auto 0;
  height: 620rpx;
  background: linear-gradient(155deg, #0f3430 0%, #174a45 56%, #39776d 118%);
}

.identity-bg::after {
  /* 黄铜光斑：模拟光从右上进入 */
  content: '';
  position: absolute;
  top: -80rpx;
  right: -60rpx;
  width: 360rpx;
  height: 360rpx;
  background: radial-gradient(closest-side, rgba(212,149,90,0.55), transparent 70%);
}

.identity-head {
  position: relative;
  z-index: 2;
  background: transparent;
}
.identity-head :deep(.page-header) {
  background: transparent;
  border-bottom: none;
}
.identity-head :deep(.ph-title) { color: #fdfcfa; }
.identity-head :deep(.ph-eyebrow) { color: rgba(255,255,255,0.66); }
.identity-head :deep(.ph-subtitle) { color: rgba(253,252,250,0.82); }
.identity-head :deep(.ph-ws-name) { color: rgba(253,252,250,0.88); }
.identity-head :deep(.ph-workspace) { background: rgba(253,252,250,0.16); }
.identity-head :deep(.ph-action) {
  background: rgba(253,252,250,0.14);
  border-color: rgba(253,252,250,0.22);
}

.identity-body {
  position: relative;
  z-index: 3;
  padding: $space-4 $space-5 0;
  color: $uni-text-inverse;
}

.av-group {
  display: flex;
  align-items: center;
  gap: $space-4;
  padding-bottom: $space-5;
}

.av-wrap {
  position: relative;
  width: 156rpx;
  height: 156rpx;
  flex-shrink: 0;
}

.avatar {
  width: 156rpx;
  height: 156rpx;
  border-radius: 42rpx; /* 拒绝圆：学院派圆角方形头像 */
  border: 4rpx solid rgba(253,252,250,0.7);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: $weight-bold;
  font-size: $uni-font-size-xl;
  box-shadow: 0 16rpx 40rpx rgba(0,0,0,0.2);
}

.level-badge {
  position: absolute;
  right: -8rpx;
  bottom: -8rpx;
  padding: 4rpx 14rpx;
  border-radius: $uni-border-radius-full;
  background: $uni-color-accent;
  color: #fdfcfa;
  font-size: $uni-font-size-caption;
  font-weight: $weight-bold;
  box-shadow: 0 4rpx 12rpx rgba(141, 87, 40, 0.4);
}

.id-meta { flex: 1; min-width: 0; }

.id-name {
  display: block;
  font-size: $uni-font-size-xl;
  font-weight: $weight-bold;
  line-height: 1.2;
  @include truncate;
}

.id-bio {
  display: block;
  margin-top: 6rpx;
  font-size: $uni-font-size-sm;
  opacity: 0.88;
  @include truncate;
}

.id-tag-row {
  display: flex;
  gap: $space-2;
  margin-top: $space-3;
  flex-wrap: wrap;
}

.id-tag {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 6rpx 14rpx;
  border-radius: $uni-border-radius-full;
  background: rgba(253,252,250,0.16);
  font-size: $uni-font-size-xs;
}

.id-edit {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: rgba(253,252,250,0.16);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* ===== Stats ===== */
.stats {
  display: grid;
  /* 拒绝等分：大 1.618 : 小 1 */
  grid-template-columns: 1.618fr 1fr;
  gap: $space-3;
  margin-bottom: $space-5;
}

.stat-main {
  background: $uni-bg-card;
  color: $uni-text-primary;
  border-radius: $uni-border-radius-lg;
  padding: $space-4;
  box-shadow: $shadow-2;
}

.stat-label-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: $space-2;
}
.stat-label { font-size: $uni-font-size-xs; color: $uni-text-secondary; }
.stat-val {
  font-size: $uni-font-size-md;
  font-weight: $weight-bold;
  .unit { font-size: $uni-font-size-xs; font-weight: $weight-regular; color: $uni-text-secondary; }
}

.xp-bar {
  height: 14rpx;
  background: $uni-bg-input;
  border-radius: 999rpx;
  overflow: hidden;
  margin-bottom: $space-2;
}

.xp-fill {
  height: 100%;
  background: linear-gradient(90deg, $uni-color-primary, $uni-color-accent);
  border-radius: 999rpx;
  transition: width 0.5s ease;
}

.stat-hint { font-size: $uni-font-size-caption; color: $uni-text-placeholder; }

.stat-side { display: flex; flex-direction: column; gap: $space-3; }

.s-card {
  flex: 1;
  border-radius: $uni-border-radius-lg;
  padding: $space-4;
  box-shadow: $shadow-1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.s-card-a { background: linear-gradient(135deg, #f7f2e4, #eee0b9); color: #3d3832; }
.s-card-b { background: linear-gradient(135deg, #e7f1ee, #cce1db); color: #0f3430; }

.s-num {
  font-size: $uni-font-size-display;
  font-weight: $weight-bold;
  line-height: 1;
}

.s-lab { font-size: $uni-font-size-xs; color: rgba(0,0,0,0.6); margin-top: 8rpx; }

/* ===== Badges ===== */
.badges {
  margin: 0 $space-5 $space-4;
  background: $uni-bg-card;
  border-radius: $uni-border-radius-lg;
  padding: $space-4;
  box-shadow: $shadow-1;
  border: $hairline solid $uni-border-light;
}

.b-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: $space-3;
}
.b-title { font-size: $uni-font-size-md; font-weight: $weight-bold; color: $uni-text-primary; }
.b-more  { font-size: $uni-font-size-xs; color: $uni-color-primary; font-weight: $weight-medium; }

.b-row { white-space: nowrap; padding-bottom: 4rpx; }

.b-empty {
  padding: 24rpx 8rpx;
  color: $uni-text-placeholder;
  font-size: $uni-font-size-xs;
}

.b-item {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  width: 136rpx;
  padding: $space-3 0;
  margin-right: $space-3;
  border-radius: $uni-border-radius-base;
  background: $uni-bg-page;
  border: $hairline solid $uni-border-light;
}

.b-ico {
  width: 72rpx;
  height: 72rpx;
  border-radius: $uni-border-radius-sm;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $uni-color-primary, $uni-color-primary-dark);
  box-shadow: $shadow-1;
  margin-bottom: $space-2;
}

.b-item.tier-2 .b-ico { background: linear-gradient(135deg, #c9793f, #945225); }
.b-item.tier-3 .b-ico { background: linear-gradient(135deg, #5c4a8d, #3e335e); }
.b-item.tier-4 .b-ico { background: linear-gradient(135deg, #2f5d7a, #1e3c4f); }
.b-item.tier-5 .b-ico {
  background: linear-gradient(135deg, #b8443a, #c9793f);
  box-shadow: $shadow-accent;
}

.b-item.off .b-ico { background: $uni-bg-input; }

.b-name { font-size: $uni-font-size-caption; color: $uni-text-regular; font-weight: $weight-medium; }

/* ===== Chart ===== */
.chart-card {
  margin: 0 $space-5 $space-4;
  background: $uni-bg-card;
  border-radius: $uni-border-radius-lg;
  padding: $space-4;
  box-shadow: $shadow-1;
  border: $hairline solid $uni-border-light;
}

.cc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-4;
}

.cc-title { font-size: $uni-font-size-md; font-weight: $weight-bold; color: $uni-text-primary; }

.cc-chip {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 4rpx 14rpx;
  border-radius: $uni-border-radius-xs;
  background: $uni-color-primary-fog;
  color: $uni-color-primary-dark;
  font-size: $uni-font-size-xs;
  font-weight: $weight-semibold;
}

.bars {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: $space-2;
  align-items: end;
  height: 240rpx;
}

.bar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  height: 100%;
  justify-content: flex-end;
}

.bar-wrap {
  width: 100%;
  flex: 1;
  background: $uni-bg-input;
  border-radius: $uni-border-radius-xs;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
}

.bar-fill {
  width: 100%;
  background: linear-gradient(180deg, $uni-color-primary-soft, $uni-color-primary);
  border-radius: $uni-border-radius-xs;
  min-height: 6rpx;
  transition: height 0.4s ease;
}

.bar-fill.hot {
  background: linear-gradient(180deg, $uni-color-accent-soft, $uni-color-accent-dark);
  box-shadow: 0 4rpx 12rpx rgba(184, 114, 58, 0.28);
}

.bar-label { font-size: $uni-font-size-caption; color: $uni-text-secondary; }
.bar-val   { font-size: $uni-font-size-caption; color: $uni-text-regular; font-weight: $weight-semibold; }

/* ===== Entry ===== */
.entry-grid {
  padding: 0 $space-5 $space-5;
  display: grid;
  /* 非等分：2 大 + 4 小 分三列排布：小 1 大 小 1 大 小 1 小 */
  grid-template-columns: 1fr 1fr 1fr;
  gap: $space-3;
}

.e-big {
  grid-column: span 2;
  background: $uni-bg-card;
  border-radius: $uni-border-radius-base;
  padding: $space-4;
  display: flex;
  align-items: center;
  gap: $space-3;
  box-shadow: $shadow-1;
  border: $hairline solid $uni-border-light;
  position: relative;
  overflow: hidden;
}

.e-checkin::after {
  content: '';
  position: absolute;
  right: -40rpx;
  top: -40rpx;
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  background: radial-gradient(closest-side, rgba(184,114,58,0.18), transparent 70%);
}
.e-notif::after {
  content: '';
  position: absolute;
  right: -40rpx;
  top: -40rpx;
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  background: radial-gradient(closest-side, rgba(43,95,75,0.18), transparent 70%);
}

.e-ico {
  width: 80rpx;
  height: 80rpx;
  border-radius: $uni-border-radius-sm;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.e-ico.yellow { background: $uni-color-accent-fog; }
.e-ico.green  { background: $uni-color-primary-fog; }
.e-ico.purple { background: rgba(92, 74, 141, 0.14); }
.e-ico.red    { background: rgba(184, 68, 58, 0.14); }
.e-ico.blue   { background: rgba(47, 93, 122, 0.14); }
.e-ico.sand   { background: rgba(107, 90, 59, 0.14); }

.e-meta { flex: 1; min-width: 0; z-index: 1; }

.e-title {
  display: block;
  font-size: $uni-font-size-md;
  font-weight: $weight-semibold;
  color: $uni-text-primary;
}

.e-sub {
  display: block;
  margin-top: 4rpx;
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
  @include truncate;
}

.e-badge {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background: rgba(253,252,250,0.7);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.e-dot {
  min-width: 40rpx;
  height: 40rpx;
  padding: 0 10rpx;
  border-radius: 999rpx;
  background: #b8443a;
  color: #fdfcfa;
  font-size: $uni-font-size-caption;
  font-weight: $weight-bold;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.e-small {
  background: $uni-bg-card;
  border-radius: $uni-border-radius-base;
  padding: $space-4 $space-2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-2;
  box-shadow: $shadow-1;
  border: $hairline solid $uni-border-light;
}

.e-st {
  font-size: $uni-font-size-xs;
  color: $uni-text-regular;
  font-weight: $weight-medium;
}

/* ===== Logout ===== */
.logout-row {
  padding: $space-6 $space-5 $space-8;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  border-radius: $uni-border-radius-base;
  background: #fdfcfa;
  border: $hairline solid rgba(184, 68, 58, 0.3);
  color: #b8443a;
  font-size: $uni-font-size-base;
  font-weight: $weight-semibold;
  box-shadow: none;

  &::after { border: none; }
}
</style>
