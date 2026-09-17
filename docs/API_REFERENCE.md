# AI-SecondBrain API 接口文档

版本：V2.1
更新日期：2026-09-03
维护人：AI-SecondBrain Team

---

## 接口概览

| 模块 | 章节号 | 接口数 | 说明 |
|------|--------|--------|------|
| 认证与用户模块 | 第1章 | 9 | 登录注册、用户信息管理、AI配置 |
| 知识管理模块 | 第2章 | 35 | 知识CRUD、图谱、标签、编辑锁、版本管理、待确认知识点 |
| AI对话与采集模块 | 第3章 | 10 | 对话采集、批量导入、快捷采集、会话管理 |
| 复习系统模块 | 第4章 | 17 | 复习卡片生成、提交、统计、题目池 |
| AI问答与报告模块 | 第5章 | 19 | RAG问答、学习报告、DeerFlow深度研究、AI服务商 |
| 研究项目模块 | 第6章 | 28 | 研究项目CRUD、任务管理、计划、来源、记忆 |
| 工作区模块 | 第7章 | 13 | 工作区CRUD、成员管理、权限控制 |
| 知识广场与分享模块 | 第8章 | 15 | 发布、点赞、评论、收藏、分享 |
| 游戏化与统计模块 | 第9章 | 9 | 积分、成就、排行榜、学习统计 |
| 通知与导出模块 | 第10章 | 9 | 通知管理、数据导出 |
| 向量、系统与管理模块 | 第11章 | 15 | 向量管理、系统健康、管理后台 |
| **合计** | | **179** | |

---

## 通用说明

### 基础信息

- **Base URL**: `http://localhost:8080/api`
- **API 文档**: `http://localhost:8080/api/doc.html` (Knife4j)
- **协议**: HTTP/HTTPS

### 认证方式

除登录、注册、健康检查、访问分享等公开接口外，所有接口需要在请求头中携带 JWT Token：

```
Authorization: Bearer <token>
```

Token 由登录接口（1.2 用户登录）返回，有效期默认 7 天。

### 请求格式

```
Content-Type: application/json
```

### 统一响应格式

所有接口（除特殊说明外）均返回统一 JSON 格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 401 | 未登录或Token过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如编辑锁冲突） |
| 500 | 服务器内部错误 |

### 分页参数

分页接口统一使用以下参数：

| 参数名 | 类型 | 位置 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| current | int | query | 否 | 1 | 当前页码 |
| size | int | query | 否 | 10 | 每页数量 |

分页响应格式（MyBatis-Plus Page）：

