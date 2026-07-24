import axios from "axios";
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/stores/user";
import router from "@/router";

const request = axios.create({
  baseURL: "/api",
  timeout: 300000,
});

// 检查是否为AI未配置错误，若是则弹窗引导用户前往设置页
function handleAiNotConfigured(message) {
  if (message && message.includes("请先在设置页配置")) {
    ElMessageBox.confirm(
      message + "\n\n是否前往个人设置页配置AI服务商和模型？",
      "AI 未配置",
      {
        confirmButtonText: "前往设置",
        cancelButtonText: "稍后再说",
        type: "warning",
      }
    ).then(() => {
      router.push("/settings");
    }).catch(() => {});
    return true;
  }
  return false;
}

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

request.interceptors.response.use(
  (response) => {
    const res = response.data;

    if (response.config.responseType === "blob") {
      return response.data;
    }

    if (res.code === 200) {
      return res.data;
    } else {
      if (!handleAiNotConfigured(res.message)) {
        ElMessage.error(res.message || "请求失败");
      }
      return Promise.reject(new Error(res.message || "请求失败"));
    }
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      const userStore = useUserStore();
      userStore.logout();
      ElMessage.error("登录已过期，请重新登录");
    } else {
      ElMessage.error(error.message || "网络错误");
    }
    return Promise.reject(error);
  },
);

export default request;
