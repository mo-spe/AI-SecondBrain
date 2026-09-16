<template>
  <div class="knowledge-detail-page">
    <!-- Loading -->
    <div v-if="loading" class="detail-loading">
      <el-skeleton :rows="12" animated />
    </div>

    <!-- Not Found -->
    <div v-else-if="!knowledge" class="detail-empty">
      <el-empty description="知识点不存在或无权访问" :image-size="120" />
      <el-button type="primary" @click="$router.push('/knowledge')">返回知识管理</el-button>
    </div>

    <!-- Content -->
    <template v-else>
      <!-- Top bar -->
      <div class="detail-topbar">
        <div class="topbar-left">
          <button class="back-btn" @click="$router.push('/knowledge')">
            <el-icon size="16"><ArrowLeft /></el-icon>
            <span>返回</span>
          </button>
        </div>
        <div class="topbar-right">
          <el-button plain size="default" @click="handleShare" :loading="shareLoading">
            <el-icon size="14"><Share /></el-icon>
            <span>分享</span>
          </el-button>
          <el-button plain size="default" @click="showVersionHistory = true">
            <el-icon size="14"><Clock /></el-icon>
            <span>历史</span>
          </el-button>
          <el-button type="primary" size="default" @click="handleEdit">
            <el-icon size="14"><Edit /></el-icon>
            <span>编辑</span>
          </el-button>
        </div>
      </div>

      <!-- Hero header -->
      <div class="detail-hero">
        <h1 class="detail-title">{{ knowledge.title }}</h1>
        <div class="detail-meta">
          <span class="meta-item">
            <el-icon size="13"><Clock /></el-icon>
            <span>创建于 {{ formatDate(knowledge.createTime) }}</span>
          </span>
          <span v-if="knowledge.updateTime" class="meta-item">
            <span>· 更新于 {{ formatDate(knowledge.updateTime) }}</span>
          </span>
          <span class="meta-divider">|</span>
          <span class="meta-item mastery">
            <span>掌握程度</span>
            <el-progress
              :percentage="getMasteryPercentage(knowledge.masteryLevel)"
              :color="getMasteryColor(knowledge.masteryLevel)"
              :stroke-width="6"
              :show-text="false"
              style="width: 60px;"
            />
          </span>
          <span class="meta-item">
            <el-rate
              :model-value="knowledge.importance"
              disabled
              show-score
              text-color="#f59e0b"
              :max="5"
              size="small"
            />
          </span>
        </div>
        <div class="detail-tags">
          <TagChips
            :tags="knowledge.tags || []"
            :editable="true"
            :node-id="knowledge.id"
            :available-tags="flatTagList"
            @add="refreshKnowledge"
            @remove="refreshKnowledge"
          />
        </div>
      </div>

      <!-- Content body -->
      <div class="detail-body">
        <div
          class="markdown-content"
          v-html="renderedContent"
        ></div>
        <div v-if="!knowledge.contentMd" class="content-empty">
          <p>暂无正文内容</p>
        </div>
      </div>
    </template>

    <!-- Version History drawer -->
    <VersionHistory
      v-model="showVersionHistory"
      :node-id="knowledge?.id"
      :can-rollback="true"
      @rollback-success="refreshKnowledge"
    />

    <!-- Share dialog -->
    <el-dialog
      v-model="showShareDialog"
      title="分享知识文档"
      width="460px"
      :close-on-click-modal="false"
    >
      <div v-if="!shareLink" class="share-create">
        <el-form label-width="80px">
          <el-form-item label="有效期">
            <el-radio-group v-model="shareForm.expireType">
              <el-radio value="permanent">永久有效</el-radio>
              <el-radio value="7d">7天</el-radio>
              <el-radio value="24h">24小时</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <div style="text-align:right;">
          <el-button type="primary" @click="handleCreateShare" :loading="shareLoading">
            生成分享链接
          </el-button>
        </div>
      </div>
      <div v-else class="share-result">
        <p style="margin:0 0 8px;color:var(--text-secondary);font-size:13px;">分享链接已生成：</p>
        <div style="display:flex;gap:8px;">
          <input
            class="share-url-input"
            :value="shareLink"
            readonly
            @focus="$event.target.select()"
          />
          <el-button type="primary" size="small" @click="copyShareLink">复制</el-button>
        </div>
        <el-button link size="small" @click="shareLink = ''" style="margin-top:12px;">重新生成</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  ArrowLeft, Clock, Edit, Share,
} from "@element-plus/icons-vue";
import { knowledgeAPI } from "@/api/knowledge";
import { collaborationAPI } from "@/api/collaboration";
import { tagsAPI } from "@/api/tags";
import { renderMarkdown } from "@/utils/markdown";
import TagChips from "@/components/TagChips.vue";
import VersionHistory from "@/components/VersionHistory.vue";

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const knowledge = ref(null);
const showVersionHistory = ref(false);
const showShareDialog = ref(false);
const shareLink = ref("");
const shareLoading = ref(false);
const shareForm = ref({ expireType: "permanent" });
const flatTagList = ref([]);

const renderedContent = computed(() => {
  if (!knowledge.value?.contentMd) return "";
  return renderMarkdown(knowledge.value.contentMd);
});

onMounted(async () => {
  try {
    const [data, tags] = await Promise.all([
      knowledgeAPI.getById(route.params.id),
      tagsAPI.getAll(),
    ]);
    knowledge.value = data;
    flatTagList.value = tags;
  } catch (e) {
    console.error("加载知识详情失败", e);
    knowledge.value = null;
  } finally {
    loading.value = false;
  }
});

