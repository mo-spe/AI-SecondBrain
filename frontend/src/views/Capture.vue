<template>
  <div class="capture-container">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><Collection /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">数据捕捉</h1>
            <p class="welcome-subtitle">从多种渠道捕捉和收集知识</p>
          </div>
        </div>
      </div>

      <div class="capture-section">
        <div class="capture-card">
          <div class="capture-tabs-wrapper">
            <el-tabs v-model="activeTab" class="capture-tabs">
              <el-tab-pane label="文档上传" name="document">
                <div class="tab-content">
                  <div class="upload-section">
                    <el-upload
                      class="upload-demo"
                      drag
                      :auto-upload="false"
                      :on-change="handleDocumentUpload"
                      accept=".pdf,.doc,.docx,.txt,.xlsx,.xls"
                      :limit="1"
                    >
                      <el-icon class="el-icon--upload"
                        ><UploadFilled
                      /></el-icon>
                      <div class="el-upload__text">
                        将文件拖到此处，或<em>点击上传</em>
                      </div>
                      <template #tip>
                        <div class="el-upload__tip">
                          支持 PDF、Word、Excel、TXT 格式，文件大小不超过 10MB
                        </div>
                      </template>
                    </el-upload>

                    <div v-if="documentFile" class="file-info">
                      <div class="file-icon">
                        <el-icon><Document /></el-icon>
                      </div>
                      <div class="file-details">
                        <div class="file-name">{{ documentFile.name }}</div>
                        <div class="file-size">
                          {{ formatFileSize(documentFile.size) }}
                        </div>
                      </div>
                      <el-button
                        type="danger"
                        size="small"
                        @click="documentFile = null"
                        circle
                      >
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="笔记输入" name="note">
                <div class="tab-content">
                  <div class="note-section">
                    <el-form :model="noteForm" class="note-form">
                      <el-form-item>
                        <el-input
                          v-model="noteForm.title"
                          placeholder="笔记标题"
                          size="large"
                          maxlength="200"
                          show-word-limit
                        >
                          <template #prefix>
                            <el-icon><Edit /></el-icon>
                          </template>
                        </el-input>
                      </el-form-item>
                      <el-form-item>
                        <el-input
                          v-model="noteForm.content"
                          type="textarea"
                          :rows="12"
                          placeholder="输入笔记内容（支持Markdown格式）"
                          maxlength="10000"
                          show-word-limit
                        />
                      </el-form-item>
                      <el-form-item>
                        <el-button
                          type="primary"
                          size="large"
                          @click="handleNoteCapture"
                          :loading="noteLoading"
                          style="width: 100%"
                        >
                          <el-icon><Document /></el-icon>
                          捕捉笔记
                        </el-button>
                      </el-form-item>
                    </el-form>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  UploadFilled,
  Document,
  Delete,
  Collection,
  Edit,
} from "@element-plus/icons-vue";
import { captureAPI } from "@/api/capture";
import { userAPI } from "@/api/user";

const activeTab = ref("document");
const documentFile = ref(null);
const noteForm = ref({ title: "", content: "" });
const noteLoading = ref(false);
const userId = ref(null);

onMounted(async () => {
  try {
    const response = await userAPI.getUserInfo();
    if (response && response.id) {
      userId.value = response.id;
    }
  } catch (error) {
    ElMessage.error("获取用户信息失败");
  }
});

const handleDocumentUpload = async (file) => {
  const maxSize = 10 * 1024 * 1024;
  if (file.size > maxSize) {
    ElMessage.error("文件大小不能超过 10MB");
    return;
  }

  documentFile.value = file;

  try {
    await captureAPI.captureDocument(file.raw, userId.value);
    ElMessage.success("文档上传成功，正在提取知识点...");
    documentFile.value = null;
  } catch (error) {
    ElMessage.error("文档上传失败：" + (error.message || "未知错误"));
  }
};

const handleNoteCapture = async () => {
  if (!noteForm.value.title) {
    ElMessage.warning("请输入笔记标题");
    return;
  }

  if (!noteForm.value.content) {
    ElMessage.warning("请输入笔记内容");
    return;
  }

  noteLoading.value = true;

  try {
    await captureAPI.captureNote(
      noteForm.value.title,
      noteForm.value.content,
      userId.value,
    );
    ElMessage.success("笔记捕捉成功，正在提取知识点...");
    noteForm.value = { title: "", content: "" };
  } catch (error) {
    ElMessage.error("笔记捕捉失败：" + (error.message || "未知错误"));
  } finally {
    noteLoading.value = false;
  }
};

const formatFileSize = (bytes) => {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
};
</script>

<style scoped>
.capture-container {
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

.capture-section {
  margin-bottom: 20px;
}

.capture-card {
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

.capture-tabs-wrapper {
  padding: 0;
}

.tab-content {
  padding: 20px 0;
}

.upload-section {
  padding: 20px;
}

.upload-demo {
  width: 100%;
}

:deep(.el-upload-dragger) {
  border: 2px dashed #d0d0d0;
  border-radius: 12px;
  background: #fafafa;
  transition: all 0.3s;
}

:deep(.el-upload-dragger:hover) {
  border-color: #667eea;
  background: #f0f4ff;
}

:deep(.el-upload-dragger .el-icon--upload) {
  color: #667eea;
  font-size: 48px;
  margin-bottom: 16px;
}

:deep(.el-upload__text) {
  color: #606266;
  font-size: 14px;
}

:deep(.el-upload__text em) {
  color: #667eea;
  font-style: normal;
  font-weight: bold;
}

:deep(.el-upload__tip) {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
  margin-top: 20px;
  border: 2px solid #e0e0e0;
  transition: all 0.3s;
}

.file-info:hover {
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
}

.file-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.file-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  word-break: break-all;
}

.file-size {
  font-size: 12px;
  color: #909399;
}

.note-section {
  padding: 20px;
}

.note-form {
  max-width: 800px;
  margin: 0 auto;
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

:deep(.el-textarea__inner) {
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  transition: all 0.3s;
  font-size: 14px;
  line-height: 1.6;
}

:deep(.el-textarea__inner:hover) {
  border-color: #667eea;
}

:deep(.el-textarea__inner:focus) {
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

:deep(.el-button--danger) {
  border-radius: 50%;
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

:deep(.el-button--danger:hover) {
  transform: rotate(90deg);
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.4);
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
}

:deep(.el-input__count) {
  color: #909399;
  font-size: 12px;
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

  .capture-card {
    padding: 16px;
  }

  .tab-content {
    padding: 10px 0;
  }

  .upload-section,
  .note-section {
    padding: 10px;
  }

  .note-form {
    max-width: 100%;
  }

  :deep(.el-tabs__item) {
    padding: 0 15px;
    font-size: 14px;
  }
}
</style>
