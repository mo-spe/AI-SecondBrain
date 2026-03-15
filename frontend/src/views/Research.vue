<template>
  <div class="research-container">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><TrendCharts /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">AI学习研究</h1>
            <p class="welcome-subtitle">
              基于DeerFlow多Agent系统的深度学习研究
            </p>
          </div>
        </div>
      </div>

      <div class="research-section">
        <div class="research-card">
          <div class="research-tabs-wrapper">
            <el-tabs v-model="activeTab" class="research-tabs">
              <el-tab-pane label="学习路径" name="path">
                <div class="tab-content">
                  <div class="path-section">
                    <el-form :model="pathForm" class="research-form">
                      <el-form-item label="学习主题">
                        <el-input
                          v-model="pathForm.topic"
                          placeholder="例如：Redis、Python、机器学习"
                          maxlength="100"
                          show-word-limit
                          size="large"
                        >
                          <template #prefix>
                            <el-icon><Reading /></el-icon>
                          </template>
                        </el-input>
                      </el-form-item>

                      <el-form-item label="当前水平">
                        <el-select
                          v-model="pathForm.currentLevel"
                          size="large"
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
                          size="large"
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
                          size="large"
                          @click="generateLearningPath"
                          :loading="generatingPath"
                          style="width: 100%"
                        >
                          <el-icon><MagicStick /></el-icon>
                          生成学习路径
                        </el-button>
                      </el-form-item>
                    </el-form>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="知识盲区" name="gaps">
                <div class="tab-content">
                  <div class="gaps-section">
                    <el-form :model="gapsForm" class="research-form">
                      <el-form-item label="目标主题">
                        <el-input
                          v-model="gapsForm.topic"
                          placeholder="例如：微服务架构、前端开发"
                          maxlength="100"
                          show-word-limit
                          size="large"
                        >
                          <template #prefix>
                            <el-icon><Aim /></el-icon>
                          </template>
                        </el-input>
                      </el-form-item>

                      <el-form-item label="已掌握的知识点">
                        <el-select
                          v-model="gapsForm.userKnowledge"
                          multiple
                          filterable
                          allow-create
                          placeholder="输入或选择已掌握的知识点"
                          size="large"
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
                          size="large"
                          @click="researchKnowledgeGaps"
                          :loading="analyzingGaps"
                          style="width: 100%"
                        >
                          <el-icon><Search /></el-icon>
                          分析知识盲区
                        </el-button>
                      </el-form-item>
                    </el-form>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="历史记录" name="history">
                <div class="tab-content">
                  <div class="history-section">
                    <div v-loading="loading" class="history-list">
                      <div
                        v-for="item in historyList"
                        :key="item.id"
                        class="history-item"
                        @click="viewHistory(item)"
                      >
                        <div class="history-header">
                          <div class="history-title">
                            <el-tag
                              :type="
                                item.type === 'path' ? 'primary' : 'success'
                              "
                              size="small"
                            >
                              {{
                                item.type === "path" ? "学习路径" : "知识盲区"
                              }}
                            </el-tag>
                            <span>{{ item.topic }}</span>
                          </div>
                          <div class="history-time">
                            <el-icon><Clock /></el-icon>
                            <span>{{ formatDate(item.createTime) }}</span>
                          </div>
                        </div>
                        <div class="history-meta">
                          <div class="meta-item">
                            <el-icon><Reading /></el-icon>
                            <span>{{
                              item.type === "path"
                                ? `${item.currentLevel} → ${item.targetLevel}`
                                : `${item.knowledgeCount}个知识点`
                            }}</span>
                          </div>
                          <el-button
                            type="danger"
                            size="small"
                            @click.stop="deleteHistory(item)"
                          >
                            <el-icon><Delete /></el-icon>
                          </el-button>
                        </div>
                      </div>

                      <el-empty
                        v-if="!loading && historyList.length === 0"
                        description="暂无历史记录"
                      />
                    </div>

                    <div
                      v-if="historyList.length > 0"
                      class="pagination-wrapper"
                    >
                      <el-pagination
                        v-model:current-page="pagination.current"
                        v-model:page-size="pagination.size"
                        :total="pagination.total"
                        :page-sizes="[10, 20, 50]"
                        layout="total, sizes, prev, pager, next, jumper"
                        @size-change="handleSizeChange"
                        @current-change="handleCurrentChange"
                      />
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>

      <el-dialog
        v-model="showPathDialog"
        title="学习路径"
        width="900px"
        class="result-dialog"
      >
        <div class="view-mode-toggle">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="simple">简洁模式</el-radio-button>
            <el-radio-button label="full">完整模式</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="learningPathResult" class="result-content">
          <div v-if="viewMode === 'simple'" class="simple-content">
            <div v-html="renderSimpleContent(learningPathResult)"></div>
          </div>
          <div
            v-else
            class="markdown-content"
            v-html="renderMarkdown(learningPathResult)"
          ></div>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showPathDialog = false" size="large">
              <el-icon><Close /></el-icon>
              关闭
            </el-button>
            <el-button type="primary" @click="exportPath" size="large">
              <el-icon><Download /></el-icon>
              导出路径
            </el-button>
          </div>
        </template>
      </el-dialog>

      <el-dialog
        v-model="showGapsDialog"
        title="知识盲区分析"
        width="900px"
        class="result-dialog"
      >
        <div class="view-mode-toggle">
          <el-radio-group v-model="gapsViewMode" size="small">
            <el-radio-button label="simple">简洁模式</el-radio-button>
            <el-radio-button label="full">完整模式</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="gapsAnalysisResult" class="result-content">
          <div v-if="gapsViewMode === 'simple'" class="simple-content">
            <div v-html="renderSimpleContent(gapsAnalysisResult)"></div>
          </div>
          <div
            v-else
            class="markdown-content"
            v-html="renderMarkdown(gapsAnalysisResult)"
          ></div>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showGapsDialog = false" size="large">
              <el-icon><Close /></el-icon>
              关闭
            </el-button>
            <el-button type="primary" @click="exportGaps" size="large">
              <el-icon><Download /></el-icon>
              导出分析
            </el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  TrendCharts,
  Reading,
  Aim,
  MagicStick,
  Search,
  Close,
  Download,
  Clock,
  Delete,
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

