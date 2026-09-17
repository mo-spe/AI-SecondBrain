<script setup>
import { ref, computed, watch } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { communityAPI } from '@/api/community'

// ===== 频道与筛选 =====
const CHANNELS = [
  { key: 'recommend', name: '推荐', hot: true,  weight: 1.618 },
  { key: 'hot',       name: '热议', hot: false, weight: 1 },
  { key: 'newest',    name: '最新', hot: false, weight: 1 },
  { key: 'essence',   name: '精华', hot: false, weight: 1 },
  { key: 'following', name: '关注', hot: false, weight: 1 },
  { key: 'knowledge', name: '知识', hot: false, weight: 1 },
]

const TAGS = [
  '全部', '备考学习', '效率工具', 'AI 协作', '外语',
  '写作表达', '读书笔记', '科研方法', '技术成长',
]

const channel = ref('recommend')
const tag = ref('全部')
const list = ref([])
const loading = ref(false)
const loadError = ref('')
const current = ref(1)
const size = 10
const total = ref(0)
const hasMore = ref(true)

const showCompose = ref(false)
const submitting = ref(false)
const form = ref({ title: '', content: '', tags: [] })
const tagInput = ref('')

// ===== 排序映射 =====
const sortKey = computed(() => {
  if (channel.value === 'newest')    return 'newest'
  if (channel.value === 'hot')       return 'hot'
  if (channel.value === 'essence')   return 'essence'
  if (channel.value === 'following') return 'following'
  return 'recommend'
})

