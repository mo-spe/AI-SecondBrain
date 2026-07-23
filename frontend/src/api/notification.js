import request from "@/utils/request";

export const notificationAPI = {
  getList(params) {
    return request({ url: "/notification/list", method: "get", params });
  },

  getUnreadCount() {
    return request({ url: "/notification/unread-count", method: "get" });
  },

  markAsRead(id) {
    return request({ url: `/notification/${id}/read`, method: "put" });
  },

  markAllAsRead() {
    return request({ url: "/notification/read-all", method: "put" });
  },
};
