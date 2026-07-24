> 本篇目标：带你从零开始，按照 AI-SecondBrain 的代码规范，完整接入一个新的业务模块，从数据库表到前端页面，每一步都有可直接套用的代码模板。

# 07 - 对接指南：新模块接入完整流程

本文以**通知中心模块（Notification）**为例，演示从 0 到 1 接入一个新业务模块的完整步骤。你可以照着这套流程，把 "Notification" 替换成你自己的模块名，快速接入新功能。

---

## 第一步：理解三层架构（概念层）

AI-SecondBrain 采用经典的三层架构，每层职责单一，从上到下调用，从下到上返回：

```
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│     Controller      │────▶│      Service        │────▶│       Mapper        │
│     （HTTP层）       │     │     （业务层）       │     │    （数据访问层）    │
└─────────────────────┘     └─────────────────────┘     └─────────────────────┘
          │                           │                           │
          ▼                           ▼                           ▼
    接收请求参数                  业务逻辑处理                  数据库操作
    参数校验                      调用 Mapper                   CRUD + 复杂查询
    返回统一 Result               事务管理                      分页/排序
                                  权限校验
```

**三个核心概念**：

| 层级 | 职责 | 类比 |
|------|------|------|
| Controller | 管"协议"——HTTP 请求怎么进来、怎么出去 | 餐厅的前台服务员，接客、传菜、结账 |
| Service | 管"业务"——核心逻辑在这里 | 餐厅的后厨，做菜、调味、出品质控 |
| Mapper | 管"数据"——和数据库打交道 | 餐厅的仓库管理员，取食材、存食材 |

❌ **没有框架的写法**：所有逻辑挤在一个类里，SQL 拼接到处飞，改需求牵一发动全身。

✅ **用框架的写法**：Controller 只收参返回，Service 只做业务，Mapper 只碰数据库，各司其职，改哪层都不影响其他层。

---

## 第二步：数据库表设计

先有表，再有代码。表设计好了，后面的代码都是围绕表来写的。

以通知表为例，建表 SQL 放在 `sql/` 目录下，命名格式 `V{版本号}__{描述}.sql`：

```sql
-- sql/V5__add_notification_table.sql

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '接收通知的用户ID',
    type VARCHAR(30) NOT NULL COMMENT '通知类型：like/comment/report_result',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content VARCHAR(500) COMMENT '通知内容',
    target_type VARCHAR(30) COMMENT '关联目标类型：post',
    target_id BIGINT COMMENT '关联目标ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知';
```

**设计规范速查**：

| 字段 | 要求 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | 统一自增主键 |
| created_at | DATETIME NOT NULL | 创建时间（注意：本项目部分表用 createdAt 驼峰命名，看具体业务） |
| updated_at | DATETIME | 更新时间（如果需要自动填充） |
| deleted | TINYINT DEFAULT 0 | 逻辑删除标记（需要的表加） |
| 索引 | 业务查询字段加索引 | 如 user_id、is_read 等常用查询条件 |

> **命名约定**：表名用下划线小写（如 `notification`），字段名也用下划线小写（如 `user_id`），Java 实体类里自动转驼峰。

---

## 第三步：定义 Entity 实体类

Entity 是数据库表在 Java 里的映射，一张表对应一个 Entity 类。

**路径**：`backend/src/main/java/com/secondbrain/entity/Notification.java`

```java
package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 通知实体.
 *
 * <p>用于广场互动通知（点赞、评论、举报结果），与邮件通知服务分离。</p>
 */
@Getter
@Setter
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String type;

    private String title;

    private String content;

    private String targetType;

    private Long targetId;

    private Integer isRead;

    private LocalDateTime createdAt;
}
```

**注解速查**：

| 注解 | 作用 | 示例 |
|------|------|------|
| `@TableName("xxx")` | 指定对应的数据库表名 | `@TableName("notification")` |
| `@Getter @Setter` | Lombok 自动生成 getter/setter | 写在类上 |
| `@TableId(type = IdType.AUTO)` | 标记主键，自增策略 | 写在 id 字段上 |
| `@TableField(fill = FieldFill.INSERT)` | 插入时自动填充 | createTime 字段用 |
| `@TableField(fill = FieldFill.INSERT_UPDATE)` | 插入和更新时自动填充 | updateTime 字段用 |
| `@TableLogic` | 逻辑删除标记 | deleted 字段用 |

