<template>
  <div class="knowledge-container">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><Reading /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">知识管理</h1>
            <p class="welcome-subtitle">管理和组织您的知识体系</p>
          </div>
        </div>
      </div>

      <div class="toolbar-section">
        <div class="toolbar-card">
          <div class="toolbar-left">
            <div class="search-box">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索知识点..."
                clearable
                size="large"
                @clear="handleSearch"
                @keyup.enter="handleSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>

            <el-select
              v-model="filterImportance"
              placeholder="重要程度"
              size="large"
              style="width: 150px"
              clearable
              @change="handleSearch"
            >
              <el-option label="全部" value="" />
              <el-option label="非常重要" :value="5" />
              <el-option label="重要" :value="4" />
              <el-option label="一般" :value="3" />
              <el-option label="较低" :value="2" />
              <el-option label="很低" :value="1" />
            </el-select>

            <el-select
              v-model="filterMastery"
              placeholder="掌握程度"
              size="large"
              style="width: 150px"
              clearable
              @change="handleSearch"
            >
              <el-option label="全部" value="" />
              <el-option label="专家" :value="5" />
              <el-option label="精通" :value="4" />
              <el-option label="掌握" :value="3" />
              <el-option label="熟悉" :value="2" />
              <el-option label="入门" :value="1" />
              <el-option label="未掌握" :value="0" />
            </el-select>
          </div>

          <div class="toolbar-right">
            <el-button type="primary" size="large" @click="handleAddKnowledge">
              <el-icon><Plus /></el-icon>
              添加知识点
            </el-button>
          </div>
        </div>
      </div>

      <div class="content-section">
        <div class="content-card">
          <div class="card-header">
            <h3>
              <el-icon><Document /></el-icon>
              知识点列表
            </h3>
            <el-tag type="info">{{ pagination.total }}条</el-tag>
          </div>

          <div class="card-body">
            <div v-loading="loading" class="knowledge-grid">
              <div
                v-for="knowledge in knowledgeList"
                :key="knowledge.id"
                class="knowledge-item"
                @click="viewDetail(knowledge)"
              >
                <div class="knowledge-checkbox">
                  <el-checkbox
                    v-model="selectedKnowledgeIds"
                    :value="knowledge.id"
                    @click.stop
                  />
                </div>
                <div class="knowledge-header">
                  <div class="knowledge-title">{{ knowledge.title }}</div>
                  <div class="knowledge-actions">
                    <el-button
                      type="success"
                      size="small"
                      @click.stop="generateQuestion(knowledge)"
                      :loading="knowledge.generating"
                      title="生成题目"
                    >
                      <el-icon><Document /></el-icon>
                      <span style="margin-left: 4px">生成</span>
                    </el-button>
                    <el-button
                      type="primary"
                      size="small"
                      @click.stop="editKnowledge(knowledge)"
                    >
                      <el-icon><Edit /></el-icon>
                    </el-button>
                    <el-button
                      type="danger"
                      size="small"
                      @click.stop="deleteKnowledge(knowledge)"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>

                <div class="knowledge-summary">{{ knowledge.summary }}</div>

                <div class="knowledge-stats">
                  <div class="stat-row">
                    <el-icon><Star /></el-icon>
                    <span>重要程度</span>
                    <el-rate
                      v-model="knowledge.importance"
                      disabled
                      show-score
                      text-color="#ff9900"
                      :max="5"
                      size="small"
                    />
                  </div>
                  <div class="stat-row">
                    <el-icon><Medal /></el-icon>
                    <span>掌握程度</span>
                    <el-tag
                      :type="getMasteryType(knowledge.masteryLevel)"
                      effect="dark"
                      size="small"
                    >
                      {{ getMasteryText(knowledge.masteryLevel) }}
                    </el-tag>
                  </div>
                  <div class="stat-row">
                    <el-icon><Document /></el-icon>
                    <span>复习次数</span>
                    <el-badge
                      :value="knowledge.reviewCount"
                      :max="10"
                      type="warning"
                    />
                  </div>
                  <div class="stat-row">
                    <el-icon><Clock /></el-icon>
                    <span>下次复习</span>
                    <span class="time-text">{{
                      formatRelativeTime(knowledge.nextReviewTime)
                    }}</span>
                  </div>
                </div>
              </div>

              <el-empty
                v-if="!loading && knowledgeList.length === 0"
                description="暂无知识点"
                :image-size="150"
              />
            </div>

            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="pagination.current"
                v-model:page-size="pagination.size"
                :total="pagination.total"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                background
              />
            </div>
          </div>
        </div>
      </div>

      <div class="export-section">
        <div class="export-card">
          <div class="card-header">
            <h3>
              <el-icon><Download /></el-icon>
              数据导出
            </h3>
          </div>

          <div class="card-body">
            <div class="export-content">
              <p class="export-description">
                将您的知识点数据导出为不同格式，方便备份和分享。
              </p>
              <div class="export-mode">
                <el-radio-group v-model="exportMode" size="large">
                  <el-radio-button value="all">全部导出</el-radio-button>
                  <el-radio-button
                    value="selected"
                    :disabled="selectedKnowledgeIds.length === 0"
                  >
                    导出选中 ({{ selectedKnowledgeIds.length }})
                  </el-radio-button>
                </el-radio-group>
              </div>
              <div class="export-buttons">
                <el-button
                  type="primary"
                  size="large"
                  @click="handleExport('markdown')"
                  :loading="exportLoading.markdown"
                  :disabled="
                    exportMode === 'selected' &&
                    selectedKnowledgeIds.length === 0
                  "
                >
                  <el-icon><Document /></el-icon>
                  Markdown
                </el-button>
                <el-button
                  type="primary"
                  size="large"
                  @click="handleExport('pdf')"
                  :loading="exportLoading.pdf"
                  :disabled="
                    exportMode === 'selected' &&
                    selectedKnowledgeIds.length === 0
                  "
                >
                  <el-icon><Files /></el-icon>
                  PDF
                </el-button>
                <el-button
                  type="primary"
                  size="large"
                  @click="handleExport('word')"
                  :loading="exportLoading.word"
                  :disabled="
                    exportMode === 'selected' &&
                    selectedKnowledgeIds.length === 0
                  "
                >
                  <el-icon><Notebook /></el-icon>
                  Word
                </el-button>
                <el-button
                  type="primary"
                  size="large"
                  @click="handleExport('json')"
                  :loading="exportLoading.json"
                  :disabled="
                    exportMode === 'selected' &&
                    selectedKnowledgeIds.length === 0
                  "
                >
                  <el-icon><Tickets /></el-icon>
                  JSON
                </el-button>
                <el-button
                  type="primary"
                  size="large"
                  @click="handleExport('csv')"
                  :loading="exportLoading.csv"
                  :disabled="
                    exportMode === 'selected' &&
                    selectedKnowledgeIds.length === 0
                  "
                >
                  <el-icon><Grid /></el-icon>
                  CSV
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showDetailDialog"
      title="知识点详情"
      width="900px"
      class="detail-dialog"
    >
      <div v-if="currentKnowledge" class="detail-content">
        <div class="detail-header">
          <div class="detail-title">{{ currentKnowledge.title }}</div>
          <div class="detail-time">
            <el-icon><Clock /></el-icon>
            <span>{{ formatDate(currentKnowledge.createTime) }}</span>
          </div>
        </div>

        <div class="detail-body">
          <div class="detail-section">
            <h4>
              <el-icon><Star /></el-icon>
              重要程度
            </h4>
            <el-rate
              v-model="currentKnowledge.importance"
              disabled
              show-score
              text-color="#ff9900"
              :max="5"
              size="large"
            />
          </div>

          <div class="detail-section">
            <h4>
              <el-icon><Medal /></el-icon>
              掌握程度
            </h4>
            <el-progress
              :percentage="getMasteryPercentage(currentKnowledge.masteryLevel)"
              :color="getMasteryColor(currentKnowledge.masteryLevel)"
              :stroke-width="20"
            />
          </div>

          <div class="detail-section">
            <h4>
              <el-icon><Document /></el-icon>
              复习信息
            </h4>
            <div class="review-info">
              <div class="info-item">
                <span class="info-label">复习次数：</span>
                <span class="info-value"
                  >{{ currentKnowledge.reviewCount }}次</span
                >
              </div>
              <div class="info-item">
                <span class="info-label">下次复习：</span>
                <span class="info-value">{{
                  formatDate(currentKnowledge.nextReviewTime)
                }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4>
              <el-icon><ChatDotRound /></el-icon>
              摘要
            </h4>
            <div class="detail-text">{{ currentKnowledge.summary }}</div>
          </div>

          <div class="detail-section" v-if="currentKnowledge.contentMd">
            <h4>
              <el-icon><Document /></el-icon>
              内容
            </h4>
            <div class="detail-text">{{ currentKnowledge.contentMd }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDetailDialog = false" size="large">
            <el-icon><Close /></el-icon>
            关闭
          </el-button>
          <el-button
            type="primary"
            @click="editKnowledge(currentKnowledge)"
            size="large"
          >
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showEditDialog"
      :title="editForm.id ? '编辑知识点' : '添加知识点'"
      width="900px"
      class="edit-dialog"
    >
      <el-form
        :model="editForm"
        :rules="editRules"
        ref="editFormRef"
        label-width="100px"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="editForm.title"
            placeholder="请输入标题"
            maxlength="200"
            show-word-limit
            size="large"
          />
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="editForm.summary"
            type="textarea"
            :rows="4"
            placeholder="请输入摘要"
            maxlength="500"
            show-word-limit
            size="large"
          />
        </el-form-item>
        <el-form-item label="内容" prop="contentMd">
          <el-input
            v-model="editForm.contentMd"
            type="textarea"
            :rows="10"
            placeholder="请输入内容，支持Markdown格式..."
            maxlength="10000"
            show-word-limit
            size="large"
          />
        </el-form-item>
        <el-form-item label="重要程度" prop="importance">
          <el-rate
            v-model="editForm.importance"
            show-score
            text-color="#ff9900"
            :max="5"
            size="large"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditDialog = false" size="large">
            <el-icon><Close /></el-icon>
            取消
          </el-button>
          <el-button
            type="primary"
            @click="handleSave"
            :loading="saveLoading"
            size="large"
          >
            <el-icon><Check /></el-icon>
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { knowledgeAPI } from "@/api/knowledge";
import { reviewAPI } from "@/api/review";
import { exportAPI } from "@/api/export";
import {
  Reading,
  Search,
  Plus,
  Document,
  Star,
  Medal,
  Clock,
  Edit,
  Delete,
  Close,
  Check,
  ChatDotRound,
  Download,
  Files,
  Notebook,
  Tickets,
  Grid,
} from "@element-plus/icons-vue";

const loading = ref(false);
const saveLoading = ref(false);
const exportLoading = ref({
  markdown: false,
  pdf: false,
  word: false,
  json: false,
  csv: false,
});
const selectedKnowledgeIds = ref([]);
const exportMode = ref("all");
const searchKeyword = ref("");
const filterImportance = ref("");
const filterMastery = ref("");
const showDetailDialog = ref(false);
const showEditDialog = ref(false);
const showAddDialog = ref(false);

const knowledgeList = ref([]);
const currentKnowledge = ref(null);
const editFormRef = ref(null);

const editForm = ref({
  id: null,
  title: "",
  summary: "",
  contentMd: "",
  importance: 3,
});

const editRules = {
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  summary: [{ required: true, message: "请输入摘要", trigger: "blur" }],
};

const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
});