async function refreshKnowledge() {
  try {
    const data = await knowledgeAPI.getById(route.params.id);
    knowledge.value = data;
  } catch (e) {
    console.warn("刷新失败", e);
  }
}

async function handleEdit() {
  try {
    const res = await collaborationAPI.acquireLock(knowledge.value.id);
    if (res.code === 409) {
      ElMessage.warning(res.message || "其他用户正在编辑此知识点");
      return;
    }
  } catch (e) {
    ElMessage.warning("获取编辑锁失败");
    return;
  }
  router.push(`/knowledge/${knowledge.value.id}/edit`);
}

function handleShare() {
  shareLink.value = "";
  shareForm.value = { expireType: "permanent" };
  showShareDialog.value = true;
}

async function handleCreateShare() {
  if (!knowledge.value) return;
  shareLoading.value = true;
  try {
    const data = await collaborationAPI.createShare({
      nodeId: knowledge.value.id,
      expireType: shareForm.value.expireType,
    });
    shareLink.value = `${window.location.origin}/share/${data.token}`;
  } catch (e) {
    ElMessage.error("创建分享失败：" + (e.message || "未知错误"));
  } finally {
    shareLoading.value = false;
  }
}

async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(shareLink.value);
    ElMessage.success("链接已复制");
  } catch {
    ElMessage.info("请手动复制链接");
  }
}

function formatDate(dateStr) {
  if (!dateStr) return "-";
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function getMasteryPercentage(level) {
  return ((level || 0) / 5) * 100;
}

function getMasteryColor(level) {
  const map = { 0: "#ff4d4f", 1: "#ff6b6b", 2: "#f56c6c", 3: "#e6a23c", 4: "#67c23a", 5: "#409eff" };
  return map[level] || "#909399";
}
</script>

<style scoped>
.knowledge-detail-page {
  max-width: 860px;
  margin: 0 auto;
  padding: var(--spacing-xl) var(--spacing-lg) var(--spacing-3xl);
}

.detail-loading {
  padding: var(--spacing-3xl) 0;
}

.detail-empty {
  padding: var(--spacing-3xl) 0;
  text-align: center;
}

/* ---- Top bar ---- */

.detail-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-2xl);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: none;
  background: var(--bg-input);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.back-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.topbar-right {
  display: flex;
  gap: var(--spacing-sm);
}

/* ---- Hero ---- */

.detail-hero {
  margin-bottom: var(--spacing-2xl);
  padding-bottom: var(--spacing-xl);
  border-bottom: 1px solid var(--border-lighter);
}

.detail-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 var(--spacing-lg);
  line-height: 1.3;
  letter-spacing: -0.02em;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin-bottom: var(--spacing-md);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-item.mastery {
  gap: 8px;
}

.meta-divider {
  color: var(--border-light);
  margin: 0 2px;
}

.detail-tags {
  margin-top: var(--spacing-sm);
}

/* ---- Body ---- */

.detail-body {
  position: relative;
}

.content-empty {
  text-align: center;
  padding: var(--spacing-3xl);
  color: var(--text-placeholder);
  font-size: var(--font-size-base);
}

/* ---- Share ---- */

.share-url-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  background: var(--bg-input);
  outline: none;
}

.share-url-input:focus {
  border-color: var(--color-primary);
}

/* ---- Markdown rendered content ---- */

.markdown-content {
  line-height: 1.8;
  color: var(--text-regular);
  font-size: 15px;
}

.markdown-content :deep(h1) {
  font-size: 24px;
  font-weight: 700;
  margin: 28px 0 14px 0;
  color: var(--text-primary);
  padding-bottom: 8px;
  border-bottom: 2px solid var(--color-primary);
}

.markdown-content :deep(h2) {
  font-size: 20px;
  font-weight: 600;
  margin: 24px 0 12px 0;
  color: var(--text-primary);
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border-light);
}

.markdown-content :deep(h3) {
  font-size: 17px;
  font-weight: 600;
  margin: 20px 0 10px 0;
  color: var(--text-primary);
}

.markdown-content :deep(h4) {
  font-size: 15px;
  font-weight: 600;
  margin: 16px 0 8px 0;
  color: var(--text-regular);
}

.markdown-content :deep(p) {
  margin: 10px 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 24px;
  margin: 10px 0;
}

.markdown-content :deep(li) {
  margin: 4px 0;
  line-height: 1.6;
}

.markdown-content :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 16px;
  border-left: 3px solid var(--color-primary);
  background: var(--color-primary-alpha-10);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  color: var(--text-secondary);
}

.markdown-content :deep(code) {
  background: var(--bg-input);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: "Consolas", "Monaco", "Courier New", monospace;
  font-size: 13px;
}

.markdown-content :deep(pre) {
  background: #1e1e1e;
  padding: 16px;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 14px 0;
}

.markdown-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #d4d4d4;
  font-size: 13px;
  line-height: 1.6;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 14px 0;
  font-size: var(--font-size-sm);
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid var(--border-light);
  padding: 8px 12px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: var(--bg-input);
  font-weight: 600;
  color: var(--text-primary);
}

.markdown-content :deep(tr:nth-child(even)) {
  background: var(--bg-list-item);
}

.markdown-content :deep(a) {
  color: var(--color-primary);
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-md);
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-lighter);
  margin: 24px 0;
}

.markdown-content :deep(strong) {
  color: var(--text-primary);
}
</style>
