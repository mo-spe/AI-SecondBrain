<template>
  <div class="settings-container">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><User /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">个人设置</h1>
            <p class="welcome-subtitle">管理您的账户信息和偏好设置</p>
          </div>
        </div>
      </div>

      <div class="settings-grid">
        <div class="settings-left">
          <div class="settings-card">
            <div class="card-header">
              <h3>
                <el-icon><Edit /></el-icon>
                基本信息
              </h3>
            </div>

            <div class="card-body">
              <el-form
                :model="settingsForm"
                :rules="settingsRules"
                ref="settingsFormRef"
                label-width="100px"
                class="settings-form"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input
                    v-model="settingsForm.username"
                    placeholder="请输入用户名"
                    size="large"
                  >
                    <template #prefix>
                      <el-icon><User /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="邮箱" prop="email">
                  <el-input
                    v-model="settingsForm.email"
                    placeholder="请输入邮箱"
                    size="large"
                  >
                    <template #prefix>
                      <el-icon><Message /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="手机号" prop="phone">
                  <el-input
                    v-model="settingsForm.phone"
                    placeholder="请输入手机号"
                    size="large"
                  >
                    <template #prefix>
                      <el-icon><Phone /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="个人简介" prop="bio">
                  <el-input
                    v-model="settingsForm.bio"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入个人简介"
                    maxlength="500"
                    show-word-limit
                  />
                </el-form-item>

                <el-form-item label="API Key" prop="apiKey">
                  <el-input
                    v-model="settingsForm.apiKey"
                    type="password"
                    placeholder="请输入API Key"
                    size="large"
                    show-password
                  >
                    <template #prefix>
                      <el-icon><Key /></el-icon>
                    </template>
                  </el-input>
                  <div class="form-tip">
                    <p>用于AI功能调用的API Key</p>
                    <p>请妥善保管，不要泄露给他人</p>
                  </div>
                </el-form-item>

                <el-form-item>
                  <el-button
                    type="primary"
                    size="large"
                    @click="handleSave"
                    :loading="saveLoading"
                  >
                    <el-icon><Check /></el-icon>
                    保存设置
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </div>

        <div class="settings-right">
          <div class="info-card">
            <div class="card-header">
              <h3>
                <el-icon><InfoFilled /></el-icon>
                账户信息
              </h3>
              <el-button
                type="primary"
                size="small"
                @click="showPasswordDialog = true"
              >
                <el-icon><Lock /></el-icon>
                修改密码
              </el-button>
            </div>

            <div class="card-body">
              <div class="avatar-section">
                <div class="avatar-wrapper">
                  <el-avatar
                    :size="120"
                    :src="userStore.userInfo.avatar || defaultAvatar"
                    class="user-avatar"
                  >
                    <el-icon :size="60"><User /></el-icon>
                  </el-avatar>
                  <el-upload
                    class="avatar-uploader"
                    :show-file-list="false"
                    :before-upload="beforeAvatarUpload"
                    :http-request="handleAvatarUpload"
                    accept="image/*"
                  >
                    <div class="avatar-upload-btn">
                      <el-icon><Camera /></el-icon>
                    </div>
                  </el-upload>
                </div>
                <div class="avatar-tips">
                  <p>支持 JPG、PNG、GIF 格式</p>
                  <p>文件大小不超过 5MB</p>
                </div>
              </div>

              <div class="info-content">
                <div class="info-item">
                  <div class="info-icon">
                    <el-icon><User /></el-icon>
                  </div>
                  <div class="info-details">
                    <div class="info-label">用户ID</div>
                    <div class="info-value">
                      {{ userStore.userInfo.id || "-" }}
                    </div>
                  </div>
                </div>

                <div class="info-item">
                  <div class="info-icon">
                    <el-icon><User /></el-icon>
                  </div>
                  <div class="info-details">
                    <div class="info-label">用户名</div>
                    <div class="info-value">
                      {{ userStore.userInfo.username || "-" }}
                    </div>
                  </div>
                </div>

                <div class="info-item">
                  <div class="info-icon">
                    <el-icon><Message /></el-icon>
                  </div>
                  <div class="info-details">
                    <div class="info-label">邮箱</div>
                    <div class="info-value">
                      {{ userStore.userInfo.email || "-" }}
                    </div>
                  </div>
                </div>

                <div class="info-item">
                  <div class="info-icon">
                    <el-icon><Phone /></el-icon>
                  </div>
                  <div class="info-details">
                    <div class="info-label">手机号</div>
                    <div class="info-value">
                      {{ userStore.userInfo.phone || "-" }}
                    </div>
                  </div>
                </div>

                <div class="info-item">
                  <div class="info-icon">
                    <el-icon><Calendar /></el-icon>
                  </div>
                  <div class="info-details">
                    <div class="info-label">注册时间</div>
                    <div class="info-value">
                      {{ formatDate(userStore.userInfo.registerTime) }}
                    </div>
                  </div>
                </div>

                <div class="info-item">
                  <div class="info-icon">
                    <el-icon><Clock /></el-icon>
                  </div>
                  <div class="info-details">
                    <div class="info-label">最后登录</div>
                    <div class="info-value">
                      {{ formatDate(userStore.userInfo.lastLoginTime) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="danger-card">
            <div class="card-header">
              <h3>
                <el-icon><Warning /></el-icon>
                危险操作
              </h3>
            </div>

            <div class="card-body">
              <div class="danger-content">
                <p class="danger-text">
                  退出登录后，您需要重新输入凭据才能访问您的账户。
                </p>
                <el-button
                  type="danger"
                  size="large"
                  @click="handleLogout"
                  style="width: 100%"
                >
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="passwordForm"
        :rules="passwordRules"
        ref="passwordFormRef"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handlePasswordChange"
          :loading="passwordLoading"
        >
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";
import { userAPI } from "@/api/user";
import {
  User,
  Edit,
  Message,
  Phone,
  Check,
  InfoFilled,
  Calendar,
  Clock,
  Warning,
  SwitchButton,
  Lock,
  Camera,
  Key,
} from "@element-plus/icons-vue";

const router = useRouter();
const userStore = useUserStore();

const defaultAvatar =
  "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png";

const settingsFormRef = ref(null);
const passwordFormRef = ref(null);
const saveLoading = ref(false);
const passwordLoading = ref(false);
const avatarLoading = ref(false);
const settingsForm = ref({
  username: "",
  email: "",
  phone: "",
  bio: "",
  apiKey: "",
});

const passwordForm = ref({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const showPasswordDialog = ref(false);

const settingsRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    {
      min: 3,
      max: 20,
      message: "用户名长度在 3 到 20 个字符",
      trigger: "blur",
    },
    {
      pattern: /^[a-zA-Z0-9_]+$/,
      message: "用户名只能包含字母、数字和下划线",
      trigger: "blur",
    },
  ],
  email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: "blur" }],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号",
      trigger: "blur",
    },
  ],
  bio: [{ max: 500, message: "个人简介不能超过500个字符", trigger: "blur" }],
};

