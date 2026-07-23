# PRD：多用户数据隔离与管理员管控

> 版本：V1.0
> 日期：2026-07-23
> 状态：草稿
> 来源：`docs/需求池.md` 需求 #1

---

## 1. Executive Summary

### 问题陈述

当前 AI-SecondBrain 是单用户系统——每个用户只能管理自己的知识卡片，User 实体无角色/权限字段，JWT 仅存 userId 无权限声明，数据隔离仅靠 controller 层手动校验 userId。无法支持团队协作、平台运维等场景。

### 解决方案

引入 **租户（Workspace）+ RBAC** 双层权限体系：
- **租户层**：用户可创建/加入多个 Workspace，每个 Workspace 内数据隔离
- **角色层**：平台级（Super Admin / User）+ Workspace 级（Owner / Admin / Editor / Viewer）
- **权限层**：对知识节点的 CRUD 操作进行细粒度控制

### 成功标准

| # | KPI | 目标值 |
|---|-----|--------|
| 1 | 跨租户数据隔离 | 用户 A 请求用户 B 的 Workspace 内数据返回 403 |
| 2 | 权限校验覆盖率 | 100% 受保护接口经过 RBAC 校验（移除手动 userId 检查） |
| 3 | 现有功能兼容 | 所有现有 API 在单用户 Workspace 场景下行为不变 |
| 4 | 权限校验延迟 | JWT 解析 + 权限查询 < 50ms（P95） |

---

## 2. User Experience & Functionality

### 2.1 用户角色

| 角色 | 层级 | 描述 |
|------|------|------|
| **Super Admin** | 平台 | 平台运维方，管理所有用户和 Workspace，可查看系统统计 |
| **User** | 平台 | 普通注册用户，可创建 Workspace 或加入他人 Workspace |
| **Workspace Owner** | Workspace | Workspace 创建者，拥有最高权限（含删除 Workspace） |
| **Workspace Admin** | Workspace | Workspace 管理员，可管理成员和权限，可编辑所有内容 |
| **Workspace Editor** | Workspace | 可创建/编辑知识点，不可管理成员 |
| **Workspace Viewer** | Workspace | 只读访问知识库，不可编辑 |

### 2.2 用户故事

| ID | 描述 | 优先级 |
|----|------|--------|
| US-1 | 作为用户，创建 Workspace 并自动成为 Owner | P0 |
| US-2 | 作为 Workspace Owner/Admin，邀请其他用户加入 Workspace 并分配角色 | P0 |
| US-3 | 作为 Workspace 成员，切换不同的 Workspace 上下文查看对应数据 | P0 |
| US-4 | 作为 Workspace 成员，只能访问所属 Workspace 的知识数据 | P0 |
| US-5 | 作为 Workspace Owner，转让所有权或删除 Workspace | P1 |
| US-6 | 作为 Super Admin，查看平台所有 Workspace 和用户统计 | P1 |
| US-7 | 作为 Super Admin，禁用违规用户或 Workspace | P1 |
| US-8 | 作为用户，在个人设置中看到自己所属的所有 Workspace | P1 |

### 2.3 验收标准

**US-1：创建 Workspace**

- [ ] `POST /api/workspace` 传入 name + description，创建后返回 workspaceId
- [ ] 创建者自动成为 Workspace Owner
- [ ] 每个用户最多创建 10 个 Workspace（可配置）
- [ ] 创建后自动生成默认的 "个人" 知识库（等同于当前个人空间）

**US-2：邀请成员**

- [ ] `POST /api/workspace/{id}/members` 传入 userId + role（admin/editor/viewer）
- [ ] 仅 Owner 和 Admin 可邀请
- [ ] 被邀请者收到站内通知，可选择接受/拒绝
- [ ] 重复邀请幂等处理（不创建重复成员记录）

**US-3：切换 Workspace**

- [ ] `GET /api/workspace` 返回用户所属 Workspace 列表
- [ ] 前端全局 Workspace 切换器，切换后所有 API 请求自动带当前 workspaceId
- [ ] 刷新页面后保持当前选中的 Workspace（localStorage）

**US-4：数据隔离**

- [ ] 所有 `/knowledge/**`、`/review/**`、`/chat/**`、`/report/**` 接口在查询时自动过滤当前 workspaceId
- [ ] 通过直接修改请求参数中的知识节点 ID 访问其他 Workspace 数据时返回 403
- [ ] 知识图谱、复习卡片等关联数据一并隔离

**US-5：所有权转让与删除**

- [ ] Owner 可转让所有权给 Workspace 内任意成员
- [ ] 转让后原 Owner 降为 Admin
- [ ] Owner 可删除 Workspace（软删除），所有关联数据标记删除
- [ ] 删除操作需二次确认（输入 Workspace 名称）

**US-6：Super Admin 仪表盘**

