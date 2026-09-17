<script>
/**
 * AppIcon — 统一全局图标组件
 *
 * 设计原则（避免经验 #856026 的图标混乱）：
 * 1) 单家族：所有 path 均来自同一份 24×24 viewBox 线性风格（圆角端点/圆角拐点、stroke-width 1.8）
 * 2) 单一组件入口：页面禁止手写 SVG path 或 emoji，必须经此组件
 * 3) 语义名白名单：增加图标必须在这里登记并配 path，防止页面各处随意新造
 *
 * 用法：<AppIcon name="book-open" :size="20" color="#2b5f4b" />
 */

/* 图标白名单（路径统一 24×24 视框、stroke-width 1.8、round 端点/拐点，fill none，currentColor） */
const PATHS = {
  /* ===== 知识 / 学习 ===== */
  'book-open':
    'M2 4h7a4 4 0 0 1 4 4v13M22 4h-7a4 4 0 0 0-4 4v13M2 8v12h7a3 3 0 0 1 3 3M22 8v12h-7a3 3 0 0 0-3 3',
  book:
    'M4 4.5A2.5 2.5 0 0 1 6.5 2H20v18H6.5A2.5 2.5 0 0 0 4 22.5V4.5zM4 19.5A2.5 2.5 0 0 1 6.5 17H20',
  layers:
    'M12 2 2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5',
  'sparkles':
    'M12 3v4M12 17v4M3 12h4M17 12h4M5.6 5.6l2.8 2.8M15.6 15.6l2.8 2.8M5.6 18.4l2.8-2.8M15.6 8.4l2.8-2.8',

  /* ===== 广场 / 内容 ===== */
  newspaper:
    'M4 5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5zM8 7h8M8 11h8M8 15h4M14 15h2M14 11h2',
  users:
    'M9 11a4 4 0 1 0-4-4 4 4 0 0 0 4 4zM3 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2M16 3.1A4 4 0 0 1 16 11M21 21v-2a4 4 0 0 0-3-3.9',
  message:
    'M21 12a8 8 0 0 1-11.6 7.1L4 20l1-5.3A8 8 0 1 1 21 12z',
  comment:
    'M12 2a10 10 0 1 0 3.7 19.3L22 22l-1-5.8A10 10 0 0 0 12 2zM8 10h8M8 14h5',
  'message-square':
    'M20 15a3 3 0 0 1-3 3H9l-5 3v-3a3 3 0 0 1-1-2.2V7a3 3 0 0 1 3-3h11a3 3 0 0 1 3 3z',
  heart:
    'M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 1 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z',
  star:
    'm12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9z',
  'heart-stroke':
    'M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 1 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z',
  'heart-fill': null, /* 语义别名，由组件样式处理 */
  bookmark:
    'M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z',

  /* ===== 社区 / 问答 ===== */
  'circle-help':
    'M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zM9.1 9a3 3 0 0 1 5.8 1c0 2-3 2.7-3 4M12 17h.01',
  'circle-check':
    'M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zM8 12l3 3 5-6',
  'circle-alert':
    'M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zM12 8v5M12 16h.01',
  badge:
    'M9 12h6M12 9v6M20.5 8.6A9 9 0 1 1 15.4 3.5L12 6 9 3l-2 2-3-1 1 3-3 2 3 1-2 2 1 3-1 3 3-1 2 3 3 1 1-3 3-2-1 3-2 2 3 1 2-3 3-3a9 9 0 0 1-2.5-10.4z',

  /* ===== 导航 / 动作 ===== */
  search: 'M11 19a8 8 0 1 1 8-8 8 8 0 0 1-8 8zM21 21l-4.3-4.3',
  plus:  'M12 5v14M5 12h14',
  minus: 'M5 12h14',
  close: 'M18 6 6 18M6 6l12 12',
  check: 'M5 12l5 5L20 7',
  send:  'M22 2 11 13M22 2l-7 20-4-9-9-4z',
  share:
    'M4 12v7a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7M16 6l-4-4-4 4M12 2v13',
  download:
    'M12 3v12m0 0-4-4m4 4 4-4M4 21h16',
  upload:
    'M12 21V9m0 0-4 4m4-4 4 4M4 3h16',
  filter:
    'M3 5h18M6 12h12M10 19h4',
  sort:
    'M7 4v16m0 0-3-3m3 3 3-3M17 20V4m0 0-3 3m3-3 3 3',

  /* ===== 箭头 / 指示 ===== */
  'chevron-right': 'M9 6l6 6-6 6',
  'chevron-left':  'M15 6 9 12l6 6',
  'chevron-down':  'M6 9l6 6 6-6',
  'chevron-up':    'M6 15l6-6 6 6',
  'arrow-right':   'M5 12h14M13 6l6 6-6 6',
  'arrow-left':    'M19 12H5M11 6l-6 6 6 6',
  'arrow-up-right':'M7 17 17 7M8 7h9v9',
  'caret-right':   'M10 6l8 6-8 6z', /* 实心箭头 */

  /* ===== 用户 / 身份 ===== */
  user:    'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0-4-4 4 4 0 0 0 4 4z',
  'user-plus':
    'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M8 11a4 4 0 1 0-4-4 4 4 0 0 0 4 4zM19 8v6M22 11h-6',
  'user-minus':
    'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M8 11a4 4 0 1 0-4-4 4 4 0 0 0 4 4zM17 11h5',
  logout:  'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
  settings:
    'M12 8a4 4 0 1 0 4 4 4 4 0 0 0-4-4zM19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 0 1 0 2.8 2.1 2.1 0 0 1-2.8 0l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1A1.7 1.7 0 0 0 9 19.4a1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 0 1-2.8 0 2.1 2.1 0 0 1 0-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 0 1 0-2.8 2.1 2.1 0 0 1 2.8 0l.1.1a1.7 1.7 0 0 0 1.9.3h0a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5h0a1.7 1.7 0 0 0 1.9-.3l.1-.1a2.1 2.1 0 0 1 2.8 0 2 2 0 0 1 0 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9v0a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z',
  bell:
    'M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0',

  /* ===== 成就 / 排行 ===== */
  trophy:
    'M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0zM17 4h3v1a3 3 0 0 1-3 3zM7 4H4v1a3 3 0 0 0 3 3',
  podium:
    'M4 17h5v4H4zM15 13h5v8h-5zM8 9h8v12H8z',
  fire:
    'M12 2s4 4 4 8a4 4 0 1 1-8 0c0-2 1-3 1-3s-2 2-2 5a5 5 0 0 0 10 0c0-6-5-10-5-10z',
  'cal-grid':
    'M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zM8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01M16 18h.01',

  /* ===== AI ===== */
  bot:
    'M12 3a3 3 0 0 0-3 3v1H5a3 3 0 0 0-3 3v9a3 3 0 0 0 3 3h14a3 3 0 0 0 3-3v-9a3 3 0 0 0-3-3h-4V6a3 3 0 0 0-3-3zM9 13h.01M15 13h.01M9 17h6',
  lab:
    'M10 3h4M7 7V3h10v4M4 21h16M7 7 3 19a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1L17 7M9 7v4M15 7v4',

  /* ===== 签到 / 工作区 ===== */
  fold:
    'M3 7l6-3h6l6 3v10l-6 3H9l-6-3zM7 7l5 3 5-3M12 10v11',
  'switch-horizontal':
    'M7 8l-4 4 4 4M3 12h13m4-8 4 4-4 4m4-4H8',
  home:
    'M3 10.5 12 3l9 7.5V21a1 1 0 0 1-1 1h-5v-7h-6v7H4a1 1 0 0 1-1-1z',
  'squares-layered':
    'M5 3h9v9H5zM10 8h9v9h-9zM3 15h8v6H3z',

  /* ===== 杂项 ===== */
  pin:
    'M12 22s-7-7.58-7-13a7 7 0 1 1 14 0C19 14.42 12 22 12 22zM12 12a2 2 0 1 0-2-2 2 2 0 0 0 2 2z',
  image:
    'M3 5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM9 12a2 2 0 1 0-2-2 2 2 0 0 0 2 2zM21 19l-5-5-4 4-3-3-4 4',
  link:
    'M10 13a5 5 0 0 0 7.1 0l3-3a5 5 0 0 0-7.1-7.1L11.6 5M14 11a5 5 0 0 0-7.1 0l-3 3a5 5 0 0 0 7.1 7.1L12.4 19',
  clock:
    'M12 22a10 10 0 1 1 10-10 10 10 0 0 1-10 10zM12 6v6l4 2',
  'chart-bar':
    'M3 3v18h18M7 17v-6M12 17V7M17 17v-10',
  dots:
    'M6 12h.01M12 12h.01M18 12h.01',
  'eye-open':
    'M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12zM12 15a3 3 0 1 0-3-3 3 3 0 0 0 3 3z',
  trash:
    'M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6M10 11v6M14 11v6',
  edit:
    'M12 20h9M16.5 3.5a2.1 2.1 0 1 1 3 3L7 19l-4 1 1-4z',
  edit2:
    'M17 3 21 7l-11 11H6v-4zM16 5l3 3',
  'refresh-cw':
    'M21 12a9 9 0 0 1-15.5 6.3L3 16M3 12a9 9 0 0 1 15.5-6.3L21 8M21 3v5h-5M3 21v-5h5',
}

