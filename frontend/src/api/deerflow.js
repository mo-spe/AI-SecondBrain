import request from "@/utils/request";

export const deerFlowAPI = {
  generateLearningReport(data) {
    return request({
      url: "/deerflow/research/learning-report",
      method: "post",
      data,
    });
  },

  generateLearningPath(data) {
    return request({
      url: "/deerflow/research/learning-path",
      method: "post",
      data,
    });
  },

  researchKnowledgeGap(data) {
    return request({
      url: "/deerflow/research/knowledge-gap",
      method: "post",
      data,
    });
  },

  saveResearchHistory(data) {
    return request({
      url: "/deerflow/research/history",
      method: "post",
      data,
    });
  },

  getResearchHistoryList(params) {
    return request({
      url: "/deerflow/research/history",
      method: "get",
      params,
    });
  },

  deleteResearchHistory(id) {
    return request({
      url: `/deerflow/research/history/${id}`,
      method: "delete",
    });
  },

  checkHealth() {
    return request({
      url: "/deerflow/health",
      method: "get",
    });
  },
};
