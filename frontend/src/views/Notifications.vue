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
    // Navigate to square with post detail — for now just go to square
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