> **注意**：本项目的 Notification 表使用 `createdAt` 而不是自动填充的 `createTime`，因为通知的创建时间是业务逻辑里手动设置的。如果你的表需要自动填充时间，参考 `KnowledgeNode` 实体的写法。

---

## 第四步：定义 Mapper 接口

Mapper 是数据访问层，负责和数据库交互。得益于 MyBatis-Plus，基础 CRUD 不用写 SQL。

**路径**：`backend/src/main/java/com/secondbrain/mapper/NotificationMapper.java`

```java
package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知 Mapper.
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
```

就这么简单！继承 `BaseMapper<Entity>` 后，自动拥有：

- `insert(entity)` - 插入
- `deleteById(id)` - 按 ID 删除
- `updateById(entity)` - 按 ID 更新
- `selectById(id)` - 按 ID 查询
- `selectList(wrapper)` - 条件查询列表
- `selectPage(page, wrapper)` - 分页查询
- `selectCount(wrapper)` - 统计数量

**需要复杂查询时**，可以在 Mapper 接口里加方法，配合 XML 或注解写 SQL：

```java
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    
    // 注解方式（简单 SQL）
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    long countUnreadByUserId(Long userId);
}
```

---

## 第五步：定义 DTO / VO

DTO 和 VO 都是数据传输对象，但用途不同：

| 类型 | 用途 | 放哪儿 |
|------|------|--------|
| DTO（Data Transfer Object） | 请求参数——前端传进来的数据 | `dto/` 目录 |
| VO（View Object） | 响应对象——返回给前端的数据 | `vo/` 目录 |

**为什么不直接用 Entity？**
- Entity 是数据库映射，字段和表一一对应
- 前端可能不需要所有字段（比如密码、内部状态）
- 前端可能需要组合字段（比如用户信息 + 积分信息）
- 接口参数和表字段不一定对应（比如查询条件是时间范围）

### DTO 示例：查询条件

**路径**：`backend/src/main/java/com/secondbrain/dto/NotificationQueryDTO.java`

```java
package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 通知查询请求.
 */
@Getter
@Setter
public class NotificationQueryDTO {
    
    private Integer current = 1;
    
    private Integer size = 20;
    
    private String type;
    
    private Integer isRead;
}
```

### VO 示例：返回给前端的通知数据

**路径**：`backend/src/main/java/com/secondbrain/vo/NotificationVO.java`

```java
package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 通知 VO.
 */
@Getter
@Setter
public class NotificationVO {
    
    private Long id;
    
    private String type;
    
    private String title;
    
    private String content;
    
    private String targetType;
    
    private Long targetId;
    
    private Integer isRead;
    
    private LocalDateTime createdAt;
}
```

> **经验法则**：简单的接口可以直接用 Entity 返回，但只要有字段裁剪、字段组合、字段重命名的需求，就上 VO。本项目的 Knowledge 模块就用了 `KnowledgeNodeVO`。

---

## 第六步：定义 Service 接口 + 实现

Service 是业务逻辑层，核心的业务规则都在这里。接口定义契约，实现类写具体逻辑。

### 6.1 Service 接口

**路径**：`backend/src/main/java/com/secondbrain/service/NotificationService.java`

```java
package com.secondbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.entity.Notification;

/**
 * 通知服务接口.
 */
public interface NotificationService {

    /**
     * 分页查询通知列表.
     *
     * @param userId  用户ID
     * @param current 当前页
     * @param size    每页大小
     * @return 通知分页数据
     */
    IPage<Notification> list(Long userId, Integer current, Integer size);

    /**
     * 获取未读通知数.
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    long getUnreadCount(Long userId);

    /**
     * 标记单条通知为已读.
     *
     * @param id     通知ID
     * @param userId 用户ID
     */
    void markAsRead(Long id, Long userId);

    /**
     * 全部标记为已读.
     *
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 创建通知.
     *
     * @param userId   接收用户ID
     * @param type     通知类型
     * @param title    标题
     * @param content  内容
     * @param targetType 关联目标类型
     * @param targetId   关联目标ID
     */
    void create(Long userId, String type, String title, String content,
                String targetType, Long targetId);
}
```

### 6.2 Service 实现类

**路径**：`backend/src/main/java/com/secondbrain/service/impl/NotificationServiceImpl.java`

