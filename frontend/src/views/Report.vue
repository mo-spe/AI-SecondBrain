<template>
  <div class="report-container">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><Document /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">学习报告</h1>
            <p class="welcome-subtitle">基于AI深度分析的学习报告生成</p>
          </div>
        </div>
      </div>

      <div class="report-section">
        <div class="report-card">
          <div class="report-tabs-wrapper">
            <el-tabs v-model="activeTab" class="report-tabs">
              <el-tab-pane label="生成报告" name="generate">
                <div class="tab-content">
                  <div class="generate-section">
                    <el-form :model="reportForm" class="report-form">
                      <el-form-item label="学习主题">
                        <el-input
                          v-model="reportForm.topic"
                          placeholder="例如：Python编程学习、机器学习基础"
                          maxlength="100"
                          show-word-limit
                          size="large"
                        >
                          <template #prefix>
                            <el-icon><Edit /></el-icon>
                          </template>
                        </el-input>
                      </el-form-item>

                      <el-form-item label="时间范围">
                        <el-select
                          v-model="reportForm.days"
                          size="large"
                          style="width: 100%"
                        >
                          <el-option label="最近1天" :value="1" />
                          <el-option label="最近7天" :value="7" />
                          <el-option label="最近30天" :value="30" />
                          <el-option label="最近90天" :value="90" />
                          <el-option label="全部时间" :value="365" />
                        </el-select>
                      </el-form-item>

                      <el-form-item>
                        <el-button
                          type="primary"
                          size="large"
                          @click="generateReport"
                          :loading="generating"
                          style="width: 100%"
                        >
                          <el-icon><MagicStick /></el-icon>
                          生成学习报告
                        </el-button>
                      </el-form-item>
                    </el-form>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="历史报告" name="history">
                <div class="tab-content">
                  <div class="history-section">
                    <div v-loading="loading" class="report-list">
                      <div
                        v-for="report in reportList"
                        :key="report.id"
                        class="report-item"
                        @click="viewReport(report)"
                      >
                        <div class="report-header">
                          <div class="report-title">{{ report.topic }}</div>
                          <div class="report-time">
                            <el-icon><Clock /></el-icon>
                            <span>{{ formatDate(report.createTime) }}</span>
                          </div>
                        </div>
                        <div class="report-meta">
                          <el-tag type="info" size="small">
                            {{ report.days }}天
                          </el-tag>
                          <el-button
                            type="danger"
                            size="small"
                            @click.stop="deleteReport(report)"
                          >
                            <el-icon><Delete /></el-icon>
                          </el-button>
                        </div>
                      </div>

                      <el-empty
                        v-if="!loading && reportList.length === 0"
                        description="暂无学习报告"
                      />
                    </div>

                    <div
                      v-if="reportList.length > 0"
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
        v-model="showDetailDialog"
        title="学习报告详情"
        width="900px"
        class="detail-dialog"
      >
        <div v-if="currentReport" class="detail-content">
          <div class="detail-header">
            <div class="detail-title">{{ currentReport.topic }}</div>
            <div class="detail-time">
              <el-icon><Clock /></el-icon>
              <span>{{ formatDate(currentReport.createTime) }}</span>
            </div>
          </div>

          <div class="detail-body">
            <div
              class="markdown-content"
              v-html="renderMarkdown(currentReport.content)"
            ></div>
          </div>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showDetailDialog = false" size="large">
              <el-icon><Close /></el-icon>
              关闭
            </el-button>
            <el-button type="primary" @click="exportReport" size="large">
              <el-icon><Download /></el-icon>
              导出报告
            </el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Document,
  Edit,
  MagicStick,
  Clock,
  Delete,
  Close,
  Download,
} from "@element-plus/icons-vue";
import { reportAPI } from "@/api/report";
import { marked } from "marked";

const activeTab = ref("generate");
const reportForm = ref({
  topic: "",
  days: 30,
});
const generating = ref(false);
const loading = ref(false);
const showDetailDialog = ref(false);
const currentReport = ref(null);
const reportList = ref([]);

const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
});

const generateReport = async () => {
  if (!reportForm.value.topic) {
    ElMessage.warning("请输入学习主题");
    return;
  }

  generating.value = true;

  try {
    const report = await reportAPI.generateReport(reportForm.value);
    ElMessage.success("学习报告生成成功");
    currentReport.value = {
      topic: reportForm.value.topic,
      content: report,
      createTime: new Date(),
      days: reportForm.value.days,
    };
    showDetailDialog.value = true;
    reportForm.value.topic = "";
  } catch (error) {
    ElMessage.error("生成学习报告失败：" + (error.message || "未知错误"));
  } finally {
    generating.value = false;
  }
};