const activeTab = ref("path");
const viewMode = ref("simple");
const gapsViewMode = ref("simple");
const pathForm = ref({
  topic: "",
  currentLevel: "beginner",
  targetLevel: "advanced",
});
const gapsForm = ref({
  topic: "",
  userKnowledge: [],
});

const generatingPath = ref(false);
const analyzingGaps = ref(false);
const loading = ref(false);
const showPathDialog = ref(false);
const showGapsDialog = ref(false);
const learningPathResult = ref("");
const gapsAnalysisResult = ref("");
const historyList = ref([]);
const currentPathTask = ref(null);
const currentGapsTask = ref(null);
const pathTaskPolling = ref(null);
const gapsTaskPolling = ref(null);

const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
});

const commonKnowledge = [
  "Java基础",
  "Python基础",
  "Spring Boot",
  "MySQL数据库",
  "Redis",
  "RESTful API",
  "Git版本控制",
  "Linux基础",
  "Docker",
  "前端开发",
  "Vue.js",
  "React",
  "Node.js",
  "微服务架构",
  "消息队列",
  "分布式系统",
  "算法与数据结构",
];

const generateLearningPath = async () => {
  if (!pathForm.value.topic) {
    ElMessage.warning("请输入学习主题");
    return;
  }

  console.log("用户登录状态：", userStore.isLoggedIn());
  console.log(
    "用户token：",
    userStore.token ? userStore.token.substring(0, 20) + "..." : "null",
  );
  console.log("用户信息：", userStore.userInfo);

  generatingPath.value = true;

  try {
    const task = await deerFlowAPI.generateLearningPath(pathForm.value);
    currentPathTask.value = task;

    console.log("任务响应：", task);

    if (task.status === "COMPLETED") {
      learningPathResult.value = task.result;
      showPathDialog.value = true;
      ElMessage.success("学习路径生成完成！");

      if (userStore.isLoggedIn()) {
        await saveHistory({
          type: "path",
          topic: pathForm.value.topic,
          currentLevel: pathForm.value.currentLevel,
          targetLevel: pathForm.value.targetLevel,
          content: learningPathResult.value,
        });
      }
    } else {
      ElMessage.success("学习路径生成任务已创建，正在后台生成...");
      startPathTaskPolling(task.taskId);
    }
  } catch (error) {
    console.error("生成学习路径失败：", error);
    ElMessage.error("生成学习路径失败：" + (error.message || "未知错误"));
  } finally {
    generatingPath.value = false;
  }
};

