<template>
  <div class="capture-page">
    <div class="main-content">
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">数据采集中心</h1>
          <span class="page-subtitle">收集和管理您的对话记录与知识数据</span>
        </div>
      </div>

      <div class="content-wrapper">
        <div class="left-panel">
          <div class="panel-card">
            <div class="panel-header">
              <div class="panel-icon purple">
                <el-icon size="20"><ChatDotRound /></el-icon>
              </div>
              <div class="panel-title-area">
                <h2 class="panel-title">对话采集</h2>
                <span class="panel-subtitle">收集和管理您的AI对话记录</span>
              </div>
            </div>

            <div class="filter-bar">
              <div class="search-wrapper">
                <el-icon size="14" color="#94a3b8"><Search /></el-icon>
                <input
                  type="text"
                  v-model="searchKeyword"
                  placeholder="搜索对话内容..."
                  @keyup.enter="handleSearch"
                />
              </div>
              <el-select
                v-model="filterPlatform"
                placeholder="选择平台"
                size="small"
                clearable
                @change="handleSearch"
                style="width: 140px"
              >
                <el-option label="全部平台" value="" />
                <el-option label="ChatGPT" value="chatgpt.com" />
                <el-option label="ChatGPT (OpenAI)" value="chat.openai.com" />
                <el-option label="DeepSeek" value="chat.deepseek.com" />
                <el-option label="Kimi" value="kimi.moonshot.cn" />
                <el-option label="豆包" value="www.doubao.com" />
                <el-option label="智谱" value="www.zhipuai.cn" />
                <el-option label="通义千问" value="www.qianwen.com" />
                <el-option label="其他" value="Other" />
              </el-select>
              <el-button type="primary" size="small" @click="showCollectDialog = true">
                <el-icon size="14"><Plus /></el-icon>
                <span>采集对话</span>
              </el-button>
            </div>

            <div class="chat-list-wrapper">
              <div class="list-header">
                <span class="list-title">对话列表</span>
                <span class="list-count">{{ pagination.total }}件</span>
              </div>
              <div v-loading="loading" class="chat-list">
                <div
                  v-for="chat in chatList"
                  :key="chat.id"
                  class="chat-item"
                  @click="viewDetail(chat)"
                >
                  <div class="chat-platform-tag">
                    <el-tag
                      :type="getPlatformTagType(chat.platform)"
                      size="small"
                    >
                      {{ getPlatformName(chat.platform) }}
                    </el-tag>
                  </div>
                  <div class="chat-preview">{{ chat.content }}</div>
                  <div class="chat-item-footer">
                    <span class="chat-id">ID: {{ chat.id }}</span>
                    <span class="chat-time">{{ formatDate(chat.createTime) }}</span>
                    <el-button
                      type="primary"
                      size="small"
                      @click.stop="viewDetail(chat)"
                    >
                      查看详情
                    </el-button>
                  </div>
                </div>
                <el-empty
                  v-if="!loading && chatList.length === 0"
                  description="暂无对话数据"
                  :image-size="80"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="right-panel">
          <div class="panel-card">
            <div class="panel-header">
              <div class="panel-icon blue">
                <el-icon size="20"><UploadFilled /></el-icon>
              </div>
              <div class="panel-title-area">
                <h2 class="panel-title">数据捕捉</h2>
                <span class="panel-subtitle">从多种渠道捕捉和收集知识</span>
              </div>
            </div>

            <div class="capture-tabs">
              <el-tabs v-model="activeTab" class="simple-tabs">
                <el-tab-pane label="文档上传" name="document">
                  <div class="tab-content">
                    <div class="upload-area">
                      <el-upload
                        class="upload-demo"
                        drag
                        :auto-upload="true"
                        :http-request="customUpload"
                        accept=".pdf,.doc,.docx,.txt,.xlsx,.xls"
                        :limit="1"
                      >
                        <div class="upload-icon">
                          <svg viewBox="0 0 100 100" class="cloud-icon">
                            <defs>
                              <linearGradient id="cloudGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                                <stop offset="0%" style="stop-color:#7c3aed;stop-opacity:1" />
                                <stop offset="100%" style="stop-color:#a855f7;stop-opacity:1" />
                              </linearGradient>
                            </defs>
                            <ellipse cx="35" cy="55" rx="25" ry="18" fill="url(#cloudGrad)" />
                            <ellipse cx="55" cy="50" rx="30" ry="22" fill="url(#cloudGrad)" />
                            <ellipse cx="75" cy="55" rx="20" ry="15" fill="url(#cloudGrad)" />
                            <polygon points="45,45 50,35 55,45" fill="#a855f7" />
                            <circle cx="52" cy="55" r="4" fill="#fff" opacity="0.8" />
                          </svg>
                        </div>
                        <div class="upload-text">拖拽文件到此处，或点击上传</div>
                        <div class="upload-tip">支持 PDF、Word、Excel、TXT 格式，文件大小不超过10MB</div>
                      </el-upload>
                    </div>
                    <div v-if="documentFile" class="file-preview">
                      <div class="file-icon-box">
                        <el-icon size="24"><Document /></el-icon>
                      </div>
                      <div class="file-info">
                        <div class="file-name">{{ documentFile.name }}</div>
                        <div class="file-size">{{ formatFileSize(documentFile.size) }}</div>
                      </div>
                      <el-button type="danger" size="small" circle @click="documentFile = null">
                        <el-icon size="14"><Delete /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </el-tab-pane>
                <el-tab-pane label="笔记输入" name="note">
                  <div class="tab-content">
                    <el-form :model="noteForm" class="note-form">
                      <el-form-item>
                        <el-input
                          v-model="noteForm.title"
                          placeholder="笔记标题"
                          size="large"
                          maxlength="200"
                          show-word-limit
                        />
                      </el-form-item>
                      <el-form-item>
                        <el-input
                          v-model="noteForm.content"
                          type="textarea"
                          :rows="8"
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
                </el-tab-pane>
              </el-tabs>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showCollectDialog"
      title="采集对话"
      width="600px"
      class="collect-dialog"
    >
      <el-form
        :model="collectForm"
        :rules="collectRules"
        ref="collectFormRef"
        label-width="80px"
      >
        <el-form-item label="平台" prop="platform">
          <el-select
            v-model="collectForm.platform"
            placeholder="请选择平台"
            size="large"
            style="width: 100%"
          >
            <el-option label="ChatGPT" value="ChatGPT" />
            <el-option label="DeepSeek" value="DeepSeek" />
            <el-option label="Kimi" value="Kimi" />
            <el-option label="豆包" value="豆包" />
            <el-option label="智谱" value="智谱" />
            <el-option label="通义千问" value="通义千问" />
            <el-option label="其他" value="Other" />
          </el-select>
        </el-form-item>
        <el-form-item label="对话内容" prop="content">
          <el-input
            v-model="collectForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入对话内容..."
            maxlength="5000"
            show-word-limit
            size="large"
          />
        </el-form-item>
        <el-form-item label="来源链接">
          <el-input
            v-model="collectForm.sourceUrl"
            placeholder="请输入来源URL（可选）"
            size="large"
          />
        </el-form-item>

        <el-divider />

        <el-form-item label="目标工作区">
          <el-select
            v-model="collectForm.workspaceId"
            placeholder="个人空间"
            size="large"
            style="width: 100%"
            clearable
          >
            <el-option label="个人空间" :value="null" />
            <el-option
              v-for="ws in workspaceStore.workspaces"
              :key="ws.id"
              :label="ws.name"
              :value="ws.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="提取知识点">
          <el-switch v-model="collectForm.extractKnowledge" size="large" />
          <span class="switch-hint">AI 将从对话内容中提取关键知识点，待确认后入库</span>
        </el-form-item>

        <el-form-item v-if="collectForm.extractKnowledge" label="生成复习卡片">
          <el-switch v-model="collectForm.generateCards" size="large" />
          <span class="switch-hint">确认入库时自动生成复习卡片</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCollectDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCollect" :loading="collectLoading">
          提交采集
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showDetailDialog"
      title="对话详情"
      width="700px"
      class="detail-dialog"
    >
      <div v-if="currentChat" class="detail-content">
        <div class="detail-platform-row">
          <el-tag :type="getPlatformTagType(currentChat.platform)" size="large">
            {{ getPlatformName(currentChat.platform) }}
          </el-tag>
          <span class="detail-date">{{ formatDate(currentChat.createTime) }}</span>
        </div>
        <div class="detail-id">ID: {{ currentChat.id }}</div>
        <div class="detail-content-body">
          <h4>对话内容</h4>
          <div class="detail-text">{{ currentChat.content }}</div>
        </div>
        <div v-if="currentChat.sourceUrl" class="detail-source">
          <h4>来源链接</h4>
          <el-link :href="currentChat.sourceUrl" target="_blank" type="primary">
            {{ currentChat.sourceUrl }}
          </el-link>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
        <el-button type="primary" @click="editChat">编辑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { chatAPI } from "@/api/chat";