const passwordRules = {
  oldPassword: [{ required: true, message: "请输入原密码", trigger: "blur" }],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于6位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认新密码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error("两次输入的密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
};

const formatDate = (dateString) => {
  if (!dateString) return "-";
  const date = new Date(dateString);
  return date.toLocaleString("zh-CN");
};

const loadUserInfo = async () => {
  try {
    const data = await userAPI.getUserInfo();
    userStore.setUserInfo(data);

    settingsForm.value.username = data.username || "";
    settingsForm.value.email = data.email || "";
    settingsForm.value.phone = data.phone || "";
    settingsForm.value.bio = data.bio || "";
    settingsForm.value.apiKey = data.apiKey || "";
  } catch (error) {
    ElMessage.error("加载用户信息失败：" + error.message);
  }
};

const handleSave = async () => {
  if (!settingsFormRef.value) return;

  await settingsFormRef.value.validate(async (valid) => {
    if (valid) {
      saveLoading.value = true;
      try {
        await userAPI.updateUser({
          username: settingsForm.value.username,
          email: settingsForm.value.email,
          phone: settingsForm.value.phone,
          bio: settingsForm.value.bio,
          apiKey: settingsForm.value.apiKey,
        });

        await loadUserInfo();
        ElMessage.success("保存成功");
      } catch (error) {
        ElMessage.error("保存失败：" + error.message);
      } finally {
        saveLoading.value = false;
      }
    }
  });
};

const handlePasswordChange = async () => {
  if (!passwordFormRef.value) return;

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      passwordLoading.value = true;
      try {
        await userAPI.updatePassword({
          oldPassword: passwordForm.value.oldPassword,
          newPassword: passwordForm.value.newPassword,
        });

        ElMessage.success("密码修改成功");
        showPasswordDialog.value = false;
        passwordForm.value = {
          oldPassword: "",
          newPassword: "",
          confirmPassword: "",
        };
      } catch (error) {
        ElMessage.error("密码修改失败：" + error.message);
      } finally {
        passwordLoading.value = false;
      }
    }
  });
};

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith("image/");
  const isLt5M = file.size / 1024 / 1024 < 5;

  if (!isImage) {
    ElMessage.error("只能上传图片文件！");
    return false;
  }
  if (!isLt5M) {
    ElMessage.error("图片大小不能超过 5MB！");
    return false;
  }
  return true;
};