- [ ] `GET /api/admin/statistics` 返回：总用户数、总 Workspace 数、总知识节点数
- [ ] 仅 Super Admin 角色可访问

**US-7：Super Admin 管控**

- [ ] `PUT /api/admin/users/{id}/disable` 禁用用户（禁止登录）
- [ ] `PUT /api/admin/workspaces/{id}/disable` 禁用 Workspace（所有成员不可访问）

**US-8：个人 Workspace 列表**

- [ ] `GET /api/user/workspaces` 返回用户所属 Workspace 及其角色

### 2.4 非目标（V1 不做）

- 自定义角色权限矩阵（RBAC 预定义角色，不支持自定义）
- Workspace 级资源配额管理
- 操作审计日志
- 知识库级别独立权限（V1 整个 Workspace 统一权限）
- SSO / OAuth 登录

---

## 3. Technical Specifications

### 3.1 新增数据模型

```sql
-- 租户/工作区
CREATE TABLE workspace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '工作区名称',
    description VARCHAR(500) COMMENT '工作区描述',
    owner_id BIGINT NOT NULL COMMENT '创建者用户ID',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_owner (owner_id),
    INDEX idx_status (status)
) COMMENT '工作区/租户';

-- 工作区成员
CREATE TABLE workspace_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL COMMENT '工作区ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：owner/admin/editor/viewer',
    joined_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    UNIQUE KEY uk_ws_user (workspace_id, user_id),
    INDEX idx_user (user_id)
) COMMENT '工作区成员';

-- 平台级用户角色
ALTER TABLE user ADD COLUMN role VARCHAR(20) DEFAULT 'user' COMMENT '平台角色：super_admin/user';
ALTER TABLE user ADD COLUMN status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用';

-- 现有业务表增加 workspace_id
ALTER TABLE knowledge_node ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE knowledge_relation ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE review_card ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE review_log ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE chat_session ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE chat_message ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE learning_report ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE async_task ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE raw_chat_record ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE research_history ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
```

### 3.2 实体关系

```
User (1) ──< (N) WorkspaceMember (N) >── (1) Workspace
                         │
                         ├── role: owner/admin/editor/viewer
                         │
User.role: super_admin / user  (平台级)

Workspace (1) ──< (N) KnowledgeNode
Workspace (1) ──< (N) KnowledgeRelation
Workspace (1) ──< (N) ReviewCard
... (所有业务实体)
```

### 3.3 JWT Token 增强

```java
// 原有 claims
claims.put("userId", user.getId());
claims.put("username", user.getUsername());

// 新增 claims
claims.put("role", user.getRole());              // super_admin / user
claims.put("currentWsId", currentWorkspaceId);    // 当前选中的 workspace（可切换）
```

### 3.4 权限拦截架构

```
HTTP Request
    │
    ▼
JwtInterceptor (已有，需增强)
    ├── 解析 JWT → userId, role, currentWsId
    ├── 设置 request attribute
    │
    ▼
WorkspaceInterceptor (新增)
    ├── 从 request 提取 workspaceId（header / path / param）
    ├── 校验用户是否属于该 workspace
    ├── 校验用户在该 workspace 的角色权限
    ├── 不通过 → 返回 403
    │
    ▼
Controller → Service
    ├── Service 层统一注入 workspaceId 过滤条件
    ├── 移除所有手动 "if (userId == null)" 检查
```

### 3.5 API 设计

#### Workspace 管理

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/workspace` | 登录用户 | 创建 Workspace |
| GET | `/api/workspace` | 登录用户 | 我的 Workspace 列表 |
| GET | `/api/workspace/{id}` | Workspace 成员 | Workspace 详情 |
| PUT | `/api/workspace/{id}` | Owner/Admin | 编辑 Workspace 信息 |
| DELETE | `/api/workspace/{id}` | Owner | 删除 Workspace（软删除） |
| PUT | `/api/workspace/{id}/transfer` | Owner | 转让所有权 |
| PUT | `/api/workspace/{id}/switch` | Workspace 成员 | 切换当前上下文 |

#### 成员管理

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/workspace/{id}/members` | Owner/Admin | 邀请成员 |
| GET | `/api/workspace/{id}/members` | Workspace 成员 | 成员列表 |
| PUT | `/api/workspace/{id}/members/{userId}` | Owner/Admin | 修改成员角色 |
| DELETE | `/api/workspace/{id}/members/{userId}` | Owner/Admin | 移除成员 |

