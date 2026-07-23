<template>
  <el-dialog
    v-model="visible"
    title="版本历史"
    width="800px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <div v-loading="loading" class="version-history">
      <el-empty v-if="!loading && revisions.length === 0" description="暂无版本历史" />

      <el-table v-else :data="revisions" border stripe max-height="400">
        <el-table-column label="版本号" width="80">
          <template #default="{ row }">
            <el-tag type="primary" size="small">v{{ row.revisionNum }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更说明" min-width="200">
          <template #default="{ row }">
            {{ row.changeSummary || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="保存时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
            <el-button
              v-if="canRollback"
              size="small"
              type="warning"
              @click="handleRollback(row)"
            >
              回滚
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="showDetail"
    :title="`版本 v${selectedRevision?.revisionNum} 详情`"
    width="700px"
  >
    <div v-if="selectedRevision" class="revision-detail">
      <div class="revision-meta">
        <span>版本号：v{{ selectedRevision.revisionNum }}</span>
        <span>保存时间：{{ formatDate(selectedRevision.createdAt) }}</span>
      </div>
      <div class="revision-content">
        <h4>{{ selectedRevision.title }}</h4>
        <div class="revision-body">{{ selectedRevision.contentMd }}</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="showDetail = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { collaborationAPI } from "@/api/collaboration";

const props = defineProps({
  modelValue: Boolean,
  nodeId: { type: [Number, String], required: true },
  canRollback: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue", "rollback-success"]);

const visible = ref(false);
const loading = ref(false);
const revisions = ref([]);
const showDetail = ref(false);
const selectedRevision = ref(null);

watch(() => props.modelValue, (val) => {
  visible.value = val;
  if (val) {
    loadRevisions();
  }
});

watch(visible, (val) => {
  emit("update:modelValue", val);
});

const loadRevisions = async () => {
  loading.value = true;
  try {
    const data = await collaborationAPI.getRevisionList(props.nodeId);
    revisions.value = Array.isArray(data) ? data : (data?.records || []);
  } catch (error) {
    ElMessage.error("加载版本历史失败：" + (error.message || "未知错误"));
  } finally {
    loading.value = false;
  }
};

const viewDetail = (revision) => {
  selectedRevision.value = revision;
  showDetail.value = true;
};

const handleRollback = (revision) => {
  ElMessageBox.confirm(
    `确定要回滚到版本 v${revision.revisionNum} 吗？当前内容将被覆盖。`,
    "确认回滚",
    { confirmButtonText: "确定回滚", cancelButtonText: "取消", type: "warning" }
  ).then(async () => {
    try {
      await collaborationAPI.rollbackRevision(props.nodeId, revision.id);
      ElMessage.success("回滚成功");
      visible.value = false;
      emit("rollback-success");
    } catch (error) {
      ElMessage.error("回滚失败：" + (error.message || "未知错误"));
    }
  }).catch(() => {});
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

const handleClosed = () => {
  revisions.value = [];
};
</script>

<style scoped>
.version-history {
  min-height: 200px;
}

.revision-detail {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.revision-meta {
  display: flex;
  gap: var(--spacing-xl);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-lighter);
}

.revision-content h4 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--spacing-md) 0;
}

.revision-body {
  font-size: var(--font-size-base);
  color: var(--text-regular);
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 400px;
  overflow-y: auto;
  background: var(--bg-list-item);
  padding: var(--spacing-lg);
  border-radius: var(--radius-md);
}
</style>
