import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { workspaceAPI } from "@/api/workspace";
import { useUserStore } from "@/stores/user";

const getCurrentWsIdFromToken = () => {
  const token = localStorage.getItem("token");
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.currentWsId || null;
  } catch {
    return null;
  }
};

export const useWorkspaceStore = defineStore("workspace", () => {
  const workspaces = ref([]);
  const currentId = ref(null);
  const currentName = ref("个人空间");
  const loading = ref(false);

  const currentWorkspace = computed(() => {
    return workspaces.value.find((w) => w.id === currentId.value) || null;
  });

  const hasMultiple = computed(() => workspaces.value.length > 1);

  const fetchWorkspaces = async () => {
    try {
      loading.value = true;
      const data = await workspaceAPI.list();
      workspaces.value = data || [];

      const wsId = getCurrentWsIdFromToken();
      if (wsId) {
        const ws = workspaces.value.find((w) => w.id === wsId);
        if (ws) {
          currentId.value = ws.id;
          currentName.value = ws.name;
        }
      }
    } catch (e) {
      workspaces.value = [];
    } finally {
      loading.value = false;
    }
  };

  const switchWorkspace = async (id) => {
    const result = await workspaceAPI.switchWorkspace(id);
    if (result && result.token) {
      const userStore = useUserStore();
      userStore.setToken(result.token);
      localStorage.setItem("token", result.token);
      currentId.value = id;
      const ws = workspaces.value.find((w) => w.id === id);
      if (ws) {
        currentName.value = ws.name;
      }
    }
    return result;
  };

  const setCurrent = (id, name) => {
    currentId.value = id;
    if (name) currentName.value = name;
  };

  const switchToPersonal = async () => {
    const result = await workspaceAPI.switchToPersonal();
    if (result && result.token) {
      const userStore = useUserStore();
      userStore.setToken(result.token);
      localStorage.setItem("token", result.token);
      currentId.value = null;
      currentName.value = "个人空间";
    }
    return result;
  };

  return {
    workspaces,
    currentId,
    currentName,
    loading,
    currentWorkspace,
    hasMultiple,
    fetchWorkspaces,
    switchWorkspace,
    switchToPersonal,
    setCurrent,
  };
});
