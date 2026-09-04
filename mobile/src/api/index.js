/**
 * API 统一入口：按模块聚合各子模块 API，页面可直接
 * `import { reviewAPI, userAPI } from '@/api'` 使用。
 *
 * 这样做的原因：
 * 1. 避免页面里散着 `@/api/review`、`@/api/user` 等多个路径导入，
 *    统一入口后 IDE 能自动补全，且后续 API 调整只需改动一处。
 * 2. 与网页版 `src/api/index.js` 的聚合模式保持一致，减少跨端心智负担。
 */

export * from './auth'
export * from './user'
export * from './review'
export * from './gamification'
export * from './knowledge'
export * from './square'
export * from './community'
export * from './workspace'
export * from './notification'
export * from './push'
export * from './rag'
export * from './ai'
export * from './capture'
export * from './deerflow'
