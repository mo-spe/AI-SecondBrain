import request from "@/utils/request";

export const reviewAPI = {
  getTodayReviewCards(sortBy) {
    return request({
      url: "/review/today",
      method: "get",
      params: sortBy ? { sortBy } : {},
    });
  },

  generateReviewCard(data) {
    return request({
      url: "/review/generate",
      method: "post",
      data,
    });
  },

  submitReviewResult(data) {
    return request({
      url: "/review/submit",
      method: "post",
      data,
    });
  },

  getReviewCardsByNodeId(nodeId) {
    return request({
      url: `/review/node/${nodeId}`,
      method: "get",
    });
  },

  deleteReviewCard(id) {
    return request({
      url: `/review/${id}`,
      method: "delete",
    });
  },

  deleteAllReviewCards() {
    return request({
      url: "/review/all",
      method: "delete",
    });
  },

  generateAllReviewCards() {
    return request({
      url: "/review/generate-all",
      method: "post",
    });
  },

  restoreReviewCards() {
    return request({
      url: "/review/restore",
      method: "post",
    });
  },

  updateMissingAnswers() {
    return request({
      url: "/review/update-answers",
      method: "post",
    });
  },

  getStreakDays() {
    return request({
      url: "/review/streak-days",
      method: "get",
    });
  },

  submitQualityFeedback(data) {
    return request({
      url: "/review/quality-feedback",
      method: "post",
      data,
    });
  },

  getUserAccuracy() {
    return request({
      url: "/review/accuracy",
      method: "get",
    });
  },

  getOverview() {
    return request({
      url: "/review/overview",
      method: "get",
    });
  },

  // ========== 题目池接口 ==========

  getPoolList(workspaceId) {
    return request({
      url: "/review/pool",
      method: "get",
      params: { workspaceId },
    });
  },

  getPoolDetail(poolId) {
    return request({
      url: `/review/pool/${poolId}`,
      method: "get",
    });
  },

  joinPool(poolId) {
    return request({
      url: `/review/pool/${poolId}/join`,
      method: "post",
    });
  },

  deletePoolItem(poolId) {
    return request({
      url: `/review/pool/${poolId}`,
      method: "delete",
    });
  },

  updatePoolItem(poolId, data) {
    return request({
      url: `/review/pool/${poolId}`,
      method: "put",
      data,
    });
  },
};
