<template>
  <div class="knowledge-graph-container">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><Share /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">知识图谱</h1>
            <p class="welcome-subtitle">可视化您的知识体系</p>
          </div>
        </div>
      </div>

      <div class="toolbar-section">
        <div class="toolbar-card">
          <div class="toolbar-left">
            <el-button
              type="primary"
              @click="handleAutoGenerate"
              :loading="autoGenerating"
            >
              <el-icon><MagicStick /></el-icon>
              自动生成关系
            </el-button>
            <el-button @click="handleRefresh">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
          <div class="toolbar-right">
            <el-tag type="info">节点：{{ nodeCount }}</el-tag>
            <el-tag type="success">关系：{{ edgeCount }}</el-tag>
          </div>
        </div>
      </div>

      <div class="recommendation-section" v-if="recommendations.length > 0">
        <div class="recommendation-card">
          <div class="recommendation-header">
            <h3>
              <el-icon><Star /></el-icon>
              AI 智能关系推荐
            </h3>
            <el-button @click="closeRecommendations" size="small">
              <el-icon><Close /></el-icon>
              关闭
            </el-button>
          </div>
          <div class="recommendation-body">
            <div class="recommendation-list">
              <div
                v-for="(rec, index) in recommendations"
                :key="index"
                class="recommendation-item"
              >
                <div class="recommendation-content">
                  <div class="recommendation-info">
                    <div class="recommendation-title">
                      <el-tag size="small" type="success">{{
                        rec.recommendedTypeName
                      }}</el-tag>
                      <span class="similarity"
                        >相似度：{{ (rec.similarity * 100).toFixed(1) }}%</span
                      >
                    </div>
                    <div class="recommendation-target">
                      {{ rec.targetKnowledge.title }}
                    </div>
                  </div>
                  <div class="recommendation-actions">
                    <el-button
                      type="primary"
                      size="small"
                      @click="handleAcceptRecommendation(rec)"
                    >
                      <el-icon><Check /></el-icon>
                      接受
                    </el-button>
                    <el-button
                      size="small"
                      @click="handleRejectRecommendation(index)"
                    >
                      <el-icon><Close /></el-icon>
                      拒绝
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="graph-section">
        <div class="graph-card">
          <div class="graph-header">
            <h3>
              <el-icon><Connection /></el-icon>
              知识网络图
            </h3>
            <div class="legend">
              <div class="legend-item">
                <span
                  class="legend-color"
                  style="background-color: #67c23a"
                ></span>
                <span>专家</span>
              </div>
              <div class="legend-item">
                <span
                  class="legend-color"
                  style="background-color: #95d475"
                ></span>
                <span>精通</span>
              </div>
              <div class="legend-item">
                <span
                  class="legend-color"
                  style="background-color: #e6a23c"
                ></span>
                <span>掌握</span>
              </div>
              <div class="legend-item">
                <span
                  class="legend-color"
                  style="background-color: #f56c6c"
                ></span>
                <span>熟悉</span>
              </div>
              <div class="legend-item">
                <span
                  class="legend-color"
                  style="background-color: #909399"
                ></span>
                <span>未掌握</span>
              </div>
            </div>
          </div>
          <div class="graph-body">
            <div ref="graphContainer" class="graph-container"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { ElMessage } from "element-plus";
import {
  Share,
  MagicStick,
  Refresh,
  Connection,
  Star,
  Close,
  Check,
} from "@element-plus/icons-vue";
import request from "@/utils/request";
import * as echarts from "echarts";

const graphContainer = ref(null);
let chart = null;
let clickHandler = null;
const autoGenerating = ref(false);
const nodeCount = ref(0);
const edgeCount = ref(0);
const recommendations = ref([]);
const currentKnowledgeId = ref(null);

const initGraph = async () => {
  try {
    const graph = await request.get("/knowledge/relation/graph");
    nodeCount.value = graph.nodes.length;
    edgeCount.value = graph.edges.length;
    renderGraph(graph);
  } catch (error) {
    ElMessage.error("获取知识图谱失败：" + error.message);
  }
};

