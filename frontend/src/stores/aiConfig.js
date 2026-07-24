import { defineStore } from "pinia";
import { ref } from "vue";
import { aiAPI } from "@/api/ai";

export const useAiConfigStore = defineStore("aiConfig", () => {
  const providers = ref([]);
  const configs = ref([]);
  const loading = ref(false);

  const scenarioLabels = {
    chat: "对话",
    extraction: "知识提取",
    question_gen: "题目生成",
    embedding: "Embedding向量化",
    research: "研究报告",
  };

  const scenarioCodes = Object.keys(scenarioLabels);

  async function fetchProviders() {
    try {
      const data = await aiAPI.getProviders();
      providers.value = Array.isArray(data) ? data : [];
    } catch (error) {
      console.error("加载服务商列表失败:", error);
      providers.value = [];
    }
  }

  async function fetchConfigs() {
    loading.value = true;
    try {
      const data = await aiAPI.getUserAiConfig();
      configs.value = Array.isArray(data) ? data : [];
    } catch (error) {
      console.error("加载AI配置失败:", error);
      configs.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function saveConfigs(payload) {
    await aiAPI.saveUserAiConfig(payload);
    await fetchConfigs();
  }

  async function saveProviderKey(providerId, apiKey) {
    await aiAPI.saveProviderKey(providerId, apiKey);
  }

  function getConfigForScenario(scenarioCode) {
    return configs.value.find((c) => c.scenarioCode === scenarioCode) || null;
  }

  return {
    providers,
    configs,
    loading,
    scenarioLabels,
    scenarioCodes,
    fetchProviders,
    fetchConfigs,
    saveConfigs,
    saveProviderKey,
    getConfigForScenario,
  };
});
