<template>
  <!--
    PageHeader — 所有 Tab 页的顶部统一条。
    设计目的：
    1) 把「工作区切换入口」从「我的」里解放出来，进入每个主 Tab 顶栏，与网页版一致。
    2) 左侧大标题 + 副标题形成 Display/Caption 节奏，右侧操作带轻态图标，打破 AI 样板的左右对称。
    3) 提供可选的右主 action（首页传搜索图标 + onClick 跳搜索等）。
  -->
  <view class="page-header">
    <view class="ph-left">
      <view class="ph-eyebrow">
        <view class="ph-brand-mark"></view>
        <text>SECOND BRAIN</text>
      </view>
      <view class="ph-title-row">
        <text class="ph-title">{{ title }}</text>
        <view
          class="ph-workspace tap-target"
          v-if="showWorkspace"
          role="button"
          :aria-label="`当前工作区：${workspaceStore.currentName}，点击切换`"
          hover-class="ph-workspace-pressed"
          @tap.stop="$emit('openWorkspace')"
        >
          <view class="ph-ws-dot" :class="workspaceStore.currentId ? 'active' : 'idle'"></view>
          <text class="ph-ws-name">{{ workspaceStore.currentName }}</text>
          <AppIcon name="chevron-down" :size="13" color="#66736f" />
        </view>
      </view>
      <text v-if="subtitle" class="ph-subtitle">{{ subtitle }}</text>
    </view>

    <view class="ph-actions">
      <slot name="trailing">
        <view
          v-for="a in actions"
          :key="a.key"
          class="ph-action"
          :class="a.type || 'ghost'"
          role="button"
          :aria-label="a.label || a.key"
          hover-class="ph-action-pressed"
          @tap="$emit('action', a.key)"
        >
          <AppIcon :name="a.icon" :size="a.size || 18" :color="a.color || '#174a45'" />
        </view>
      </slot>
    </view>
  </view>
</template>

<script>
import { useWorkspaceStore } from '@/stores/workspace'

export default {
  name: 'PageHeader',
  props: {
    title: { type: String, default: '' },
    subtitle: { type: String, default: '' },
    showWorkspace: { type: Boolean, default: true },
    actions: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['openWorkspace', 'action'],
  setup() {
    return { workspaceStore: useWorkspaceStore() }
  },
}
</script>

<style lang="scss" scoped>
.page-header {
  position: relative;
  z-index: 10;
  padding: calc(var(--status-bar-height) + 24rpx) 32rpx 26rpx;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: $hairline solid #e4ebe8;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: $space-4;
}

.ph-left {
  flex: 1;
  min-width: 0;
}

.ph-title-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  flex-wrap: wrap;
}

.ph-eyebrow {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 8rpx;
  color: #6d7b77;
  font-size: 19rpx;
  font-weight: $weight-semibold;
  letter-spacing: 0.16em;
}

.ph-brand-mark {
  width: 24rpx;
  height: 4rpx;
  border-radius: 2rpx;
  background: $uni-color-accent;
}

.ph-title {
  font-size: 46rpx;
  font-weight: $weight-bold;
  color: #14211e;
  letter-spacing: -0.035em;
  line-height: 1.08;
}

/* 工作区徽标 —— 像一个小药丸，带圆点状态 */
.ph-workspace {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  min-height: 56rpx;
  padding: 0 16rpx;
  background: #edf4f1;
  border: 1rpx solid #d5e4df;
  border-radius: 18rpx;
  max-width: 300rpx;
  box-sizing: border-box;
  transition: opacity 0.18s ease, background 0.18s ease;
}

.ph-workspace-pressed {
  opacity: 0.7;
  background: #e1eeea;
}

.ph-ws-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #aebbb7;
}

.ph-ws-dot.active {
  background: $uni-color-primary-soft;
  box-shadow: 0 0 0 4rpx rgba(23, 74, 69, 0.12);
}

.ph-ws-name {
  font-size: 23rpx;
  font-weight: $weight-medium;
  color: #35433f;
  @include truncate;
}

.ph-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 25rpx;
  color: #66736f;
  line-height: 1.5;
}

.ph-actions {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding-bottom: 2rpx;
}

.ph-action {
  width: 80rpx;
  height: 80rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 22rpx;
  border: $hairline solid #dde6e3;
  background: #f7f9f8;
  transition: opacity 0.18s ease, background 0.18s ease;
}

.ph-action.primary {
  background: $uni-color-primary;
  border-color: $uni-color-primary;
}

.ph-action-pressed {
  opacity: 0.68;
  background: #e8efed;
}

@media (prefers-reduced-motion: reduce) {
  .ph-action,
  .ph-workspace { transition: none; }
}
</style>
