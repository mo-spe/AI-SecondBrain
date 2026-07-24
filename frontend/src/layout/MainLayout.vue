<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ collapsed: isSidebarCollapsed }">
      <div class="sidebar-header">
        <router-link to="/dashboard" class="logo-link">
          <span class="logo-mark">SB</span>
          <span v-show="!isSidebarCollapsed" class="logo-label">SecondBrain</span>
        </router-link>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in visibleNavItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ active: isActive(item.path) }"
          :title="item.label"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span v-show="!isSidebarCollapsed" class="nav-label">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button
          class="collapse-btn"
          @click="isSidebarCollapsed = !isSidebarCollapsed"
          :title="isSidebarCollapsed ? '展开菜单' : '收起菜单'"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"
            :style="{ transform: isSidebarCollapsed ? 'scaleX(-1)' : '' }">
            <path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </aside>

    <div class="main-area" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
      <header class="topbar">
        <div class="topbar-left">
          <WorkspaceSwitcher />
        </div>

        <div class="topbar-right">
          <div class="global-search">
            <svg class="search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none">
              <circle cx="6.5" cy="6.5" r="4.5" stroke="currentColor" stroke-width="1.3"/>
              <path d="M10 10l3.5 3.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
            <input
              type="text"
              v-model="searchQuery"
              placeholder="搜索..."
              class="search-field"
              @keyup.enter="handleSearch"
            />
          </div>

          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
            <button class="user-btn" aria-label="通知" @click="handleOpenNotifications">
              <el-icon :size="18"><Bell /></el-icon>
            </button>
          </el-badge>

          <el-dropdown trigger="click" popper-class="user-menu-popper" @command="handleCommand">
            <button class="user-btn" aria-label="用户菜单">
              <span class="user-avatar">{{ userInitial }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="settings">个人设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <div class="page" :key="route.path">
              <component :is="Component" />
            </div>
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { useWorkspaceStore } from "@/stores/workspace";
import WorkspaceSwitcher from "@/components/WorkspaceSwitcher.vue";
import { notificationAPI } from "@/api/notification";
import {
  DataAnalysis,
  Collection,
  Reading,
  Search,
  Bell,
  TrendCharts,
  Share,
  Notebook,
  Setting,
  Platform,
  Trophy,
  Medal,
} from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const workspaceStore = useWorkspaceStore();

const isSidebarCollapsed = ref(false);
const searchQuery = ref("");
const unreadCount = ref(0);
let unreadTimer = null;

const navItems = [
  { path: "/dashboard", label: "工作台", icon: DataAnalysis },
  { path: "/capture", label: "数据采集", icon: Collection },
  { path: "/knowledge", label: "知识管理", icon: Reading },
  { path: "/square", label: "知识广场", icon: Platform },
  { path: "/search", label: "知识搜索", icon: Search },
  { path: "/knowledge-system", label: "知识体系", icon: Share },
  { path: "/review", label: "复习中心", icon: Bell },
  { path: "/achievements", label: "成就殿堂", icon: Trophy },
  { path: "/leaderboard", label: "排行榜", icon: Medal },
  { path: "/research", label: "AI研究", icon: TrendCharts },
  { path: "/settings", label: "个人设置", icon: Setting },
];

const adminNavItem = { path: "/admin", label: "平台管理", icon: Notebook };

const visibleNavItems = computed(() => {
  if (userStore.userInfo.role === "super_admin") {
    return [...navItems, adminNavItem];
  }
  return navItems;
});

const userInitial = computed(() => {
  const name = userStore.userInfo.username || "U";
  return name.charAt(0).toUpperCase();
});

const isActive = (path) => {
  if (path === "/dashboard") return route.path === "/dashboard";
  return route.path === path || route.path.startsWith(path + "/");
};

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: "/search", query: { q: searchQuery.value } });
  }
};

const handleCommand = (cmd) => {
  if (cmd === "logout") {
    userStore.logout();
    router.push("/login");
  } else if (cmd === "settings") {
    router.push("/settings");
  }
};

const handleOpenNotifications = () => {
  router.push("/notifications");
};

