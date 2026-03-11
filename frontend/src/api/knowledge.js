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

  createKnowledge(data) {
    return request({
      url: "/knowledge",
      method: "post",
      data,
    });
  },
};
