import request from '@/utils/request'

export const authAPI = {
  register(data) {
    return request({
      url: '/auth/register',
      method: 'post',
      data
    })
  },

  login(data) {
    return request({
      url: '/auth/login',
      method: 'post',
      data
    })
  }
}
