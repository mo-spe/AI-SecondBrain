<template>
  <div class="tag-chips">
    <span
      v-for="tag in tags"
      :key="tag.id"
      class="tag-chip"
      :style="{ background: tag.tagColor + '18', color: tag.tagColor, borderColor: tag.tagColor + '40' }"
    >
      {{ tag.tagName }}
      <button v-if="editable" class="tag-remove" @click.stop="$emit('remove', tag)">
        <el-icon size="10"><Close /></el-icon>
      </button>
    </span>
    <div v-if="editable" class="tag-add-wrapper">
      <button class="tag-add-btn" @click.stop="showSearch = !showSearch">
        <el-icon size="12"><Plus /></el-icon>
      </button>
      <div v-if="showSearch" class="tag-search-dropdown" @click.stop>
        <div class="tag-search-input-wrap">
          <input
            ref="searchInput"
            v-model="searchKeyword"
            type="text"
            placeholder="搜索标签..."
            class="tag-search-input"
            @keyup.enter="handleCreateQuick"
          />
        </div>
        <div class="tag-search-results">
          <div
            v-for="tag in filteredAvailableTags"
            :key="tag.id"
            class="tag-search-item"
            @click="handleAddTag(tag)"
          >
            <span class="tag-color-dot" :style="{ background: tag.tagColor || '#6366f1' }"></span>
            {{ tag.tagName }}
          </div>
          <div
            v-if="searchKeyword && !filteredAvailableTags.length"
            class="tag-search-item new-tag"
            @click="handleCreateQuick"
          >
            <el-icon size="12"><Plus /></el-icon>
            创建标签 "{{ searchKeyword }}"
          </div>
          <div v-if="!searchKeyword && !filteredAvailableTags.length" class="tag-search-empty">
            暂无可添加的标签
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch } from "vue";
import { tagsAPI } from "@/api/tags";
import { ElMessage } from "element-plus";

const props = defineProps({
  tags: { type: Array, default: () => [] },
  editable: { type: Boolean, default: false },
  nodeId: { type: Number, default: null },
  availableTags: { type: Array, default: () => [] },
});

const emit = defineEmits(["add", "remove", "refresh"]);

const showSearch = ref(false);
const searchKeyword = ref("");
const searchInput = ref(null);

const filteredAvailableTags = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  if (!kw) return props.availableTags.filter(t => !props.tags.find(pt => pt.id === t.id));
  return props.availableTags.filter(t =>
    t.tagName.toLowerCase().includes(kw) && !props.tags.find(pt => pt.id === t.id)
  );
});

watch(showSearch, async (val) => {
  if (val) {
    searchKeyword.value = "";
    await nextTick();
    searchInput.value?.focus();
  }
});

async function handleAddTag(tag) {
  try {
    await tagsAPI.addToNode(props.nodeId, tag.id);
    emit("add", tag);
    showSearch.value = false;
    ElMessage.success("标签已添加");
  } catch (e) {
    ElMessage.error("添加标签失败：" + (e.message || "未知错误"));
  }
}

async function handleCreateQuick() {
  const name = searchKeyword.value.trim();
  if (!name) return;
  try {
    const newTag = await tagsAPI.create({ tagName: name, tagColor: "#6366f1" });
    await tagsAPI.addToNode(props.nodeId, newTag.id);
    emit("add", newTag);
    showSearch.value = false;
    ElMessage.success("标签已创建并添加");
  } catch (e) {
    ElMessage.error("创建标签失败：" + (e.message || "未知错误"));
  }
}
</script>

<style scoped>
.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 8px;
  font-size: 11px;
  border-radius: 999px;
  border: 1px solid;
  line-height: 1.6;
  white-space: nowrap;
}

.tag-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  color: inherit;
  opacity: 0.6;
  transition: opacity 0.15s;
}

.tag-remove:hover {
  opacity: 1;
}

.tag-add-wrapper {
  position: relative;
}

.tag-add-btn {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px dashed var(--border-light);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-muted);
  transition: all 0.15s;
}

.tag-add-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-alpha-10);
}

.tag-search-dropdown {
  position: absolute;
  top: 28px;
  left: 0;
  width: 200px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  z-index: 100;
  overflow: hidden;
}

.tag-search-input-wrap {
  padding: 8px;
  border-bottom: 1px solid var(--border-lighter);
}

.tag-search-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 12px;
  color: var(--text-primary);
  background: transparent;
}

.tag-search-input::placeholder {
  color: var(--text-placeholder);
}

.tag-search-results {
  max-height: 160px;
  overflow-y: auto;
}

.tag-search-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--text-regular);
  cursor: pointer;
  transition: background 0.1s;
}

.tag-search-item:hover {
  background: var(--bg-hover);
}

.tag-search-item.new-tag {
  color: var(--color-primary);
  border-top: 1px solid var(--border-lighter);
}

.tag-search-empty {
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
}

.tag-color-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
