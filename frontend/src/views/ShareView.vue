<template>
  <div class="share-page">
    <div v-if="loading" class="share-loading">
      <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <div v-else-if="error" class="share-error">
      <el-icon :size="48"><WarningFilled /></el-icon>
      <h2>链接已失效</h2>
      <p>{{ error }}</p>
    </div>

    <article v-else class="share-content">
      <header class="share-header">
        <div class="share-badge">
          <el-icon><Share /></el-icon>
          <span>AI-SecondBrain 知识分享</span>
        </div>
        <h1 class="share-title">{{ content.title }}</h1>
        <div class="share-meta">
          <span>访问次数：{{ content.accessCount }}</span>
          <span>创建时间：{{ formatDate(content.createdAt) }}</span>
        </div>
      </header>

      <section class="share-section" v-if="content.summary">
        <h3>摘要</h3>
        <div class="share-text">{{ content.summary }}</div>
      </section>

      <section class="share-section" v-if="content.contentMd">
        <h3>内容</h3>
        <div class="share-text markdown-body">{{ content.contentMd }}</div>
      </section>
    </article>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import axios from "axios";
import { Loading, WarningFilled, Share } from "@element-plus/icons-vue";

const route = useRoute();
const loading = ref(true);
const error = ref("");
const content = ref({});

const fetchShareContent = async () => {
  try {
    const token = route.params.token;
    const res = await axios.get(`/api/share/${token}`);
    if (res.data.code === 200) {
      content.value = res.data.data;
    } else {
      error.value = res.data.message || "链接已失效或不存在";
    }
  } catch (e) {
    error.value = "链接已失效或不存在";
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

onMounted(() => {
  fetchShareContent();
});
</script>

<style scoped>
.share-page {
  min-height: 100vh;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.share-loading,
.share-error {
  text-align: center;
  color: #64748b;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.share-error h2 {
  font-size: 20px;
  color: #1e293b;
  margin: 0;
}

.share-error p {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

.loading-icon {
  animation: spin 1s linear infinite;
  color: #6366f1;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.share-content {
  max-width: 800px;
  width: 100%;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 4px 16px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.share-header {
  padding: 32px 36px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.share-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  padding: 4px 12px;
  border-radius: 20px;
  margin-bottom: 16px;
}

.share-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.3;
  margin: 0 0 12px;
}

.share-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #94a3b8;
}

.share-section {
  padding: 24px 36px;
}

.share-section + .share-section {
  border-top: 1px solid #f1f5f9;
}

.share-section h3 {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  margin: 0 0 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.share-text {
  font-size: 15px;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

@media (max-width: 640px) {
  .share-page {
    padding: 20px 12px;
  }

  .share-header {
    padding: 24px 20px 20px;
  }

  .share-section {
    padding: 20px;
  }

  .share-title {
    font-size: 22px;
  }
}
</style>