import { captureAPI } from "@/api/capture";
import { userAPI } from "@/api/user";
import { useWorkspaceStore } from "@/stores/workspace";
import {
  ChatDotRound,
  Search,
  Plus,
  UploadFilled,
  Document,
  Delete,
} from "@element-plus/icons-vue";

const router = useRouter();
const workspaceStore = useWorkspaceStore();

const loading = ref(false);
const collectLoading = ref(false);
const noteLoading = ref(false);
const searchKeyword = ref("");
const filterPlatform = ref("");
const showCollectDialog = ref(false);
const showDetailDialog = ref(false);
const activeTab = ref("document");
const documentFile = ref(null);
const userId = ref(null);

const chatList = ref([]);
const currentChat = ref(null);
const collectFormRef = ref(null);

const collectForm = ref({
  platform: "",
  content: "",
  sourceUrl: "",
  workspaceId: null,
  extractKnowledge: true,
  generateCards: false,
});

const collectRules = {
  platform: [{ required: true, message: "请选择平台", trigger: "change" }],
  content: [{ required: true, message: "请输入对话内容", trigger: "blur" }],
};

const noteForm = ref({ title: "", content: "" });

const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
});

onMounted(async () => {
  loadChatList();
  try {
    const response = await userAPI.getUserInfo();
    if (response && response.id) {
      userId.value = response.id;
    }
  } catch (error) {
    ElMessage.error("获取用户信息失败");
  }
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

const getPlatformTagType = (platform) => {
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
    ChatGPT: "primary",
    DeepSeek: "success",
    Kimi: "warning",
    豆包: "danger",
    智谱: "info",
    通义千问: "info",
  };
  return typeMap[platform] || "info";
};

const getPlatformName = (platform) => {
  const nameMap = {
    "chatgpt.com": "ChatGPT",
    "chat.openai.com": "ChatGPT",
    "chat.deepseek.com": "DeepSeek",
    "www.kimi.com": "Kimi",
    "kimi.moonshot.cn": "Kimi",
    "kimi.ai": "Kimi",
    "www.doubao.com": "豆包",
    "www.zhipuai.cn": "智谱",
    "www.qianwen.com": "通义千问",
    Other: "其他",
    ChatGPT: "ChatGPT",
    DeepSeek: "DeepSeek",
    Kimi: "Kimi",
    豆包: "豆包",
    智谱: "智谱",
    通义千问: "通义千问",
  };
  return nameMap[platform] || platform;
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");
  return `${year}/${month}/${day} ${hour}:${minute}`;
};

const formatFileSize = (bytes) => {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
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
    collectForm.value = { platform: "", content: "", sourceUrl: "", workspaceId: null, extractKnowledge: true, generateCards: false };
    loadChatList();
  } catch (error) {
    if (error !== false) {
      ElMessage.error("采集失败：" + error.message);
    }
  } finally {
    collectLoading.value = false;
  }
};

