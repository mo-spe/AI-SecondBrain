import request from "@/utils/request";

export const workspaceAPI = {
  list() {
    return request({
      url: "/workspace",
      method: "get",
    });
  },

  getById(id) {
    return request({
      url: `/workspace/${id}`,
      method: "get",
    });
  },

  create(data) {
    return request({
      url: "/workspace",
      method: "post",
      data,
    });
  },

  update(id, data) {
    return request({
      url: `/workspace/${id}`,
      method: "put",
      data,
    });
  },

  delete(id) {
    return request({
      url: `/workspace/${id}`,
      method: "delete",
    });
  },

  switchWorkspace(id) {
    return request({
      url: `/workspace/${id}/switch`,
      method: "put",
    });
  },

  switchToPersonal() {
    return request({
      url: "/workspace/personal",
      method: "put",
    });
  },

  getMembers(id) {
    return request({
      url: `/workspace/${id}/members`,
      method: "get",
    });
  },

  addMember(id, data) {
    return request({
      url: `/workspace/${id}/members`,
      method: "post",
      data,
    });
  },

  updateMemberRole(id, userId, data) {
    return request({
      url: `/workspace/${id}/members/${userId}`,
      method: "put",
      data,
    });
  },

  removeMember(id, userId) {
    return request({
      url: `/workspace/${id}/members/${userId}`,
      method: "delete",
    });
  },

  acceptInvitation(id) {
    return request({
      url: `/workspace/${id}/members/accept`,
      method: "put",
    });
  },

  transferOwnership(id, data) {
    return request({
      url: `/workspace/${id}/transfer`,
      method: "put",
      data,
    });
  },
};
