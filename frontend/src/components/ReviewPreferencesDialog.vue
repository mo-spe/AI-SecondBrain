<template>
  <el-dialog
    v-model="visible"
    title="复习节奏"
    width="min(560px, calc(100vw - 32px))"
    :close-on-click-modal="false"
    @open="loadPreferences"
  >
    <div class="preference-intro">
      <span class="preference-kicker">YOUR REVIEW RHYTHM</span>
      <p>设置答对后的复习间隔。保存不会改变已经排定的卡片，只会从你下一次答题后生效。</p>
    </div>

    <el-form label-position="top" class="preference-form">
      <el-form-item label="复习间隔（天）" required>
        <div class="interval-editor">
          <div v-for="(day, index) in intervalDays" :key="index" class="interval-cell">
            <span class="interval-step">第 {{ index + 1 }} 次</span>
            <el-input-number
              v-model="intervalDays[index]"
              :min="1"
              :max="3650"
              :controls="false"
              aria-label="复习间隔天数"
            />
            <span class="interval-unit">天后</span>
            <button
              v-if="intervalDays.length > 3"
              type="button"
              class="interval-remove"
              :aria-label="`删除第 ${index + 1} 个复习间隔`"
              @click="removeInterval(index)"
            >
              ×
            </button>
          </div>
          <el-button
            v-if="intervalDays.length < 8"
            plain
            class="interval-add"
            @click="addInterval"
          >
            添加一个阶段
          </el-button>
        </div>
        <p class="field-hint">请保持递增，支持 3 至 8 个阶段。答错时系统会缩短间隔，但不会短于第一阶段。</p>
      </el-form-item>

      <el-form-item label="邮件提醒">
        <el-switch
          v-model="reviewEmailEnabled"
          active-text="同时发送邮件"
          inactive-text="仅站内通知"
        />
        <p class="field-hint">指定时间提醒始终写入通知中心；已启用邮件服务时才会额外发送邮件。</p>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-actions">
        <el-button @click="restoreDefaults">恢复默认</el-button>
        <div class="dialog-actions__right">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="savePreferences">保存节奏</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import { reviewAPI } from "@/api/review";

const DEFAULT_INTERVAL_DAYS = [1, 7, 14, 30, 60];

const props = defineProps({
  modelValue: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue", "saved"]);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

const intervalDays = ref([...DEFAULT_INTERVAL_DAYS]);
const reviewEmailEnabled = ref(false);
const saving = ref(false);

const loadPreferences = async () => {
  try {
    const preference = await reviewAPI.getPreferences();
    const days = preference?.intervalDays;
    intervalDays.value = Array.isArray(days) && days.length ? [...days] : [...DEFAULT_INTERVAL_DAYS];
    reviewEmailEnabled.value = Boolean(preference?.reviewEmailEnabled);
  } catch (error) {
    ElMessage.error(error.message || "加载复习设置失败");
  }
};

const addInterval = () => {
  const lastDay = Number(intervalDays.value.at(-1)) || 1;
  intervalDays.value.push(Math.min(3650, lastDay + 7));
};

const removeInterval = (index) => {
  intervalDays.value.splice(index, 1);
};

const restoreDefaults = () => {
  intervalDays.value = [...DEFAULT_INTERVAL_DAYS];
};

const validateIntervals = () => {
  if (intervalDays.value.length < 3 || intervalDays.value.length > 8) {
    return "请设置 3 至 8 个复习阶段";
  }
  const normalized = intervalDays.value.map((day) => Number(day));
  if (normalized.some((day) => !Number.isInteger(day) || day < 1 || day > 3650)) {
    return "每个间隔必须是 1 至 3650 之间的整数天数";
  }
  if (normalized.some((day, index) => index > 0 && day <= normalized[index - 1])) {
    return "复习间隔需要按时间递增";
  }
  return null;
};

const savePreferences = async () => {
  const validationError = validateIntervals();
  if (validationError) {
    ElMessage.warning(validationError);
    return;
  }
  saving.value = true;
  try {
    const preference = await reviewAPI.updatePreferences({
      intervalDays: intervalDays.value.map(Number),
      reviewEmailEnabled: reviewEmailEnabled.value,
    });
    ElMessage.success("复习节奏已保存，将从下一次答题后生效");
    emit("saved", preference);
    visible.value = false;
  } catch (error) {
    ElMessage.error(error.message || "保存复习设置失败");
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.preference-intro {
  margin: -2px 0 var(--spacing-xl);
  padding: var(--spacing-lg);
  border-left: 3px solid var(--color-accent);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  background: var(--color-primary-alpha-10);
}

.preference-kicker {
  display: block;
  margin-bottom: 6px;
  color: var(--color-primary);
  font-family: var(--font-family-ui);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: 0.08em;
}

.preference-intro p,
.field-hint {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.65;
}

.preference-form :deep(.el-form-item__label) {
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
}

.interval-editor {
  display: grid;
  gap: 8px;
}

.interval-cell {
  display: grid;
  grid-template-columns: 72px minmax(88px, 116px) auto 28px;
  align-items: center;
  gap: 8px;
  min-height: 44px;
}

.interval-step {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.interval-unit {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.interval-remove {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}

.interval-remove:hover {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.interval-remove:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus-ring);
}

.interval-add {
  justify-self: start;
  min-height: 40px;
}

.field-hint {
  margin-top: 8px;
}

.dialog-actions,
.dialog-actions__right {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.dialog-actions {
  justify-content: space-between;
}

@media (max-width: 480px) {
  .interval-cell {
    grid-template-columns: 64px minmax(76px, 1fr) auto 26px;
    gap: 6px;
  }
}
</style>
