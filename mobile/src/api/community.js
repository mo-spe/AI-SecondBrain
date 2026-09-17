import { request } from '@/utils/request'

// 问答社区 —— 端点与网页版 api/community.js 对齐
export const communityAPI = {
  listQuestions(params) {
    return request({ url: '/community/questions', method: 'GET', params })
  },

  getQuestion(id) {
    return request({ url: `/community/questions/${id}`, method: 'GET' })
  },

  createQuestion(data) {
    return request({ url: '/community/questions', method: 'POST', data })
  },

  createAnswer(questionId, data) {
    return request({ url: `/community/questions/${questionId}/answers`, method: 'POST', data })
  },

  getUserProfile(userId) {
    return request({ url: `/community/users/${userId}`, method: 'GET' })
  },

  followUser(userId) {
    return request({ url: `/community/users/${userId}/follow`, method: 'POST' })
  },

  unfollowUser(userId) {
    return request({ url: `/community/users/${userId}/follow`, method: 'DELETE' })
  },

  updateMyProfile(data) {
    return request({ url: '/community/users/me/profile', method: 'PUT', data })
  },

  getFollowers(userId, params) {
    return request({ url: `/community/users/${userId}/followers`, method: 'GET', params })
  },

  getFollowing(userId, params) {
    return request({ url: `/community/users/${userId}/following`, method: 'GET', params })
  },

  blockUser(userId) {
    return request({ url: `/community/users/${userId}/block`, method: 'POST' })
  },

  unblockUser(userId) {
    return request({ url: `/community/users/${userId}/block`, method: 'DELETE' })
  },
}

export default communityAPI
