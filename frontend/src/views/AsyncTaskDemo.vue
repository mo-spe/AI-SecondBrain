<template>
  <div class="async-task-demo">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>异步任务演示</span>
          <el-button type="primary" @click="startTask" :loading="taskLoading">
            开始任务
          </el-button>
        </div>
      </template>

      <div class="task-controls">
        <el-form :model="taskForm" label-width="100px">
          <el-form-item label="任务类型">
            <el-select v-model="taskForm.taskType" placeholder="选择任务类型">
              <el-option label="学习报告" value="LEARNING_REPORT" />
              <el-option label="AI研究" value="AI_RESEARCH" />
              <el-option label="学习路径" value="LEARNING_PATH" />
              <el-option label="知识盲点" value="KNOWLEDGE_BLIND_SPOT" />
            </el-select>
          </el-form-item>

          <el-form-item label="主题">
            <el-input v-model="taskForm.topic" placeholder="输入主题" />
          </el-form-item>

          <el-form-item label="当前水平">
            <el-select v-model="taskForm.currentLevel" placeholder="选择水平">
              <el-option label="初学者" value="beginner" />
              <el-option label="中级" value="intermediate" />
              <el-option label="高级" value="advanced" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <TaskProgress v-if="currentTask" :task="currentTask" />

      <div v-if="taskResult" class="task-result">
        <h3>任务结果</h3>
        <div class="result-content">{{ taskResult }}</div>
      </div>
    </el-card>

    <el-card shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>WebSocket状态</span>
          <el-tag :type="wsConnected ? 'success' : 'danger'">
            {{ wsConnected ? '已连接' : '未连接' }}
          </el-tag>
        </div>
      </template>

      <div class="ws-info">
        <div class="info-item">
          <span class="label">连接状态:</span>
          <span class="value">{{ wsConnected ? '已连接' : '未连接' }}</span>
        </div>
        <div class="info-item">
          <span class="label">重连次数:</span>
          <span class="value">{{ reconnectCount }}</span>
        </div>
        <div class="info-item">
          <span class="label">接收消息:</span>
          <span class="value">{{ messageCount }}</span>
        </div>
      </div>

      <div class="ws-actions">
        <el-button type="primary" @click="connectWebSocket" :disabled="wsConnected">
          连接WebSocket
        </el-button>
        <el-button type="danger" @click="disconnectWebSocket" :disabled="!wsConnected">
          断开连接
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import TaskProgress from '@/components/TaskProgress.vue'
import websocketService from '@/utils/websocket'
import request from '@/utils/request'

const taskLoading = ref(false)
const currentTask = ref(null)
const taskResult = ref(null)
const wsConnected = ref(false)
const reconnectCount = ref(0)
const messageCount = ref(0)

const taskForm = ref({
  taskType: 'LEARNING_PATH',
  topic: 'Python编程',
  currentLevel: 'beginner'
})

let taskUpdateListener = null
let taskProgressListener = null
let taskCompleteListener = null
let taskFailedListener = null

const connectWebSocket = () => {
  const userId = localStorage.getItem('userId') || '1'
  websocketService.connect(userId)
  
  wsConnected.value = true
  
  ElMessage.success('WebSocket连接成功')
}

const disconnectWebSocket = () => {
  websocketService.disconnect()
  wsConnected.value = false
  
  ElMessage.info('WebSocket已断开')
}

const setupWebSocketListeners = () => {
  taskUpdateListener = (data) => {
    console.log('任务更新:', data)
    messageCount.value++
    
    if (data.taskId === currentTask.value?.taskId) {
      currentTask.value = data
    }
  }
  
  taskProgressListener = (data) => {
    console.log('任务进度:', data)
    messageCount.value++
    
    if (data.taskId === currentTask.value?.taskId) {
      currentTask.value = { ...currentTask.value, progress: data.progress }
    }
  }
  
  taskCompleteListener = (data) => {
    console.log('任务完成:', data)
    messageCount.value++
    
    if (data.taskId === currentTask.value?.taskId) {
      currentTask.value = data
      taskResult.value = data.result
      
      ElMessage.success('任务执行成功！')
    }
  }
  
  taskFailedListener = (data) => {
    console.log('任务失败:', data)
    messageCount.value++
    
    if (data.taskId === currentTask.value?.taskId) {
      currentTask.value = data
      
      ElMessage.error('任务执行失败：' + data.errorMessage)
    }
  }
  
  websocketService.subscribe('task_update', taskUpdateListener)
  websocketService.subscribe('task_progress', taskProgressListener)
  websocketService.subscribe('task_complete', taskCompleteListener)
  websocketService.subscribe('task_failed', taskFailedListener)
}

const removeWebSocketListeners = () => {
  if (taskUpdateListener) {
    websocketService.unsubscribe('task_update', taskUpdateListener)
  }
  if (taskProgressListener) {
    websocketService.unsubscribe('task_progress', taskProgressListener)
  }
  if (taskCompleteListener) {
    websocketService.unsubscribe('task_complete', taskCompleteListener)
  }
  if (taskFailedListener) {
    websocketService.unsubscribe('task_failed', taskFailedListener)
  }
}

const startTask = async () => {
  try {
    taskLoading.value = true
    taskResult.value = null
    
    const response = await request({
      url: '/deerflow/research/learning-path-async',
      method: 'post',
      data: {
        topic: taskForm.value.topic,
        currentLevel: taskForm.value.currentLevel
      }
    })
    
    if (response.code === 200) {
      currentTask.value = {
        taskId: response.data.taskId,
        taskType: taskForm.value.taskType,
        status: 'PENDING',
        progress: 0,
        createTime: new Date().toISOString()
      }
      
      ElMessage.success('任务已创建，正在处理中...')
      
      if (!wsConnected.value) {
        ElMessage.warning('WebSocket未连接，建议连接以获取实时更新')
      }
    } else {
      ElMessage.error('创建任务失败：' + response.message)
    }
  } catch (error) {
    console.error('启动任务失败:', error)
    ElMessage.error('启动任务失败：' + error.message)
  } finally {
    taskLoading.value = false
  }
}

onMounted(() => {
  setupWebSocketListeners()
  
  if (websocketService.isConnected()) {
    wsConnected.value = true
  }
})

onUnmounted(() => {
  removeWebSocketListeners()
})
</script>

<style scoped>
.async-task-demo {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-controls {
  margin-bottom: 20px;
}

.task-result {
  margin-top: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.task-result h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
}

.result-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  color: #606266;
}

.ws-info {
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  font-weight: 500;
  color: #303133;
}

.info-item .value {
  color: #606266;
}

.ws-actions {
  display: flex;
  gap: 12px;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .async-task-demo {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .ws-actions {
    flex-direction: column;
  }

  .ws-actions .el-button {
    width: 100%;
  }
}
</style>