const customUpload = async (options) => {
  const { file } = options;
  const maxSize = 10 * 1024 * 1024;
  if (file.size > maxSize) {
    ElMessage.error("文件大小不能超过 10MB");
    return;
  }
  documentFile.value = file;
  try {
    await captureAPI.captureDocument(file, userId.value);
    ElMessage.success("文档上传成功，正在提取知识点...");
    documentFile.value = null;
  } catch (error) {
    if (error.message && error.message.includes("API Key")) {
      ElMessageBox.alert(
        "AI服务不可用，请配置有效的API Key。\n\n请前往【个人设置】添加您的API Key，或联系管理员配置平台API Key。",
        "需要配置API Key",
        { confirmButtonText: "前往设置", type: "warning" },
      ).then(() => {
        router.push("/settings");
      });
    } else {
      ElMessage.error("文档上传失败：" + (error.message || "未知错误"));
    }
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
    await captureAPI.captureNote(noteForm.value.title, noteForm.value.content, userId.value);
    ElMessage.success("笔记捕捉成功，正在提取知识点...");
    noteForm.value = { title: "", content: "" };
  } catch (error) {
    if (error.message && error.message.includes("API Key")) {
      ElMessageBox.alert(
        "AI服务不可用，请配置有效的API Key。\n\n请前往【个人设置】添加您的API Key，或联系管理员配置平台API Key。",
        "需要配置API Key",
        { confirmButtonText: "前往设置", type: "warning" },
      ).then(() => {
        router.push("/settings");
      });
    } else {
      ElMessage.error("笔记捕捉失败：" + (error.message || "未知错误"));
    }
  } finally {
    noteLoading.value = false;
  }
};