```java
package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.entity.Notification;
import com.secondbrain.mapper.NotificationMapper;
import com.secondbrain.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 通知服务实现.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public IPage<Notification> list(Long userId, Integer current, Integer size) {
        Page<Notification> page = new Page<>(current, size);
        
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.orderByDesc(Notification::getCreatedAt);
        
        return notificationMapper.selectPage(page, wrapper);
    }

    @Override
    public long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new IllegalStateException("通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalStateException("无权操作此通知");
        }
        
        Notification update = new Notification();
        update.setId(id);
        update.setIsRead(1);
        notificationMapper.updateById(update);
        
        log.info("标记通知已读，id：{}，userId：{}", id, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.eq(Notification::getIsRead, 0);
        
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update, wrapper);
        
        log.info("全部标记已读，userId：{}", userId);
    }

    @Override
    public void create(Long userId, String type, String title, String content,
                       String targetType, Long targetId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        
        log.info("创建通知，userId：{}，type：{}", userId, type);
    }
}
```

**Service 层写法规约**：

| 规范 | 说明 |
|------|------|
| `@Service` | 实现类加这个注解，Spring 自动扫描 |
| 构造器注入 | 不用 `@Autowired` 字段注入，用构造器注入，方便单测 |
| `LambdaQueryWrapper` | 条件查询用这个，类型安全，避免写字段名字符串 |
| 业务校验 | 数据存在性、权限校验在 Service 层做，抛 `IllegalStateException` |
| log 日志 | 关键操作打日志，用 SLF4J |

---

## 第七步：写 Controller

Controller 是 HTTP 接口层，负责接收请求、调用 Service、返回统一结果。

**路径**：`backend/src/main/java/com/secondbrain/controller/NotificationController.java`

```java
package com.secondbrain.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.common.Result;
import com.secondbrain.entity.Notification;
import com.secondbrain.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知控制器.
 *
 * <p>提供通知列表、未读计数和已读标记接口。</p>
 */
@RestController
@RequestMapping("/notification")
@Tag(name = "通知中心", description = "广场互动通知")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    @Operation(summary = "通知列表")
    public Result<IPage<Notification>> list(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(notificationService.list(userId, current, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数")
    public Result<Map<String, Long>> unreadCount(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        long count = notificationService.getUnreadCount(userId);
        return Result.success(Map.of("unreadCount", count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记已读")
    public Result<Void> markAsRead(@Parameter(description = "通知ID") @PathVariable Long id,
                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        notificationService.markAsRead(id, userId);
        return Result.success(null);
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public Result<Void> markAllAsRead(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        notificationService.markAllAsRead(userId);
        return Result.success(null);
    }
}
```

**Controller 层写法规约**：

| 规范 | 说明 |
|------|------|
| `@RestController` | 等于 `@Controller` + `@ResponseBody`，返回 JSON |
| `@RequestMapping("/xxx")` | 类上的基础路径，一般和模块名一致 |
| `@Tag(name = "...")` | Knife4j 接口文档分组 |
| `@Operation(summary = "...")` | 每个接口的文档说明 |
| `Result<T>` | 统一返回格式，成功用 `Result.success(data)`，失败用 `Result.error(msg)` |
| `HttpServletRequest` | 从 request 里取 `userId`——JWT 拦截器已经放进去了 |
| `request.getAttribute("workspaceId")` | 工作区级别的模块，从这里取 workspaceId 做数据隔离 |

> **userId 从哪来？** JWT 拦截器（`JwtInterceptor`）会解析请求头里的 token，把 userId 放到 request 的 attribute 里。你直接取就行，不用自己解析 token。

---

## 第八步：前端 API 封装

前端的 API 调用统一封装在 `frontend/src/api/` 目录下，每个模块一个文件，内部调用 `request.js`。

**路径**：`frontend/src/api/notification.js`

```javascript
import request from "@/utils/request";

export const notificationAPI = {
  getList(params) {
    return request({ url: "/notification/list", method: "get", params });
  },

  getUnreadCount() {
    return request({ url: "/notification/unread-count", method: "get" });
  },

  markAsRead(id) {
    return request({ url: `/notification/${id}/read`, method: "put" });
  },

  markAllAsRead() {
    return request({ url: "/notification/read-all", method: "put" });
  },
};
```

**调用方式**：

```javascript
import { notificationAPI } from "@/api/notification";

// 调用
const data = await notificationAPI.getList({ current: 1, size: 20 });
```

**request.js 做了什么？**
- 自动加请求前缀（`/api`）
- 自动从 localStorage 取 token 放到请求头
- 自动处理响应，把 `data` 层直接返回（`response.data.data`）
- 自动处理 401 跳登录
- 统一错误提示

