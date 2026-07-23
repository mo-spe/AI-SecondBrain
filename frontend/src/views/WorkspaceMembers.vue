<template>
  <div class="members-page">
    <div class="main-content">
      <div class="page-header">
        <div class="header-left">
          <el-button @click="goBack" text>
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <div class="header-info">
            <h1 class="page-title">{{ workspaceName }} - 成员管理</h1>
            <p class="page-subtitle">管理工作区成员、角色与权限</p>
          </div>
        </div>
        <el-button v-if="isManager" type="primary" @click="openInviteDialog">
          <el-icon><Plus /></el-icon>
          邀请成员
        </el-button>
      </div>

      <div class="section-card">
        <el-table :data="members" border stripe :loading="loading" empty-text="暂无成员">
          <el-table-column label="用户" min-width="200">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="36">{{ row.username?.charAt(0)?.toUpperCase() }}</el-avatar>
                <span class="user-name">{{ row.username }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              <el-tag :type="roleTagType(row.role)" size="small">
                {{ roleLabel(row.role) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="加入时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.joinedTime) }}
            </template>
          </el-table-column>
          <el-table-column v-if="isManager" label="操作" width="200">
            <template #default="{ row }">
              <el-button
                v-if="row.role !== 'owner'"
                size="small"
                @click="openChangeRoleDialog(row)"
              >
                修改角色
              </el-button>
              <el-button
                v-if="row.role !== 'owner' && row.userId !== currentUserId"
                size="small"
                type="danger"
                @click="handleRemoveMember(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog
      v-model="showInviteDialog"
      title="邀请成员"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form :model="inviteForm" ref="inviteFormRef" label-width="80px">
        <el-form-item label="用户ID" required>
          <el-input v-model.number="inviteForm.userId" type="number" placeholder="请输入要邀请的用户ID" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="inviteForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="Admin（管理员）" value="admin" />
            <el-option label="Editor（编辑者）" value="editor" />
            <el-option label="Viewer（观察者）" value="viewer" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" @click="handleInvite" :loading="inviteLoading">邀请</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showRoleDialog"
      title="修改角色"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ selectedMember?.username }}</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRole" placeholder="请选择角色" style="width: 100%">
            <el-option label="Admin（管理员）" value="admin" />
            <el-option label="Editor（编辑者）" value="editor" />
            <el-option label="Viewer（观察者）" value="viewer" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoleDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangeRole" :loading="roleLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowLeft, Plus } from "@element-plus/icons-vue";
import { workspaceAPI } from "@/api/workspace";

const route = useRoute();
const router = useRouter();

const workspaceId = Number(route.params.id);
const workspaceName = ref("");
const members = ref([]);
const loading = ref(false);
const currentUserId = ref(null);
const isManager = ref(false);

const showInviteDialog = ref(false);
const inviteFormRef = ref(null);
const inviteLoading = ref(false);
const inviteForm = ref({ userId: null, role: "editor" });

const showRoleDialog = ref(false);
const selectedMember = ref(null);
const selectedRole = ref("");
const roleLoading = ref(false);

const roleTagType = (role) => {
  const map = { owner: "warning", admin: "success", editor: "", viewer: "info" };
  return map[role] || "info";
};

const roleLabel = (role) => {
  const map = { owner: "Owner", admin: "Admin", editor: "Editor", viewer: "Viewer" };
  return map[role] || role;
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

const goBack = () => {
  router.push("/settings");
};

const loadData = async () => {
  loading.value = true;
  try {
    const [ws, memberList] = await Promise.all([
      workspaceAPI.getById(workspaceId),
      workspaceAPI.getMembers(workspaceId),
    ]);
    workspaceName.value = ws.name || "工作区";
    members.value = memberList || [];

    const self = members.value.find((m) => m.userId === currentUserId.value);
    if (self) {
      isManager.value = self.role === "owner" || self.role === "admin";
    }
  } catch (error) {
    ElMessage.error("加载数据失败：" + (error.message || "未知错误"));
  } finally {
    loading.value = false;
  }
};

const openInviteDialog = () => {
  inviteForm.value = { userId: null, role: "editor" };
  showInviteDialog.value = true;
};

const handleInvite = async () => {
  if (!inviteForm.value.userId) {
    ElMessage.warning("请输入用户ID");
    return;
  }
  inviteLoading.value = true;
  try {
    await workspaceAPI.addMember(workspaceId, {
      userId: inviteForm.value.userId,
      role: inviteForm.value.role,
    });
    ElMessage.success("成员已邀请");
    showInviteDialog.value = false;
    await loadData();
  } catch (error) {
    ElMessage.error("邀请失败：" + (error.message || "未知错误"));
  } finally {
    inviteLoading.value = false;
  }
};

const openChangeRoleDialog = (member) => {
  selectedMember.value = member;
  selectedRole.value = member.role;
  showRoleDialog.value = true;
};

const handleChangeRole = async () => {
  roleLoading.value = true;
  try {
    await workspaceAPI.updateMemberRole(workspaceId, selectedMember.value.userId, {
      role: selectedRole.value,
    });
    ElMessage.success("角色已更新");
    showRoleDialog.value = false;
    await loadData();
  } catch (error) {
    ElMessage.error("操作失败：" + (error.message || "未知错误"));
  } finally {
    roleLoading.value = false;
  }
};

const handleRemoveMember = (member) => {
  ElMessageBox.confirm(
    `确定要移除成员「${member.username}」吗？`,
    "移除成员",
    { confirmButtonText: "确认移除", cancelButtonText: "取消", type: "warning" }
  ).then(async () => {
    try {
      await workspaceAPI.removeMember(workspaceId, member.userId);
      ElMessage.success("成员已移除");
      await loadData();
    } catch (error) {
      ElMessage.error("移除失败：" + (error.message || "未知错误"));
    }
  }).catch(() => {});
};

onMounted(async () => {
  const token = localStorage.getItem("token");
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split(".")[1]));
      currentUserId.value = payload.userId;
    } catch {}
  }
  await loadData();
});
</script>

<style scoped>
.members-page {
  min-height: 100%;
  background: var(--bg-page);
  padding: var(--spacing-xl) 0;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.page-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin: 0;
}

.page-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin: 0;
}

.section-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.user-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}
</style>
