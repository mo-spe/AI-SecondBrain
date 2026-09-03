<template>
  <AuthShell>
    <header class="form-header">
      <span class="eyebrow">CREATE YOUR SPACE</span>
      <h1>从今天开始，<br />积累长期知识</h1>
      <p>创建你的私人知识空间，之后再决定分享什么。</p>
    </header>
    <el-form ref="registerFormRef" :model="registerForm" :rules="rules" label-position="top" class="auth-form" @submit.prevent>
      <el-form-item label="用户名" prop="username"><el-input v-model="registerForm.username" size="large" autocomplete="username" placeholder="3—20 位字母、数字或下划线" clearable /></el-form-item>
      <el-form-item label="密码" prop="password"><el-input v-model="registerForm.password" size="large" autocomplete="new-password" type="password" show-password placeholder="至少 6 个字符" /></el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="registerForm.confirmPassword" size="large" autocomplete="new-password" type="password" show-password placeholder="再次输入密码" @keyup.enter="handleRegister" /></el-form-item>
      <el-checkbox v-model="agreementAccepted" class="agreement">我已阅读并同意服务条款与隐私说明</el-checkbox>
      <el-button class="submit-button" type="primary" native-type="submit" :loading="loading" :disabled="!agreementAccepted" @click="handleRegister">
        {{ loading ? "正在创建知识空间" : "创建知识空间" }}<el-icon v-if="!loading"><ArrowRight /></el-icon>
      </el-button>
      <p class="switch-link">已经有账户？<router-link to="/login">返回登录</router-link></p>
    </el-form>
  </AuthShell>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowRight } from "@element-plus/icons-vue";
import AuthShell from "@/components/AuthShell.vue";
import { authAPI } from "@/api/auth";

const router = useRouter();
const registerFormRef = ref(null);
const loading = ref(false);
const agreementAccepted = ref(false);
const registerForm = ref({ username: "", password: "", confirmPassword: "" });
const validateConfirmPassword = (_rule, value, callback) => {
  if (!value) callback(new Error("请再次输入密码"));
  else if (value !== registerForm.value.password) callback(new Error("两次输入的密码不一致"));
  else callback();
};
const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }, { min: 3, max: 20, message: "用户名长度为 3—20 个字符", trigger: "blur" }, { pattern: /^[a-zA-Z0-9_]+$/, message: "只能使用字母、数字和下划线", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }, { min: 6, message: "密码至少需要 6 个字符", trigger: "blur" }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: "blur" }],
};

const handleRegister = async () => {
  if (!registerFormRef.value || loading.value) return;
  if (!agreementAccepted.value) {
    ElMessage.warning("请先同意服务条款与隐私说明");
    return;
  }
  const valid = await registerFormRef.value.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    await authAPI.register({ username: registerForm.value.username, password: registerForm.value.password });
    ElMessage.success("账户创建成功，请登录");
    await router.push("/login");
  } catch (error) {
    ElMessage.error("注册失败：" + error.message);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.form-header { margin-bottom: 30px; }.eyebrow { color: var(--color-accent); font: 700 11px/1 var(--font-family-ui); letter-spacing: .18em; }.form-header h1 { margin: 13px 0 12px; font-size: clamp(38px, 4vw, 55px); font-weight: 500; letter-spacing: -.04em; line-height: 1.04; }.form-header p { color: var(--text-secondary); font-size: 15px; }
.auth-form :deep(.el-form-item) { margin-bottom: 17px; }.auth-form :deep(.el-form-item__label) { height: auto; padding-bottom: 7px; color: var(--text-primary); font-size: 13px; font-weight: 650; }.auth-form :deep(.el-input__wrapper) { min-height: 50px; border: 1px solid var(--border-light); border-radius: 8px; background: rgba(253,252,250,.75); box-shadow: none; }.auth-form :deep(.el-input__wrapper.is-focus) { border-color: var(--color-primary); box-shadow: var(--shadow-focus-ring); }
.agreement { min-height: 44px; margin: 0 0 16px; }.submit-button { width: 100%; min-height: 52px; gap: 9px; border-radius: 8px; font-size: 15px; font-weight: 700; box-shadow: 0 12px 26px rgba(43,95,75,.18); }.switch-link { margin-top: 22px; color: var(--text-secondary); font-size: 14px; text-align: center; }.switch-link a { color: var(--color-primary); font-weight: 700; text-decoration: none; }.switch-link a:hover { text-decoration: underline; text-underline-offset: 3px; }
@media (max-width: 520px) { .form-header h1 { font-size: 38px; } }
</style>