/** 语义别名映射（避免重复 path） */
const ALIAS = {
  'heart-fill': 'heart-stroke',
  x: 'close',
}

export default {
  name: 'AppIcon',
  props: {
    name: { type: String, required: true },
    size: { type: [Number, String], default: 18 },
    color: { type: String, default: 'currentColor' },
    /* 特殊：heart-fill 等实心态走 filled 视觉 */
    filled: { type: Boolean, default: false },
  },
  computed: {
    path() {
      const key = ALIAS[this.name] || this.name
      return PATHS[key] || ''
    },
    /* 真实尺寸：组件接受数字（会按 rpx=2*CSSpx 缩放）或字符串 */
    boxSize() {
      const s = Number(this.size)
      if (!Number.isNaN(s)) return s * 2 + 'rpx'
      return this.size
    },
    isFilled() {
      if (this.filled) return true
      return this.name.endsWith('-fill')
    },
    fillRule() {
      return this.isFilled ? this.color : 'none'
    },
    strokeRule() {
      return this.color
    },
  },
}
</script>

<template>
  <svg
    class="app-icon"
    :style="{ width: boxSize, height: boxSize, color }"
    viewBox="0 0 24 24"
    :fill="fillRule"
    fill-rule="evenodd"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <g
      :stroke="strokeRule"
      :stroke-width="1.8"
      :fill="fillRule"
      fill-rule="evenodd"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <template v-if="path">
        <path
          v-for="(line, i) in path.split(/(?=M)/g)"
          :key="i"
          :d="line"
        />
      </template>
      <!-- 未知名兜底：空渲染，控制台不会报错 -->
      <template v-else>
        <circle cx="12" cy="12" r="2" :fill="color" />
      </template>
    </g>
  </svg>
</template>

<style lang="scss" scoped>
.app-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  flex-shrink: 0;
}
</style>
