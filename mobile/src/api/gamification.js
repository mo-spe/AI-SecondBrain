import { request } from '@/utils/request'

// 游戏化模块（签到/成就/排行榜）—— 端点与网页版 api/gamification.js 对齐
export const gamificationAPI = {
  getProfile() {
    return request({ url: '/gamification/profile', method: 'GET' })
  },

  checkIn() {
    return request({ url: '/gamification/check-in', method: 'POST' })
  },

  getAchievements(params) {
    return request({ url: '/gamification/achievements', method: 'GET', params })
  },

  getLeaderboard(params) {
    return request({ url: '/gamification/leaderboard', method: 'GET', params })
  },

  getStreakCalendar(months) {
    return request({ url: '/gamification/streak-calendar', method: 'GET', params: { months } })
  },

  getPointsLog(params) {
    return request({ url: '/gamification/points-log', method: 'GET', params })
  },
}

export default gamificationAPI