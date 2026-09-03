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

    <div class="content-wrapper" :style="{ '--rag-panel-width': ragPanelCollapsed ? '44px' : ragPanelWidth + 'px' }">
      <div class="left-panel">
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-icon purple"></div>
            <div class="stat-content">
              <div class="stat-value">{{ nodeCount }}</div>
              <div class="stat-label">知识点总数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon blue"></div>
            <div class="stat-content">
              <div class="stat-value">{{ edgeCount }}</div>
              <div class="stat-label">连接关系</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon green"></div>
            <div class="stat-content">
              <div class="stat-value">{{ categoryCount }}</div>
              <div class="stat-label">主题领域</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon orange"></div>
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

      <div class="right-panel" :class="{ collapsed: ragPanelCollapsed, dragging: isDragging }">
        <div
          class="resize-handle"
          :class="{ dragging: isDragging, collapsed: ragPanelCollapsed }"
          @mousedown="onResizeStart"
        >
          <div class="resize-grip">
            <span></span><span></span><span></span>
          </div>
        </div>
        <div v-if="ragPanelCollapsed" class="collapsed-tab" @click="toggleRagPanel" title="展开RAG问答">
          <el-icon size="18"><ChatDotRound /></el-icon>
          <span class="collapsed-label">RAG</span>
        </div>
        <div class="rag-card" v-show="!ragPanelCollapsed">
          <div class="rag-header">
            <div class="rag-title-area">
              <div class="rag-header-icon">
                <el-icon size="20" color="white"><ChatDotRound /></el-icon>
              </div>
              <h3 class="rag-title">RAG 知识问答</h3>
            </div>
            <div class="rag-header-actions">
              <el-button
                size="small"
                class="history-btn"
                aria-label="查看历史会话"
                title="查看历史会话"
                @click="showSessionList = !showSessionList"
              >
                <el-icon size="14"><Clock /></el-icon>
              </el-button>
              <el-button size="small" class="history-btn" @click="handleNewSession" :disabled="isStreaming">
                新对话
              </el-button>
              <el-button size="small" class="collapse-btn" @click="toggleRagPanel" title="收起面板">
                <el-icon size="14"><ArrowLeft /></el-icon>
              </el-button>
            </div>
          </div>

          <div v-if="showSessionList" class="session-list-panel">
            <div class="session-list-header">
              <span>历史会话</span>
              <el-button text size="small" @click="showSessionList = false">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
            <div class="session-list-body">
              <div
                v-for="s in sessions"
                :key="s.id"
                class="session-item"
                :class="{ active: s.id === currentSessionId }"
                @click="switchSession(s.id)"
              >
                <div class="session-title">{{ s.title }}</div>
                <div class="session-time">{{ formatTime(s.updateTime || s.createTime) }}</div>
                <div class="session-actions">
                  <el-button
                    text
                    size="small"
                    class="session-action"
                    aria-label="重命名会话"
                    title="重命名"
                    @click.stop="handleRenameSession(s)"
                  >
                    <el-icon size="13"><EditPen /></el-icon>
                  </el-button>
                  <el-button
                    text
                    size="small"
                    class="session-action session-delete"
                    aria-label="删除会话"
                    title="删除"
                    @click.stop="handleDeleteSession(s.id)"
                  >
                    <el-icon size="12"><Close /></el-icon>
                  </el-button>
                </div>
              </div>
              <div v-if="sessions.length === 0" class="session-empty">暂无历史会话</div>
            </div>
          </div>

          <div class="rag-body" ref="ragBodyRef">
            <div v-if="messages.length === 0 && !isStreaming" class="quick-questions">
              <p class="quick-title">基于您的知识推荐问题：</p>
              <div class="quick-list">
                <div
                  v-for="(q, index) in quickQuestions"
                  :key="index"
                  class="quick-item"
                  @click="askQuestion(q.text)"
                >
                  <span class="question-dot"></span>
                  <span>{{ q.text }}</span>
                </div>
              </div>
            </div>

            <div v-for="(msg, index) in messages" :key="index" class="chat-message" :class="msg.role">
              <div class="chat-avatar">
                <el-avatar v-if="msg.role === 'user'" :size="28" :src="userAvatar" />
                <div v-else class="ai-avatar">
                  <el-icon size="16" color="white"><ChatLineRound /></el-icon>
                </div>
              </div>
              <div class="chat-content">
                <div class="chat-text">{{ msg.content }}</div>
                <div v-if="msg.role === 'assistant' && msg.references && msg.references.length > 0" class="chat-references">
                  <div class="ref-label">参考来源</div>
                  <div class="ref-list">
                    <span
                      v-for="(ref, rIdx) in msg.references"
                      :key="rIdx"
                      class="ref-chip"
                    >
                      {{ ref.title }}
                      <span class="ref-score">{{ (ref.similarity * 100).toFixed(0) }}%</span>
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="isStreaming && streamingContent !== null" class="chat-message assistant streaming">
              <div class="chat-avatar">
                <div class="ai-avatar">
                  <el-icon size="16" color="white"><ChatLineRound /></el-icon>
                </div>
              </div>
              <div class="chat-content">
                <div class="chat-text">{{ streamingContent }}<span class="streaming-cursor">|</span></div>
                <div v-if="streamingReferences.length > 0" class="chat-references">
                  <div class="ref-label">参考来源</div>
                  <div class="ref-list">
                    <span v-for="(ref, rIdx) in streamingReferences" :key="rIdx" class="ref-chip">
                      {{ ref.title }}
                      <span class="ref-score">{{ (ref.similarity * 100).toFixed(0) }}%</span>
                    </span>
                  </div>
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
                  :loading="loading && !isStreaming"
                  :disabled="isStreaming"
                  class="send-btn"
                >
                  <el-icon><Promotion /></el-icon>
                  发送
                </el-button>
                <el-button
                  v-if="isStreaming"
                  type="danger"
                  @click="handleStop"
                  class="send-btn"
                >
                  <el-icon><Close /></el-icon>
                  停止
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
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
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
  Clock,
  EditPen,
} from "@element-plus/icons-vue";
import request from "@/utils/request";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import * as echarts from "echarts";

