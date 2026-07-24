<template>
  <Teleport to="body">
    <TransitionGroup
      name="achievement-toast"
      tag="div"
      class="achievement-toast-container"
    >
      <div
        v-for="item in toasts"
        :key="item.id"
        class="achievement-toast"
        :class="'tier-' + item.tier"
        @click="dismiss(item.id)"
      >
        <div class="toast-icon">
          <el-icon :size="24"><Trophy /></el-icon>
        </div>
        <div class="toast-body">
          <span class="toast-title">成就解锁！</span>
          <span class="toast-name">{{ item.name }}</span>
          <span class="toast-points">+{{ item.pointsReward }} 积分</span>
        </div>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup>
import { ref } from "vue";
import { Trophy } from "@element-plus/icons-vue";

const toasts = ref([]);
let nextId = 0;

const show = (achievement) => {
  const id = ++nextId;
  toasts.value.push({ id, ...achievement });
  setTimeout(() => dismiss(id), 4000);
};

const dismiss = (id) => {
  toasts.value = toasts.value.filter((t) => t.id !== id);
};

defineExpose({ show });
</script>

<style scoped>
.achievement-toast-container {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.achievement-toast {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-lighter);
  cursor: pointer;
  pointer-events: auto;
  min-width: 260px;
}

.achievement-toast.tier-platinum {
  border-left: 4px solid #06b6d4;
}

.achievement-toast.tier-gold {
  border-left: 4px solid #f59e0b;
}

.achievement-toast.tier-silver {
  border-left: 4px solid #94a3b8;
}

.achievement-toast.tier-bronze {
  border-left: 4px solid #d97706;
}

.toast-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: var(--radius-full);
  color: #fff;
  flex-shrink: 0;
}

.toast-body {
  display: flex;
  flex-direction: column;
}

.toast-title {
  font-size: 11px;
  color: var(--color-primary);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.toast-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.toast-points {
  font-size: 12px;
  color: var(--text-secondary);
}

.achievement-toast-enter-active {
  transition: all 0.4s ease;
}

.achievement-toast-leave-active {
  transition: all 0.3s ease;
}

.achievement-toast-enter-from {
  opacity: 0;
  transform: translateX(40px);
}

.achievement-toast-leave-to {
  opacity: 0;
  transform: translateX(40px);
}
</style>
