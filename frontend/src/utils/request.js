import axios from "axios";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";

const request = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 300000,
});

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
      ElMessage.error(res.message || "请求失败");
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
