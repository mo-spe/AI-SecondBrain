<template>
  <div class="knowledge-system-container">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">知识体系</h1>
        <p class="page-subtitle">可视化您的知识网络，构建完整的知识体系</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="goGraphWorkspace">
          <el-icon><Connection /></el-icon>
          打开全屏图谱
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
        </div>

        <div class="knowledge-tree-card">
          <div class="tree-header">
            <div>
              <span class="tree-kicker">KNOWLEDGE TAGS</span>
              <h3 class="tree-title">知识标签</h3>
            </div>
            <button type="button" class="manage-tags-btn" aria-label="前往知识管理标签" @click="openKnowledgeManagement">
              管理
            </button>
          </div>
          <div class="tree-search-box">
            <el-icon size="14" color="#94a3b8"><Search /></el-icon>
            <input
              type="text"
              v-model="searchQuery"
              placeholder="搜索标签..."
              class="tree-search-input"
            />
          </div>
          <div v-if="tagLoading" class="tree-state" role="status">正在加载标签…</div>
          <div v-else-if="tagError" class="tree-state tree-error" role="alert">
            <span>标签暂时无法加载</span>
            <button type="button" @click="loadTagTree">重试</button>
          </div>
          <div v-else-if="tagTree.length === 0" class="tree-state tree-empty">
            <span>还没有知识标签</span>
            <button type="button" @click="openKnowledgeManagement">去创建标签</button>
          </div>
          <div v-else class="tree-body">
            <el-tree
              ref="treeRef"
              :data="treeData"
              :props="treeProps"
              :highlight-current="true"
              :default-expanded-keys="['all-knowledge']"
              :filter-node-method="filterTreeNode"
              node-key="id"
              @node-click="handleNodeClick"
              class="knowledge-tree"
            >
              <template #default="{ node, data }">
                <div class="tree-node">
                  <el-icon :size="14" :color="data.color || '#64748b'">
                    <component :is="data.icon || Document" />
                  </el-icon>
                  <span class="node-label">{{ node.label }}</span>
                  <span class="node-count">{{ data.count || 0 }}</span>
                </div>
              </template>
            </el-tree>
          </div>
        </div>
      </div>

      <div class="middle-panel">
        <section class="graph-launcher" aria-label="全屏知识图谱入口">
          <div class="launcher-grid" aria-hidden="true"></div>
          <div class="launcher-constellation" aria-hidden="true">
            <span class="constellation-line line-one"></span>
            <span class="constellation-line line-two"></span>
            <span class="constellation-line line-three"></span>
            <span class="constellation-orbit orbit-one"></span>
            <span class="constellation-orbit orbit-two"></span>
            <i class="constellation-point point-core"></i>
            <i class="constellation-point point-one"></i>
            <i class="constellation-point point-two"></i>
            <i class="constellation-point point-three"></i>
            <i class="constellation-point point-four"></i>
          </div>
          <div class="launcher-topbar">
            <span class="launcher-status"><i></i> 图谱已就绪</span>
            <div class="launcher-live-stats" aria-label="图谱实时统计">
              <span><strong>{{ nodeCount }}</strong> 知识点</span>
              <span><strong>{{ edgeCount }}</strong> 连接</span>
            </div>
          </div>
          <div class="launcher-content">
            <span class="launcher-kicker">KNOWLEDGE GRAPH · EXPLORATION MODE</span>
            <h2>让每一条知识<br /><em>找到它的关联。</em></h2>
            <p>用完整画布追踪概念之间的脉络：缩放、拖拽、聚焦关联，并从一个节点进入知识详情。</p>
            <el-button type="primary" @click="goGraphWorkspace">
              开始探索知识网络
              <el-icon><Connection /></el-icon>
            </el-button>
          </div>
          <p class="launcher-footer-note">空间布局仅在本次浏览中保留</p>
        </section>
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
import { ref, computed, onMounted, nextTick, watch } from "vue";
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
  Close,
  Clock,
  EditPen,
} from "@element-plus/icons-vue";
import request from "@/utils/request";
import { tagsAPI } from "@/api/tags";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();

const treeRef = ref(null);

const userAvatar = "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png";

const nodeCount = ref(0);
const edgeCount = ref(0);

const searchQuery = ref("");
const tagTree = ref([]);
const tagLoading = ref(false);
const tagError = ref("");
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

