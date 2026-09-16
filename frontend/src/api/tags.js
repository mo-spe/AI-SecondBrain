import request from "@/utils/request";

export const tagsAPI = {
  /** 获取标签树（含层级结构和知识点数量） */
  getTree() {
    return request({
      url: "/tags/tree",
      method: "get",
    });
  },

  /** 获取所有标签（扁平列表） */
  getAll() {
    return request({
      url: "/tags/all",
      method: "get",
    });
  },

  /** 创建标签 */
  create({ tagName, tagColor, parentId }) {
    return request({
      url: "/tags",
      method: "post",
      params: { tagName, tagColor, parentId },
    });
  },

  /** 更新标签 */
  update(id, data) {
    return request({
      url: `/tags/${id}`,
      method: "put",
      data,
    });
  },

  /** 删除标签 */
  delete(id) {
    return request({
      url: `/tags/${id}`,
      method: "delete",
    });
  },

  /** 给知识点添加标签 */
  addToNode(nodeId, tagId) {
    return request({
      url: `/tags/node/${nodeId}/tag/${tagId}`,
      method: "post",
    });
  },

  /** 移除知识点的标签 */
  removeFromNode(nodeId, tagId) {
    return request({
      url: `/tags/node/${nodeId}/tag/${tagId}`,
      method: "delete",
    });
  },

  /** 获取知识点的标签 */
  getByNode(nodeId) {
    return request({
      url: `/tags/node/${nodeId}`,
      method: "get",
    });
  },

  /** AI建议标签 */
  suggest(title, summary) {
    return request({
      url: "/tags/ai-suggest",
      method: "post",
      data: { title, summary },
    });
  },
};
