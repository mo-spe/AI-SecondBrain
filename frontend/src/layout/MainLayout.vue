<template>
  <div class="layout-container">
    <aside class="sidebar" :class="{ collapsed: isSidebarCollapsed }">
      <div class="sidebar-header">
        <router-link to="/dashboard" class="logo">
          <div class="logo-icon">
              <BrainIcon :size="40" />
            </div>
          <span class="logo-text">AI-SecondBrain</span>
        </router-link>
        <button class="collapse-btn" @click="isSidebarCollapsed = !isSidebarCollapsed">
          <el-icon :size="18"><Fold v-if="!isSidebarCollapsed" /><Expand v-else /></el-icon>
        </button>
      </div>

      <nav class="sidebar-menu">
        <template v-for="group in sidebarGroups" :key="group.title">
          <div v-if="group.children && !isSidebarCollapsed" class="menu-group-title">
            {{ group.title }}
          </div>
          <router-link
            v-if="!group.children"
            :to="group.path"
            class="sidebar-item"
            :class="{ active: isActive(group.path) }"
          >
            <el-icon :size="18" :color="group.color || 'inherit'">
              <component :is="group.icon" />
            </el-icon>
            <span v-if="!isSidebarCollapsed">{{ group.title }}</span>
          </router-link>
          <div v-else class="sidebar-subgroup">
            <router-link
              v-for="child in group.children"
              :key="child.path"
              :to="child.path"
              class="sidebar-item"
              :class="{ active: isActive(child.path) }"
            >
              <el-icon :size="18">
                <component :is="child.icon" />
              </el-icon>
              <span v-if="!isSidebarCollapsed">{{ child.title }}</span>
            </router-link>
          </div>
        </template>
      </nav>

      <div v-if="!isSidebarCollapsed" class="sidebar-bottom">
        <div class="daily-goal-card">
          <div class="goal-header">
            <el-icon size="16" color="#7c3aed"><Target /></el-icon>
            <span>今日学习目标</span>
          </div>
          <div class="goal-progress">
            <div class="progress-ring">
              <svg class="ring-svg" viewBox="0 0 100 100">
                <circle
                  class="ring-bg"
                  cx="50"
                  cy="50"
                  r="42"
                  fill="none"
                  stroke="#e2e8f0"
                  stroke-width="8"
                />
                <circle
                  class="ring-progress"
                  cx="50"
                  cy="50"
                  r="42"
                  fill="none"
                  stroke="#7c3aed"
                  stroke-width="8"
                  stroke-linecap="round"
                  :stroke-dasharray="264"
                  :stroke-dashoffset="264 * (1 - dailyProgress / 100)"
                  transform="rotate(-90 50 50)"
                />
              </svg>
              <div class="progress-text">{{ dailyProgress }}%</div>
            </div>
          </div>
          <div class="goal-stats">
            <div class="stat-item">
              <span class="stat-label">目标</span>
              <span class="stat-value">复习 20 个卡片</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已完成</span>
              <span class="stat-value">{{ Math.round(dailyProgress * 0.2) }} 个</span>
            </div>
          </div>
          <div class="goal-tip">
            <el-icon size="14" color="#f59e0b"><Trophy /></el-icon>
            <span>继续加油，您可以做得更好！</span>
          </div>
        </div>

        <div class="copyright">
          <span>&copy; 2026 AI-SecondBrain</span>
          <span>版本 v2.0.0</span>
        </div>
      </div>
    </aside>

    <header class="top-navbar">
      <div class="navbar-left">
        <nav class="main-menu">
          <template v-for="group in topNavGroups" :key="group.title">
            <router-link
              v-if="!group.children"
              :to="group.path"
              class="nav-item"
              :class="{ active: isActive(group.path) }"
            >
              <el-icon :size="16">
                <component :is="group.icon" />
              </el-icon>
              <span>{{ group.title }}</span>
            </router-link>
            <el-dropdown
              v-else
              :class="['nav-group', { active: isGroupActive(group) }]"
              trigger="hover"
              popper-class="nav-dropdown-popper"
            >
              <div class="nav-item">
                <el-icon :size="16">
                  <component :is="group.icon" />
                </el-icon>
                <span>{{ group.title }}</span>
                <el-icon size="12"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="child in group.children"
                    :key="child.path"
                    @click="router.push(child.path)"
                  >
                    <el-icon :size="14">
                      <component :is="child.icon" />
                    </el-icon>
                    <span>{{ child.title }}</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </nav>
      </div>

      <div class="navbar-right">
        <div class="search-wrapper">
          <el-icon size="16" color="#94a3b8"><Search /></el-icon>
          <input
            type="text"
            placeholder="搜索知识点、卡片..."
            class="search-input"
            v-model="searchQuery"
            @keyup.enter="handleSearch"
          />
        </div>

        <button class="notification-btn" aria-label="通知">
          <el-icon :size="18" color="#64748b"><Bell /></el-icon>
          <span class="badge">6</span>
        </button>

        <el-dropdown @command="handleCommand" trigger="click" popper-class="nav-dropdown-popper">
          <div class="user-dropdown">
            <el-avatar :size="32" :src="userAvatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <span class="username">{{ userStore.userInfo.username || "用户" }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人资料
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon>
                账户设置
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <button class="theme-toggle" :aria-label="theme === 'dark' ? '切换到亮色模式' : '切换到暗色模式'" @click="toggleTheme">
          <el-icon :size="18">
            <Sunny v-if="theme === 'dark'" />
            <Moon v-else />
          </el-icon>
        </button>
      </div>
    </header>

    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <div class="page-wrapper" :key="route.path">
            <component :is="Component" />
          </div>
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { useThemeStore } from "@/stores/theme";
import BrainIcon from "@/components/BrainIcon.vue";
import {
  DataAnalysis,
  ChatDotRound,
  Collection,
  Reading,
  Bell,
  Search,
  Setting,
  User,
  ArrowDown,
  SwitchButton,
  DocumentCopy,
  TrendCharts,
  Share,
  Sunny,
  Moon,
  Fold,
  Expand,
  Notebook,
  Star,
  Brush,
  Document,
  Aim,
  Medal,
  Trophy,
} from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const { theme, toggleTheme } = useThemeStore();

