<template>
  <div class="settings-page">
    <div class="main-content">
      <div class="settings-layout">
        <div class="left-sidebar">
          <div class="user-card">
            <div class="user-greeting">
              <div class="greeting-icon">👋</div>
              <div class="greeting-text">
                <div class="greeting-title">こんにちは, {{ userStore.userInfo.username }}</div>
                <div class="greeting-subtitle">欢迎来到 AI-SecondBrain</div>
              </div>
            </div>

            <div class="user-avatar-section">
              <div class="avatar-wrapper">
                <el-avatar
                  :size="100"
                  :src="userStore.userInfo.avatar || defaultAvatar"
                  class="user-avatar"
                >
                  <el-icon :size="50"><User /></el-icon>
                </el-avatar>
              </div>
            </div>

            <div class="user-info-list">
              <div class="info-row">
                <el-icon size="14"><User /></el-icon>
                <span class="info-label">用户ID</span>
                <span class="info-value">{{ userStore.userInfo.id || '-' }}</span>
              </div>
              <div class="info-row">
                <el-icon size="14"><Message /></el-icon>
                <span class="info-label">邮箱</span>
                <span class="info-value">{{ userStore.userInfo.email }}</span>
              </div>
              <div class="info-row">
                <el-icon size="14"><Phone /></el-icon>
                <span class="info-label">手机号</span>
                <span class="info-value">{{ userStore.userInfo.phone }}</span>
              </div>
              <div class="info-row">
                <el-icon size="14"><Clock /></el-icon>
                <span class="info-label">注册时间</span>
                <span class="info-value">{{ formatDate(userStore.userInfo.registerTime) }}</span>
              </div>
              <div class="info-row">
                <el-icon size="14"><Calendar /></el-icon>
                <span class="info-label">最后登录</span>
                <span class="info-value">{{ formatDate(userStore.userInfo.lastLoginTime) }}</span>
              </div>
            </div>

            <el-button
              type="primary"
              size="small"
              @click="showPasswordDialog = true"
              style="width: 100%; margin-top: 16px"
            >
              <el-icon><Lock /></el-icon>
              <span>修改个人信息</span>
            </el-button>
          </div>
        </div>

        <div class="right-content">
          <div class="page-header">
            <div class="header-icon">
              <el-icon size="28" color="#7c3aed"><User /></el-icon>
            </div>
            <div class="header-text">
              <h1 class="page-title">个人设置</h1>
              <p class="page-subtitle">管理您的账户信息和偏好设置</p>
            </div>
          </div>

          <div class="settings-sections">
            <div class="section-card">
              <div class="section-header">
                <div class="section-icon purple">
                  <el-icon size="16" color="white"><Edit /></el-icon>
                </div>
                <h3 class="section-title">账户信息</h3>
              </div>
              <div class="section-body">
                <div class="form-grid">
                  <div class="form-item">
                    <label class="form-label">* 用户名</label>
                    <div class="form-input-wrapper">
                      <el-icon size="14" color="#94a3b8"><User /></el-icon>
                      <input
                        type="text"
                        v-model="settingsForm.username"
                        placeholder="请输入用户名"
                        class="form-input"
                      />
                    </div>
                  </div>
                  <div class="form-item">
                    <label class="form-label">手机号</label>
                    <div class="form-input-wrapper">
                      <el-icon size="14" color="#94a3b8"><Phone /></el-icon>
                      <input
                        type="text"
                        v-model="settingsForm.phone"
                        placeholder="请输入手机号"
                        class="form-input"
                      />
                    </div>
                  </div>
                  <div class="form-item">
                    <label class="form-label">邮箱</label>
                    <div class="form-input-wrapper">
                      <el-icon size="14" color="#94a3b8"><Message /></el-icon>
                      <input
                        type="text"
                        v-model="settingsForm.email"
                        placeholder="请输入邮箱"
                        class="form-input"
                      />
                    </div>
                  </div>
                  <div class="form-item">
                    <label class="form-label">API Key</label>
                    <div class="form-input-wrapper">
                      <el-icon size="14" color="#94a3b8"><Key /></el-icon>
                      <input
                        type="password"
                        v-model="settingsForm.apiKey"
                        placeholder="请输入API Key"
                        class="form-input"
                      />
                      <el-icon size="14" color="#94a3b8" class="eye-icon"><View /></el-icon>
                    </div>
                  </div>
                  <div class="form-item full-width">
                    <label class="form-label">个人简介</label>
                    <textarea
                      v-model="settingsForm.bio"
                      placeholder="请输入个人简介"
                      rows="3"
                      class="form-textarea"
                    ></textarea>
                  </div>
                </div>
              </div>
            </div>

            <div class="section-card">
              <div class="section-header">
                <div class="section-icon green">
                  <el-icon size="16" color="white"><Collection /></el-icon>
                </div>
                <h3 class="section-title">我的工作区</h3>
                <el-button type="primary" size="small" @click="openCreateWorkspace">+ 新建工作区</el-button>
              </div>
              <div class="section-body">
                <el-empty v-if="!workspaceStore.loading && workspaceStore.workspaces.length === 0" description="暂无工作区" :image-size="80" />
                <div v-else class="workspace-list">
                  <div v-for="ws in workspaceStore.workspaces" :key="ws.id" class="workspace-row">
                    <div class="workspace-info">
                      <div class="workspace-name">{{ ws.name }}</div>
                      <div class="workspace-meta">
                        <el-tag size="small" :type="ws.role === 'owner' ? 'warning' : ws.role === 'admin' ? 'success' : 'info'">
                          {{ ws.role === 'owner' ? 'Owner' : ws.role === 'admin' ? 'Admin' : ws.role === 'editor' ? 'Editor' : 'Viewer' }}
                        </el-tag>
                        <span v-if="ws.memberCount !== undefined" class="workspace-stat">{{ ws.memberCount }} 名成员</span>
                        <span class="workspace-stat">{{ ws.createTime }}</span>
                      </div>
                    </div>
                    <div class="workspace-actions">
                      <el-button size="small" @click="openEditWorkspace(ws)">编辑</el-button>
                      <el-button size="small" @click="goToMembers(ws.id)">成员</el-button>
                      <el-button v-if="ws.role === 'owner'" size="small" type="danger" @click="handleDeleteWorkspace(ws)">删除</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="logout-section">
              <div class="logout-card">
                <div class="logout-content">
                  <h3 class="logout-title">退出登录</h3>
                  <p class="logout-desc">安全登出，在您选择的安全环境中登出</p>
                </div>
                <el-button
                  type="danger"
                  size="large"
                  @click="handleLogout"
                  class="logout-btn"
                >
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showWorkspaceDialog"
      :title="workspaceForm.id ? '编辑工作区' : '新建工作区'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="workspaceForm"
        ref="workspaceFormRef"
        label-width="100px"
      >
        <el-form-item label="名称" required>
          <el-input v-model="workspaceForm.name" placeholder="请输入工作区名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="workspaceForm.description" type="textarea" :rows="3" placeholder="请输入工作区描述" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showWorkspaceDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveWorkspace" :loading="workspaceLoading">保存</el-button>
      </template>
    </el-dialog>

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
import { useWorkspaceStore } from "@/stores/workspace";
import { ElMessage, ElMessageBox } from "element-plus";
import { userAPI } from "@/api/user";
import { workspaceAPI } from "@/api/workspace";
import {
  User,
  Edit,
  Message,
  Phone,
  Clock,
  Calendar,
  Lock,
  Key,
  View,
  SwitchButton,
  Trophy,
  Folder,
  Collection,
  Cpu,
  Monitor,
  DataLine,
} from "@element-plus/icons-vue";

