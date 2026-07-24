import request from "@/utils/request";

export const aiAPI = {
  getProviders() {
    return request({
      url: "/ai/providers",
      method: "get",
    });
  },

  getModels(providerId) {
    return request({
      url: `/ai/providers/${providerId}/models`,
      method: "get",
    });
  },

  getUserAiConfig() {
    return request({
      url: "/user/ai-config",
      method: "get",
    });
  },

  saveUserAiConfig(configs) {
    return request({
      url: "/user/ai-config",
      method: "put",
      data: configs,
    });
  },

  saveProviderKey(providerId, apiKey) {
    return request({
      url: "/user/ai-config/provider-key",
      method: "put",
      data: { providerId, apiKey },
    });
  },
};