const toTreeNode = (tag) => ({
  id: `tag-${tag.id}`,
  tagId: tag.id,
  label: tag.tagName || "未命名标签",
  count: Number(tag.nodeCount) || 0,
  color: tag.tagColor || "#5d9279",
  icon: Array.isArray(tag.children) && tag.children.length ? FolderOpened : DocumentCopy,
  children: Array.isArray(tag.children) ? tag.children.map(toTreeNode) : [],
});

const treeData = computed(() => [{
  id: "all-knowledge",
  label: "全部知识",
  count: nodeCount.value,
  color: "#2b5f4b",
  icon: Notebook,
  isAllKnowledge: true,
  children: tagTree.value.map(toTreeNode),
}]);

const treeProps = {
  label: "label",
  children: "children",
};

const filterTreeNode = (keyword, data) => !keyword || data.label.toLocaleLowerCase().includes(keyword.toLocaleLowerCase());

const loadTagTree = async () => {
  tagLoading.value = true;
  tagError.value = "";
  try {
    const data = await tagsAPI.getTree();
    tagTree.value = Array.isArray(data) ? data : [];
  } catch (error) {
    tagTree.value = [];
    tagError.value = error?.message || "请检查网络后重试";
  } finally {
    tagLoading.value = false;
  }
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

watch(searchQuery, (keyword) => {
  treeRef.value?.filter(keyword);
});

const initGraph = async () => {
  try {
    const graph = await request.get("/knowledge/relation/graph");
    nodeCount.value = Array.isArray(graph?.nodes) ? graph.nodes.length : 0;
    edgeCount.value = Array.isArray(graph?.edges) ? graph.edges.length : 0;
  } catch (error) {
    // 入口页只反映真实关系数据；请求失败时不以示例数据误导用户。
    nodeCount.value = 0;
    edgeCount.value = 0;
  }
};

const handleNodeClick = (data) => {
  router.push(data.isAllKnowledge ? "/knowledge" : { path: "/knowledge", query: { tagId: String(data.tagId) } });
};

const openKnowledgeManagement = () => router.push("/knowledge");
const goGraphWorkspace = () => router.push("/knowledge-graph");

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
  initGraph();
  loadTagTree();
});
</script>

<style scoped>
.knowledge-system-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--navbar-height));
  min-height: 660px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
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
  flex: 1;
  grid-template-columns: 260px 1fr var(--rag-panel-width, 380px);
  gap: 16px;
  min-height: 0;
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
  min-height: 0;
  gap: 14px;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 16px 12px;
  border-bottom: 1px solid var(--border-lighter);
}

.tree-kicker {
  display: block;
  margin-bottom: 4px;
  color: var(--color-primary);
  font-family: var(--font-family-ui);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.13em;
}

.tree-title {
  font-family: var(--font-family-display);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
}

.manage-tags-btn {
  min-height: 32px;
  padding: 0 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--color-primary);
  font: 600 12px var(--font-family-ui);
  cursor: pointer;
}

.manage-tags-btn:hover,
.manage-tags-btn:focus-visible {
  background: var(--color-primary-alpha-10);
  outline: none;
}

