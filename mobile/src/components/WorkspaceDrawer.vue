<template>
  <!--
    WorkspaceDrawer — 从底部弹出的工作区切换抽屉
    模仿网页版 WorkspaceSwitcher 语义，但移动端改为底部弹出（更符合单手操作）：
    - 「个人空间」始终在顶部
    - 工作区列表带圆点、角色徽标、当前选中态
    - 底部提供「新建工作区」占位入口（未授权场景可隐藏）
  -->
  <view v-if="visible" class="ws-mask" @tap="close">
    <view class="ws-sheet" @tap.stop>
      <view class="ws-handle"></view>

      <view class="ws-head">
        <view class="ws-kicker">
          <view class="ws-kicker-line"></view>
          <text>知识边界</text>
        </view>
        <text class="ws-title">选择你的工作空间</text>
        <text class="ws-subtitle">笔记、复习记录和 AI 上下文会随空间切换</text>
        <view class="ws-close tap-target" hover-class="tap-muted" @tap="close">
          <AppIcon name="close" :size="18" color="#54615e" />
        </view>
      </view>

      <scroll-view class="ws-list" scroll-y="true" :enhanced="true" :show-scrollbar="false">
        <!-- 个人空间 -->
        <view
          class="ws-item"
          :class="{
            current: workspaceStore.isCurrent(null),
            switching: workspaceStore.switchingId === 'personal',
            disabled: workspaceStore.switching,
          }"
          hover-class="ws-item-pressed"
          @tap="selectPersonal"
        >
          <view class="ws-icon home">
            <AppIcon name="home" :size="18" color="#174a45" />
          </view>
          <view class="ws-meta">
            <text class="ws-name">个人空间</text>
            <text class="ws-desc">仅你可见的私密知识</text>
          </view>
          <view v-if="workspaceStore.switchingId === 'personal'" class="ws-progress"></view>
          <view v-else-if="workspaceStore.isCurrent(null)" class="ws-current-mark">
            <text>当前</text>
          </view>
          <AppIcon v-else name="chevron-right" :size="16" color="#9aa5a2" />
        </view>

        <view class="ws-section-label">
          <text>协作空间</text>
          <text class="ws-count">{{ workspaceStore.workspaces.length }}</text>
        </view>

        <!-- 加载中 -->
        <view v-if="workspaceStore.loading && workspaceStore.workspaces.length === 0" class="ws-empty">
          <view class="ws-progress large"></view>
          <text class="ws-empty-text">正在同步工作区</text>
        </view>

        <!-- 空状态 -->
        <view
          v-else-if="!workspaceStore.loading && workspaceStore.workspaces.length === 0"
          class="ws-empty"
        >
          <template v-if="workspaceStore.loadError">
            <view class="ws-empty-illu error">
              <AppIcon name="circle-alert" :size="28" color="#b8443a" />
            </view>
            <text class="ws-empty-text">工作区暂时无法加载</text>
            <text class="ws-empty-hint">{{ workspaceStore.loadError }}</text>
            <button class="ws-retry" hover-class="ws-create-pressed" @tap.stop="workspaceStore.fetchWorkspaces">重试</button>
          </template>
          <template v-else>
            <view class="ws-empty-illu">
              <AppIcon name="squares-layered" :size="28" color="#bfb29a" />
            </view>
            <text class="ws-empty-text">还没有协作工作区</text>
            <text class="ws-empty-hint">创建或被邀请后会出现在这里</text>
          </template>
        </view>

        <!-- 工作区列表 -->
        <view
          v-for="ws in workspaceStore.workspaces"
          :key="ws.id"
          class="ws-item"
          :class="{
            current: workspaceStore.isCurrent(ws.id),
            switching: workspaceStore.switchingId === String(ws.id),
            disabled: workspaceStore.switching,
          }"
          hover-class="ws-item-pressed"
          @tap="select(ws)"
        >
          <view class="ws-icon team" :style="avatarStyle(ws)">
            <text class="ws-icon-text">{{ initial(ws.name) }}</text>
          </view>
          <view class="ws-meta">
            <view class="ws-name-row">
              <text class="ws-name">{{ ws.name }}</text>
              <view v-if="ws.role" class="ws-role" :class="roleClass(ws.role)">
                {{ roleText(ws.role) }}
              </view>
            </view>
            <text class="ws-desc">{{ ws.description || '未填写描述' }}</text>
          </view>
          <view v-if="workspaceStore.switchingId === String(ws.id)" class="ws-progress"></view>
          <view v-else-if="workspaceStore.isCurrent(ws.id)" class="ws-current-mark">
            <text>当前</text>
          </view>
          <AppIcon v-else name="chevron-right" :size="16" color="#9aa5a2" />
        </view>
      </scroll-view>

      <view class="ws-foot">
        <button class="ws-create" hover-class="ws-create-pressed" @tap.stop="$emit('create')" :disabled="creating">
          <AppIcon name="plus" :size="17" color="#174a45" />
          <text>创建或管理工作区</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { useWorkspaceStore } from '@/stores/workspace'
