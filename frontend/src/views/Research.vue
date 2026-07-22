<template>
  <div class="research-page">
    <div class="main-content">
      <div class="page-header">
        <div class="header-banner">
          <div class="banner-icon">
            <el-icon size="32" color="white"><TrendCharts /></el-icon>
          </div>
          <div class="banner-content">
            <h1 class="banner-title">AI-Learning-Research-研报中心</h1>
            <p class="banner-subtitle">基于DeepFlow Agent的AI分析和规划</p>
          </div>
        </div>
      </div>

      <div class="content-wrapper">
        <div class="cards-grid">
          <div class="research-card">
            <div class="card-icon purple">
              <el-icon size="24" color="white"><Compass /></el-icon>
            </div>
            <div class="card-header">
              <h3 class="card-title">学习路径规划</h3>
            </div>
            <div class="card-body">
              <el-form :model="pathForm" class="card-form">
                <el-form-item label="学习主题">
                  <el-input
                    v-model="pathForm.topic"
                    placeholder="例如：Redis、Python、机器学习"
                    size="small"
                    maxlength="100"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item label="当前水平">
                  <el-select
                    v-model="pathForm.currentLevel"
                    size="small"
                    style="width: 100%"
                  >
                    <el-option label="初学者" value="beginner" />
                    <el-option label="中级" value="intermediate" />
                    <el-option label="高级" value="advanced" />
                  </el-select>
                </el-form-item>
                <el-form-item label="目标水平">
                  <el-select
                    v-model="pathForm.targetLevel"
                    size="small"
                    style="width: 100%"
                  >
                    <el-option label="初学者" value="beginner" />
                    <el-option label="中级" value="intermediate" />
                    <el-option label="高级" value="advanced" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    size="small"
                    @click="generateLearningPath"
                    :loading="generatingPath"
                    style="width: 100%"
                  >
                    <el-icon size="14"><MagicStick /></el-icon>
                    <span>生成学习路径</span>
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>

          <div class="research-card">
            <div class="card-icon blue">
              <el-icon size="24" color="white"><Search /></el-icon>
            </div>
            <div class="card-header">
              <h3 class="card-title">知识盲区分析</h3>
            </div>
            <div class="card-body">
              <el-form :model="gapsForm" class="card-form">
                <el-form-item label="目标主题">
                  <el-input
                    v-model="gapsForm.topic"
                    placeholder="例如：微服务架构、前端开发"
                    size="small"
                    maxlength="100"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item label="已掌握的知识点">
                  <el-select
                    v-model="gapsForm.userKnowledge"
                    multiple
                    filterable
                    allow-create
                    placeholder="输入或选择已掌握的知识点"
                    size="small"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="item in commonKnowledge"
                      :key="item"
                      :label="item"
                      :value="item"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    size="small"
                    @click="researchKnowledgeGaps"
                    :loading="analyzingGaps"
                    style="width: 100%"
                  >
                    <el-icon size="14"><Search /></el-icon>
                    <span>分析知识盲区</span>
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>

          <div class="research-card">
            <div class="card-icon orange">
              <el-icon size="24" color="white"><Document /></el-icon>
            </div>
            <div class="card-header">
              <h3 class="card-title">学习报告生成</h3>
            </div>
            <div class="card-body">
              <el-form :model="reportForm" class="card-form">
                <el-form-item label="报告主题">
                  <el-input
                    v-model="reportForm.topic"
                    placeholder="例如：Python编程学习、机器学习"
                    size="small"
                    maxlength="100"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item label="分析深度">
                  <el-select
                    v-model="reportForm.depth"
                    size="small"
                    style="width: 100%"
                  >
                    <el-option label="浅度分析" value="shallow" />
                    <el-option label="中度分析" value="medium" />
                    <el-option label="深度分析" value="deep" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    size="small"
                    @click="generateReport"
                    :loading="generatingReport"
                    style="width: 100%"
                  >
                    <el-icon size="14"><Document /></el-icon>
                    <span>生成学习报告</span>
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </div>

        <div class="history-section">
          <div class="section-header">
            <div class="section-icon">
              <el-icon size="20" color="#7c3aed"><Clock /></el-icon>
            </div>
            <h3 class="section-title">AI活动&历史</h3>
          </div>
          <div class="history-tabs">
            <el-tabs v-model="activeTab" class="simple-tabs">
              <el-tab-pane label="最近活动" name="recent" />
              <el-tab-pane label="生成的路径" name="path" />
              <el-tab-pane label="盲区报告" name="gaps" />
              <el-tab-pane label="生成的报告" name="report" />
            </el-tabs>
          </div>
          <div class="history-table-wrapper">
            <el-table :data="filteredHistory" border stripe :loading="loading">
              <el-table-column prop="topic" label="名称" min-width="200">
                <template #default="scope">
                  <div class="topic-cell">
                    <el-tag
                      :type="getHistoryTagType(scope.row.type)"
                      size="small"
                    >
                      {{ getHistoryTypeName(scope.row.type) }}
                    </el-tag>
                    <span>{{ scope.row.topic }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态">
                <template #default="scope">
                  <el-tag
                    :type="getStatusTagType(scope.row.status)"
                    size="small"
                  >
                    {{ scope.row.status || '已完成' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="日期" width="120">
                <template #default="scope">
                  {{ formatDate(scope.row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="scope">
                  <el-button
                    type="text"
                    size="small"
                    @click="viewHistory(scope.row)"
                  >
                    <el-icon><Link /></el-icon>
                    <span>查看</span>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!loading && filteredHistory.length === 0" class="empty-state">
              <el-empty description="暂无记录" :image-size="60" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showPathDialog"
      title="学习路径"
      width="900px"
      class="result-dialog"
    >
      <div v-if="learningPathResult" class="result-content">
        <div
          class="markdown-content"
          v-html="renderMarkdown(learningPathResult)"
        ></div>
      </div>
      <template #footer>
        <el-button @click="showPathDialog = false">关闭</el-button>
        <el-button type="primary" @click="exportPath">导出路径</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showGapsDialog"
      title="知识盲区分析"
      width="900px"
      class="result-dialog"
    >
      <div v-if="gapsAnalysisResult" class="result-content">
        <div
          class="markdown-content"
          v-html="renderMarkdown(gapsAnalysisResult)"
        ></div>
      </div>
      <template #footer>
        <el-button @click="showGapsDialog = false">关闭</el-button>
        <el-button type="primary" @click="exportGaps">导出分析</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showReportDialog"
      title="学习报告"
      width="900px"
      class="result-dialog"
    >
      <div v-if="currentReport" class="result-content">
        <div
          class="markdown-content"
          v-html="renderMarkdown(currentReport.content)"
        ></div>
      </div>
      <template #footer>
        <el-button @click="showReportDialog = false">关闭</el-button>
        <el-button type="primary" @click="exportReport">导出报告</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  TrendCharts,
  Compass,
  Search,
  Document,
  MagicStick,
  Clock,
  Link,
} from "@element-plus/icons-vue";
import { deerFlowAPI } from "@/api/deerflow";
import request from "@/utils/request";
import { marked } from "marked";
import DOMPurify from "dompurify";
import { useUserStore } from "@/stores/user";

marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false,
});

const userStore = useUserStore();

const activeTab = ref("recent");
const pathForm = ref({
  topic: "",
  currentLevel: "beginner",
  targetLevel: "advanced",
});
const gapsForm = ref({
  topic: "",
  userKnowledge: [],
});
const reportForm = ref({
  topic: "",
  depth: "medium",
});

const generatingPath = ref(false);
const analyzingGaps = ref(false);
const generatingReport = ref(false);
const loading = ref(false);

const showPathDialog = ref(false);
const showGapsDialog = ref(false);
const showReportDialog = ref(false);

const learningPathResult = ref("");
const gapsAnalysisResult = ref("");
const currentReport = ref(null);
const historyList = ref([]);

const commonKnowledge = ref([
  "Java 基础",
  "Python 基础",
  "Spring Boot",
  "MySQL 数据库",
  "Redis",
  "RESTful API",
  "Git 版本控制",
  "Linux 基础",
  "Docker",
  "前端开发",
  "Vue.js",
  "React",
  "Node.js",
  "微服务架构",
  "消息队列",
  "分布式系统",
  "算法与数据结构",
]);

const filteredHistory = computed(() => {
  if (activeTab.value === "recent") return historyList.value;
  return historyList.value.filter((item) => item.type === activeTab.value);
});

onMounted(async () => {
  if (userStore.isLoggedIn()) {
    loadHistoryList();
    loadUserKnowledge();
  }
});

const loadUserKnowledge = async () => {
  try {
    const response = await request.get("/knowledge/list", {
      params: { current: 1, size: 100 },
    });
    const nodes = response?.records || [];
    if (nodes.length > 0) {
      commonKnowledge.value = nodes.map((node) => node.title);
    }
  } catch (error) {
    console.error("加载知识点失败：", error);
  }
};

const loadHistoryList = async () => {
  try {
    loading.value = true;
    const data = await deerFlowAPI.getResearchHistoryList({
      current: 1,
      size: 100,
    });
    historyList.value = data.records || [];
  } catch (error) {
    ElMessage.error("加载历史记录失败：" + error.message);
  } finally {
    loading.value = false;
  }
};

const generateLearningPath = async () => {
  if (!pathForm.value.topic) {
    ElMessage.warning("请输入学习主题");
    return;
  }
  generatingPath.value = true;
  try {
    const task = await deerFlowAPI.generateLearningPath(pathForm.value);
    if (task.status === "COMPLETED") {
      learningPathResult.value = task.result;
      showPathDialog.value = true;
      ElMessage.success("学习路径生成完成！");
      saveHistory({
        type: "path",
        topic: pathForm.value.topic,
        currentLevel: pathForm.value.currentLevel,
        targetLevel: pathForm.value.targetLevel,
        content: learningPathResult.value,
      });
    } else {
      ElMessage.success("学习路径生成任务已创建");
    }
  } catch (error) {
    ElMessage.error("生成学习路径失败：" + (error.message || "未知错误"));
  } finally {
    generatingPath.value = false;
  }
};

const researchKnowledgeGaps = async () => {
  if (!gapsForm.value.topic) {
    ElMessage.warning("请输入目标主题");
    return;
  }
  if (gapsForm.value.userKnowledge.length === 0) {
    ElMessage.warning("请至少选择一个已掌握的知识点");
    return;
  }
  analyzingGaps.value = true;
  try {
    const task = await deerFlowAPI.researchKnowledgeGap(gapsForm.value);
    if (task.status === "COMPLETED") {
      gapsAnalysisResult.value = task.result;
      showGapsDialog.value = true;
      ElMessage.success("知识盲区分析完成！");
      saveHistory({
        type: "gaps",
        topic: gapsForm.value.topic,
        userKnowledge: gapsForm.value.userKnowledge,
        content: gapsAnalysisResult.value,
      });
    } else {
      ElMessage.success("知识盲区分析任务已创建");
    }
  } catch (error) {
    ElMessage.error("分析知识盲区失败：" + (error.message || "未知错误"));
  } finally {
    analyzingGaps.value = false;
  }
};

const generateReport = async () => {
  if (!reportForm.value.topic) {
    ElMessage.warning("请输入报告主题");
    return;
  }
  generatingReport.value = true;
  try {
    const task = await deerFlowAPI.generateLearningReport(reportForm.value);
    if (task.status === "COMPLETED") {
      currentReport.value = {
        topic: reportForm.value.topic,
        content: task.result,
        depth: reportForm.value.depth,
      };
      showReportDialog.value = true;
      ElMessage.success("学习报告生成完成！");
      saveHistory({
        type: "report",
        topic: reportForm.value.topic,
        depth: reportForm.value.depth,
        content: task.result,
      });
    } else {
      ElMessage.success("学习报告生成任务已创建");
    }
  } catch (error) {
    ElMessage.error("生成学习报告失败：" + (error.message || "未知错误"));
  } finally {
    generatingReport.value = false;
  }
};

const saveHistory = async (historyData) => {
  if (!userStore.isLoggedIn()) return;
  try {
    await deerFlowAPI.saveResearchHistory(historyData);
    await loadHistoryList();
  } catch (error) {
    console.error("保存历史记录失败：", error);
  }
};

const viewHistory = (item) => {
  let content = item.content;
  try {
    const parsed = JSON.parse(content);
    if (typeof parsed === "string") content = parsed;
    else if (typeof parsed === "object") content = JSON.stringify(parsed, null, 2);
  } catch (e) {}

  if (item.type === "path") {
    learningPathResult.value = content;
    showPathDialog.value = true;
  } else if (item.type === "gaps") {
    gapsAnalysisResult.value = content;
    showGapsDialog.value = true;
  } else {
    currentReport.value = { topic: item.topic, content };
    showReportDialog.value = true;
  }
};

const getHistoryTagType = (type) => {
  const typeMap = { path: "primary", gaps: "success", report: "warning" };
  return typeMap[type] || "info";
};

const getHistoryTypeName = (type) => {
  const nameMap = { path: "学习路径", gaps: "知识盲区", report: "学习报告" };
  return nameMap[type] || type;
};

const getStatusTagType = (status) => {
  const statusMap = { "已完成": "success", "已提交": "warning", "进行中": "primary" };
  return statusMap[status] || "info";
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const renderMarkdown = (content) => {
  if (!content) return "";
  let parsedContent = content;
  try {
    const parsed = JSON.parse(content);
    if (typeof parsed === "string") parsedContent = parsed;
    else if (typeof parsed === "object") parsedContent = JSON.stringify(parsed, null, 2);
  } catch (e) {}
  const html = marked(parsedContent);
  return DOMPurify.sanitize(html);
};

const exportPath = () => {
  if (!learningPathResult.value) return;
  downloadFile(`${pathForm.value.topic}_学习路径.md`, learningPathResult.value);
};

const exportGaps = () => {
  if (!gapsAnalysisResult.value) return;
  downloadFile(`${gapsForm.value.topic}_知识盲区分析.md`, gapsAnalysisResult.value);
};

const exportReport = () => {
  if (!currentReport.value) return;
  downloadFile(`${currentReport.value.topic}_学习报告.md`, currentReport.value.content);
};

const downloadFile = (filename, content) => {
  const blob = new Blob([content], { type: "text/markdown" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  ElMessage.success("导出成功");
};
</script>

<style scoped>
.research-page {
  min-height: 100%;
  background: var(--bg-page);
}

.main-content {
  padding: var(--spacing-xl) 0;
}

.page-header {
  margin-bottom: var(--spacing-xl);
}

.header-banner {
  background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 50%, #9333ea 100%);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  display: flex;
  align-items: center;
  gap: var(--spacing-xl);
  box-shadow: var(--shadow-lg);
}

.banner-icon {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.banner-content {
  flex: 1;
}

.banner-title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: white;
  margin: 0 0 var(--spacing-sm) 0;
}

.banner-subtitle {
  font-size: var(--font-size-base);
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-xl);
}

.research-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--spacing-lg);
}

.card-icon.purple {
  background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 100%);
}

.card-icon.blue {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
}

.card-icon.orange {
  background: linear-gradient(135deg, #f97316 0%, #fb923c 100%);
}

.card-header {
  margin-bottom: var(--spacing-lg);
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
}

.card-body {
  display: flex;
  flex-direction: column;
}

.card-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.card-form :deep(.el-form-item) {
  margin-bottom: var(--spacing-md);
}

.card-form :deep(.el-form-item__label) {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.card-form :deep(.el-input__wrapper) {
  box-shadow: none;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}

.card-form :deep(.el-select .el-input__wrapper) {
  box-shadow: none;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}

.card-form :deep(.el-button--primary) {
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-md);
}

.history-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-lighter);
}

.section-icon {
  width: 36px;
  height: 36px;
  background: rgba(124, 58, 237, 0.1);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
}

.history-tabs {
  margin-bottom: var(--spacing-lg);
}

.simple-tabs {
  --el-tabs-header-padding: 0;
}

.simple-tabs :deep(.el-tabs__item) {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
}

.simple-tabs :deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
}