const load = async (reset) => {
  if (loading.value) return
  if (reset) {
    current.value = 1
    hasMore.value = true
  }
  loading.value = true
  loadError.value = ''
  try {
    const data = await communityAPI.listQuestions({
      sort: sortKey.value,
      tag: tag.value === '全部' ? undefined : tag.value,
      current: current.value,
      size,
    })
    const records = (data && data.records) || []
    total.value = (data && data.total) || 0
    list.value = reset ? records : list.value.concat(records)
    hasMore.value = list.value.length < total.value
    if (reset) current.value = 2
    else       current.value += 1
  } catch (e) {
    loadError.value = e?.message || '请检查网络或服务地址配置'
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

const switchChannel = (k) => {
  if (k === channel.value) return
  channel.value = k
  uni.pageScrollTo({ scrollTop: 0, duration: 260 })
  load(true)
}

const switchTag = (t) => {
  if (t === tag.value) return
  tag.value = t
  load(true)
}

// ===== 卡片辅助 =====
const avBg = (name) => {
  const palette = ['#2b5f4b', '#b8723a', '#5c4a8d', '#2f5d7a', '#a05463', '#6b5a3b']
  const s = String(name || '?').charCodeAt(0) || 1
  return palette[s % palette.length]
}
const avInit = (name) => (String(name || '?').charAt(0) || '?').toUpperCase()
const timeText = (t) => {
  if (!t) return ''
  const s = String(t)
  return s.length > 10 ? s.slice(5, 10) : s
}
const contentText = (q) => {
  const c = String(q.content || q.summary || q.description || '').replace(/<[^>]+>/g, '')
  return c.length > 90 ? c.slice(0, 90) + '…' : c
}
const pinTag = (q) => (Array.isArray(q?.tags) && q.tags[0]) || (q?.essence ? '精华回答' : '热门问题')

// 置顶卡片只从接口返回的真实问题中生成，避免推荐区展示与数据库无关的示例内容。
const pinnedQuestions = computed(() =>
  list.value
    .filter((question) => question && (question.top || question.pinned || question.essence))
    .slice(0, 3),
)

// ===== 互动状态（前端即时反馈） =====
const toggleLike = (q) => {
  if (!q) return
  const was = !!q.isLiked
  q.isLiked = !was
  q.likeCount = Math.max(0, (q.likeCount || 0) + (was ? -1 : 1))
  if (!was) {
    communityAPI.likeQuestion && communityAPI.likeQuestion(q.id).catch(() => {
      q.isLiked = was
      q.likeCount = Math.max(0, (q.likeCount || 0) - 1)
    })
  } else {
    communityAPI.unlikeQuestion && communityAPI.unlikeQuestion(q.id).catch(() => {
      q.isLiked = !was
      q.likeCount = (q.likeCount || 0) + 1
    })
  }
}
const toggleFav = (q) => {
  if (!q) return
  const was = !!q.isFav
  q.isFav = !was
  q.favCount = Math.max(0, (q.favCount || 0) + (was ? -1 : 1))
}

// ===== 提问 =====
const openCompose = () => {
  form.value = { title: '', content: '', tags: [] }
  tagInput.value = ''
  showCompose.value = true
}
const closeCompose = () => {
  if (submitting.value) return
  showCompose.value = false
}
const addTag = () => {
  const t = tagInput.value.trim()
  if (!t) return
  if (!form.value.tags.includes(t) && form.value.tags.length < 5) {
    form.value.tags.push(t)
  }
  tagInput.value = ''
}
const removeTag = (t) => {
  form.value.tags = form.value.tags.filter(x => x !== t)
}

const submit = async () => {
  const f = form.value
  if (!f.title || f.title.length < 6) {
    return uni.showToast({ title: '标题至少 6 个字', icon: 'none' })
  }
  if (!f.content || f.content.length < 15) {
    return uni.showToast({ title: '内容至少 15 个字', icon: 'none' })
  }
  submitting.value = true
  try {
    await communityAPI.createQuestion({
      title: f.title,
      content: f.content,
      tags: f.tags,
    })
    uni.showToast({ title: '发布成功', icon: 'success' })
    showCompose.value = false
    load(true)
  } catch (_) { /* request 已 toast */ }
  finally { submitting.value = false }
}

const goProfile = (uid) => {
  if (!uid) return
  uni.navigateTo({ url: `/pages/community/profile?id=${uid}` })
}

const goDetail = (qid) => {
  if (!qid) return
  uni.navigateTo({ url: `/pages/community/detail?id=${qid}` })
}

onPullDownRefresh(() => load(true))
onReachBottom(() => { if (hasMore.value) load(false) })
onLoad(() => load(true))
</script>

<template>
  <view class="qa-page">

    <view class="qa-head">
      <view class="qh-bg"></view>
      <PageHeader
        title="问答社区"
        subtitle="提一问，识一识，众人拾柴"
        :showWorkspace="false"
      />

      <!-- 频道条：非等分 tab（推荐比其他宽 1.618x） -->
      <view class="ch-row">
        <view
          v-for="(c, i) in CHANNELS"
          :key="c.key"
          class="ch-item"
          :class="{ active: channel === c.key, hot: c.hot }"
          :style="{ flex: c.weight }"
          @tap="switchChannel(c.key)"
        >
          <text class="ch-name">{{ c.name }}</text>
          <view v-if="c.hot && channel === c.key" class="ch-bar"></view>
          <view v-else-if="channel === c.key" class="ch-bar ch-bar-sm"></view>
        </view>
      </view>
    </view>

    <!-- 标签滚动条 -->
    <scroll-view
      class="tag-bar"
      scroll-x="true"
      :enhanced="true"
      :show-scrollbar="false"
    >
      <view
        v-for="(t, i) in TAGS"
        :key="t"
        class="tag-pill"
        :class="{ active: tag === t }"
        :style="i === 0 ? 'margin-left: 24rpx;' : ''"
        @tap="switchTag(t)"
      >
        {{ t }}
      </view>
    </scroll-view>

    <!-- 推荐频道：置顶问题横滑卡 -->
    <view v-if="channel === 'recommend' && pinnedQuestions.length" class="pin-row">
      <scroll-view scroll-x="true" :enhanced="true" :show-scrollbar="false">
        <view class="pin-list">
          <view
            v-for="p in pinnedQuestions"
              :key="p.id"
              class="pin-card"
              @tap="goDetail(p.id)"
            >
              <view class="pin-chip">{{ pinTag(p) }}</view>
              <text class="pin-title">{{ p.title }}</text>
              <view class="pin-foot">
                <view class="pin-heat">
                  <AppIcon name="fire" :size="12" color="#b8443a" />
                  <text>{{ p.likeCount || p.viewCount || 0 }}</text>
                </view>
                <text class="pin-cta">查看 ·</text>
              </view>
            </view>
        </view>
      </scroll-view>
    </view>

    <!-- 列表 -->
    <view class="q-list">
      <view
        v-for="(q, i) in list"
        :key="q.id || ('k' + i)"
        class="q-card"
        :class="{ top: i === 0 && (q.top || q.pinned) }"
        @tap="goDetail(q.id)"
      >
        <view class="qc-head">
          <view class="qc-author" @tap.stop="goProfile(q.authorId || q.userId)">
            <view class="qc-av" :style="{ background: avBg(q.authorName) }">
              <text>{{ avInit(q.authorName) }}</text>
            </view>
            <view class="qc-ameta">
              <text class="qc-aname">{{ q.authorName || '匿名学者' }}</text>
              <text class="qc-atime">{{ timeText(q.createTime || q.createdAt) }} · {{ q.viewCount || 0 }} 阅读</text>
            </view>
          </view>
          <view v-if="q.essence" class="qc-badge e">精华</view>
          <view v-else-if="q.top || q.pinned" class="qc-badge p">置顶</view>
        </view>

        <text class="qc-title">{{ q.title }}</text>
        <text class="qc-text">{{ contentText(q) }}</text>

        <view v-if="q.tags && q.tags.length" class="qc-tags">
          <text v-for="(t, ti) in q.tags.slice(0, 4)" :key="ti" class="qc-tag">#{{ t }}</text>
        </view>

        <view class="qc-foot">
          <view class="qc-stats">
            <view class="qc-s s-reply">
              <AppIcon name="message-square" :size="13" color="#6b6458" />
              <text>{{ q.answerCount || q.replyCount || 0 }}</text>
            </view>
            <view
              class="qc-s s-like"
              :class="{ on: q.isLiked }"
              @tap.stop="toggleLike(q)"
            >
              <AppIcon name="heart" :size="13" :color="q.isLiked ? '#b8443a' : '#6b6458'" />
              <text>{{ q.likeCount || 0 }}</text>
            </view>
            <view
              class="qc-s s-fav"
              :class="{ on: q.isFav }"
              @tap.stop="toggleFav(q)"
            >
              <AppIcon name="star" :size="13" :color="q.isFav ? '#b8723a' : '#6b6458'" />
              <text>{{ q.favCount || 0 }}</text>
            </view>
          </view>

          <view class="qc-last">
            <view v-if="q.lastAnswer" class="qc-la">
              <view class="la-av" :style="{ background: avBg(q.lastAnswer.userName) }">
                <text>{{ avInit(q.lastAnswer.userName) }}</text>
              </view>
              <view class="la-meta">
                <text class="la-name">{{ q.lastAnswer.userName }} 回答</text>
                <text class="la-content">{{ String(q.lastAnswer.content || '').slice(0, 28) }}…</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view v-if="!loading && list.length === 0" class="empty">
        <view class="empty-ico" :class="{ error: loadError }">
          <AppIcon :name="loadError ? 'circle-alert' : 'layers'" :size="40" :color="loadError ? '#b8443a' : '#bfb29a'" />
        </view>
        <text class="empty-t">{{ loadError ? '问答暂时无法加载' : '暂无相关问答' }}</text>
        <text class="empty-s">{{ loadError || '换个标签或频道看看～' }}</text>
        <button v-if="loadError" class="empty-retry" @tap="load(true)">重试</button>
      </view>

      <view v-else-if="loading" class="more">加载中…</view>
      <view v-else-if="!hasMore" class="more">— 到底了 —</view>
    </view>

    <!-- FAB 发布按钮 -->
    <view class="fab" @tap="openCompose">
      <AppIcon name="plus" :size="22" color="#fdfcfa" />
      <text>提问</text>
    </view>

    <!-- 提问弹层 -->
    <view v-if="showCompose" class="compose-mask" @tap="closeCompose"></view>
    <view class="compose" :class="{ show: showCompose }">
      <view class="cp-head">
        <text class="cp-close" @tap="closeCompose">取消</text>
        <text class="cp-title">发起新提问</text>
        <text class="cp-submit" :class="{ dis: submitting }" @tap="submit">
          {{ submitting ? '发布中…' : '发布' }}
        </text>
      </view>

      <view class="cp-form">
        <input
          v-model="form.title"
          class="cp-title-input"
          placeholder="一个好的问题，胜过十个好答案"
          :maxlength="60"
          placeholder-class="cp-ph"
        />
        <view class="cp-tl">{{ form.title.length }}/60</view>

        <textarea
          v-model="form.content"
          class="cp-content"
          placeholder="可以写背景、尝试过的方法、以及你期望的结果…"
          :maxlength="2000"
          placeholder-class="cp-ph"
          :auto-height="false"
          :adjust-position="true"
        />

        <view class="cp-tags">
          <view class="cp-tags-list">
            <view v-for="(t, i) in form.tags" :key="i" class="cp-tag">
              <text>#{{ t }}</text>
              <text class="cp-tag-x" @tap="removeTag(t)">
                <AppIcon name="x" :size="10" color="#6b6458" />
              </text>
            </view>
            <input
              v-if="form.tags.length < 5"
              v-model="tagInput"
              class="cp-tag-input"
              placeholder="+ 加标签（回车添加）"
              confirm-type="done"
              @confirm="addTag"
              @blur="addTag"
              placeholder-class="cp-ph"
            />
          </view>
          <text class="cp-tip">标签最多 5 个，方便他人找到你的问题</text>
        </view>
      </view>
    </view>

  </view>
</template>

<style lang="scss" scoped>
.qa-page {
  min-height: 100vh;
  background: $uni-bg-page;
  padding-bottom: 180rpx;
}

/* ===== Head ===== */
.qa-head { position: relative; }

.qh-bg {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 480rpx;
  background: linear-gradient(175deg, #f3ece0 0%, #fdfcfa 80%);
  z-index: 0;
}

.ch-row {
  position: relative;
  z-index: 2;
  display: flex;
  padding: $space-4 $space-5 0;
  gap: $space-2;
}

.ch-item {
  position: relative;
  padding: $space-2 0 $space-3;
  text-align: center;
}

.ch-name {
  position: relative;
  z-index: 1;
  font-size: $uni-font-size-base;
  font-weight: $weight-medium;
  color: $uni-text-regular;
  transition: all 0.2s ease;
}
.ch-item.active .ch-name {
  color: $uni-text-primary;
  font-weight: $weight-bold;
  font-size: $uni-font-size-md;
}

.ch-bar {
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 48rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: linear-gradient(90deg, $uni-color-accent, #d5a277);
  box-shadow: 0 2rpx 6rpx rgba(184, 114, 58, 0.3);
}
.ch-bar-sm {
  width: 28rpx;
  height: 6rpx;
  background: $uni-color-primary;
  box-shadow: none;
}

.ch-item.hot .ch-name {
  color: #b8443a;
}

/* ===== Tag bar ===== */
.tag-bar {
  position: relative;
  z-index: 2;
  padding: $space-3 0;
  white-space: nowrap;
  background: $uni-bg-page;
  border-top: $hairline solid $uni-border-light;
  border-bottom: $hairline solid $uni-border-light;
  margin-top: $space-3;
}

.tag-pill {
  display: inline-block;
  padding: 10rpx 24rpx;
  margin-right: $space-2;
  background: $uni-bg-card;
  border: $hairline solid $uni-border-light;
  border-radius: $uni-border-radius-full;
  font-size: $uni-font-size-xs;
  color: $uni-text-regular;
  vertical-align: middle;
}

.tag-pill.active {
  background: $uni-color-primary;
  color: #fdfcfa;
  border-color: $uni-color-primary;
  font-weight: $weight-semibold;
  box-shadow: 0 4rpx 14rpx rgba(43, 95, 75, 0.22);
}

/* ===== Pin row ===== */
.pin-row {
  padding: $space-4 0 0;
}
.pin-list {
  display: flex;
  gap: $space-3;
  padding: 0 $space-5;
  white-space: nowrap;
}
.pin-card {
  display: inline-flex;
  flex-direction: column;
  width: 540rpx;
  padding: $space-4;
  border-radius: $uni-border-radius-lg;
  background: linear-gradient(145deg, #fdf3e4, #efe2c5);
  margin-right: $space-3;
  position: relative;
  overflow: hidden;
}
.pin-card::after {
  content: '';
  position: absolute;
  right: -60rpx;
  top: -60rpx;
  width: 220rpx;
  height: 220rpx;
  background: radial-gradient(closest-side, rgba(184,68,58,0.16), transparent 70%);
}
.pin-chip {
  display: inline-flex;
  align-self: flex-start;
  padding: 4rpx 14rpx;
  border-radius: $uni-border-radius-xs;
  background: rgba(253, 252, 250, 0.7);
  color: #8d5728;
  font-size: $uni-font-size-caption;
  font-weight: $weight-semibold;
  z-index: 1;
}
.pin-title {
  display: block;
  margin-top: $space-3;
  font-size: $uni-font-size-md;
  font-weight: $weight-bold;
  line-height: 1.4;
  color: #3d3832;
  z-index: 1;
  white-space: normal;
  @include truncate(2);
}
.pin-foot {
  margin-top: $space-4;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 1;
}
.pin-heat {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  color: #b8443a;
  font-size: $uni-font-size-xs;
  font-weight: $weight-semibold;
}
.pin-cta { color: #8d5728; font-size: $uni-font-size-xs; }

/* ===== Q List ===== */
.q-list {
  padding: $space-4 $space-5 0;
  display: flex;
  flex-direction: column;
  gap: $space-4;
}

.q-card {
  background: $uni-bg-card;
  border-radius: $uni-border-radius-lg;
  padding: $space-4;
  border: $hairline solid $uni-border-light;
  box-shadow: $shadow-1;
  position: relative;
}
.q-card.top {
  background: linear-gradient(180deg, #fefaf2 0%, #fdfcfa 100%);
  border-color: rgba(184, 114, 58, 0.24);
  box-shadow: $shadow-2;
}

.qc-head {
  display: flex;
  align-items: center;
  gap: $space-3;
  margin-bottom: $space-3;
}

.qc-author {
  flex: 1;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: $space-3;
}

.qc-av {
  width: 64rpx;
  height: 64rpx;
  border-radius: $uni-border-radius-base;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fdfcfa;
  font-size: $uni-font-size-sm;
  font-weight: $weight-bold;
}

.qc-ameta { display: flex; flex-direction: column; min-width: 0; }
.qc-aname {
  font-size: $uni-font-size-sm;
  font-weight: $weight-semibold;
  color: $uni-text-primary;
  @include truncate;
}
.qc-atime {
  font-size: $uni-font-size-caption;
  color: $uni-text-secondary;
  margin-top: 2rpx;
}

.qc-badge {
  padding: 4rpx 14rpx;
  border-radius: $uni-border-radius-xs;
  font-size: $uni-font-size-caption;
  font-weight: $weight-semibold;
}
.qc-badge.e { background: $uni-color-accent-fog; color: $uni-color-accent-dark; }
.qc-badge.p { background: rgba(184, 68, 58, 0.12); color: #b8443a; }

.qc-title {
  display: block;
  font-size: $uni-font-size-lg;
  font-weight: $weight-bold;
  color: $uni-text-primary;
  line-height: 1.4;
  margin-bottom: $space-2;
}

.qc-text {
  display: block;
  font-size: $uni-font-size-sm;
  color: $uni-text-regular;
  line-height: 1.6;
  @include truncate(2);
}

.qc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
  margin-top: $space-3;
}
.qc-tag {
  font-size: $uni-font-size-caption;
  color: $uni-color-primary-dark;
  background: $uni-color-primary-fog;
  padding: 4rpx 14rpx;
  border-radius: $uni-border-radius-full;
}

.qc-foot {
  margin-top: $space-4;
  padding-top: $space-3;
  border-top: $hairline dashed $uni-border-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $space-3;
}

.qc-stats { display: inline-flex; align-items: center; gap: $space-3; }

.qc-s {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 14rpx;
  border-radius: $uni-border-radius-full;
  background: $uni-bg-page;
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
  transition: all 0.2s ease;
}

.qc-s.on { background: rgba(184, 68, 58, 0.1); color: #b8443a; }
.qc-s.s-fav.on { background: rgba(184, 114, 58, 0.12); color: $uni-color-accent-dark; }

.qc-last { flex: 1; min-width: 0; }

.qc-la {
  display: flex;
  align-items: center;
  gap: $space-2;
  min-width: 0;
}

.la-av {
  width: 40rpx;
  height: 40rpx;
  border-radius: $uni-border-radius-sm;
  color: #fdfcfa;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: $uni-font-size-caption;
  font-weight: $weight-bold;
  flex-shrink: 0;
}

.la-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.la-name { font-size: $uni-font-size-caption; color: $uni-text-secondary; }
.la-content {
  font-size: $uni-font-size-caption;
  color: $uni-text-regular;
  @include truncate;
}

/* ===== Empty / more ===== */
.empty {
  padding: $space-8 $space-5;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-2;
}
.empty-ico {
  width: 140rpx;
  height: 140rpx;
  border-radius: $uni-border-radius-lg;
  background: $uni-bg-card;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $space-2;
}
.empty-ico.error { background: #fff4f1; border: $hairline solid #f1c7bf; }
.empty-t { font-size: $uni-font-size-md; color: $uni-text-primary; font-weight: $weight-semibold; }
.empty-s { font-size: $uni-font-size-sm; color: $uni-text-secondary; }
.empty-retry {
  min-width: 150rpx;
  height: 68rpx;
  margin-top: 12rpx;
  padding: 0 24rpx;
  border: 2rpx solid #bfd5cf;
  border-radius: 18rpx;
  background: #ffffff;
  color: $uni-color-primary;
  font-size: $uni-font-size-sm;
  line-height: 64rpx;
}
.empty-retry::after { border: none; }

.more {
  padding: $space-6 0 $space-8;
  text-align: center;
  font-size: $uni-font-size-xs;
  color: $uni-text-placeholder;
}

/* ===== FAB ===== */
.fab {
  position: fixed;
  right: $space-5;
  bottom: 200rpx;
  z-index: 40;
  padding: 22rpx 30rpx;
  background: linear-gradient(135deg, $uni-color-primary-dark, $uni-color-primary);
  color: #fdfcfa;
  border-radius: 999rpx;
  display: inline-flex;
  align-items: center;
  gap: $space-2;
  font-size: $uni-font-size-base;
  font-weight: $weight-bold;
  box-shadow: $shadow-2, 0 8rpx 20rpx rgba(43, 95, 75, 0.3);
}

/* ===== Compose ===== */
.compose-mask {
  position: fixed;
  inset: 0;
  background: rgba(26, 25, 22, 0.46);
  z-index: 60;
}

.compose {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 61;
  background: $uni-bg-card;
  border-top-left-radius: $uni-border-radius-lg;
  border-top-right-radius: $uni-border-radius-lg;
  transform: translateY(100%);
  transition: transform 0.28s ease;
  padding-bottom: env(safe-area-inset-bottom);
  max-height: 88vh;
  display: flex;
  flex-direction: column;
}
.compose.show { transform: translateY(0); }

.cp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-4 $space-5;
  border-bottom: $hairline solid $uni-border-light;
}
.cp-close {
  font-size: $uni-font-size-sm;
  color: $uni-text-secondary;
}
.cp-title {
  font-size: $uni-font-size-md;
  font-weight: $weight-bold;
  color: $uni-text-primary;
}
.cp-submit {
  font-size: $uni-font-size-sm;
  color: $uni-color-primary-dark;
  font-weight: $weight-bold;
  padding: 8rpx 22rpx;
  background: $uni-color-primary-fog;
  border-radius: $uni-border-radius-full;
}
.cp-submit.dis { opacity: 0.5; }

.cp-form {
  padding: $space-4 $space-5 $space-6;
  overflow-y: auto;
}

.cp-title-input {
  height: 80rpx;
  font-size: $uni-font-size-lg;
  font-weight: $weight-bold;
  color: $uni-text-primary;
}

.cp-tl {
  text-align: right;
  font-size: $uni-font-size-caption;
  color: $uni-text-placeholder;
  margin-bottom: $space-3;
}

.cp-content {
  width: 100%;
  min-height: 280rpx;
  background: $uni-bg-page;
  border-radius: $uni-border-radius-base;
  padding: $space-3;
  font-size: $uni-font-size-base;
  line-height: 1.6;
  color: $uni-text-primary;
  box-sizing: border-box;
}

.cp-ph { color: $uni-text-placeholder; }

.cp-tags {
  margin-top: $space-4;
}
.cp-tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
}

.cp-tag {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  border-radius: $uni-border-radius-full;
  background: $uni-color-primary-fog;
  color: $uni-color-primary-dark;
  font-size: $uni-font-size-xs;
  font-weight: $weight-medium;
}

.cp-tag-x {
  width: 24rpx;
  height: 24rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.cp-tag-input {
  flex: 1;
  min-width: 200rpx;
  height: 56rpx;
  padding: 0 16rpx;
  font-size: $uni-font-size-xs;
  color: $uni-text-regular;
}

.cp-tip {
  display: block;
  margin-top: $space-2;
  font-size: $uni-font-size-caption;
  color: $uni-text-placeholder;
}
</style>