import { workspaceAPI } from '@/api/workspace'

export default {
  name: 'WorkspaceDrawer',
  props: {
    visible: { type: Boolean, default: false },
  },
  emits: ['close', 'update:visible', 'changed', 'create'],
  setup() {
    return { workspaceStore: useWorkspaceStore() }
  },
  data() {
    return { creating: false }
  },
  watch: {
    visible(next) {
      if (next) this.workspaceStore.fetchWorkspaces()
    },
  },
  methods: {
    close() {
      this.$emit('update:visible', false)
      this.$emit('close')
    },

    initial(name) {
      if (!name) return 'W'
      const t = String(name).trim()
      return t.charAt(0).toUpperCase()
    },

    /* 色板 —— 稳定的暖色配色池，按 id 取模，不随机 */
    avatarStyle(ws) {
      const pool = [
        { bg: '#174a45', fg: '#ffffff' },
        { bg: '#c9793f', fg: '#ffffff' },
        { bg: '#5c4a8d', fg: '#fdfcfa' },
        { bg: '#3e6b8b', fg: '#fdfcfa' },
        { bg: '#a05463', fg: '#fdfcfa' },
      ]
      const i = (Number(ws.id) || 0) % pool.length
      return { background: pool[i].bg, color: pool[i].fg }
    },

    roleText(r) {
      return (
        { Owner: '所有者', Admin: '管理员', Editor: '编辑者', Viewer: '查看者' }[r] || '成员'
      )
    },

    roleClass(r) {
      return (
        {
          Owner: 'role-owner',
          Admin: 'role-admin',
          Editor: 'role-editor',
          Viewer: 'role-viewer',
        }[r] || 'role-viewer'
      )
    },

    async selectPersonal() {
      if (this.workspaceStore.isCurrent(null) || this.workspaceStore.switching) {
        this.close()
        return
      }
      try {
        await this.workspaceStore.switchToPersonal()
        uni.showToast({ title: '已切换到个人空间', icon: 'success' })
        this.$emit('changed')
        this.close()
      } catch (e) {
        uni.showToast({ title: e.message || '切换失败', icon: 'none' })
      }
    },

    async select(ws) {
      if (this.workspaceStore.isCurrent(ws.id) || this.workspaceStore.switching) {
        this.close()
        return
      }
      try {
        await this.workspaceStore.switchWorkspace(ws.id)
        uni.showToast({ title: `已切换到「${ws.name}」`, icon: 'success' })
        this.$emit('changed')
        this.close()
      } catch (e) {
        uni.showToast({ title: e.message || '切换失败', icon: 'none' })
      }
    },
  },
}
</script>

<style lang="scss" scoped>
.ws-mask {
  position: fixed;
  inset: 0;
  background: rgba(12, 24, 22, 0.54);
  z-index: 100;
  display: flex;
  align-items: flex-end;
}

.ws-sheet {
  width: 100%;
  max-height: 84vh;
  background: #f8faf9;
  border-top-left-radius: 44rpx;
  border-top-right-radius: 44rpx;
  box-shadow: 0 -24rpx 64rpx rgba(11, 34, 30, 0.18);
  display: flex;
  flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom);
}

.ws-handle {
  width: 72rpx;
  height: 8rpx;
  border-radius: 4rpx;
  background: #c8d1ce;
  margin: 16rpx auto 0;
}

.ws-head {
  position: relative;
  padding: 28rpx 36rpx 26rpx;
}

.ws-kicker {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 12rpx;
  color: $uni-color-primary-soft;
  font-size: 21rpx;
  font-weight: $weight-semibold;
  letter-spacing: 0.12em;
}

.ws-kicker-line {
  width: 28rpx;
  height: 3rpx;
  border-radius: 2rpx;
  background: $uni-color-accent;
}

.ws-title {
  display: block;
  font-size: 40rpx;
  font-weight: $weight-bold;
  color: #162320;
  line-height: 1.18;
  letter-spacing: -0.02em;
}

.ws-subtitle {
  display: block;
  margin-top: 12rpx;
  padding-right: 68rpx;
  font-size: 25rpx;
  color: $uni-text-secondary;
}

.ws-close {
  position: absolute;
  right: 28rpx;
  top: 24rpx;
  width: 88rpx;
  height: 88rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #edf1ef;
}

.ws-list {
  width: 100%;
  height: 720rpx;
  max-height: 54vh;
  padding: 0 28rpx;
  box-sizing: border-box;
}