const router = useRouter();
const userStore = useUserStore();

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
const isStreaming = ref(false);
const abortController = ref(null);
const showSessionList = ref(false);
const deepThink = ref(false);
const question = ref("");
const messages = ref([]);
const streamingContent = ref(null);
const streamingReferences = ref([]);
const currentSessionId = ref(null);
const sessions = ref([]);
const ragBodyRef = ref(null);
const recommendations = ref([]);
const currentKnowledgeId = ref(null);

const ragPanelCollapsed = ref(false);
const ragPanelWidth = ref(Number(localStorage.getItem('ragPanelWidth')) || 380);
const isDragging = ref(false);
const MIN_PANEL_WIDTH = 320;
const MAX_PANEL_WIDTH = 700;

const onResizeStart = (e) => {
  isDragging.value = true;
  const startX = e.clientX;
  const startWidth = ragPanelWidth.value;

  const onMouseMove = (moveEvent) => {
    const delta = startX - moveEvent.clientX;
    const newWidth = Math.min(MAX_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, startWidth + delta));
    ragPanelWidth.value = newWidth;
  };

  const onMouseUp = () => {
    isDragging.value = false;
    localStorage.setItem('ragPanelWidth', ragPanelWidth.value);
    document.removeEventListener('mousemove', onMouseMove);
    document.removeEventListener('mouseup', onMouseUp);
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
  };

  document.addEventListener('mousemove', onMouseMove);
  document.addEventListener('mouseup', onMouseUp);
  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';
  e.preventDefault();
};

const toggleRagPanel = () => {
  ragPanelCollapsed.value = !ragPanelCollapsed.value;
};

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

const initSession = async () => {
  try {
    const res = await request.get("/sessions", { current: 1, size: 50 });
    if (res && res.records) {
      sessions.value = res.records;
      if (res.records.length > 0) {
        const lastSession = res.records[0];
        currentSessionId.value = lastSession.id;
        await loadMessages(lastSession.id);
        return;
      }
    }
    await createNewSession();
  } catch (e) {
    console.warn("加载会话失败，创建新会话", e);
    await createNewSession();
  }
};