.simple-tabs :deep(.el-tabs__active-bar) {
  background: var(--color-primary);
}

.history-table-wrapper {
  overflow-x: auto;
}

.topic-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.topic-cell span {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

.empty-state {
  padding: var(--spacing-2xl);
}

.result-content {
  max-height: 600px;
  overflow-y: auto;
}

.markdown-content {
  line-height: 1.7;
  color: var(--text-regular);
  font-size: var(--font-size-base);
  padding: var(--spacing-lg);
  background: var(--bg-page);
  border-radius: var(--radius-md);
}

.markdown-content :deep(h1) {
  font-size: 22px;
  font-weight: 700;
  margin: 16px 0 12px 0;
  color: var(--text-primary);
  padding-bottom: 6px;
  border-bottom: 2px solid var(--color-primary);
}

.markdown-content :deep(h2) {
  font-size: 18px;
  font-weight: 600;
  margin: 14px 0 10px 0;
  color: var(--text-primary);
  padding-bottom: 4px;
  border-bottom: 1px solid var(--border-light);
}

.markdown-content :deep(h3) {
  font-size: var(--font-size-lg);
  font-weight: 600;
  margin: 12px 0 8px 0;
  color: var(--text-regular);
}

.markdown-content :deep(p) {
  margin: 8px 0;
  line-height: 1.6;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.markdown-content :deep(li) {
  margin: 4px 0;
  line-height: 1.5;
}

.markdown-content :deep(code) {
  background: var(--bg-input);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: "Consolas", "Monaco", "Courier New", monospace;
  font-size: var(--font-size-sm);
}

.markdown-content :deep(pre) {
  background: #1e1e1e;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #f8f8f2;
  font-size: var(--font-size-xs);
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: var(--font-size-sm);
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid var(--border-light);
  padding: 8px 10px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: var(--gradient-primary);
  color: white;
  font-weight: 600;
}

.markdown-content :deep(tr:nth-child(even)) {
  background: var(--bg-input);
}

@media (max-width: 1200px) {
  .cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .cards-grid {
    grid-template-columns: 1fr;
  }

  .header-banner {
    flex-direction: column;
    text-align: center;
  }

  .banner-title {
    font-size: var(--font-size-xl);
  }
}
</style>