const renderGraph = (graph) => {
  if (!chart) {
    chart = echarts.init(graphContainer.value);
  }

  const option = {
    animation: false,
    animationDuration: 0,
    animationDurationUpdate: 0,
    animationEasing: "linear",
    animationEasingUpdate: "linear",
    title: {
      text: "知识网络图",
      top: 10,
      left: 10,
      textStyle: {
        fontSize: 16,
        fontWeight: "bold",
        color: "#2c3e50",
      },
    },
    tooltip: {
      formatter: function (params) {
        if (params.dataType === "node") {
          return `
            <div style="padding: 8px;">
              <div style="font-weight: bold; margin-bottom: 5px;">${params.name}</div>
              <div>重要程度：${params.data.importance || 0}</div>
              <div>掌握程度：${getMasteryLevelText(params.data.masteryLevel)}</div>
            </div>
          `;
        } else {
          return `
            <div style="padding: 8px;">
              <div style="font-weight: bold; margin-bottom: 5px;">${params.data.label}</div>
              <div>强度：${params.data.strength}</div>
            </div>
          `;
        }
      },
    },
    series: [
      {
        type: "graph",
        layout: "force",
        draggable: true,
        data: graph.nodes.map((node) => ({
          id: node.id,
          name: node.label,
          symbolSize: node.size,
          itemStyle: {
            color: node.color,
            borderColor: "#fff",
            borderWidth: 2,
          },
          importance: node.importance,
          masteryLevel: node.masteryLevel,
          label: {
            show: true,
            position: "bottom",
            fontSize: 12,
            color: "#2c3e50",
          },
        })),
        links: graph.edges.map((edge) => ({
          source: edge.source,
          target: edge.target,
          label: {
            show: true,
            formatter: edge.label,
            fontSize: 10,
            color: "#606266",
          },
          lineStyle: {
            width: edge.strength,
            color: "#909399",
            curveness: 0.2,
          },
          strength: edge.strength,
        })),
        roam: true,
        label: {
          show: true,
          position: "right",
          formatter: "{b}",
        },
        force: {
          repulsion: 500,
          edgeLength: [100, 300],
          gravity: 0.05,
          friction: 0.6,
          layoutAnimation: false,
        },
        emphasis: {
          focus: "adjacency",
          lineStyle: {
            width: 4,
            color: "#409EFF",
          },
        },
      },
    ],
  };

  chart.setOption(option, true);

  if (clickHandler) {
    chart.off("click", clickHandler);
  }

  clickHandler = function (params) {
    if (params.dataType === "node") {
      showRecommendations(params.data.id);
    }
  };

  chart.on("click", clickHandler);
};

const getMasteryLevelText = (level) => {
  const levels = {
    5: "专家",
    4: "精通",
    3: "掌握",
    2: "熟悉",
    1: "入门",
    0: "未掌握",
  };
  return levels[level] || "未知";
};

const handleAutoGenerate = async () => {
  autoGenerating.value = true;
  try {
    await request.post("/knowledge/relation/auto-generate");
    ElMessage.success("自动生成完成");
    await initGraph();
  } catch (error) {
    ElMessage.error("自动生成失败：" + error.message);
  } finally {
    autoGenerating.value = false;
  }
};

const handleRefresh = () => {
  initGraph();
};

const handleAcceptRecommendation = async (recommendation) => {
  try {
    await request.post("/knowledge/relation", {
      sourceId: currentKnowledgeId.value,
      targetId: recommendation.targetKnowledge.id,
      relationType: recommendation.recommendedType,
      relationName: recommendation.recommendedTypeName,
      relationStrength: Math.round(recommendation.similarity * 5),
    });
    ElMessage.success("关系创建成功");
    await initGraph();
    recommendations.value = recommendations.value.filter(
      (r) => r !== recommendation,
    );
  } catch (error) {
    ElMessage.error("关系创建失败：" + error.message);
  }
};

const handleRejectRecommendation = (index) => {
  recommendations.value.splice(index, 1);
  ElMessage.info("已拒绝推荐");
};

const closeRecommendations = () => {
  recommendations.value = [];
};

const showRecommendations = async (knowledgeId) => {
  try {
    currentKnowledgeId.value = knowledgeId;
    const data = await request.get(
      `/knowledge/relation/recommend/${knowledgeId}`,
    );
    recommendations.value = data || [];
    if (recommendations.value.length > 0) {
      ElMessage.success(
        `为您推荐了 ${recommendations.value.length} 个知识关系`,
      );
    } else {
      ElMessage.info("暂无推荐关系");
    }
  } catch (error) {
    ElMessage.error("获取推荐失败：" + error.message);
  }
};

onMounted(() => {
  initGraph();
  window.addEventListener("resize", () => {
    if (chart) {
      chart.resize();
    }
  });
});

onUnmounted(() => {
  if (chart) {
    if (clickHandler) {
      chart.off("click", clickHandler);
    }
    chart.dispose();
  }
});
</script>

<style scoped>
.knowledge-graph-container {
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
  margin: 0;
  font-size: 28px;
  color: white;
  font-weight: 600;
}

.welcome-subtitle {
  margin: 8px 0 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.toolbar-section {
  margin-bottom: 20px;
}

.toolbar-card {
  background: white;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
}

.graph-section {
  margin-bottom: 20px;
}

.graph-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.graph-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.graph-header h3 {
  margin: 0;
  font-size: 18px;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend {
  display: flex;
  gap: 15px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #606266;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}

.graph-body {
  padding: 20px;
}

.graph-container {
  width: 100%;
  height: 600px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background-color: #fafafa;
}

.recommendation-section {
  margin-bottom: 20px;
}

.recommendation-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.recommendation-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.recommendation-header h3 {
  margin: 0;
  font-size: 16px;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.recommendation-body {
  padding: 16px 20px;
}

.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommendation-item {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}

.recommendation-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.recommendation-info {
  flex: 1;
}

.recommendation-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.similarity {
  font-size: 12px;
  color: #909399;
}

.recommendation-target {
  font-size: 14px;
  color: #2c3e50;
  font-weight: 500;
}

.recommendation-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 768px) {
  .knowledge-graph-container {
    padding: 20px;
  }

  .page-title {
    font-size: 24px;
  }

  .toolbar-card {
    flex-direction: column;
    gap: 16px;
  }

  .toolbar-left,
  .toolbar-right {
    width: 100%;
    justify-content: center;
  }

  .graph-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .legend {
    flex-wrap: wrap;
    gap: 10px;
  }

  .graph-container {
    height: 400px;
  }
}
</style>
