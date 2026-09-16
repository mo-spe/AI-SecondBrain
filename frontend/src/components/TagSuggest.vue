<template>
  <div v-if="suggestions.length > 0 || loading" class="tag-suggest">
    <div class="suggest-label">
      <el-icon size="13"><MagicStick /></el-icon>
      AI 建议标签
    </div>
    <div class="suggest-chips">
      <span
        v-for="s in suggestions"
        :key="s.tagName"
        class="suggest-chip"
        :class="{ confirmed: s._confirmed, ignored: s._ignored }"
        @click="handleConfirm(s)"
      >
        {{ s.tagName }}
        <span class="suggest-confidence" v-if="!s._confirmed && !s._ignored">{{ s.confidence }}%</span>
        <el-icon v-if="s._confirmed" size="12"><Check /></el-icon>
        <button
          v-if="!s._confirmed && !s._ignored"
          class="suggest-ignore"
          @click.stop="handleIgnore(s)"
        >
          <el-icon size="10"><Close /></el-icon>
        </button>
      </span>
      <span v-if="loading" class="suggest-chip loading">...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { tagsAPI } from "@/api/tags";
import { ElMessage } from "element-plus";
import { MagicStick, Check, Close } from "@element-plus/icons-vue";

const props = defineProps({
  title: { type: String, default: "" },
  summary: { type: String, default: "" },
  nodeId: { type: Number, default: null },
});

const emit = defineEmits(["confirm", "ignore"]);

const suggestions = ref([]);
const loading = ref(false);

async function fetchSuggestions() {
  if (!props.title) return;
  loading.value = true;
  try {
    const data = await tagsAPI.suggest(props.title, props.summary || "");
    suggestions.value = (Array.isArray(data) ? data : []).map((s) => ({
      ...s,
      _confirmed: false,
      _ignored: false,
    }));
  } catch (e) {
    console.warn("AI标签建议失败", e);
  } finally {
    loading.value = false;
  }
}

async function handleConfirm(suggestion) {
  try {
    if (suggestion.existingTagId) {
      if (props.nodeId) {
        await tagsAPI.addToNode(props.nodeId, suggestion.existingTagId);
      }
      suggestion._confirmed = true;
      emit("confirm", suggestion);
    } else {
      const newTag = await tagsAPI.create({ tagName: suggestion.tagName, tagColor: "#6366f1" });
      if (props.nodeId) {
        await tagsAPI.addToNode(props.nodeId, newTag.id);
      }
      suggestion._confirmed = true;
      suggestion.existingTagId = newTag.id;
      emit("confirm", { ...suggestion, existingTagId: newTag.id });
    }
  } catch (e) {
    ElMessage.error("添加标签失败：" + (e.message || "未知错误"));
  }
}

function handleIgnore(suggestion) {
  suggestion._ignored = true;
  emit("ignore", suggestion);
}

defineExpose({ fetchSuggestions });

onMounted(() => {
  fetchSuggestions();
});
</script>

<style scoped>
.tag-suggest {
  margin-top: 12px;
}

.suggest-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.suggest-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.suggest-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  font-size: 12px;
  border-radius: 999px;
  border: 1.5px dashed var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-alpha-10);
  cursor: pointer;
  transition: all 0.15s;
}

.suggest-chip:hover {
  background: var(--color-primary);
  color: #fff;
}

.suggest-chip.confirmed {
  border-style: solid;
  background: var(--color-primary);
  color: #fff;
  cursor: default;
}

.suggest-chip.ignored {
  display: none;
}

.suggest-chip.loading {
  border-color: var(--border-light);
  color: var(--text-muted);
  cursor: default;
  animation: pulse 1.5s ease-in-out infinite;
}

.suggest-confidence {
  font-size: 10px;
  opacity: 0.7;
}

.suggest-ignore {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  color: inherit;
  opacity: 0.6;
}

.suggest-ignore:hover {
  opacity: 1;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
