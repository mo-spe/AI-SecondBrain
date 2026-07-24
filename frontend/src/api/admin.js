import request from "@/utils/request";

export const adminAPI = {
  getStatistics() {
    return request({
      url: "/admin/statistics",
      method: "get",
    });
  },

  getUsers(params) {
    return request({
      url: "/admin/users",
      method: "get",
      params,
    });
  },

  disableUser(id, status) {
    return request({
      url: `/admin/users/${id}/disable`,
      method: "put",
      params: { status },
    });
  },

  getWorkspaces(params) {
    return request({
      url: "/admin/workspaces",
      method: "get",
      params,
    });
  },

  disableWorkspace(id, status) {
    return request({
      url: `/admin/workspaces/${id}/disable`,
      method: "put",
      params: { status },
    });
  },

  getReports(params) {
    return request({
      url: "/admin/reports",
      method: "get",
      params,
    });
  },

  handleReport(id, data) {
    return request({
      url: `/admin/reports/${id}/handle`,
      method: "put",
      data,
    });
  },

  getSensitiveWords() {
    return request({
      url: "/admin/sensitive-words",
      method: "get",
    });
  },

  addSensitiveWord(word) {
    return request({
      url: "/admin/sensitive-words",
      method: "post",
      params: { word },
    });
  },

  deleteSensitiveWord(id) {
    return request({
      url: `/admin/sensitive-words/${id}`,
      method: "delete",
    });
  },

  // ===== AI 服务商管理 =====
  getAiProviders() {
    return request({
      url: "/admin/ai/providers",
      method: "get",
    });
  },

  saveAiProvider(data) {
    return request({
      url: "/admin/ai/providers",
      method: "post",
      data,
    });
  },

  updateAiProvider(id, data) {
    return request({
      url: `/admin/ai/providers/${id}`,
      method: "put",
      data,
    });
  },

  deleteAiProvider(id) {
    return request({
      url: `/admin/ai/providers/${id}`,
      method: "delete",
    });
  },

  getAiModels(providerId) {
    return request({
      url: `/admin/ai/providers/${providerId}/models`,
      method: "get",
    });
  },

  saveAiModel(providerId, data) {
    return request({
      url: `/admin/ai/providers/${providerId}/models`,
      method: "post",
      data,
    });
  },

  updateAiModel(id, data) {
    return request({
      url: `/admin/ai/models/${id}`,
      method: "put",
      data,
    });
  },

  deleteAiModel(id) {
    return request({
      url: `/admin/ai/models/${id}`,
      method: "delete",
    });
  },
};
