<template>
  <div class="knowledge-edit-page">
    <!-- Loading -->
    <div v-if="loading" class="edit-loading">
      <el-skeleton :rows="10" animated />
    </div>

    <template v-else-if="knowledge">
      <!-- Top bar -->
      <div class="edit-topbar">
        <div class="topbar-left">
          <button class="back-btn" @click="handleCancel">
            <el-icon size="16"><ArrowLeft /></el-icon>
            <span>返回</span>
          </button>
          <span class="lock-badge" v-if="isCreateMode">
            <el-icon size="12"><Edit /></el-icon>
            <span>新建</span>
          </span>
          <span class="lock-badge" v-else-if="hasLock">
            <el-icon size="12"><Lock /></el-icon>
            <span>编辑中</span>
          </span>
          <span class="lock-badge warn" v-else>
            <el-icon size="12"><Warning /></el-icon>
            <span>未获取编辑锁</span>
          </span>
        </div>
        <div class="topbar-right">
          <el-button size="default" @click="handleCancel">
            <span>取消</span>
          </el-button>
          <el-button type="primary" size="default" @click="handleSave" :loading="saving">
            <el-icon size="14"><Check /></el-icon>
            <span>保存</span>
          </el-button>
        </div>
      </div>

      <!-- Title -->
      <div class="edit-title-area">
        <input
          ref="titleInput"
          v-model="form.title"
          class="title-input"
          placeholder="输入标题..."
          maxlength="200"
        />
      </div>

      <!-- Editor body -->
      <div class="edit-body">
        <div class="editor-pane">
          <div class="pane-header">
            <span>Markdown</span>
          </div>
          <textarea
            ref="editorTextarea"
            v-model="form.contentMd"
            class="editor-textarea"
            placeholder="开始写作... 支持 Markdown 语法"
          ></textarea>
        </div>
        <div class="preview-pane">
          <div class="pane-header">
            <span>预览</span>
          </div>
          <div
            class="preview-content markdown-content"
            v-html="previewHtml"
          ></div>
        </div>
      </div>

      <!-- Properties bar -->
      <div class="edit-properties">
        <div class="prop-group" v-if="!isCreateMode">
          <label class="prop-label">标签</label>
          <TagChips
            :tags="knowledge.tags || []"
            :editable="true"
            :node-id="knowledge.id"
            :available-tags="flatTagList"
            @add="onTagChanged"
            @remove="onTagChanged"
          />
        </div>
        <div class="prop-group">
          <label class="prop-label">重要程度</label>
          <el-rate
            v-model="form.importance"
            show-score
            text-color="#f59e0b"
            :max="5"
            size="default"
          />
        </div>
        <div class="prop-group">
          <label class="prop-label">掌握程度</label>
          <div class="mastery-slider">
            <el-slider
              v-model="form.masteryLevel"
              :min="0"
              :max="5"
              :step="1"
              :marks="masteryMarks"
              show-stops
              style="width: 160px;"
            />
          </div>
        </div>
      </div>
    </template>

    <div v-else class="edit-empty">
      <el-empty description="知识点不存在或无权访问" :image-size="120" />
      <el-button type="primary" @click="$router.push('/knowledge')">返回知识管理</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  ArrowLeft, Lock, Warning, Check, Edit,
} from "@element-plus/icons-vue";
import { knowledgeAPI } from "@/api/knowledge";
import { collaborationAPI } from "@/api/collaboration";
import { tagsAPI } from "@/api/tags";
import { renderMarkdown } from "@/utils/markdown";
import TagChips from "@/components/TagChips.vue";

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const saving = ref(false);
const knowledge = ref(null);
const hasLock = ref(false);
const lockRenewTimer = ref(null);
const editorTextarea = ref(null);
const titleInput = ref(null);
const flatTagList = ref([]);

const form = reactive({
  title: "",
  contentMd: "",
  importance: 3,
  masteryLevel: 3,
});

const masteryMarks = {
  0: "0",
  1: "1",
  2: "2",
  3: "3",
  4: "4",
  5: "5",
};

const previewHtml = computed(() => {
  if (!form.contentMd) return "<p style='color:var(--text-placeholder)'>预览区域</p>";
  return renderMarkdown(form.contentMd);
});

const isCreateMode = computed(() => !route.params.id || route.params.id === "new");