const router = useRouter();
const userStore = useUserStore();
const workspaceStore = useWorkspaceStore();

const defaultAvatar =
  "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png";

const settingsFormRef = ref(null);
const passwordFormRef = ref(null);
const saveLoading = ref(false);
const passwordLoading = ref(false);
const showPasswordDialog = ref(false);

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

const showWorkspaceDialog = ref(false);
const workspaceFormRef = ref(null);
const workspaceLoading = ref(false);
const workspaceForm = ref({
  id: null,
  name: "",
  description: "",
});

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

const handleLogout = () => {
  userStore.logout();
  router.push("/login");
  ElMessage.success("已退出登录");
};

const openCreateWorkspace = () => {
  workspaceForm.value = { id: null, name: "", description: "" };
  showWorkspaceDialog.value = true;
};

const openEditWorkspace = (ws) => {
  workspaceForm.value = { id: ws.id, name: ws.name, description: ws.description || "" };
  showWorkspaceDialog.value = true;
};

const handleSaveWorkspace = async () => {
  if (!workspaceForm.value.name.trim()) {
    ElMessage.warning("请输入工作区名称");
    return;
  }
  workspaceLoading.value = true;
  try {
    if (workspaceForm.value.id) {
      await workspaceAPI.update(workspaceForm.value.id, {
        name: workspaceForm.value.name,
        description: workspaceForm.value.description,
      });
      ElMessage.success("工作区已更新");
    } else {
      await workspaceAPI.create({
        name: workspaceForm.value.name,
        description: workspaceForm.value.description,
      });
      ElMessage.success("工作区已创建");
    }
    showWorkspaceDialog.value = false;
    await workspaceStore.fetchWorkspaces();
  } catch (error) {
    ElMessage.error("操作失败：" + (error.message || "未知错误"));
  } finally {
    workspaceLoading.value = false;
  }
};