const startPathTaskPolling = (taskId) => {
  if (pathTaskPolling.value) {
    clearInterval(pathTaskPolling.value);
  }

  pathTaskPolling.value = setInterval(async () => {
    try {
      const response = await request({
        url: `/deerflow/async-task/status/${taskId}`,
        method: "get",
      });

      if (response.status === "COMPLETED") {
        clearInterval(pathTaskPolling.value);
        pathTaskPolling.value = null;

        learningPathResult.value = response.result;
        showPathDialog.value = true;

        ElMessage.success("学习路径生成完成！");

        if (userStore.isLoggedIn()) {
          await saveHistory({
            type: "path",
            topic: pathForm.value.topic,
            currentLevel: pathForm.value.currentLevel,
            targetLevel: pathForm.value.targetLevel,
            content: learningPathResult.value,
          });
        }
      } else if (response.status === "FAILED") {
        clearInterval(pathTaskPolling.value);
        pathTaskPolling.value = null;
        currentPathTask.value = null;

        ElMessage.error(
          "学习路径生成失败：" + (response.errorMessage || "未知错误"),
        );
      }
    } catch (error) {
      console.error("查询任务状态失败：", error);
    }
  }, 3000);
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
    currentGapsTask.value = task;

    if (task.status === "COMPLETED") {
      gapsAnalysisResult.value = task.result;
      showGapsDialog.value = true;
      ElMessage.success("知识盲区分析完成！");

      if (userStore.isLoggedIn()) {
        await saveHistory({
          type: "gaps",
          topic: gapsForm.value.topic,
          userKnowledge: gapsForm.value.userKnowledge,
          knowledgeCount: gapsForm.value.userKnowledge.length,
          content: gapsAnalysisResult.value,
        });
      }
    } else {
      ElMessage.success("知识盲区分析任务已创建，正在后台分析...");
      startGapsTaskPolling(task.taskId);
    }
  } catch (error) {
    ElMessage.error("分析知识盲区失败：" + (error.message || "未知错误"));
  } finally {
    analyzingGaps.value = false;
  }
};

const startGapsTaskPolling = (taskId) => {
  if (gapsTaskPolling.value) {
    clearInterval(gapsTaskPolling.value);
  }

  gapsTaskPolling.value = setInterval(async () => {
    try {
      const response = await request({
        url: `/deerflow/async-task/status/${taskId}`,
        method: "get",
      });

      if (response.status === "COMPLETED") {
        clearInterval(gapsTaskPolling.value);
        gapsTaskPolling.value = null;

        gapsAnalysisResult.value = response.result;
        showGapsDialog.value = true;

        ElMessage.success("知识盲区分析完成！");

        if (userStore.isLoggedIn()) {
          await saveHistory({
            type: "gaps",
            topic: gapsForm.value.topic,
            userKnowledge: gapsForm.value.userKnowledge,
            knowledgeCount: gapsForm.value.userKnowledge.length,
            content: gapsAnalysisResult.value,
          });
        }
      } else if (response.status === "FAILED") {
        clearInterval(gapsTaskPolling.value);
        gapsTaskPolling.value = null;
        currentGapsTask.value = null;

        ElMessage.error(
          "知识盲区分析失败：" + (response.errorMessage || "未知错误"),
        );
      }
    } catch (error) {
      console.error("查询任务状态失败：", error);
    }
  }, 3000);
};

const saveHistory = async (historyData) => {
  console.log("saveHistory函数被调用，参数：", historyData);
  console.log("用户登录状态：", userStore.isLoggedIn());

  if (!userStore.isLoggedIn()) {
    console.log("用户未登录，返回");
    return;
  }

  console.log("准备调用API保存历史记录...");
  try {
    console.log("调用deerFlowAPI.saveResearchHistory，参数：", historyData);
    const result = await deerFlowAPI.saveResearchHistory(historyData);
    console.log("API调用成功，结果：", result);
    await loadHistoryList();
    console.log("历史记录列表已重新加载");
  } catch (error) {
    console.error("保存历史记录失败：", error);
    console.error("错误详情：", error.message, error.stack);
  }
};

const loadHistoryList = async () => {
  try {
    loading.value = true;
    const data = await deerFlowAPI.getResearchHistoryList({
      current: pagination.value.current,
      size: pagination.value.size,
    });

    historyList.value = (data.records || []).filter(
      (item) => item.type === "path" || item.type === "gaps",
    );
    pagination.value.total = historyList.value.length;
  } catch (error) {
    ElMessage.error("加载历史记录失败：" + error.message);
  } finally {
    loading.value = false;
  }
};

