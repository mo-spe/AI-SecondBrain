import { request } from '@/utils/request'

// 知识管理 —— 端点与网页版 api/knowledge.js 对齐
export const knowledgeAPI = {
  getList(params) {
    return request({ url: '/knowledge/list', method: 'GET', params })
  },

  getById(id) {
    return request({ url: `/knowledge/${id}`, method: 'GET' })
  },

  search(params) {
    return request({ url: '/knowledge/search', method: 'GET', params })
  },

  createKnowledge(data) {
    return request({ url: '/knowledge', method: 'POST', data })
  },

  updateKnowledge(id, data) {
    return request({ url: `/knowledge/${id}`, method: 'PUT', data })
  },

  deleteById(id) {
    return request({ url: `/knowledge/${id}`, method: 'DELETE' })
  },

  toggleNeedReview(id, needReview) {
    return request({ url: `/knowledge/${id}/review-target`, method: 'PUT', params: { needReview } })
  },

  // 知识关系图谱数据（节点 + 关系边）
  getRelationGraph() {
    return request({ url: '/knowledge/relation/graph', method: 'GET' })
  },
}

export default knowledgeAPI