const handleDeleteWorkspace = (ws) => {
  ElMessageBox.confirm(
    `确定要删除工作区「${ws.name}」吗？删除后所有关联数据将不可恢复。`,
    "删除工作区",
    {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning",
    }
  ).then(async () => {
    try {
      await workspaceAPI.delete(ws.id);
      ElMessage.success("工作区已删除");
      await workspaceStore.fetchWorkspaces();
    } catch (error) {
      ElMessage.error("删除失败：" + (error.message || "未知错误"));
    }
  }).catch(() => {});
};

const goToMembers = (id) => {
  router.push(`/workspace/${id}/members`);
};

onMounted(async () => {
  loadUserInfo();
  await workspaceStore.fetchWorkspaces();
});
</script>

<style scoped>
.settings-page {
  min-height: 100%;
  background: var(--bg-page);
}

.main-content {
  padding: var(--spacing-xl) 0;
}

.settings-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: var(--spacing-xl);
}

.left-sidebar {
  display: flex;
  flex-direction: column;
}

.user-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
}

.user-greeting {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.greeting-icon {
  font-size: 24px;
}

.greeting-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.greeting-subtitle {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.user-avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: var(--spacing-lg);
}

.avatar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
}

.user-avatar {
  border: 3px solid var(--color-primary-light);
  box-shadow: var(--shadow-md);
}

.user-level {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  background: rgba(124, 58, 237, 0.1);
  padding: 2px 12px;
  border-radius: var(--radius-full);
  font-weight: var(--font-weight-medium);
}

.user-progress {
  margin-bottom: var(--spacing-lg);
}

.progress-bar {
  height: 6px;
  background: var(--border-lighter);
  border-radius: var(--radius-full);
  overflow: hidden;
  margin-bottom: var(--spacing-xs);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #7c3aed, #a855f7);
  border-radius: var(--radius-full);
  transition: width var(--transition-base);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.user-info-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.info-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-xs);
  padding: var(--spacing-xs) 0;
}

.info-row .el-icon {
  color: var(--text-muted);
  flex-shrink: 0;
}

.info-label {
  color: var(--text-muted);
  min-width: 56px;
}

.info-value {
  color: var(--text-regular);
  flex: 1;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.right-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.page-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
}

.header-icon {
  width: 48px;
  height: 48px;
  background: rgba(124, 58, 237, 0.1);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin: 0 0 var(--spacing-xs) 0;
}

.page-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin: 0;
}

.settings-sections {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.section-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-lighter);
}

.section-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.section-icon.purple {
  background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 100%);
}

.section-icon.blue {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
}

.section-icon.green {
  background: linear-gradient(135deg, #22c55e 0%, #4ade80 100%);
}

.section-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
  flex: 1;
}

.view-all-btn {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-lg);
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.form-item.full-width {
  grid-column: span 2;
}

.form-label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.form-input-wrapper {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  background: var(--bg-input);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: var(--spacing-sm) var(--spacing-md);
}

.form-input-wrapper:focus-within {
  border-color: var(--color-primary);
}

.form-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  outline: none;
}

.form-input::placeholder {
  color: var(--text-placeholder);
}

.eye-icon {
  cursor: pointer;
}

.form-textarea {
  width: 100%;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  background: var(--bg-input);
  outline: none;
  resize: vertical;
  font-family: inherit;
}

.form-textarea:focus {
  border-color: var(--color-primary);
}

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-lg);
}

.achievement-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  background: var(--bg-input);
  border-radius: var(--radius-md);
}

.achievement-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.achievement-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.achievement-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.achievement-meta {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.achievement-progress {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.progress-track {
  flex: 1;
  height: 4px;
  background: var(--border-lighter);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-text {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  min-width: 32px;
  text-align: right;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-lg);
}

.knowledge-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  background: var(--bg-input);
  border-radius: var(--radius-md);
}

.knowledge-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.knowledge-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.knowledge-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.knowledge-meta {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.knowledge-progress {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.workspace-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.workspace-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md);
  background: var(--bg-input);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-lighter);
}

.workspace-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.workspace-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.workspace-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.workspace-stat {
  color: var(--text-muted);
}

.workspace-actions {
  display: flex;
  gap: var(--spacing-xs);
  flex-shrink: 0;
}

.logout-section {
  margin-top: var(--spacing-md);
}

.logout-card {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  border: 1px solid #fecaca;
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logout-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: #991b1b;
  margin: 0 0 var(--spacing-xs) 0;
}

.logout-desc {
  font-size: var(--font-size-sm);
  color: #b91c1c;
  margin: 0;
}

.logout-btn {
  background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);
  border: none;
}

.logout-btn:hover {
  background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
}

@media (max-width: 1200px) {
  .settings-layout {
    grid-template-columns: 1fr;
  }

  .left-sidebar {
    max-width: 400px;
    margin: 0 auto;
    width: 100%;
  }
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-item.full-width {
    grid-column: span 1;
  }

  .achievement-grid,
  .knowledge-grid {
    grid-template-columns: 1fr;
  }

  .logout-card {
    flex-direction: column;
    gap: var(--spacing-md);
    text-align: center;
  }
}
</style>