```json
{
  "code": 200,
  "data": {
    "records": [],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

### 工作区隔离

所有业务接口都支持工作区数据隔离。JWT 拦截器会自动从 Token 或 Header 中解析 `workspaceId`，并注入到 `HttpServletRequest` 的 attribute 中。Service 层查询时会自动附加 `workspace_id` 条件，确保不同工作区的数据互不可见。

---

## 接口详情

### 第1章：认证与用户模块

#### 1.1 用户注册

- **接口名称**：用户注册
- **请求方法和路径**：`POST /api/auth/register`
- **接口描述**：用户注册接口，提供新用户账号创建功能。用户名长度需在 3-20 个字符，密码长度不少于 6 位。
- **是否需要登录认证**：否

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| username | String | body | 是 | 用户名（3-20 个字符） |
| password | String | body | 是 | 密码（不少于 6 位） |

**请求示例**

```json
{
  "username": "alice",
  "password": "alice123456"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "注册成功",
  "data": "注册成功"
}
```

---

#### 1.2 用户登录

- **接口名称**：用户登录
- **请求方法和路径**：`POST /api/auth/login`
- **接口描述**：用户登录接口，校验账号密码后返回 JWT Token 及用户基本信息，后续请求需在 Header 中携带该 Token。
- **是否需要登录认证**：否

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| username | String | body | 是 | 用户名 |
| password | String | body | 是 | 密码 |

**请求示例**

```json
{
  "username": "alice",
  "password": "alice123456"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlcklkIjoxLCJleHAiOjE3NTMyMzQ1Njd9.xxxxx",
    "userInfo": {
      "id": 1,
      "username": "alice",
      "email": "alice@example.com",
      "phone": "13800000000",
      "bio": "学习爱好者",
      "role": "user"
    }
  }
}
```

---

#### 1.2.1 微信小程序登录

- **接口名称**：微信小程序登录
- **请求方法和路径**：`POST /api/auth/wx-login`
- **接口描述**：接收 `wx.login` 返回的一次性 code，由服务端向微信换取 openid 并绑定本地用户，返回与账号密码登录一致的 JWT。
- **是否需要登录认证**：否
- **前置配置**：服务端通过 `WECHAT_APP_ID` 与 `WECHAT_APP_SECRET` 注入小程序凭证；未配置时返回 503，不创建伪账户。
- **隐私说明**：仅保存 provider + openid 映射，微信 `session_key` 不落库、不回传。

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| code | String | body | 是 | `wx.login` 返回的一次性登录凭证 |

**请求示例**

```json
{
  "code": "微信一次性 code"
}
```

**响应示例**

响应结构与 1.2 用户登录相同，`data.token` 为平台 JWT，`data.userInfo.username` 为系统生成的公开昵称。

---

#### 1.3 获取用户信息

- **接口名称**：获取用户信息
- **请求方法和路径**：`GET /api/user/info`
- **接口描述**：获取当前登录用户的详细信息（密码字段会被置空，不返回）。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |

**请求示例**

```http
GET /api/user/info
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "alice",
    "password": null,
    "email": "alice@example.com",
    "phone": "13800000000",
    "bio": "学习爱好者",
    "avatar": "https://oss.example.com/avatar/1.png",
    "apiKey": "sk-xxxx",
    "registerTime": "2026-07-20T10:00:00",
    "lastLoginTime": "2026-07-24T09:30:00",
    "createTime": "2026-07-20T10:00:00",
    "updateTime": "2026-07-24T09:30:00",
    "deleted": 0,
    "role": "user",
    "status": 1
  }
}
```

---

#### 1.4 更新用户信息

- **接口名称**：更新用户信息
- **请求方法和路径**：`PUT /api/user/update`
- **接口描述**：更新当前登录用户的基本信息，包括用户名、邮箱、手机号、个人简介及 API Key。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| username | String | body | 否 | 用户名 |
| email | String | body | 否 | 邮箱 |
| phone | String | body | 否 | 手机号 |
| bio | String | body | 否 | 个人简介 |
| apiKey | String | body | 否 | API Key（用于调用外部 AI 服务） |

**请求示例**

```json
{
  "username": "alice_new",
  "email": "alice_new@example.com",
  "phone": "13900000000",
  "bio": "终身学习者",
  "apiKey": "sk-newkeyxxxx"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "更新成功",
  "data": "更新成功"
}
```

---

#### 1.5 修改密码

- **接口名称**：修改密码
- **请求方法和路径**：`PUT /api/user/password`
- **接口描述**：修改当前登录用户的密码，需提供原密码用于校验。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| oldPassword | String | body | 是 | 原密码 |
| newPassword | String | body | 是 | 新密码 |

**请求示例**

```json
{
  "oldPassword": "alice123456",
  "newPassword": "alice654321"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": "密码修改成功"
}
```

---

#### 1.6 上传头像

- **接口名称**：上传头像
- **请求方法和路径**：`POST /api/user/avatar`
- **接口描述**：上传用户头像。仅支持图片类型文件（`image/*`），单文件大小不能超过 5MB。上传成功后会更新用户的头像 URL。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| file | MultipartFile | body (form-data) | 是 | 头像图片文件，必须为图片类型，最大 5MB |

**请求示例**

```http
POST /api/user/avatar
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="avatar.png"
Content-Type: image/png

<二进制文件内容>
------WebKitFormBoundary--
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "https://oss.example.com/avatar/1.png"
}
```

---

#### 1.7 获取用户AI配置

- **接口名称**：获取用户AI配置
- **请求方法和路径**：`GET /api/user/ai-config`
- **接口描述**：获取当前用户按场景配置的AI服务商、模型和API Key信息。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |

**请求示例**

```http
GET /api/user/ai-config
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "scene": "chat",
      "providerId": 1,
      "providerCode": "openai",
      "providerName": "OpenAI",
      "modelId": 10,
      "modelName": "gpt-4o",
      "apiKey": "sk-***",
      "hasKey": true
    },
    {
      "scene": "embedding",
      "providerId": 2,
      "providerCode": "siliconflow",
      "providerName": "SiliconFlow",
      "modelId": 21,
      "modelName": "bge-large-zh",
      "apiKey": null,
      "hasKey": false
    }
  ]
}
```

---

#### 1.8 批量保存场景配置

- **接口名称**：批量保存场景配置
- **请求方法和路径**：`PUT /api/user/ai-config`
- **接口描述**：批量保存当前用户在不同场景下的AI服务商和模型配置。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| body | Object | body | 是 | 场景配置列表，每个元素包含场景、服务商ID、模型ID等字段 |

**请求示例**

```http
PUT /api/user/ai-config
Authorization: Bearer <token>
Content-Type: application/json

[
  {
    "scene": "chat",
    "providerId": 1,
    "modelId": 10
  },
  {
    "scene": "embedding",
    "providerId": 2,
    "modelId": 21
  }
]
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

#### 1.9 保存服务商全局API Key

- **接口名称**：保存服务商全局API Key
- **请求方法和路径**：`PUT /api/user/ai-config/provider-key`
- **接口描述**：为指定AI服务商保存当前用户的全局API Key，该Key可被同服务商下的多个场景复用。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| body | Object | body | 是 | 请求体，包含 providerId 和 apiKey 字段 |

**请求示例**

```http
PUT /api/user/ai-config/provider-key
Authorization: Bearer <token>
Content-Type: application/json

{
  "providerId": 1,
  "apiKey": "sk-xxxxxxxxxxxxxxxxxxxx"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

### 第2章：知识管理模块

#### 2.1 知识列表

- **接口名称**：知识列表
- **请求方法和路径**：`GET /api/knowledge/list`
- **接口描述**：分页查询当前用户在当前工作区下的知识列表，支持按关键词、重要程度、掌握程度过滤。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| current | Integer | query | 否 | 当前页，默认 1 |
| size | Integer | query | 否 | 每页大小，默认 10 |
| keyword | String | query | 否 | 搜索关键词 |
| importance | Integer | query | 否 | 重要程度（1-5） |
| masteryLevel | Integer | query | 否 | 掌握程度（0-5） |

**请求示例**

```http
GET /api/knowledge/list?current=1&size=10&keyword=Java&importance=4&masteryLevel=2
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1001,
        "title": "Java 并发编程基础",
        "summary": "介绍线程、锁、并发容器等核心概念",
        "contentMd": "# Java 并发编程\n...",
        "importance": 4,
        "masteryLevel": 2,
        "reviewCount": 3,
        "nextReviewTime": "2026-07-26T10:00:00",
        "createTime": "2026-07-20T10:00:00",
        "score": null
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 2.2 知识详情

- **接口名称**：知识详情
- **请求方法和路径**：`GET /api/knowledge/{id}`
- **接口描述**：根据知识节点 ID 查询知识详情。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 知识节点 ID |

**请求示例**

```http
GET /api/knowledge/1001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "title": "Java 并发编程基础",
    "summary": "介绍线程、锁、并发容器等核心概念",
    "contentMd": "# Java 并发编程\n线程是进程中的执行单元...",
    "importance": 4,
    "masteryLevel": 2,
    "reviewCount": 3,
    "nextReviewTime": "2026-07-26T10:00:00",
    "createTime": "2026-07-20T10:00:00",
    "score": null
  }
}
```

---

#### 2.3 创建知识

- **接口名称**：创建知识
- **请求方法和路径**：`POST /api/knowledge`
- **接口描述**：创建新的知识节点。返回创建后的知识详情。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| title | String | body | 是 | 标题 |
| summary | String | body | 否 | 摘要 |
| contentMd | String | body | 否 | 内容（Markdown 格式） |
| importance | Integer | body | 否 | 重要程度（1-5） |

**请求示例**

```json
{
  "title": "Spring Boot 启动流程",
  "summary": "梳理 SpringApplication.run 的关键步骤",
  "contentMd": "# Spring Boot 启动流程\n1. 创建 SpringApplication 实例\n2. 准备环境\n3. 创建 ApplicationContext\n4. refreshContext",
  "importance": 5
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1002,
    "title": "Spring Boot 启动流程",
    "summary": "梳理 SpringApplication.run 的关键步骤",
    "contentMd": "# Spring Boot 启动流程\n1. 创建 SpringApplication 实例\n2. 准备环境\n3. 创建 ApplicationContext\n4. refreshContext",
    "importance": 5,
    "masteryLevel": 0,
    "reviewCount": 0,
    "nextReviewTime": "2026-07-27T10:00:00",
    "createTime": "2026-07-24T11:00:00",
    "score": null
  }
}
```

---

#### 2.4 删除知识

- **接口名称**：删除知识
- **请求方法和路径**：`DELETE /api/knowledge/{id}`
- **接口描述**：根据知识节点 ID 删除知识。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 知识节点 ID |

**请求示例**

```http
DELETE /api/knowledge/1002
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

#### 2.5 更新重要程度

- **接口名称**：更新重要程度
- **请求方法和路径**：`PUT /api/knowledge/{id}/importance`
- **接口描述**：单独更新知识点的重要程度。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 知识节点 ID |
| importance | Integer | query | 是 | 重要程度（1-5） |

**请求示例**

```http
PUT /api/knowledge/1001/importance?importance=5
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

---

#### 2.6 更新知识点

- **接口名称**：更新知识点
- **请求方法和路径**：`PUT /api/knowledge/{id}`
- **接口描述**：更新知识点内容。更新前会检查编辑锁：若存在他人持有的锁则拒绝更新（返回 409）。更新成功后会自动保存版本快照。若请求中包含 importance 字段，则同时更新重要程度。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 知识节点 ID |
| title | String | body | 否 | 标题 |
| summary | String | body | 否 | 摘要 |
| contentMd | String | body | 否 | 内容（Markdown 格式） |
| importance | Integer | body | 否 | 重要程度（1-5），非空时同步更新 |

**请求示例**

```json
{
  "title": "Java 并发编程基础（更新版）",
  "summary": "新增 JUC 包介绍",
  "contentMd": "# Java 并发编程\n线程是进程中的执行单元...\n## JUC 包\nAtomicXxx、Lock、并发容器等",
  "importance": 5
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

**冲突响应示例**

```json
{
  "code": 409,
  "message": "bob 正在编辑此知识点，请稍后再试",
  "data": null
}
```

---

#### 2.7 搜索知识点

- **接口名称**：搜索知识点
- **请求方法和路径**：`GET /api/knowledge/search`
- **接口描述**：根据关键词搜索知识点。未登录时返回 401。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| keyword | String | query | 是 | 搜索关键词 |

**请求示例**

```http
GET /api/knowledge/search?keyword=并发
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1001,
      "title": "Java 并发编程基础",
      "summary": "介绍线程、锁、并发容器等核心概念",
      "contentMd": "# Java 并发编程\n...",
      "importance": 4,
      "masteryLevel": 2,
      "reviewCount": 3,
      "nextReviewTime": "2026-07-26T10:00:00",
      "createTime": "2026-07-20T10:00:00",
      "score": null
    }
  ]
}
```

---

#### 2.8 多字段搜索知识点

- **接口名称**：多字段搜索知识点
- **请求方法和路径**：`GET /api/knowledge/search/multi`
- **接口描述**：在标题、摘要、内容等多个字段中搜索知识点。未登录时返回 401。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| keyword | String | query | 是 | 搜索关键词 |

**请求示例**

```http
GET /api/knowledge/search/multi?keyword=Spring
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1002,
      "title": "Spring Boot 启动流程",
      "summary": "梳理 SpringApplication.run 的关键步骤",
      "contentMd": "# Spring Boot 启动流程\n...",
      "importance": 5,
      "masteryLevel": 0,
      "reviewCount": 0,
      "nextReviewTime": "2026-07-27T10:00:00",
      "createTime": "2026-07-24T11:00:00",
      "score": null
    }
  ]
}
```

---

#### 2.9 语义搜索知识点

- **接口名称**：语义搜索知识点
- **请求方法和路径**：`GET /api/knowledge/search/semantic`
- **接口描述**：使用向量相似度进行语义搜索。系统会读取用户的 API Key 调用嵌入服务；若用户未配置 API Key 则使用默认配置。未登录时返回 401。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| queryText | String | query | 是 | 搜索文本 |
| topK | Integer | query | 否 | 返回结果数量，默认 10 |

**请求示例**

```http
GET /api/knowledge/search/semantic?queryText=如何理解线程安全&topK=5
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1001,
      "title": "Java 并发编程基础",
      "summary": "介绍线程、锁、并发容器等核心概念",
      "contentMd": "# Java 并发编程\n...",
      "importance": 4,
      "masteryLevel": 2,
      "reviewCount": 3,
      "nextReviewTime": "2026-07-26T10:00:00",
      "createTime": "2026-07-20T10:00:00",
      "score": 0.8923
    }
  ]
}
```

---

#### 2.10 同步到Elasticsearch

- **接口名称**：同步到Elasticsearch
- **请求方法和路径**：`POST /api/knowledge/sync-to-elasticsearch`
- **接口描述**：将当前用户在当前工作区下的所有知识点同步到 Elasticsearch 索引，便于全文检索。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |

**请求示例**

```http
POST /api/knowledge/sync-to-elasticsearch
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "同步成功",
  "data": null
}
```

---

#### 2.11 版本历史列表

- **接口名称**：版本历史列表
- **请求方法和路径**：`GET /api/knowledge/{nodeId}/revisions`
- **接口描述**：获取指定知识节点的版本历史记录。若知识点不存在返回 404。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| nodeId | Long | path | 是 | 知识节点 ID |

**请求示例**

```http
GET /api/knowledge/1001/revisions
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 5001,
      "nodeId": 1001,
      "userId": 1,
      "title": "Java 并发编程基础（更新版）",
      "contentMd": "# Java 并发编程\n线程是进程中的执行单元...\n## JUC 包\n...",
      "summary": "新增 JUC 包介绍",
      "revisionNum": 2,
      "changeSummary": null,
      "createdAt": "2026-07-24T11:30:00"
    },
    {
      "id": 5000,
      "nodeId": 1001,
      "userId": 1,
      "title": "Java 并发编程基础",
      "contentMd": "# Java 并发编程\n线程是进程中的执行单元...",
      "summary": "介绍线程、锁、并发容器等核心概念",
      "revisionNum": 1,
      "changeSummary": null,
      "createdAt": "2026-07-20T10:00:00"
    }
  ]
}
```

---

#### 2.12 版本详情

- **接口名称**：版本详情
- **请求方法和路径**：`GET /api/knowledge/{nodeId}/revisions/{revId}`
- **接口描述**：获取指定版本的详细内容。若知识点或版本不存在、或版本与节点不匹配，返回 404。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| nodeId | Long | path | 是 | 知识节点 ID |
| revId | Long | path | 是 | 版本 ID |

**请求示例**

```http
GET /api/knowledge/1001/revisions/5001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 5001,
    "nodeId": 1001,
    "userId": 1,
    "title": "Java 并发编程基础（更新版）",
    "contentMd": "# Java 并发编程\n线程是进程中的执行单元...\n## JUC 包\nAtomicXxx、Lock、并发容器等",
    "summary": "新增 JUC 包介绍",
    "revisionNum": 2,
    "changeSummary": null,
    "createdAt": "2026-07-24T11:30:00"
  }
}
```

---

#### 2.13 回滚到指定版本

- **接口名称**：回滚到指定版本
- **请求方法和路径**：`POST /api/knowledge/{nodeId}/revisions/{revId}/rollback`
- **接口描述**：将知识节点内容恢复到指定版本的状态。仅工作区 owner/admin 可执行回滚操作。知识点不存在时返回 404。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| nodeId | Long | path | 是 | 知识节点 ID |
| revId | Long | path | 是 | 目标版本 ID |

**请求示例**

```http
POST /api/knowledge/1001/revisions/5000/rollback
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "回滚成功",
  "data": null
}
```

---

#### 2.14 获取知识图谱

- **接口名称**：获取知识图谱
- **请求方法和路径**：`GET /api/knowledge/relation/graph`
- **接口描述**：获取当前用户在当前工作区下的完整知识图谱，包含节点和边数据，用于前端可视化。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |

**请求示例**

```http
GET /api/knowledge/relation/graph
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "nodes": [
      {
        "id": "1001",
        "label": "Java 并发编程基础",
        "type": "knowledge",
        "importance": 4,
        "masteryLevel": 2,
        "size": 30,
        "color": "#5B8FF9"
      },
      {
        "id": "1002",
        "label": "Spring Boot 启动流程",
        "type": "knowledge",
        "importance": 5,
        "masteryLevel": 0,
        "size": 36,
        "color": "#5AD8A6"
      }
    ],
    "edges": [
      {
        "source": "1001",
        "target": "1002",
        "label": "related",
        "strength": 3
      }
    ]
  }
}
```

---

#### 2.15 添加知识关系

- **接口名称**：添加知识关系
- **请求方法和路径**：`POST /api/knowledge/relation`
- **接口描述**：在两个知识节点之间添加一条关系。关系类型支持 `contains`、`depends`、`related`、`inherits`、`implements`。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| sourceId | Long | body | 是 | 源知识节点 ID |
| targetId | Long | body | 是 | 目标知识节点 ID |
| relationType | String | body | 是 | 关系类型（contains/depends/related/inherits/implements） |
| relationName | String | body | 否 | 关系名称 |
| relationStrength | Integer | body | 否 | 关系强度（1-5） |

**请求示例**

```json
{
  "sourceId": 1001,
  "targetId": 1002,
  "relationType": "related",
  "relationName": "相关",
  "relationStrength": 3
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "添加成功",
  "data": null
}
```

---

#### 2.16 删除知识关系

- **接口名称**：删除知识关系
- **请求方法和路径**：`DELETE /api/knowledge/relation/{id}`
- **接口描述**：根据关系 ID 删除知识关系。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 关系 ID |

**请求示例**

```http
DELETE /api/knowledge/relation/2001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

#### 2.17 自动生成关系

- **接口名称**：自动生成关系
- **请求方法和路径**：`POST /api/knowledge/relation/auto-generate`
- **接口描述**：基于知识节点之间的相似度自动生成知识关系。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |

**请求示例**

```http
POST /api/knowledge/relation/auto-generate
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "自动生成完成",
  "data": null
}
```

---

#### 2.18 推荐关系

- **接口名称**：推荐关系
- **请求方法和路径**：`GET /api/knowledge/relation/recommend/{knowledgeId}`
- **接口描述**：为指定知识节点推荐可能的关系，返回推荐的目标节点、相似度及推荐的关系类型。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| knowledgeId | Long | path | 是 | 知识节点 ID |

**请求示例**

```http
GET /api/knowledge/relation/recommend/1001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "targetKnowledge": {
        "id": 1002,
        "title": "Spring Boot 启动流程",
        "summary": "梳理 SpringApplication.run 的关键步骤",
        "importance": 5,
        "masteryLevel": 0
      },
      "similarity": 0.7821,
      "recommendedType": "related",
      "recommendedTypeName": "相关"
    },
    {
      "targetKnowledge": {
        "id": 1003,
        "title": "JVM 内存模型",
        "summary": "堆、栈、方法区等内存区域",
        "importance": 4,
        "masteryLevel": 1
      },
      "similarity": 0.6543,
      "recommendedType": "depends",
      "recommendedTypeName": "依赖"
    }
  ]
}
```

---

#### 2.19 获取编辑锁

- **接口名称**：获取编辑锁
- **请求方法和路径**：`POST /api/knowledge/{nodeId}/lock`
- **接口描述**：为当前用户获取指定知识节点的编辑锁。同一节点同一时刻仅允许一个用户持有锁。锁默认有效期 15 分钟，支持续期。若已被他人持有则返回 409。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| nodeId | Long | path | 是 | 知识节点 ID |

**请求示例**

```http
POST /api/knowledge/1001/lock
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "获取编辑锁成功",
  "data": {
    "nodeId": 1001,
    "userId": 1,
    "expiresAt": "2026-07-24T11:45:00"
  }
}
```

**冲突响应示例**

```json
{
  "code": 409,
  "message": "bob 正在编辑此知识点，请稍后再试",
  "data": null
}
```

---

#### 2.20 释放编辑锁

- **接口名称**：释放编辑锁
- **请求方法和路径**：`DELETE /api/knowledge/{nodeId}/lock`
- **接口描述**：释放当前用户持有的指定知识节点编辑锁。通常在编辑结束或退出编辑页面时调用。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| nodeId | Long | path | 是 | 知识节点 ID |

**请求示例**

```http
DELETE /api/knowledge/1001/lock
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "释放编辑锁成功",
  "data": null
}
```

---

#### 2.21 查询锁状态

- **接口名称**：查询锁状态
- **请求方法和路径**：`GET /api/knowledge/{nodeId}/lock`
- **接口描述**：查询指定知识节点的编辑锁状态。若无人持有锁，则 data 为 null。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| nodeId | Long | path | 是 | 知识节点 ID |

**请求示例**

```http
GET /api/knowledge/1001/lock
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例（已被持有）**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "nodeId": 1001,
    "userId": 1,
    "userName": "alice",
    "acquiredAt": "2026-07-24T11:30:00",
    "expiresAt": "2026-07-24T11:45:00"
  }
}
```

**响应示例（无人持有）**

```json
{
  "code": 200,
  "message": "该知识点当前无人编辑",
  "data": null
}
```

---

#### 2.22 获取用户的所有标签

- **接口名称**：获取用户的所有标签
- **请求方法和路径**：`GET /api/tags`
- **接口描述**：获取当前登录用户创建的所有标签。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |

**请求示例**

```http
GET /api/tags
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "tagName": "Java",
      "tagColor": "#5B8FF9",
      "createTime": "2026-07-20T10:00:00",
      "deleted": 0
    },
    {
      "id": 2,
      "userId": 1,
      "tagName": "数据库",
      "tagColor": "#5AD8A6",
      "createTime": "2026-07-21T09:00:00",
      "deleted": 0
    }
  ]
}
```

---

#### 2.23 获取全部标签

- **接口名称**：获取全部标签
- **请求方法和路径**：`GET /api/tags/all`
- **接口描述**：获取系统中全部标签，用于排行榜领域筛选。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |

**请求示例**

```http
GET /api/tags/all
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "tagName": "Java",
      "tagColor": "#5B8FF9",
      "createTime": "2026-07-20T10:00:00",
      "deleted": 0
    },
    {
      "id": 3,
      "userId": 2,
      "tagName": "Python",
      "tagColor": "#F6BD16",
      "createTime": "2026-07-22T14:00:00",
      "deleted": 0
    }
  ]
}
```

---

#### 2.24 创建标签

- **接口名称**：创建标签
- **请求方法和路径**：`POST /api/tags`
- **接口描述**：创建一个新标签，标签归属于当前登录用户。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| tagName | String | query | 是 | 标签名称 |
| tagColor | String | query | 否 | 标签颜色（十六进制，如 `#5B8FF9`） |

**请求示例**

```http
POST /api/tags?tagName=算法&tagColor=%23F6BD16
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 4,
    "userId": 1,
    "tagName": "算法",
    "tagColor": "#F6BD16",
    "createTime": "2026-07-24T12:00:00",
    "deleted": 0
  }
}
```

---

#### 2.25 删除标签

- **接口名称**：删除标签
- **请求方法和路径**：`DELETE /api/tags/{id}`
- **接口描述**：删除指定标签。仅标签所属用户可删除。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 标签 ID |

**请求示例**

```http
DELETE /api/tags/4
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "已删除",
  "data": null
}
```

---

#### 2.26 给知识节点添加标签

- **接口名称**：给知识节点添加标签
- **请求方法和路径**：`POST /api/tags/node/{nodeId}/tag/{tagId}`
- **接口描述**：将指定标签关联到知识节点上。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| nodeId | Long | path | 是 | 知识节点 ID |
| tagId | Long | path | 是 | 标签 ID |

**请求示例**

```http
POST /api/tags/node/1001/tag/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "标签已添加",
  "data": null
}
```

---

#### 2.27 移除知识节点的标签

- **接口名称**：移除知识节点的标签
- **请求方法和路径**：`DELETE /api/tags/node/{nodeId}/tag/{tagId}`
- **接口描述**：移除知识节点上已关联的指定标签。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| nodeId | Long | path | 是 | 知识节点 ID |
| tagId | Long | path | 是 | 标签 ID |

**请求示例**

```http
DELETE /api/tags/node/1001/tag/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "标签已移除",
  "data": null
}
```

---

#### 2.28 获取知识节点的所有标签

- **接口名称**：获取知识节点的所有标签
- **请求方法和路径**：`GET /api/tags/node/{nodeId}`
- **接口描述**：获取指定知识节点上关联的所有标签。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| nodeId | Long | path | 是 | 知识节点 ID |

**请求示例**

```http
GET /api/tags/node/1001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "tagName": "Java",
      "tagColor": "#5B8FF9",
      "createTime": "2026-07-20T10:00:00",
      "deleted": 0
    },
    {
      "id": 2,
      "userId": 1,
      "tagName": "数据库",
      "tagColor": "#5AD8A6",
      "createTime": "2026-07-21T09:00:00",
      "deleted": 0
    }
  ]
}
```

#### 2.29 待确认知识点列表

- **接口名称**：待确认知识点列表
- **请求方法和路径**：`GET /api/knowledge/pending`
- **接口描述**：获取当前工作区下所有待确认（AI 抽取后未入库）的知识点列表。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |

**请求示例**

```http
GET /api/knowledge/pending
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 501,
      "userId": 88,
      "workspaceId": 12,
      "rawChatId": 3321,
      "title": "向量检索的Top-K含义",
      "summary": "Top-K 表示从向量库中召回相似度最高的 K 条结果",
      "content": "在 RAG 流程中，Top-K 控制召回阶段返回的知识条数……",
      "status": 0,
      "createTime": "2026-07-29 09:12:30",
      "updateTime": "2026-07-29 09:12:30",
      "deleted": 0
    }
  ]
}
```

---

#### 2.30 批量确认入库

- **接口名称**：批量确认入库
- **请求方法和路径**：`POST /api/knowledge/pending/confirm`
- **接口描述**：将选中的待确认知识点批量迁移到知识库，可选在入库后同时生成复习卡片。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| body | Object | body | 是 | 批量确认请求体，包含 items 列表和 generateCards 开关 |

**请求示例**

```http
POST /api/knowledge/pending/confirm
Authorization: Bearer <token>
Content-Type: application/json

{
  "items": [
    {
      "pendingId": 501,
      "title": "向量检索的Top-K含义",
      "summary": "Top-K 表示从向量库中召回相似度最高的 K 条结果",
      "content": "在 RAG 流程中，Top-K 控制召回阶段返回的知识条数……"
    },
    {
      "pendingId": 502,
      "title": "嵌入模型的维度",
      "summary": "嵌入向量的维度决定了语义表达能力与计算成本",
      "content": "常见的嵌入维度有 768、1024、1536……"
    }
  ],
  "generateCards": true
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "确认入库成功",
  "data": null
}
```

---

#### 2.31 丢弃待确认知识点

- **接口名称**：丢弃待确认知识点
- **请求方法和路径**：`DELETE /api/knowledge/pending/{id}`
- **接口描述**：将指定待确认知识点标记为已丢弃，不再进入知识库。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 待确认知识点记录ID |

**请求示例**

```http
DELETE /api/knowledge/pending/501
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已丢弃",
  "data": null
}
```

---

#### 2.32 手动新增待确认知识点

- **接口名称**：手动新增待确认知识点
- **请求方法和路径**：`POST /api/knowledge/pending/add`
- **接口描述**：在当前工作区的待确认列表中手动添加一条知识点，后续可通过确认接口入库。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| body | Object | body | 是 | 待确认知识点对象，包含 title/summary/content 等字段 |

**请求示例**

```http
POST /api/knowledge/pending/add
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "手动记录的知识点",
  "summary": "一段简要描述",
  "content": "详细内容正文……",
  "rawChatId": null
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 503,
    "userId": 88,
    "workspaceId": 12,
    "rawChatId": null,
    "title": "手动记录的知识点",
    "summary": "一段简要描述",
    "content": "详细内容正文……",
    "status": 0,
    "createTime": "2026-07-29 10:00:00",
    "updateTime": "2026-07-29 10:00:00",
    "deleted": 0
  }
}
```

---

#### 2.33 获取标签树

- **接口名称**：获取标签树（含层级结构）
- **请求方法和路径**：`GET /api/tags/tree`
- **接口描述**：获取当前用户的标签树形结构，包含父子层级关系和每个标签下关联的知识点数量。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |

**请求示例**

```http
GET /api/tags/tree
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "userId": 88,
      "tagName": "前端",
      "tagColor": "#1890ff",
      "parentId": null,
      "knowledgeCount": 12,
      "children": [
        {
          "id": 2,
          "userId": 88,
          "tagName": "React",
          "tagColor": "#61dafb",
          "parentId": 1,
          "knowledgeCount": 8,
          "children": []
        }
      ]
    }
  ]
}
```

---

#### 2.34 更新标签

- **接口名称**：更新标签
- **请求方法和路径**：`PUT /api/tags/{id}`
- **接口描述**：更新指定标签的名称、颜色或父标签。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| id | Long | path | 是 | 待更新的标签ID |
| body | Object | body | 是 | 更新请求体，包含 tagName/tagColor/parentId 字段 |

**请求示例**

```http
PUT /api/tags/2
Authorization: Bearer <token>
Content-Type: application/json

{
  "tagName": "React18",
  "tagColor": "#61dafb",
  "parentId": 1
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 2,
    "userId": 88,
    "tagName": "React18",
    "tagColor": "#61dafb",
    "parentId": 1
  }
}
```

---

#### 2.35 AI建议标签

- **接口名称**：AI建议标签
- **请求方法和路径**：`POST /api/tags/ai-suggest`
- **接口描述**：根据知识点的标题和摘要，由AI推荐合适的标签列表，标识是匹配已有标签还是建议新建。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| body | Object | body | 是 | 请求体，包含 title 和 summary 字段 |

**请求示例**

```http
POST /api/tags/ai-suggest
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "使用 React Hooks 管理状态",
  "summary": "介绍 useState、useReducer 等 Hook 在状态管理中的应用"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "tagName": "React",
      "existingTagId": 2,
      "isNew": false,
      "confidence": 92
    },
    {
      "tagName": "前端状态管理",
      "existingTagId": null,
      "isNew": true,
      "confidence": 78
    }
  ]
}
```

---

### 第3章：AI对话与采集模块

本章涵盖 AI 对话采集、批量导入以及多渠道数据捕捉（文档、笔记）相关接口。所有接口均需要登录认证，JWT 拦截器会从请求头 `Authorization: Bearer <token>` 中解析 `userId`，并从 header 或 token 中解析 `workspaceId`，放入 request attribute 供 Controller 使用。

---

#### 3.1 采集对话

- **接口名称**：采集对话
- **请求方法和路径**：`POST /api/chat/collect`
- **接口描述**：采集 AI 对话内容并提取知识点，写入当前用户的工作区。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| content | String | body | 是 | 对话内容，不能为空 |
| platform | String | body | 否 | 来源平台（如 wechat/chatgpt/other） |
| sourceUrl | String | body | 否 | 原始对话链接 |

**请求示例**

```http
POST /api/chat/collect
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "用户：请解释一下什么是 RAG？\nAI：RAG 是检索增强生成技术，先从知识库中检索相关片段，再交给大模型生成答案……",
  "platform": "chatgpt",
  "sourceUrl": "https://chat.openai.com/c/abc123"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "采集成功",
  "data": null
}
```

---

#### 3.2 批量导入对话

- **接口名称**：批量导入对话
- **请求方法和路径**：`POST /api/chat/batch-import`
- **接口描述**：批量导入 AI 对话并提取知识点，返回实际导入的对话数量。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| chats | Array | body | 是 | 对话列表 |
| chats[].content | String | body | 是 | 对话内容 |
| chats[].platform | String | body | 否 | 来源平台 |
| chats[].sourceUrl | String | body | 否 | 来源 URL |

**请求示例**

```http
POST /api/chat/batch-import
Authorization: Bearer <token>
Content-Type: application/json

{
  "chats": [
    {
      "content": "用户：什么是向量数据库？\nAI：向量数据库是专门存储和检索高维向量的数据库……",
      "platform": "chatgpt",
      "sourceUrl": "https://chat.openai.com/c/001"
    },
    {
      "content": "用户：Spring Boot 如何配置 CORS？\nAI：可以通过 WebMvcConfigurer 或 @CrossOrigin 注解……",
      "platform": "wechat",
      "sourceUrl": "https://mp.weixin.qq.com/s/xyz"
    }
  ]
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "批量导入成功，共导入2条对话",
  "data": "2"
}
```

---

#### 3.3 对话列表

- **接口名称**：对话列表
- **请求方法和路径**：`GET /api/chat/list`
- **接口描述**：分页查询当前用户当前工作区下的对话记录，可按平台和关键词过滤。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| current | Long | query | 否 | 当前页码，默认 1 |
| size | Long | query | 否 | 每页大小，默认 10 |
| platform | String | query | 否 | 来源平台过滤 |
| keyword | String | query | 否 | 关键词模糊匹配对话内容 |

**请求示例**

```http
GET /api/chat/list?current=1&size=10&platform=chatgpt&keyword=RAG
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 5001,
        "userId": 2008,
        "platform": "chatgpt",
        "content": "用户：请解释一下什么是 RAG？\nAI：RAG 是检索增强生成技术……",
        "sourceUrl": "https://chat.openai.com/c/abc123",
        "createTime": "2026-07-23T14:21:08",
        "updateTime": "2026-07-23T14:21:08"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 3.4 对话详情

- **接口名称**：对话详情
- **请求方法和路径**：`GET /api/chat/{id}`
- **接口描述**：根据对话 ID 查询单条对话详情。仅能查询当前用户、当前工作区下的对话。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 对话 ID |

**请求示例**

```http
GET /api/chat/5001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 5001,
    "userId": 2008,
    "platform": "chatgpt",
    "content": "用户：请解释一下什么是 RAG？\nAI：RAG 是检索增强生成技术，先从知识库中检索相关片段，再交给大模型生成答案……",
    "sourceUrl": "https://chat.openai.com/c/abc123",
    "createTime": "2026-07-23T14:21:08",
    "updateTime": "2026-07-23T14:21:08"
  }
}
```

---

#### 3.5 文档捕捉

- **接口名称**：文档捕捉
- **请求方法和路径**：`POST /api/capture/document`
- **接口描述**：上传文档文件，提取文本内容后发送到捕捉通道进行知识沉淀。`userId` 优先从 token 解析，请求参数中的 `userId` 作为兜底。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| file | File | formData | 是 | 上传的文档文件（multipart/form-data） |
| userId | Long | formData | 否 | 用户 ID，可选；未传时从 token 解析 |

**请求示例**

```http
POST /api/capture/document
Authorization: Bearer <token>
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="learning-notes.pdf"
Content-Type: application/pdf

<二进制文件内容>
------WebKitFormBoundary
Content-Disposition: form-data; name="userId"

2008
------WebKitFormBoundary--
```

**响应示例**

```json
{
  "code": 200,
  "message": "文档捕捉成功",
  "data": null
}
```

---

#### 3.6 笔记捕捉

- **接口名称**：笔记捕捉
- **请求方法和路径**：`POST /api/capture/note`
- **接口描述**：提交 Markdown 笔记内容进行知识捕捉，返回捕捉处理结果描述。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| title | String | body | 否 | 笔记标题 |
| content | String | body | 否 | 笔记内容（Markdown 文本） |
| userId | Long | body | 否 | 用户 ID，可选；未传时从 token 解析 |

**请求示例**

```http
POST /api/capture/note
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Spring Boot 启动流程",
  "content": "# Spring Boot 启动流程\n1. SpringApplication.run 入口\n2. 加载 ApplicationContextInitializer\n3. 准备 ApplicationContext ……",
  "userId": 2008
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "笔记《Spring Boot 启动流程》捕捉完成，已提取 3 个知识点"
}
```

---

#### 3.7 会话列表

- **接口名称**：会话列表
- **请求方法和路径**：`GET /api/sessions`
- **接口描述**：分页获取当前用户在当前工作区下的 RAG 对话会话列表。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| current | Integer | query | 否 | 当前页码，默认 1 |
| size | Integer | query | 否 | 每页大小，默认 20 |

**请求示例**

```http
GET /api/sessions?current=1&size=20
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 9001,
        "userId": 88,
        "workspaceId": 12,
        "title": "关于 RAG 的对话",
        "createTime": "2026-07-29 09:00:00",
        "updateTime": "2026-07-29 09:30:00",
        "deleted": 0
      }
    ],
    "total": 1,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 3.8 创建会话

- **接口名称**：创建会话
- **请求方法和路径**：`POST /api/sessions`
- **接口描述**：在当前工作区下创建新的 RAG 对话会话。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| body | Object | body | 是 | 请求体，包含 title 字段（不传时默认为"新对话"） |

**请求示例**

```http
POST /api/sessions
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "向量检索相关问题"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 9002,
    "userId": 88,
    "workspaceId": 12,
    "title": "向量检索相关问题",
    "createTime": "2026-07-29 10:00:00",
    "updateTime": "2026-07-29 10:00:00",
    "deleted": 0
  }
}
```

---

#### 3.8.1 重命名会话

- **接口名称**：重命名会话
- **请求方法和路径**：`PUT /api/sessions/{id}/title`
- **接口描述**：修改当前用户可访问的 RAG 会话标题。标题去除首尾空格后不能为空，最长 100 个字符。
- **是否需要登录认证**：是

**请求示例**

```http
PUT /api/sessions/9002/title
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Spring Boot 自动配置原理"
}
```

**响应说明**

成功时返回更新后的 `ChatSession`。会话不存在或不属于当前用户/工作区时返回 404。

---

#### 3.9 删除会话

- **接口名称**：删除会话
- **请求方法和路径**：`DELETE /api/sessions/{id}`
- **接口描述**：删除指定的 RAG 对话会话（逻辑删除）。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 待删除的会话ID |

**请求示例**

```http
DELETE /api/sessions/9002
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

#### 3.10 消息列表

- **接口名称**：消息列表
- **请求方法和路径**：`GET /api/sessions/{id}/messages`
- **接口描述**：分页获取指定会话下的聊天消息记录，按时间顺序排列。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 会话ID |
| current | Integer | query | 否 | 当前页码，默认 1 |
| size | Integer | query | 否 | 每页大小，默认 100 |

**请求示例**

```http
GET /api/sessions/9001/messages?current=1&size=100
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 70001,
        "sessionId": 9001,
        "workspaceId": 12,
        "role": "user",
        "content": "什么是 Top-K 检索？",
        "createTime": "2026-07-29 09:01:00",
        "deleted": 0
      },
      {
        "id": 70002,
        "sessionId": 9001,
        "workspaceId": 12,
        "role": "assistant",
        "content": "Top-K 检索是指从向量库中召回相似度最高的 K 条结果……",
        "createTime": "2026-07-29 09:01:05",
        "deleted": 0
      }
    ],
    "total": 2,
    "size": 100,
    "current": 1,
    "pages": 1
  }
}
```

---

### 第4章：复习系统模块

复习系统模块基于知识点生成不同类型的复习卡片，并支持间隔重复（Spaced Repetition）复习流程，包括卡片生成、查询、答题提交、恢复、质量反馈、连续复习天数与准确率统计等能力。所有接口均需要登录认证。

---

#### 4.1 生成复习卡片

- **接口名称**：生成复习卡片
- **请求方法和路径**：`POST /api/review/generate`
- **接口描述**：基于指定知识点生成一张复习卡片，支持选择题、填空题、简答题、判断题等类型。生成类型不传时默认 `auto`。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| nodeId | Long | body | 是 | 知识点 ID |
| cardType | String | body | 否 | 卡片类型（choice/fill/essay/judge） |
| generationType | String | body | 否 | 生成类型（auto/manual），默认 auto |

**请求示例**

```http
POST /api/review/generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "nodeId": 8801,
  "cardType": "choice",
  "generationType": "auto"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 12001,
    "nodeId": 8801,
    "nodeTitle": "RAG 检索增强生成",
    "nodeSummary": "RAG 通过检索知识库片段辅助大模型生成答案",
    "question": "下列关于 RAG 的描述，哪一项是正确的？\nA. RAG 不需要检索\nB. RAG 先检索后生成\nC. RAG 仅依赖大模型参数\nD. RAG 无法引用来源",
    "answer": "B",
    "cardType": "choice",
    "difficulty": 3,
    "reviewCount": 0,
    "correctCount": 0,
    "incorrectCount": 0,
    "masteryLevel": 0,
    "memoryStrength": 0.5,
    "lastReviewTime": null,
    "nextReviewTime": "2026-07-24 18:00:00",
    "status": 0,
    "aiGenerated": "true",
    "createTime": "2026-07-24 10:00:00",
    "nodeReviewCount": 0,
    "nodeMasteryLevel": 0,
    "isRestored": 0
  }
}
```

---

#### 4.2 获取今日复习卡片

- **接口名称**：获取今日复习卡片
- **请求方法和路径**：`GET /api/review/today`
- **接口描述**：获取当前用户当前工作区下今日需要复习的全部卡片，可按时间或难度排序。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| sortBy | String | query | 否 | 排序方式：`time`（按下次复习时间升序）/ `difficulty`（按难度降序），不传则保持默认顺序 |

**请求示例**

```http
GET /api/review/today?sortBy=difficulty
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 12001,
      "nodeId": 8801,
      "nodeTitle": "RAG 检索增强生成",
      "nodeSummary": "RAG 通过检索知识库片段辅助大模型生成答案",
      "question": "下列关于 RAG 的描述，哪一项是正确的？",
      "answer": "B",
      "cardType": "choice",
      "difficulty": 4,
      "reviewCount": 2,
      "correctCount": 1,
      "incorrectCount": 1,
      "masteryLevel": 2,
      "memoryStrength": 0.55,
      "lastReviewTime": "2026-07-23 19:30:00",
      "nextReviewTime": "2026-07-24 18:00:00",
      "status": 0,
      "aiGenerated": "true",
      "createTime": "2026-07-20 10:00:00",
      "nodeReviewCount": 2,
      "nodeMasteryLevel": 2,
      "isRestored": 0
    }
  ]
}
```

---

#### 4.3 提交复习结果

- **接口名称**：提交复习结果
- **请求方法和路径**：`POST /api/review/submit`
- **接口描述**：提交用户对某张卡片的答题结果，系统会根据答题情况更新卡片的复习次数、记忆强度、下次复习时间等。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| cardId | Long | body | 是 | 卡片 ID |
| userAnswer | String | body | 否 | 用户提交的答案 |
| duration | Integer | body | 否 | 答题耗时（秒） |

**请求示例**

```http
POST /api/review/submit
Authorization: Bearer <token>
Content-Type: application/json

{
  "cardId": 12001,
  "userAnswer": "B",
  "duration": 25
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "isCorrect": true,
    "correctAnswer": "B",
    "explanation": "RAG 的核心流程是先从外部知识库检索相关片段，再将其作为上下文输入大模型生成答案，因此选项 B 正确。",
    "message": "回答正确，记忆强度已提升"
  }
}
```

---

#### 4.4 根据节点ID获取复习卡片

- **接口名称**：根据节点ID获取复习卡片
- **请求方法和路径**：`GET /api/review/node/{nodeId}`
- **接口描述**：查询指定知识点下当前用户、当前工作区的全部复习卡片。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| nodeId | Long | path | 是 | 知识点 ID |

**请求示例**

```http
GET /api/review/node/8801
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 12001,
      "nodeId": 8801,
      "nodeTitle": "RAG 检索增强生成",
      "nodeSummary": "RAG 通过检索知识库片段辅助大模型生成答案",
      "question": "下列关于 RAG 的描述，哪一项是正确的？",
      "answer": "B",
      "cardType": "choice",
      "difficulty": 4,
      "reviewCount": 2,
      "correctCount": 1,
      "incorrectCount": 1,
      "masteryLevel": 2,
      "memoryStrength": 0.55,
      "lastReviewTime": "2026-07-23 19:30:00",
      "nextReviewTime": "2026-07-24 18:00:00",
      "status": 0,
      "aiGenerated": "true",
      "createTime": "2026-07-20 10:00:00",
      "nodeReviewCount": 2,
      "nodeMasteryLevel": 2,
      "isRestored": 0
    }
  ]
}
```

---

#### 4.5 删除复习卡片

- **接口名称**：删除复习卡片
- **请求方法和路径**：`DELETE /api/review/{id}`
- **接口描述**：逻辑删除指定 ID 的复习卡片。仅卡片所属用户可删除。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 卡片 ID |

**请求示例**

```http
DELETE /api/review/12001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

#### 4.6 删除所有复习卡片

- **接口名称**：删除所有复习卡片
- **请求方法和路径**：`DELETE /api/review/all`
- **接口描述**：逻辑删除当前用户、当前工作区下的所有复习卡片。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
DELETE /api/review/all
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

#### 4.7 为所有节点生成复习卡片

- **接口名称**：为所有节点生成复习卡片
- **请求方法和路径**：`POST /api/review/generate-all`
- **接口描述**：为当前用户、当前工作区下的所有知识点批量生成练习卡片，返回实际生成的卡片数量。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
POST /api/review/generate-all
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功生成12张练习卡片",
  "data": null
}
```

---

#### 4.8 恢复复习卡片

- **接口名称**：恢复复习卡片
- **请求方法和路径**：`POST /api/review/restore`
- **接口描述**：恢复当前用户、当前工作区下被逻辑删除的复习卡片，返回实际恢复的卡片数量。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
POST /api/review/restore
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 8
}
```

---

#### 4.9 更新缺失答案的复习卡片

- **接口名称**：更新缺失答案的复习卡片
- **请求方法和路径**：`POST /api/review/update-answers`
- **接口描述**：扫描全库复习卡片，为 `answer` 字段缺失的卡片补充正确答案。属于维护性接口，不区分用户。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
POST /api/review/update-answers
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功更新缺失答案的复习卡片",
  "data": null
}
```

---

#### 4.10 获取连续复习天数

- **接口名称**：获取连续复习天数
- **请求方法和路径**：`GET /api/review/streak-days`
- **接口描述**：计算当前用户、当前工作区的连续复习天数（streak），用于激励展示。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
GET /api/review/streak-days
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 7
}
```

---

#### 4.11 提交质量反馈

- **接口名称**：提交质量反馈
- **请求方法和路径**：`POST /api/review/quality-feedback`
- **接口描述**：对某张复习卡片的质量进行评分与反馈，用于后续优化卡片生成策略。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| cardId | Long | body | 是 | 卡片 ID |
| rating | Integer | body | 是 | 评分（1-5） |
| comment | String | body | 否 | 反馈内容 |

**请求示例**

```http
POST /api/review/quality-feedback
Authorization: Bearer <token>
Content-Type: application/json

{
  "cardId": 12001,
  "rating": 4,
  "comment": "题目表述清晰，但选项 D 有歧义"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "感谢您的反馈！",
  "data": null
}
```

---

#### 4.12 获取用户准确率

- **接口名称**：获取用户准确率
- **请求方法和路径**：`GET /api/review/accuracy`
- **接口描述**：统计当前用户、当前工作区下所有复习卡片的答题准确率，返回整数百分比。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
GET /api/review/accuracy
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 78
}
```

---

#### 4.13 获取工作区题目池列表

- **接口名称**：获取工作区题目池列表
- **请求方法和路径**：`GET /api/review/pool`
- **接口描述**：获取指定工作区下的复习题目池列表，包含社区标签（参与人数热度）和当前用户的加入状态。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| workspaceId | Long | query | 是 | 工作区ID |

**请求示例**

```http
GET /api/review/pool?workspaceId=12
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 2001,
      "nodeId": 501,
      "nodeTitle": "向量检索的Top-K含义",
      "questionPreview": "在 RAG 中，Top-K 检索的作用是什么？",
      "cardType": "essay",
      "difficulty": 3,
      "generationType": "auto",
      "createUserId": 88,
      "createTime": "2026-07-29 09:12:30",
      "memberCount": 5,
      "communityLabel": "green",
      "communityText": "热门",
      "isJoined": true,
      "userCardId": 8001
    }
  ]
}
```

---

#### 4.14 获取池子题目详情

- **接口名称**：获取池子题目详情
- **请求方法和路径**：`GET /api/review/pool/{poolId}`
- **接口描述**：根据池子题目ID获取题目完整详情，包括题目内容、答案、社区标签和当前用户加入状态。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| poolId | Long | path | 是 | 池子题目ID |

**请求示例**

```http
GET /api/review/pool/2001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 2001,
    "nodeId": 501,
    "nodeTitle": "向量检索的Top-K含义",
    "questionPreview": "在 RAG 中，Top-K 检索的作用是什么？",
    "cardType": "essay",
    "difficulty": 3,
    "generationType": "auto",
    "createUserId": 88,
    "createTime": "2026-07-29 09:12:30",
    "memberCount": 5,
    "communityLabel": "green",
    "communityText": "热门",
    "isJoined": true,
    "userCardId": 8001
  }
}
```

---

#### 4.15 加入复习

- **接口名称**：加入复习
- **请求方法和路径**：`POST /api/review/pool/{poolId}/join`
- **接口描述**：将指定池子题目复制生成当前用户的个人复习卡片副本，开始独立的复习进度追踪。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| poolId | Long | path | 是 | 池子题目ID |

**请求示例**

```http
POST /api/review/pool/2001/join
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 8002,
    "poolId": 2001,
    "userId": 88,
    "workspaceId": 12,
    "reviewCount": 0,
    "correctCount": 0,
    "incorrectCount": 0,
    "masteryLevel": 0,
    "memoryStrength": 0.0,
    "lastReviewTime": null,
    "nextReviewTime": "2026-07-30 10:00:00",
    "status": 0,
    "isArchived": 0,
    "createTime": "2026-07-29 10:00:00",
    "updateTime": "2026-07-29 10:00:00"
  }
}
```

---

#### 4.16 删除池子题目

- **接口名称**：删除池子题目
- **请求方法和路径**：`DELETE /api/review/pool/{poolId}`
- **接口描述**：删除指定池子题目，仅工作区 owner 或题目创建者可执行。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| poolId | Long | path | 是 | 池子题目ID |

**请求示例**

```http
DELETE /api/review/pool/2001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

#### 4.17 编辑池子题目

- **接口名称**：编辑池子题目
- **请求方法和路径**：`PUT /api/review/pool/{poolId}`
- **接口描述**：编辑指定池子题目的题目内容和答案，仅工作区 owner 或题目创建者可执行。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| poolId | Long | path | 是 | 池子题目ID |
| body | Object | body | 是 | 编辑请求体，包含 question 和 answer 字段 |

**请求示例**

```http
PUT /api/review/pool/2001
Authorization: Bearer <token>
Content-Type: application/json

{
  "question": "请说明 RAG 中 Top-K 检索的作用，并举例说明 K 值大小的影响。",
  "answer": "Top-K 检索用于从向量库中召回相似度最高的 K 条知识……"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 2001,
    "nodeId": 501,
    "workspaceId": 12,
    "question": "请说明 RAG 中 Top-K 检索的作用，并举例说明 K 值大小的影响。",
    "answer": "Top-K 检索用于从向量库中召回相似度最高的 K 条知识……",
    "cardType": "essay",
    "difficulty": 3,
    "generationType": "auto",
    "createUserId": 88,
    "createTime": "2026-07-29 09:12:30",
    "updateTime": "2026-07-29 10:30:00",
    "deleted": 0
  }
}
```

---

### 第5章：AI问答与报告模块

本章涵盖基于知识库的 RAG 智能问答、学习报告生成与查询、DeerFlow 深度研究（学习报告 / 学习路径 / 知识盲区分析的异步任务）以及研究历史管理，最后提供一个 AI 知识点提取测试接口。除健康检查外，所有接口均需要登录认证。

---

#### 5.1 知识问答

- **接口名称**：知识问答
- **请求方法和路径**：`POST /api/rag/answer`
- **接口描述**：基于当前用户的知识库（向量检索）回答用户问题。系统会优先使用当前用户的 API Key 调用大模型，支持返回引用的知识片段。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| question | String | body | 是 | 用户问题 |
| topK | Integer | body | 否 | 检索的知识数量，默认 3 |
| includeReferences | Boolean | body | 否 | 是否返回详细引用，默认 true |

**请求示例**

```http
POST /api/rag/answer
Authorization: Bearer <token>
Content-Type: application/json

{
  "question": "RAG 的核心流程是什么？",
  "topK": 3,
  "includeReferences": true
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "answer": "RAG 的核心流程是：1) 将用户问题向量化；2) 从向量数据库检索最相关的知识片段；3) 将检索到的片段作为上下文拼入 Prompt；4) 由大模型基于上下文生成最终答案，并可在响应中附带引用来源。",
    "references": [
      {
        "knowledgeId": 8801,
        "title": "RAG 检索增强生成",
        "summary": "RAG 通过检索知识库片段辅助大模型生成答案",
        "similarity": 0.91,
        "matchedContent": "RAG 是检索增强生成技术，先从知识库中检索相关片段，再交给大模型生成答案……"
      },
      {
        "knowledgeId": 8802,
        "title": "向量数据库选型",
        "summary": "对比 Milvus、Qdrant、PgVector 等向量库",
        "similarity": 0.78,
        "matchedContent": "向量数据库用于存储文本的 embedding，支持近似最近邻检索……"
      }
    ],
    "retrievalTime": 142,
    "generationTime": 1836
  }
}
```

---

#### 5.2 生成向量

- **接口名称**：生成向量
- **请求方法和路径**：`POST /api/rag/generate-vectors`
- **接口描述**：为当前用户的所有知识节点批量生成向量并写入向量库，任务异步执行。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
POST /api/rag/generate-vectors
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "向量生成任务已提交",
  "data": null
}
```

---

#### 5.3 生成学习报告

- **接口名称**：生成学习报告
- **请求方法和路径**：`POST /api/report/generate`
- **接口描述**：基于用户学习数据同步生成学习报告，返回报告正文内容。报告主题和学习天数可自定义。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| topic | String | body | 否 | 报告主题 |
| days | Integer | body | 否 | 学习天数 |

**请求示例**

```http
POST /api/report/generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "topic": "近一周 RAG 学习总结",
  "days": 7
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "# 学习报告：近一周 RAG 学习总结\n\n## 一、学习概览\n本周共采集 12 条对话，新增 8 个知识点，完成 23 张复习卡片……\n\n## 二、知识掌握情况\n- RAG 检索增强生成：掌握度 80%\n- 向量数据库选型：掌握度 65%……\n\n## 三、建议\n建议加强向量检索召回率相关内容的学习……"
}
```

---

#### 5.4 异步生成学习报告

- **接口名称**：异步生成学习报告
- **请求方法和路径**：`POST /api/report/generate-async`
- **接口描述**：异步生成学习报告，立即返回任务 ID，可通过任务查询接口轮询进度。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| topic | String | body | 否 | 报告主题 |
| days | Integer | body | 否 | 学习天数 |

**请求示例**

```http
POST /api/report/generate-async
Authorization: Bearer <token>
Content-Type: application/json

{
  "topic": "近一周 RAG 学习总结",
  "days": 7
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "任务已创建，请使用任务ID查询进度",
  "data": {
    "taskId": "rpt-20260724-0001",
    "status": "PENDING",
    "taskType": "LEARNING_REPORT",
    "createTime": "2026-07-24 10:00:00",
    "completeTime": null,
    "progress": 0,
    "result": null,
    "errorMessage": null
  }
}
```

---

#### 5.5 获取报告列表

- **接口名称**：获取报告列表
- **请求方法和路径**：`GET /api/report/list`
- **接口描述**：分页查询当前用户、当前工作区下的学习报告列表。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| current | Integer | query | 否 | 当前页码，默认 1 |
| size | Integer | query | 否 | 每页大小，默认 10 |

**请求示例**

```http
GET /api/report/list?current=1&size=10
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 3001,
        "userId": 2008,
        "workspaceId": 1001,
        "topic": "近一周 RAG 学习总结",
        "content": "# 学习报告：近一周 RAG 学习总结\n……",
        "days": 7,
        "createTime": "2026-07-24 10:05:12",
        "deleted": 0
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 5.6 获取报告详情

- **接口名称**：获取报告详情
- **请求方法和路径**：`GET /api/report/detail/{id}`
- **接口描述**：根据报告 ID 获取学习报告的详细内容。仅能查询当前用户、当前工作区下的报告。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 报告 ID |

**请求示例**

```http
GET /api/report/detail/3001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 3001,
    "userId": 2008,
    "workspaceId": 1001,
    "topic": "近一周 RAG 学习总结",
    "content": "# 学习报告：近一周 RAG 学习总结\n\n## 一、学习概览\n本周共采集 12 条对话……",
    "days": 7,
    "createTime": "2026-07-24 10:05:12",
    "deleted": 0
  }
}
```

---

#### 5.7 删除报告

- **接口名称**：删除报告
- **请求方法和路径**：`DELETE /api/report/delete/{id}`
- **接口描述**：逻辑删除指定的学习报告。仅报告所属用户可删除，否则返回失败。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 报告 ID |

**请求示例**

```http
DELETE /api/report/delete/3001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

#### 5.8 异步生成学习报告（DeerFlow）

- **接口名称**：异步生成学习报告（DeerFlow 深度研究）
- **请求方法和路径**：`POST /api/deerflow/research/learning-report-async`
- **接口描述**：基于 DeerFlow 异步生成深度学习报告，返回任务 ID。系统会优先使用当前用户的 API Key 调用大模型。`topic` 不传时默认 `综合学习分析`，`depth` 不传时默认 `medium`。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| topic | String | body | 否 | 研究主题，默认 `综合学习分析` |
| depth | String | body | 否 | 研究深度（beginner/intermediate/advanced），默认 `medium` |
| learningData | String | body | 否 | 学习数据 |
| goal | String | body | 否 | 学习目标 |
| currentLevel | String | body | 否 | 当前水平 |
| targetLevel | String | body | 否 | 目标水平 |
| userKnowledge | Array<String> | body | 否 | 用户已有知识列表 |

**请求示例**

```http
POST /api/deerflow/research/learning-report-async
Authorization: Bearer <token>
Content-Type: application/json

{
  "topic": "RAG 系统深度学习报告",
  "depth": "advanced",
  "learningData": "近 30 天学习记录摘要……",
  "goal": "能够独立设计并实现 RAG 系统"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "deer-rpt-20260724-0001",
    "status": "PENDING",
    "taskType": "LEARNING_REPORT",
    "createTime": "2026-07-24 10:10:00",
    "completeTime": null,
    "progress": 0,
    "result": null,
    "errorMessage": null
  }
}
```

---

#### 5.9 异步生成学习路径

- **接口名称**：异步生成学习路径
- **请求方法和路径**：`POST /api/deerflow/research/learning-path-async`
- **接口描述**：基于 DeerFlow 异步生成个性化学习路径，返回任务 ID。需指定主题、当前水平和目标水平。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| topic | String | body | 否 | 研究主题，默认 `综合学习分析` |
| currentLevel | String | body | 否 | 当前水平 |
| targetLevel | String | body | 否 | 目标水平 |
| learningData | String | body | 否 | 学习数据 |
| goal | String | body | 否 | 学习目标 |
| depth | String | body | 否 | 研究深度 |
| userKnowledge | Array<String> | body | 否 | 用户已有知识列表 |

**请求示例**

```http
POST /api/deerflow/research/learning-path-async
Authorization: Bearer <token>
Content-Type: application/json

{
  "topic": "从零搭建 RAG 系统",
  "currentLevel": "beginner",
  "targetLevel": "advanced"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "deer-path-20260724-0002",
    "status": "PENDING",
    "taskType": "LEARNING_PATH",
    "createTime": "2026-07-24 10:12:00",
    "completeTime": null,
    "progress": 0,
    "result": null,
    "errorMessage": null
  }
}
```

---

#### 5.10 异步分析知识盲区

- **接口名称**：异步分析知识盲区
- **请求方法和路径**：`POST /api/deerflow/research/knowledge-blind-spot-async`
- **接口描述**：基于 DeerFlow 异步分析用户在指定主题下的知识盲区，返回任务 ID。需提供用户已掌握的知识点列表。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| topic | String | body | 否 | 研究主题，默认 `综合学习分析` |
| userKnowledge | Array<String> | body | 否 | 用户已有知识列表 |
| learningData | String | body | 否 | 学习数据 |
| goal | String | body | 否 | 学习目标 |
| depth | String | body | 否 | 研究深度 |
| currentLevel | String | body | 否 | 当前水平 |
| targetLevel | String | body | 否 | 目标水平 |

**请求示例**

```http
POST /api/deerflow/research/knowledge-blind-spot-async
Authorization: Bearer <token>
Content-Type: application/json

{
  "topic": "RAG 系统知识盲区分析",
  "userKnowledge": [
    "RAG 基本概念",
    "向量数据库选型",
    "Embedding 模型"
  ]
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "deer-gap-20260724-0003",
    "status": "PENDING",
    "taskType": "KNOWLEDGE_BLIND_SPOT",
    "createTime": "2026-07-24 10:15:00",
    "completeTime": null,
    "progress": 0,
    "result": null,
    "errorMessage": null
  }
}
```

---

#### 5.11 查询研究任务状态

- **接口名称**：查询研究任务状态
- **请求方法和路径**：`GET /api/deerflow/research/status/{taskNumber}`
- **接口描述**：根据任务编号查询异步研究任务的状态、进度与结果。任务不存在时返回错误。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| taskNumber | String | path | 是 | 任务编号 |

**请求示例**

```http
GET /api/deerflow/research/status/deer-rpt-20260724-0001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "deer-rpt-20260724-0001",
    "status": "COMPLETED",
    "taskType": "LEARNING_REPORT",
    "createTime": "2026-07-24 10:10:00",
    "completeTime": "2026-07-24 10:13:25",
    "progress": 100,
    "result": {
      "report": "# RAG 系统深度学习报告\n\n## 一、学习概览\n……\n## 二、知识掌握情况\n……\n## 三、改进建议\n……"
    },
    "errorMessage": null
  }
}
```

---

#### 5.12 健康检查

- **接口名称**：DeerFlow 研究服务健康检查
- **请求方法和路径**：`GET /api/deerflow/research/health`
- **接口描述**：检查 DeerFlow 研究服务是否可用，返回服务健康状态。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
GET /api/deerflow/research/health
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "健康检查完成",
  "data": {
    "deerflow_healthy": true,
    "message": "DeerFlow研究服务正常"
  }
}
```

---

#### 5.13 保存研究历史

- **接口名称**：保存研究历史
- **请求方法和路径**：`POST /api/deerflow/research/history`
- **接口描述**：保存一条 AI 研究历史记录。未登录时不会保存，但接口仍返回成功（data 为 null）。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| type | String | body | 否 | 研究类型（LEARNING_REPORT/LEARNING_PATH/KNOWLEDGE_BLIND_SPOT） |
| topic | String | body | 否 | 研究主题 |
| content | String | body | 否 | 研究内容/结果 |
| currentLevel | String | body | 否 | 当前水平 |
| targetLevel | String | body | 否 | 目标水平 |
| depth | String | body | 否 | 研究深度 |
| userKnowledge | Array<String> | body | 否 | 用户已有知识列表 |
| knowledgeCount | Integer | body | 否 | 涉及知识点数量 |

**请求示例**

```http
POST /api/deerflow/research/history
Authorization: Bearer <token>
Content-Type: application/json

{
  "type": "LEARNING_REPORT",
  "topic": "RAG 系统深度学习报告",
  "content": "# RAG 系统深度学习报告\n……",
  "depth": "advanced",
  "knowledgeCount": 12
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "保存成功",
  "data": {
    "id": 7001,
    "userId": 2008,
    "workspaceId": 1001,
    "type": "LEARNING_REPORT",
    "topic": "RAG 系统深度学习报告",
    "content": "# RAG 系统深度学习报告\n……",
    "currentLevel": null,
    "targetLevel": null,
    "depth": "advanced",
    "userKnowledge": null,
    "knowledgeCount": 12,
    "createTime": "2026-07-24 10:14:00",
    "deleted": 0
  }
}
```

---

#### 5.14 获取研究历史列表

- **接口名称**：获取研究历史列表
- **请求方法和路径**：`GET /api/deerflow/research/history`
- **接口描述**：分页查询当前用户、当前工作区下的研究历史记录，可按研究类型过滤。未登录时返回空列表。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| current | Integer | query | 否 | 当前页码，默认 1 |
| size | Integer | query | 否 | 每页大小，默认 10 |
| type | String | query | 否 | 研究类型过滤（LEARNING_REPORT/LEARNING_PATH/KNOWLEDGE_BLIND_SPOT） |

**请求示例**

```http
GET /api/deerflow/research/history?current=1&size=10&type=LEARNING_REPORT
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "records": [
      {
        "id": 7001,
        "userId": 2008,
        "workspaceId": 1001,
        "type": "LEARNING_REPORT",
        "topic": "RAG 系统深度学习报告",
        "content": "# RAG 系统深度学习报告\n……",
        "currentLevel": null,
        "targetLevel": null,
        "depth": "advanced",
        "userKnowledge": null,
        "knowledgeCount": 12,
        "createTime": "2026-07-24 10:14:00",
        "deleted": 0
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

---

#### 5.15 删除研究历史

- **接口名称**：删除研究历史
- **请求方法和路径**：`DELETE /api/deerflow/research/history/{id}`
- **接口描述**：逻辑删除指定的研究历史记录。仅记录所属用户可删除，未登录时返回未登录错误。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 研究历史 ID |

**请求示例**

```http
DELETE /api/deerflow/research/history/7001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

#### 5.16 AI测试-知识点提取

- **接口名称**：AI测试-知识点提取
- **请求方法和路径**：`POST /api/ai/test-extract`
- **接口描述**：测试 AI 知识点提取能力。传入一段文本内容，返回 AI 抽取出的知识点列表及数量。该接口为测试用途，响应体不使用统一 `Result` 包装，直接返回包含 `success`、`data`、`count` 的 Map。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| content | String | body | 是 | 待提取知识点的文本内容 |

**请求示例**

```http
POST /api/ai/test-extract
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "RAG 是检索增强生成技术。其核心流程包括：1) 文本分块；2) 向量化；3) 存入向量数据库；4) 检索相关片段；5) 拼接 Prompt 后由大模型生成答案。常见的向量数据库有 Milvus、Qdrant、PgVector 等。"
}
```

**响应示例**

```json
{
  "success": true,
  "data": [
    {
      "title": "RAG 检索增强生成",
      "summary": "RAG 是一种先检索后生成的大模型应用范式",
      "content": "RAG 是检索增强生成技术，核心流程包括文本分块、向量化、存入向量数据库、检索相关片段、拼接 Prompt 后由大模型生成答案。",
      "keywords": ["RAG", "检索增强生成", "大模型"]
    },
    {
      "title": "向量数据库",
      "summary": "用于存储和检索高维向量的数据库",
      "content": "常见的向量数据库有 Milvus、Qdrant、PgVector 等，用于存储文本 embedding 并支持近似最近邻检索。",
      "keywords": ["向量数据库", "Milvus", "Qdrant", "PgVector"]
    }
  ],
  "count": 2
}
```

#### 5.17 流式知识问答

- **接口名称**：流式知识问答（SSE）
- **请求方法和路径**：`POST /api/rag/answer/stream`
- **接口描述**：基于知识库流式回答用户问题，采用 SSE（Server-Sent Events）逐字输出。响应类型为 `text/event-stream`，前端需根据事件名（`token`/`references`/`metrics`/`done`/`error`）分发处理。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| Accept | String | header | 是 | `text/event-stream` |
| body | Object | body | 是 | RAG 问答请求体，包含 question/topK/includeReferences/sessionId 字段 |

**请求示例**

```http
POST /api/rag/answer/stream
Authorization: Bearer <token>
Content-Type: application/json
Accept: text/event-stream

{
  "question": "什么是向量检索的 Top-K？",
  "topK": 3,
  "includeReferences": true,
  "sessionId": 9001
}
```

**响应示例**

响应类型：`text/event-stream`，以下为 SSE 事件流示例（每行以空行分隔）：

```
event:token
data:"在"

event:token
data:"RAG "

event:token
data:"中，Top-K "

event:references
data:"[{\"nodeId\":501,\"title\":\"向量检索的Top-K含义\",\"score\":0.91}]"

event:metrics
data:"{\"retrievalTime\":120,\"generationTime\":856}"

event:done
data:"completed"
```

事件说明：

| 事件名 | data 内容 | 说明 |
|--------|-----------|------|
| token | 字符串片段（JSON 字符串） | AI 生成的文本片段，前端需追加显示 |
| references | JSON 数组字符串 | 检索到的知识引用列表 |
| metrics | JSON 对象字符串 | 性能指标，含 retrievalTime/generationTime |
| done | "completed" | 流式生成完成 |
| error | 错误信息字符串 | 生成失败 |

---

#### 5.18 可用服务商列表

- **接口名称**：可用服务商列表
- **请求方法和路径**：`GET /api/ai/providers`
- **接口描述**：获取系统中已启用的 AI 服务商列表，供前端用户在选择模型时使用。此接口为公开接口，无需登录认证。
- **是否需要登录认证**：否

**请求参数**

无

**请求示例**

```http
GET /api/ai/providers
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "code": "openai",
      "name": "OpenAI",
      "baseUrl": "https://api.openai.com/v1",
      "apiType": "openai_compatible",
      "logoUrl": "https://example.com/logo/openai.png",
      "isEnabled": 1,
      "sortOrder": 1,
      "createTime": "2026-07-01 00:00:00",
      "updateTime": "2026-07-01 00:00:00"
    },
    {
      "id": 2,
      "code": "siliconflow",
      "name": "SiliconFlow",
      "baseUrl": "https://api.siliconflow.cn/v1",
      "apiType": "openai_compatible",
      "logoUrl": "https://example.com/logo/sf.png",
      "isEnabled": 1,
      "sortOrder": 2,
      "createTime": "2026-07-01 00:00:00",
      "updateTime": "2026-07-01 00:00:00"
    }
  ]
}
```

---

#### 5.19 服务商下的可用模型列表

- **接口名称**：服务商下的可用模型列表
- **请求方法和路径**：`GET /api/ai/providers/{providerId}/models`
- **接口描述**：获取指定 AI 服务商下已启用的模型列表。此接口为公开接口，无需登录认证。
- **是否需要登录认证**：否

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| providerId | Long | path | 是 | 服务商ID |

**请求示例**

```http
GET /api/ai/providers/1/models
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 10,
      "providerId": 1,
      "modelName": "gpt-4o",
      "displayName": "GPT-4o",
      "isEnabled": 1,
      "supportedScenarios": "[\"chat\",\"embedding\"]",
      "sortOrder": 1,
      "createTime": "2026-07-01 00:00:00",
      "updateTime": "2026-07-01 00:00:00"
    },
    {
      "id": 11,
      "providerId": 1,
      "modelName": "gpt-4o-mini",
      "displayName": "GPT-4o mini",
      "isEnabled": 1,
      "supportedScenarios": "[\"chat\"]",
      "sortOrder": 2,
      "createTime": "2026-07-01 00:00:00",
      "updateTime": "2026-07-01 00:00:00"
    }
  ]
}
```

---

### 第6章：研究项目模块

#### 6.1 创建研究项目

- **接口名称**：创建研究项目
- **请求方法和路径**：`POST /api/research/projects`
- **接口描述**：创建一个新的 AI 研究项目，进入 DRAFT 草稿状态。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| body | Object | body | 是 | 创建研究项目请求体 |

**请求示例**

```http
POST /api/research/projects
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "大语言模型在医疗领域的应用研究",
  "goal": "调研 LLM 在医疗诊断、病历生成等场景的应用现状与挑战",
  "workspaceId": null
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 1001,
    "userId": 88,
    "workspaceId": null,
    "title": "大语言模型在医疗领域的应用研究",
    "goal": "调研 LLM 在医疗诊断、病历生成等场景的应用现状与挑战",
    "status": "DRAFT",
    "complexity": null,
    "agentWorkflow": null,
    "maxIterations": null,
    "currentIteration": null,
    "planJson": null,
    "resultSummary": null,
    "resultReport": null,
    "contextSnapshot": null,
    "idempotencyKey": null,
    "version": 0,
    "startedAt": null,
    "pausedAt": null,
    "completedAt": null,
    "createTime": "2026-07-29 10:00:00",
    "updateTime": "2026-07-29 10:00:00",
    "deleted": 0
  }
}
```

---

#### 6.2 查询研究项目列表

- **接口名称**：查询研究项目列表
- **请求方法和路径**：`GET /api/research/projects`
- **接口描述**：分页查询当前用户的研究项目列表，支持按状态过滤和标题关键词搜索。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| current | int | query | 否 | 当前页码，默认 1 |
| size | int | query | 否 | 每页大小，默认 10 |
| status | String | query | 否 | 项目状态过滤：DRAFT/PLANNING/RESEARCHING/REVIEWING/SYNTHESIZING/COMPLETED/ARCHIVED/FAILED/PAUSED |
| keyword | String | query | 否 | 标题关键词 |

**请求示例**

```http
GET /api/research/projects?current=1&size=10&status=RESEARCHING&keyword=LLM
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1001,
        "userId": 88,
        "workspaceId": null,
        "title": "大语言模型在医疗领域的应用研究",
        "goal": "调研 LLM 在医疗诊断、病历生成等场景的应用现状与挑战",
        "status": "RESEARCHING",
        "complexity": "DEEP",
        "agentWorkflow": "PLANNER,RESEARCHER,REVIEWER,SYNTHESIZER",
        "maxIterations": 5,
        "currentIteration": 2,
        "startedAt": "2026-07-29 10:05:00",
        "createTime": "2026-07-29 10:00:00",
        "updateTime": "2026-07-29 10:30:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 6.3 查询研究项目详情

- **接口名称**：查询研究项目详情
- **请求方法和路径**：`GET /api/research/projects/{id}`
- **接口描述**：根据项目 ID 查询单个研究项目的详细信息。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 项目ID |

**请求示例**

```http
GET /api/research/projects/1001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "userId": 88,
    "workspaceId": null,
    "title": "大语言模型在医疗领域的应用研究",
    "goal": "调研 LLM 在医疗诊断、病历生成等场景的应用现状与挑战",
    "status": "RESEARCHING",
    "complexity": "DEEP",
    "agentWorkflow": "PLANNER,RESEARCHER,REVIEWER,SYNTHESIZER",
    "maxIterations": 5,
    "currentIteration": 2,
    "planJson": "{\"tasks\":[...]}",
    "resultSummary": null,
    "resultReport": null,
    "contextSnapshot": null,
    "idempotencyKey": "exec-1001-1722218400",
    "version": 3,
    "startedAt": "2026-07-29 10:05:00",
    "pausedAt": null,
    "completedAt": null,
    "createTime": "2026-07-29 10:00:00",
    "updateTime": "2026-07-29 10:30:00",
    "deleted": 0
  }
}
```

---

#### 6.4 更新研究项目

- **接口名称**：更新研究项目
- **请求方法和路径**：`PUT /api/research/projects/{id}`
- **接口描述**：更新研究项目的标题和目标描述，所有字段均为可选，仅更新非 null 字段。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| id | Long | path | 是 | 项目ID |
| body | Object | body | 是 | 更新研究项目请求体 |

**请求示例**

```http
PUT /api/research/projects/1001
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "大语言模型在医疗领域的应用研究（更新版）",
  "goal": "深入调研 LLM 在医疗诊断、病历生成、药物研发等场景的应用现状、挑战与未来趋势"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 1001,
    "userId": 88,
    "workspaceId": null,
    "title": "大语言模型在医疗领域的应用研究（更新版）",
    "goal": "深入调研 LLM 在医疗诊断、病历生成、药物研发等场景的应用现状、挑战与未来趋势",
    "status": "DRAFT",
    "complexity": null,
    "agentWorkflow": null,
    "maxIterations": null,
    "currentIteration": null,
    "planJson": null,
    "resultSummary": null,
    "resultReport": null,
    "contextSnapshot": null,
    "idempotencyKey": null,
    "version": 1,
    "startedAt": null,
    "pausedAt": null,
    "completedAt": null,
    "createTime": "2026-07-29 10:00:00",
    "updateTime": "2026-07-29 10:10:00",
    "deleted": 0
  }
}
```

---

#### 6.5 删除研究项目

- **接口名称**：删除研究项目
- **请求方法和路径**：`DELETE /api/research/projects/{id}`
- **接口描述**：逻辑删除指定的研究项目及其关联数据。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 项目ID |

**请求示例**

```http
DELETE /api/research/projects/1001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已删除",
  "data": null
}
```

---

#### 6.6 启动研究

- **接口名称**：启动研究
- **请求方法和路径**：`POST /api/research/projects/{id}/execute`
- **接口描述**：启动指定项目的研究流程，触发 Planner Agent 进行研究规划与执行。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 项目ID |

**请求示例**

```http
POST /api/research/projects/1001/execute
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "研究已启动",
  "data": {
    "id": 1001,
    "userId": 88,
    "workspaceId": null,
    "title": "大语言模型在医疗领域的应用研究",
    "goal": "调研 LLM 在医疗诊断、病历生成等场景的应用现状与挑战",
    "status": "PLANNING",
    "complexity": null,
    "agentWorkflow": null,
    "maxIterations": null,
    "currentIteration": 0,
    "startedAt": "2026-07-29 10:05:00",
    "createTime": "2026-07-29 10:00:00",
    "updateTime": "2026-07-29 10:05:00",
    "deleted": 0
  }
}
```

---

#### 6.7 暂停研究

- **接口名称**：暂停研究
- **请求方法和路径**：`POST /api/research/projects/{id}/pause`
- **接口描述**：暂停正在执行的研究流程，保存当前上下文快照以便后续恢复。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 项目ID |

**请求示例**

```http
POST /api/research/projects/1001/pause
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已暂停",
  "data": null
}
```

---

#### 6.8 恢复研究

- **接口名称**：恢复研究
- **请求方法和路径**：`POST /api/research/projects/{id}/resume`
- **接口描述**：恢复已暂停的研究流程，从上下文快照继续执行。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 项目ID |

**请求示例**

```http
POST /api/research/projects/1001/resume
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已恢复",
  "data": {
    "id": 1001,
    "userId": 88,
    "workspaceId": null,
    "title": "大语言模型在医疗领域的应用研究",
    "goal": "调研 LLM 在医疗诊断、病历生成等场景的应用现状与挑战",
    "status": "RESEARCHING",
    "complexity": "DEEP",
    "currentIteration": 2,
    "startedAt": "2026-07-29 10:05:00",
    "createTime": "2026-07-29 10:00:00",
    "updateTime": "2026-07-29 10:35:00",
    "deleted": 0
  }
}
```

---

#### 6.9 归档研究

- **接口名称**：归档研究
- **请求方法和路径**：`POST /api/research/projects/{id}/archive`
- **接口描述**：将已完成的研究项目归档，归档后项目将进入只读状态。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| id | Long | path | 是 | 项目ID |

**请求示例**

```http
POST /api/research/projects/1001/archive
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已归档",
  "data": null
}
```

---

#### 6.10 查询任务列表

- **接口名称**：查询任务列表
- **请求方法和路径**：`GET /api/research/projects/{projectId}/tasks`
- **接口描述**：查询指定项目下的所有研究任务列表。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |

**请求示例**

```http
GET /api/research/projects/1001/tasks
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 2001,
      "projectId": 1001,
      "planId": 3001,
      "title": "调研 LLM 在医疗诊断中的应用",
      "description": "收集并分析 LLM 用于辅助诊断的案例与文献",
      "question": "LLM 在医疗诊断中的准确率和局限性如何？",
      "status": "COMPLETED",
      "dependsOn": null,
      "asyncTaskId": 9001,
      "requiresExternalSearch": 1,
      "resultSummary": "LLM 在影像诊断中表现出较高准确率，但在罕见病诊断上存在局限。",
      "sortOrder": 1,
      "startedAt": "2026-07-29 10:10:00",
      "completedAt": "2026-07-29 10:25:00",
      "createTime": "2026-07-29 10:06:00",
      "updateTime": "2026-07-29 10:25:00"
    },
    {
      "id": 2002,
      "projectId": 1001,
      "planId": 3001,
      "title": "调研 LLM 在病历生成中的应用",
      "description": "收集并分析 LLM 用于自动生成病历的实践",
      "question": "LLM 生成的病历在临床可接受度如何？",
      "status": "PENDING",
      "dependsOn": 2001,
      "asyncTaskId": null,
      "requiresExternalSearch": 1,
      "resultSummary": null,
      "sortOrder": 2,
      "startedAt": null,
      "completedAt": null,
      "createTime": "2026-07-29 10:06:00",
      "updateTime": "2026-07-29 10:06:00"
    }
  ]
}
```

---

#### 6.11 查询任务详情

- **接口名称**：查询任务详情
- **请求方法和路径**：`GET /api/research/projects/{projectId}/tasks/{taskId}`
- **接口描述**：根据项目ID和任务ID查询单个研究任务的详细信息。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |
| taskId | Long | path | 是 | 任务ID |

**请求示例**

```http
GET /api/research/projects/1001/tasks/2001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 2001,
    "projectId": 1001,
    "planId": 3001,
    "title": "调研 LLM 在医疗诊断中的应用",
    "description": "收集并分析 LLM 用于辅助诊断的案例与文献",
    "question": "LLM 在医疗诊断中的准确率和局限性如何？",
    "status": "COMPLETED",
    "dependsOn": null,
    "asyncTaskId": 9001,
    "requiresExternalSearch": 1,
    "resultSummary": "LLM 在影像诊断中表现出较高准确率，但在罕见病诊断上存在局限。",
    "sortOrder": 1,
    "startedAt": "2026-07-29 10:10:00",
    "completedAt": "2026-07-29 10:25:00",
    "createTime": "2026-07-29 10:06:00",
    "updateTime": "2026-07-29 10:25:00"
  }
}
```

---

#### 6.12 创建研究任务

- **接口名称**：创建研究任务
- **请求方法和路径**：`POST /api/research/projects/{projectId}/tasks`
- **接口描述**：在指定项目下手动创建一个新的研究任务。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| projectId | Long | path | 是 | 项目ID |
| body | Object | body | 是 | 创建研究任务请求体 |

**请求示例**

```http
POST /api/research/projects/1001/tasks
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "调研 LLM 在药物研发中的应用",
  "description": "收集并分析 LLM 辅助药物分子设计、靶点发现的实践案例",
  "question": "LLM 在药物研发的哪些环节最具潜力？",
  "dependsOn": 2001,
  "requiresExternalSearch": true,
  "sortOrder": 3
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 2003,
    "projectId": 1001,
    "planId": null,
    "title": "调研 LLM 在药物研发中的应用",
    "description": "收集并分析 LLM 辅助药物分子设计、靶点发现的实践案例",
    "question": "LLM 在药物研发的哪些环节最具潜力？",
    "status": "PENDING",
    "dependsOn": 2001,
    "asyncTaskId": null,
    "requiresExternalSearch": 1,
    "resultSummary": null,
    "sortOrder": 3,
    "startedAt": null,
    "completedAt": null,
    "createTime": "2026-07-29 10:40:00",
    "updateTime": "2026-07-29 10:40:00"
  }
}
```

---

#### 6.13 更新研究任务

- **接口名称**：更新研究任务
- **请求方法和路径**：`PUT /api/research/projects/{projectId}/tasks/{taskId}`
- **接口描述**：更新研究任务信息，所有字段均为可选，仅更新非 null 字段。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| projectId | Long | path | 是 | 项目ID |
| taskId | Long | path | 是 | 任务ID |
| body | Object | body | 是 | 更新研究任务请求体 |

**请求示例**

```http
PUT /api/research/projects/1001/tasks/2002
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "调研 LLM 在病历生成中的应用（更新版）",
  "description": "深入分析 LLM 自动生成结构化病历的实践与评估方法",
  "question": "LLM 生成的病历在临床可接受度与合规性方面表现如何？",
  "status": "RUNNING",
  "dependsOn": 2001,
  "requiresExternalSearch": true,
  "resultSummary": null,
  "sortOrder": 2
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 2002,
    "projectId": 1001,
    "planId": 3001,
    "title": "调研 LLM 在病历生成中的应用（更新版）",
    "description": "深入分析 LLM 自动生成结构化病历的实践与评估方法",
    "question": "LLM 生成的病历在临床可接受度与合规性方面表现如何？",
    "status": "RUNNING",
    "dependsOn": 2001,
    "asyncTaskId": null,
    "requiresExternalSearch": 1,
    "resultSummary": null,
    "sortOrder": 2,
    "startedAt": "2026-07-29 10:45:00",
    "completedAt": null,
    "createTime": "2026-07-29 10:06:00",
    "updateTime": "2026-07-29 10:45:00"
  }
}
```

---

#### 6.14 删除研究任务

- **接口名称**：删除研究任务
- **请求方法和路径**：`DELETE /api/research/projects/{projectId}/tasks/{taskId}`
- **接口描述**：删除指定项目下的研究任务。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |
| taskId | Long | path | 是 | 任务ID |

**请求示例**

```http
DELETE /api/research/projects/1001/tasks/2003
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已删除",
  "data": null
}
```

---

#### 6.15 批量创建任务

- **接口名称**：批量创建任务
- **请求方法和路径**：`POST /api/research/projects/{projectId}/tasks/batch`
- **接口描述**：在指定项目下批量创建多个研究任务，请求体为任务对象数组。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| projectId | Long | path | 是 | 项目ID |
| body | Object | body | 是 | 批量创建任务请求体（任务对象数组） |

**请求示例**

```http
POST /api/research/projects/1001/tasks/batch
Authorization: Bearer <token>
Content-Type: application/json

[
  {
    "title": "调研 LLM 在影像诊断中的应用",
    "description": "分析 LLM 辅助医学影像诊断的案例",
    "question": "LLM 在影像诊断中的准确率如何？",
    "dependsOn": null,
    "requiresExternalSearch": true,
    "sortOrder": 1
  },
  {
    "title": "调研 LLM 在病历生成中的应用",
    "description": "分析 LLM 自动生成病历的实践",
    "question": "LLM 生成病历的临床可接受度如何？",
    "dependsOn": null,
    "requiresExternalSearch": true,
    "sortOrder": 2
  }
]
```

**响应示例**

```json
{
  "code": 200,
  "message": "批量创建成功",
  "data": [
    {
      "id": 2010,
      "projectId": 1001,
      "planId": null,
      "title": "调研 LLM 在影像诊断中的应用",
      "description": "分析 LLM 辅助医学影像诊断的案例",
      "question": "LLM 在影像诊断中的准确率如何？",
      "status": "PENDING",
      "dependsOn": null,
      "asyncTaskId": null,
      "requiresExternalSearch": 1,
      "resultSummary": null,
      "sortOrder": 1,
      "startedAt": null,
      "completedAt": null,
      "createTime": "2026-07-29 11:00:00",
      "updateTime": "2026-07-29 11:00:00"
    },
    {
      "id": 2011,
      "projectId": 1001,
      "planId": null,
      "title": "调研 LLM 在病历生成中的应用",
      "description": "分析 LLM 自动生成病历的实践",
      "question": "LLM 生成病历的临床可接受度如何？",
      "status": "PENDING",
      "dependsOn": null,
      "asyncTaskId": null,
      "requiresExternalSearch": 1,
      "resultSummary": null,
      "sortOrder": 2,
      "startedAt": null,
      "completedAt": null,
      "createTime": "2026-07-29 11:00:00",
      "updateTime": "2026-07-29 11:00:00"
    }
  ]
}
```

---

#### 6.16 获取最新研究计划

- **接口名称**：获取最新研究计划
- **请求方法和路径**：`GET /api/research/projects/{projectId}/plans/latest`
- **接口描述**：获取指定项目的最新版本研究计划。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |

**请求示例**

```http
GET /api/research/projects/1001/plans/latest
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 3001,
    "projectId": 1001,
    "version": 2,
    "complexity": "DEEP",
    "agentChain": "PLANNER,RESEARCHER,REVIEWER,SYNTHESIZER",
    "tasksJson": "[{\"title\":\"调研诊断应用\",\"requiresExternalSearch\":true}]",
    "rationale": "该研究涉及多个细分场景且需要外部文献支撑，判定为深度研究。",
    "estimatedTokens": 80000,
    "createdBy": "PLANNER_AGENT",
    "createTime": "2026-07-29 10:06:00"
  }
}
```

---

#### 6.17 查询所有研究计划

- **接口名称**：查询所有研究计划
- **请求方法和路径**：`GET /api/research/projects/{projectId}/plans`
- **接口描述**：查询指定项目下的所有研究计划版本列表。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |

**请求示例**

```http
GET /api/research/projects/1001/plans
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 3000,
      "projectId": 1001,
      "version": 1,
      "complexity": "STANDARD",
      "agentChain": "PLANNER,RESEARCHER,SYNTHESIZER",
      "tasksJson": "[{\"title\":\"初步调研\",\"requiresExternalSearch\":false}]",
      "rationale": "初步评估为标准研究。",
      "estimatedTokens": 30000,
      "createdBy": "PLANNER_AGENT",
      "createTime": "2026-07-29 10:05:30"
    },
    {
      "id": 3001,
      "projectId": 1001,
      "version": 2,
      "complexity": "DEEP",
      "agentChain": "PLANNER,RESEARCHER,REVIEWER,SYNTHESIZER",
      "tasksJson": "[{\"title\":\"调研诊断应用\",\"requiresExternalSearch\":true}]",
      "rationale": "该研究涉及多个细分场景且需要外部文献支撑，判定为深度研究。",
      "estimatedTokens": 80000,
      "createdBy": "PLANNER_AGENT",
      "createTime": "2026-07-29 10:06:00"
    }
  ]
}
```

---

#### 6.18 查询研究计划详情

- **接口名称**：查询研究计划详情
- **请求方法和路径**：`GET /api/research/projects/{projectId}/plans/{planId}`
- **接口描述**：根据计划ID查询单个研究计划的详细信息。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |
| planId | Long | path | 是 | 计划ID |

**请求示例**

```http
GET /api/research/projects/1001/plans/3001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 3001,
    "projectId": 1001,
    "version": 2,
    "complexity": "DEEP",
    "agentChain": "PLANNER,RESEARCHER,REVIEWER,SYNTHESIZER",
    "tasksJson": "[{\"title\":\"调研诊断应用\",\"description\":\"...\",\"requiresExternalSearch\":true,\"dependsOn\":null}]",
    "rationale": "该研究涉及多个细分场景且需要外部文献支撑，判定为深度研究。",
    "estimatedTokens": 80000,
    "createdBy": "PLANNER_AGENT",
    "createTime": "2026-07-29 10:06:00"
  }
}
```

---

#### 6.19 创建研究计划

- **接口名称**：创建研究计划
- **请求方法和路径**：`POST /api/research/projects/{projectId}/plans`
- **接口描述**：为指定项目创建研究计划，通常由 Planner Agent 调用。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| Content-Type | String | header | 是 | `application/json` |
| projectId | Long | path | 是 | 项目ID |
| body | Object | body | 是 | 创建研究计划请求体 |

**请求示例**

```http
POST /api/research/projects/1001/plans
Authorization: Bearer <token>
Content-Type: application/json

{
  "complexity": "DEEP",
  "agentChain": "PLANNER,RESEARCHER,REVIEWER,SYNTHESIZER",
  "tasksJson": "[{\"title\":\"调研诊断应用\",\"description\":\"...\",\"requiresExternalSearch\":true,\"dependsOn\":null}]",
  "rationale": "该研究涉及多个细分场景且需要外部文献支撑，判定为深度研究。",
  "estimatedTokens": 80000,
  "createdBy": "PLANNER_AGENT"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 3002,
    "projectId": 1001,
    "version": 3,
    "complexity": "DEEP",
    "agentChain": "PLANNER,RESEARCHER,REVIEWER,SYNTHESIZER",
    "tasksJson": "[{\"title\":\"调研诊断应用\",\"description\":\"...\",\"requiresExternalSearch\":true,\"dependsOn\":null}]",
    "rationale": "该研究涉及多个细分场景且需要外部文献支撑，判定为深度研究。",
    "estimatedTokens": 80000,
    "createdBy": "PLANNER_AGENT",
    "createTime": "2026-07-29 10:50:00"
  }
}
```

---

#### 6.20 查询研究来源

- **接口名称**：查询研究来源
- **请求方法和路径**：`GET /api/research/projects/{projectId}/sources`
- **接口描述**：查询指定项目下的所有研究信息来源。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |

**请求示例**

```http
GET /api/research/projects/1001/sources
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 4001,
      "projectId": 1001,
      "taskId": 2001,
      "title": "LLM 在医学影像诊断中的最新研究综述",
      "url": "https://example.com/papers/llm-medical-imaging",
      "sourceType": "paper",
      "snippet": "本文综述了近三年 LLM 在医学影像辅助诊断中的主要进展...",
      "fullContent": "...完整抓取内容...",
      "relevanceScore": 0.92,
      "reliability": "high",
      "contentHash": "a1b2c3d4e5f6...",
      "fetchStatus": "success",
      "fetchedAt": "2026-07-29 10:15:00",
      "createTime": "2026-07-29 10:15:00"
    }
  ]
}
```

---

#### 6.21 查询研究来源详情

- **接口名称**：查询研究来源详情
- **请求方法和路径**：`GET /api/research/projects/{projectId}/sources/{sourceId}`
- **接口描述**：根据来源ID查询单个研究信息来源的详细信息。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |
| sourceId | Long | path | 是 | 来源ID |

**请求示例**

```http
GET /api/research/projects/1001/sources/4001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 4001,
    "projectId": 1001,
    "taskId": 2001,
    "title": "LLM 在医学影像诊断中的最新研究综述",
    "url": "https://example.com/papers/llm-medical-imaging",
    "sourceType": "paper",
    "snippet": "本文综述了近三年 LLM 在医学影像辅助诊断中的主要进展...",
    "fullContent": "...完整抓取内容...",
    "relevanceScore": 0.92,
    "reliability": "high",
    "contentHash": "a1b2c3d4e5f6...",
    "fetchStatus": "success",
    "fetchedAt": "2026-07-29 10:15:00",
    "createTime": "2026-07-29 10:15:00"
  }
}
```

---

#### 6.22 删除研究来源

- **接口名称**：删除研究来源
- **请求方法和路径**：`DELETE /api/research/projects/{projectId}/sources/{sourceId}`
- **接口描述**：删除指定的研究信息来源。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |
| sourceId | Long | path | 是 | 来源ID |

**请求示例**

```http
DELETE /api/research/projects/1001/sources/4001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已删除",
  "data": null
}
```

---

#### 6.23 查询任务执行步骤

- **接口名称**：查询任务执行步骤
- **请求方法和路径**：`GET /api/research/projects/{projectId}/tasks/{taskId}/steps`
- **接口描述**：查询指定任务下的所有 Agent 执行步骤日志，按步骤序号排序。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |
| taskId | Long | path | 是 | 任务ID |

**请求示例**

```http
GET /api/research/projects/1001/tasks/2001/steps
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 5001,
      "taskId": 2001,
      "executionId": 7001,
      "agentName": "RESEARCHER",
      "stepType": "THINKING",
      "title": "分析研究问题",
      "content": "需要先检索 LLM 在医疗诊断中的最新文献...",
      "toolName": null,
      "toolInput": null,
      "toolOutput": null,
      "tokenUsage": "{\"prompt\":1200,\"completion\":300}",
      "status": "COMPLETED",
      "errorMessage": null,
      "sortOrder": 1,
      "durationMs": 1500,
      "createTime": "2026-07-29 10:10:05"
    },
    {
      "id": 5002,
      "taskId": 2001,
      "executionId": 7001,
      "agentName": "RESEARCHER",
      "stepType": "TOOL_CALL",
      "title": "调用搜索工具",
      "content": "执行 web 搜索获取相关文献",
      "toolName": "web_search",
      "toolInput": "{\"query\":\"LLM 医疗影像诊断 2025\"}",
      "toolOutput": "{\"results\":[...]}",
      "tokenUsage": null,
      "status": "COMPLETED",
      "errorMessage": null,
      "sortOrder": 2,
      "durationMs": 2300,
      "createTime": "2026-07-29 10:10:10"
    }
  ]
}
```

---

#### 6.24 查询项目所有执行步骤

- **接口名称**：查询项目所有执行步骤
- **请求方法和路径**：`GET /api/research/projects/{projectId}/steps`
- **接口描述**：查询指定项目下所有任务的执行步骤日志，用于整体审计和回溯。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |

**请求示例**

```http
GET /api/research/projects/1001/steps
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 5001,
      "taskId": 2001,
      "executionId": 7001,
      "agentName": "RESEARCHER",
      "stepType": "THINKING",
      "title": "分析研究问题",
      "content": "需要先检索 LLM 在医疗诊断中的最新文献...",
      "toolName": null,
      "toolInput": null,
      "toolOutput": null,
      "tokenUsage": "{\"prompt\":1200,\"completion\":300}",
      "status": "COMPLETED",
      "errorMessage": null,
      "sortOrder": 1,
      "durationMs": 1500,
      "createTime": "2026-07-29 10:10:05"
    },
    {
      "id": 5010,
      "taskId": 2002,
      "executionId": 7002,
      "agentName": "RESEARCHER",
      "stepType": "TOOL_CALL",
      "title": "调用搜索工具",
      "content": "执行 web 搜索获取病历生成相关文献",
      "toolName": "web_search",
      "toolInput": "{\"query\":\"LLM 自动生成病历 临床评估\"}",
      "toolOutput": "{\"results\":[...]}",
      "tokenUsage": null,
      "status": "COMPLETED",
      "errorMessage": null,
      "sortOrder": 1,
      "durationMs": 2100,
      "createTime": "2026-07-29 10:30:05"
    }
  ]
}
```

---

#### 6.25 获取最新研究报告

- **接口名称**：获取最新研究报告
- **请求方法和路径**：`GET /api/research/projects/{projectId}/report`
- **接口描述**：获取指定项目最新版本的研究报告。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |

**请求示例**

```http
GET /api/research/projects/1001/report
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 6001,
    "projectId": 1001,
    "version": 2,
    "title": "大语言模型在医疗领域的应用研究报告",
    "summary": "本研究调研了 LLM 在医疗诊断、病历生成等场景的应用，发现其在影像辅助诊断方面表现优异，但在罕见病诊断和合规性方面存在挑战。",
    "contentMd": "# 大语言模型在医疗领域的应用研究\n\n## 1. 研究背景\n...完整 Markdown 报告...",
    "researchQuestions": "[\"LLM 在医疗诊断中的准确率如何？\",\"LLM 生成病历的临床可接受度如何？\"]",
    "keyFindings": "[{\"finding\":\"LLM 在影像诊断中准确率较高\",\"confidence\":0.85}]",
    "knowledgeGaps": "[\"罕见病诊断数据不足\",\"长期合规性评估缺失\"]",
    "newKnowledgeIds": "[10001,10002]",
    "newRelationIds": "[20001,20002]",
    "sourceCount": 15,
    "conclusionCount": 8,
    "tokenUsageTotal": "{\"prompt\":45000,\"completion\":12000}",
    "durationTotalMs": 1800000,
    "generatedBy": "SYNTHESIZER",
    "createTime": "2026-07-29 11:30:00"
  }
}
```

---

#### 6.26 获取所有研究报告版本

- **接口名称**：获取所有研究报告版本
- **请求方法和路径**：`GET /api/research/projects/{projectId}/reports`
- **接口描述**：获取指定项目所有版本的研究报告列表，支持版本回溯对比。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |

**请求示例**

```http
GET /api/research/projects/1001/reports
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 6000,
      "projectId": 1001,
      "version": 1,
      "title": "大语言模型在医疗领域的应用研究报告（初版）",
      "summary": "初步调研表明 LLM 在医疗影像诊断方面具有应用潜力。",
      "contentMd": "# 初步报告\n...",
      "researchQuestions": "[\"LLM 在医疗诊断中的准确率如何？\"]",
      "keyFindings": "[{\"finding\":\"LLM 在影像诊断中准确率较高\",\"confidence\":0.7}]",
      "knowledgeGaps": "[]",
      "newKnowledgeIds": "[10001]",
      "newRelationIds": "[20001]",
      "sourceCount": 8,
      "conclusionCount": 3,
      "tokenUsageTotal": "{\"prompt\":20000,\"completion\":5000}",
      "durationTotalMs": 900000,
      "generatedBy": "SYNTHESIZER",
      "createTime": "2026-07-29 11:00:00"
    },
    {
      "id": 6001,
      "projectId": 1001,
      "version": 2,
      "title": "大语言模型在医疗领域的应用研究报告",
      "summary": "本研究调研了 LLM 在医疗诊断、病历生成等场景的应用，发现其在影像辅助诊断方面表现优异，但在罕见病诊断和合规性方面存在挑战。",
      "contentMd": "# 大语言模型在医疗领域的应用研究\n\n## 1. 研究背景\n...完整 Markdown 报告...",
      "researchQuestions": "[\"LLM 在医疗诊断中的准确率如何？\",\"LLM 生成病历的临床可接受度如何？\"]",
      "keyFindings": "[{\"finding\":\"LLM 在影像诊断中准确率较高\",\"confidence\":0.85}]",
      "knowledgeGaps": "[\"罕见病诊断数据不足\",\"长期合规性评估缺失\"]",
      "newKnowledgeIds": "[10001,10002]",
      "newRelationIds": "[20001,20002]",
      "sourceCount": 15,
      "conclusionCount": 8,
      "tokenUsageTotal": "{\"prompt\":45000,\"completion\":12000}",
      "durationTotalMs": 1800000,
      "generatedBy": "SYNTHESIZER",
      "createTime": "2026-07-29 11:30:00"
    }
  ]
}
```

---

#### 6.27 查询项目所有研究记忆

- **接口名称**：查询项目所有研究记忆
- **请求方法和路径**：`GET /api/research/projects/{projectId}/memory`
- **接口描述**：查询指定项目下的所有研究记忆，包括知识状态、缺口发现、搜索结果等中间状态信息。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |

**请求示例**

```http
GET /api/research/projects/1001/memory
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 8001,
      "projectId": 1001,
      "userId": 88,
      "memoryKey": "diagnosis_accuracy",
      "memoryType": "knowledge_state",
      "content": "{\"topic\":\"影像诊断准确率\",\"value\":\"LLM 在常见病影像诊断准确率达 92%\"}",
      "lastAccessedAt": "2026-07-29 10:25:00",
      "expiresAt": null,
      "createTime": "2026-07-29 10:20:00",
      "updateTime": "2026-07-29 10:25:00"
    },
    {
      "id": 8002,
      "projectId": 1001,
      "userId": 88,
      "memoryKey": "rare_disease_gap",
      "memoryType": "gap_found",
      "content": "{\"gap\":\"罕见病诊断训练数据不足\",\"severity\":\"high\"}",
      "lastAccessedAt": "2026-07-29 10:26:00",
      "expiresAt": null,
      "createTime": "2026-07-29 10:22:00",
      "updateTime": "2026-07-29 10:26:00"
    }
  ]
}
```

---

#### 6.28 按类型查询研究记忆

- **接口名称**：按类型查询研究记忆
- **请求方法和路径**：`GET /api/research/projects/{projectId}/memory/{type}`
- **接口描述**：根据记忆类型查询指定项目下的研究记忆。记忆类型包括：knowledge_state/gap_found/search_result/user_preference/decision。需要登录认证。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | header | 是 | 认证 Token，格式为 `Bearer <token>` |
| projectId | Long | path | 是 | 项目ID |
| type | String | path | 是 | 记忆类型：knowledge_state/gap_found/search_result/user_preference/decision |

**请求示例**

```http
GET /api/research/projects/1001/memory/gap_found
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 8002,
      "projectId": 1001,
      "userId": 88,
      "memoryKey": "rare_disease_gap",
      "memoryType": "gap_found",
      "content": "{\"gap\":\"罕见病诊断训练数据不足\",\"severity\":\"high\"}",
      "lastAccessedAt": "2026-07-29 10:26:00",
      "expiresAt": null,
      "createTime": "2026-07-29 10:22:00",
      "updateTime": "2026-07-29 10:26:00"
    },
    {
      "id": 8005,
      "projectId": 1001,
      "userId": 88,
      "memoryKey": "compliance_gap",
      "memoryType": "gap_found",
      "content": "{\"gap\":\"LLM 生成病历的长期合规性评估数据缺失\",\"severity\":\"medium\"}",
      "lastAccessedAt": "2026-07-29 10:28:00",
      "expiresAt": null,
      "createTime": "2026-07-29 10:27:00",
      "updateTime": "2026-07-29 10:28:00"
    }
  ]
}
```

---

### 第7章：工作区模块

工作区模块提供工作区（多人协作空间）的创建、查询、更新、删除以及成员管理能力，是 RBAC 多租户体系的核心。所有接口均需要登录认证，部分接口还要求当前用户具备工作区所有者或管理员权限。

---

#### 7.1 创建工作区

- **接口名称**：创建工作区
- **请求方法和路径**：`POST /api/workspace`
- **接口描述**：当前登录用户创建一个新的工作区，创建者自动成为该工作区的所有者（owner）。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| name | String | body | 是 | 工作区名称，不能为空，最长 100 字符 |
| description | String | body | 否 | 工作区描述，最长 500 字符 |

**请求示例**

```http
POST /api/workspace
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "前端学习小组",
  "description": "用于沉淀前端学习笔记与项目复盘"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "工作区创建成功",
  "data": {
    "id": 1001,
    "name": "前端学习小组",
    "description": "用于沉淀前端学习笔记与项目复盘",
    "ownerId": 2008,
    "role": "owner",
    "memberCount": 1,
    "status": 1,
    "createTime": "2026-07-24T10:12:30"
  }
}
```

---

#### 7.2 工作区列表

- **接口名称**：我的工作区列表
- **请求方法和路径**：`GET /api/workspace`
- **接口描述**：获取当前登录用户已加入的全部工作区列表（含已确认和待确认邀请）。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
GET /api/workspace
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1001,
      "name": "前端学习小组",
      "description": "用于沉淀前端学习笔记与项目复盘",
      "ownerId": 2008,
      "role": "owner",
      "memberCount": 5,
      "status": 1,
      "createTime": "2026-07-24T10:12:30"
    },
    {
      "id": 1002,
      "name": "后端架构组",
      "description": "后端架构设计与代码评审空间",
      "ownerId": 2010,
      "role": "editor",
      "memberCount": 8,
      "status": 1,
      "createTime": "2026-06-12T09:00:00"
    }
  ]
}
```

---

#### 7.3 工作区详情

- **接口名称**：工作区详情
- **请求方法和路径**：`GET /api/workspace/{id}`
- **接口描述**：获取指定工作区的基本信息。当前用户必须是该工作区成员，否则返回无权限。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 工作区ID |

**请求示例**

```http
GET /api/workspace/1001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "name": "前端学习小组",
    "description": "用于沉淀前端学习笔记与项目复盘",
    "ownerId": 2008,
    "role": "owner",
    "memberCount": 5,
    "status": 1,
    "createTime": "2026-07-24T10:12:30"
  }
}
```

---

#### 7.4 更新工作区

- **接口名称**：更新工作区信息
- **请求方法和路径**：`PUT /api/workspace/{id}`
- **接口描述**：更新工作区名称与描述。仅工作区所有者（owner）或管理员（admin）可执行此操作。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 工作区ID |
| name | String | body | 是 | 工作区名称，不能为空，最长 100 字符 |
| description | String | body | 否 | 工作区描述，最长 500 字符 |

**请求示例**

```http
PUT /api/workspace/1001
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "前端学习小组-V2",
  "description": "更新后的描述：覆盖前端工程化、性能优化等方向"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

---

#### 7.5 删除工作区

- **接口名称**：删除工作区
- **请求方法和路径**：`DELETE /api/workspace/{id}`
- **接口描述**：删除指定工作区及其成员关系。仅工作区所有者（owner）可执行此操作。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 工作区ID |

**请求示例**

```http
DELETE /api/workspace/1001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

#### 7.6 转让工作区

- **接口名称**：转让工作区所有权
- **请求方法和路径**：`PUT /api/workspace/{id}/transfer`
- **接口描述**：将工作区所有权转让给指定成员。仅当前所有者可操作；目标用户必须是该工作区的成员。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 工作区ID |
| newOwnerUserId | Long | body | 是 | 新所有者的用户ID，不能为空 |

**请求示例**

```http
PUT /api/workspace/1001/transfer
Authorization: Bearer <token>
Content-Type: application/json

{
  "newOwnerUserId": 2010
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "所有权转让成功",
  "data": null
}
```

---

#### 7.7 邀请成员

- **接口名称**：添加成员
- **请求方法和路径**：`POST /api/workspace/{id}/members`
- **接口描述**：向指定工作区邀请一名成员，被邀请人初始状态为 `pending`，需通过 `6.10 接受邀请` 接口确认后才会正式加入。仅所有者或管理员可调用。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 工作区ID |
| userId | Long | body | 是 | 被邀请的用户ID，不能为空 |
| role | String | body | 是 | 邀请的角色，取值：admin / editor / viewer，不能为空 |

**请求示例**

```http
POST /api/workspace/1001/members
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": 2025,
  "role": "editor"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "邀请已发送，等待对方确认",
  "data": null
}
```

---

#### 7.8 成员列表

- **接口名称**：成员列表
- **请求方法和路径**：`GET /api/workspace/{id}/members`
- **接口描述**：查询指定工作区的全部成员（含待确认成员）。当前用户必须是工作区成员。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 工作区ID |

**请求示例**

```http
GET /api/workspace/1001/members
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "userId": 2008,
      "username": "alice",
      "avatar": "https://oss.example.com/avatar/2008.png",
      "role": "owner",
      "joinedTime": "2026-07-24T10:12:30",
      "status": "accepted"
    },
    {
      "userId": 2025,
      "username": "bob",
      "avatar": "https://oss.example.com/avatar/2025.png",
      "role": "editor",
      "joinedTime": "2026-07-24T11:00:00",
      "status": "pending"
    }
  ]
}
```

---

#### 7.9 更新成员角色

- **接口名称**：更新成员角色
- **请求方法和路径**：`PUT /api/workspace/{id}/members/{targetUserId}`
- **接口描述**：修改某成员在工作区中的角色。仅所有者或管理员可操作，不能修改自己的角色，也不能修改所有者的角色。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 工作区ID |
| targetUserId | Long | path | 是 | 目标用户ID |
| role | String | body | 是 | 新角色，取值：admin / editor / viewer，不能为空 |

**请求示例**

```http
PUT /api/workspace/1001/members/2025
Authorization: Bearer <token>
Content-Type: application/json

{
  "role": "admin"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "角色更新成功",
  "data": null
}
```

---

#### 7.10 接受邀请

- **接口名称**：确认加入工作区
- **请求方法和路径**：`PUT /api/workspace/{id}/members/accept`
- **接口描述**：被邀请人确认加入指定工作区，将成员状态从 `pending` 更新为 `accepted`。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 工作区ID |

**请求示例**

```http
PUT /api/workspace/1001/members/accept
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已成功加入工作区",
  "data": null
}
```

---

#### 7.11 移除成员

- **接口名称**：移除成员
- **请求方法和路径**：`DELETE /api/workspace/{id}/members/{targetUserId}`
- **接口描述**：将指定成员从工作区移除。仅所有者或管理员可操作，且不能移除工作区所有者。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 工作区ID |
| targetUserId | Long | path | 是 | 目标用户ID |

**请求示例**

```http
DELETE /api/workspace/1001/members/2025
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "成员已移除",
  "data": null
}
```

---

#### 7.12 切换工作区

- **接口名称**：切换到指定工作区
- **请求方法和路径**：`PUT /api/workspace/{id}/switch`
- **接口描述**：将当前会话切换到指定工作区，服务端会签发新的 JWT（包含 workspaceId 声明）。当前用户必须是该工作区成员，否则返回 403。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 工作区ID |

**请求示例**

```http
PUT /api/workspace/1001/switch
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "工作区切换成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjIwMDgsIndvcmtzcGFjZUlkIjoxMDAxfQ.xxx",
    "workspaceId": 1001
  }
}
```

---

#### 7.13 获取/创建个人工作区

- **接口名称**：切换到个人空间
- **请求方法和路径**：`PUT /api/workspace/personal`
- **接口描述**：退出当前工作区上下文，切换回个人空间。服务端会签发不含 workspaceId 的新 JWT。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
PUT /api/workspace/personal
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已切换到个人空间",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjIwMDgsIndvcmtzcGFjZUlkIjpudWxsfQ.xxx"
  }
}
```

---

### 第8章：知识广场与分享模块

知识广场模块面向工作区之间的协作知识分享，提供帖子发布、浏览、点赞、评论、收藏、举报等互动能力；分享模块则面向外部用户，通过随机 token 将知识节点对外公开分享。除 `8.13 访问分享` 为公开接口外，其余接口均需登录认证。

---

#### 8.1 发布到广场

- **接口名称**：发布到广场
- **请求方法和路径**：`POST /api/square/publish`
- **接口描述**：将一个知识节点发布到知识广场，可指定范围 `global`（全站可见）或 `workspace`（仅本工作区可见，需指定 workspaceId）。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| nodeId | Long | body | 是 | 知识节点ID，不能为空 |
| recommendText | String | body | 否 | 推荐语/分享理由，最长 200 字 |
| scope | String | body | 否 | 发布范围，取值 `global` / `workspace`，默认 `global` |
| workspaceId | Long | body | 否 | 工作区ID，scope=workspace 时必填 |

**请求示例**

```http
POST /api/square/publish
Authorization: Bearer <token>
Content-Type: application/json

{
  "nodeId": 5001,
  "recommendText": "这篇笔记系统梳理了 React Hooks 的核心原理与避坑指南，强烈推荐。",
  "scope": "global"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "发布成功",
  "data": {
    "postId": 8001,
    "nodeId": 5001,
    "nodeTitle": "React Hooks 核心原理与避坑指南",
    "nodeSummary": "系统梳理 useEffect、useMemo、useCallback 等核心 Hook 的运行机制……",
    "recommendText": "这篇笔记系统梳理了 React Hooks 的核心原理与避坑指南，强烈推荐。",
    "authorId": 2008,
    "authorName": "alice",
    "authorAvatar": "https://oss.example.com/avatar/2008.png",
    "likeCount": 0,
    "commentCount": 0,
    "bookmarkCount": 0,
    "isLiked": false,
    "isBookmarked": false,
    "status": "published",
    "createdAt": "2026-07-24T14:30:00",
    "comments": null
  }
}
```

---

#### 8.2 删除发布

- **接口名称**：下架帖子
- **请求方法和路径**：`DELETE /api/square/{id}`
- **接口描述**：将自己在广场发布的帖子下架（逻辑删除，状态置为 `removed`）。仅发布者本人可操作。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 帖子ID |

**请求示例**

```http
DELETE /api/square/8001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已下架",
  "data": null
}
```

---

#### 8.3 广场列表

- **接口名称**：广场帖子列表
- **请求方法和路径**：`GET /api/square/list`
- **接口描述**：分页查询广场帖子列表，支持按范围、排序方式与关键词过滤。当 `scope=workspace` 时返回当前工作区内的帖子。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| scope | String | query | 否 | 发布范围：`global` / `workspace`，默认 `global` |
| sort | String | query | 否 | 排序方式：`newest`（最新）/ `hottest`（最热），默认 `newest` |
| keyword | String | query | 否 | 搜索关键词，按标题/摘要/推荐语模糊匹配 |
| current | Integer | query | 否 | 当前页码，默认 1 |
| size | Integer | query | 否 | 每页大小，默认 10 |

**请求示例**

```http
GET /api/square/list?scope=global&sort=hottest&keyword=React&current=1&size=10
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "postId": 8001,
        "nodeId": 5001,
        "nodeTitle": "React Hooks 核心原理与避坑指南",
        "nodeSummary": "系统梳理 useEffect、useMemo、useCallback 等核心 Hook 的运行机制……",
        "recommendText": "这篇笔记系统梳理了 React Hooks 的核心原理与避坑指南，强烈推荐。",
        "authorId": 2008,
        "authorName": "alice",
        "authorAvatar": "https://oss.example.com/avatar/2008.png",
        "likeCount": 128,
        "commentCount": 12,
        "bookmarkCount": 35,
        "isLiked": true,
        "isBookmarked": false,
        "status": "published",
        "createdAt": "2026-07-24T14:30:00",
        "comments": null
      }
    ],
    "total": 86,
    "size": 10,
    "current": 1,
    "pages": 9
  }
}
```

---

#### 8.4 广场详情

- **接口名称**：帖子详情
- **请求方法和路径**：`GET /api/square/{id}`
- **接口描述**：获取指定帖子的详细信息，返回结果中包含评论列表（`comments` 字段）。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 帖子ID |

**请求示例**

```http
GET /api/square/8001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "postId": 8001,
    "nodeId": 5001,
    "nodeTitle": "React Hooks 核心原理与避坑指南",
    "nodeSummary": "系统梳理 useEffect、useMemo、useCallback 等核心 Hook 的运行机制……",
    "recommendText": "这篇笔记系统梳理了 React Hooks 的核心原理与避坑指南，强烈推荐。",
    "authorId": 2008,
    "authorName": "alice",
    "authorAvatar": "https://oss.example.com/avatar/2008.png",
    "likeCount": 128,
    "commentCount": 2,
    "bookmarkCount": 35,
    "isLiked": true,
    "isBookmarked": true,
    "status": "published",
    "createdAt": "2026-07-24T14:30:00",
    "comments": [
      {
        "id": 30001,
        "postId": 8001,
        "userId": 2010,
        "username": "carol",
        "avatar": "https://oss.example.com/avatar/2010.png",
        "content": "总结得很全面，特别是 useEffect 闭包陷阱那一段很受用。",
        "createdAt": "2026-07-24T15:00:00",
        "deleted": 0
      },
      {
        "id": 30002,
        "postId": 8001,
        "userId": 2025,
        "username": "bob",
        "avatar": "https://oss.example.com/avatar/2025.png",
        "content": "求源码链接～",
        "createdAt": "2026-07-24T15:30:00",
        "deleted": 0
      }
    ]
  }
}
```

---

#### 8.5 点赞

- **接口名称**：点赞/取消点赞
- **请求方法和路径**：`POST /api/square/{id}/like`
- **接口描述**：对指定帖子进行点赞或取消点赞的切换操作。返回 `true` 表示当前已点赞，`false` 表示已取消点赞。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 帖子ID |

**请求示例**

```http
POST /api/square/8001/like
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已点赞",
  "data": true
}
```

---

#### 8.6 评论

- **接口名称**：发表评论
- **请求方法和路径**：`POST /api/square/{id}/comment`
- **接口描述**：在指定帖子下发表一条评论。返回新建评论的视图对象。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 帖子ID |
| content | String | body | 是 | 评论内容，1~500 字，不能为空 |

**请求示例**

```http
POST /api/square/8001/comment
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "总结得非常系统，特别是 useEffect 闭包陷阱那一段很受用。"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "评论成功",
  "data": {
    "id": 30003,
    "postId": 8001,
    "userId": 2008,
    "username": "alice",
    "avatar": "https://oss.example.com/avatar/2008.png",
    "content": "总结得非常系统，特别是 useEffect 闭包陷阱那一段很受用。",
    "createdAt": "2026-07-24T16:00:00",
    "deleted": 0
  }
}
```

---

#### 8.7 删除评论

- **接口名称**：删除评论
- **请求方法和路径**：`DELETE /api/square/{id}/comment/{commentId}`
- **接口描述**：删除指定帖子下的一条评论（逻辑删除）。仅评论发布者本人可操作。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 帖子ID |
| commentId | Long | path | 是 | 评论ID |

**请求示例**

```http
DELETE /api/square/8001/comment/30003
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "评论已删除",
  "data": null
}
```

---

#### 8.8 收藏

- **接口名称**：收藏/取消收藏
- **请求方法和路径**：`POST /api/square/{id}/bookmark`
- **接口描述**：对指定帖子进行收藏或取消收藏的切换操作。返回 `true` 表示当前已收藏，`false` 表示已取消收藏。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 帖子ID |

**请求示例**

```http
POST /api/square/8001/bookmark
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "已收藏",
  "data": true
}
```

---

#### 8.9 收藏列表

- **接口名称**：我的收藏列表
- **请求方法和路径**：`GET /api/square/bookmarks`
- **接口描述**：分页查询当前登录用户收藏的全部广场帖子。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| current | Integer | query | 否 | 当前页码，默认 1 |
| size | Integer | query | 否 | 每页大小，默认 10 |

**请求示例**

```http
GET /api/square/bookmarks?current=1&size=10
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "postId": 8001,
        "nodeId": 5001,
        "nodeTitle": "React Hooks 核心原理与避坑指南",
        "nodeSummary": "系统梳理 useEffect、useMemo、useCallback 等核心 Hook 的运行机制……",
        "recommendText": "这篇笔记系统梳理了 React Hooks 的核心原理与避坑指南，强烈推荐。",
        "authorId": 2008,
        "authorName": "alice",
        "authorAvatar": "https://oss.example.com/avatar/2008.png",
        "likeCount": 128,
        "commentCount": 12,
        "bookmarkCount": 35,
        "isLiked": true,
        "isBookmarked": true,
        "status": "published",
        "createdAt": "2026-07-24T14:30:00",
        "comments": null
      }
    ],
    "total": 5,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 8.10 点赞列表

- **接口名称**：我的点赞列表
- **请求方法和路径**：`GET /api/square/likes`
- **接口描述**：分页查询当前登录用户点赞过的全部广场帖子。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| current | Integer | query | 否 | 当前页码，默认 1 |
| size | Integer | query | 否 | 每页大小，默认 10 |

**请求示例**

```http
GET /api/square/likes?current=1&size=10
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "postId": 8001,
        "nodeId": 5001,
        "nodeTitle": "React Hooks 核心原理与避坑指南",
        "nodeSummary": "系统梳理 useEffect、useMemo、useCallback 等核心 Hook 的运行机制……",
        "recommendText": "这篇笔记系统梳理了 React Hooks 的核心原理与避坑指南，强烈推荐。",
        "authorId": 2008,
        "authorName": "alice",
        "authorAvatar": "https://oss.example.com/avatar/2008.png",
        "likeCount": 128,
        "commentCount": 12,
        "bookmarkCount": 35,
        "isLiked": true,
        "isBookmarked": false,
        "status": "published",
        "createdAt": "2026-07-24T14:30:00",
        "comments": null
      }
    ],
    "total": 12,
    "size": 10,
    "current": 1,
    "pages": 2
  }
}
```

---

#### 8.11 举报

- **接口名称**：举报帖子
- **请求方法和路径**：`POST /api/square/{id}/report`
- **接口描述**：举报指定帖子，举报记录进入后台审核队列。同一用户对同一帖子重复举报会被幂等处理。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 帖子ID |
| reason | String | body | 是 | 举报原因，最长 500 字，不能为空 |

**请求示例**

```http
POST /api/square/8001/report
Authorization: Bearer <token>
Content-Type: application/json

{
  "reason": "内容涉嫌抄袭，且包含与事实不符的结论。"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "举报已提交",
  "data": null
}
```

---

#### 8.12 创建分享

- **接口名称**：创建分享链接
- **请求方法和路径**：`POST /api/share`
- **接口描述**：为指定知识节点创建一条对外分享链接，token 使用 SHA-256 随机生成不可猜测。可指定有效期类型 `permanent`（永久）/ `7d`（7天）/ `24h`（24小时）。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| nodeId | Long | body | 是 | 知识节点ID，不能为空 |
| expireType | String | body | 否 | 有效期类型，取值 `permanent` / `7d` / `24h`，默认 `permanent` |

**请求示例**

```http
POST /api/share
Authorization: Bearer <token>
Content-Type: application/json

{
  "nodeId": 5001,
  "expireType": "7d"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "分享链接创建成功",
  "data": {
    "id": 9001,
    "nodeId": 5001,
    "ownerId": 2008,
    "token": "a3f5c7e9b1d4f6a8c2e0b3d5f7a9c1e3b5d7f9a1c3e5b7d9f1a3c5e7b9d1f3a5",
    "expireType": "7d",
    "expiresAt": "2026-07-31T14:30:00",
    "shareUrl": "https://secondbrain.example.com/share/a3f5c7e9b1d4f6a8c2e0b3d5f7a9c1e3b5d7f9a1c3e5b7d9f1a3c5e7b9d1f3a5"
  }
}
```

---

#### 8.13 访问分享

- **接口名称**：访问分享内容（公开）
- **请求方法和路径**：`GET /api/share/{token}`
- **接口描述**：通过分享 token 公开访问知识节点内容，无需登录鉴权。若 token 已撤销、过期或不存在，返回 404。每次访问累计访问次数 +1。
- **是否需要登录认证**：否

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| token | String | path | 是 | 分享令牌（64 字符 SHA-256） |

**请求示例**

```http
GET /api/share/a3f5c7e9b1d4f6a8c2e0b3d5f7a9c1e3b5d7f9a1c3e5b7d9f1a3c5e7b9d1f3a5
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "nodeId": 5001,
    "title": "React Hooks 核心原理与避坑指南",
    "summary": "系统梳理 useEffect、useMemo、useCallback 等核心 Hook 的运行机制……",
    "content": "## 一、useEffect 的执行时机\n\nReact 会在每次渲染 commit 之后调用 effect……",
    "ownerName": "alice",
    "shareCreatedAt": "2026-07-24T14:30:00"
  }
}
```

---

#### 8.14 删除分享

- **接口名称**：撤销分享
- **请求方法和路径**：`DELETE /api/share/{id}`
- **接口描述**：撤销指定分享链接（将 `isRevoked` 置为 1），撤销后该 token 不再可访问。仅分享创建者本人可操作。
- **是否需要登录认证**：是

**请求参数**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | path | 是 | 分享记录ID |

**请求示例**

```http
DELETE /api/share/9001
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "分享已撤销",
  "data": null
}
```

---

#### 8.15 分享列表

- **接口名称**：我的分享列表
- **请求方法和路径**：`GET /api/share/list`
- **接口描述**：查询当前登录用户创建的全部分享链接记录（含已撤销记录）。
- **是否需要登录认证**：是

**请求参数**

无业务参数。

**请求示例**

```http
GET /api/share/list
Authorization: Bearer <token>
```

**响应示例**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 9001,
      "nodeId": 5001,
      "ownerId": 2008,
      "token": "a3f5c7e9b1d4f6a8c2e0b3d5f7a9c1e3b5d7f9a1c3e5b7d9f1a3c5e7b9d1f3a5",
      "expireType": "7d",
      "expiresAt": "2026-07-31T14:30:00",
      "accessCount": 42,
      "isRevoked": 0,
      "createdAt": "2026-07-24T14:30:00"
    },
    {
      "id": 9002,
      "nodeId": 5002,
      "ownerId": 2008,
      "token": "b1d4f6a8c2e0b3d5f7a9c1e3b5d7f9a1c3e5b7d9f1a3c5e7b9d1f3a5c7e9",
      "expireType": "permanent",
      "expiresAt": null,
      "accessCount": 18,
      "isRevoked": 1,
      "createdAt": "2026-06-10T09:00:00"
    }
  ]
}
```

### 第9章：游戏化与统计模块

#### 9.1 游戏化档案

- **接口名称**：获取游戏化概览
- **请求方法和路径**：GET /api/gamification/profile
- **接口描述**：获取当前登录用户的游戏化档案，包含总积分、当前积分、等级、经验值、连续签到天数、最长连续天数、是否今日已签到、剩余补签卡数量、复习统计、准确率以及下一个待解锁成就等信息。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
GET /api/gamification/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalPoints": 1280,
    "currentPoints": 560,
    "level": 5,
    "experience": 1280,
    "experienceToNextLevel": 1800,
    "levelProgressPercent": 62.5,
    "currentStreak": 7,
    "maxStreak": 21,
    "lastCheckInDate": "2026-07-24",
    "checkedInToday": true,
    "makeupCardsRemaining": 3,
    "totalReviewCount": 156,
    "totalCorrectCount": 132,
    "totalNodeCount": 48,
    "accuracyRate": 84.6,
    "nextAchievement": {
      "id": 12,
      "code": "STREAK_30",
      "name": "坚持三十天",
      "description": "连续签到 30 天",
      "icon": "🔥",
      "category": "streak",
      "tier": "gold",
      "triggerValue": 30,
      "pointsReward": 300,
      "makeupCardReward": 1,
      "currentValue": 7,
      "progressPercent": 23,
      "unlocked": false,
      "unlockedAt": null
    },
    "newAchievements": null,
    "pointsEarned": null
  }
}
```
- **是否需要登录认证**：是

---

#### 9.2 每日签到

- **接口名称**：每日签到
- **请求方法和路径**：POST /api/gamification/check-in
- **接口描述**：当前用户执行每日签到，签到成功后会发放积分，并可能触发成就解锁。返回最新的游戏化档案，包含本次签到获得的积分和可能新解锁的成就列表。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
POST /api/gamification/check-in
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
Content-Type: application/json
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "签到成功",
  "data": {
    "totalPoints": 1300,
    "currentPoints": 580,
    "level": 5,
    "experience": 1300,
    "experienceToNextLevel": 1800,
    "levelProgressPercent": 72.2,
    "currentStreak": 8,
    "maxStreak": 21,
    "lastCheckInDate": "2026-07-24",
    "checkedInToday": true,
    "makeupCardsRemaining": 3,
    "totalReviewCount": 156,
    "totalCorrectCount": 132,
    "totalNodeCount": 48,
    "accuracyRate": 84.6,
    "nextAchievement": {
      "id": 12,
      "code": "STREAK_30",
      "name": "坚持三十天",
      "description": "连续签到 30 天",
      "icon": "🔥",
      "category": "streak",
      "tier": "gold",
      "triggerValue": 30,
      "pointsReward": 300,
      "makeupCardReward": 1,
      "currentValue": 8,
      "progressPercent": 26,
      "unlocked": false,
      "unlockedAt": null
    },
    "newAchievements": [],
    "pointsEarned": 20
  }
}
```
- **是否需要登录认证**：是

---

#### 9.3 成就列表

- **接口名称**：获取成就列表
- **请求方法和路径**：GET /api/gamification/achievements
- **接口描述**：获取全部成就列表，并附带当前用户的进度信息（含是否已解锁、当前进度值、进度百分比、解锁时间），支持按分类和状态筛选。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| category | String | Query | 否 | 分类筛选，如 `streak`、`review`、`knowledge` 等 |
| filter | String | Query | 否 | 状态筛选：`all`（全部）/`unlocked`（已解锁）/`locked`（未解锁） |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
GET /api/gamification/achievements?category=streak&filter=locked
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 11,
      "code": "STREAK_7",
      "name": "坚持一周",
      "description": "连续签到 7 天",
      "icon": "⭐",
      "category": "streak",
      "tier": "bronze",
      "triggerValue": 7,
      "pointsReward": 100,
      "makeupCardReward": 0,
      "currentValue": 8,
      "progressPercent": 100,
      "unlocked": true,
      "unlockedAt": "2026-07-21T08:30:15"
    },
    {
      "id": 12,
      "code": "STREAK_30",
      "name": "坚持三十天",
      "description": "连续签到 30 天",
      "icon": "🔥",
      "category": "streak",
      "tier": "gold",
      "triggerValue": 30,
      "pointsReward": 300,
      "makeupCardReward": 1,
      "currentValue": 8,
      "progressPercent": 26,
      "unlocked": false,
      "unlockedAt": null
    }
  ]
}
```
- **是否需要登录认证**：是

---

#### 9.4 排行榜

- **接口名称**：获取排行榜
- **请求方法和路径**：GET /api/gamification/leaderboard
- **接口描述**：获取积分排行榜，支持按时间周期（日/周/月/全部）和领域标签筛选，返回前 N 名用户以及当前用户的排名信息（即使未上榜也会单独返回）。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| period | String | Query | 否 | 周期：`daily`/`weekly`/`monthly`/`all`，默认 `daily` |
| domain | String | Query | 否 | 领域标签名或 `all`，默认 `all` |
| size | Integer | Query | 否 | 返回条数，默认 `100` |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
GET /api/gamification/leaderboard?period=weekly&domain=all&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "entries": [
      {
        "rank": 1,
        "userId": 102,
        "username": "studyMaster",
        "avatar": "https://cdn.example.com/avatar/102.png",
        "score": 980,
        "level": 9
      },
      {
        "rank": 2,
        "userId": 105,
        "username": "alice",
        "avatar": "https://cdn.example.com/avatar/105.png",
        "score": 820,
        "level": 8
      },
      {
        "rank": 3,
        "userId": 100,
        "username": "bob",
        "avatar": "https://cdn.example.com/avatar/100.png",
        "score": 560,
        "level": 5
      }
    ],
    "currentUser": {
      "rank": 3,
      "userId": 100,
      "username": "bob",
      "avatar": "https://cdn.example.com/avatar/100.png",
      "score": 560,
      "level": 5
    }
  }
}
```
- **是否需要登录认证**：是

---

#### 9.5 补签

- **接口名称**：使用补签卡
- **请求方法和路径**：POST /api/gamification/makeup
- **接口描述**：使用补签卡为漏签日期补签，会消耗一张补签卡并发放对应积分，可能触发成就解锁。返回最新游戏化档案（含本次操作获得的积分和新解锁成就）。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
POST /api/gamification/makeup
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
Content-Type: application/json
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "补签成功",
  "data": {
    "totalPoints": 1320,
    "currentPoints": 600,
    "level": 5,
    "experience": 1320,
    "experienceToNextLevel": 1800,
    "levelProgressPercent": 77.8,
    "currentStreak": 8,
    "maxStreak": 21,
    "lastCheckInDate": "2026-07-24",
    "checkedInToday": true,
    "makeupCardsRemaining": 2,
    "totalReviewCount": 156,
    "totalCorrectCount": 132,
    "totalNodeCount": 48,
    "accuracyRate": 84.6,
    "nextAchievement": null,
    "newAchievements": [
      {
        "id": 25,
        "code": "MAKEUP_FIRST",
        "name": "亡羊补牢",
        "description": "首次使用补签卡",
        "icon": "🃏",
        "category": "makeup",
        "tier": "bronze",
        "triggerValue": 1,
        "pointsReward": 50,
        "makeupCardReward": 0,
        "currentValue": 1,
        "progressPercent": 100,
        "unlocked": true,
        "unlockedAt": "2026-07-24T10:12:33"
      }
    ],
    "pointsEarned": 20
  }
}
```
- **是否需要登录认证**：是

---

#### 9.6 积分记录

- **接口名称**：获取积分流水
- **请求方法和路径**：GET /api/gamification/points-log
- **接口描述**：分页查询当前用户的积分变动流水记录，包括签到、补签、成就解锁、复习等场景的积分增减。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| current | Integer | Query | 否 | 当前页，默认 `1` |
| size | Integer | Query | 否 | 每页大小，默认 `20` |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
GET /api/gamification/points-log?current=1&size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1001,
        "points": 20,
        "type": "check_in",
        "description": "每日签到 +20",
        "createTime": "2026-07-24T08:30:15"
      },
      {
        "id": 1000,
        "points": 50,
        "type": "achievement",
        "description": "解锁成就【坚持一周】 +50",
        "createTime": "2026-07-21T08:31:02"
      },
      {
        "id": 999,
        "points": 20,
        "type": "check_in",
        "description": "每日签到 +20",
        "createTime": "2026-07-23T08:28:11"
      }
    ],
    "total": 56,
    "size": 20,
    "current": 1,
    "pages": 3
  }
}
```
- **是否需要登录认证**：是

---

#### 9.7 连续打卡日历

- **接口名称**：获取签到热力图数据
- **请求方法和路径**：GET /api/gamification/streak-calendar
- **接口描述**：获取用户最近 N 个月内的每日签到与复习情况，用于前端绘制签到热力图/日历图。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| months | Integer | Query | 否 | 回溯月数，默认 `3` |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
GET /api/gamification/streak-calendar?months=3
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "date": "2026-07-24",
      "hasReview": true,
      "hasCheckIn": true,
      "pointsEarned": 20
    },
    {
      "date": "2026-07-23",
      "hasReview": false,
      "hasCheckIn": true,
      "pointsEarned": 20
    },
    {
      "date": "2026-07-22",
      "hasReview": true,
      "hasCheckIn": false,
      "pointsEarned": 0
    },
    {
      "date": "2026-07-21",
      "hasReview": true,
      "hasCheckIn": true,
      "pointsEarned": 70
    }
  ]
}
```
- **是否需要登录认证**：是

---

#### 9.8 学习统计概览

- **接口名称**：获取统计数据
- **请求方法和路径**：GET /api/statistics
- **接口描述**：获取当前用户在当前工作区下的学习统计数据，包括对话总数、知识点总数、待复习数量、已完成复习数量。任何一项子查询失败时该项返回 0，不影响其他字段。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |
| workspaceId | Long | Header | 否 | 工作区 ID（由拦截器从 header 或 token 中解析） |

- **请求示例**：
```http
GET /api/statistics
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "chatCount": 128,
    "knowledgeCount": 56,
    "pendingReviewCount": 12,
    "completedReviewCount": 144
  }
}
```
- **是否需要登录认证**：是

---

#### 9.9 学习统计图表

- **接口名称**：获取图表数据
- **请求方法和路径**：GET /api/statistics/chart
- **接口描述**：获取学习趋势图表数据，按时间周期返回对话数、知识点数、复习数三条数据序列及对应的横轴标签，用于前端绘制折线图/柱状图。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| period | String | Query | 否 | 时间周期：`week`-本周（近 7 天，按日）/`month`-本月（近 30 天，按日）/`year`-全年（近 12 个月，按月），默认 `week` |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |
| workspaceId | Long | Header | 否 | 工作区 ID（由拦截器从 header 或 token 中解析） |

- **请求示例**：
```http
GET /api/statistics/chart?period=week
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**（week 周期）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "labels": [
      "2026-07-18",
      "2026-07-19",
      "2026-07-20",
      "2026-07-21",
      "2026-07-22",
      "2026-07-23",
      "2026-07-24"
    ],
    "chatData": [12, 8, 15, 20, 18, 22, 33],
    "knowledgeData": [3, 1, 5, 4, 2, 6, 7],
    "reviewData": [10, 12, 8, 15, 9, 11, 14]
  }
}
```
- **是否需要登录认证**：是

---

### 第10章：通知与导出模块

#### 10.1 通知列表

- **接口名称**：通知列表
- **请求方法和路径**：GET /api/notification/list
- **接口描述**：分页查询当前用户的通知列表，包含广场互动通知（点赞、评论、举报结果）等。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| current | Integer | Query | 否 | 当前页，默认 `1` |
| size | Integer | Query | 否 | 每页大小，默认 `20` |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
GET /api/notification/list?current=1&size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 5001,
        "userId": 100,
        "type": "like",
        "title": "你的帖子收到了新的点赞",
        "content": "alice 赞了你的帖子【Java 并发编程要点】",
        "targetType": "post",
        "targetId": 88,
        "isRead": 0,
        "createdAt": "2026-07-24T09:15:22"
      },
      {
        "id": 5000,
        "userId": 100,
        "type": "comment",
        "title": "你的帖子收到了新评论",
        "content": "bob 评论：写得很清晰，感谢分享！",
        "targetType": "post",
        "targetId": 88,
        "isRead": 0,
        "createdAt": "2026-07-24T08:42:11"
      },
      {
        "id": 4998,
        "userId": 100,
        "type": "report_result",
        "title": "举报处理结果",
        "content": "你举报的帖子已被管理员移除",
        "targetType": "post",
        "targetId": 76,
        "isRead": 1,
        "createdAt": "2026-07-23T18:05:00"
      }
    ],
    "total": 32,
    "size": 20,
    "current": 1,
    "pages": 2
  }
}
```
- **是否需要登录认证**：是

---

#### 10.2 未读数量

- **接口名称**：未读通知数
- **请求方法和路径**：GET /api/notification/unread-count
- **接口描述**：获取当前用户的未读通知数量，常用于顶部铃铛红点展示。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
GET /api/notification/unread-count
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "unreadCount": 12
  }
}
```
- **是否需要登录认证**：是

---

#### 10.3 标记已读

- **接口名称**：标记已读
- **请求方法和路径**：PUT /api/notification/{id}/read
- **接口描述**：将指定 ID 的通知标记为已读，仅能标记属于当前用户的通知。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | Path | 是 | 通知 ID |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
PUT /api/notification/5001/read
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```
- **是否需要登录认证**：是

---

#### 10.4 全部已读

- **接口名称**：全部标记已读
- **请求方法和路径**：PUT /api/notification/read-all
- **接口描述**：将当前用户所有未读通知一次性标记为已读。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
PUT /api/notification/read-all
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```
- **是否需要登录认证**：是

---

#### 10.5 导出 Markdown

- **接口名称**：导出为 Markdown
- **请求方法和路径**：POST /api/export/markdown
- **接口描述**：将指定知识点（或当前用户/工作区下的全部知识点）导出为 Markdown 文件。请求体可为 `null` 或知识点 ID 数组；为空时导出全部。响应体为文件流，Content-Type 为 `text/markdown`，未登录时返回 HTTP 401。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| ids | Array&lt;Long&gt; | Body | 否 | 知识点 ID 列表，为空则导出当前用户工作区下全部知识点 |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |
| workspaceId | Long | Header | 否 | 工作区 ID（由拦截器从 header 或 token 中解析） |

- **请求示例**：
```http
POST /api/export/markdown
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
Content-Type: application/json

[101, 102, 105]
```
- **响应示例**：成功时为二进制文件流：
```
HTTP/1.1 200 OK
Content-Type: text/markdown
Content-Disposition: attachment; filename="knowledge.md"

# Java 并发编程要点
...
```
未登录时：
```
HTTP/1.1 401 Unauthorized
```
- **是否需要登录认证**：是

---

#### 10.6 导出 PDF

- **接口名称**：导出为 PDF
- **请求方法和路径**：POST /api/export/pdf
- **接口描述**：将指定知识点（或当前用户/工作区下的全部知识点）导出为 PDF 文件。请求体可为 `null` 或知识点 ID 数组；为空时导出全部。响应体为二进制文件流，Content-Type 为 `application/pdf`，未登录时返回 HTTP 401。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| ids | Array&lt;Long&gt; | Body | 否 | 知识点 ID 列表，为空则导出当前用户工作区下全部知识点 |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |
| workspaceId | Long | Header | 否 | 工作区 ID（由拦截器从 header 或 token 中解析） |

- **请求示例**：
```http
POST /api/export/pdf
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
Content-Type: application/json

[101, 102, 105]
```
- **响应示例**：成功时为二进制文件流：
```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="knowledge.pdf"

%PDF-1.7
...
```
未登录时：
```
HTTP/1.1 401 Unauthorized
```
- **是否需要登录认证**：是

---

#### 10.7 导出 Word

- **接口名称**：导出为 Word
- **请求方法和路径**：POST /api/export/word
- **接口描述**：将指定知识点（或当前用户/工作区下的全部知识点）导出为 Word 文档。请求体可为 `null` 或知识点 ID 数组；为空时导出全部。响应体为二进制文件流，Content-Type 为 `application/vnd.openxmlformats-officedocument.wordprocessingml.document`，未登录时返回 HTTP 401。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| ids | Array&lt;Long&gt; | Body | 否 | 知识点 ID 列表，为空则导出当前用户工作区下全部知识点 |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |
| workspaceId | Long | Header | 否 | 工作区 ID（由拦截器从 header 或 token 中解析） |

- **请求示例**：
```http
POST /api/export/word
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
Content-Type: application/json

[101, 102, 105]
```
- **响应示例**：成功时为二进制文件流：
```
HTTP/1.1 200 OK
Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document
Content-Disposition: attachment; filename="knowledge.docx"

(二进制流)
```
未登录时：
```
HTTP/1.1 401 Unauthorized
```
- **是否需要登录认证**：是

---

#### 10.8 导出 JSON

- **接口名称**：导出为 JSON
- **请求方法和路径**：POST /api/export/json
- **接口描述**：将指定知识点（或当前用户/工作区下的全部知识点）导出为 JSON 文件。请求体可为 `null` 或知识点 ID 数组；为空时导出全部。响应体为 JSON 文件流，Content-Type 为 `application/json`，未登录时返回 HTTP 401。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| ids | Array&lt;Long&gt; | Body | 否 | 知识点 ID 列表，为空则导出当前用户工作区下全部知识点 |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |
| workspaceId | Long | Header | 否 | 工作区 ID（由拦截器从 header 或 token 中解析） |

- **请求示例**：
```http
POST /api/export/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
Content-Type: application/json

[101, 102, 105]
```
- **响应示例**：成功时为 JSON 文件流：
```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Disposition: attachment; filename="knowledge.json"

[
  {
    "id": 101,
    "title": "Java 并发编程要点",
    "content": "..."
  },
  {
    "id": 102,
    "title": "Spring Boot 启动流程",
    "content": "..."
  }
]
```
未登录时：
```
HTTP/1.1 401 Unauthorized
```
- **是否需要登录认证**：是

---

#### 10.9 导出 CSV

- **接口名称**：导出为 CSV
- **请求方法和路径**：POST /api/export/csv
- **接口描述**：将指定知识点（或当前用户/工作区下的全部知识点）导出为 CSV 文件。请求体可为 `null` 或知识点 ID 数组；为空时导出全部。响应体为 CSV 文件流，Content-Type 为 `text/csv`，未登录时返回 HTTP 401。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| ids | Array&lt;Long&gt; | Body | 否 | 知识点 ID 列表，为空则导出当前用户工作区下全部知识点 |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |
| workspaceId | Long | Header | 否 | 工作区 ID（由拦截器从 header 或 token 中解析） |

- **请求示例**：
```http
POST /api/export/csv
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
Content-Type: application/json

[101, 102, 105]
```
- **响应示例**：成功时为 CSV 文件流：
```
HTTP/1.1 200 OK
Content-Type: text/csv
Content-Disposition: attachment; filename="knowledge.csv"

id,title,content
101,Java 并发编程要点,...
102,Spring Boot 启动流程,...
```
未登录时：
```
HTTP/1.1 401 Unauthorized
```
- **是否需要登录认证**：是

---

### 第11章：向量、系统与管理模块

#### 11.1 重新生成向量

- **接口名称**：重新生成向量
- **请求方法和路径**：POST /api/vector/regenerate/{knowledgeId}
- **接口描述**：为指定知识节点重新生成向量（向量化会异步执行），用于知识内容更新后重建向量索引。提交后立即返回任务受理结果。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| knowledgeId | Long | Path | 是 | 知识节点 ID |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
POST /api/vector/regenerate/101
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "向量重新生成任务已提交",
  "data": null
}
```
- **是否需要登录认证**：是

---

#### 11.2 批量生成向量

- **接口名称**：批量生成向量
- **请求方法和路径**：POST /api/vector/batch-generate
- **接口描述**：为当前用户的所有知识节点批量生成向量（异步执行），常用于首次接入向量检索或向量索引重建。提交后立即返回任务受理结果。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
POST /api/vector/batch-generate
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "批量向量生成任务已提交",
  "data": null
}
```
- **是否需要登录认证**：是

---

#### 11.3 查询异步任务状态

- **接口名称**：查询任务状态
- **请求方法和路径**：GET /api/async-task/status/{taskNumber}
- **接口描述**：根据任务编号查询异步任务的执行状态、进度、结果或错误信息。任务不存在时返回 500 错误。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| taskNumber | String | Path | 是 | 任务编号 |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>` |

- **请求示例**：
```http
GET /api/async-task/status/TASK-20260724-0001
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMH0.xxx
```
- **响应示例**（成功）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "TASK-20260724-0001",
    "status": "COMPLETED",
    "taskType": "vector_generate",
    "createTime": "2026-07-24T10:00:00",
    "completeTime": "2026-07-24T10:02:35",
    "progress": 100,
    "result": {
      "successCount": 48,
      "failCount": 0
    },
    "errorMessage": null
  }
}
```
- **响应示例**（任务不存在）：
```json
{
  "code": 500,
  "message": "任务不存在",
  "data": null
}
```
- **是否需要登录认证**：是

---

#### 11.4 健康检查

- **接口名称**：健康检查
- **请求方法和路径**：GET /api/health
- **接口描述**：检查系统运行状态，返回服务名、状态和时间戳。可用于负载均衡、K8s 探针或监控告警调用。该接口无需登录认证。
- **请求参数**：无

- **请求示例**：
```http
GET /api/health
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "UP",
    "timestamp": "2026-07-24T10:15:30.123",
    "service": "AI-SecondBrain"
  }
}
```
- **是否需要登录认证**：否

---

#### 11.5 测试 DeerFlow 连接

- **接口名称**：测试 DeerFlow 连接
- **请求方法和路径**：GET /api/test/deerflow-connection
- **接口描述**：测试后端与 DeerFlow 研究服务的连通性。直接返回 Map 结构（非统一 Result 包装），包含是否成功、是否健康及提示信息。
- **请求参数**：无

- **请求示例**：
```http
GET /api/test/deerflow-connection
```
- **响应示例**（连接正常）：
```json
{
  "success": true,
  "healthy": true,
  "message": "DeerFlow API连接正常"
}
```
- **响应示例**（连接失败）：
```json
{
  "success": true,
  "healthy": false,
  "message": "DeerFlow API连接失败"
}
```
- **是否需要登录认证**：否（该接口为测试接口，路径无显式认证要求，实际是否拦截取决于拦截器配置）

---

#### 11.6 管理统计

- **接口名称**：平台统计
- **请求方法和路径**：GET /api/admin/statistics
- **接口描述**：获取平台全局统计数据，包括用户总数、工作区总数、知识点总数。仅 Super Admin 角色可访问。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>`（需 Super Admin 角色） |

- **请求示例**：
```http
GET /api/admin/statistics
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userCount": 1280,
    "workspaceCount": 365,
    "knowledgeCount": 0
  }
}
```
- **响应示例**（无权限）：
```json
{
  "code": 403,
  "message": "仅超级管理员可访问",
  "data": null
}
```
- **是否需要登录认证**：是（且需要 Super Admin 角色）

---

#### 11.7 用户列表

- **接口名称**：用户列表
- **请求方法和路径**：GET /api/admin/users
- **接口描述**：分页查询所有未删除用户列表，按创建时间倒序排列。返回数据中已过滤掉 password 字段。仅 Super Admin 角色可访问。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| current | Integer | Query | 否 | 当前页，默认 `1` |
| size | Integer | Query | 否 | 每页大小，默认 `10` |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>`（需 Super Admin 角色） |

- **请求示例**：
```http
GET /api/admin/users?current=1&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 100,
        "username": "bob",
        "password": null,
        "email": "bob@example.com",
        "phone": "13800000001",
        "bio": "学习使我快乐",
        "avatar": "https://cdn.example.com/avatar/100.png",
        "apiKey": "sk-xxx",
        "registerTime": "2026-06-01T09:00:00",
        "lastLoginTime": "2026-07-24T08:30:00",
        "createTime": "2026-06-01T09:00:00",
        "updateTime": "2026-07-24T08:30:00",
        "deleted": 0,
        "role": "user",
        "status": 1
      },
      {
        "id": 1,
        "username": "admin",
        "password": null,
        "email": "admin@example.com",
        "phone": "13800000000",
        "bio": "平台管理员",
        "avatar": "https://cdn.example.com/avatar/1.png",
        "apiKey": null,
        "registerTime": "2026-01-01T00:00:00",
        "lastLoginTime": "2026-07-24T07:00:00",
        "createTime": "2026-01-01T00:00:00",
        "updateTime": "2026-07-24T07:00:00",
        "deleted": 0,
        "role": "super_admin",
        "status": 1
      }
    ],
    "total": 1280,
    "size": 10,
    "current": 1,
    "pages": 128
  }
}
```
- **是否需要登录认证**：是（且需要 Super Admin 角色）

---

#### 11.8 禁用用户

- **接口名称**：禁用/启用用户
- **请求方法和路径**：PUT /api/admin/users/{id}/disable
- **接口描述**：禁用或启用指定用户。`status=1` 表示启用，`status=0` 表示禁用。仅 Super Admin 角色可访问。用户不存在时返回 404。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | Path | 是 | 用户 ID |
| status | Integer | Query | 是 | 状态：`1`-启用，`0`-禁用 |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>`（需 Super Admin 角色） |

- **请求示例**：
```http
PUT /api/admin/users/100/disable?status=0
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "用户已禁用",
  "data": null
}
```
- **响应示例**（用户不存在）：
```json
{
  "code": 404,
  "message": "用户不存在",
  "data": null
}
```
- **是否需要登录认证**：是（且需要 Super Admin 角色）

---

#### 11.9 工作区列表

- **接口名称**：工作区列表
- **请求方法和路径**：GET /api/admin/workspaces
- **接口描述**：分页查询所有未删除工作区列表，按创建时间倒序排列。仅 Super Admin 角色可访问。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| current | Integer | Query | 否 | 当前页，默认 `1` |
| size | Integer | Query | 否 | 每页大小，默认 `10` |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>`（需 Super Admin 角色） |

- **请求示例**：
```http
GET /api/admin/workspaces?current=1&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "Bob 的个人空间",
        "description": "默认工作区",
        "ownerId": 100,
        "status": 1,
        "createTime": "2026-06-01T09:00:00",
        "updateTime": "2026-06-01T09:00:00",
        "deleted": 0
      },
      {
        "id": 2,
        "name": "团队协作空间",
        "description": "项目协作",
        "ownerId": 1,
        "status": 1,
        "createTime": "2026-05-15T14:20:00",
        "updateTime": "2026-07-10T11:00:00",
        "deleted": 0
      }
    ],
    "total": 365,
    "size": 10,
    "current": 1,
    "pages": 37
  }
}
```
- **是否需要登录认证**：是（且需要 Super Admin 角色）

---

#### 11.10 禁用工作区

- **接口名称**：禁用/启用工作区
- **请求方法和路径**：PUT /api/admin/workspaces/{id}/disable
- **接口描述**：禁用或启用指定工作区。`status=1` 表示启用，`status=0` 表示禁用。仅 Super Admin 角色可访问。工作区不存在时返回 404。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | Path | 是 | 工作区 ID |
| status | Integer | Query | 是 | 状态：`1`-启用，`0`-禁用 |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>`（需 Super Admin 角色） |

- **请求示例**：
```http
PUT /api/admin/workspaces/2/disable?status=0
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "工作区已禁用",
  "data": null
}
```
- **响应示例**（工作区不存在）：
```json
{
  "code": 404,
  "message": "工作区不存在",
  "data": null
}
```
- **是否需要登录认证**：是（且需要 Super Admin 角色）

---

#### 11.11 举报列表

- **接口名称**：举报列表
- **请求方法和路径**：GET /api/admin/reports
- **接口描述**：分页查询广场帖子举报记录，支持按处理状态过滤。仅 Super Admin 角色可访问。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| status | String | Query | 否 | 过滤状态：`pending`（待处理）/`ignored`（已忽略）/`removed`（已移除），默认 `pending` |
| current | Integer | Query | 否 | 当前页，默认 `1` |
| size | Integer | Query | 否 | 每页大小，默认 `10` |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>`（需 Super Admin 角色） |

- **请求示例**：
```http
GET /api/admin/reports?status=pending&current=1&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 2001,
        "postId": 88,
        "postTitle": "Java 并发编程要点",
        "reporterId": 105,
        "reporterName": "alice",
        "reason": "内容含有不当言论",
        "status": "pending",
        "handlerId": null,
        "handleNote": null,
        "createdAt": "2026-07-23T20:15:00",
        "handledAt": null
      },
      {
        "id": 2002,
        "postId": 92,
        "postTitle": "机器学习入门",
        "reporterId": 100,
        "reporterName": "bob",
        "reason": "广告推广",
        "status": "pending",
        "handlerId": null,
        "handleNote": null,
        "createdAt": "2026-07-24T09:30:00",
        "handledAt": null
      }
    ],
    "total": 18,
    "size": 10,
    "current": 1,
    "pages": 2
  }
}
```
- **是否需要登录认证**：是（且需要 Super Admin 角色）

---

#### 11.12 处理举报

- **接口名称**：处理举报
- **请求方法和路径**：PUT /api/admin/reports/{id}/handle
- **接口描述**：处理指定的举报记录，可选择忽略（ignore）或移除被举报内容（remove），并可填写处理备注。处理人会记录为当前登录用户。仅 Super Admin 角色可访问。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | Path | 是 | 举报 ID |
| action | String | Body | 是 | 处理方式：`ignore`（忽略）或 `remove`（移除） |
| handleNote | String | Body | 否 | 处理备注，最长 500 字 |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>`（需 Super Admin 角色） |

- **请求示例**：
```http
PUT /api/admin/reports/2001/handle
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
Content-Type: application/json

{
  "action": "remove",
  "handleNote": "经核实内容违规，已移除帖子"
}
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "处理完成",
  "data": null
}
```
- **响应示例**（参数校验失败）：
```json
{
  "code": 500,
  "message": "处理方式不能为空",
  "data": null
}
```
- **是否需要登录认证**：是（且需要 Super Admin 角色）

---

#### 11.13 敏感词列表

- **接口名称**：敏感词列表
- **请求方法和路径**：GET /api/admin/sensitive-words
- **接口描述**：查询全部敏感词列表，用于广场发布与评论的自动过滤。仅 Super Admin 角色可访问。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>`（需 Super Admin 角色） |

- **请求示例**：
```http
GET /api/admin/sensitive-words
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "word": "违禁词1",
      "createdAt": "2026-01-01T00:00:00"
    },
    {
      "id": 2,
      "word": "违禁词2",
      "createdAt": "2026-03-15T10:30:00"
    },
    {
      "id": 3,
      "word": "广告链接",
      "createdAt": "2026-06-20T14:00:00"
    }
  ]
}
```
- **是否需要登录认证**：是（且需要 Super Admin 角色）

---

#### 11.14 添加敏感词

- **接口名称**：添加敏感词
- **请求方法和路径**：POST /api/admin/sensitive-words
- **接口描述**：向敏感词库中添加新的敏感词，添加后将自动应用于广场发布与评论的过滤。仅 Super Admin 角色可访问。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| word | String | Query | 是 | 敏感词 |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>`（需 Super Admin 角色） |

- **请求示例**：
```http
POST /api/admin/sensitive-words?word=新违禁词
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "敏感词已添加",
  "data": null
}
```
- **是否需要登录认证**：是（且需要 Super Admin 角色）

---

#### 11.15 删除敏感词

- **接口名称**：删除敏感词
- **请求方法和路径**：DELETE /api/admin/sensitive-words/{id}
- **接口描述**：根据敏感词 ID 删除敏感词，删除后不再用于过滤。仅 Super Admin 角色可访问。
- **请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| id | Long | Path | 是 | 敏感词 ID |
| Authorization | String | Header | 是 | 认证 Token，格式：`Bearer <token>`（需 Super Admin 角色） |

- **请求示例**：
```http
DELETE /api/admin/sensitive-words/3
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
```
- **响应示例**：
```json
{
  "code": 200,
  "message": "敏感词已删除",
  "data": null
}
```
- **是否需要登录认证**：是（且需要 Super Admin 角色）

---

### 第十章：知识社区问答

知识社区问答接口统一使用 `/api/community/questions` 前缀并需要登录认证。回答引用知识点时，服务端只接受当前用户拥有的知识点，并在发布时保存公开快照，避免后续私有编辑静默改变已发布回答。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/community/questions` | 发布问题，标题 5—120 字、正文 10—5000 字、最多 5 个标签 |
| GET | `/api/community/questions` | 问题分页；支持 `sort=newest/unanswered`、`keyword`、`current`、`size` |
| GET | `/api/community/questions/{id}` | 问题详情、回答及知识点快照 |
| POST | `/api/community/questions/{id}/answers` | 发布回答，正文 10—10000 字，可携带最多 5 个 `knowledgeNodeIds` |
| POST | `/api/community/questions/{id}/answers/{answerId}/accept` | 问题作者采纳回答，每个问题只能采纳一次 |

社区人物关系接口统一使用 `/api/community/users` 前缀并需要登录认证。公开 Profile VO 不返回邮箱、手机号、密码、API Key 等账户敏感字段。`isBlocked` 表示任一方向存在拉黑，`isBlockedByMe` 与 `isBlockedByTarget` 用于正确呈现解除拉黑能力。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/community/users/{userId}` | 公开主页、关系状态、贡献统计及最近公开内容 |
| PUT | `/api/community/users/me/profile` | 更新本人公开简介与擅长领域；最多 8 个标签，每个不超过 30 字 |
| POST | `/api/community/users/{userId}/follow` | 关注用户；禁止自关注，重复关注幂等，拉黑期间禁止关注 |
| DELETE | `/api/community/users/{userId}/follow` | 取消关注，重复操作幂等 |
| GET | `/api/community/users/{userId}/followers` | 粉丝分页；`size` 最大为 50 |
| GET | `/api/community/users/{userId}/following` | 关注列表分页；`size` 最大为 50 |
| POST | `/api/community/users/{userId}/block` | 拉黑用户并删除双方关注关系 |
| DELETE | `/api/community/users/{userId}/block` | 解除当前用户发起的拉黑 |

**发布问题示例**

```json
{
  "title": "如何准备第一次大学生程序设计竞赛？",
  "content": "我刚学完 Java 基础，希望了解组队、刷题顺序和赛前准备。",
  "tags": ["竞赛", "编程", "Java"]
}
```

**发布回答示例**

```json
{
  "content": "建议先建立基础题型清单，再进行限时训练……",
  "knowledgeNodeIds": [101, 108]
}
```

---

## 变更记录

| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|----------|
| V1.0 | 2026-07-24 | AI-SecondBrain Team | 初始版本，合并 4 个 API 文档片段，涵盖 10 个模块共 129 个接口 |
| V2.0 | 2026-07-29 | AI-SecondBrain Team | 新增研究项目模块(28)、会话管理(4)、待确认知识点(4)、标签树(3)、题目池(5)、流式问答(1)、AI服务商(2)、用户AI配置(3)接口 |