const loadKnowledgeList = async () => {
  try {
    loading.value = true;
    const params = {
      current: pagination.value.current,
      size: pagination.value.size,
    };
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value;
    }
    if (filterImportance.value !== "") {
      params.importance = filterImportance.value;
    }
    if (filterMastery.value !== "") {
      params.masteryLevel = filterMastery.value;
    }

    const data = await knowledgeAPI.getList(params);
    knowledgeList.value = (data.records || []).map((knowledge) => ({
      ...knowledge,
      generating: false,
    }));
    pagination.value.total = data.total || 0;
  } catch (error) {
    ElMessage.error("加载知识点列表失败：" + error.message);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.value.current = 1;
  loadKnowledgeList();
};

const handleSizeChange = (size) => {
  pagination.value.size = size;
  pagination.value.current = 1;
  loadKnowledgeList();
};

const handleCurrentChange = (current) => {
  pagination.value.current = current;
  loadKnowledgeList();
};

const getMasteryType = (level) => {
  const typeMap = {
    0: "danger",
    1: "warning",
    2: "info",
    3: "primary",
    4: "success",
    5: "success",
  };
  return typeMap[level] || "info";
};

const getMasteryText = (level) => {
  const textMap = {
    0: "未掌握",
    1: "入门",
    2: "熟悉",
    3: "掌握",
    4: "精通",
    5: "专家",
  };
  return textMap[level] || "未知";
};

