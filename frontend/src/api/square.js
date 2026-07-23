import request from "@/utils/request";

export const squareAPI = {
  publish(data) {
    return request({ url: "/square/publish", method: "post", data });
  },

  unpublish(id) {
    return request({ url: `/square/${id}`, method: "delete" });
  },

  getList(params) {
    return request({ url: "/square/list", method: "get", params });
  },

  getDetail(id) {
    return request({ url: `/square/${id}`, method: "get" });
  },

  toggleLike(id) {
    return request({ url: `/square/${id}/like`, method: "post" });
  },

  addComment(id, data) {
    return request({ url: `/square/${id}/comment`, method: "post", data });
  },

  deleteComment(postId, commentId) {
    return request({ url: `/square/${postId}/comment/${commentId}`, method: "delete" });
  },

  toggleBookmark(id) {
    return request({ url: `/square/${id}/bookmark`, method: "post" });
  },

  getMyBookmarks(params) {
    return request({ url: "/square/bookmarks", method: "get", params });
  },

  getMyLikes(params) {
    return request({ url: "/square/likes", method: "get", params });
  },

  report(id, data) {
    return request({ url: `/square/${id}/report`, method: "post", data });
  },
};
