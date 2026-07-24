<template>
  <button
    class="checkin-btn"
    :class="{ checked: checkedIn, loading }"
    :disabled="checkedIn || loading"
    @click="handleCheckIn"
  >
    <el-icon :size="16">
      <component :is="checkedIn ? CircleCheck : Sunny" />
    </el-icon>
    <span>{{ checkedIn ? '已签到' : '签到' }}</span>
    <span v-if="checkedIn && streak" class="streak-badge">{{ streak }}天</span>
  </button>
</template>

<script setup>
import { ref, computed } from "vue";
import { ElMessage } from "element-plus";
import { Sunny, CircleCheck } from "@element-plus/icons-vue";
import { useGamificationStore } from "@/stores/gamification";

const emit = defineEmits(["checkedIn"]);

const gamificationStore = useGamificationStore();
const loading = ref(false);

const checkedIn = computed(() => gamificationStore.profile?.checkedInToday ?? false);
const streak = computed(() => gamificationStore.profile?.currentStreak ?? 0);

const handleCheckIn = async () => {
  loading.value = true;
  try {
    const result = await gamificationStore.checkIn();
    const points = result.pointsEarned || 0;
    ElMessage.success(`签到成功！+${points}积分${result.currentStreak ? '，连续' + result.currentStreak + '天' : ''}`);
    emit("checkedIn", result);
  } catch (e) {
    ElMessage.error(e.message || "签到失败");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.checkin-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.checkin-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.checkin-btn:disabled {
  cursor: default;
}

.checkin-btn.checked {
  background: var(--bg-card);
  border-color: var(--border-light);
  color: var(--text-secondary);
}

.checkin-btn .streak-badge {
  background: rgba(255, 255, 255, 0.25);
  padding: 1px 6px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
}

.checkin-btn.checked .streak-badge {
  background: rgba(99, 102, 241, 0.1);
}
</style>
