<template>
  <div class="login-container">
    <div class="login-left">
      <div class="left-content">
        <div class="brand">
          <div class="brand-icon">
            <BrainIcon :size="56" />
          </div>
          <span class="brand-text">AI-SecondBrain</span>
        </div>

        <div class="characters-container">
          <div 
            ref="purpleRef"
            class="character purple-character"
            :style="purpleStyle"
          >
            <div class="character-eyes" :style="purpleEyesStyle">
              <div 
                class="eye-ball"
                :class="{ blinking: isPurpleBlinking }"
              >
                <div 
                  class="pupil"
                  :style="getPupilStyle('purple')"
                ></div>
              </div>
              <div 
                class="eye-ball"
                :class="{ blinking: isPurpleBlinking }"
              >
                <div 
                  class="pupil"
                  :style="getPupilStyle('purple')"
                ></div>
              </div>
            </div>
          </div>

          <div 
            ref="blackRef"
            class="character black-character"
            :style="blackStyle"
          >
            <div class="character-eyes" :style="blackEyesStyle">
              <div 
                class="eye-ball"
                :class="{ blinking: isBlackBlinking }"
              >
                <div 
                  class="pupil"
                  :style="getPupilStyle('black')"
                ></div>
              </div>
              <div 
                class="eye-ball"
                :class="{ blinking: isBlackBlinking }"
              >
                <div 
                  class="pupil"
                  :style="getPupilStyle('black')"
                ></div>
              </div>
            </div>
          </div>

          <div 
            ref="orangeRef"
            class="character orange-character"
            :style="orangeStyle"
          >
            <div class="character-pupils" :style="orangePupilsStyle">
              <div class="pupil" :style="getPupilStyle('orange')"></div>
              <div class="pupil" :style="getPupilStyle('orange')"></div>
            </div>
          </div>

          <div 
            ref="yellowRef"
            class="character yellow-character"
            :style="yellowStyle"
          >
            <div class="character-pupils" :style="yellowPupilsStyle">
              <div class="pupil" :style="getPupilStyle('yellow')"></div>
              <div class="pupil" :style="getPupilStyle('yellow')"></div>
            </div>
            <div class="character-mouth" :style="yellowMouthStyle"></div>
          </div>
        </div>

        <div class="footer-links">
          <a href="#" class="footer-link">Privacy Policy</a>
          <a href="#" class="footer-link">Terms of Service</a>
          <a href="#" class="footer-link">Contact</a>
        </div>

        <div class="decorative-grid"></div>
        <div class="decorative-blur blur-1"></div>
        <div class="decorative-blur blur-2"></div>
      </div>
    </div>

    <div class="login-right">
      <div class="right-content">
        <div class="mobile-brand">
          <div class="brand-icon">
            <BrainIcon :size="20" color="#7c3aed" />
          </div>
          <span class="brand-text">AI-SecondBrain</span>
        </div>

        <div class="form-header">
          <h1 class="form-title">Welcome back!</h1>
          <p class="form-subtitle">Please enter your details</p>
        </div>

        <el-form
          :model="loginForm"
          :rules="rules"
          ref="loginFormRef"
          label-width="0"
          class="login-form"
        >
          <el-form-item prop="username">
            <label class="form-label">Username</label>
            <div class="input-wrapper">
              <el-input
                v-model="loginForm.username"
                placeholder="Enter your username"
                clearable
                class="custom-input"
                @focus="isTyping = true"
                @blur="isTyping = false"
              />
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <label class="form-label">Password</label>
            <div class="input-wrapper password-input">
              <el-input
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="••••••••"
                clearable
                class="custom-input"
                @keyup.enter="handleLogin"
              />
              <button 
                type="button" 
                class="password-toggle"
                @click="showPassword = !showPassword"
              >
                <span v-if="showPassword" class="eye-icon" style="color: #7c3aed;">👁</span>
                <span v-else class="eye-icon" style="color: #94a3b8;">👁‍🗨</span>
              </button>
            </div>
          </el-form-item>

          <div class="form-options">
            <div class="remember-me">
              <el-checkbox v-model="rememberMe" id="remember">
                <label for="remember" class="checkbox-label">Remember for 30 days</label>
              </el-checkbox>
            </div>
            <a href="#" class="forgot-link">Forgot password?</a>
          </div>

          <el-form-item v-if="error">
            <div class="error-message">{{ error }}</div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              @click="handleLogin"
              :loading="loading"
              class="login-button"
            >
              {{ loading ? 'Signing in...' : 'Log in' }}
            </el-button>
          </el-form-item>

          <div class="social-login">
            <el-button class="social-btn" type="primary" plain>
              <span class="social-icon">🌐</span>
              <span>Log in with Google</span>
            </el-button>
          </div>

          <div class="sign-up-link">
            Don't have an account? 
            <router-link to="/register" class="link">Sign Up</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { 
  Star 
} from "@element-plus/icons-vue";
import BrainIcon from "@/components/BrainIcon.vue";
import { useUserStore } from "@/stores/user";
import { authAPI } from "@/api/auth";

