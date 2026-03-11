<template>
  <div class="search-container">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><Search /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">知识搜索</h1>
            <p class="welcome-subtitle">快速查找和探索您的知识体系</p>
          </div>
        </div>
      </div>

      <div class="search-section">
        <div class="search-card">
          <div class="search-type-selector">
            <el-radio-group v-model="searchType" size="large">
              <el-radio-button value="keyword">
                <el-icon><Key /></el-icon>
                关键词搜索
              </el-radio-button>
              <el-radio-button value="multi">
                <el-icon><Grid /></el-icon>
                多字段搜索
              </el-radio-button>
              <el-radio-button value="semantic">
                <el-icon><MagicStick /></el-icon>
                语义搜索
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="search-box-wrapper">
            <el-input
              v-model="searchKeyword"
              placeholder="请输入搜索关键词..."
              clearable
              size="large"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>
          </div>

          <div class="search-tips">
            <el-icon><InfoFilled /></el-icon>
            <span>提示：语义搜索可以理解您的意图，提供更精准的结果</span>
          </div>
        </div>
      </div>

      <div v-if="searchResults.length > 0" class="results-section">
        <div class="results-card">
          <div class="results-header">
            <div class="results-title">
              <el-icon><Document /></el-icon>
              <span>搜索结果</span>
            </div>
            <div class="results-actions">
              <el-tag type="info" size="large"
                >{{ searchResults.length }}条</el-tag
              >
              <el-button type="primary" @click="clearSearch">
                <el-icon><RefreshLeft /></el-icon>
                清除
              </el-button>
            </div>
          </div>

          <div class="results-body">
            <el-scrollbar max-height="calc(100vh - 500px)">
              <div class="results-grid">
                <div
                  v-for="item in searchResults"
                  :key="item.id"
                  class="result-item"
                  @click="viewDetail(item)"
                >
                  <div class="result-header">
                    <div class="result-title">{{ item.title }}</div>
                    <div class="result-score" v-if="item.score">
                      <el-icon><TrendCharts /></el-icon>
                      <span>{{ (item.score * 100).toFixed(1) }}%</span>
                    </div>
                  </div>

                  <div class="result-summary">{{ item.summary }}</div>

                  <div class="result-meta">
                    <div class="meta-item">
                      <el-icon><Star /></el-icon>
                      <span>重要程度</span>
                      <el-rate
                        v-model="item.importance"
                        disabled
                        show-score
                        text-color="#ff9900"
                        :max="5"
                        size="small"
                      />
                    </div>

                    <div class="meta-item">
                      <el-icon><Calendar /></el-icon>
                      <span>{{ formatDate(item.createTime) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-scrollbar>
          </div>
        </div>
      </div>

      <div v-else-if="hasSearched" class="empty-section">
        <div class="empty-card">
          <el-empty description="暂无搜索结果">
            <el-button type="primary" @click="clearSearch">重新搜索</el-button>
          </el-empty>
        </div>
      </div>

      <div v-else class="empty-section">
        <div class="empty-card">
          <el-empty description="请输入关键词进行搜索">
            <div class="empty-tips">
              <div class="tip-item">
                <el-icon><Search /></el-icon>
                <span>支持关键词、多字段和语义搜索</span>
              </div>
              <div class="tip-item">
                <el-icon><MagicStick /></el-icon>
                <span>语义搜索能理解您的真实意图</span>
              </div>
              <div class="tip-item">
                <el-icon><Document /></el-icon>
                <span>快速定位您需要的知识点</span>
              </div>
            </div>
          </el-empty>
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
          <h2>{{ currentKnowledge.title }}</h2>
          <el-tag :type="getImportanceType(currentKnowledge.importance)">
            重要程度：{{ currentKnowledge.importance }}
          </el-tag>
        </div>

        <div class="detail-body">
          <div class="detail-section">
            <h3>摘要</h3>
            <div class="summary-text">{{ currentKnowledge.summary }}</div>
          </div>

          <div class="detail-section" v-if="currentKnowledge.contentMd">
            <h3>详细内容</h3>
            <div class="content-text">{{ currentKnowledge.contentMd }}</div>
          </div>

          <div class="detail-stats">
            <div class="stat-item">
              <el-icon><Star /></el-icon>
              <span>重要程度</span>
              <el-rate
                v-model="currentKnowledge.importance"
                disabled
                show-score
                text-color="#ff9900"
                :max="5"
              />
            </div>
            <div class="stat-item">
              <el-icon><Trophy /></el-icon>
              <span>掌握程度</span>
              <el-progress
                :percentage="
                  getMasteryPercentage(currentKnowledge.masteryLevel)
                "
                :color="getMasteryColor(currentKnowledge.masteryLevel)"
              />
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import {
  Search,
  Key,
  Grid,
  MagicStick,
  InfoFilled,
  Document,
  RefreshLeft,
  Star,
  Calendar,
  TrendCharts,
  Trophy,
} from "@element-plus/icons-vue";
import { knowledgeAPI } from "@/api/knowledge";

const searchKeyword = ref("");
const searchType = ref("keyword");
const searchResults = ref([]);
const hasSearched = ref(false);
const showDetailDialog = ref(false);
const currentKnowledge = ref(null);
const searchLoading = ref(false);

const getImportanceType = (importance) => {
  if (importance >= 4) return "danger";
  if (importance >= 3) return "warning";
  return "info";
};

const getMasteryPercentage = (level) => {
  const percentageMap = {
    0: 0,
    1: 20,
    2: 40,
    3: 60,
    4: 80,
    5: 100,
  };
  return percentageMap[level] || 0;
};

const getMasteryColor = (level) => {
  const colorMap = {
    0: "#909399",
    1: "#f56c6c",
    2: "#e6a23c",
    3: "#409eff",
    4: "#67c23a",
    5: "#67c23a",
  };
  return colorMap[level] || "#409eff";
};

const formatDate = (dateString) => {
  if (!dateString) return "-";
  const date = new Date(dateString);
  return date.toLocaleString("zh-CN");
};

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning("请输入搜索关键词");
    return;
  }

  searchLoading.value = true;
  hasSearched.value = true;

  try {
    let results = [];

    switch (searchType.value) {
      case "keyword":
        results = await knowledgeAPI.search({
          keyword: searchKeyword.value,
        });
        break;
      case "multi":
        results = await knowledgeAPI.multiFieldSearch({
          keyword: searchKeyword.value,
        });
        break;
      case "semantic":
        results = await knowledgeAPI.semanticSearch({
          queryText: searchKeyword.value,
          topK: 10,
        });
        break;
    }

    searchResults.value = results || [];

    if (searchResults.value.length === 0) {
      ElMessage.info("未找到相关知识点");
    } else {
      ElMessage.success(`找到 ${searchResults.value.length} 条相关知识点`);
    }
  } catch (error) {
    ElMessage.error("搜索失败：" + error.message);
    searchResults.value = [];
  } finally {
    searchLoading.value = false;
  }
};

