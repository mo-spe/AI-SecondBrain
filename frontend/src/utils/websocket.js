import { ElNotification } from 'element-plus'

class WebSocketService {
  constructor() {
    this.ws = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectInterval = 3000
    this.listeners = new Map()
    this.isConnected = false
  }

  connect(userId) {
    if (this.ws) {
      this.disconnect()
    }

    const wsUrl = `ws://localhost:8080/api/ws`
    
    try {
      this.ws = new WebSocket(wsUrl)
      
      this.ws.onopen = () => {
        console.log('WebSocket连接成功')
        this.isConnected = true
        this.reconnectAttempts = 0
        
        ElNotification({
          title: '连接成功',
          message: '实时通知已启用',
          type: 'success',
          duration: 2000
        })
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.notifyListeners(data)
        } catch (error) {
          console.error('解析WebSocket消息失败:', error)
        }
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket错误:', error)
        this.isConnected = false
      }

      this.ws.onclose = () => {
        console.log('WebSocket连接关闭')
        this.isConnected = false
        this.attemptReconnect(userId)
      }

    } catch (error) {
      console.error('创建WebSocket连接失败:', error)
      this.attemptReconnect(userId)
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
      this.isConnected = false
    }
  }

  attemptReconnect(userId) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
      
      setTimeout(() => {
        this.connect(userId)
      }, this.reconnectInterval)
    } else {
      ElNotification({
        title: '连接失败',
        message: '无法建立实时连接，请刷新页面重试',
        type: 'error',
        duration: 5000
      })
    }
  }

  subscribe(eventType, callback) {
    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, [])
    }
    this.listeners.get(eventType).push(callback)
  }

  unsubscribe(eventType, callback) {
    if (this.listeners.has(eventType)) {
      const callbacks = this.listeners.get(eventType)
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  notifyListeners(data) {
    const eventType = data.type || 'task_update'
    
    if (this.listeners.has(eventType)) {
      const callbacks = this.listeners.get(eventType)
      callbacks.forEach(callback => {
        try {
          callback(data)
        } catch (error) {
          console.error('执行回调函数失败:', error)
        }
      })
    }
  }

  isConnected() {
    return this.isConnected
  }
}

export default new WebSocketService()