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
            <div style="padding: 12px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 8px; color: white;">
              <div style="font-weight: bold; margin-bottom: 8px; font-size: 14px;">${params.name}</div>
              <div style="font-size: 12px; margin-bottom: 4px;">📊 重要程度：${"⭐".repeat(params.data.importance || 0)}</div>
              <div style="font-size: 12px;">🎯 掌握程度：${getMasteryLevelText(params.data.masteryLevel)}</div>
            </div>
          `;
        } else {
          return `
            <div style="padding: 12px; background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); border-radius: 8px; color: white;">
              <div style="font-weight: bold; margin-bottom: 8px; font-size: 14px;">${params.data.label}</div>
              <div style="font-size: 12px;">💪 强度：${(params.data.strength * 100).toFixed(1)}%</div>
            </div>
          `;
        }
      },
      backgroundColor: "rgba(255, 255, 255, 0.95)",
      borderColor: "#eee",
      borderWidth: 2,
      textStyle: {
        color: "#333",
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
            borderWidth: 3,
            shadowBlur: 10,
            shadowColor: node.color,
          },
          importance: node.importance,
          masteryLevel: node.masteryLevel,
          label: {
            show: true,
            position: "bottom",
            fontSize: 13,
            fontWeight: "bold",
            color: "#2c3e50",
            textBorderColor: "#fff",
            textBorderWidth: 2,
          },
        })),
        links: graph.edges.map((edge) => ({
          source: edge.source,
          target: edge.target,
          label: {
            show: true,
            formatter: edge.label,
            fontSize: 11,
            fontWeight: "bold",
            color: "#606266",
            textBorderColor: "#fff",
            textBorderWidth: 1,
          },
          lineStyle: {
            width: edge.strength * 2 + 1,
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: "rgba(102, 126, 234, 0.8)" },
              { offset: 0.5, color: "rgba(118, 75, 162, 0.8)" },
              { offset: 1, color: "rgba(240, 147, 251, 0.8)" },
            ]),
            curveness: 0.3,
            shadowBlur: 5,
            shadowColor: "rgba(102, 126, 234, 0.5)",
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
          repulsion: 800,
          edgeLength: [150, 400],
          gravity: 0.03,
          friction: 0.6,
          layoutAnimation: true,
        },
        emphasis: {
          focus: "adjacency",
          lineStyle: {
            width: 6,
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: "#667eea" },
              { offset: 1, color: "#f093fb" },
            ]),
            shadowBlur: 10,
            shadowColor: "#667eea",
          },
          itemStyle: {
            shadowBlur: 20,
            shadowColor: "#667eea",
          },
        },
        blur: {
          itemStyle: {
            opacity: 0.3,
          },
          lineStyle: {
            opacity: 0.1,
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
  background:
    radial-gradient(
      ellipse at 20% 30%,
      rgba(102, 126, 234, 0.4) 0%,
      transparent 50%
    ),
    radial-gradient(
      ellipse at 80% 20%,
      rgba(118, 75, 162, 0.4) 0%,
      transparent 50%
    ),
    radial-gradient(
      ellipse at 40% 80%,
      rgba(240, 147, 251, 0.3) 0%,
      transparent 50%
    ),
    radial-gradient(
      ellipse at 60% 60%,
      rgba(102, 126, 234, 0.2) 0%,
      transparent 40%
    ),
    linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  z-index: 0;
}

.background-gradient::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    radial-gradient(
      2px 2px at 20px 30px,
      rgba(255, 255, 255, 0.3),
      transparent
    ),
    radial-gradient(
      2px 2px at 40px 70px,
      rgba(255, 255, 255, 0.2),
      transparent
    ),
    radial-gradient(
      1px 1px at 90px 40px,
      rgba(255, 255, 255, 0.3),
      transparent
    ),
    radial-gradient(
      2px 2px at 130px 80px,
      rgba(255, 255, 255, 0.2),
      transparent
    ),
    radial-gradient(
      1px 1px at 160px 120px,
      rgba(255, 255, 255, 0.4),
      transparent
    );
  background-size: 200px 200px;
  animation: twinkle 4s ease-in-out infinite;
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
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
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 30px;
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  animation: pulse 2s infinite;
  box-shadow:
    0 4px 20px rgba(102, 126, 234, 0.5),
    inset 0 2px 0 rgba(255, 255, 255, 0.3);
  position: relative;
  overflow: hidden;
}

.welcome-icon::before {
  content: "";
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    45deg,
    transparent 30%,
    rgba(255, 255, 255, 0.3) 50%,
    transparent 70%
  );
  animation: shine 3s infinite;
}

@keyframes shine {
  0% {
    transform: translateX(-100%) rotate(45deg);
  }
  100% {
    transform: translateX(100%) rotate(45deg);
  }
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.5);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 6px 30px rgba(102, 126, 234, 0.7);
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
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.3);
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
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.3);
  overflow: hidden;
}

.graph-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.1);
}

.graph-header h3 {
  margin: 0;
  font-size: 18px;
  color: white;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
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
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.1);
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.legend-color {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  display: inline-block;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.graph-body {
  padding: 20px;
}

.graph-container {
  width: 100%;
  height: 600px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  box-shadow: inset 0 2px 10px rgba(0, 0, 0, 0.1);
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