const editChat = () => {
  ElMessage.info("编辑功能开发中");
};
</script>

<style scoped>
.capture-page {
  min-height: 100%;
  background: var(--bg-page);
}

.main-content {
  padding: var(--spacing-xl) 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-lighter);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.page-title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin: 0;
}

.page-subtitle {
  font-size: var(--font-size-base);
  color: var(--text-muted);
}

.content-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-xl);
}

.left-panel,
.right-panel {
  display: flex;
  flex-direction: column;
}

.panel-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  height: fit-content;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-lighter);
}

.panel-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.panel-icon.purple {
  background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 100%);
}

.panel-icon.blue {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
}

.panel-title-area {
  flex: 1;
}

.panel-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
}

.panel-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.search-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  background: var(--bg-input);
  border-radius: var(--radius-md);
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--border-light);
}

.search-wrapper input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  outline: none;
}

.search-wrapper input::placeholder {
  color: var(--text-placeholder);
}

.chat-list-wrapper {
  flex: 1;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.list-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.list-count {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  max-height: 500px;
  overflow-y: auto;
}

.chat-item {
  background: white;
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  border: 1px solid var(--border-lighter);
  cursor: pointer;
  transition: all var(--transition-base);
}

.chat-item:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
}

.chat-platform-tag {
  margin-bottom: var(--spacing-sm);
}

.chat-preview {
  font-size: var(--font-size-sm);
  color: var(--text-regular);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: var(--spacing-sm);
}

.chat-item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.chat-id {
  margin-right: var(--spacing-md);
}

.capture-tabs {
  margin-top: var(--spacing-md);
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

.tab-content {
  padding-top: var(--spacing-lg);
}

.upload-area {
  margin-top: var(--spacing-md);
}

.upload-demo {
  width: 100%;
}

.upload-demo :deep(.el-upload-dragger) {
  border: 2px dashed var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--bg-input);
  padding: var(--spacing-xl);
  transition: all var(--transition-base);
}

.upload-demo :deep(.el-upload-dragger:hover) {
  border-color: var(--color-primary);
  background: rgba(124, 58, 237, 0.05);
}

.upload-icon {
  display: flex;
  justify-content: center;
  margin-bottom: var(--spacing-md);
}

.cloud-icon {
  width: 80px;
  height: 80px;
}

.upload-text {
  font-size: var(--font-size-base);
  color: var(--text-primary);
  text-align: center;
  margin-bottom: var(--spacing-sm);
}

.upload-tip {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  text-align: center;
}

.file-preview {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  background: var(--bg-input);
  border-radius: var(--radius-md);
  margin-top: var(--spacing-lg);
}

.file-icon-box {
  width: 48px;
  height: 48px;
  background: var(--gradient-primary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.file-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.file-size {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.note-form {
  margin-top: var(--spacing-md);
}

.note-form :deep(.el-form-item) {
  margin-bottom: var(--spacing-lg);
}

.note-form :deep(.el-input__wrapper) {
  box-shadow: none;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}

.note-form :deep(.el-textarea__inner) {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}

.detail-content {
  padding: var(--spacing-sm);
}

.detail-platform-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.detail-date {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.detail-id {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-bottom: var(--spacing-lg);
}

.detail-content-body,
.detail-source {
  background: var(--bg-list-item);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
}

.detail-content-body h4,
.detail-source h4 {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  margin: 0 0 var(--spacing-md) 0;
}

.detail-text {
  font-size: var(--font-size-base);
  color: var(--text-regular);
  line-height: 1.6;
  white-space: pre-wrap;
}

.switch-hint {
  margin-left: var(--spacing-md);
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

@media (max-width: 1200px) {
  .content-wrapper {
    grid-template-columns: 1fr;
  }
}
</style>