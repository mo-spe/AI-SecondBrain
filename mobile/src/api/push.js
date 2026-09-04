import { request } from '@/utils/request'

// 推送模块（移动端新增，对应后端待建的 PushController + DeviceToken 表）
export const pushAPI = {
  // 注册/刷新设备令牌，服务端按 platform 区分 iOS APNs / 安卓厂商 / 微信订阅消息
  registerDeviceToken({ platform, token }) {
    return request({ url: '/push/register', method: 'POST', data: { platform, token } })
  },

  // 解除设备绑定（登出时调用，避免继续收到推送）
  unregisterDeviceToken({ platform, token }) {
    return request({ url: '/push/unregister', method: 'POST', data: { platform, token } })
  },
}

export default pushAPI