const router = useRouter();
const userStore = useUserStore();

const loginFormRef = ref(null);
const loading = ref(false);
const rememberMe = ref(false);
const showPassword = ref(false);
const error = ref("");
const loginForm = ref({
  username: "",
  password: "",
});

const rules = {
  username: [
    { required: true, message: "Please enter username", trigger: "blur" },
    { min: 3, max: 20, message: "Username must be 3-20 characters", trigger: "blur" }
  ],
  password: [
    { required: true, message: "Please enter password", trigger: "blur" },
    { min: 6, message: "Password must be at least 6 characters", trigger: "blur" },
  ],
};

const mouseX = ref(0);
const mouseY = ref(0);
const isPurpleBlinking = ref(false);
const isBlackBlinking = ref(false);
const isTyping = ref(false);
const isLookingAtEachOther = ref(false);
const isPurplePeeking = ref(false);

const purpleRef = ref(null);
const blackRef = ref(null);
const orangeRef = ref(null);
const yellowRef = ref(null);

let purpleBlinkTimeout = null;
let blackBlinkTimeout = null;
let peekTimeout = null;
let lookTimer = null;

const getRandomBlinkInterval = () => Math.random() * 4000 + 3000;

const schedulePurpleBlink = () => {
  purpleBlinkTimeout = setTimeout(() => {
    isPurpleBlinking.value = true;
    setTimeout(() => {
      isPurpleBlinking.value = false;
      schedulePurpleBlink();
    }, 150);
  }, getRandomBlinkInterval());
};

const scheduleBlackBlink = () => {
  blackBlinkTimeout = setTimeout(() => {
    isBlackBlinking.value = true;
    setTimeout(() => {
      isBlackBlinking.value = false;
      scheduleBlackBlink();
    }, 150);
  }, getRandomBlinkInterval());
};

const schedulePeek = () => {
  peekTimeout = setTimeout(() => {
    isPurplePeeking.value = true;
    setTimeout(() => {
      isPurplePeeking.value = false;
    }, 800);
  }, Math.random() * 3000 + 2000);
};

const calculatePosition = (elRef) => {
  if (!elRef.value) return { faceX: 0, faceY: 0, bodySkew: 0 };
  const rect = elRef.value.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 3;
  const deltaX = mouseX.value - centerX;
  const deltaY = mouseY.value - centerY;
  const faceX = Math.max(-15, Math.min(15, deltaX / 20));
  const faceY = Math.max(-10, Math.min(10, deltaY / 30));
  const bodySkew = Math.max(-6, Math.min(6, -deltaX / 120));
  return { faceX, faceY, bodySkew };
};

const purplePos = computed(() => calculatePosition(purpleRef));
const blackPos = computed(() => calculatePosition(blackRef));
const orangePos = computed(() => calculatePosition(orangeRef));
const yellowPos = computed(() => calculatePosition(yellowRef));

