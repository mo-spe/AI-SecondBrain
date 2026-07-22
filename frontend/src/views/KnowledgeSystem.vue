<template>
  <div class="knowledge-system-container">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">知识体系</h1>
        <p class="page-subtitle">可视化您的知识网络，构建完整的知识体系</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleAutoGenerate" :loading="autoGenerating">
          <el-icon><MagicStick /></el-icon>
          自动生成关系
        </el-button>
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="content-wrapper">
      <div class="left-panel">
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-icon purple">
              <el-icon size="20"><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ nodeCount }}</div>
              <div class="stat-label">知识点总数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon blue">
              <el-icon size="20"><Link /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ edgeCount }}</div>
              <div class="stat-label">连接关系</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon green">
              <el-icon size="20"><Folder /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ categoryCount }}</div>
              <div class="stat-label">主题领域</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon orange">
              <el-icon size="20"><MapLocation /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ pathCount }}</div>
              <div class="stat-label">学习路径</div>
            </div>
          </div>
        </div>

        <div class="knowledge-tree-card">
          <div class="tree-header">
            <h3 class="tree-title">知识体系目录</h3>
          </div>
          <div class="tree-search-box">
            <el-icon size="14" color="#94a3b8"><Search /></el-icon>
            <input
              type="text"
              v-model="searchQuery"
              placeholder="搜索知识..."
              class="tree-search-input"
            />
          </div>
          <div class="tree-body">
            <el-tree
              ref="treeRef"
              :data="treeData"
              :props="treeProps"
              :highlight-current="true"
              :default-expand-all="false"
              @node-click="handleNodeClick"
              class="knowledge-tree"
            >
              <template #default="{ node, data }">
                <div class="tree-node">
                  <el-icon :size="14" :color="data.color || '#64748b'">
                    <component :is="data.icon || Document" />
                  </el-icon>
                  <span class="node-label">{{ node.label }}</span>
                  <span v-if="data.count" class="node-count">{{ data.count }}</span>
                </div>
              </template>
            </el-tree>
          </div>
        </div>
      </div>

      <div class="middle-panel">
        <div class="graph-card">
          <div class="graph-header">
            <h3>
              <el-icon><Connection /></el-icon>
              知识网络图
            </h3>
            <div class="graph-actions">
              <button class="graph-action-btn" @click="handleResetView">
                <el-icon size="14"><ZoomIn /></el-icon>
                重置视图
              </button>
              <button class="graph-action-btn" @click="handleFitView">
                <el-icon size="14"><FullScreen /></el-icon>
                自适应
              </button>
            </div>
          </div>
          <div class="graph-body">
            <div ref="graphContainer" class="graph-container"></div>
          </div>
          <div class="graph-footer">
            <div class="legend">
              <div class="legend-item">
                <span class="legend-dot" style="background: #7c3aed;"></span>
                <span>核心主题</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background: #3b82f6;"></span>
                <span>重要主题</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background: #22c55e;"></span>
                <span>相关主题</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background: #f59e0b;"></span>
                <span>扩展主题</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background: #ef4444;"></span>
                <span>待学习</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="recommendations.length > 0" class="recommendation-card">
          <div class="recommendation-header">
            <h3>
              <el-icon><StarFilled /></el-icon>
              AI 智能关系推荐
            </h3>
            <el-button @click="closeRecommendations" size="small" text>
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
                      <el-tag size="small" type="success">{{ rec.recommendedTypeName }}</el-tag>
                      <span class="similarity">相似度：{{ (rec.similarity * 100).toFixed(1) }}%</span>
                    </div>
                    <div class="recommendation-target">{{ rec.targetKnowledge?.title || rec.targetKnowledgeTitle }}</div>
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

      <div class="right-panel">
        <div class="rag-card">
          <div class="rag-header">
            <div class="rag-title-area">
              <div class="rag-header-icon">
                <el-icon size="20" color="white"><ChatDotRound /></el-icon>
              </div>
              <h3 class="rag-title">RAG 知识体系</h3>
            </div>
            <el-button size="small" class="history-btn" @click="showHistory = !showHistory">
              历史记录
            </el-button>
          </div>

          <div class="rag-body">
            <div v-if="!showHistory" class="quick-questions">
              <p class="quick-title">基于您的知识推荐问题：</p>
              <div class="quick-list">
                <div
                  v-for="(q, index) in quickQuestions"
                  :key="index"
                  class="quick-item"
                  :class="{ active: question === q.text }"
                  @click="askQuestion(q.text)"
                >
                  <span class="question-dot"></span>
                  <span>{{ q.text }}</span>
                </div>
              </div>
            </div>

            <div v-if="!showHistory && chatHistory.length > 0" class="chat-history">
              <div
                v-for="(chat, index) in chatHistory"
                :key="index"
                class="chat-item"
                :class="{ user: chat.isUser }"
              >
                <div class="chat-avatar">
                  <el-avatar v-if="chat.isUser" :size="28" :src="userAvatar" />
                  <div v-else class="ai-avatar">
                    <el-icon size="16" color="white"><ChatLineRound /></el-icon>
                  </div>
                </div>
                <div class="chat-content">
                  <div class="chat-text">{{ chat.content }}</div>
                  <div v-if="chat.tags && chat.tags.length > 0" class="chat-tags">
                    <el-tag
                      v-for="(tag, tagIndex) in chat.tags"
                      :key="tagIndex"
                      size="small"
                      type="primary"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="!showHistory && currentAnswer" class="answer-section">
              <div class="answer-content" v-html="formattedAnswer"></div>
              <div v-if="currentAnswer.references && currentAnswer.references.length > 0" class="answer-references">
                <p class="references-title">参考来源</p>
                <div class="references-list">
                  <div
                    v-for="(ref, index) in currentAnswer.references"
                    :key="index"
                    class="reference-item"
                  >
                    <span class="ref-index">{{ index + 1 }}</span>
                    <span class="ref-title">{{ ref.title }}</span>
                    <span class="ref-similarity">{{ (ref.similarity * 100).toFixed(0) }}%</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="showHistory" class="history-panel">
              <div class="history-header">
                <h4>历史记录</h4>
                <el-button text size="small" @click="showHistory = false">
                  <el-icon><ArrowLeft /></el-icon>
                  返回
                </el-button>
              </div>
              <div v-for="(item, index) in historyList" :key="index" class="history-item">
                <div class="history-question">{{ item.question }}</div>
                <div class="history-meta">
                  <span class="history-time">{{ item.time }}</span>
                  <el-button size="small" text @click="askQuestion(item.question)">
                    重新提问
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <div class="rag-input-area">
            <div class="input-box">
              <textarea
                v-model="question"
                placeholder="输入知识问题..."
                rows="3"
                @keyup.ctrl.enter="handleAsk"
                class="question-input"
              ></textarea>
              <div class="input-actions">
                <div class="mode-switch">
                  <span class="mode-label" :class="{ active: !deepThink }">快速回答</span>
                  <el-switch v-model="deepThink" inline-prompt active-text="深" inactive-text="快" />
                  <span class="mode-label" :class="{ active: deepThink }">深度思考</span>
                </div>
                <el-button
                  type="primary"
                  @click="handleAsk"
                  :loading="loading"
                  class="send-btn"
                >
                  <el-icon><Promotion /></el-icon>
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from "vue";
import { ElMessage } from "element-plus";
import {
  Document,
  Notebook,
  FolderOpened,
  DocumentCopy,
  ChatDotRound,
  ChatLineRound,
  Promotion,
  ArrowLeft,
  Search,
  Connection,
  MagicStick,
  Refresh,
  ZoomIn,
  FullScreen,
  Link,
  Folder,
  MapLocation,
  StarFilled,
  Close,
  Check,
} from "@element-plus/icons-vue";
import request from "@/utils/request";
import * as echarts from "echarts";