const fetchUnreadCount = async () => {
  try {
    const data = await notificationAPI.getUnreadCount();
    unreadCount.value = data.unreadCount || 0;
  } catch {
    // ignore — user might not be logged in
  }
};

onMounted(() => {
  workspaceStore.fetchWorkspaces();
  if (userStore.isLoggedIn()) {
    fetchUnreadCount();
    unreadTimer = setInterval(fetchUnreadCount, 30000);
  }
});

onUnmounted(() => {
  if (unreadTimer) clearInterval(unreadTimer);
});
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--bg-page);
  font-family: var(--font-family-base);
  color: var(--text-primary);
}

/* ===== Sidebar ===== */
.sidebar {
  width: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 100;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-lighter);
  transition: width var(--transition-base);
}

.sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

.sidebar-header {
  padding: var(--spacing-lg) var(--spacing-lg);
  border-bottom: 1px solid var(--border-lighter);
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text-primary);
}

.logo-mark {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  font-family: var(--font-family-display);
  border-radius: var(--radius-sm);
  letter-spacing: -0.02em;
  flex-shrink: 0;
}

.logo-label {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  white-space: nowrap;
  letter-spacing: -0.01em;
}

/* ===== Sidebar Nav ===== */
.sidebar-nav {
  flex: 1;
  padding: var(--spacing-sm);
  overflow-y: auto;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: 9px var(--spacing-md);
  margin-bottom: 2px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-normal);
  transition: background var(--transition-fast), color var(--transition-fast);
  white-space: nowrap;
}

.nav-link:hover {
  background: var(--bg-sidebar-hover);
  color: var(--text-primary);
}

.nav-link.active {
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
}

.nav-label {
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== Sidebar Footer ===== */
.sidebar-footer {
  padding: var(--spacing-sm);
  border-top: 1px solid var(--border-lighter);
}

.collapse-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  cursor: pointer;
  color: var(--text-secondary);
  transition: background var(--transition-fast), color var(--transition-fast);
}

.collapse-btn:hover {
  background: var(--bg-sidebar-hover);
  color: var(--text-primary);
}

/* ===== Main Area ===== */
.main-area {
  flex: 1;
  margin-left: var(--sidebar-width);
  transition: margin-left var(--transition-base);
}

.main-area.sidebar-collapsed {
  margin-left: var(--sidebar-collapsed-width);
}

/* ===== Top Bar ===== */
.topbar {
  position: sticky;
  top: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--navbar-height);
  padding: 0 var(--spacing-xl);
  background: var(--bg-page);
  border-bottom: 1px solid var(--border-lighter);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

/* ===== Global Search ===== */
.global-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.global-search:focus-within {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus-ring);
}

.search-icon {
  color: var(--text-secondary);
  flex-shrink: 0;
}

.search-field {
  border: none;
  background: transparent;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  outline: none;
  width: 180px;
}

.search-field::placeholder {
  color: var(--text-placeholder);
}

/* ===== User ===== */
.user-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-full);
  background: var(--color-primary-alpha-10);
  cursor: pointer;
  transition: background var(--transition-fast);
  padding: 0;
}

.user-btn:hover {
  background: var(--color-primary-alpha-20);
}

.user-avatar {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
  line-height: 1;
}

/* ===== Content ===== */
.content {
  min-height: calc(100vh - var(--navbar-height));
}

.page {
  padding: var(--spacing-2xl);
}

/* ===== Page transition ===== */
.page-enter-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
  }

  .sidebar.collapsed {
    transform: translateX(0);
    width: var(--sidebar-width);
  }

  .main-area,
  .main-area.sidebar-collapsed {
    margin-left: 0;
  }

  .page {
    padding: var(--spacing-lg);
  }

  .global-search {
    display: none;
  }
}
</style>

<style>
/* ===== Global: Element Plus dropdown overrides ===== */
.user-menu-popper {
  margin-top: 4px !important;
}

.user-menu-popper .el-dropdown-menu {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-dropdown);
  padding: 4px;
}

.user-menu-popper .el-dropdown-menu__item {
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  color: var(--text-primary);
}

.user-menu-popper .el-dropdown-menu__item:hover {
  background: var(--bg-hover);
}
</style>