const purpleStyle = computed(() => {
  const baseHeight = (isTyping.value || (loginForm.value.password.length > 0 && !showPassword.value)) ? 440 : 400;
  let transform = "";
  
  if (loginForm.value.password.length > 0 && showPassword.value) {
    transform = "skewX(0deg)";
  } else if (isTyping.value || (loginForm.value.password.length > 0 && !showPassword.value)) {
    transform = `skewX(${(purplePos.value.bodySkew || 0) - 12}deg) translateX(40px)`;
  } else {
    transform = `skewX(${purplePos.value.bodySkew || 0}deg)`;
  }
  
  return {
    height: `${baseHeight}px`,
    transform,
    transformOrigin: "bottom center",
  };
});

const purpleEyesStyle = computed(() => {
  let left, top;
  if (loginForm.value.password.length > 0 && showPassword.value) {
    left = "20px";
    top = "35px";
  } else if (isLookingAtEachOther.value) {
    left = "55px";
    top = "65px";
  } else {
    left = `${45 + purplePos.value.faceX}px`;
    top = `${40 + purplePos.value.faceY}px`;
  }
  return { left, top };
});

const blackStyle = computed(() => {
  let transform = "";
  
  if (loginForm.value.password.length > 0 && showPassword.value) {
    transform = "skewX(0deg)";
  } else if (isLookingAtEachOther.value) {
    transform = `skewX(${(blackPos.value.bodySkew || 0) * 1.5 + 10}deg) translateX(20px)`;
  } else if (isTyping.value || (loginForm.value.password.length > 0 && !showPassword.value)) {
    transform = `skewX(${(blackPos.value.bodySkew || 0) * 1.5}deg)`;
  } else {
    transform = `skewX(${blackPos.value.bodySkew || 0}deg)`;
  }
  
  return {
    transform,
    transformOrigin: "bottom center",
  };
});

const blackEyesStyle = computed(() => {
  let left, top;
  if (loginForm.value.password.length > 0 && showPassword.value) {
    left = "10px";
    top = "28px";
  } else if (isLookingAtEachOther.value) {
    left = "32px";
    top = "12px";
  } else {
    left = `${26 + blackPos.value.faceX}px`;
    top = `${32 + blackPos.value.faceY}px`;
  }
  return { left, top };
});

const orangeStyle = computed(() => ({
  transform: (loginForm.value.password.length > 0 && showPassword.value) 
    ? "skewX(0deg)" 
    : `skewX(${orangePos.value.bodySkew || 0}deg)`,
  transformOrigin: "bottom center",
}));

const orangePupilsStyle = computed(() => {
  let left, top;
  if (loginForm.value.password.length > 0 && showPassword.value) {
    left = "50px";
    top = "85px";
  } else {
    left = `${82 + (orangePos.value.faceX || 0)}px`;
    top = `${90 + (orangePos.value.faceY || 0)}px`;
  }
  return { left, top };
});

const yellowStyle = computed(() => ({
  transform: (loginForm.value.password.length > 0 && showPassword.value) 
    ? "skewX(0deg)" 
    : `skewX(${yellowPos.value.bodySkew || 0}deg)`,
  transformOrigin: "bottom center",
}));

const yellowPupilsStyle = computed(() => {
  let left, top;
  if (loginForm.value.password.length > 0 && showPassword.value) {
    left = "20px";
    top = "35px";
  } else {
    left = `${52 + (yellowPos.value.faceX || 0)}px`;
    top = `${40 + (yellowPos.value.faceY || 0)}px`;
  }
  return { left, top };
});

const yellowMouthStyle = computed(() => ({
  left: loginForm.value.password.length > 0 && showPassword.value 
    ? "10px" 
    : `${40 + (yellowPos.value.faceX || 0)}px`,
  top: `${88 + (yellowPos.value.faceY || 0)}px`,
}));

