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
};
