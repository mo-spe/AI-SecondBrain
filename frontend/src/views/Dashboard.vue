<template>
  <div class="dashboard-container">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><DataAnalysis /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">数据统计</h1>
            <p class="welcome-subtitle">您的学习数据总览</p>
          </div>
        </div>
      </div>

      <div class="stats-section">
        <div class="stat-grid">
          <div class="stat-card chat">
            <div class="stat-header">
              <div class="stat-icon-wrapper chat">
                <el-icon :size="32"><ChatDotRound /></el-icon>
              </div>
              <div class="stat-trend">
                <el-icon class="trend-up"><TrendCharts /></el-icon>
                <span>+12%</span>
              </div>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ statistics.chatCount || 0 }}</div>
              <div class="stat-label">对话总数</div>
            </div>
            <div class="stat-footer">
              <div class="stat-chart">
                <div class="chart-bar" style="width: 80%"></div>
              </div>
            </div>
          </div>

          <div class="stat-card knowledge">
            <div class="stat-header">
              <div class="stat-icon-wrapper knowledge">
                <el-icon :size="32"><Reading /></el-icon>
              </div>
              <div class="stat-trend">
                <el-icon class="trend-up"><TrendCharts /></el-icon>
                <span>+8%</span>
              </div>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ statistics.knowledgeCount || 0 }}</div>
              <div class="stat-label">知识点总数</div>
            </div>
            <div class="stat-footer">
              <div class="stat-chart">
                <div class="chart-bar" style="width: 65%"></div>
              </div>
            </div>
          </div>

          <div class="stat-card review">
            <div class="stat-header">
              <div class="stat-icon-wrapper review">
                <el-icon :size="32"><Bell /></el-icon>
              </div>
              <div class="stat-trend">
                <el-icon class="trend-down"><Bottom /></el-icon>
                <span>-5%</span>
              </div>
            </div>
            <div class="stat-body">
              <div class="stat-value">
                {{ statistics.pendingReviewCount || 0 }}
              </div>
              <div class="stat-label">待复习</div>
            </div>
            <div class="stat-footer">
              <div class="stat-chart">
                <div class="chart-bar" style="width: 45%"></div>
              </div>
            </div>
          </div>

          <div class="stat-card completed">
            <div class="stat-header">
              <div class="stat-icon-wrapper completed">
                <el-icon :size="32"><CircleCheck /></el-icon>
              </div>
              <div class="stat-trend">
                <el-icon class="trend-up"><TrendCharts /></el-icon>
                <span>+15%</span>
              </div>
            </div>
            <div class="stat-body">
              <div class="stat-value">
                {{ statistics.completedReviewCount || 0 }}
              </div>
              <div class="stat-label">已完成复习</div>
            </div>
            <div class="stat-footer">
              <div class="stat-chart">
                <div class="chart-bar" style="width: 90%"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="content-section">
        <div class="content-grid">
          <div class="content-card">
            <div class="card-header">
              <h3>
                <el-icon><ChatDotRound /></el-icon>
                最近对话
              </h3>
              <el-tag type="info">{{ recentChats.length }}条</el-tag>
            </div>
            <div class="card-body">
              <div v-loading="chatsLoading" class="list-container">
                <div
                  v-for="chat in recentChats"
                  :key="chat.id"
                  class="list-item"
                  @click="viewChatDetail(chat)"
                >
                  <div class="item-header">
                    <el-tag :type="getPlatformType(chat.platform)" size="small">
                      {{ chat.platform }}
                    </el-tag>
                    <span class="item-time">{{
                      formatRelativeTime(chat.createTime)
                    }}</span>
                  </div>
                  <div class="item-content">{{ chat.content }}</div>
                </div>
                <el-empty
                  v-if="!chatsLoading && recentChats.length === 0"
                  description="暂无对话数据"
                  :image-size="100"
                />
              </div>
            </div>
          </div>

          <div class="content-card">
            <div class="card-header">
              <h3>
                <el-icon><Reading /></el-icon>
                最近知识点
              </h3>
              <el-tag type="info">{{ recentKnowledge.length }}条</el-tag>
            </div>
            <div class="card-body">
              <div v-loading="knowledgeLoading" class="list-container">
                <div
                  v-for="knowledge in recentKnowledge"
                  :key="knowledge.id"
                  class="list-item"
                  @click="viewKnowledgeDetail(knowledge)"
                >
                  <div class="item-header">
                    <el-rate
                      v-model="knowledge.importance"
                      disabled
                      show-score
                      text-color="#ff9900"
                      :max="5"
                      size="small"
                    />
                    <span class="item-time">{{
                      formatRelativeTime(knowledge.createTime)
                    }}</span>
                  </div>
                  <div class="item-title">{{ knowledge.title }}</div>
                  <div class="item-summary">{{ knowledge.summary }}</div>
                </div>
                <el-empty
                  v-if="!knowledgeLoading && recentKnowledge.length === 0"
                  description="暂无知识点"
                  :image-size="100"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-section">
        <div class="chart-card">
          <div class="chart-header">
            <h3>
              <el-icon><TrendCharts /></el-icon>
              学习趋势
            </h3>
            <el-radio-group v-model="chartPeriod" size="small">
              <el-radio-button value="week">本周</el-radio-button>
              <el-radio-button value="month">本月</el-radio-button>
              <el-radio-button value="year">全年</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="chartRef" class="chart-container"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from "vue";
