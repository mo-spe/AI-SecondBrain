<template>
  <AuthShell>
    <header class="form-header">
      <span class="eyebrow">WELCOME BACK</span>
      <h1>继续构建你的<br />第二大脑</h1>
      <p>登录后回到你的知识、研究与学习进度。</p>
    </header>
    <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-position="top" class="auth-form" @submit.prevent>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="loginForm.username" size="large" autocomplete="username" placeholder="输入用户名" clearable />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="loginForm.password" size="large" autocomplete="current-password" type="password" show-password placeholder="输入密码" @keyup.enter="handleLogin" />
      </el-form-item>
      <div class="form-options"><el-checkbox v-model="rememberMe">记住用户名</el-checkbox><span>知识，从上次离开的地方继续</span></div>
      <el-button class="submit-button" type="primary" native-type="submit" :loading="loading" @click="handleLogin">
        {{ loading ? "正在进入工作空间" : "进入工作空间" }}<el-icon v-if="!loading"><ArrowRight /></el-icon>
      </el-button>
      <p class="switch-link">还没有账户？<router-link to="/register">创建一个知识空间</router-link></p>
    </el-form>
  </AuthShell>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowRight } from "@element-plus/icons-vue";
import AuthShell from "@/components/AuthShell.vue";
import { useUserStore } from "@/stores/user";
import { authAPI } from "@/api/auth";

const router = useRouter();
const userStore = useUserStore();
const loginFormRef = ref(null);
const loading = ref(false);
const rememberMe = ref(false);
const loginForm = ref({ username: "", password: "" });
const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }, { min: 3, max: 20, message: "用户名长度为 3—20 个字符", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }, { min: 6, message: "密码至少需要 6 个字符", trigger: "blur" }],
};

const handleLogin = async () => {
  if (!loginFormRef.value || loading.value) return;
  const valid = await loginFormRef.value.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    const response = await authAPI.login(loginForm.value);
    userStore.setToken(response.token);
    userStore.setUserInfo(response.userInfo);
    if (rememberMe.value) localStorage.setItem("rememberedUsername", loginForm.value.username);
    else localStorage.removeItem("rememberedUsername");
    ElMessage.success("欢迎回来");
    await router.push("/dashboard");
  } catch (error) {
    ElMessage.error("登录失败：" + error.message);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  const rememberedUsername = localStorage.getItem("rememberedUsername");
  if (rememberedUsername) {
    loginForm.value.username = rememberedUsername;
    rememberMe.value = true;
  }
});
</script>

<style scoped>
.form-header { margin-bottom: 38px; }.eyebrow { color: var(--color-accent); font: 700 11px/1 var(--font-family-ui); letter-spacing: .18em; }.form-header h1 { margin: 13px 0 12px; font-size: clamp(42px, 4.4vw, 62px); font-weight: 500; letter-spacing: -.045em; line-height: 1.02; }.form-header p { color: var(--text-secondary); font-size: 16px; }
.auth-form :deep(.el-form-item) { margin-bottom: 23px; }.auth-form :deep(.el-form-item__label) { height: auto; padding-bottom: 8px; color: var(--text-primary); font-size: 13px; font-weight: 650; }.auth-form :deep(.el-input__wrapper) { min-height: 52px; border: 1px solid var(--border-light); border-radius: 8px; background: rgba(253,252,250,.75); box-shadow: none; }.auth-form :deep(.el-input__wrapper.is-focus) { border-color: var(--color-primary); box-shadow: var(--shadow-focus-ring); }
.form-options { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin: -5px 0 24px; color: var(--text-muted); font-size: 12px; }.submit-button { width: 100%; min-height: 52px; gap: 9px; border-radius: 8px; font-size: 15px; font-weight: 700; box-shadow: 0 12px 26px rgba(43,95,75,.18); }.switch-link { margin-top: 24px; color: var(--text-secondary); font-size: 14px; text-align: center; }.switch-link a { color: var(--color-primary); font-weight: 700; text-decoration: none; }.switch-link a:hover { text-decoration: underline; text-underline-offset: 3px; }
@media (max-width: 520px) { .form-header h1 { font-size: 40px; }.form-options { align-items: flex-start; flex-direction: column; gap: 5px; } }
</style>