const createNewSession = async () => {
  const res = await request.post("/sessions", { title: "新对话" });
  if (res && res.id) {
    currentSessionId.value = res.id;
    messages.value = [];
    sessions.value.unshift(res);
  }
};

const loadSessions = async () => {
  try {
    const res = await request.get("/sessions", { current: 1, size: 50 });
    if (res && res.records) {
      sessions.value = res.records;
    }
  } catch (e) {
    console.warn("加载会话列表失败", e);
  }
};

const loadMessages = async (sessionId) => {
  try {
    const res = await request.get(`/sessions/${sessionId}/messages`, { current: 1, size: 200 });
    if (res && res.records) {
      const sorted = [...res.records].sort((a, b) => {
        return new Date(a.createTime) - new Date(b.createTime);
      });
      messages.value = sorted.map((m) => ({
        role: m.role,
        content: m.content,
        references: [],
      }));
      scrollToBottom();
    }
  } catch (e) {
    console.warn("加载消息失败", e);
  }
};

const switchSession = async (sessionId) => {
  if (sessionId === currentSessionId.value || isStreaming.value) return;
  currentSessionId.value = sessionId;
  await loadMessages(sessionId);
  showSessionList.value = false;
};

const handleNewSession = async () => {
  await createNewSession();
  showSessionList.value = false;
};

const handleRenameSession = async (session) => {
  try {
    const { value } = await ElMessageBox.prompt("为这次会话设置一个更容易识别的名称", "重命名会话", {
      inputValue: session.title,
      inputPlaceholder: "例如：Spring Boot 自动配置原理",
      inputValidator: (title) => {
        const normalized = title?.trim();
        if (!normalized) return "会话名称不能为空";
        if (normalized.length > 100) return "会话名称不能超过100个字符";
        return true;
      },
      confirmButtonText: "保存",
      cancelButtonText: "取消",
      autofocus: true,
    });
    const updated = await request.put(`/sessions/${session.id}/title`, { title: value.trim() });
    sessions.value = sessions.value.map((item) => (item.id === session.id ? updated : item));
    ElMessage.success("会话名称已更新");
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error("重命名失败：" + (error.message || "未知错误"));
    }
  }
};

const handleDeleteSession = async (sessionId) => {
  try {
    await request.delete(`/sessions/${sessionId}`);
    sessions.value = sessions.value.filter((s) => s.id !== sessionId);
    if (sessionId === currentSessionId.value) {
      currentSessionId.value = null;
      messages.value = [];
      if (sessions.value.length > 0) {
        await switchSession(sessions.value[0].id);
      } else {
        await createNewSession();
      }
    }
    ElMessage.success("会话已删除");
  } catch (e) {
    ElMessage.error("删除失败：" + (e.message || "未知错误"));
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (ragBodyRef.value) {
      ragBodyRef.value.scrollTop = ragBodyRef.value.scrollHeight;
    }
  });
};

const formatTime = (timeStr) => {
  if (!timeStr) return "";
  const d = new Date(timeStr);
  const now = new Date();
  const diff = now - d;
  if (diff < 60000) return "刚刚";
  if (diff < 3600000) return Math.floor(diff / 60000) + "分钟前";
  if (diff < 86400000) return Math.floor(diff / 3600000) + "小时前";
  return d.toLocaleDateString("zh-CN");
};

