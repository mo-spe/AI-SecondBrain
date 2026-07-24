<template>
  <div class="admin-page">
    <div class="main-content">
      <div class="page-header">
        <div class="header-info">
          <h1 class="page-title">平台管理</h1>
          <p class="page-subtitle">系统统计、用户与工作区管控</p>
        </div>
      </div>

      <div class="section-card">
        <el-tabs v-model="activeTab" @tab-change="onTabChange">
          <el-tab-pane label="平台统计" name="stats">
            <div class="stats-grid">
              <div class="stat-card">
                <div class="stat-icon" style="background: var(--color-primary-light)">
                  <el-icon size="24" color="var(--color-primary)"><User /></el-icon>
                </div>
                <div class="stat-body">
                  <div class="stat-value">{{ statistics.userCount ?? '-' }}</div>
                  <div class="stat-label">总用户数</div>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon" style="background: rgba(34, 197, 94, 0.1)">
                  <el-icon size="24" color="#22c55e"><OfficeBuilding /></el-icon>
                </div>
                <div class="stat-body">
                  <div class="stat-value">{{ statistics.workspaceCount ?? '-' }}</div>
                  <div class="stat-label">总工作区数</div>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon" style="background: rgba(245, 158, 11, 0.1)">
                  <el-icon size="24" color="#f59e0b"><Document /></el-icon>
                </div>
                <div class="stat-body">
                  <div class="stat-value">{{ statistics.knowledgeCount ?? '-' }}</div>
                  <div class="stat-label">总知识节点数</div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="用户管理" name="users">
            <el-table :data="users" border stripe :loading="userLoading">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="username" label="用户名" min-width="140" />
              <el-table-column prop="email" label="邮箱" min-width="180" />
              <el-table-column label="平台角色" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.role === 'super_admin' ? 'danger' : 'info'" size="small">
                    {{ row.role === 'super_admin' ? 'Super Admin' : 'User' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '正常' : '已禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="注册时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button
                    v-if="row.role !== 'super_admin'"
                    size="small"
                    :type="row.status === 1 ? 'danger' : 'success'"
                    @click="handleToggleUser(row)"
                  >
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrap">
              <el-pagination
                v-model:current-page="userPagination.current"
                v-model:page-size="userPagination.size"
                :total="userPagination.total"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @size-change="loadUsers"
                @current-change="loadUsers"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="工作区管理" name="workspaces">
            <el-table :data="adminWorkspaces" border stripe :loading="workspaceLoading">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="name" label="名称" min-width="180" />
              <el-table-column prop="ownerId" label="创建者ID" width="100" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '正常' : '已禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="创建时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    :type="row.status === 1 ? 'danger' : 'success'"
                    @click="handleToggleWorkspace(row)"
                  >
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrap">
              <el-pagination
                v-model:current-page="wsPagination.current"
                v-model:page-size="wsPagination.size"
                :total="wsPagination.total"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @size-change="loadAdminWorkspaces"
                @current-change="loadAdminWorkspaces"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="举报管理" name="reports">
            <el-table :data="reports" border stripe :loading="reportLoading">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="postTitle" label="被举报内容" min-width="180" show-overflow-tooltip />
              <el-table-column prop="reporterName" label="举报人" width="120" />
              <el-table-column prop="reason" label="举报原因" min-width="200" show-overflow-tooltip />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="reportStatusType(row.status)" size="small">
                    {{ reportStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="举报时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <template v-if="row.status === 'pending'">
                    <el-button size="small" @click="handleIgnoreReport(row)">
                      忽略
                    </el-button>
                    <el-button size="small" type="danger" @click="handleRemovePost(row)">
                      移除
                    </el-button>
                  </template>
                  <span v-else class="handled-info">
                    {{ row.handleNote || '-' }}
                  </span>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrap">
              <el-pagination
                v-model:current-page="reportPagination.current"
                v-model:page-size="reportPagination.size"
                :total="reportPagination.total"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @size-change="loadReports"
                @current-change="loadReports"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="敏感词管理" name="sensitiveWords">
            <div class="sensitive-word-header">
              <el-input
                v-model="newSensitiveWord"
                placeholder="输入敏感词..."
                style="width: 300px"
                @keyup.enter="handleAddSensitiveWord"
              />
              <el-button type="primary" @click="handleAddSensitiveWord">
                添加
              </el-button>
            </div>
            <el-table :data="sensitiveWords" border stripe style="margin-top: 16px">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="word" label="敏感词" min-width="200" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button size="small" type="danger" @click="handleDeleteSensitiveWord(row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="AI服务商管理" name="aiProviders">
            <div class="ai-provider-header">
              <el-button type="primary" @click="showProviderDialog = true; editingProvider = null; providerForm = {}">
                新增服务商
              </el-button>
            </div>
            <el-table :data="aiProviders" border stripe style="margin-top: 16px">
              <el-table-column prop="id" label="ID" width="60" />
              <el-table-column prop="name" label="名称" width="140" />
              <el-table-column prop="code" label="标识" width="120" />
              <el-table-column prop="apiType" label="API类型" width="160" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.isEnabled === 1 ? 'success' : 'info'" size="small">
                    {{ row.isEnabled === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sortOrder" label="排序" width="60" />
              <el-table-column label="操作" width="280">
                <template #default="{ row }">
                  <el-button size="small" @click="loadAiModels(row)">模型</el-button>
                  <el-button size="small" type="primary" @click="editProvider(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="handleDeleteProvider(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 模型管理（展开） -->
            <div v-if="selectedProvider" class="ai-model-section">
              <h3>{{ selectedProvider.name }} — 模型列表</h3>
              <el-button type="primary" size="small" @click="showModelDialog = true; editingModel = null; modelForm = {}">
                新增模型
              </el-button>
              <el-table :data="aiModels" border stripe style="margin-top: 12px">
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column prop="displayName" label="显示名称" width="160" />
                <el-table-column prop="modelName" label="模型标识" width="180" />
                <el-table-column label="状态" width="80">
                  <template #default="{ row2 }">
                    <el-tag :type="row2.isEnabled === 1 ? 'success' : 'info'" size="small">
                      {{ row2.isEnabled === 1 ? '启用' : '禁用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="适用场景" min-width="240">
                  <template #default="{ row: row2 }">
                    <el-tag
                      v-for="sc in parseScenarios(row2.supportedScenarios)"
                      :key="sc"
                      size="small"
                      style="margin-right: 4px"
                    >
                      {{ scenarioLabel(sc) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140">
                  <template #default="{ row: row2 }">
                    <el-button size="small" type="primary" @click="editModel(row2)">编辑</el-button>
                    <el-button size="small" type="danger" @click="handleDeleteModel(row2)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 服务商编辑弹窗 -->
    <el-dialog
      v-model="showProviderDialog"
      :title="editingProvider ? '编辑服务商' : '新增服务商'"
      width="520px"
      destroy-on-close
    >
      <el-form :model="providerForm" label-width="100px">
        <el-form-item label="标识" required>
          <el-input v-model="providerForm.code" placeholder="如 openai, qwen" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="providerForm.name" placeholder="如 OpenAI, 千问" />
        </el-form-item>
        <el-form-item label="API 地址">
          <el-input v-model="providerForm.baseUrl" placeholder="如 https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API 类型">
          <el-select v-model="providerForm.apiType" style="width: 100%">
            <el-option label="OpenAI 兼容" value="openai_compatible" />
            <el-option label="Anthropic" value="anthropic" />
            <el-option label="Gemini" value="gemini" />
          </el-select>
        </el-form-item>
        <el-form-item label="Logo URL">
          <el-input v-model="providerForm.logoUrl" placeholder="可选" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="providerForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="providerForm.isEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProviderDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveProvider">保存</el-button>
      </template>
    </el-dialog>

    <!-- 模型编辑弹窗 -->
    <el-dialog
      v-model="showModelDialog"
      :title="editingModel ? '编辑模型' : '新增模型'"
      width="520px"
      destroy-on-close
    >
      <el-form :model="modelForm" label-width="100px">
        <el-form-item label="模型标识" required>
          <el-input v-model="modelForm.modelName" placeholder="如 gpt-4o, deepseek-chat" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="modelForm.displayName" placeholder="如 GPT-4o" />
        </el-form-item>
        <el-form-item label="适用场景">
          <el-input
            v-model="modelForm.supportedScenarios"
            placeholder="多个用逗号分隔：chat,extraction,question_gen,embedding,research"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="modelForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="modelForm.isEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showModelDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveModel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { User, OfficeBuilding, Document } from "@element-plus/icons-vue";
import { adminAPI } from "@/api/admin";

const activeTab = ref("stats");

const statistics = reactive({
  userCount: null,
  workspaceCount: null,
  knowledgeCount: null,
});

const users = ref([]);
const userLoading = ref(false);
const userPagination = reactive({ current: 1, size: 10, total: 0 });

const adminWorkspaces = ref([]);
const workspaceLoading = ref(false);
const wsPagination = reactive({ current: 1, size: 10, total: 0 });

const reports = ref([]);
const reportLoading = ref(false);
const reportPagination = reactive({ current: 1, size: 10, total: 0 });

const sensitiveWords = ref([]);
const newSensitiveWord = ref("");

// AI 服务商管理
const aiProviders = ref([]);
const aiModels = ref([]);
const selectedProvider = ref(null);
const showProviderDialog = ref(false);
const showModelDialog = ref(false);
const editingProvider = ref(null);
const editingModel = ref(null);
const providerForm = reactive({
  code: "",
  name: "",
  baseUrl: "",
  apiType: "openai_compatible",
  logoUrl: "",
  isEnabled: 1,
  sortOrder: 0,
});
const modelForm = reactive({
  modelName: "",
  displayName: "",
  isEnabled: 1,
  supportedScenarios: "",
  sortOrder: 0,
});

const scenarioLabel = (code) => {
  const map = {
    chat: "对话",
    extraction: "知识提取",
    question_gen: "题目生成",
    embedding: "Embedding向量化",
    research: "研究报告",
  };
  return map[code] || code;
};

const parseScenarios = (val) => {
  if (!val) return [];
  if (Array.isArray(val)) return val;
  try {
    return JSON.parse(val);
  } catch {
    return [];
  }
};

const reportStatusType = (status) => {
  if (status === "pending") return "warning";
  if (status === "ignored") return "info";
  if (status === "removed") return "danger";
  return "info";
};

const reportStatusLabel = (status) => {
  if (status === "pending") return "待处理";
  if (status === "ignored") return "已忽略";
  if (status === "removed") return "已移除";
  return status;
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

const loadStatistics = async () => {
  try {
    const data = await adminAPI.getStatistics();
    Object.assign(statistics, data);
  } catch (error) {
    ElMessage.error("加载统计数据失败");
  }
};

const loadUsers = async () => {
  userLoading.value = true;
  try {
    const data = await adminAPI.getUsers({
      current: userPagination.current,
      size: userPagination.size,
    });
    users.value = data.records || [];
    userPagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error("加载用户列表失败");
  } finally {
    userLoading.value = false;
  }
};

const loadAdminWorkspaces = async () => {
  workspaceLoading.value = true;
  try {
    const data = await adminAPI.getWorkspaces({
      current: wsPagination.current,
      size: wsPagination.size,
    });
    adminWorkspaces.value = data.records || [];
    wsPagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error("加载工作区列表失败");
  } finally {
    workspaceLoading.value = false;
  }
};

const handleToggleUser = (user) => {
  const newStatus = user.status === 1 ? 0 : 1;
  const action = newStatus === 0 ? "禁用" : "启用";
  ElMessageBox.confirm(`确定要${action}用户「${user.username}」吗？`, `${action}用户`, {
    confirmButtonText: `确认${action}`,
    cancelButtonText: "取消",
    type: "warning",
  }).then(async () => {
    try {
      await adminAPI.disableUser(user.id, newStatus);
      ElMessage.success(`用户已${action}`);
      await loadUsers();
    } catch (error) {
      ElMessage.error(`${action}失败：` + (error.message || "未知错误"));
    }
  }).catch(() => {});
};

const handleToggleWorkspace = (ws) => {
  const newStatus = ws.status === 1 ? 0 : 1;
  const action = newStatus === 0 ? "禁用" : "启用";
  ElMessageBox.confirm(`确定要${action}工作区「${ws.name}」吗？`, `${action}工作区`, {
    confirmButtonText: `确认${action}`,
    cancelButtonText: "取消",
    type: "warning",
  }).then(async () => {
    try {
      await adminAPI.disableWorkspace(ws.id, newStatus);
      ElMessage.success(`工作区已${action}`);
      await loadAdminWorkspaces();
    } catch (error) {
      ElMessage.error(`${action}失败：` + (error.message || "未知错误"));
    }
  }).catch(() => {});
};

const loadReports = async () => {
  reportLoading.value = true;
  try {
    const data = await adminAPI.getReports({
      current: reportPagination.current,
      size: reportPagination.size,
    });
    reports.value = data.records || [];
    reportPagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error("加载举报列表失败");
  } finally {
    reportLoading.value = false;
  }
};

const handleIgnoreReport = async (row) => {
  try {
    await ElMessageBox.confirm("确定要忽略该举报吗？", "忽略举报", {
      confirmButtonText: "确认忽略",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }

  try {
    await adminAPI.handleReport(row.id, { action: "ignore" });
    ElMessage.success("已忽略该举报");
    await loadReports();
  } catch (error) {
    ElMessage.error("操作失败: " + (error.message || "未知错误"));
  }
};

const handleRemovePost = async (row) => {
  let handleNote = "";
  try {
    const { value } = await ElMessageBox.prompt("请输入移除原因", "移除分享内容", {
      confirmButtonText: "确认移除",
      cancelButtonText: "取消",
      inputType: "textarea",
      inputValidator: (val) => (val ? true : "请输入移除原因"),
    });
    handleNote = value;
  } catch {
    return;
  }

  try {
    await adminAPI.handleReport(row.id, { action: "remove", handleNote });
    ElMessage.success("内容已移除");
    await loadReports();
  } catch (error) {
    ElMessage.error("操作失败: " + (error.message || "未知错误"));
  }
};

const onTabChange = (name) => {
  if (name === "users") loadUsers();
  else if (name === "workspaces") loadAdminWorkspaces();
  else if (name === "reports") loadReports();
  else if (name === "sensitiveWords") loadSensitiveWords();
  else if (name === "aiProviders") loadAiProviders();
};

const loadSensitiveWords = async () => {
  try {
    const data = await adminAPI.getSensitiveWords();
    sensitiveWords.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error("加载敏感词列表失败");
  }
};

const handleAddSensitiveWord = async () => {
  const word = newSensitiveWord.value.trim();
  if (!word) {
    ElMessage.warning("请输入敏感词");
    return;
  }
  try {
    await adminAPI.addSensitiveWord(word);
    ElMessage.success("敏感词已添加");
    newSensitiveWord.value = "";
    await loadSensitiveWords();
  } catch (error) {
    ElMessage.error("添加失败: " + (error.message || "未知错误"));
  }
};

const handleDeleteSensitiveWord = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除敏感词「${row.word}」吗？`, "删除敏感词", {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  try {
    await adminAPI.deleteSensitiveWord(row.id);
    ElMessage.success("敏感词已删除");
    await loadSensitiveWords();
  } catch (error) {
    ElMessage.error("删除失败: " + (error.message || "未知错误"));
  }
};

// AI 服务商管理方法
const loadAiProviders = async () => {
  try {
    const data = await adminAPI.getAiProviders();
    aiProviders.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error("加载服务商列表失败");
  }
};

const loadAiModels = async (provider) => {
  selectedProvider.value = provider;
  try {
    const data = await adminAPI.getAiModels(provider.id);
    aiModels.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error("加载模型列表失败");
  }
};

const editProvider = (row) => {
  editingProvider.value = row;
  Object.assign(providerForm, {
    code: row.code,
    name: row.name,
    baseUrl: row.baseUrl || "",
    apiType: row.apiType || "openai_compatible",
    logoUrl: row.logoUrl || "",
    isEnabled: row.isEnabled ?? 1,
    sortOrder: row.sortOrder ?? 0,
  });
  showProviderDialog.value = true;
};

const handleDeleteProvider = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除服务商「${row.name}」吗？关联模型也会被删除。`, "删除服务商", {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  try {
    await adminAPI.deleteAiProvider(row.id);
    ElMessage.success("服务商已删除");
    await loadAiProviders();
    if (selectedProvider.value?.id === row.id) {
      selectedProvider.value = null;
      aiModels.value = [];
    }
  } catch (error) {
    ElMessage.error("删除失败: " + (error.message || "未知错误"));
  }
};

const handleSaveProvider = async () => {
  if (!providerForm.code || !providerForm.name) {
    ElMessage.warning("请填写服务商标识和名称");
    return;
  }
  try {
    if (editingProvider.value) {
      await adminAPI.updateAiProvider(editingProvider.value.id, { ...providerForm });
      ElMessage.success("服务商已更新");
    } else {
      await adminAPI.saveAiProvider({ ...providerForm });
      ElMessage.success("服务商已创建");
    }
    showProviderDialog.value = false;
    await loadAiProviders();
  } catch (error) {
    ElMessage.error("保存失败: " + (error.message || "未知错误"));
  }
};

const editModel = (row) => {
  editingModel.value = row;
  Object.assign(modelForm, {
    modelName: row.modelName,
    displayName: row.displayName || "",
    isEnabled: row.isEnabled ?? 1,
    supportedScenarios: Array.isArray(row.supportedScenarios)
      ? row.supportedScenarios.join(",")
      : (row.supportedScenarios || ""),
    sortOrder: row.sortOrder ?? 0,
  });
  showModelDialog.value = true;
};

const handleDeleteModel = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除模型「${row.displayName || row.modelName}」吗？`, "删除模型", {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  try {
    await adminAPI.deleteAiModel(row.id);
    ElMessage.success("模型已删除");
    if (selectedProvider.value) {
      await loadAiModels(selectedProvider.value);
    }
  } catch (error) {
    ElMessage.error("删除失败: " + (error.message || "未知错误"));
  }
};

const handleSaveModel = async () => {
  if (!modelForm.modelName) {
    ElMessage.warning("请填写模型标识");
    return;
  }
  if (!selectedProvider.value && !editingModel.value) {
    ElMessage.warning("请先选择服务商");
    return;
  }
  try {
    const payload = {
      modelName: modelForm.modelName,
      displayName: modelForm.displayName || modelForm.modelName,
      isEnabled: modelForm.isEnabled,
      supportedScenarios: modelForm.supportedScenarios
        ? modelForm.supportedScenarios.split(",").map((s) => s.trim()).filter(Boolean)
        : [],
      sortOrder: modelForm.sortOrder,
    };
    if (editingModel.value) {
      await adminAPI.updateAiModel(editingModel.value.id, payload);
      ElMessage.success("模型已更新");
    } else {
      await adminAPI.saveAiModel(selectedProvider.value.id, payload);
      ElMessage.success("模型已创建");
    }
    showModelDialog.value = false;
    if (selectedProvider.value) {
      await loadAiModels(selectedProvider.value);
    }
  } catch (error) {
    ElMessage.error("保存失败: " + (error.message || "未知错误"));
  }
};

onMounted(() => {
  loadStatistics();
});
</script>

<style scoped>
.admin-page {
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
  padding: var(--spacing-lg);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-lg);
  padding: var(--spacing-lg) 0;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-xl);
  background: var(--bg-input);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-lighter);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--spacing-lg);
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

.handled-info {
  font-size: 13px;
  color: var(--text-secondary);
}

.sensitive-word-header {
  display: flex;
  gap: 12px;
  align-items: center;
}

.ai-provider-header {
  display: flex;
  gap: 12px;
  align-items: center;
}

.ai-model-section {
  margin-top: var(--spacing-xl);
  padding: var(--spacing-lg);
  background: var(--bg-input);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-lighter);
}

.ai-model-section h3 {
  margin: 0 0 var(--spacing-md) 0;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}
</style>
