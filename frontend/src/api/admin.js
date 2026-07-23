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
};