const loadReportList = async () => {
  try {
    loading.value = true;
    const data = await reportAPI.getReportList({
      current: pagination.value.current,
      size: pagination.value.size,
    });
    reportList.value = data.records || [];
    pagination.value.total = data.total || 0;
  } catch (error) {
    ElMessage.error("加载报告列表失败：" + error.message);
  } finally {
    loading.value = false;
  }
};

const viewReport = (report) => {
  currentReport.value = report;
  showDetailDialog.value = true;
};

const deleteReport = async (report) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除报告"${report.topic}"吗？`,
      "确认删除",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      },
    );

    await reportAPI.deleteReport(report.id);
    ElMessage.success("删除成功");
    loadReportList();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("删除失败：" + error.message);
    }
  }
};

const exportReport = () => {
  if (!currentReport.value) return;

  const blob = new Blob([currentReport.value.content], {
    type: "text/markdown",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${currentReport.value.topic}_学习报告.md`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);

  ElMessage.success("报告导出成功");
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

const renderMarkdown = (content) => {
  if (!content) return "";
  return marked(content);
};

const handleSizeChange = (size) => {
  pagination.value.size = size;
  pagination.value.current = 1;
  loadReportList();
};

const handleCurrentChange = (current) => {
  pagination.value.current = current;
  loadReportList();
};

onMounted(() => {
  loadReportList();
});
</script>

<style scoped>
.report-container {
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

.report-section {
  margin-bottom: 20px;
}

.report-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
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

.report-tabs-wrapper {
  padding: 0;
}

.tab-content {
  padding: 20px 0;
}

.generate-section,
.history-section {
  padding: 20px;
}

.report-form {
  max-width: 600px;
  margin: 0 auto;
}

.report-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 400px;
}

.report-item {
  background: white;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.report-item:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  transform: translateY(-4px);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.report-title {
  flex: 1;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  line-height: 1.4;
}

.report-time {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
}

.report-time .el-icon {
  color: #667eea;
}

.report-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
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

.detail-time .el-icon {
  color: #667eea;
}

.detail-body {
  max-height: 600px;
  overflow-y: auto;
}

.markdown-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.8;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3 {
  color: #303133;
  margin-top: 20px;
  margin-bottom: 12px;
}

.markdown-content h1 {
  font-size: 24px;
  border-bottom: 2px solid #667eea;
  padding-bottom: 8px;
}

.markdown-content h2 {
  font-size: 20px;
}

.markdown-content h3 {
  font-size: 18px;
}

.markdown-content p {
  margin-bottom: 12px;
}

.markdown-content ul,
.markdown-content ol {
  margin-bottom: 12px;
  padding-left: 20px;
}

.markdown-content li {
  margin-bottom: 8px;
}

.markdown-content strong {
  color: #667eea;
  font-weight: 600;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  padding: 0 30px;
  transition: all 0.3s;
}

:deep(.el-tabs__item:hover) {
  color: #667eea;
}

:deep(.el-tabs__item.is-active) {
  color: #667eea;
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  height: 3px;
  border-radius: 2px;
}

:deep(.el-input__wrapper) {
  box-shadow: none;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  transition: all 0.3s;
}

:deep(.el-input__wrapper:hover) {
  border-color: #667eea;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

:deep(.el-input__inner) {
  font-size: 14px;
}

:deep(.el-input__prefix) {
  color: #667eea;
}

:deep(.el-select .el-input__wrapper) {
  box-shadow: none;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  transition: all 0.3s;
}

:deep(.el-select .el-input__wrapper:hover) {
  border-color: #667eea;
}

:deep(.el-select .el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 10px;
  padding: 12px 24px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

:deep(.el-button--primary:active) {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .welcome-card {
    flex-direction: column;
    text-align: center;
    padding: 20px;
  }

  .welcome-title {
    font-size: 24px;
  }

  .welcome-subtitle {
    font-size: 14px;
  }

  .report-card {
    padding: 16px;
  }

  .tab-content {
    padding: 10px 0;
  }

  .generate-section,
  .history-section {
    padding: 10px;
  }

  .report-form {
    max-width: 100%;
  }

  :deep(.el-tabs__item) {
    padding: 0 15px;
    font-size: 14px;
  }
}
</style>
