<template>
  <div class="chat-container">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><ChatDotRound /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">对话采集</h1>
            <p class="welcome-subtitle">收集和管理您的AI对话记录</p>
          </div>
        </div>
      </div>

      <div class="toolbar-section">
        <div class="toolbar-card">
          <div class="toolbar-left">
            <div class="search-box">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索对话内容..."
                clearable
                size="large"
                @keyup.enter="handleSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>

            <el-select
              v-model="filterPlatform"
              placeholder="选择平台"
              size="large"
              style="width: 150px"
              clearable
              @change="handleSearch"
            >
              <el-option label="全部平台" value="" />
              <el-option label="ChatGPT" value="chatgpt.com" />
              <el-option label="ChatGPT (OpenAI)" value="chat.openai.com" />
              <el-option label="DeepSeek" value="chat.deepseek.com" />
              <el-option label="Kimi (官网)" value="www.kimi.com" />
              <el-option label="Kimi (Moonshot)" value="kimi.moonshot.cn" />
              <el-option label="Kimi (AI)" value="kimi.ai" />
              <el-option label="豆包" value="www.doubao.com" />
              <el-option label="智谱" value="www.zhipuai.cn" />
              <el-option label="通义千问" value="www.qianwen.com" />
              <el-option label="其他" value="Other" />
            </el-select>
          </div>

          <div class="toolbar-right">
            <el-button
              type="primary"
              size="large"
              @click="showCollectDialog = true"
            >
              <el-icon><Plus /></el-icon>
              采集对话
            </el-button>
          </div>
        </div>
      </div>

      <div class="content-section">
        <div class="content-card">
          <div class="card-header">
            <h3>
              <el-icon><Document /></el-icon>
              对话列表
            </h3>
            <el-tag type="info">{{ pagination.total }}条</el-tag>
          </div>

          <div class="card-body">
            <div v-loading="loading" class="chat-list">
              <div
                v-for="chat in chatList"
                :key="chat.id"
                class="chat-item"
                @click="viewDetail(chat)"
              >
                <div class="chat-header">
                  <div class="chat-platform">
                    <el-tag
                      :type="getPlatformType(chat.platform)"
                      effect="dark"
                      size="large"
                    >
                      {{ getPlatformName(chat.platform) }}
                    </el-tag>
                  </div>
                  <div class="chat-time">
                    <el-icon><Clock /></el-icon>
                    <span>{{ formatRelativeTime(chat.createTime) }}</span>
                  </div>
                </div>

                <div class="chat-content">
                  <div class="content-text">{{ chat.content }}</div>
                </div>

                <div class="chat-footer">
                  <div class="chat-meta">
                    <span class="meta-item">
                      <el-icon><Document /></el-icon>
                      ID: {{ chat.id }}
                    </span>
                    <span class="meta-item" v-if="chat.sourceUrl">
                      <el-icon><Link /></el-icon>
                      有来源链接
                    </span>
                  </div>
                  <el-button
                    type="primary"
                    size="small"
                    @click.stop="viewDetail(chat)"
                  >
                    <el-icon><View /></el-icon>
                    查看详情
                  </el-button>
                </div>
              </div>

              <el-empty
                v-if="!loading && chatList.length === 0"
                description="暂无对话数据"
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
    </div>

    <el-dialog
      v-model="showCollectDialog"
      title="采集对话"
      width="700px"
      class="collect-dialog"
    >
      <el-form
        :model="collectForm"
        :rules="collectRules"
        ref="collectFormRef"
        label-width="100px"
      >
        <el-form-item label="平台" prop="platform">
          <el-select
            v-model="collectForm.platform"
            placeholder="请选择平台"
            size="large"
            style="width: 100%"
          >
            <el-option label="ChatGPT" value="ChatGPT">
              <div class="option-item">
                <el-icon><ChatDotRound /></el-icon>
                <span>ChatGPT</span>
              </div>
            </el-option>
            <el-option label="DeepSeek" value="DeepSeek">
              <div class="option-item">
                <el-icon><ChatDotRound /></el-icon>
                <span>DeepSeek</span>
              </div>
            </el-option>
            <el-option label="Kimi" value="Kimi">
              <div class="option-item">
                <el-icon><ChatDotRound /></el-icon>
                <span>Kimi</span>
              </div>
            </el-option>
            <el-option label="其他" value="Other">
              <div class="option-item">
                <el-icon><ChatDotRound /></el-icon>
                <span>其他</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="对话内容" prop="content">
          <el-input
            v-model="collectForm.content"
            type="textarea"
            :rows="10"
            placeholder="请输入对话内容，支持Markdown格式..."
            maxlength="5000"
            show-word-limit
            size="large"
          />
        </el-form-item>
        <el-form-item label="来源链接" prop="sourceUrl">
          <el-input
            v-model="collectForm.sourceUrl"
            placeholder="请输入来源URL（可选）"
            size="large"
          >
            <template #prefix>
              <el-icon><Link /></el-icon>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCollectDialog = false" size="large">
            <el-icon><Close /></el-icon>
            取消
          </el-button>
          <el-button
            type="primary"
            @click="handleCollect"
            :loading="collectLoading"
            size="large"
          >
            <el-icon><Check /></el-icon>
            提交采集
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showDetailDialog"
      title="对话详情"
      width="900px"
      class="detail-dialog"
    >
      <div v-if="currentChat" class="detail-content">
        <div class="detail-header">
          <div class="detail-platform">
            <el-tag
              :type="getPlatformType(currentChat.platform)"
              effect="dark"
              size="large"
            >
              {{ getPlatformName(currentChat.platform) }}
            </el-tag>
          </div>
          <div class="detail-time">
            <el-icon><Clock /></el-icon>
            <span>{{ formatDate(currentChat.createTime) }}</span>
          </div>
        </div>

        <div class="detail-body">
          <div class="detail-section">
            <h4>
              <el-icon><Document /></el-icon>
              对话ID
            </h4>
            <div class="detail-value">{{ currentChat.id }}</div>
          </div>

          <div class="detail-section" v-if="currentChat.sourceUrl">
            <h4>
              <el-icon><Link /></el-icon>
              来源链接
            </h4>
            <el-link
              :href="currentChat.sourceUrl"
              target="_blank"
              type="primary"
            >
              {{ currentChat.sourceUrl }}
            </el-link>
          </div>

          <div class="detail-section">
            <h4>
              <el-icon><ChatDotRound /></el-icon>
              对话内容
            </h4>
            <div class="detail-text">{{ currentChat.content }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDetailDialog = false" size="large">
            <el-icon><Close /></el-icon>
            关闭
          </el-button>
          <el-button type="primary" @click="editChat" size="large">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { chatAPI } from "@/api/chat";
import {
  ChatDotRound,
  Search,
  Plus,
  Document,
  Clock,
  Link,
  View,
  Close,
  Check,
  Edit,
} from "@element-plus/icons-vue";

const loading = ref(false);
const collectLoading = ref(false);
const searchKeyword = ref("");
const filterPlatform = ref("");
const showCollectDialog = ref(false);
const showDetailDialog = ref(false);

const chatList = ref([]);
const currentChat = ref(null);
const collectFormRef = ref(null);

const collectForm = ref({
  platform: "",
  content: "",
  sourceUrl: "",
});

const collectRules = {
  platform: [{ required: true, message: "请选择平台", trigger: "change" }],
  content: [{ required: true, message: "请输入对话内容", trigger: "blur" }],
};

const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
});

