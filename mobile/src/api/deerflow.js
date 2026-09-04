import { request } from '@/utils/request'

// AI 研究模块 —— 端点与后端 DeerFlowResearchController 对齐
export const deerFlowAPI = {
  // 异步生成学习报告
  generateLearningReport(data) {
    return request({ url: '/deerflow/research/learning-report-async', method: 'POST', data })
  },

  // 查询研究任务状态（轮询）
  getTaskStatus(taskNumber) {
    return request({ url: `/deerflow/research/status/${taskNumber}`, method: 'GET' })
  },

  // 保存研究历史
  saveHistory(data) {
    return request({ url: '/deerflow/research/history', method: 'POST', data })
  },

  // 查询研究历史列表
  getHistoryList(params) {
    return request({ url: '/deerflow/research/history', method: 'GET', params })
  },

  // 删除研究历史
  deleteHistory(id) {
    return request({ url: `/deerflow/research/history/${id}`, method: 'DELETE' })
  },
}

export default deerFlowAPI