.ws-section-label {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 28rpx 10rpx 14rpx;
  font-size: 22rpx;
  font-weight: $weight-semibold;
  color: $uni-text-secondary;
  letter-spacing: 0.08em;
}

.ws-count {
  min-width: 34rpx;
  height: 34rpx;
  padding: 0 8rpx;
  border-radius: 17rpx;
  background: #e7eeeb;
  color: $uni-color-primary;
  line-height: 34rpx;
  text-align: center;
  box-sizing: border-box;
}

.ws-item {
  display: flex;
  align-items: center;
  gap: 22rpx;
  min-height: 112rpx;
  padding: 16rpx 18rpx;
  border-radius: 24rpx;
  background: transparent;
  border: 2rpx solid transparent;
  margin-bottom: 8rpx;
  box-sizing: border-box;
  transition: background 0.18s ease, border-color 0.18s ease;
}

.ws-item.current {
  border-color: #c8dcd6;
  background: #eaf3f0;
}

.ws-item-pressed {
  background: #edf1ef;
}

.ws-item.disabled {
  opacity: 0.55;
}

.ws-item.switching {
  opacity: 1;
}

.ws-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ws-icon.home {
  background: #dcebe6;
}

.ws-icon-text {
  font-size: $uni-font-size-md;
  font-weight: $weight-bold;
}

.ws-meta {
  flex: 1;
  min-width: 0;
}

.ws-name-row {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.ws-name {
  font-size: $uni-font-size-md;
  font-weight: $weight-semibold;
  color: $uni-text-primary;
  @include truncate;
}

.ws-role {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: $uni-border-radius-xs;
  flex-shrink: 0;
}

.role-owner  { background: $uni-color-primary-fog;   color: $uni-color-primary-dark; }
.role-admin  { background: $uni-color-accent-fog;    color: $uni-color-accent-dark; }
.role-editor { background: rgba(62,107,139, 0.12);   color: #2f5d7a; }
.role-viewer { background: $uni-bg-badge;            color: $uni-text-secondary; }

.ws-desc {
  display: block;
  margin-top: 6rpx;
  font-size: $uni-font-size-xs;
  color: $uni-text-secondary;
  @include truncate;
}

.ws-current-mark {
  min-width: 70rpx;
  height: 42rpx;
  padding: 0 12rpx;
  border-radius: 21rpx;
  background: #d5e8e2;
  color: $uni-color-primary-dark;
  font-size: 21rpx;
  font-weight: $weight-semibold;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.ws-progress {
  width: 30rpx;
  height: 30rpx;
  border: 4rpx solid #c7d8d3;
  border-top-color: $uni-color-primary;
  border-radius: 50%;
  animation: ws-spin 0.75s linear infinite;
}

.ws-progress.large {
  width: 40rpx;
  height: 40rpx;
  margin: 0 auto;
}

.ws-empty {
  padding: $space-7 0;
  text-align: center;
}

.ws-empty-illu {
  display: inline-flex;
  width: 120rpx;
  height: 120rpx;
  border-radius: $uni-border-radius-lg;
  background: $uni-bg-card;
  align-items: center;
  justify-content: center;
  border: $hairline dashed $uni-border-base;
}

.ws-empty-illu.error {
  background: #fff4f1;
  border-color: #f1c7bf;
}

.ws-empty-text {
  display: block;
  margin-top: $space-4;
  font-size: $uni-font-size-sm;
  color: $uni-text-regular;
}

.ws-empty-hint {
  display: block;
  margin-top: $space-2;
  font-size: $uni-font-size-xs;
  color: $uni-text-placeholder;
}

.ws-retry {
  margin: 22rpx auto 0;
  min-width: 168rpx;
  height: 68rpx;
  padding: 0 28rpx;
  border: 2rpx solid #bfd5cf;
  border-radius: 18rpx;
  background: #ffffff;
  color: $uni-color-primary;
  font-size: $uni-font-size-sm;
  line-height: 64rpx;
}

.ws-retry::after { border: none; }

.ws-foot {
  padding: 18rpx 36rpx 28rpx;
  border-top: $hairline solid #e4ebe8;
  background: #f8faf9;
}

.ws-create {
  width: 100%;
  height: 88rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  border-radius: 22rpx;
  background: transparent;
  color: $uni-color-primary;
  font-size: $uni-font-size-base;
  font-weight: $weight-semibold;
  border: 2rpx solid #bfd5cf;
  box-shadow: none;

  &::after { border: none; }
}

.ws-create-pressed,
.tap-muted {
  opacity: 0.72;
}

@keyframes ws-spin {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .ws-item,
  .ws-progress { transition: none; animation-duration: 1.5s; }
}
</style>
