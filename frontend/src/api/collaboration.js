import request from "@/utils/request";

/**
 * 协作 API 模块.
 * 提供编辑锁和版本历史相关接口。
 */
export const collaborationAPI = {
  // ========== 编辑锁 ==========

  /** 获取编辑锁 */
  acquireLock(nodeId) {
    return request({
      url: `/knowledge/${nodeId}/lock`,
      method: "post",
    });
  },

  /** 释放编辑锁 */
  releaseLock(nodeId) {
    return request({
      url: `/knowledge/${nodeId}/lock`,
      method: "delete",
    });
  },

  /** 查询锁状态 */
  getLockStatus(nodeId) {
    return request({
      url: `/knowledge/${nodeId}/lock`,
      method: "get",
    });
  },

  // ========== 版本历史 ==========

  /** 获取版本历史列表 */
  getRevisionList(nodeId) {
    return request({
      url: `/knowledge/${nodeId}/revisions`,
      method: "get",
    });
  },

  /** 获取版本详情 */
  getRevisionDetail(nodeId, revisionId) {
    return request({
      url: `/knowledge/${nodeId}/revisions/${revisionId}`,
      method: "get",
    });
  },

  /** 回滚到指定版本 */
  rollbackRevision(nodeId, revisionId) {
    return request({
      url: `/knowledge/${nodeId}/revisions/${revisionId}/rollback`,
      method: "post",
    });
  },

  // ========== 分享链接 ==========

  /** 创建分享链接 */
  createShare(data) {
    return request({
      url: "/share",
      method: "post",
      data,
    });
  },

  /** 获取我的分享列表 */
  getShareList() {
    return request({
      url: "/share/list",
      method: "get",
    });
  },

  /** 撤销分享 */
  revokeShare(id) {
    return request({
      url: `/share/${id}`,
      method: "delete",
    });
  },
};