#### Super Admin

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/admin/statistics` | Super Admin | 平台统计 |
| GET | `/api/admin/users` | Super Admin | 用户列表 |
| PUT | `/api/admin/users/{id}/disable` | Super Admin | 禁用/启用用户 |
| GET | `/api/admin/workspaces` | Super Admin | Workspace 列表 |
| PUT | `/api/admin/workspaces/{id}/disable` | Super Admin | 禁用/启用 Workspace |

### 3.6 安全设计

- **水平越权防护**：Service 层统一注入 `workspaceId` 过滤，所有 MyBatis-Plus 查询自动带 `workspace_id = ?` 条件，不可通过 API 参数覆盖
- **JWT 安全**：切换 Workspace 时颁发新 JWT（含新 currentWsId），旧 token 在有效期内仍可用但指向旧 workspace 上下文
- **Super Admin 登录**：与非 Admin 共用 `/api/auth/login`，通过 user 表的 role 字段区分，JWT 中携带 role claim
- **密码安全**：沿用现有 BCrypt 方案，不做改动

### 3.7 现有代码改动范围

| 层 | 改动 | 影响 |
|----|------|------|
| Entity | User 加 role/status；新增 Workspace/WorkspaceMember；10 个业务实体加 workspaceId | 向后兼容（workspaceId 默认 NULL = 个人数据） |
| Mapper | 新增 WorkspaceMapper / WorkspaceMemberMapper；10 个现有 Mapper 增加 workspaceId 查询条件 | 需修改自定义 SQL（如有） |
| Service | 新增 WorkspaceService / WorkspaceMemberService；所有业务 Service 增加 workspaceId 过滤 | 核心改动 |
| Controller | 新增 WorkspaceController / AdminController；所有业务 Controller 移除手动 userId 校验 | 统一为拦截器校验 |
| Interceptor | 新增 WorkspaceInterceptor；增强 JwtInterceptor | 新增 |
| JWT | JwtUtil 增加 role/workspaceId claim | 兼容旧 token（无 claim 时默认单用户模式） |
| 前端 | 新增 Workspace 管理页面；全局 Workspace 切换器；路由守卫更新 | 新页面 + 全局组件 |
| DDL | 2 张新表 + 1 张 ALTER + 10 张表加列 | 需数据迁移脚本 |

---

## 4. Risks & Roadmap

### 4.1 分阶段实施

```
V1.0 (MVP) — 2~3 周
├── 新增 workspace / workspace_member 表 + user 表加 role/status 列
├── 新增 Workspace CRUD + 成员管理 API
├── 业务表加 workspace_id 列（默认 NULL，兼容现有数据）
├── WorkspaceInterceptor 实现
├── JWT 增强（role + currentWsId）
├── Service 层 workspaceId 过滤
├── 前端：Workspace 创建/切换、成员管理页
└── 移除所有手动 userId 校验代码

V1.1 — 1 周
├── Super Admin 仪表盘
├── Super Admin 用户/Workspace 管控
└── 前端 Admin 页面

V1.2 — 1 周
├── 数据迁移：现有 userId 数据自动归入默认个人 Workspace
├── 兼容模式：无 workspaceId 的旧接口保持个人空间行为
└── 集成测试全覆盖
```

### 4.2 技术风险

| 风险 | 概率 | 影响 | 缓解 |
|------|------|------|------|
| 现有数据迁移时 workspaceId 回填错误 | 中 | 高 | V1.0 保持 NULL 兼容，V1.2 做背景迁移，提供回滚脚本 |
| Service 层 workspaceId 过滤遗漏导致数据泄漏 | 中 | 高 | 编写集成测试覆盖所有 API；Code Review 检查 List 查询 |
| MyBatis-Plus 自定义 SQL 漏加 workspaceId 条件 | 中 | 中 | 全局搜索 XML Mapper 和 `@Select` 注解，逐一审计 |
| JWT 中 role 被篡改导致越权 | 低 | 高 | JWT 由服务端签发（HMAC-SHA256），不可客户端伪造 |
| 现有多余 userId 校验代码和拦截器冲突 | 低 | 低 | 搜索所有 `if (userId == null)` 模式，V1.0 统一移除 |

### 4.3 回滚方案

- 新增表（workspace/workspace_member）可独立回滚，不影响现有功能
- 业务表新增的 workspace_id 列为 NULL 可空，删列即可回滚
- JWT 新增 claim 向前兼容（旧 token 无 claim → 拦截器跳过 workspace 校验 → 保持单用户行为）
- Git 回滚粒度：DDL 单独脚本，代码按模块拆分 commit

---

## 附录

### A. 数据库迁移计划

```
Step 1: 执行 DDL（新增表 + ALTER TABLE，不影响现有数据读写）
Step 2: 部署 V1.0 代码（workspace_id NULL → 拦截器放行）
Step 3: V1.2 数据迁移脚本：为每个 userId 创建默认个人 Workspace，回填所有 NULL workspace_id
Step 4: workspace_id 设为 NOT NULL（需确认迁移完成且无遗漏）
```

### B. API 向后兼容

```
V1.0 发布后：
- 无 workspaceId header 的请求 → 拦截器从用户默认 Workspace 获取
- 旧客户端无感知，行为与单用户模式一致
- 新客户端传 workspaceId → 拦截器校验权限
```