const getPupilStyle = (character) => {
  let forceX, forceY;
  
  if (loginForm.value.password.length > 0 && showPassword.value) {
    if (character === "purple") {
      forceX = isPurplePeeking.value ? 4 : -4;
      forceY = isPurplePeeking.value ? 5 : -4;
    } else {
      forceX = -5;
      forceY = -4;
    }
  } else if (isLookingAtEachOther.value) {
    if (character === "purple") {
      forceX = 3;
      forceY = 4;
    } else if (character === "black") {
      forceX = 0;
      forceY = -4;
    }
  }
  
  if (forceX !== undefined && forceY !== undefined) {
    return {
      transform: `translate(${forceX}px, ${forceY}px)`,
    };
  }
  
  return {};
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

onMounted(() => {
  const handleMouseMove = (e) => {
    mouseX.value = e.clientX;
    mouseY.value = e.clientY;
  };
  
  window.addEventListener("mousemove", handleMouseMove);
  
  schedulePurpleBlink();
  scheduleBlackBlink();

  if (localStorage.getItem("rememberedUsername")) {
    loginForm.value.username = localStorage.getItem("rememberedUsername");
    rememberMe.value = true;
  }

  onUnmounted(() => {
    window.removeEventListener("mousemove", handleMouseMove);
    if (purpleBlinkTimeout) clearTimeout(purpleBlinkTimeout);
    if (blackBlinkTimeout) clearTimeout(blackBlinkTimeout);
    if (peekTimeout) clearTimeout(peekTimeout);
    if (lookTimer) clearTimeout(lookTimer);
  });
});

import { watch } from "vue";

watch(isTyping, (val) => {
  if (val) {
    isLookingAtEachOther.value = true;
    lookTimer = setTimeout(() => {
      isLookingAtEachOther.value = false;
    }, 800);
  } else {
    isLookingAtEachOther.value = false;
    if (lookTimer) clearTimeout(lookTimer);
  }
});

watch([() => loginForm.value.password, showPassword, isPurplePeeking], ([password, show, peeking]) => {
  if (password.length > 0 && show && !peeking) {
    schedulePeek();
  } else {
    isPurplePeeking.value = false;
    if (peekTimeout) clearTimeout(peekTimeout);
  }
});
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.login-left {
  background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 50%, #7c3aed 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.left-content {
  position: relative;
  z-index: 2;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: white;
  font-size: 18px;
  font-weight: 600;
}

.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.characters-container {
  position: relative;
  width: 550px;
  height: 400px;
  margin: 0 auto;
}

.character {
  position: absolute;
  bottom: 0;
  transition: all 0.7s ease-in-out;
}

.purple-character {
  left: 70px;
  width: 180px;
  background-color: #6C3FF5;
  border-radius: 10px 10px 0 0;
  z-index: 1;
}

.black-character {
  left: 240px;
  width: 120px;
  height: 310px;
  background-color: #2D2D2D;
  border-radius: 8px 8px 0 0;
  z-index: 2;
}

.orange-character {
  left: 0px;
  width: 240px;
  height: 200px;
  background-color: #FF9B6B;
  border-radius: 120px 120px 0 0;
  z-index: 3;
}

.yellow-character {
  left: 310px;
  width: 140px;
  height: 230px;
  background-color: #E8D754;
  border-radius: 70px 70px 0 0;
  z-index: 4;
}

.character-eyes {
  position: absolute;
  display: flex;
  gap: 8px;
  transition: all 0.7s ease-in-out;
}

.eye-ball {
  width: 18px;
  height: 18px;
  background-color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: height 0.15s ease;
}

.black-character .eye-ball {
  width: 16px;
  height: 16px;
}

.eye-ball.blinking {
  height: 2px;
}

.eye-ball.blinking .pupil {
  display: none;
}

.pupil {
  width: 7px;
  height: 7px;
  background-color: #2D2D2D;
  border-radius: 50%;
  transition: transform 0.1s ease-out;
}

.black-character .pupil {
  width: 6px;
  height: 6px;
}

.character-pupils {
  position: absolute;
  display: flex;
  gap: 8px;
  transition: all 0.2s ease-out;
}

.orange-character .pupil,
.yellow-character .pupil {
  width: 12px;
  height: 12px;
  max-width: 12px;
  max-height: 12px;
}

.character-mouth {
  position: absolute;
  width: 80px;
  height: 4px;
  background-color: #2D2D2D;
  border-radius: 2px;
  transition: all 0.2s ease-out;
}

.footer-links {
  display: flex;
  gap: 32px;
}

.footer-link {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  text-decoration: none;
  transition: color 0.3s ease;
}

.footer-link:hover {
  color: white;
}

.decorative-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 20px 20px;
}