.tree-search-box {
  margin: 10px 12px;
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

.tree-state {
  display: grid;
  flex: 1;
  align-content: center;
  justify-items: start;
  gap: 8px;
  padding: 20px 16px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.tree-state button {
  min-height: 32px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font: 600 12px var(--font-family-ui);
  cursor: pointer;
}

.tree-state button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
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
  min-height: 0;
}

.graph-launcher {
  position: relative;
  display: flex;
  flex: 1;
  align-items: flex-end;
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(65, 94, 80, 0.65);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 72% 38%, rgba(107, 177, 136, 0.17), transparent 30%),
    radial-gradient(circle at 44% 105%, rgba(46, 114, 85, 0.2), transparent 35%),
    linear-gradient(137deg, #0c1511 0%, #122119 54%, #17291f 100%);
  box-shadow: 0 22px 46px rgba(18, 38, 28, 0.14);
}

.launcher-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(200, 226, 211, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(200, 226, 211, 0.055) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: linear-gradient(90deg, black 5%, rgba(0, 0, 0, 0.55) 66%, transparent);
}

.launcher-constellation {
  position: absolute;
  z-index: 0;
  top: 7%;
  right: 4%;
  width: min(58%, 620px);
  height: 78%;
  opacity: 0.9;
}

.constellation-line,
.constellation-orbit {
  position: absolute;
  display: block;
  transform-origin: left center;
}

.constellation-line {
  height: 1px;
  background: linear-gradient(90deg, rgba(175, 225, 192, 0.05), rgba(175, 225, 192, 0.58), rgba(175, 225, 192, 0.08));
}

.line-one { top: 28%; left: 11%; width: 55%; transform: rotate(24deg); }
.line-two { top: 57%; left: 25%; width: 49%; transform: rotate(-33deg); }
.line-three { top: 41%; left: 45%; width: 38%; transform: rotate(59deg); }

.constellation-orbit {
  width: 46%;
  aspect-ratio: 1;
  border: 1px solid rgba(168, 219, 184, 0.14);
  border-radius: 50%;
}

.orbit-one { top: 12%; left: 30%; }
.orbit-two { right: -8%; bottom: 4%; width: 58%; border-color: rgba(168, 219, 184, 0.09); }

.constellation-point {
  position: absolute;
  width: 11px;
  height: 11px;
  border: 2px solid rgba(225, 243, 231, 0.76);
  border-radius: 50%;
  background: #76b58c;
  box-shadow: 0 0 0 7px rgba(118, 181, 140, 0.09), 0 0 22px rgba(126, 203, 152, 0.34);
}

.point-core { top: 39%; left: 43%; width: 22px; height: 22px; background: #a8e48b; box-shadow: 0 0 0 10px rgba(168, 228, 139, 0.1), 0 0 36px rgba(168, 228, 139, 0.52); }
.point-one { top: 17%; left: 12%; background: #709bcd; }
.point-two { top: 26%; right: 9%; background: #d3ac69; }
.point-three { bottom: 16%; left: 25%; background: #c27672; }
.point-four { bottom: 9%; right: 12%; background: #8fbf9a; }

.launcher-topbar {
  position: absolute;
  z-index: 1;
  top: 22px;
  right: 24px;
  left: 26px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: #b7cbc0;
  font-family: var(--font-family-ui);
  font-size: 11px;
}

.launcher-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  letter-spacing: 0.04em;
}

.launcher-status i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #93d4a3;
  box-shadow: 0 0 0 4px rgba(147, 212, 163, 0.1);
}

.launcher-live-stats {
  display: flex;
  gap: 13px;
}

.launcher-live-stats span {
  padding-left: 13px;
  border-left: 1px solid rgba(198, 229, 210, 0.15);
}

.launcher-live-stats strong {
  margin-right: 4px;
  color: #edf6f0;
  font-size: 13px;
}

.launcher-content {
  position: relative;
  z-index: 1;
  max-width: 515px;
  margin: 0 0 10% 7%;
  padding: 29px 30px 29px 26px;
  border-left: 2px solid rgba(167, 211, 178, 0.78);
  background: linear-gradient(90deg, rgba(10, 22, 15, 0.9), rgba(10, 22, 15, 0.54), transparent);
  color: #edf5ef;
}

.launcher-kicker {
  display: block;
  margin-bottom: 14px;
  color: #a7d3b2;
  font-family: var(--font-family-ui);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.launcher-content h2 {
  margin: 0 0 14px;
  color: #f1f8f3;
  font-family: var(--font-family-display);
  font-size: clamp(30px, 3vw, 44px);
  font-weight: 700;
  letter-spacing: -0.035em;
  line-height: 1.08;
}

.launcher-content h2 em {
  color: #a6d9ad;
  font-style: normal;
}

.launcher-content p {
  max-width: 460px;
  margin: 0 0 26px;
  color: #c4d4cb;
  font-size: 14px;
  line-height: 1.8;
}

.launcher-content :deep(.el-button) {
  min-height: 44px;
  padding: 0 17px;
  border-color: rgba(170, 222, 181, 0.62);
  background: #317257;
  box-shadow: 0 9px 22px rgba(4, 16, 9, 0.27);
  font-weight: 650;
}

.launcher-content :deep(.el-button:hover) {
  border-color: #b5e3ba;
  background: #3d8565;
  transform: translateY(-1px);
}

.launcher-footer-note {
  position: absolute;
  z-index: 1;
  right: 24px;
  bottom: 18px;
  margin: 0;
  color: rgba(190, 211, 200, 0.58);
  font-family: var(--font-family-ui);
  font-size: 10px;
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
  .knowledge-system-container {
    height: auto;
    min-height: 100%;
  }

  .content-wrapper {
    flex: none;
    grid-template-columns: 1fr;
    min-height: auto;
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
