# 工作区/RBAC 前端实施计划

> 版本：V1.0
> 日期：2026-07-23
> 状态：执行中
> 来源：`docs/requirements/prd-multi-tenant-rbac.md`

---

## 1. 背景

多租户工作区/RBAC 功能已完成约 70%。后端（WorkspaceInterceptor、JwtInterceptor、WorkspaceController、AdminController、所有 Entity/Mapper/Service）已全部就绪。前端需补充 3 个页面 + API 层。

### 已完成（无需改动）

- **后端**: WorkspaceInterceptor、JwtInterceptor、WorkspaceController、AdminController、所有 Entity/Mapper/Service、WebMvcConfig
- **前端**: workspace API（基础 CRUD）、workspace Pinia store、WorkspaceSwitcher 组件、MainLayout 集成、设计系统（sage green 色系）

### 本计划覆盖

1. 前端 API 层：补充 member + admin 接口
2. 设置页 → 工作区管理区块
3. 新页面：工作区成员管理
4. 新页面：超级管理员面板
5. 路由配置更新

---

## 2. 任务分解

### 任务 1：补充前端 API 层

**1a. `frontend/src/api/workspace.js` — 追加成员接口**

```js
getMembers(id)           → GET    /workspace/{id}/members
addMember(id, data)      → POST   /workspace/{id}/members
updateMemberRole(id, userId, data) → PUT /workspace/{id}/members/{userId}
removeMember(id, userId) → DELETE /workspace/{id}/members/{userId}
transferOwnership(id, data) → PUT /workspace/{id}/transfer
```

**1b. 新建 `frontend/src/api/admin.js`**

```js
getStatistics()              → GET  /admin/statistics
getUsers(params)             → GET  /admin/users
disableUser(id, status)      → PUT  /admin/users/{id}/disable?status=
getWorkspaces(params)        → GET  /admin/workspaces
disableWorkspace(id, status) → PUT  /admin/workspaces/{id}/disable?status=
```

### 任务 2：设置页 → 工作区管理区块

**文件**: `frontend/src/views/Settings.vue`（修改）

在"知识管理"区块与退出登录区块之间新增"我的工作区" section-card：
- 工作区列表（名称、角色标签、成员数、创建时间、操作按钮）
- 新建/编辑弹窗（共用一个 el-dialog）
- 删除二次确认（ElMessageBox.confirm）
- 空状态（el-empty）

### 任务 3：工作区成员管理页面

**文件**: `frontend/src/views/WorkspaceMembers.vue`（新建）
**路由**: `/workspace/:id/members`

- 成员表格（用户名、角色、加入时间、操作）
- 邀请成员弹窗（用户名输入 + 角色选择）
- 修改角色弹窗
- 移除成员二次确认
- Owner/Admin 权限控制

### 任务 4：超级管理员面板

**文件**: `frontend/src/views/AdminDashboard.vue`（新建）
**路由**: `/admin`（需 super_admin 角色）

- Tab 1: 平台统计（3 个统计卡片）
- Tab 2: 用户管理（分页表格 + 禁用/启用）
- Tab 3: 工作区管理（分页表格 + 禁用/启用）

### 任务 5：路由配置更新

**文件**: `frontend/src/router/index.js`（修改）

- 新增 `/workspace/:id/members` 路由
- 新增 `/admin` 路由（requiresAdmin 元数据）
- beforeEach 守卫增加 requiresAdmin 检查

---

## 3. 实施顺序

| 顺序 | 任务 | 依赖 |
|------|------|------|
| 1 | API 层（workspace.js 补充 + admin.js 新建） | 无 |
| 2 | 设置页工作区区块 | 任务 1 |
| 3 | 成员管理页面 | 任务 1 |
| 4 | 管理员面板 | 任务 1 |
| 5 | 路由更新 | 任务 3, 4 |

---

## 4. 涉及文件

### 新建
- `frontend/src/api/admin.js`
- `frontend/src/views/WorkspaceMembers.vue`
- `frontend/src/views/AdminDashboard.vue`

### 修改
- `frontend/src/api/workspace.js`
- `frontend/src/views/Settings.vue`
- `frontend/src/router/index.js`

---

## 5. 验证

1. 设置页：进入 `/settings`，确认工作区列表展示、新建/编辑/删除工作区
2. 成员管理：进入某工作区成员页，确认列表、邀请、修改角色、移除成员
3. 工作区切换器：切换工作区后 header 和数据正确更新
4. 管理员面板：super_admin 进入 `/admin`，三个 tab 均正常
5. 权限守卫：普通用户访问 `/admin` 被重定向到 dashboard