const graphContainer = ref(null);
const treeRef = ref(null);
let chart = null;
let clickHandler = null;

const userAvatar = "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png";

const nodeCount = ref(253);
const edgeCount = ref(72);
const categoryCount = ref(12);
const pathCount = ref(8);

const searchQuery = ref("");
const autoGenerating = ref(false);
const loading = ref(false);
const showHistory = ref(false);
const deepThink = ref(false);
const question = ref("");
const currentAnswer = ref(null);
const recommendations = ref([]);
const currentKnowledgeId = ref(null);

const treeData = ref([
  {
    id: 1,
    label: "全部知识",
    count: 253,
    color: "#7c3aed",
    icon: Notebook,
    children: [
      { id: 11, label: "后端开发", count: 128, icon: DocumentCopy },
      { id: 12, label: "数据库", count: 45, icon: FolderOpened },
      { id: 13, label: "分布式系统", count: 32, icon: DocumentCopy },
      { id: 14, label: "微服务架构", count: 28, icon: DocumentCopy },
      { id: 15, label: "前端开发", count: 20, icon: DocumentCopy },
    ],
  },
  {
    id: 2,
    label: "Java 核心技术",
    count: 45,
    color: "#3b82f6",
    icon: Notebook,
    children: [
      { id: 21, label: "Spring Boot", count: 12 },
      { id: 22, label: "JVM", count: 10 },
      { id: 23, label: "多线程", count: 9 },
      { id: 24, label: "设计模式", count: 8 },
      { id: 25, label: "集合框架", count: 6 },
    ],
  },
  {
    id: 3,
    label: "数据库系统",
    count: 32,
    color: "#22c55e",
    icon: FolderOpened,
    children: [
      { id: 31, label: "MySQL", count: 15 },
      { id: 32, label: "Redis", count: 8 },
      { id: 33, label: "MongoDB", count: 5 },
      { id: 34, label: "PostgreSQL", count: 4 },
    ],
  },
  {
    id: 4,
    label: "分布式系统",
    count: 28,
    color: "#f59e0b",
    icon: DocumentCopy,
    children: [
      { id: 41, label: "消息队列", count: 10 },
      { id: 42, label: "分布式缓存", count: 8 },
      { id: 43, label: "分布式事务", count: 6 },
      { id: 44, label: "服务治理", count: 4 },
    ],
  },
]);

