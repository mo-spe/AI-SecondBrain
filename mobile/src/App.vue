<script>
import { useUserStore } from '@/stores/user'

export default {
  onLaunch() {
    // 启动时依据本地登录态决定落地页：已登录直达复习首页，未登录进登录页
    const userStore = useUserStore()
    if (userStore.isLoggedIn()) {
      uni.switchTab({ url: '/pages/review/index' })
    }
  },
}
</script>

<style lang="scss">
/* ============================================================
   AI-SecondBrain Mobile — 墨色知识编辑器体系
   青绿表达知识边界，暖铜只承担重点，页面主体保持清洁和高对比
   ============================================================ */
page {
  /* 品牌色与语义色沿用 variables.css 的色板 */
  --color-primary: #174a45;
  --color-primary-light: #2f756c;
  --color-primary-dark: #0f3430;
  --color-accent: #c9793f;
  --color-accent-light: #e2a06a;
  --color-success: #2f7560;
  --color-warning: #b96c32;
  --color-danger: #b8443a;

  /* 纸感背景与暖灰文字 */
  --bg-page: #f3f6f5;
  --bg-card: #ffffff;
  --bg-input: #edf2f0;
  --bg-hover: #e6edeb;
  --text-primary: #162320;
  --text-regular: #35433f;
  --text-secondary: #66736f;
  --text-placeholder: #929d99;
  --border-base: #ced9d5;
  --border-light: #e4ebe8;

  --radius-sm: 8rpx;
  --radius-md: 16rpx;
  --radius-lg: 24rpx;

  color: var(--text-regular);
  background-color: var(--bg-page);
  font-family: 'PingFang SC', 'Noto Sans SC', system-ui,
    -apple-system, sans-serif;
  font-size: 28rpx;
  line-height: 1.6;
}

/* 通用卡片容器 */
.card {
  background: var(--bg-card);
  border: 1rpx solid var(--border-light);
  border-radius: var(--radius-md);
  box-sizing: border-box;
}

/* 通用主按钮 */
.btn-primary {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  border-radius: 20rpx;
  background: var(--color-primary);
  color: #fdfcfa;
  font-size: 30rpx;
  font-weight: 500;
  letter-spacing: 1rpx;
}

button {
  box-sizing: border-box;
}

/* 触控目标不小于 88rpx（约 44px），对齐移动端可点性规范 */
.tap-target {
  min-height: 88rpx;
  min-width: 88rpx;
}

/* 空状态占位 */
.empty-state {
  padding: 120rpx 40rpx;
  text-align: center;
  color: var(--text-placeholder);
  font-size: 26rpx;
}

/* 文本省略 */
.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.ellipsis-2 {
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
</style>