.decorative-blur {
  position: absolute;
  border-radius: 50%;
  filter: blur(64px);
}

.blur-1 {
  top: 25%;
  right: 25%;
  width: 256px;
  height: 256px;
  background: rgba(255, 255, 255, 0.1);
}

.blur-2 {
  bottom: 25%;
  left: 25%;
  width: 384px;
  height: 384px;
  background: rgba(255, 255, 255, 0.05);
}

.login-right {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: var(--bg-page);
}

.right-content {
  width: 100%;
  max-width: 420px;
}

.mobile-brand {
  display: none;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 48px;
}

.form-header {
  text-align: center;
  margin-bottom: 40px;
}

.form-title {
  font-family: var(--font-family-display);
  font-size: var(--font-size-4xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin: 0 0 8px 0;
  letter-spacing: -0.02em;
}

.form-subtitle {
  font-family: var(--font-family-body);
  font-size: var(--font-size-md);
  color: var(--text-secondary);
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-regular);
  margin-bottom: 8px;
}

.input-wrapper {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}

.input-wrapper:focus-within {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus-ring);
}

.custom-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background: transparent;
  padding: 12px 16px;
}

.custom-input :deep(.el-input__inner) {
  font-size: 15px;
  color: #1e293b;
}

.password-input :deep(.el-input__wrapper) {
  padding-right: 48px;
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
}

.eye-icon {
  font-size: 18px;
}

.social-icon {
  font-size: 18px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
}

.checkbox-label {
  font-size: 14px;
  color: #374151;
  cursor: pointer;
}

.forgot-link {
  font-size: 14px;
  color: #7c3aed;
  font-weight: 500;
  text-decoration: none;
  transition: color 0.3s ease;
}

.forgot-link:hover {
  color: #5b21b6;
  text-decoration: underline;
}

.error-message {
  padding: 12px 16px;
  font-size: 14px;
  color: #f87171;
  background: rgba(248, 113, 113, 0.1);
  border: 1px solid rgba(248, 113, 113, 0.3);
  border-radius: 8px;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-primary-hover);
}

.login-button:active {
  transform: translateY(0);
}

.social-login {
  margin-top: 8px;
}

.social-btn {
  width: 100%;
  height: 48px;
  font-size: 14px;
  font-weight: 500;
  background: white;
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #374151;
  transition: all 0.3s ease;
}

.social-btn:hover {
  background: #f5f3ff;
  border-color: #7c3aed;
}

.sign-up-link {
  text-align: center;
  font-size: 14px;
  color: #64748b;
  margin-top: 32px;
}

.sign-up-link .link {
  color: #1e293b;
  font-weight: 500;
  text-decoration: none;
  transition: color 0.3s ease;
}

.sign-up-link .link:hover {
  color: #7c3aed;
  text-decoration: underline;
}

@media (max-width: 1024px) {
  .login-container {
    grid-template-columns: 1fr;
  }

  .login-left {
    display: none;
  }

  .mobile-brand {
    display: flex;
  }

  .login-right {
    padding: 24px;
  }
}
</style>