const clearSearch = () => {
  searchKeyword.value = "";
  searchResults.value = [];
  hasSearched.value = false;
};

const viewDetail = (item) => {
  currentKnowledge.value = item;
  showDetailDialog.value = true;
};
</script>

<style scoped>
.search-container {
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

.search-section {
  margin-bottom: 20px;
}

.search-card {
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

.search-type-selector {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.search-box-wrapper {
  margin-bottom: 15px;
}

.search-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-radius: 8px;
  color: #667eea;
  font-size: 14px;
}

.results-section {
  margin-bottom: 20px;
}

.results-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease-out;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.results-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.results-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.results-body {
  min-height: 200px;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
}

.result-item {
  background: white;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.result-item::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transform: scaleX(0);
  transition: transform 0.3s;
}

.result-item:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  transform: translateY(-4px);
}

.result-item:hover::before {
  transform: scaleX(1);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.result-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  flex: 1;
  line-height: 1.4;
}

.result-score {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-radius: 12px;
  font-size: 12px;
  color: #667eea;
  font-weight: bold;
  white-space: nowrap;
  margin-left: 10px;
}

.result-summary {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #909399;
}

.meta-item .el-rate {
  margin-left: 4px;
}

.empty-section {
  margin-bottom: 20px;
}

.empty-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease-out;
}

.empty-tips {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea10 0%, #764ba210 100%);
  border-radius: 8px;
  color: #667eea;
  font-size: 14px;
}

.detail-content {
  padding: 10px 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.detail-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-section {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
}

.detail-section h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #303133;
}

.summary-text,
.content-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  color: #606266;
  max-height: 300px;
  overflow-y: auto;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.stat-item {
  background: white;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-item > span {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #909399;
}

:deep(.detail-dialog .el-dialog__body) {
  padding: 20px 30px 30px;
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

:deep(.el-radio-button__inner) {
  border: 2px solid #e0e0e0;
  transition: all 0.3s;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
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
  padding-right: 8px;
}

@media (max-width: 768px) {
  .welcome-card {
    flex-direction: column;
    text-align: center;
  }

  .welcome-title {
    font-size: 24px;
  }

  .results-grid {
    grid-template-columns: 1fr;
  }

  .results-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .results-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
