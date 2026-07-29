import request from "@/utils/request";

export const knowledgeAPI = {
  getList(params) {
    return request({
      url: "/knowledge/list",
      method: "get",
      params,
    });
  },

  getById(id) {
    return request({
      url: `/knowledge/${id}`,
      method: "get",
    });
  },

  search(params) {
    return request({
      url: "/knowledge/search",
      method: "get",
      params,
    });
  },

  multiFieldSearch(params) {
    return request({
      url: "/knowledge/search/multi",
      method: "get",
      params,
    });
  },

  semanticSearch(params) {
    return request({
      url: "/knowledge/search/semantic",
      method: "get",
      params,
    });
  },

  deleteById(id) {
    return request({
      url: `/knowledge/${id}`,
      method: "delete",
    });
  },

  updateImportance(id, importance) {
    return request({
      url: `/knowledge/${id}/importance`,
      method: "put",
      params: { importance },
    });
  },

  updateKnowledge(id, data) {
    return request({
      url: `/knowledge/${id}`,
      method: "put",
      data,
    });
  },

  toggleNeedReview(id, needReview) {
    return request({
      url: `/knowledge/${id}/review-target`,
      method: "put",
      params: { needReview },
    });
  },

  createKnowledge(data) {
    return request({
      url: "/knowledge",
      method: "post",
      data,
    });
  },

  // ========== 待确认知识点 ==========

  getPendingKnowledge(params) {
    return request({
      url: "/knowledge/pending",
      method: "get",
      params,
    });
  },

  confirmPendingKnowledge(data) {
    return request({
      url: "/knowledge/pending/confirm",
      method: "post",
      data,
    });
  },

  discardPendingKnowledge(id) {
    return request({
      url: `/knowledge/pending/${id}`,
      method: "delete",
    });
  },

  addPendingKnowledge(data) {
    return request({
      url: "/knowledge/pending/add",
      method: "post",
      data,
    });
  },
};