watch(() => messages.value.length, () => {
  scrollToBottom();
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
  if (!currentSessionId.value) {
    await createNewSession();
  }

  loading.value = true;
  isStreaming.value = true;
  streamingContent.value = "";
  streamingReferences.value = [];

  const userQuestion = question.value;
  messages.value.push({ role: "user", content: userQuestion });
  question.value = "";
  showSessionList.value = false;
  scrollToBottom();

  const controller = new AbortController();
  abortController.value = controller;

  try {
    const token = userStore.token;
    const response = await fetch("/api/rag/answer/stream", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        question: userQuestion,
        topK: 3,
        includeReferences: true,
        sessionId: currentSessionId.value,
      }),
      signal: controller.signal,
    });

    if (!response.ok) {
      const text = await response.text();
      if (text.includes("请先在设置页配置")) {
        ElMessageBox.alert(
          "AI服务不可用，请配置有效的API Key。\n\n请前往【个人设置】添加您的API Key，或联系管理员配置平台API Key。",
          "需要配置API Key",
          { confirmButtonText: "前往设置", type: "warning" },
        ).then(() => router.push("/settings"));
        messages.value.pop();
        return;
      }
      throw new Error(text || `HTTP ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let currentEvent = "message";
    let startTime = Date.now();

    while (true) {
      const { done, value } = await reader.read();

      if (value) {
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() || "";

        for (const line of lines) {
          if (line.startsWith("event:")) {
            currentEvent = line.substring(6).trim();
          } else if (line.startsWith("data:")) {
            const data = line.substring(5).trim();
            dispatchEvent(currentEvent, data, startTime);
            currentEvent = "message";
          }
        }
      }

      if (done) break;
    }

    if (buffer.trim()) {
      const line = buffer.trim();
      if (line.startsWith("data:")) {
        dispatchEvent(currentEvent, line.substring(5).trim(), startTime);
      }
    }
  } catch (error) {
    if (error.name === "AbortError") {
      ElMessage.info("已停止生成");
      return;
    }
    console.error("流式问答失败:", error);
    if (error.message && error.message.includes("API Key")) {
      ElMessageBox.alert(
        "AI服务不可用，请配置有效的API Key。\n\n请前往【个人设置】添加您的API Key，或联系管理员配置平台API Key。",
        "需要配置API Key",
        { confirmButtonText: "前往设置", type: "warning" },
      ).then(() => router.push("/settings"));
    } else {
      ElMessage.error("回答失败：" + (error.message || "网络错误"));
    }
  } finally {
    if (streamingContent.value) {
      messages.value.push({
        role: "assistant",
        content: streamingContent.value,
        references: streamingReferences.value,
      });
      streamingContent.value = null;
      streamingReferences.value = [];
    }
    loading.value = false;
    isStreaming.value = false;
    abortController.value = null;
    loadSessions();
  }
};

function dispatchEvent(eventType, data, startTime) {
  switch (eventType) {
    case "token":
      streamingContent.value += data;
      break;
    case "references":
      try {
        streamingReferences.value = JSON.parse(data);
      } catch (e) {
        console.warn("解析引用失败:", e);
      }
      break;
    case "metrics":
      // metrics are optional, silently ignore
      break;
    case "done":
      if (!streamingContent.value) {
        streamingContent.value = "（未生成回答）";
      }
      messages.value.push({
        role: "assistant",
        content: streamingContent.value,
        references: streamingReferences.value,
      });
      streamingContent.value = null;
      streamingReferences.value = [];
      scrollToBottom();
      break;
    case "error":
      if (data && data.includes("请先在设置页配置")) {
        ElMessageBox.alert(data, "需要配置API Key", {
          confirmButtonText: "前往设置",
          type: "warning",
        }).then(() => router.push("/settings"));
      } else {
        ElMessage.error(data || "生成失败");
      }
      break;
  }
}

const handleStop = () => {
  if (abortController.value) {
    abortController.value.abort();
    if (streamingContent.value) {
      messages.value.push({
        role: "assistant",
        content: streamingContent.value + "（已停止）",
        references: streamingReferences.value,
      });
      streamingContent.value = null;
      streamingReferences.value = [];
    }
  }
};

onMounted(async () => {
  await nextTick();
  initSession();
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
  grid-template-columns: 260px 1fr var(--rag-panel-width, 380px);
  gap: 16px;
  min-height: calc(100vh - 180px);
}

.content-wrapper:has(.right-panel.collapsed) {
  grid-template-columns: 260px 1fr 44px;
}

.content-wrapper:has(.right-panel.dragging) {
  transition: none;
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
  background: var(--bg-card);
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 4px;
  height: 40px;
  border-radius: 2px;
  flex-shrink: 0;
}

.stat-icon.purple { background: var(--color-primary); }
.stat-icon.blue   { background: var(--color-info); }
.stat-icon.green  { background: var(--color-success); }
.stat-icon.orange { background: var(--color-accent); }

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
  background: var(--bg-card);
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-lg);
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
  font-family: var(--font-family-display);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
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
  min-width: 0;
  position: relative;
}

.right-panel.dragging {
  transition: none;
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
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
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
  font-family: var(--font-family-display);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: white;
  margin: 0;
}

.rag-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: white;
  font-size: var(--font-size-xs);
  padding: 4px 12px;
  transition: background var(--transition-fast);
}

.history-btn:hover {
  background: rgba(255, 255, 255, 0.35);
  color: white;
}

.collapse-btn {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  width: 26px;
  height: 26px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--transition-fast);
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  color: white;
}

/* ---- Collapsed tab ---- */
.collapsed-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
  background: white;
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-secondary);
  transition: color var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
  height: 100%;
  justify-content: center;
}

.collapsed-tab:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.collapsed-label {
  writing-mode: vertical-rl;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  letter-spacing: 0.1em;
}

/* ---- Resize handle ---- */
.resize-handle {
  position: absolute;
  left: -6px;
  top: 0;
  bottom: 0;
  width: 12px;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  transition: background var(--transition-fast);
}

.resize-handle:hover,
.resize-handle.dragging {
  background: var(--color-primary-alpha-10);
}

.resize-grip {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 4px 2px;
  border-radius: 3px;
  transition: background var(--transition-fast);
}

.resize-grip:hover {
  background: var(--color-primary-alpha-10);
}

.resize-grip span {
  display: block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--border-base);
  transition: background var(--transition-fast);
}

.resize-handle.dragging .resize-grip span {
  background: var(--color-primary);
}

.resize-handle.collapsed {
  display: none;
}

.rag-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-questions {
  margin-bottom: 8px;
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

.quick-item:hover {
  background: var(--color-primary-alpha-10);
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

/* ---- Chat Messages ---- */
.chat-message {
  display: flex;
  gap: 10px;
  animation: fadeIn 0.2s ease-out;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.chat-message.user .chat-content {
  background: var(--color-primary);
  color: white;
  border-radius: 12px 12px 4px 12px;
}

.chat-message:not(.user) .chat-content {
  background: var(--bg-page);
  color: var(--text-primary);
  border-radius: 12px 12px 12px 4px;
  border: 1px solid var(--border-lighter);
}

.chat-message.streaming .chat-content {
  border-color: var(--color-primary);
  background: var(--color-primary-alpha-10);
}

.chat-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-primary);
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
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-references {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--border-lighter);
}

.ref-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.ref-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.ref-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  font-size: 11px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 999px;
  color: var(--text-secondary);
}

.ref-score {
  color: var(--color-primary);
  font-weight: 600;
}

/* ---- Session List ---- */
.session-list-panel {
  border-bottom: 1px solid var(--border-lighter);
  max-height: 220px;
  overflow-y: auto;
  background: var(--bg-page);
}

.session-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.session-list-body {
  padding: 0 8px 8px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.session-item:hover,
.session-item.active {
  background: var(--color-primary-alpha-10);
}

.session-title {
  flex: 1;
  font-size: 13px;
  color: var(--text-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 11px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.session-actions {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.session-action {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  padding: 0;
}

.session-item:hover .session-actions,
.session-item:focus-within .session-actions {
  opacity: 1;
}

.session-delete:hover {
  color: var(--color-danger);
}

.session-empty {
  padding: 24px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}

/* ---- Streaming ---- */
.streaming-cursor {
  display: inline;
  font-size: 14px;
  color: var(--color-primary);
  animation: blink 0.8s step-end infinite;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
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
    grid-template-columns: 220px 1fr var(--rag-panel-width, 340px);
    gap: 12px;
  }
}

@media (max-width: 1200px) {
  .content-wrapper {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .resize-handle {
    display: none;
  }

  .right-panel.collapsed {
    display: none;
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