onMounted(async () => {
  try {
    const [tags] = await Promise.all([
      tagsAPI.getAll(),
    ]);
    flatTagList.value = tags;

    if (!isCreateMode.value) {
      const data = await knowledgeAPI.getById(route.params.id);
      knowledge.value = data;
      form.title = data.title || "";
      form.contentMd = data.contentMd || "";
      form.importance = data.importance || 3;
      form.masteryLevel = data.masteryLevel || 3;
      await acquireLock();
    } else {
      knowledge.value = { id: null, tags: [] };
    }
  } catch (e) {
    console.error("加载知识详情失败", e);
    knowledge.value = null;
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  releaseCurrentLock();
});

async function acquireLock() {
  try {
    const res = await collaborationAPI.acquireLock(knowledge.value.id);
    if (res.code === 409) {
      ElMessage.warning(res.message || "其他用户正在编辑此知识点");
      hasLock.value = false;
      return;
    }
    hasLock.value = true;
    lockRenewTimer.value = setInterval(() => {
      collaborationAPI.acquireLock(knowledge.value.id).catch(() => {});
    }, 5 * 60 * 1000);
  } catch (e) {
    console.warn("获取编辑锁失败", e);
    hasLock.value = false;
  }
}

function releaseCurrentLock() {
  if (knowledge.value?.id && hasLock.value) {
    collaborationAPI.releaseLock(knowledge.value.id).catch(() => {});
    hasLock.value = false;
  }
  if (lockRenewTimer.value) {
    clearInterval(lockRenewTimer.value);
    lockRenewTimer.value = null;
  }
}

async function handleSave() {
  if (!form.title.trim()) {
    ElMessage.warning("标题不能为空");
    titleInput.value?.focus();
    return;
  }

  saving.value = true;
  try {
    if (isCreateMode.value) {
      await knowledgeAPI.createKnowledge({
        title: form.title.trim(),
        summary: "",
        contentMd: form.contentMd,
        importance: form.importance,
      });
      ElMessage.success("创建成功");
      router.push("/knowledge");
    } else {
      await knowledgeAPI.updateKnowledge(knowledge.value.id, {
        title: form.title.trim(),
        summary: knowledge.value.summary || "",
        contentMd: form.contentMd,
        importance: form.importance,
      });
      if (form.masteryLevel !== knowledge.value.masteryLevel) {
        await knowledgeAPI.updateImportance(knowledge.value.id, form.importance);
      }
      releaseCurrentLock();
      ElMessage.success("保存成功");
      router.push(`/knowledge/${knowledge.value.id}`);
    }
  } catch (e) {
    ElMessage.error("保存失败：" + (e.message || "未知错误"));
  } finally {
    saving.value = false;
  }
}

async function handleCancel() {
  if (hasChanged.value) {
    try {
      await ElMessageBox.confirm("有未保存的修改，确定要离开吗？", "确认离开", {
        confirmButtonText: "确定离开",
        cancelButtonText: "继续编辑",
        type: "warning",
      });
    } catch {
      return;
    }
  }
  releaseCurrentLock();
  if (isCreateMode.value) {
    router.push("/knowledge");
  } else {
    router.push(`/knowledge/${knowledge.value.id}`);
  }
}

const hasChanged = computed(() => {
  if (isCreateMode.value) return form.title.trim() !== "" || form.contentMd.trim() !== "";
  if (!knowledge.value) return false;
  return (
    form.title !== knowledge.value.title ||
    form.contentMd !== (knowledge.value.contentMd || "") ||
    form.importance !== knowledge.value.importance
  );
});

async function onTagChanged() {
  const data = await knowledgeAPI.getById(route.params.id);
  if (data) {
    knowledge.value.tags = data.tags || [];
  }
}
</script>

<style scoped>
.knowledge-edit-page {
  max-width: 100%;
  height: calc(100vh - 56px);
  display: flex;
  flex-direction: column;
  padding: 0 var(--spacing-lg);
}

.edit-loading {
  padding: var(--spacing-3xl);
}

.edit-empty {
  padding: var(--spacing-3xl) 0;
  text-align: center;
}

/* ---- Top bar ---- */

.edit-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid var(--border-lighter);
  flex-shrink: 0;
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

.lock-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #22c55e;
  padding: 2px 10px;
  background: rgba(34, 197, 94, 0.1);
  border-radius: var(--radius-full);
}

.lock-badge.warn {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
}

/* ---- Title ---- */

.edit-title-area {
  padding: var(--spacing-lg) 0;
  flex-shrink: 0;
}

.title-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  background: transparent;
  letter-spacing: -0.02em;
  line-height: 1.3;
}

.title-input::placeholder {
  color: var(--text-placeholder);
  font-weight: 400;
}

/* ---- Editor body ---- */

.edit-body {
  flex: 1;
  display: flex;
  gap: 1px;
  background: var(--border-lighter);
  min-height: 0;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.editor-pane,
.preview-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
}

.pane-header {
  padding: 6px 14px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  background: var(--bg-list-item);
  border-bottom: 1px solid var(--border-lighter);
  flex-shrink: 0;
}

.editor-textarea {
  flex: 1;
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  padding: var(--spacing-lg);
  font-family: "Consolas", "Monaco", "Courier New", monospace;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  background: var(--bg-card);
  tab-size: 2;
}

.editor-textarea::placeholder {
  color: var(--text-placeholder);
}

.preview-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
}

/* ---- Properties bar ---- */

.edit-properties {
  display: flex;
  align-items: center;
  gap: var(--spacing-xl);
  padding: var(--spacing-md) 0;
  border-top: 1px solid var(--border-lighter);
  flex-shrink: 0;
  flex-wrap: wrap;
}

.prop-group {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.prop-label {
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--text-muted);
  white-space: nowrap;
}

.mastery-slider {
  display: flex;
  align-items: center;
}

/* ---- Markdown preview ---- */

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

.markdown-content :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-md);
}

.markdown-content :deep(a) {
  color: var(--color-primary);
}

.markdown-content :deep(strong) {
  color: var(--text-primary);
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-lighter);
  margin: 24px 0;
}
</style>