const handleAvatarUpload = async (options) => {
  const { file } = options;

  if (!beforeAvatarUpload(file)) {
    return;
  }

  avatarLoading.value = true;
  try {
    const avatarUrl = await userAPI.uploadAvatar(file);
    userStore.setUserInfo({
      ...userStore.userInfo,
      avatar: avatarUrl,
    });
    ElMessage.success("头像上传成功");
  } catch (error) {
    ElMessage.error("头像上传失败：" + error.message);
  } finally {
    avatarLoading.value = false;
  }
};

const handleLogout = () => {
  userStore.logout();
  router.push("/login");
  ElMessage.success("已退出登录");
};

onMounted(() => {
  loadUserInfo();
});
</script>

<style scoped>
.settings-container {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

.background-gradient {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 0;
}

.main-content {
  position: relative;
  z-index: 1;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-section {
  margin-bottom: 20px;
}

.welcome-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 30px;
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInDown 0.6s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.welcome-icon {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.welcome-content {
  flex: 1;
}

.welcome-title {
  font-size: 32px;
  font-weight: bold;
  color: white;
  margin: 0 0 8px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.welcome-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.settings-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.settings-left,
.settings-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.settings-card,
.info-card,
.danger-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: all 0.3s;
}

.settings-card:hover,
.info-card:hover,
.danger-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header h3 .el-icon {
  color: #667eea;
}

.card-body {
  padding: 24px;
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f9f9f9;
  border-radius: 12px;
  transition: all 0.3s;
}

.info-item:hover {
  background: #f0f0f0;
  transform: translateX(4px);
}

.info-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.info-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 13px;
  color: #909399;
}

.info-value {
  font-size: 16px;
  color: #303133;
  font-weight: bold;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #e0e0e0;
  margin-bottom: 20px;
}

.avatar-wrapper {
  position: relative;
  margin-bottom: 16px;
}

.user-avatar {
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.avatar-uploader {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.avatar-uploader:hover {
  transform: scale(1.1);
}

.avatar-upload-btn {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
}

.avatar-tips {
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.avatar-tips p {
  margin: 4px 0;
}

.danger-content {
  padding: 10px 0;
}

.danger-text {
  color: #f56c6c;
  font-size: 14px;
  margin: 0 0 16px 0;
  line-height: 1.6;
}

:deep(.el-input__wrapper) {
  box-shadow: none;
  border: 2px solid #e0e0e0;
  transition: all 0.3s;
}

:deep(.el-input__wrapper:hover) {
  border-color: #667eea;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

:deep(.el-textarea__inner) {
  border: 2px solid #e0e0e0;
  transition: all 0.3s;
}

:deep(.el-textarea__inner:hover) {
  border-color: #667eea;
}

:deep(.el-textarea__inner:focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

:deep(.el-button--danger) {
  transition: all 0.3s;
}

:deep(.el-button--danger:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(245, 108, 108, 0.4);
}

:deep(.el-form-item__content) {
  display: flex;
  align-items: center;
}

.form-tip {
  margin-top: 8px;
  padding: 12px;
  background: #f0f9ff;
  border-left: 3px solid #667eea;
  border-radius: 4px;
}

.form-tip p {
  margin: 4px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.form-tip p:first-child {
  font-weight: 500;
  color: #303133;
}

@media (max-width: 1024px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
