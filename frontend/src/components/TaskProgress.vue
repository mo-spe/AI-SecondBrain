<template>
  <div class="task-progress-container">
    <el-card v-if="task" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="task-title">{{ taskTitle }}</span>
          <el-tag :type="getStatusType(task.status)" size="small">
            {{ getStatusText(task.status) }}
          </el-tag>
        </div>
      </template>

      <div class="progress-content">
        <el-progress
          :percentage="task.progress"
          :status="getProgressStatus(task.status)"
          :stroke-width="12"
          :text-inside="true"
        >
          <template #default="{ percentage }">
            <span class="progress-text">{{ percentage }}%</span>
          </template>
        </el-progress>

        <div class="task-info">
          <div class="info-item">
            <el-icon><Clock /></el-icon>
            <span>创建时间：{{ formatTime(task.createTime) }}</span>
          </div>
          <div v-if="task.completeTime" class="info-item">
            <el-icon><Check /></el-icon>
            <span>完成时间：{{ formatTime(task.completeTime) }}</span>
          </div>
        </div>

        <div v-if="task.status === 'FAILED'" class="error-message">
          <el-icon><Warning /></el-icon>
          <span>{{ task.errorMessage || '任务执行失败' }}</span>
        </div>

        <div v-if="task.status === 'COMPLETED'" class="success-message">
          <el-icon><SuccessFilled /></el-icon>
          <span>任务执行成功！</span>
        </div>

        <div v-if="task.status === 'PROCESSING'" class="processing-hint">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>任务处理中，请稍候...</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Clock, Check, Warning, SuccessFilled, Loading } from '@element-plus/icons-vue'

const props = defineProps({
  task: {
    type: Object,
    default: null
  }
})

const taskTitle = computed(() => {
  const titles = {
    'LEARNING_REPORT': '学习报告生成',
    'AI_RESEARCH': 'AI研究分析',
    'LEARNING_PATH': '学习路径生成',
    'KNOWLEDGE_BLIND_SPOT': '知识盲点分析'
  }
  return titles[props.task?.taskType] || '未知任务'
})

const getStatusType = (status) => {
  const types = {
    'PENDING': 'info',
    'PROCESSING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'PENDING': '等待中',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'FAILED': '失败'
  }
  return texts[status] || status
}

const getProgressStatus = (status) => {
  if (status === 'FAILED') return 'exception'
  if (status === 'COMPLETED') return 'success'
  return ''
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.task-progress-container {
  margin: 20px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.progress-content {
  padding: 20px 0;
}

.progress-text {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.task-info {
  margin-top: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.info-item:last-child {
  margin-bottom: 0;
}

.error-message {
  margin-top: 16px;
  padding: 12px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  color: #f56c6c;
  display: flex;
  align-items: center;
  gap: 8px;
}

.success-message {
  margin-top: 16px;
  padding: 12px;
  background: #f0f9ff;
  border: 1px solid #d1e9ff;
  border-radius: 8px;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.processing-hint {
  margin-top: 16px;
  padding: 12px;
  background: #fff7e6;
  border: 1px solid #ffe7ba;
  border-radius: 8px;
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 8px;
}

.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 移动端优化 */
@media (max-width: 768px) {
  .task-progress-container {
    margin: 12px 0;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .task-title {
    font-size: 14px;
  }

  .progress-content {
    padding: 16px 0;
  }

  .task-info {
    padding: 12px;
  }

  .info-item {
    font-size: 13px;
  }
}
</style>