---

## 第九步：前端页面

页面放在 `frontend/src/views/` 目录下，使用 Vue 3 + `<script setup>` 语法，UI 组件用 Element Plus。

**路径**：`frontend/src/views/Notifications.vue`

```vue
<template>
  <div class="notifications-page">
    <div class="main-content">
      <div class="page-header">
        <div class="header-info">
          <h1 class="page-title">通知中心</h1>
        </div>
        <el-button v-if="unreadCount > 0" text @click="handleMarkAllRead">
          全部标记已读
        </el-button>
      </div>

      <div v-loading="loading" class="notification-list">
        <div v-if="!loading && list.length === 0" class="empty-state">
          <el-empty description="暂无通知" />
        </div>

        <div
          v-for="item in list"
          :key="item.id"
          class="notification-item"
          :class="{ unread: item.isRead === 0 }"
          @click="handleClick(item)"
        >
          <div class="notification-dot" v-if="item.isRead === 0"></div>
          <div class="notification-body">
            <p class="notification-title">{{ item.title }}</p>
            <p v-if="item.content" class="notification-content">{{ item.content }}</p>
            <span class="notification-time">{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>
      </div>

      <div v-if="pagination.total > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { notificationAPI } from "@/api/notification";

const router = useRouter();

const loading = ref(false);
const list = ref([]);
const unreadCount = ref(0);
const pagination = reactive({ current: 1, size: 20, total: 0 });

const loadList = async () => {
  loading.value = true;
  try {
    const data = await notificationAPI.getList({
      current: pagination.current,
      size: pagination.size,
    });
    list.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error("加载通知失败");
  } finally {
    loading.value = false;
  }
};

const handleClick = async (item) => {
  if (item.isRead === 0) {
    try {
      await notificationAPI.markAsRead(item.id);
      item.isRead = 1;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    } catch {
      // ignore
    }
  }
  if (item.targetType === "post" && item.targetId) {
    router.push("/square");
  }
};

const handleMarkAllRead = async () => {
  try {
    await notificationAPI.markAllAsRead();
    list.value.forEach((item) => (item.isRead = 1));
    unreadCount.value = 0;
    ElMessage.success("已全部标记已读");
  } catch (error) {
    ElMessage.error("操作失败");
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

onMounted(() => {
  loadList();
});
</script>

<style scoped>
.notifications-page {
  min-height: 100%;
  background: var(--bg-page);
  padding: var(--spacing-xl) 0;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-lg);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.empty-state {
  padding: 60px 0;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 20px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.15s;
  border: 1px solid var(--border-lighter);
}

.notification-item:hover {
  background: rgba(99, 102, 241, 0.03);
}

.notification-item.unread {
  background: rgba(99, 102, 241, 0.04);
  border-left: 3px solid var(--color-primary);
}

.notification-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  flex-shrink: 0;
  margin-top: 6px;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.notification-content {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
}
</style>
```

**前端页面写法规约**：

| 规范 | 说明 |
|------|------|
| `<script setup>` | Vue 3 组合式 API 的语法糖，写起来更简洁 |
| `ref` / `reactive` | 响应式数据，简单值用 `ref`，对象用 `reactive` |
| `onMounted` | 页面加载后执行，一般用来拉数据 |
| `ElMessage` | Element Plus 的消息提示，成功/失败都用这个 |
| CSS 变量 | 颜色、间距用 `var(--xxx)`，适配亮暗主题 |
| `scoped` | 样式只在当前组件生效，不会污染全局 |

---

## 第十步：注册路由

页面写完了，要注册路由才能访问。路由配置在 `frontend/src/router/index.js`。

**路径**：`frontend/src/router/index.js`

在 `MainLayout` 的 `children` 数组里加一条：

```javascript
{
  path: "notifications",
  name: "Notifications",
  component: () => import("@/views/Notifications.vue"),
  meta: { title: "通知中心" },
},
```

**完整结构示意**：

```javascript
const routes = [
  {
    path: "/",
    component: () => import("@/layout/MainLayout.vue"),
    meta: { requiresAuth: true },
    children: [
      { path: "dashboard", ... },
      { path: "knowledge", ... },
      // ... 其他路由
      
      // ↓ 新增的通知中心路由
      {
        path: "notifications",
        name: "Notifications",
        component: () => import("@/views/Notifications.vue"),
        meta: { title: "通知中心" },
      },
    ],
  },
];
```

**路由 meta 字段说明**：