const treeProps = {
  label: "label",
  children: "children",
};

const quickQuestions = [
  { text: "我可以从哪些知识点开始学习？" },
  { text: "什么是 Spring Boot 自动配置？" },
  { text: "如何设计一个高并发的系统？" },
  { text: "Redis 的持久化机制有哪些？" },
];

const chatHistory = ref([
  {
    isUser: true,
    content: "Spring Boot 自动配置原理是什么？",
  },
]);

const historyList = ref([
  { question: "什么是微服务架构？", time: "2024-01-15 14:30" },
  { question: "MySQL 索引优化策略", time: "2024-01-14 10:20" },
  { question: "Java 并发编程最佳实践", time: "2024-01-13 16:45" },
]);

const formattedAnswer = computed(() => {
  if (!currentAnswer.value) return "";
  return currentAnswer.value.answer.replace(/\n/g, "<br>");
});

const initGraph = async () => {
  try {
    const graph = await request.get("/knowledge/relation/graph");
    nodeCount.value = graph.nodes.length;
    edgeCount.value = graph.edges.length;
    renderGraph(graph);
  } catch (error) {
    renderMockGraph();
  }
};

const renderMockGraph = () => {
  const nodes = [
    { id: 1, label: "Spring Boot 核心技术", size: 55, color: "#f87171", importance: 5, masteryLevel: 5 },
    { id: 2, label: "JVM 运行时数据区", size: 48, color: "#f87171", importance: 4, masteryLevel: 4 },
    { id: 3, label: "Java 并发编程核心方法", size: 45, color: "#fb923c", importance: 4, masteryLevel: 3 },
    { id: 4, label: "Redis 缓存", size: 42, color: "#60a5fa", importance: 3, masteryLevel: 3 },
    { id: 5, label: "JVM", size: 40, color: "#60a5fa", importance: 3, masteryLevel: 4 },
    { id: 6, label: "垃圾回收机制", size: 38, color: "#4ade80", importance: 3, masteryLevel: 2 },
    { id: 7, label: "线程池与并发容器", size: 36, color: "#4ade80", importance: 3, masteryLevel: 3 },
    { id: 8, label: "MySQL 优化", size: 34, color: "#60a5fa", importance: 3, masteryLevel: 2 },
    { id: 9, label: "微服务架构", size: 44, color: "#fb923c", importance: 4, masteryLevel: 3 },
    { id: 10, label: "消息队列", size: 32, color: "#4ade80", importance: 2, masteryLevel: 2 },
    { id: 11, label: "分布式缓存", size: 30, color: "#4ade80", importance: 2, masteryLevel: 2 },
    { id: 12, label: "API 网关", size: 28, color: "#60a5fa", importance: 2, masteryLevel: 1 },
    { id: 13, label: "Python 基础", size: 30, color: "#a78bfa", importance: 2, masteryLevel: 1 },
    { id: 14, label: "Python 数据结构", size: 26, color: "#a78bfa", importance: 2, masteryLevel: 0 },
    { id: 15, label: "Python 装饰器", size: 24, color: "#a78bfa", importance: 2, masteryLevel: 1 },
    { id: 16, label: "进程管理", size: 22, color: "#60a5fa", importance: 2, masteryLevel: 2 },
    { id: 17, label: "信息通信方式", size: 20, color: "#f87171", importance: 2, masteryLevel: 1 },
  ];

  const edges = [
    { source: 1, target: 2, label: "依赖", strength: 0.9 },
    { source: 1, target: 3, label: "应用", strength: 0.85 },
    { source: 1, target: 4, label: "集成", strength: 0.8 },
    { source: 1, target: 9, label: "架构", strength: 0.75 },
    { source: 2, target: 5, label: "基础", strength: 0.9 },
    { source: 2, target: 6, label: "机制", strength: 0.85 },
    { source: 3, target: 7, label: "组件", strength: 0.8 },
    { source: 5, target: 6, label: "基础", strength: 0.75 },
    { source: 4, target: 8, label: "配合", strength: 0.8 },
    { source: 9, target: 10, label: "组件", strength: 0.8 },
    { source: 9, target: 11, label: "组件", strength: 0.75 },
    { source: 9, target: 12, label: "组件", strength: 0.7 },
    { source: 13, target: 14, label: "基础", strength: 0.85 },
    { source: 13, target: 15, label: "特性", strength: 0.75 },
    { source: 16, target: 17, label: "机制", strength: 0.7 },
  ];

  renderGraph({ nodes, edges });
};

