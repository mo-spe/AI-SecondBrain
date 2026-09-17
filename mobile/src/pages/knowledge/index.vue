<template>
  <view class="knowledge-page">
    <PageHeader
      title="知识库"
      subtitle="整理你的第二大脑，按语义关联而生长"
      :actions="headerActions"
      @openWorkspace="wsVisible = true"
      @action="onHeaderAction"
    />

    <!-- 搜索条（浮起，带分类 chip 下拉） -->
    <view class="search-wrap" @tap="focusSearch">
      <view class="search-bar">
        <AppIcon name="search" :size="16" color="#9b9488" />
        <input
          v-model="keyword"
          confirm-type="search"
          class="search-input"
          placeholder="搜索笔记标题 / 正文 / 标签…"
          @confirm="doSearch"
        />
        <view v-if="keyword" class="search-clear" @tap.stop="clearKeyword">
          <AppIcon name="close" :size="12" color="#6b6458" />
        </view>
        <view class="search-go">
          <AppIcon name="filter" :size="14" color="#6b6458" />
        </view>
      </view>
      <scroll-view
        class="chip-row"
        scroll-x="true"
        :enhanced="true"
        :show-scrollbar="false"
      >
        <view
          v-for="c in categories"
          :key="c.key"
          class="chip"
          :class="{ active: category === c.key }"
          @tap="category = c.key; load(true)"
        >
          <text>{{ c.label }}</text>
          <text v-if="c.count" class="chip-count">{{ c.count }}</text>
        </view>
      </scroll-view>
    </view>

    <!-- 精选区：左 featured 大卡（2 行占比 2:1 视觉黄金比），右 2 小卡 -->
    <view class="featured" v-if="featuredCard">
      <view class="ftr-big" @tap="goDetail(featuredCard.id)">
        <view class="ftr-cover" :style="coverStyle(featuredCard)"></view>
        <view class="ftr-content">
          <view class="ftr-tag">精选</view>
          <text class="ftr-title">{{ featuredCard.title }}</text>
          <text class="ftr-sum">{{ (featuredCard.summary || '').slice(0, 60) }}</text>
          <view class="ftr-foot">
            <view class="ftr-author" v-if="featuredCard.authorName">
              <text class="fa-init">{{ avatarInit(featuredCard.authorName) }}</text>
              <text class="fa-name">{{ featuredCard.authorName }}</text>
            </view>
            <view class="ftr-meta">
              <AppIcon name="eye-open" :size="12" color="#6b6458" />
              <text>{{ featuredCard.viewCount || 0 }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="ftr-small">
        <view
          v-for="(k, i) in featuredSmall"
          :key="'s'+(k.id||i)"
          class="ftr-s"
          @tap="goDetail(k.id)"
        >
          <view class="ftr-s-top">
            <text class="ftr-s-title">{{ k.title }}</text>
            <view class="ftr-s-cover" :style="coverStyle(k, 80)"></view>
          </view>
          <text class="ftr-s-sum">{{ (k.summary || '').slice(0, 38) }}</text>
          <view class="ftr-s-foot">
            <text class="ftr-s-tag">{{ k.type || '笔记' }}</text>
            <text class="ftr-s-date">{{ formatDate(k.createTime) }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 知识瀑布（拒绝等分：双列错落，按封面有无决定高度） -->
    <view class="section-head">
      <text class="s-title">全部知识</text>
      <text class="s-more" @tap="goGraph">知识图谱</text>
    </view>

    <view class="waterfall" v-if="list.length">
      <view class="wf-col">
        <view
          v-for="k in colA"
          :key="k.id"
          class="wf-card"
          @tap="goDetail(k.id)"
        >
          <view v-if="k.cover" class="wf-cover" :style="coverStyle(k, 320)"></view>
          <view class="wf-body">
            <view class="wf-type-row">
              <text class="wf-type">{{ typeText(k.type) }}</text>
              <text v-if="k.wordCount" class="wf-word">{{ wordText(k.wordCount) }}</text>
            </view>
            <text class="wf-title">{{ k.title }}</text>
            <text class="wf-sum" v-if="k.summary">{{ (k.summary).slice(0, 40) }}</text>
            <view class="wf-foot">
              <text class="wf-date">{{ formatDate(k.createTime) }}</text>
              <view class="wf-dots">
                <AppIcon name="bookmark" :size="12" color="#9b9488" />
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="wf-col">
        <view
          v-for="k in colB"
          :key="k.id"
          class="wf-card"
          @tap="goDetail(k.id)"
        >
          <view v-if="k.cover" class="wf-cover" :style="coverStyle(k, 360)"></view>
          <view class="wf-body">
            <view class="wf-type-row">
              <text class="wf-type">{{ typeText(k.type) }}</text>
              <text v-if="k.wordCount" class="wf-word">{{ wordText(k.wordCount) }}</text>
            </view>
            <text class="wf-title">{{ k.title }}</text>
            <text class="wf-sum" v-if="k.summary">{{ (k.summary).slice(0, 46) }}</text>
            <view class="wf-foot">
              <text class="wf-date">{{ formatDate(k.createTime) }}</text>
              <view class="wf-dots">
                <AppIcon name="dots" :size="14" color="#9b9488" />
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-else-if="!loading" class="empty-soft">
      <template v-if="loadError">
        <AppIcon name="circle-alert" :size="22" color="#b8443a" />
        <text>知识库暂时无法加载</text>
        <text class="empty-sub">{{ loadError }}</text>
        <button class="empty-retry" @tap="load(true)">重试</button>
      </template>
      <template v-else>
        <AppIcon name="book-open" :size="22" color="#c9793f" />
        <text>{{ keyword ? '没有匹配的知识点' : '当前工作区还没有知识点' }}</text>
        <text class="empty-sub">{{ keyword ? '换个关键词试试' : '点击右下角按钮创建一条' }}</text>
      </template>
    </view>

    <view v-if="loading && list.length" class="loadmore">加载中…</view>

    <!-- FAB 创建按钮 -->
    <view class="fab" @tap="goCreate">
      <AppIcon name="plus" :size="20" color="#fdfcfa" />
    </view>

    <WorkspaceDrawer
      v-model:visible="wsVisible"
      @changed="onWsChanged"
      @create="onWsCreate"
    />
  </view>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { knowledgeAPI } from '@/api/knowledge'
import { useWorkspaceStore } from '@/stores/workspace'

const COVER_POOL = [
  'linear-gradient(135deg,#174a45,#2f756c)',
  'linear-gradient(135deg,#c9793f,#e2a06a)',
  'linear-gradient(135deg,#5c4a8d,#8e7ac1)',
  'linear-gradient(135deg,#3e6b8b,#6fa1c6)',
  'linear-gradient(135deg,#a05463,#d48c99)',
]

export default {
  setup() {
    return { workspaceStore: useWorkspaceStore() }
  },
  data() {
    return {
      wsVisible: false,
      list: [],
      loading: false,
      loadError: '',
      current: 1,
      size: 12,
      total: 0,
      hasMore: true,
      keyword: '',
      category: 'all',
      categories: [
        { key: 'all',     label: '全部', count: 0 },
        { key: 'note',    label: '笔记' },
        { key: 'article', label: '文章' },
        { key: 'qa',      label: '问答' },
        { key: 'ai',      label: 'AI 笔记' },
      ],
    }
  },
  computed: {
    headerActions() {
      return [
        { key: 'search',   icon: 'search', color: '#3d3832' },
        { key: 'graph',    icon: 'layers', color: '#3d3832' },
      ]
    },
    featuredCard() {
      return this.list.find((k) => k.pinned || k.cover) || this.list[0] || null
    },
    featuredSmall() {
      const rest = this.list.filter((k) => k !== this.featuredCard)
      return rest.slice(0, 2)
    },
    restList() {
      return this.list.filter((k) => k !== this.featuredCard && !this.featuredSmall.includes(k))
    },
    colA() { return this.restList.filter((_, i) => i % 2 === 0) },
    colB() { return this.restList.filter((_, i) => i % 2 === 1) },
  },
  onShow() {
    this.load(true)
  },
  onMounted() {
    this.workspaceStore.fetchWorkspaces()
    this.load(true)
  },
  onPullDownRefresh() {
    this.load(true).finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() { this.load(false) },
  methods: {
    async load(reset) {
      if (this.loading) return
      if (reset) { this.current = 1; this.list = []; this.hasMore = true }
      if (!this.hasMore) return
      this.loading = true
      this.loadError = ''
      try {
        const res = await knowledgeAPI.getList({
          current: this.current,
          size:    this.size,
          keyword: this.keyword || undefined,
          type:    this.category === 'all' ? undefined : this.category,
        })
        const recs = Array.isArray(res?.records)
          ? res.records
          : Array.isArray(res?.list)
            ? res.list
            : (Array.isArray(res) ? res : [])
        this.list = this.list.concat(recs)
        this.total = res && res.total ? res.total : this.list.length
        this.hasMore = this.list.length < this.total && recs.length >= this.size
        this.current += 1
        this.categories[0].count = this.total || 0
      } catch (error) {
        this.loadError = error?.message || '请检查网络或服务地址配置'
      } finally {
        this.loading = false
      }
    },
    doSearch() { this.load(true) },
    clearKeyword() { this.keyword = ''; this.load(true) },
    focusSearch() {},
    onHeaderAction(k) {
      if (k === 'graph')  return this.goGraph()
    },
    onWsChanged() { this.load(true) },
    onWsCreate() {
      uni.showToast({ title: '请在网页版创建工作区', icon: 'none' })
      this.wsVisible = false
    },
    goDetail(id)  { uni.navigateTo({ url: `/pages/knowledge/detail?id=${id}` }) },
    goCreate()    { uni.navigateTo({ url: '/pages/knowledge/create' }) },
    goGraph()     { uni.navigateTo({ url: '/pages/knowledge/graph' }) },

    typeText(t) {
      return (
        { note: '笔记', article: '文章', qa: '问答', ai: 'AI 笔记', 'node-imported': '导入' }[t] || '笔记'
      )
    },
    wordText(n) {
      if (!n) return ''
      if (n < 1000) return n + ' 字'
      return (n / 1000).toFixed(1) + 'k'
    },
    formatDate(t) {
      if (!t) return ''
      const d = new Date(t)
      const now = new Date()
      if (d.toDateString() === now.toDateString()) return '今天'
      const diff = Math.round((now - d) / 86400000)
      if (diff < 7) return diff + ' 天前'
      return (d.getMonth() + 1) + '月' + d.getDate() + '日'
    },
    avatarInit(name) { return (name || '?').charAt(0).toUpperCase() },
    /* 封面：真实 cover 优先，否则按 hash 取色板，保证稳定 */
    coverStyle(k, height) {
      const base =
        k.cover
          ? `background-image:url(${k.cover});background-size:cover;background-position:center;`
          : `background-image:${COVER_POOL[(Number(k.id) || this.hashStr(k.title || '')) % COVER_POOL.length]};`
      return base + (height ? `height:${height}rpx;` : '')
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
.knowledge-page {
  min-height: 100vh;
  background: $uni-bg-page;
  padding-bottom: 160rpx;
}

.search-wrap {
  padding: 0 $space-5 $space-4;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: $space-3 $space-4;
  background: $uni-bg-card;
  border: $hairline solid $uni-border-light;
  border-radius: $uni-border-radius-base;
  box-shadow: $shadow-1;
}

.search-input {
  flex: 1;
  font-size: $uni-font-size-sm;
  color: $uni-text-primary;
  background: transparent;
}

.search-clear {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: $uni-bg-input;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.search-go {
  width: 60rpx;
  height: 56rpx;
  border-radius: $uni-border-radius-sm;
  background: $uni-bg-input;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.chip-row {
  white-space: nowrap;
  margin-top: $space-4;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  border-radius: $uni-border-radius-full;
  background: $uni-bg-card;
  border: $hairline solid $uni-border-light;
  margin-right: $space-2;
  font-size: $uni-font-size-xs;
  color: $uni-text-regular;
}

.chip.active {
  background: $uni-color-primary;
  color: #fdfcfa;
  border-color: $uni-color-primary;
}

.chip-count {
  padding: 0 8rpx;
  border-radius: $uni-border-radius-xs;
  background: rgba(255, 255, 255, 0.22);
  font-size: $uni-font-size-caption;
}

/* ===== Featured ===== */
.featured {
  padding: 0 $space-5 $space-4;
  display: grid;
  grid-template-columns: 1.618fr 1fr;
  gap: $space-4;
}

.ftr-big {
  background: $uni-bg-card;
  border-radius: $uni-border-radius-lg;
  overflow: hidden;
  border: $hairline solid $uni-border-light;
  box-shadow: $shadow-2;
  display: flex;
  flex-direction: column;
}

.ftr-cover {
  height: 220rpx;
  background: $uni-color-primary;
  position: relative;
}

.ftr-cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(30, 27, 24, 0.4) 100%);
}

.ftr-content {
  padding: $space-4;
}

.ftr-tag {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: $uni-border-radius-xs;
  background: $uni-color-accent;
  color: #fdfcfa;
  font-size: $uni-font-size-caption;
  font-weight: $weight-semibold;
  margin-bottom: $space-2;
}

.ftr-title {
  display: block;
  font-size: $uni-font-size-lg;
  font-weight: $weight-bold;
  color: $uni-text-primary;
  line-height: 1.3;
  @include truncate-2;
}

.ftr-sum {
  display: block;
  margin-top: 6rpx;
  font-size: $uni-font-size-sm;
  color: $uni-text-secondary;
  @include truncate-2;
  min-height: 74rpx;
}

.ftr-foot {
  margin-top: $space-3;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ftr-author {
  display: inline-flex;
  align-items: center;
  gap: $space-2;
}

.fa-init {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: $uni-color-primary-fog;
  color: $uni-color-primary-dark;
  font-size: $uni-font-size-xs;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: $weight-bold;
}

.fa-name { font-size: $uni-font-size-xs; color: $uni-text-regular; }

.ftr-meta {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
}

.ftr-small {
  display: flex;
  flex-direction: column;
  gap: $space-4;
}

.ftr-s {
  flex: 1;
  padding: $space-4;
  background: $uni-bg-card;
  border: $hairline solid $uni-border-light;
  border-radius: $uni-border-radius-lg;
  box-shadow: $shadow-1;
  display: flex;
  flex-direction: column;
}

.ftr-s-top {
  display: flex;
  gap: $space-3;
  align-items: flex-start;
}

.ftr-s-title {
  flex: 1;
  font-size: $uni-font-size-md;
  font-weight: $weight-semibold;
  color: $uni-text-primary;
  line-height: 1.3;
  @include truncate-2;
}

.ftr-s-cover {
  width: 120rpx;
  height: 100rpx;
  border-radius: $uni-border-radius-sm;
  flex-shrink: 0;
  background-size: cover;
}

.ftr-s-sum {
  margin-top: $space-3;
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
  flex: 1;
  @include truncate;
}

.ftr-s-foot {
  margin-top: $space-3;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ftr-s-tag {
  padding: 2rpx 12rpx;
  border-radius: $uni-border-radius-xs;
  background: $uni-color-primary-fog;
  color: $uni-color-primary-dark;
  font-size: $uni-font-size-caption;
  font-weight: $weight-semibold;
}

.ftr-s-date { font-size: $uni-font-size-caption; color: $uni-text-placeholder; }

/* ===== Section head (same as review page) ===== */
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
  display: inline-flex;
  align-items: center;
}

/* ===== Waterfall ===== */
.waterfall {
  padding: 0 $space-5 $space-5;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $space-3;
  align-items: flex-start; /* 错落：关键 */
}

.wf-col {
  display: flex;
  flex-direction: column;
  gap: $space-3;
}

.wf-card {
  background: $uni-bg-card;
  border: $hairline solid $uni-border-light;
  border-radius: $uni-border-radius-base;
  overflow: hidden;
  box-shadow: $shadow-1;
}

.wf-cover {
  width: 100%;
  background-size: cover;
  background-position: center;
}

.wf-body { padding: $space-3; }

.wf-type-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-2;
}

.wf-type {
  padding: 2rpx 12rpx;
  border-radius: $uni-border-radius-xs;
  background: $uni-bg-badge;
  color: $uni-text-secondary;
  font-size: $uni-font-size-caption;
  font-weight: $weight-semibold;
}

.wf-word {
  font-size: $uni-font-size-caption;
  color: $uni-text-placeholder;
}

.wf-title {
  display: block;
  font-size: $uni-font-size-base;
  font-weight: $weight-semibold;
  color: $uni-text-primary;
  line-height: 1.35;
  @include truncate-2;
}

.wf-sum {
  display: block;
  margin-top: $space-2;
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
  line-height: 1.5;
  @include truncate-2;
  min-height: 66rpx;
}

.wf-foot {
  margin-top: $space-3;
  padding-top: $space-2;
  border-top: $hairline dashed $uni-border-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.wf-date {
  font-size: $uni-font-size-caption;
  color: $uni-text-placeholder;
}

.wf-dots {
  width: 40rpx;
  height: 40rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: $uni-bg-input;
}

.loadmore {
  padding: $space-4;
  text-align: center;
  color: $uni-text-placeholder;
  font-size: $uni-font-size-xs;
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
  gap: $space-2;
  color: $uni-text-secondary;
  font-size: $uni-font-size-sm;
  .empty-sub { color: $uni-text-placeholder; font-size: $uni-font-size-xs; }
}

.empty-retry {
  min-width: 150rpx;
  height: 68rpx;
  margin-top: $space-2;
  padding: 0 24rpx;
  border: 2rpx solid #bfd5cf;
  border-radius: 18rpx;
  background: #ffffff;
  color: $uni-color-primary;
  font-size: $uni-font-size-sm;
  line-height: 64rpx;
}

.empty-retry::after { border: none; }

/* ===== FAB ===== */
.fab {
  position: fixed;
  right: $space-5;
  bottom: calc(env(safe-area-inset-bottom) + 140rpx);
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: $uni-color-primary;
  color: #fdfcfa;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-primary;
  z-index: 80;
}
</style>