const loadChatList = async () => {
  try {
    loading.value = true;
    const params = {
      current: pagination.value.current,
      size: pagination.value.size,
    };
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value;
    }
    if (filterPlatform.value) {
      params.platform = filterPlatform.value;
    }

    const data = await chatAPI.getList(params);
    chatList.value = data.records || [];
    pagination.value.total = data.total || 0;
  } catch (error) {
    ElMessage.error("加载对话列表失败：" + error.message);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.value.current = 1;
  loadChatList();
};

const handleSizeChange = (size) => {
  pagination.value.size = size;
  pagination.value.current = 1;
  loadChatList();
};

const handleCurrentChange = (current) => {
  pagination.value.current = current;
  loadChatList();
};

const getPlatformType = (platform) => {
  const typeMap = {
    "chatgpt.com": "primary",
    "chat.openai.com": "primary",
    "chat.deepseek.com": "success",
    "www.kimi.com": "warning",
    "kimi.moonshot.cn": "warning",
    "kimi.ai": "warning",
    "www.doubao.com": "danger",
    "www.zhipuai.cn": "info",
    "www.qianwen.com": "info",
    Other: "info",
  };
  return typeMap[platform] || "info";
};

const getPlatformName = (platform) => {
  const nameMap = {
    "chatgpt.com": "ChatGPT",
    "chat.openai.com": "ChatGPT (OpenAI)",
    "chat.deepseek.com": "DeepSeek",
    "www.kimi.com": "Kimi (官网)",
    "kimi.moonshot.cn": "Kimi (Moonshot)",
    "kimi.ai": "Kimi (AI)",
    "www.doubao.com": "豆包",
    "www.zhipuai.cn": "智谱",
    "www.qianwen.com": "通义千问",
    Other: "其他",
  };
  return nameMap[platform] || platform;
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

const formatRelativeTime = (dateStr) => {
  if (!dateStr) return "-";
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now - date;

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return formatDate(dateStr);
};

const viewDetail = (chat) => {
  currentChat.value = chat;
  showDetailDialog.value = true;
};

const handleCollect = async () => {
  if (!collectFormRef.value) return;

  try {
    await collectFormRef.value.validate();
    collectLoading.value = true;

    await chatAPI.collect(collectForm.value);
    ElMessage.success("采集成功");
    showCollectDialog.value = false;
    collectForm.value = {
      platform: "",
      content: "",
      sourceUrl: "",
    };
    loadChatList();
  } catch (error) {
    if (error !== false) {
      ElMessage.error("采集失败：" + error.message);
    }
  } finally {
    collectLoading.value = false;
  }
};

const editChat = () => {
  ElMessage.info("编辑功能开发中");
};

onMounted(() => {
  loadChatList();
});
</script>

<style scoped>
.chat-container {
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

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 600px;
  overflow-y: auto;
}

.chat-item {
  background: white;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.chat-item:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  transform: translateX(4px);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chat-platform {
  flex: 1;
}

.chat-time {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
}

.chat-content {
  margin-bottom: 12px;
}

.content-text {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.chat-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.chat-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
}

.meta-item .el-icon {
  color: #667eea;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-content {
  color: #303133;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.detail-platform {
  flex: 1;
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

.detail-value {
  color: #303133;
  font-size: 16px;
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
}
</style>