const viewHistory = (item) => {
  console.log("查看历史记录，原始内容类型：", typeof item.content);
  console.log(
    "查看历史记录，内容长度：",
    item.content ? item.content.length : 0,
  );
  console.log(
    "查看历史记录，内容前100字符：",
    item.content ? item.content.substring(0, 100) : "empty",
  );

  let content = item.content;

  try {
    const parsedContent = JSON.parse(content);
    if (typeof parsedContent === "string") {
      content = parsedContent;
    } else if (typeof parsedContent === "object") {
      content = JSON.stringify(parsedContent, null, 2);
    }
  } catch (e) {
    console.log("内容不是JSON格式，直接使用原始内容");
  }

  console.log("处理后内容类型：", typeof content);
  console.log("处理后内容前100字符：", content.substring(0, 100));

  if (item.type === "path") {
    learningPathResult.value = content;
    showPathDialog.value = true;
  } else {
    gapsAnalysisResult.value = content;
    showGapsDialog.value = true;
  }
};

const deleteHistory = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除这条${item.type === "path" ? "学习路径" : "知识盲区分析"}记录吗？`,
      "确认删除",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      },
    );

    await deerFlowAPI.deleteResearchHistory(item.id);
    ElMessage.success("删除成功");
    loadHistoryList();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("删除失败：" + error.message);
    }
  }
};

const handleSizeChange = (size) => {
  pagination.value.size = size;
  pagination.value.current = 1;
  loadHistoryList();
};

const handleCurrentChange = (current) => {
  pagination.value.current = current;
  loadHistoryList();
};

const formatDate = (dateString) => {
  if (!dateString) return "-";
  return new Date(dateString).toLocaleString("zh-CN");
};

const exportPath = () => {
  if (!learningPathResult.value) return;

  const blob = new Blob([learningPathResult.value], {
    type: "text/markdown",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${pathForm.value.topic}_学习路径.md`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);

  ElMessage.success("学习路径导出成功");
};

