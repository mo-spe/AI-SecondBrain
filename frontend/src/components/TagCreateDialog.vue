<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑标签' : '新建标签'"
    width="420px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
      <el-form-item label="标签名称" prop="tagName">
        <el-input
          v-model="form.tagName"
          placeholder="请输入标签名称"
          maxlength="20"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="标签颜色" prop="tagColor">
        <div class="color-picker-row">
          <div
            v-for="color in presetColors"
            :key="color"
            class="color-swatch"
            :class="{ active: form.tagColor === color }"
            :style="{ background: color }"
            @click="form.tagColor = color"
          ></div>
          <el-color-picker
            v-model="form.tagColor"
            :predefine="presetColors"
            size="default"
            class="custom-picker"
          />
        </div>
      </el-form-item>
      <el-form-item label="父标签">
        <el-select
          v-model="form.parentId"
          placeholder="无（作为顶级标签）"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="tag in flatTagList"
            :key="tag.id"
            :label="tag.tagName"
            :value="tag.id"
            :disabled="tag.id === currentTagId"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :loading="loading">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  modelValue: Boolean,
  /** 编辑模式：传入要编辑的标签 */
  tag: { type: Object, default: null },
  /** 可选的父标签列表（扁平） */
  availableTags: { type: Array, default: () => [] },
});

const emit = defineEmits(["update:modelValue", "saved"]);

const presetColors = [
  "#6366f1", "#3b82f6", "#22c55e", "#f59e0b",
  "#ef4444", "#ec4899", "#8b5cf6", "#06b6d4",
];

const formRef = ref(null);
const loading = ref(false);

const visible = ref(props.modelValue);
const isEdit = ref(false);
const currentTagId = ref(null);

const form = reactive({
  tagName: "",
  tagColor: "#6366f1",
  parentId: null,
});

const flatTagList = ref([]);

const rules = {
  tagName: [{ required: true, message: "请输入标签名称", trigger: "blur" }],
};

watch(() => props.modelValue, (val) => {
  visible.value = val;
  if (val) {
    flatTagList.value = flattenTags(props.availableTags);
    if (props.tag) {
      isEdit.value = true;
      currentTagId.value = props.tag.id;
      form.tagName = props.tag.tagName;
      form.tagColor = props.tag.tagColor || "#6366f1";
      form.parentId = props.tag.parentId || null;
    } else {
      isEdit.value = false;
      currentTagId.value = null;
      form.tagName = "";
      form.tagColor = "#6366f1";
      form.parentId = null;
    }
  }
});

watch(visible, (val) => {
  emit("update:modelValue", val);
});

function flattenTags(tags, result = []) {
  for (const tag of tags) {
    result.push(tag);
    if (tag.children && tag.children.length > 0) {
      flattenTags(tag.children, result);
    }
  }
  return result;
}

const handleClosed = () => {
  formRef.value?.resetFields();
};

const handleConfirm = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  loading.value = true;
  try {
    emit("saved", {
      tagName: form.tagName.trim(),
      tagColor: form.tagColor,
      parentId: form.parentId || null,
    });
    visible.value = false;
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.color-picker-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.color-swatch {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: transform 0.15s, border-color 0.15s;
}

.color-swatch:hover {
  transform: scale(1.15);
}

.color-swatch.active {
  border-color: var(--text-primary);
  transform: scale(1.1);
}

.custom-picker {
  margin-left: 4px;
}
</style>