import { ElMessage } from "element-plus";
import { statisticsAPI } from "@/api/statistics";
import { chatAPI } from "@/api/chat";
import { knowledgeAPI } from "@/api/knowledge";
import * as echarts from "echarts";
import {
  DataAnalysis,
  ChatDotRound,
  Reading,
  Bell,
  CircleCheck,
  TrendCharts,
  Bottom,
} from "@element-plus/icons-vue";

const statistics = ref({
  chatCount: 0,
  knowledgeCount: 0,
  pendingReviewCount: 0,
  completedReviewCount: 0,
});

const recentChats = ref([]);
const recentKnowledge = ref([]);
const chatsLoading = ref(false);
const knowledgeLoading = ref(false);
const chartPeriod = ref("week");

const chartRef = ref(null);
let chartInstance = null;

const loadStatistics = async () => {
  try {
    const data = await statisticsAPI.getStatistics();
    console.log("统计数据API返回的数据:", data);
    statistics.value = data;
    console.log("设置后的statistics.value:", statistics.value);
  } catch (error) {
    console.error("加载统计数据失败:", error);
    ElMessage.error("加载统计数据失败：" + error.message);
  }
};

const loadChartData = async () => {
  try {
    const data = await statisticsAPI.getChartData(chartPeriod.value);
    console.log("图表数据API返回的数据:", data);
    return data;
  } catch (error) {
    console.error("加载图表数据失败:", error);
    ElMessage.error("加载图表数据失败：" + error.message);
    return null;
  }
};

const loadRecentChats = async () => {
  try {
    chatsLoading.value = true;
    const data = await chatAPI.getList({ current: 1, size: 5 });
    recentChats.value = data.records || [];
  } catch (error) {
    ElMessage.error("加载对话数据失败：" + error.message);
  } finally {
    chatsLoading.value = false;
  }
};

const loadRecentKnowledge = async () => {
  try {
    knowledgeLoading.value = true;
    const data = await knowledgeAPI.getList({ current: 1, size: 5 });
    recentKnowledge.value = data.records || [];
  } catch (error) {
    ElMessage.error("加载知识点数据失败：" + error.message);
  } finally {
    knowledgeLoading.value = false;
  }
};