| 字段 | 作用 |
|------|------|
| `title` | 页面标题，会显示在浏览器标签上 |
| `requiresAuth` | 是否需要登录，默认 true（因为父路由设了） |
| `requiresAdmin` | 是否需要管理员权限 |

> **菜单入口**：侧边栏菜单在 `MainLayout.vue` 里配置。如果需要在侧边栏显示入口，去布局组件里加一条菜单项。

---

## 完整调用流程

从用户操作到数据落库，再到页面展示，完整链路是这样的：

```
用户点击"创建通知"
        │
        ▼
前端填表单 → 调 notificationAPI.create(data)
        │
        ▼
request.js 自动加 token → 发 POST /api/notification
        │
        ▼
JwtInterceptor 解析 token → 把 userId 放进 request
        │
        ▼
NotificationController.create() 接收参数
        │  从 request 取 userId
        ▼
NotificationService.create() 处理业务逻辑
        │  校验参数、设置默认值
        ▼
NotificationMapper.insert(entity)
        │
        ▼
写入 MySQL 数据库
        │
        ▼
返回 Result.success(data) ← 逐层向上返回
        │
        ▼
前端收到响应 → ElMessage.success("创建成功")
        │
        ▼
刷新列表 → 重新调用 loadList()
```

---

## 常见问题排查表

| 现象 | 排查点 |
|------|--------|
| 接口 404 | Controller 上有没有 `@RestController`？`@RequestMapping` 路径对不对？后端启动了吗？ |
| 接口 401 未授权 | 有没有登录？token 有没有过期？请求头里有没有带 Authorization？ |
| 接口 500 | 看后端控制台报错日志，一般是空指针、SQL 语法错、字段不存在 |
| 数据库表不存在 | 表建了吗？`@TableName` 写对了吗？`application.yml` 连的数据库对不对？ |
| 字段全是 null | Entity 字段名和表列名对应吗？下划线转驼峰开了吗？（MyBatis-Plus 默认开） |
| 前端调不通 CORS | `vite.config.js` 代理配了吗？后端 `CorsConfig` 开了吗？ |
| 分页不好使 | `MybatisPlusConfig` 里配了分页插件吗？参数名是 `current` 和 `size` 吗？ |
| 新增数据时间为 null | 用了自动填充的话，`MyMetaObjectHandler` 配了吗？字段加了 `@TableField(fill=...)` 吗？ |
| 逻辑删除不生效 | Entity 里 `deleted` 字段加 `@TableLogic` 了吗？配置文件开了吗？ |
| 前端页面白屏 | F12 看控制台报错，一般是 import 路径错了、组件名写错了 |
| 路由跳不过去 | 路由 path 对吗？`router.push()` 里写的路径和配置一致吗？ |
| workspaceId 取不到 | 有没有经过 WorkspaceInterceptor？请求头里有没有 `X-Workspace-Id`？ |

---

## 新模块接入检查清单

每接入一个新模块，对照这个清单逐项检查，确保不漏项：

```
后端部分
[ ] 数据库表建好了（字段、索引、注释齐全）
[ ] Entity 类写了（@TableName, @Getter @Setter, @TableId）
[ ] Mapper 接口写了（继承 BaseMapper，加 @Mapper）
[ ] DTO 定义了（请求参数类）
[ ] VO 定义了（响应数据类）
[ ] Service 接口写了（业务方法定义）
[ ] ServiceImpl 写了（@Service, 实现接口, 构造器注入）
[ ] Controller 写了（@RestController, @RequestMapping, 所有接口）
[ ] 接口加了 Knife4j 注解（@Tag, @Operation, @Parameter）
[ ] 接口做了权限校验（userId / workspaceId 隔离）
[ ] 关键操作打了日志

前端部分
[ ] API 封装写了（api/xxx.js，调用 request.js）
[ ] 页面组件写了（views/Xxx.vue，<script setup> 语法）
[ ] 用了 CSS 变量（适配亮暗主题）
[ ] 路由注册了（router/index.js）
[ ] 菜单入口加了（布局组件的侧边栏）

测试验证
[ ] 列表分页接口测了
[ ] 详情接口测了
[ ] 新增接口测了
[ ] 编辑接口测了
[ ] 删除接口测了
[ ] 权限边界测了（别人的数据能访问吗？）
[ ] 前端页面能正常打开
[ ] 增删改查操作都走通了
[ ] 异常情况有提示（网络错、权限错、参数错）
```