const isSidebarCollapsed = ref(false);
const searchQuery = ref("");
const dailyProgress = ref(65);

const sidebarGroups = [
  {
    path: "/settings",
    title: "个人设置",
    icon: Setting,
    color: "#7c3aed",
  },
  {
    title: "学习成长",
    children: [
      { path: "/review", title: "复习中心", icon: Bell },
      { path: "/report", title: "学习统计", icon: TrendCharts },
    ],
  },
  {
    title: "知识管理",
    children: [
      { path: "/knowledge", title: "知识管理", icon: Reading },
      { path: "/knowledge", title: "知识点管理", icon: Notebook },
      { path: "/knowledge-system", title: "知识体系", icon: Share },
    ],
  },
  {
    title: "我的内容",
    children: [
      { path: "/knowledge", title: "我的笔记", icon: Notebook },
      { path: "/review", title: "我的卡片", icon: DocumentCopy },
      { path: "/knowledge", title: "我的收藏", icon: Star },
    ],
  },
];

const topNavGroups = [
  {
    path: "/dashboard",
    title: "工作台",
    icon: DataAnalysis,
  },
  {
    title: "数据采集",
    icon: Collection,
    children: [
      { path: "/capture", title: "数据采集", icon: Collection },
    ],
  },
  {
    title: "知识中心",
    icon: Document,
    children: [
      { path: "/knowledge", title: "知识管理", icon: Reading },
      { path: "/search", title: "知识搜索", icon: Search },
      { path: "/knowledge-system", title: "知识体系", icon: Share },
    ],
  },
  {
    title: "学习成长",
    icon: TrendCharts,
    children: [
      { path: "/review", title: "复习提醒", icon: Bell },
      { path: "/research", title: "AI学习研究", icon: TrendCharts },
    ],
  },
  {
    title: "系统管理",
    icon: Setting,
    children: [
      { path: "/settings", title: "个人设置", icon: Setting },
    ],
  },
];

const userAvatar = computed(() => {
  return userStore.userInfo.avatar || "";
});

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + "/");
};

const isGroupActive = (group) => {
  if (!group.children) return false;
  return group.children.some((child) => isActive(child.path));
};

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: "/search", query: { q: searchQuery.value } });
  }
};

const handleCommand = (command) => {
  if (command === "logout") {
    userStore.logout();
    router.push("/login");
  } else if (command === "profile") {
    router.push("/settings");
  } else if (command === "settings") {
    router.push("/settings");
  }
};
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: var(--bg-page);
  display: flex;
}

.sidebar {
  width: 240px;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-lighter);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 1001;
  transition: width var(--transition-base);
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--border-lighter);
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  text-decoration: none;
  color: var(--text-primary);
}

.logo-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  white-space: nowrap;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: var(--bg-input);
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--text-secondary);
  transition: background var(--transition-base), color var(--transition-base);
}

.collapse-btn:hover {
  background: var(--bg-hover);
  color: var(--color-primary);
}

.sidebar-menu {
  flex: 1;
  padding: var(--spacing-md);
  overflow-y: auto;
}

.menu-group-title {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: var(--spacing-md) var(--spacing-sm);
  margin-bottom: var(--spacing-xs);
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  color: var(--text-regular);
  text-decoration: none;
  font-size: var(--font-size-base);
  transition: background var(--transition-base), color var(--transition-base);
  white-space: nowrap;
}

