<template>
  <view class="square-page">
    <PageHeader
      title="知识广场"
      subtitle="把你的知识公开，与同频者一起成长"
      :actions="headerActions"
      @openWorkspace="wsVisible = true"
      @action="onHeaderAction"
    />

    <!-- 频道 tab（非等分：主「推荐」比其他宽） -->
    <view class="channels">
      <view
        v-for="(c, i) in channels"
        :key="c.key"
        class="ch"
        :class="{ active: tab === c.key, hero: i === 0 }"
        @tap="onTab(c.key)"
      >
        <text>{{ c.label }}</text>
        <view v-if="c.badge" class="ch-badge">{{ c.badge }}</view>
      </view>
    </view>

    <!-- 精选卡片（仅在推荐 tab） -->
    <view v-if="tab === 'recommend' && featuredPost" class="feature">
      <view class="feat-card" @tap="goPost(featuredPost.id)">
        <view class="feat-cover" :style="coverStyle(featuredPost)"></view>
        <view class="feat-body">
          <view class="feat-tags">
            <text v-for="(t, i) in (featuredPost.tags||[]).slice(0,2)" :key="i" class="feat-tag">#{{ t }}</text>
          </view>
          <text class="feat-title">{{ featuredPost.title }}</text>
          <view class="feat-foot">
            <view class="feat-author">
              <view class="fa-av" :style="avatarStyle(featuredPost.authorId, featuredPost.authorName)">
                <text>{{ avatarInit(featuredPost.authorName) }}</text>
              </view>
              <text class="fa-name">{{ featuredPost.authorName || '匿名' }}</text>
            </view>
            <view class="feat-actions">
              <view class="act">
                <AppIcon name="eye-open" :size="12" color="#6b6458" />
                <text>{{ featuredPost.viewCount || 0 }}</text>
              </view>
              <view class="act">
                <AppIcon name="heart-stroke" :size="12" :color="featuredPost.liked ? '#b8443a' : '#6b6458'" />
                <text>{{ featuredPost.likeCount || 0 }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- Feed 列表 -->
    <view class="section-head" style="padding-top: 4rpx">
      <text class="s-title">{{ tabTitle }}</text>
      <text class="s-more" @tap="goBookmarks">我的收藏</text>
    </view>

    <view class="feed">
      <view
        v-for="p in feed"
        :key="p.id"
        class="post"
        @tap="goPost(p.id)"
      >
        <view class="post-head">
          <view class="author">
            <view class="a-av" :style="avatarStyle(p.authorId, p.authorName)">
              <text>{{ avatarInit(p.authorName) }}</text>
            </view>
            <view class="a-meta">
              <text class="a-name">{{ p.authorName || '匿名' }}</text>
              <text class="a-time">{{ formatTime(p.createTime) }}</text>
            </view>
          </view>
          <view class="post-menu" @tap.stop>
            <AppIcon name="dots" :size="16" color="#9b9488" />
          </view>
        </view>

        <!-- 布局：如果有封面则图文 1.618 分栏，否则纯文字 -->
        <view class="post-body" :class="{ split: p.cover }">
          <view class="post-text">
            <text class="post-title" v-if="p.title">{{ p.title }}</text>
            <text class="post-content">{{ (p.content || '').slice(0, 120) }}</text>
            <view class="post-tags" v-if="p.tags && p.tags.length">
              <text v-for="(t,i) in p.tags.slice(0, 3)" :key="i">#{{ t }}</text>
            </view>
          </view>
          <view v-if="p.cover" class="post-cover" :style="coverStyle(p)"></view>
        </view>

        <view class="post-foot">
          <view class="stat" :class="{ on: p.liked }" @tap.stop="toggleLike(p)">
            <AppIcon name="heart-stroke" :filled="p.liked" :size="16" :color="p.liked ? '#b8443a' : '#6b6458'" />
            <text>{{ p.likeCount || 0 }}</text>
          </view>
          <view class="stat" @tap.stop="goPost(p.id)">
            <AppIcon name="comment" :size="16" color="#6b6458" />
            <text>{{ p.commentCount || 0 }}</text>
          </view>
          <view class="stat" :class="{ on: p.bookmarked }" @tap.stop="toggleBookmark(p)">
            <AppIcon name="bookmark" :size="16" :color="p.bookmarked ? '#c9793f' : '#66736f'" />
            <text>收藏</text>
          </view>
          <view class="stat share" @tap.stop="onShare(p)">
            <AppIcon name="share" :size="16" color="#6b6458" />
            <text>分享</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!loading && feed.length === 0" class="empty-soft">
      <AppIcon name="newspaper" :size="22" color="#c9793f" />
      <text>还没有公开内容</text>
      <text class="empty-sub">点击右下角发布你的知识</text>
    </view>

    <view v-if="loading" class="loadmore">加载中…</view>

    <!-- FAB 发布 -->
    <view class="fab" @tap="goPublish">
      <AppIcon name="edit2" :size="20" color="#fdfcfa" />
    </view>

    <WorkspaceDrawer
      v-model:visible="wsVisible"
      @changed="onWsChanged"
      @create="onWsCreate"
    />
  </view>
</template>

<script>
import { computed, onMounted, ref } from 'vue'
import { squareAPI } from '@/api/square'
import { useWorkspaceStore } from '@/stores/workspace'

const AVATAR_COLORS = [
  ['#174a45', '#ffffff'], ['#c9793f', '#ffffff'],
  ['#5c4a8d', '#fdfcfa'], ['#3e6b8b', '#fdfcfa'],
  ['#a05463', '#fdfcfa'], ['#2f5d7a', '#fdfcfa'],
]

export default {
  setup() { return { workspaceStore: useWorkspaceStore() } },
  data() {
    return {
      wsVisible: false,
      tab: 'recommend',
      channels: [
        { key: 'recommend', label: '推荐', badge: 'HOT' },
        { key: 'latest',    label: '最新' },
        { key: 'follow',    label: '关注' },
        { key: 'square-qa', label: '问答' },
      ],
      feed: [],
      loading: false,
      current: 1,
      size: 10,
      total: 0,
      hasMore: true,
    }
  },
  computed: {
    headerActions() {
      return [
        { key: 'search',  icon: 'search',  color: '#3d3832' },
        { key: 'qa',      icon: 'circle-help', color: '#3d3832' },
      ]
    },
    tabTitle() {
      return (this.channels.find((c) => c.key === this.tab) || {}).label || '广场'
    },
    featuredPost() {
      if (this.tab !== 'recommend' || !this.feed.length) return null
      const x = this.feed.find((p) => p.pinned || p.likeCount > 5)
      return x || (this.feed.length >= 2 ? this.feed[0] : null)
    },
  },
  onShow() { this.load(true) },
  onMounted() {
    this.workspaceStore.fetchWorkspaces()
    this.load(true)
  },
  onPullDownRefresh() { this.load(true).finally(() => uni.stopPullDownRefresh()) },
  onReachBottom()   { this.load(false) },
  methods: {
    async load(reset) {
      if (this.loading) return
      if (reset) { this.current = 1; this.feed = []; this.hasMore = true }
      if (!this.hasMore) return
      this.loading = true
      try {
        const res = await squareAPI.list({
          current: this.current,
          size: this.size,
          tab: this.tab,
        })
        const rawRecords = (res && res.records) || (Array.isArray(res) ? res : [])
        // 广场后端 VO 以 nodeTitle/nodeSummary/postId 命名，移动卡片统一转换为轻量展示模型。
        const recs = rawRecords.map((item) => ({
          ...item,
          id: item.id || item.postId,
          title: item.title || item.nodeTitle,
          content: item.content || item.nodeSummary || item.recommendText,
          createTime: item.createTime || item.createdAt,
          liked: item.liked ?? item.isLiked,
          bookmarked: item.bookmarked ?? item.isBookmarked,
        }))
        this.feed = this.feed.concat(recs)
        this.total  = res && res.total ? res.total : this.feed.length
        this.hasMore = this.feed.length < this.total && recs.length >= this.size
        this.current += 1
      } finally {
        this.loading = false
      }
    },
    onTab(k) {
      this.tab = k
      if (k === 'square-qa') {
        uni.navigateTo({ url: '/pages/community/index' })
        this.tab = 'recommend'
        return
      }
      this.load(true)
    },
    onHeaderAction(k) {
      if (k === 'qa') return uni.navigateTo({ url: '/pages/community/index' })
    },
    onWsChanged() { this.load(true) },
    onWsCreate() {
      uni.showToast({ title: '请在网页版创建工作区', icon: 'none' })
      this.wsVisible = false
    },
    goPost()       { uni.showToast({ title: '知识详情请前往知识页查看', icon: 'none' }) },
    goPublish()    { uni.showToast({ title: '请在网页版发布知识文章', icon: 'none' }) },
    goBookmarks()  { uni.showToast({ title: '收藏列表将在移动端后续开放', icon: 'none' }) },
    async toggleLike(p) {
      try {
        await squareAPI.like(p.id)
        p.liked = !p.liked
        p.likeCount = (p.likeCount || 0) + (p.liked ? 1 : -1)
      } catch (e) { /* ignore */ }
    },
    async toggleBookmark(p) {
      try {
        await squareAPI.bookmark(p.id)
        p.bookmarked = !p.bookmarked
        uni.showToast({ title: p.bookmarked ? '已收藏' : '已取消', icon: 'none' })
      } catch (e) { /* ignore */ }
    },
    onShare() { uni.showToast({ title: '分享：后续接入小程序分享', icon: 'none' }) },

    avatarInit(n) { return (n || '?').charAt(0).toUpperCase() },
    avatarStyle(id, name) {
      const i = (Number(id) || this.hashStr(name || '')) % AVATAR_COLORS.length
      const [bg, fg] = AVATAR_COLORS[i]
      return `background:${bg};color:${fg};`
    },
    coverStyle(p) {
      if (p && p.cover) return `background-image:url(${p.cover});background-size:cover;background-position:center;`
      const i = (Number(p && p.id) || this.hashStr((p && p.title) || 'c')) % 5
      const gradients = [
        'linear-gradient(135deg,#174a45,#4d8b82)',
        'linear-gradient(135deg,#c9793f,#e6b183)',
        'linear-gradient(135deg,#3e6b8b,#8ab7d4)',
        'linear-gradient(135deg,#5c4a8d,#a18dd0)',
        'linear-gradient(145deg,#a05463,#d48c99)',
      ]
      return `background-image:${gradients[i]};`
    },
    formatTime(t) {
      if (!t) return ''
      const d = new Date(t)
      const now = Date.now()
      const diff = Math.max(0, Math.round((now - d.getTime()) / 1000))
      if (diff < 60) return '刚刚'
      if (diff < 3600) return Math.round(diff / 60) + ' 分钟前'
      if (diff < 86400) return Math.round(diff / 3600) + ' 小时前'
      const day = Math.round(diff / 86400)
      if (day < 7) return day + ' 天前'
      return (d.getMonth() + 1) + '月' + d.getDate() + '日'
    },
    hashStr(s) {
      let h = 0
      for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
      return h
    },
  },
}
</script>

<style lang="scss" scoped>
.square-page {
  min-height: 100vh;
  background: $uni-bg-page;
  padding-bottom: 160rpx;
}

.channels {
  padding: $space-4 $space-5;
  display: flex;
  align-items: center;
  gap: $space-2;
}

.ch {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 14rpx 28rpx;
  border-radius: $uni-border-radius-full;
  background: $uni-bg-card;
  border: $hairline solid $uni-border-light;
  color: $uni-text-regular;
  font-size: $uni-font-size-sm;
}

.ch.hero {
  font-weight: $weight-semibold;
  padding: 14rpx 32rpx;
}

.ch.active {
  background: $uni-text-primary;
  color: #fdfcfa;
  border-color: $uni-text-primary;
  position: relative;
}

.ch-badge {
  font-size: 18rpx;
  padding: 2rpx 10rpx;
  border-radius: $uni-border-radius-xs;
  background: $uni-color-accent;
  color: #fdfcfa;
  font-weight: $weight-bold;
}

/* ========== Feature ========== */
.feature { padding: 0 $space-5 $space-5; }

.feat-card {
  background: $uni-bg-card;
  border-radius: $uni-border-radius-lg;
  overflow: hidden;
  box-shadow: $shadow-2;
  border: $hairline solid $uni-border-light;
  display: flex;
  flex-direction: column;
}

.feat-cover {
  height: 280rpx;
  width: 100%;
  background: $uni-color-primary;
  background-size: cover;
  background-position: center;
}

.feat-body { padding: $space-5; }

.feat-tags { display: flex; gap: $space-2; margin-bottom: $space-3; }

.feat-tag {
  padding: 4rpx 14rpx;
  border-radius: $uni-border-radius-xs;
  background: $uni-color-accent-fog;
  color: $uni-color-accent-dark;
  font-size: $uni-font-size-xs;
  font-weight: $weight-semibold;
}

.feat-title {
  display: block;
  font-size: $uni-font-size-xl;
  font-weight: $weight-bold;
  color: $uni-text-primary;
  line-height: 1.25;
  @include truncate-2;
}

.feat-foot {
  margin-top: $space-4;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.feat-author {
  display: inline-flex;
  align-items: center;
  gap: $space-2;
}

.fa-av {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fdfcfa;
  font-size: $uni-font-size-xs;
  font-weight: $weight-bold;
}

.fa-name { font-size: $uni-font-size-xs; color: $uni-text-regular; }

.feat-actions {
  display: inline-flex;
  gap: $space-3;
  color: $uni-text-secondary;
  font-size: $uni-font-size-xs;
}

.act {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
}

/* ========== Section head ========== */
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
.s-more { font-size: $uni-font-size-xs; color: $uni-color-primary; font-weight: $weight-medium; }

/* ========== Feed ========== */
.feed { padding: 0 $space-5 $space-5; display: flex; flex-direction: column; gap: $space-4; }

.post {
  background: $uni-bg-card;
  border-radius: $uni-border-radius-base;
  padding: $space-4;
  border: $hairline solid $uni-border-light;
  box-shadow: $shadow-1;
}

.post-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-3;
}

.author { display: inline-flex; align-items: center; gap: $space-3; }

.a-av {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: $uni-font-size-sm;
  font-weight: $weight-bold;
}

.a-meta { display: flex; flex-direction: column; }

.a-name {
  font-size: $uni-font-size-sm;
  font-weight: $weight-semibold;
  color: $uni-text-primary;
}

.a-time { font-size: $uni-font-size-caption; color: $uni-text-placeholder; }

.post-menu {
  width: 52rpx;
  height: 52rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: $uni-bg-input;
}

.post-body {
  display: flex;
  flex-direction: column;
  gap: $space-3;
}

.post-body.split {
  flex-direction: row;
  gap: $space-4;
  align-items: flex-start;
}

.post-text { flex: 1; min-width: 0; }

.post-title {
  display: block;
  font-size: $uni-font-size-md;
  font-weight: $weight-bold;
  color: $uni-text-primary;
  line-height: 1.3;
  @include truncate-2;
  margin-bottom: $space-2;
}

.post-content {
  display: block;
  font-size: $uni-font-size-sm;
  color: $uni-text-regular;
  line-height: 1.55;
  @include truncate-3;
}

.post-cover {
  width: 220rpx;
  height: 170rpx;
  border-radius: $uni-border-radius-sm;
  flex-shrink: 0;
  background-size: cover;
  background-position: center;
}

.post-tags {
  display: flex;
  gap: $space-2;
  margin-top: $space-3;
  font-size: $uni-font-size-xs;
  color: $uni-color-primary;
  font-weight: $weight-medium;
}

.post-foot {
  margin-top: $space-4;
  padding-top: $space-3;
  border-top: $hairline solid $uni-border-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  color: $uni-text-secondary;
  font-size: $uni-font-size-xs;
  padding: 8rpx 14rpx;
  border-radius: $uni-border-radius-xs;
}

.stat.on { color: #b8443a; }

.stat.share { color: $uni-text-regular; }

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
  gap: $space-2;
  color: $uni-text-secondary;
  font-size: $uni-font-size-sm;
  .empty-sub { color: $uni-text-placeholder; font-size: $uni-font-size-xs; }
}

.loadmore { padding: $space-4; text-align: center; color: $uni-text-placeholder; font-size: $uni-font-size-xs; }

/* ========== FAB ========== */
.fab {
  position: fixed;
  right: $space-5;
  bottom: calc(env(safe-area-inset-bottom) + 140rpx);
  width: 108rpx;
  height: 108rpx;
  border-radius: 40rpx; /* 拒绝 100% 圆，微方 */
  background: linear-gradient(135deg, $uni-color-primary, $uni-color-primary-dark);
  color: #fdfcfa;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-primary;
  z-index: 80;
}
</style>