const getMasteryPercentage = (level) => {
  return (level / 5) * 100;
};

const getMasteryColor = (level) => {
  const colorMap = {
    0: "#ff4d4f",
    1: "#ff6b6b",
    2: "#f56c6c",
    3: "#e6a23c",
    4: "#67c23a",
    5: "#409eff",
  };
  return colorMap[level] || "#909399";
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

const formatRelativeTime = (dateStr) => {
  if (!dateStr) return "-";
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return "-";
  const now = new Date();
  const diff = date - now;

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  if (minutes < 0) return "已过期";
  if (minutes < 60) return `${minutes}分钟后`;
  if (hours < 24) return `${hours}小时后`;
  if (days < 7) return `${days}天后`;
  return formatDate(dateStr);
};

const viewDetail = (knowledge) => {
  currentKnowledge.value = knowledge;
  showDetailDialog.value = true;
};

const editKnowledge = (knowledge) => {
  editForm.value = {
    id: knowledge.id,
    title: knowledge.title,
    summary: knowledge.summary,
    contentMd: knowledge.contentMd || "",
    importance: knowledge.importance,
  };
  showEditDialog.value = true;
};

const handleAddKnowledge = () => {
  editForm.value = {
    id: null,
    title: "",
    summary: "",
    contentMd: "",
    importance: 3,
  };
  showEditDialog.value = true;
};

const handleSave = async () => {
  if (!editFormRef.value) return;

  try {
    await editFormRef.value.validate();
    saveLoading.value = true;

    if (editForm.value.id) {
      await knowledgeAPI.updateKnowledge(editForm.value.id, editForm.value);
      ElMessage.success("更新成功");
    } else {
      await knowledgeAPI.createKnowledge(editForm.value);
      ElMessage.success("创建成功");
    }

    showEditDialog.value = false;
    loadKnowledgeList();
  } catch (error) {
    if (error !== false) {
      ElMessage.error("保存失败：" + error.message);
    }
  } finally {
    saveLoading.value = false;
  }
};

const deleteKnowledge = async (knowledge) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除知识点"${knowledge.title}"吗？`,
      "确认删除",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      },
    );

    await knowledgeAPI.deleteKnowledge(knowledge.id);
    ElMessage.success("删除成功");
    loadKnowledgeList();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("删除失败：" + error.message);
    }
  }
};

const generateQuestion = async (knowledge) => {
  try {
    console.log("开始生成题目，knowledge:", knowledge);
    ElMessage.info("正在生成题目...");

    knowledge.generating = true;

    const result = await reviewAPI.generateReviewCard({
      nodeId: knowledge.id,
      cardType: "choice",
    });

    console.log("生成题目结果:", result);

    ElMessage.success("生成题目成功");
  } catch (error) {
    console.error("生成题目失败:", error);
    ElMessage.error("生成题目失败：" + error.message);
  } finally {
    knowledge.generating = false;
  }
};

const handleExport = async (format) => {
  exportLoading.value[format] = true;
  try {
    let response;
    let filename;
    let mimeType;

    const ids =
      exportMode.value === "selected" ? selectedKnowledgeIds.value : [];

    switch (format) {
      case "markdown":
        response = await exportAPI.exportToMarkdown(ids);
        filename =
          ids.length > 0
            ? "knowledge_selected_export.md"
            : "knowledge_export.md";
        mimeType = "text/markdown";
        break;
      case "pdf":
        response = await exportAPI.exportToPDF(ids);
        filename =
          ids.length > 0
            ? "knowledge_selected_export.pdf"
            : "knowledge_export.pdf";
        mimeType = "application/pdf";
        break;
      case "word":
        response = await exportAPI.exportToWord(ids);
        filename =
          ids.length > 0
            ? "knowledge_selected_export.docx"
            : "knowledge_export.docx";
        mimeType =
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        break;
      case "json":
        response = await exportAPI.exportToJSON(ids);
        filename =
          ids.length > 0
            ? "knowledge_selected_export.json"
            : "knowledge_export.json";
        mimeType = "application/json";
        break;
      case "csv":
        response = await exportAPI.exportToCSV(ids);
        filename =
          ids.length > 0
            ? "knowledge_selected_export.csv"
            : "knowledge_export.csv";
        mimeType = "text/csv";
        break;
      default:
        throw new Error("不支持的导出格式");
    }

    const blob = new Blob([response], { type: mimeType });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);

    ElMessage.success("导出成功");
  } catch (error) {
    ElMessage.error("导出失败：" + error.message);
  } finally {
    exportLoading.value[format] = false;
  }
};

onMounted(() => {
  loadKnowledgeList();
});
</script>

<style scoped>
.knowledge-container {
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

.toolbar-section {
  margin-bottom: 20px;
}

.toolbar-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-box {
  width: 400px;
}

.content-section {
  margin-bottom: 20px;
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
  min-height: 400px;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.knowledge-item {
  background: white;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px 20px 20px 45px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: visible;
}

.knowledge-checkbox {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 10;
}

.knowledge-item:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  transform: translateY(-4px);
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.knowledge-title {
  flex: 1;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  line-height: 1.4;
  margin-right: 10px;
}

.knowledge-actions {
  display: flex;
  gap: 8px;
  z-index: 100;
  position: relative;
  pointer-events: auto;
}

.knowledge-actions .el-button {
  z-index: 101;
  position: relative;
  pointer-events: auto;
  cursor: pointer;
}

.knowledge-summary {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.knowledge-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
}

.stat-row .el-icon {
  color: #667eea;
}

.time-text {
  color: #667eea;
  font-weight: bold;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.export-section {
  margin-top: 20px;
}

.export-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease-out;
}

.export-content {
  text-align: center;
}

.export-description {
  font-size: 14px;
  color: #606266;
  margin-bottom: 20px;
}

.export-mode {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.export-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.export-buttons .el-button {
  min-width: 120px;
}

.detail-content {
  color: #303133;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.detail-title {
  flex: 1;
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  line-height: 1.4;
}

.detail-time {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 16px;
}

.detail-section h4 {
  font-size: 13px;
  color: #909399;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.detail-section h4 .el-icon {
  color: #667eea;
}

.review-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-label {
  color: #909399;
  font-size: 14px;
}

.info-value {
  color: #303133;
  font-size: 15px;
  font-weight: bold;
}

.detail-text {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

:deep(.el-empty) {
  background: transparent;
  color: #909399;
}

:deep(.el-empty__description p) {
  color: #909399;
}

:deep(.el-input__wrapper) {
  box-shadow: none;
  border: 2px solid #e0e0e0;
  transition: all 0.3s;
}

:deep(.el-input__wrapper:hover) {
  border-color: #667eea;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

:deep(.el-textarea__inner) {
  border: 2px solid #e0e0e0;
  transition: all 0.3s;
}

:deep(.el-textarea__inner:hover) {
  border-color: #667eea;
}

:deep(.el-textarea__inner:focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
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

@media (max-width: 768px) {
  .welcome-card {
    flex-direction: column;
    text-align: center;
  }

  .welcome-title {
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

  .search-box {
    width: 100%;
  }

  .knowledge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
