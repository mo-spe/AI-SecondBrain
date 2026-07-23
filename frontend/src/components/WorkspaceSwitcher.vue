<template>
  <div class="ws-switcher">
    <button class="ws-trigger" @click="toggle" :aria-expanded="open">
      <span class="ws-dot" :class="activityClass"></span>
      <span class="ws-name">{{ workspaceStore.currentName }}</span>
      <svg class="ws-chevron" :class="{ open }" width="12" height="12" viewBox="0 0 12 12" fill="none">
        <path d="M3 5l3 3 3-3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>

    <teleport to="body">
      <transition name="ws-drop">
        <div v-if="open" class="ws-dropdown" :style="dropdownStyle">
          <div class="ws-dropdown-header">切换工作区</div>
          <div class="ws-list">
            <button
              class="ws-option"
              :class="{ current: !workspaceStore.currentId }"
              @click="selectPersonal"
            >
              <span class="ws-option-dot" :class="!workspaceStore.currentId ? 'active' : ''"></span>
              <span class="ws-option-name">个人空间</span>
              <span v-if="!workspaceStore.currentId" class="ws-option-badge">当前</span>
            </button>
            <button
              v-for="ws in workspaceStore.workspaces"
              :key="ws.id"
              class="ws-option"
              :class="{ current: ws.id === workspaceStore.currentId }"
              @click="select(ws)"
            >
              <span class="ws-option-dot" :class="ws.id === workspaceStore.currentId ? 'active' : ''"></span>
              <span class="ws-option-name">{{ ws.name }}</span>
              <span v-if="ws.id === workspaceStore.currentId" class="ws-option-badge">当前</span>
            </button>
          </div>
          <div class="ws-dropdown-footer">
            <button class="ws-manage-btn" @click="goManage">管理所有工作区</button>
          </div>
        </div>
      </transition>
    </teleport>

    <div v-if="open" class="ws-backdrop" @click="close"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useWorkspaceStore } from "@/stores/workspace";

const router = useRouter();
const workspaceStore = useWorkspaceStore();

const open = ref(false);
const triggerEl = ref(null);
const dropdownStyle = ref({});

const activityClass = computed(() => {
  if (!workspaceStore.currentId) return "idle";
  return "active";
});

const toggle = () => {
  open.value = !open.value;
  if (open.value) {
    nextTick(() => positionDropdown());
  }
};

const close = () => {
  open.value = false;
};

const select = async (ws) => {
  if (ws.id === workspaceStore.currentId) {
    close();
    return;
  }
  try {
    await workspaceStore.switchWorkspace(ws.id);
    close();
    window.location.reload();
  } catch (e) {
    ElMessage.error("切换工作区失败：" + (e.message || "未知错误"));
  }
};

const selectPersonal = async () => {
  if (!workspaceStore.currentId) {
    close();
    return;
  }
  try {
    await workspaceStore.switchToPersonal();
    close();
    window.location.reload();
  } catch (e) {
    ElMessage.error("切换到个人空间失败：" + (e.message || "未知错误"));
  }
};

const goManage = () => {
  close();
  router.push("/settings");
};

const positionDropdown = () => {
  const btn = document.querySelector(".ws-trigger");
  if (!btn) return;
  const rect = btn.getBoundingClientRect();
  dropdownStyle.value = {
    top: rect.bottom + 6 + "px",
    left: rect.left + "px",
    minWidth: Math.max(rect.width, 200) + "px",
  };
};

const onResize = () => {
  if (open.value) positionDropdown();
};

onMounted(() => {
  window.addEventListener("resize", onResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", onResize);
});
</script>

<style scoped>
.ws-switcher {
  position: relative;
}

.ws-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  cursor: pointer;
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  font-family: var(--font-family-base);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
  white-space: nowrap;
  line-height: 1.4;
}

.ws-trigger:hover {
  border-color: var(--border-base);
  box-shadow: var(--shadow-sm);
}

.ws-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #C5CDC5;
  flex-shrink: 0;
  transition: background var(--transition-base);
}

.ws-dot.active {
  background: var(--color-primary);
}

.ws-dot.idle {
  background: #C5CDC5;
}

.ws-name {
  font-weight: var(--font-weight-medium);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ws-chevron {
  color: var(--text-secondary);
  transition: transform var(--transition-fast);
  flex-shrink: 0;
}

.ws-chevron.open {
  transform: rotate(180deg);
}

.ws-dropdown {
  position: fixed;
  z-index: 3000;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-dropdown);
  overflow: hidden;
}

.ws-dropdown-header {
  padding: 10px 14px;
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  border-bottom: 1px solid var(--border-lighter);
}

.ws-list {
  max-height: 240px;
  overflow-y: auto;
  padding: 4px;
}

.ws-option {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 10px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  cursor: pointer;
  font-family: var(--font-family-base);
  font-size: var(--font-size-base);
  color: var(--text-primary);
  transition: background var(--transition-fast);
  text-align: left;
}

.ws-option:hover {
  background: var(--bg-hover);
}

.ws-option.current {
  background: var(--color-primary-alpha-10);
}

.ws-option-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #D5D0C8;
  flex-shrink: 0;
}

.ws-option-dot.active {
  background: var(--color-primary);
}

.ws-option-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ws-option-badge {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  background: var(--color-primary-alpha-10);
  padding: 1px 6px;
  border-radius: var(--radius-xs);
  font-weight: var(--font-weight-medium);
}

.ws-dropdown-footer {
  border-top: 1px solid var(--border-lighter);
  padding: 6px;
}

.ws-manage-btn {
  width: 100%;
  padding: 8px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  cursor: pointer;
  font-family: var(--font-family-base);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  transition: background var(--transition-fast), color var(--transition-fast);
}

.ws-manage-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.ws-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2999;
}

.ws-drop-enter-active,
.ws-drop-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.ws-drop-enter-from,
.ws-drop-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