const renderGraph = (graph) => {
  if (!chart) {
    chart = echarts.init(graphContainer.value);
  }
  setTimeout(() => {
    if (chart) chart.resize();
  }, 100);

  const option = {
    backgroundColor: "#1a1a2e",
    tooltip: {
      backgroundColor: "rgba(26, 26, 46, 0.95)",
      borderColor: "#7c3aed",
      borderWidth: 1,
      textStyle: { color: "#fff" },
      formatter: function (params) {
        if (params.dataType === "node") {
          return `
            <div style="padding: 12px; background: rgba(26,26,46,0.95); border-radius: 8px; border: 1px solid #7c3aed;">
              <div style="font-weight: bold; margin-bottom: 8px; font-size: 14px; color: #fff;">${params.name}</div>
              <div style="font-size: 12px; margin-bottom: 4px; color: #a78bfa;">重要程度：${"⭐".repeat(params.data.importance || 0)}</div>
              <div style="font-size: 12px; color: #a78bfa;">掌握程度：${getMasteryLevelText(params.data.masteryLevel)}</div>
            </div>
          `;
        } else {
          return `
            <div style="padding: 8px 12px; background: rgba(26,26,46,0.95); border-radius: 6px; border: 1px solid #7c3aed;">
              <div style="font-weight: bold; font-size: 13px; color: #fff;">${params.data.label}</div>
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
            shadowBlur: 20,
            shadowColor: node.color,
          },
          importance: node.importance,
          masteryLevel: node.masteryLevel,
          label: {
            show: true,
            position: "bottom",
            fontSize: 11,
            fontWeight: 500,
            color: "#e2e8f0",
            textBorderColor: "transparent",
            textBorderWidth: 0,
          },
        })),
        links: graph.edges.map((edge) => ({
          source: edge.source,
          target: edge.target,
          label: {
            show: true,
            formatter: edge.label,
            fontSize: 9,
            fontWeight: 500,
            color: "#94a3b8",
          },
          lineStyle: {
            width: edge.strength * 1.5 + 0.5,
            color: "rgba(148, 163, 184, 0.4)",
            curveness: 0.2,
          },
          strength: edge.strength,
        })),
        roam: true,
        force: {
          repulsion: 500,
          edgeLength: [100, 250],
          gravity: 0.05,
          friction: 0.6,
          layoutAnimation: true,
        },
        emphasis: {
          focus: "adjacency",
          lineStyle: {
            width: 3,
            color: "#a855f7",
          },
          itemStyle: {
            shadowBlur: 30,
            shadowColor: "#a855f7",
          },
        },
        blur: {
          itemStyle: { opacity: 0.2 },
          lineStyle: { opacity: 0.05 },
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

const handleNodeClick = (data) => {
  if (data.id) {
    showRecommendations(data.id);
  }
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

const handleResetView = () => {
  if (chart) {
    chart.dispatchAction({ type: "restore" });
  }
};

const handleFitView = () => {
  if (chart) {
    chart.resize();
    const option = chart.getOption();
    if (option.series && option.series[0]) {
      option.series[0].force = {
        repulsion: 500,
        edgeLength: [100, 250],
        gravity: 0.05,
        friction: 0.6,
        layoutAnimation: true,
      };
      chart.setOption(option);
    }
  }
};

const showRecommendations = async (knowledgeId) => {
  try {
    currentKnowledgeId.value = knowledgeId;
    const data = await request.get(`/knowledge/relation/recommend/${knowledgeId}`);
    recommendations.value = data || [];
    if (recommendations.value.length > 0) {
      ElMessage.success(`为您推荐了 ${recommendations.value.length} 个知识关系`);
    } else {
      ElMessage.info("暂无推荐关系");
    }
  } catch (error) {
    ElMessage.error("获取推荐失败：" + error.message);
  }
};

const handleAcceptRecommendation = async (recommendation) => {
  try {
    await request.post("/knowledge/relation", {
      sourceId: currentKnowledgeId.value,
      targetId: recommendation.targetKnowledge?.id || recommendation.targetKnowledgeId,
      relationType: recommendation.recommendedType,
      relationName: recommendation.recommendedTypeName,
      relationStrength: Math.round(recommendation.similarity * 5),
    });
    ElMessage.success("关系创建成功");
    await initGraph();
    recommendations.value = recommendations.value.filter((r) => r !== recommendation);
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

const askQuestion = (text) => {
  question.value = text;
  handleAsk();
};

const handleAsk = async () => {
  if (!question.value.trim()) {
    ElMessage.warning("请输入问题");
    return;
  }

  loading.value = true;
  chatHistory.value.push({ isUser: true, content: question.value });

  try {
    const result = await request.post("/rag/answer", {
      question: question.value,
      topK: 3,
      includeReferences: true,
      deepThink: deepThink.value,
    });

    currentAnswer.value = result;
    chatHistory.value.push({
      isUser: false,
      content: result.answer,
      tags: (result.references || []).map((r) => r.title).slice(0, 3),
    });

    ElMessage.success("回答完成");
  } catch (error) {
    console.error("回答失败:", error);
    ElMessage.error("回答失败：" + (error.message || "未知错误"));
  } finally {
    loading.value = false;
    question.value = "";
  }
};

onMounted(async () => {
  await nextTick();
  if (graphContainer.value) {
    initGraph();
  }
  window.addEventListener("resize", () => {
    if (chart) chart.resize();
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
.knowledge-system-container {
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left .page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.header-left .page-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.content-wrapper {
  display: grid;
  grid-template-columns: 260px 1fr 380px;
  gap: 24px;
  min-height: calc(100vh - 180px);
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-icon.purple {
  background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 100%);
}

.stat-icon.blue {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
}

.stat-icon.green {
  background: linear-gradient(135deg, #22c55e 0%, #4ade80 100%);
}

.stat-icon.orange {
  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.knowledge-tree-card {
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tree-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-lighter);
}

.tree-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.tree-search-box {
  margin: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-input);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}

.tree-search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-primary);
  outline: none;
}

.tree-search-input::placeholder {
  color: var(--text-placeholder);
}

.tree-body {
  flex: 1;
  padding: 0 8px 12px;
  overflow: auto;
}

.knowledge-tree {
  --el-tree-node-hover-bg-color: rgba(124, 58, 237, 0.06);
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.node-label {
  flex: 1;
  font-size: 13px;
  color: var(--text-regular);
}

.node-count {
  font-size: 11px;
  color: var(--text-secondary);
  background: var(--bg-input);
  padding: 2px 6px;
  border-radius: 10px;
}

.middle-panel {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.graph-card {
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.graph-header {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-lighter);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.graph-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.graph-actions {
  display: flex;
  gap: 8px;
}

.graph-action-btn {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  background: white;
  cursor: pointer;
  color: var(--text-regular);
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all var(--transition-base);
}

.graph-action-btn:hover {
  background: var(--bg-hover);
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.graph-body {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.graph-container {
  width: 100%;
  flex: 1;
  min-height: 500px;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.graph-footer {
  padding: 12px 20px;
  border-top: 1px solid var(--border-lighter);
  background: var(--bg-page);
}

.legend {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-regular);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.recommendation-card {
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
  overflow: hidden;
  margin-top: 16px;
}

.recommendation-header {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-lighter);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.05) 0%, rgba(168, 85, 247, 0.05) 100%);
}

.recommendation-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.recommendation-body {
  padding: 16px;
}

.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommendation-item {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 12px;
  background: var(--bg-page);
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
  margin-bottom: 6px;
}

.similarity {
  font-size: 11px;
  color: var(--text-secondary);
}

.recommendation-target {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.recommendation-actions {
  display: flex;
  gap: 8px;
}

.right-panel {
  display: flex;
  flex-direction: column;
}

.rag-card {
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.rag-header {
  background: linear-gradient(135deg, #7c3aed 0%, #a855f7 100%);
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rag-title-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rag-header-icon {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.rag-title {
  font-size: 16px;
  font-weight: 600;
  color: white;
  margin: 0;
}

.history-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
}

.history-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  color: white;
}

.rag-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.quick-questions {
  margin-bottom: 16px;
}

.quick-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-regular);
  margin: 0 0 12px 0;
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-item {
  padding: 10px 12px;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  color: var(--text-regular);
  display: flex;
  align-items: flex-start;
  gap: 8px;
  transition: all var(--transition-base);
  border: 1px solid transparent;
}

.quick-item:hover,
.quick-item.active {
  background: rgba(124, 58, 237, 0.08);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.question-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  margin-top: 7px;
  flex-shrink: 0;
}

.chat-history {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
}

.chat-item {
  display: flex;
  gap: 10px;
}

.chat-item.user {
  flex-direction: row-reverse;
}

.chat-item.user .chat-content {
  background: var(--color-primary);
  color: white;
  border-radius: 12px 12px 4px 12px;
}

.chat-item:not(.user) .chat-content {
  background: var(--bg-page);
  color: var(--text-primary);
  border-radius: 12px 12px 12px 4px;
}

.chat-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #22c55e 0%, #4ade80 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-content {
  flex: 1;
  padding: 10px 14px;
  max-width: calc(100% - 40px);
}

.chat-text {
  font-size: 13px;
  line-height: 1.6;
}

.chat-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.answer-section {
  background: var(--bg-page);
  border-radius: var(--radius-lg);
  padding: 16px;
  margin-bottom: 16px;
}

.answer-content {
  padding: 14px;
  background: white;
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-regular);
  margin-bottom: 12px;
}

.answer-references {
  border-top: 1px solid var(--border-lighter);
  padding-top: 12px;
}

.references-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin: 0 0 8px 0;
}

.references-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.reference-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: white;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-lighter);
}

.ref-index {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  background: var(--color-primary);
  color: white;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ref-title {
  font-size: 12px;
  color: var(--text-regular);
  flex: 1;
}

.ref-similarity {
  font-size: 11px;
  color: var(--text-secondary);
}

.history-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-header h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.history-item {
  padding: 12px;
  background: var(--bg-page);
  border-radius: var(--radius-md);
}

.history-question {
  font-size: 13px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-time {
  font-size: 11px;
  color: var(--text-secondary);
}

.rag-input-area {
  padding: 16px;
  border-top: 1px solid var(--border-lighter);
  background: white;
}

.input-box {
  background: var(--bg-page);
  border-radius: var(--radius-lg);
  padding: 12px;
}

.question-input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-primary);
  outline: none;
  resize: vertical;
  font-family: inherit;
  margin-bottom: 12px;
}

.question-input::placeholder {
  color: var(--text-placeholder);
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mode-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-label {
  font-size: 12px;
  color: var(--text-muted);
}

.mode-label.active {
  color: var(--color-primary);
  font-weight: 500;
}

.send-btn {
  padding: 8px 20px;
  border-radius: var(--radius-md);
  background: var(--gradient-primary);
  border: none;
}

@media (max-width: 1400px) {
  .content-wrapper {
    grid-template-columns: 240px 1fr 340px;
    gap: 16px;
  }
}

@media (max-width: 1200px) {
  .content-wrapper {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .middle-panel {
    order: -1;
    min-height: 500px;
  }

  .graph-container {
    min-height: 400px;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>