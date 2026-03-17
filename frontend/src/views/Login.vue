<template>
  <div class="login-container">
    <!-- 动态背景 -->
    <div class="animated-background">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
      <div class="circle circle-4"></div>
      <div class="circle circle-5"></div>
    </div>

    <div class="login-wrapper">
      <el-card class="login-card">
        <template #header>
          <div class="card-header">
            <div class="logo-wrapper">
              <el-icon size="40" class="logo-icon"><Document /></el-icon>
            </div>
            <div class="title-wrapper">
              <h1 class="app-title">AI-SecondBrain</h1>
              <p class="app-subtitle">智能第二大脑</p>
            </div>
          </div>
        </template>

        <el-form
          :model="loginForm"
          :rules="rules"
          ref="loginFormRef"
          label-width="0"
          class="login-form"
        >
          <el-form-item prop="username">
            <div class="input-wrapper">
              <el-icon class="input-icon"><User /></el-icon>
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                clearable
                class="custom-input"
              />
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <div class="input-wrapper">
              <el-icon class="input-icon"><Lock /></el-icon>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                clearable
                class="custom-input"
                @keyup.enter="handleLogin"
              />
            </div>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe" size="large">记住我</el-checkbox>
            <el-link type="primary" :underline="false" class="forgot-link">
              忘记密码？
            </el-link>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              @click="handleLogin"
              :loading="loading"
              class="login-button"
              size="large"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>

          <div class="divider">
            <span>或</span>
          </div>

          <div class="social-login">
            <el-button class="social-btn" circle>
              <el-icon><Platform /></el-icon>
            </el-button>
            <el-button class="social-btn" circle>
              <el-icon><ChatDotRound /></el-icon>
            </el-button>
            <el-button class="social-btn" circle>
              <el-icon><ChatLineRound /></el-icon>
            </el-button>
          </div>

          <el-form-item>
            <div class="links">
              <span class="register-text">还没有账号？</span>
              <router-link to="/register" class="register-link">立即注册</router-link>
            </div>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { 
  Document, 
  User, 
  Lock, 
  Platform, 
  ChatDotRound, 
  ChatLineRound 
} from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { authAPI } from "@/api/auth";

const router = useRouter();
const userStore = useUserStore();

const loginFormRef = ref(null);
const loading = ref(false);
const rememberMe = ref(false);
const loginForm = ref({
  username: "",
  password: "",
});

const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 20, message: "用户名长度在 3-20 个字符", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于 6 位", trigger: "blur" },
  ],
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const response = await authAPI.login(loginForm.value);
        userStore.setToken(response.token);
        userStore.setUserInfo(response.userInfo);
        
        if (rememberMe.value) {
          localStorage.setItem("rememberedUsername", loginForm.value.username);
        } else {
          localStorage.removeItem("rememberedUsername");
        }
        
        ElMessage.success("登录成功");
        router.push("/dashboard");
      } catch (error) {
        ElMessage.error("登录失败：" + error.message);
      } finally {
        loading.value = false;
      }
    }
  });
};

// 页面加载时检查是否有记住的用户名
if (localStorage.getItem("rememberedUsername")) {
  loginForm.value.username = localStorage.getItem("rememberedUsername");
  rememberMe.value = true;
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

/* 动态背景动画 */
.animated-background {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 20s infinite ease-in-out;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -150px;
  left: -150px;
  animation-delay: 0s;
}

.circle-2 {
  width: 200px;
  height: 200px;
  top: 20%;
  right: -100px;
  animation-delay: 3s;
}

.circle-3 {
  width: 250px;
  height: 250px;
  bottom: 10%;
  left: 20%;
  animation-delay: 6s;
}

.circle-4 {
  width: 180px;
  height: 180px;
  bottom: -90px;
  right: 15%;
  animation-delay: 9s;
}

.circle-5 {
  width: 150px;
  height: 150px;
  top: 50%;
  left: 50%;
  animation-delay: 12s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(20px, -30px) scale(1.1);
  }
  50% {
    transform: translate(-20px, 20px) scale(0.9);
  }
  75% {
    transform: translate(30px, 10px) scale(1.05);
  }
}

.login-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 480px;
  padding: 20px;
}

.login-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.3);
  overflow: hidden;
}

.card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.logo-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  margin-bottom: 15px;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
  animation: logoFloat 3s ease-in-out infinite;
}

.logo-icon {
  color: white;
}

@keyframes logoFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.title-wrapper {
  text-align: center;
}

.app-title {
  font-size: 28px;
  font-weight: bold;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  letter-spacing: 1px;
}

.app-subtitle {
  font-size: 14px;
  color: #999;
  margin: 5px 0 0 0;
  letter-spacing: 2px;
}

.login-form {
  padding: 10px 20px 30px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-radius: 12px;
  padding: 12px 16px;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.input-wrapper:focus-within {
  background: white;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.input-icon {
  font-size: 20px;
  color: #667eea;
  margin-right: 12px;
  flex-shrink: 0;
}

.custom-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  background: transparent !important;
  padding: 0 !important;
}

.custom-input :deep(.el-input__inner) {
  font-size: 15px;
  color: #333;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 15px 0;
  padding: 0 5px;
}

.forgot-link {
  font-size: 13px;
  color: #667eea;
  cursor: pointer;
  transition: all 0.3s ease;
}

.forgot-link:hover {
  color: #764ba2;
  transform: translateX(5px);
}

.login-button {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  transition: all 0.3s ease;
  margin-top: 10px;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.4);
}

.login-button:active {
  transform: translateY(0);
}

.divider {
  display: flex;
  align-items: center;
  text-align: center;
  margin: 25px 0;
  color: #999;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid #e0e0e0;
}

.divider span {
  padding: 0 15px;
  font-size: 14px;
  color: #999;
}

.social-login {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 25px;
}

.social-btn {
  width: 50px;
  height: 50px;
  border: 2px solid #e0e0e0;
  background: white;
  color: #666;
  font-size: 22px;
  transition: all 0.3s ease;
}

.social-btn:hover {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-3px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.2);
}

.links {
  text-align: center;
  padding: 15px 0 5px;
}

.register-text {
  color: #666;
  font-size: 14px;
}

.register-link {
  color: #667eea;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  margin-left: 5px;
  transition: all 0.3s ease;
}

.register-link:hover {
  color: #764ba2;
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-wrapper {
    padding: 15px;
  }
  
  .app-title {
    font-size: 24px;
  }
  
  .app-subtitle {
    font-size: 12px;
  }
  
  .circle {
    display: none;
  }
}
</style>
