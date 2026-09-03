import request from "@/utils/request";

export const communityAPI = {
  listQuestions(params) {
    return request({ url: "/community/questions", method: "get", params });
  },

  getQuestion(id) {
    return request({ url: `/community/questions/${id}`, method: "get" });
  },

  createQuestion(data) {
    return request({ url: "/community/questions", method: "post", data });
  },

  createAnswer(questionId, data) {
    return request({ url: `/community/questions/${questionId}/answers`, method: "post", data });
  },

  acceptAnswer(questionId, answerId) {
    return request({
      url: `/community/questions/${questionId}/answers/${answerId}/accept`,
      method: "post",
    });
  },

  getUserProfile(userId) {
    return request({ url: `/community/users/${userId}`, method: "get" });
  },

  updateMyProfile(data) {
    return request({ url: "/community/users/me/profile", method: "put", data });
  },

  followUser(userId) {
    return request({ url: `/community/users/${userId}/follow`, method: "post" });
  },

  unfollowUser(userId) {
    return request({ url: `/community/users/${userId}/follow`, method: "delete" });
  },

  getFollowers(userId, params) {
    return request({ url: `/community/users/${userId}/followers`, method: "get", params });
  },

  getFollowing(userId, params) {
    return request({ url: `/community/users/${userId}/following`, method: "get", params });
  },

  blockUser(userId) {
    return request({ url: `/community/users/${userId}/block`, method: "post" });
  },

  unblockUser(userId) {
    return request({ url: `/community/users/${userId}/block`, method: "delete" });
  },
};
