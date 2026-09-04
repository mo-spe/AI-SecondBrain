import { request } from '@/utils/request'

/**
 * 工作区相关接口 —— 与 WorkspaceController 对齐：
 *   GET    /workspace                 我的工作区列表
 *   POST   /workspace                 创建
 *   PUT    /workspace/{id}            更新
 *   DELETE /workspace/{id}            删除
 *   PUT    /workspace/{id}/switch     切换（返回新 token，含 currentWsId 载荷）
 *   PUT    /workspace/personal        切换回个人空间
 *   GET    /workspace/{id}/members    成员列表
 */
export const workspaceAPI = {
  list() {
    return request({ url: '/workspace', method: 'GET' })
  },

  create(data) {
    return request({ url: '/workspace', method: 'POST', data })
  },

  getById(id) {
    return request({ url: `/workspace/${id}`, method: 'GET' })
  },

  update(id, data) {
    return request({ url: `/workspace/${id}`, method: 'PUT', data })
  },

  remove(id) {
    return request({ url: `/workspace/${id}`, method: 'DELETE' })
  },

  switchWorkspace(id) {
    return request({ url: `/workspace/${id}/switch`, method: 'PUT' })
  },

  switchToPersonal() {
    return request({ url: '/workspace/personal', method: 'PUT' })
  },

  listMembers(id) {
    return request({ url: `/workspace/${id}/members`, method: 'GET' })
  },
}

export default workspaceAPI