const exportGaps = () => {
  if (!gapsAnalysisResult.value) return;

  const blob = new Blob([gapsAnalysisResult.value], {
    type: "text/markdown",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${gapsForm.value.topic}_知识盲区分析.md`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);

  ElMessage.success("知识盲区分析导出成功");
};

const renderMarkdown = (content) => {
  if (!content) return "";
  const html = marked(content);
  return DOMPurify.sanitize(html);
};

const renderSimpleContent = (content) => {
  if (!content) return "";

  const html = marked(content);
  const sanitized = DOMPurify.sanitize(html);

  const tempDiv = document.createElement("div");
  tempDiv.innerHTML = sanitized;

  const simpleHtml = document.createElement("div");

  Array.from(tempDiv.children).forEach((child) => {
    if (
      child.tagName === "H1" ||
      child.tagName === "H2" ||
      child.tagName === "H3"
    ) {
      simpleHtml.appendChild(child.cloneNode(true));
    } else if (child.tagName === "UL" || child.tagName === "OL") {
      const simpleList = child.cloneNode(false);
      Array.from(child.children)
        .slice(0, 5)
        .forEach((li) => {
          simpleList.appendChild(li.cloneNode(true));
        });
      if (simpleList.children.length > 0) {
        simpleHtml.appendChild(simpleList);
      }
    } else if (child.tagName === "P") {
      const text = child.textContent.trim();
      if (text.length > 0 && text.length < 200) {
        simpleHtml.appendChild(child.cloneNode(true));
      }
    } else if (child.tagName === "BLOCKQUOTE") {
      simpleHtml.appendChild(child.cloneNode(true));
    }
  });

  return simpleHtml.innerHTML;
};

onMounted(() => {
  if (userStore.isLoggedIn()) {
    loadHistoryList();
  }
});

onUnmounted(() => {
  if (pathTaskPolling.value) {
    clearInterval(pathTaskPolling.value);
  }
  if (gapsTaskPolling.value) {
    clearInterval(gapsTaskPolling.value);
  }
});
</script>

<style scoped>
.research-container {
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

.research-section {
  margin-bottom: 20px;
}

.research-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
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

.research-tabs-wrapper {
  width: 100%;
}

.research-tabs {
  width: 100%;
}

.tab-content {
  padding: 20px 0;
}

.research-form {
  max-width: 600px;
  margin: 0 auto;
}

.result-content {
  max-height: 600px;
  overflow-y: auto;
}

.markdown-content {
  line-height: 1.7;
  color: #2c3e50;
  font-size: 14px;
  padding: 16px;
  background: #ffffff;
  border-radius: 6px;
}

.markdown-content :deep(h1) {
  font-size: 22px;
  font-weight: 700;
  margin: 16px 0 12px 0;
  color: #1a1a1a;
  padding-bottom: 6px;
  border-bottom: 2px solid #e0e0e0;
}

.markdown-content :deep(h2) {
  font-size: 18px;
  font-weight: 600;
  margin: 14px 0 10px 0;
  color: #2c3e50;
  padding-bottom: 4px;
  border-bottom: 1px solid #e0e0e0;
}

.markdown-content :deep(h3) {
  font-size: 16px;
  font-weight: 600;
  margin: 12px 0 8px 0;
  color: #34495e;
}

.markdown-content :deep(h4) {
  font-size: 15px;
  font-weight: 600;
  margin: 10px 0 6px 0;
  color: #34495e;
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

.markdown-content :deep(ul ul),
.markdown-content :deep(ol ol),
.markdown-content :deep(ul ol),
.markdown-content :deep(ol ul) {
  margin: 6px 0;
}

.markdown-content :deep(strong) {
  font-weight: 700;
  color: #1a1a1a;
}

.markdown-content :deep(em) {
  font-style: italic;
}

.markdown-content :deep(a) {
  color: #3498db;
  text-decoration: none;
  transition: color 0.2s;
}

.markdown-content :deep(a:hover) {
  color: #2980b9;
  text-decoration: underline;
}

.markdown-content :deep(blockquote) {
  border-left: 3px solid #3498db;
  padding: 10px 12px;
  margin: 12px 0;
  background: #f8f9fa;
  color: #6c757d;
  border-radius: 0 4px 4px 0;
  font-style: italic;
}

.markdown-content :deep(code) {
  background: #f1f3f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: "Consolas", "Monaco", "Courier New", monospace;
  font-size: 13px;
  color: #e83e8c;
}

.markdown-content :deep(pre) {
  background: #1e1e1e;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 12px 0;
  border: 1px solid #333;
}

.markdown-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #f8f8f2;
  font-size: 12px;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 13px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid #e0e0e0;
  padding: 8px 10px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  text-transform: uppercase;
  font-size: 12px;
}

.markdown-content :deep(tr:nth-child(even)) {
  background: #f8f9fa;
}

.markdown-content :deep(tr:hover) {
  background: #e9ecef;
}

.markdown-content :deep(hr) {
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent, #3498db, transparent);
  margin: 16px 0;
}

.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  margin: 10px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.markdown-content :deep(.task-list-item) {
  list-style: none;
  padding: 8px;
  margin: 6px 0;
  background: #f8f9fa;
  border-left: 3px solid #3498db;
  border-radius: 3px;
}

.markdown-content :deep(.highlight) {
  background: linear-gradient(120deg, #ffeaa7 0%, #fdcb6e 100%);
  padding: 2px 5px;
  border-radius: 3px;
  font-weight: 600;
}

.view-mode-toggle {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
  padding: 8px 0;
  border-bottom: 1px solid #e0e0e0;
}

.simple-content {
  padding: 16px;
  background: #ffffff;
  border-radius: 6px;
  line-height: 1.6;
}

.simple-content :deep(h1) {
  font-size: 20px;
  font-weight: 700;
  margin: 12px 0 8px 0;
  color: #1a1a1a;
}

.simple-content :deep(h2) {
  font-size: 17px;
  font-weight: 600;
  margin: 10px 0 6px 0;
  color: #2c3e50;
}

.simple-content :deep(h3) {
  font-size: 15px;
  font-weight: 600;
  margin: 8px 0 4px 0;
  color: #34495e;
}

.simple-content :deep(p) {
  margin: 6px 0;
  color: #2c3e50;
}

.simple-content :deep(ul),
.simple-content :deep(ol) {
  padding-left: 18px;
  margin: 6px 0;
}

.simple-content :deep(li) {
  margin: 3px 0;
  color: #2c3e50;
}

.simple-content :deep(blockquote) {
  border-left: 3px solid #3498db;
  padding: 8px 10px;
  margin: 8px 0;
  background: #f8f9fa;
  color: #6c757d;
  border-radius: 0 3px 3px 0;
  font-style: italic;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.history-section {
  padding: 20px 0;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.history-item {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.history-item:hover {
  background: #fff;
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
  transform: translateY(-2px);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.history-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.history-time {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #7f8c8d;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #7f8c8d;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 768px) {
  .welcome-card {
    flex-direction: column;
    text-align: center;
  }

  .welcome-title {
    font-size: 24px;
  }

  .research-form {
    max-width: 100%;
  }

  .history-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .history-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
