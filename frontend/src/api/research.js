import request from "@/utils/request";

export const researchAPI = {
  // ======================== 研究项目 ========================

  createProject(data) {
    return request({
      url: "/research/projects",
      method: "post",
      data,
    });
  },

  listProjects(params) {
    return request({
      url: "/research/projects",
      method: "get",
      params,
    });
  },

  getProject(id) {
    return request({
      url: `/research/projects/${id}`,
      method: "get",
    });
  },

  updateProject(id, data) {
    return request({
      url: `/research/projects/${id}`,
      method: "put",
      data,
    });
  },

  deleteProject(id) {
    return request({
      url: `/research/projects/${id}`,
      method: "delete",
    });
  },

  executeProject(id) {
    return request({
      url: `/research/projects/${id}/execute`,
      method: "post",
    });
  },

  pauseProject(id) {
    return request({
      url: `/research/projects/${id}/pause`,
      method: "post",
    });
  },

  resumeProject(id) {
    return request({
      url: `/research/projects/${id}/resume`,
      method: "post",
    });
  },

  archiveProject(id) {
    return request({
      url: `/research/projects/${id}/archive`,
      method: "post",
    });
  },

  // ======================== 研究任务 ========================

  listTasks(projectId) {
    return request({
      url: `/research/projects/${projectId}/tasks`,
      method: "get",
    });
  },

  getTask(projectId, taskId) {
    return request({
      url: `/research/projects/${projectId}/tasks/${taskId}`,
      method: "get",
    });
  },

  createTask(projectId, data) {
    return request({
      url: `/research/projects/${projectId}/tasks`,
      method: "post",
      data,
    });
  },

  updateTask(projectId, taskId, data) {
    return request({
      url: `/research/projects/${projectId}/tasks/${taskId}`,
      method: "put",
      data,
    });
  },

  deleteTask(projectId, taskId) {
    return request({
      url: `/research/projects/${projectId}/tasks/${taskId}`,
      method: "delete",
    });
  },

  batchCreateTasks(projectId, data) {
    return request({
      url: `/research/projects/${projectId}/tasks/batch`,
      method: "post",
      data,
    });
  },

  // ======================== 研究计划 ========================

  getLatestPlan(projectId) {
    return request({
      url: `/research/projects/${projectId}/plans/latest`,
      method: "get",
    });
  },

  listPlans(projectId) {
    return request({
      url: `/research/projects/${projectId}/plans`,
      method: "get",
    });
  },

  getPlan(projectId, planId) {
    return request({
      url: `/research/projects/${projectId}/plans/${planId}`,
      method: "get",
    });
  },

  // ======================== 研究来源 ========================

  listSources(projectId) {
    return request({
      url: `/research/projects/${projectId}/sources`,
      method: "get",
    });
  },

  getSource(projectId, sourceId) {
    return request({
      url: `/research/projects/${projectId}/sources/${sourceId}`,
      method: "get",
    });
  },

  deleteSource(projectId, sourceId) {
    return request({
      url: `/research/projects/${projectId}/sources/${sourceId}`,
      method: "delete",
    });
  },

  // ======================== 研究步骤 ========================

  listTaskSteps(projectId, taskId) {
    return request({
      url: `/research/projects/${projectId}/tasks/${taskId}/steps`,
      method: "get",
    });
  },

  listProjectSteps(projectId) {
    return request({
      url: `/research/projects/${projectId}/steps`,
      method: "get",
    });
  },

  // ======================== 研究报告 ========================

  getLatestReport(projectId) {
    return request({
      url: `/research/projects/${projectId}/report`,
      method: "get",
    });
  },

  listReports(projectId) {
    return request({
      url: `/research/projects/${projectId}/reports`,
      method: "get",
    });
  },

  // ======================== 研究记忆 ========================

  listMemory(projectId) {
    return request({
      url: `/research/projects/${projectId}/memory`,
      method: "get",
    });
  },

  listMemoryByType(projectId, type) {
    return request({
      url: `/research/projects/${projectId}/memory/${type}`,
      method: "get",
    });
  },
};
