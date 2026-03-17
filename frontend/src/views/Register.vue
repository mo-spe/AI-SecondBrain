<template>
  <div class="register-container">
    <!-- 动态背景 -->
    <div class="animated-background">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
      <div class="circle circle-4"></div>
      <div class="circle circle-5"></div>
    </div>

    <div class="register-wrapper">
      <el-card class="register-card">
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
          :model="registerForm"
          :rules="rules"
          ref="registerFormRef"
          label-width="0"
          class="register-form"
        >
          <el-form-item prop="username">
            <div class="input-wrapper">
              <el-icon class="input-icon"><User /></el-icon>
              <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名（3-20 个字符）"
                clearable
                class="custom-input"
              />
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <div class="input-wrapper">
              <el-icon class="input-icon"><Lock /></el-icon>
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码（至少 6 位）"
                show-password
                clearable
                class="custom-input"
              />
            </div>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <div class="input-wrapper">
              <el-icon class="input-icon"><Lock /></el-icon>
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password
                clearable
                class="custom-input"
              />
            </div>
          </el-form-item>

          <div class="agreement-wrapper">
            <el-checkbox v-model="agreementAccepted" size="large">
              <span class="agreement-text">
                我已阅读并同意
                <el-link type="primary" :underline="false" class="agreement-link">
                  《用户服务协议》
                </el-link>
                和
                <el-link type="primary" :underline="false" class="agreement-link">
                  《隐私政策》
                </el-link>
              </span>
            </el-checkbox>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              @click="handleRegister"
              :loading="loading"
              class="register-button"
              size="large"
              :disabled="!agreementAccepted"
            >
              <span v-if="!loading">立即注册</span>
              <span v-else>注册中...</span>
            </el-button>
          </el-form-item>

          <div class="divider">
            <span>或</span>
          </div>

          <div class="social-register">
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
              <span class="login-text">已有账号？</span>
              <router-link to="/login" class="login-link">立即登录</router-link>
            </div>
          </el-form-item>

          <div class="benefits">
            <div class="benefit-item">
              <el-icon class="benefit-icon"><CircleCheck /></el-icon>
              <span>智能知识点提取</span>
            </div>
            <div class="benefit-item">
              <el-icon class="benefit-icon"><CircleCheck /></el-icon>
              <span>艾宾浩斯记忆曲线</span>
            </div>
            <div class="benefit-item">
              <el-icon class="benefit-icon"><CircleCheck /></el-icon>
              <span>AI 智能问答</span>
            </div>
          </div>
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
  ChatLineRound,
  CircleCheck
} from "@element-plus/icons-vue";
import { authAPI } from "@/api/auth";

const router = useRouter();

const loading = ref(false);
const agreementAccepted = ref(false);
const registerFormRef = ref(null);
const registerForm = ref({
  username: "",
  password: "",
  confirmPassword: "",
});

const validateConfirmPassword = (rule, value, callback) => {
  if (value === "") {
    callback(new Error("请再次输入密码"));
  } else if (value !== registerForm.value.password) {
    callback(new Error("两次输入密码不一致"));
  } else {
    callback();
  }
};

const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 20, message: "用户名长度在 3-20 个字符", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于 6 位", trigger: "blur" },
    { 
      pattern: /^[a-zA-Z0-9_@#.]+$/, 
      message: "密码只能包含字母、数字和特殊字符", 
      trigger: "blur" 
    },
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: "blur" },
  ],
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;

  if (!agreementAccepted.value) {
    ElMessage.warning("请先同意用户服务协议和隐私政策");
    return;
  }

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        await authAPI.register({
          username: registerForm.value.username,
          password: registerForm.value.password,
        });
        ElMessage.success("注册成功，请登录");
        router.push("/login");
      } catch (error) {
        ElMessage.error("注册失败：" + error.message);
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style scoped>
.register-container {
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

.register-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 500px;
  padding: 20px;
}

.register-card {
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

.register-form {
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

.agreement-wrapper {
  margin: 20px 0;
  padding: 0 5px;
}

.agreement-text {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}

.agreement-link {
  font-size: 13px;
  color: #667eea;
  cursor: pointer;
  transition: all 0.3s ease;
}

.agreement-link:hover {
  color: #764ba2;
  text-decoration: underline;
}

.register-button {
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

.register-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.4);
}

.register-button:active:not(:disabled) {
  transform: translateY(0);
}

.register-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

.social-register {
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

.login-text {
  color: #666;
  font-size: 14px;
}

.login-link {
  color: #667eea;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  margin-left: 5px;
  transition: all 0.3s ease;
}

.login-link:hover {
  color: #764ba2;
  text-decoration: underline;
}

.benefits {
  display: flex;
  justify-content: space-around;
  margin-top: 25px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  border-radius: 12px;
  border: 1px solid rgba(102, 126, 234, 0.1);
}

.benefit-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.benefit-icon {
  font-size: 28px;
  color: #667eea;
}

.benefit-item span {
  font-size: 12px;
  color: #666;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .register-wrapper {
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
  
  .benefits {
    flex-direction: column;
    gap: 15px;
  }
  
  .benefit-item {
    flex-direction: row;
    justify-content: flex-start;
  }
}
</style>
