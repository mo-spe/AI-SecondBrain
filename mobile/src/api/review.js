import { request } from '@/utils/request'

// 复习模块 —— 端点与网页版 api/review.js 完全对齐
export const reviewAPI = {
  getTodayReviewCards(sortBy) {
    return request({ url: '/review/today', method: 'GET', params: sortBy ? { sortBy } : {} })
  },

  submitReviewResult(data) {
    return request({ url: '/review/submit', method: 'POST', data })
  },

  getOverview() {
    return request({ url: '/review/overview', method: 'GET' })
  },

  getStreakDays() {
    return request({ url: '/review/streak-days', method: 'GET' })
  },

  getUserAccuracy() {
    return request({ url: '/review/accuracy', method: 'GET' })
  },

  // ========== 题目池 ==========
  getPoolList(workspaceId) {
    return request({ url: '/review/pool', method: 'GET', params: { workspaceId } })
  },

  joinPool(poolId) {
    return request({ url: `/review/pool/${poolId}/join`, method: 'POST' })
  },
}

export default reviewAPI