const getPlatformType = (platform) => {
  const typeMap = {
    ChatGPT: "primary",
    DeepSeek: "success",
    Kimi: "warning",
    Other: "info",
  };
  return typeMap[platform] || "info";
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

const formatRelativeTime = (dateStr) => {
  if (!dateStr) return "-";
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now - date;

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return formatDate(dateStr);
};

const viewChatDetail = (chat) => {
  ElMessage.info("查看对话详情功能开发中");
};

const viewKnowledgeDetail = (knowledge) => {
  ElMessage.info("查看知识点详情功能开发中");
};

const initChart = async () => {
  if (!chartRef.value) return;

  chartInstance = echarts.init(chartRef.value);

  const chartData = await loadChartData();
  if (!chartData) {
    console.error("图表数据为空，使用默认数据");
    return;
  }

  const option = {
    backgroundColor: "transparent",
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "cross",
      },
    },
    legend: {
      data: ["对话数", "知识点数", "复习数"],
      textStyle: {
        color: "#606266",
      },
    },
    grid: {
      left: "3%",
      right: "4%",
      bottom: "3%",
      containLabel: true,
    },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: chartData.labels || [],
      axisLabel: {
        color: "#909399",
      },
      axisLine: {
        lineStyle: {
          color: "#e0e0e0",
        },
      },
    },
    yAxis: {
      type: "value",
      axisLabel: {
        color: "#909399",
      },
      axisLine: {
        lineStyle: {
          color: "#e0e0e0",
        },
      },
      splitLine: {
        lineStyle: {
          color: "#f0f0f0",
        },
      },
    },
    series: [
      {
        name: "对话数",
        type: "line",
        smooth: true,
        data: chartData.chatData || [],
        itemStyle: {
          color: "#667eea",
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(102, 126, 234, 0.3)" },
            { offset: 1, color: "rgba(102, 126, 234, 0.05)" },
          ]),
        },
      },
      {
        name: "知识点数",
        type: "line",
        smooth: true,
        data: chartData.knowledgeData || [],
        itemStyle: {
          color: "#764ba2",
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(118, 75, 162, 0.3)" },
            { offset: 1, color: "rgba(118, 75, 162, 0.05)" },
          ]),
        },
      },
      {
        name: "复习数",
        type: "line",
        smooth: true,
        data: chartData.reviewData || [],
        itemStyle: {
          color: "#67c23a",
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(103, 194, 58, 0.3)" },
            { offset: 1, color: "rgba(103, 194, 58, 0.05)" },
          ]),
        },
      },
    ],
  };

  chartInstance.setOption(option);
};

watch(chartPeriod, () => {
  if (chartInstance) {
    initChart();
  }
});

onMounted(() => {
  loadStatistics();
  loadRecentChats();
  loadRecentKnowledge();
  setTimeout(() => {
    initChart();
  }, 100);
});

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose();
  }
});
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

.background-gradient {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 0;
}

.main-content {
  position: relative;
  z-index: 1;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-section {
  margin-bottom: 20px;
}

.welcome-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 30px;
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInDown 0.6s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.welcome-icon {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.welcome-content {
  flex: 1;
}

.welcome-title {
  font-size: 32px;
  font-weight: bold;
  color: white;
  margin: 0 0 8px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.welcome-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.stats-section {
  margin-bottom: 20px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.3);
}

.stat-card.chat .stat-icon-wrapper {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-card.knowledge .stat-icon-wrapper {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
}

.stat-card.review .stat-icon-wrapper {
  background: linear-gradient(135deg, #e6a23c 0%, #f0c78a 100%);
  color: white;
}

.stat-card.completed .stat-icon-wrapper {
  background: linear-gradient(135deg, #f56c6c 0%, #f89898 100%);
  color: white;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.trend-up {
  color: #67c23a;
}

.trend-down {
  color: #f56c6c;
}

.stat-body {
  margin-bottom: 16px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-footer {
  margin-top: 12px;
}

.stat-chart {
  height: 4px;
  background: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
}

.chart-bar {
  height: 100%;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 2px;
  transition: width 1s ease;
}

.content-section {
  margin-bottom: 20px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.content-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease-out;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.card-header h3 {
  font-size: 18px;
  color: #303133;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header h3 .el-icon {
  color: #667eea;
}

.card-body {
  min-height: 300px;
}

.list-container {
  max-height: 400px;
  overflow-y: auto;
}

.list-item {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.list-item:hover {
  background: #f0f0f0;
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-time {
  font-size: 12px;
  color: #909399;
}

.item-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-title {
  color: #303133;
  font-size: 15px;
  font-weight: bold;
  margin-bottom: 6px;
}

.item-summary {
  color: #909399;
  font-size: 13px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.chart-section {
  margin-bottom: 20px;
}

.chart-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease-out;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-header h3 {
  font-size: 18px;
  color: #303133;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-header h3 .el-icon {
  color: #667eea;
}

.chart-container {
  height: 400px;
  width: 100%;
}

:deep(.el-empty) {
  background: transparent;
  color: #909399;
}

:deep(.el-empty__description p) {
  color: #909399;
}

:deep(.el-radio-button__inner) {
  background: white;
  border-color: #e0e0e0;
  color: #606266;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

:deep(.el-scrollbar__view) {
  padding-right: 5px;
}

:deep(.el-scrollbar__bar) {
  background: rgba(0, 0, 0, 0.05);
}

:deep(.el-scrollbar__thumb) {
  background: rgba(102, 126, 234, 0.5);
  border-radius: 3px;
}

@media (max-width: 1024px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .welcome-card {
    flex-direction: column;
    text-align: center;
  }

  .welcome-title {
    font-size: 24px;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