.sidebar-item:hover {
  background: var(--bg-sidebar-hover);
  color: var(--color-primary);
}

.sidebar-item.active {
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
}

.sidebar-subgroup {
  padding-left: var(--spacing-sm);
}

.sidebar-bottom {
  padding: var(--spacing-md);
  border-top: 1px solid var(--border-lighter);
}

.daily-goal-card {
  background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.goal-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--spacing-lg);
}

.goal-progress {
  display: flex;
  justify-content: center;
  margin-bottom: var(--spacing-lg);
}

.progress-ring {
  position: relative;
  width: 80px;
  height: 80px;
}

.ring-svg {
  width: 100%;
  height: 100%;
}

.ring-progress {
  transition: stroke-dashoffset var(--transition-slow);
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.goal-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--spacing-md);
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-bottom: 2px;
}

.stat-value {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.goal-tip {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.6);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-sm);
}

.copyright {
  text-align: center;
}

.copyright span {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  line-height: 1.5;
}

.top-navbar {
  position: fixed;
  top: 0;
  left: 240px;
  right: 0;
  height: var(--navbar-height);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-lighter);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--spacing-xl);
  z-index: 1000;
  transition: left var(--transition-base);
  box-shadow: var(--shadow-navbar);
}

.sidebar.collapsed ~ .top-navbar {
  left: 64px;
}

.navbar-left {
  display: flex;
  align-items: center;
}

.main-menu {
  display: flex;
  gap: var(--spacing-xs);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--radius-md);
  color: var(--text-regular);
  text-decoration: none;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  transition: background var(--transition-base), color var(--transition-base);
  cursor: pointer;
}

.nav-item:hover {
  background: var(--bg-sidebar-hover);
  color: var(--color-primary);
}

.nav-item.active {
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.search-wrapper {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  background: var(--bg-input);
  border-radius: var(--radius-lg);
  padding: var(--spacing-sm) var(--spacing-lg);
  min-width: 280px;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: var(--font-size-base);
  color: var(--text-primary);
  outline: none;
}

.search-input::placeholder {
  color: var(--text-placeholder);
}

.notification-btn {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-base);
}

.notification-btn:hover {
  background: var(--bg-input);
}

.badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  background: var(--color-danger);
  color: white;
  font-size: 10px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-base);
}

.user-dropdown:hover {
  background: var(--bg-input);
}

.username {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.theme-toggle {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-secondary);
  transition: background var(--transition-base), color var(--transition-base);
}

.theme-toggle:hover {
  background: var(--bg-input);
  color: var(--text-primary);
}

.main-content {
  flex: 1;
  margin-left: 240px;
  padding-top: var(--navbar-height);
  min-height: 100vh;
  transition: margin-left var(--transition-base);
}

.sidebar.collapsed ~ .main-content {
  margin-left: 64px;
}

.page-wrapper {
  padding: var(--spacing-xl);
  min-height: calc(100vh - var(--navbar-height));
}

.page-fade-enter-active {
  transition: opacity var(--transition-base), transform var(--transition-base);
}

.page-fade-leave-active {
  transition: opacity var(--transition-fast), transform var(--transition-fast);
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

:deep(.nav-dropdown-popper .el-dropdown-menu) {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-lg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xs) 0;
  min-width: 180px;
}

:deep(.nav-dropdown-popper .el-dropdown-menu__item) {
  padding: var(--spacing-md) var(--spacing-lg);
  font-size: var(--font-size-base);
  color: var(--text-regular);
  transition: background var(--transition-base), color var(--transition-base);
}

:deep(.nav-dropdown-popper .el-dropdown-menu__item:hover) {
  background: var(--bg-sidebar-hover);
  color: var(--color-primary);
}

:deep(.nav-dropdown-popper .el-dropdown-menu__item.is-divided) {
  border-top: 1px solid var(--border-lighter);
  margin-top: var(--spacing-xs);
  padding-top: var(--spacing-md);
}

:deep(.nav-dropdown-popper .el-dropdown-menu__item .el-icon) {
  margin-right: var(--spacing-sm);
}

:deep(.el-avatar) {
  background: var(--color-primary-alpha-15);
  border: 1px solid var(--color-primary-alpha-20);
}

@media (max-width: 1024px) {
  .sidebar {
    transform: translateX(-100%);
    z-index: 2000;
  }

  .sidebar.collapsed {
    transform: translateX(0);
    width: 240px;
  }

  .top-navbar {
    left: 0;
  }

  .main-content {
    margin-left: 0;
  }

  .search-wrapper {
    min-width: 200px;
  }

  .nav-item span {
    display: none;
  }
}

@media (max-width: 768px) {
  .search-wrapper {
    display: none;
  }

  .username {
    display: none;
  }
}
</style>
