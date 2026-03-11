import request from '@/utils/request'

export const chatAPI = {
  collect(data) {
    return request({
      url: '/chat/collect',
      method: 'post',
      data
    })
  },

  batchImport(data) {
    return request({
      url: '/chat/batch-import',
      method: 'post',
      data
    })
  },

  getList(params) {
    return request({
      url: '/chat/list',
      method: 'get',
      params
    })
  },

  getById(id) {
    return request({
      url: `/chat/${id}`,
      method: 'get'
    })
  }
}
