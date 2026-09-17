<template>
  <el-dialog
    v-model="visible"
    width="min(520px, calc(100vw - 32px))"
    :close-on-click-modal="false"
    @open="loadReminder"
  >
    <template #header>
      <div class="dialog-heading">
        <span class="dialog-kicker">REVIEW REMINDER</span>
        <h2>为知识点预定提醒</h2>
      </div>
    </template>

    <div class="node-preview">
      <span class="node-preview__label">知识点</span>
      <strong>{{ node?.title || "未命名知识点" }}</strong>
    </div>

    <el-form label-position="top">
      <el-form-item label="提醒时间" required>
        <el-date-picker
          v-model="scheduledAt"
          type="datetime"
          placeholder="选择未来的提醒时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm"
          :disabled-date="disablePastDate"
          style="width: 100%"
          aria-label="选择复习提醒时间"
        />
        <p class="field-hint">到时会写入通知中心；若你在复习节奏中开启邮件提醒，系统会同时发送邮件。</p>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-actions">
        <el-button v-if="existingReminder" type="danger" plain :loading="cancelling" @click="cancelReminder">
          取消提醒
        </el-button>
        <span v-else></span>
        <div class="dialog-actions__right">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveReminder">
            {{ existingReminder ? "更新提醒" : "预定提醒" }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { reviewAPI } from "@/api/review";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  node: { type: Object, default: null },
});

const emit = defineEmits(["update:modelValue", "saved", "cancelled"]);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

const existingReminder = ref(null);
const scheduledAt = ref("");
const saving = ref(false);
const cancelling = ref(false);

const formatForPicker = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (number) => String(number).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
};

const loadReminder = async () => {
  existingReminder.value = null;
  scheduledAt.value = "";
  if (!props.node?.id) return;
  try {
    const reminders = await reviewAPI.getReminders();
    existingReminder.value = (reminders || []).find((item) => Number(item.nodeId) === Number(props.node.id) && item.status === "scheduled") || null;
    scheduledAt.value = formatForPicker(existingReminder.value?.scheduledAt);
  } catch (error) {
    ElMessage.error(error.message || "加载提醒失败");
  }
};

const disablePastDate = (date) => date.getTime() < Date.now() - 24 * 60 * 60 * 1000;

const saveReminder = async () => {
  if (!props.node?.id) return;
  if (!scheduledAt.value || new Date(scheduledAt.value).getTime() <= Date.now()) {
    ElMessage.warning("请选择未来的提醒时间");
    return;
  }
  saving.value = true;
  const isUpdate = Boolean(existingReminder.value);
  try {
    const reminder = await reviewAPI.scheduleReminder(props.node.id, { scheduledAt: scheduledAt.value });
    existingReminder.value = reminder;
    ElMessage.success(isUpdate ? "复习提醒已更新" : "复习提醒已预定");
    emit("saved", reminder);
    visible.value = false;
  } catch (error) {
    ElMessage.error(error.message || "预定提醒失败");
  } finally {
    saving.value = false;
  }
};

const cancelReminder = async () => {
  if (!props.node?.id) return;
  try {
    await ElMessageBox.confirm("取消后不会再在该时间发送提醒。", "取消复习提醒", {
      confirmButtonText: "取消提醒",
      cancelButtonText: "保留",
      type: "warning",
    });
  } catch {
    return;
  }
  cancelling.value = true;
  try {
    await reviewAPI.cancelReminder(props.node.id);
    ElMessage.success("复习提醒已取消");
    emit("cancelled", props.node.id);
    visible.value = false;
  } catch (error) {
    ElMessage.error(error.message || "取消提醒失败");
  } finally {
    cancelling.value = false;
  }
};
</script>

<style scoped>
.dialog-heading h2 {
  margin: 3px 0 0;
  color: var(--text-primary);
  font-family: var(--font-family-display);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
}

.dialog-kicker {
  color: var(--color-primary);
  font-family: var(--font-family-ui);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: 0.1em;
}

.node-preview {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 0 0 var(--spacing-xl);
  padding: var(--spacing-lg);
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-md);
  background: var(--bg-list-item);
}

.node-preview__label,
.field-hint {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.node-preview strong {
  color: var(--text-primary);
  font-family: var(--font-family-display);
  font-size: var(--font-size-lg);
  line-height: 1.4;
}

.field-hint {
  margin: 8px 0 0;
  line-height: 1.6;
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
</style>
