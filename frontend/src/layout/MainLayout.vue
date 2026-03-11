<template>
  <div class="layout-container">
    <div class="top-navbar">
      <div class="navbar-content">
        <div class="navbar-left">
          <div class="logo">
            <el-icon size="24" color="#667eea"><Document /></el-icon>
            <span class="logo-text">AI-SecondBrain</span>
          </div>
        </div>

        <div class="navbar-center">
          <nav class="main-menu">
            <router-link
              v-for="item in menuItems"
              :key="item.path"
              :to="item.path"
              class="menu-item"
              :class="{ active: isActive(item.path) }"
            >
              <el-icon :size="18">
                <component :is="item.icon" />
              </el-icon>
              <span>{{ item.title }}</span>
            </router-link>
          </nav>
        </div>

        <div class="navbar-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-dropdown">
              <el-avatar :size="36" :src="userAvatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="username">{{
                userStore.userInfo.username || "用户"
              }}</span>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
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
        </div>
      </div>
    </div>

    <div class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import {
  Document,
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
} from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const menuItems = [
  {
    path: "/dashboard",
    title: "数据统计",
    icon: DataAnalysis,
  },
  {
    path: "/chat",
    title: "对话采集",
    icon: ChatDotRound,
  },
  {
    path: "/capture",
    title: "数据捕捉",
    icon: Collection,
  },
  {
    path: "/knowledge",
    title: "知识管理",
    icon: Reading,
  },
  {
    path: "/review",
    title: "复习提醒",
    icon: Bell,
  },
  {
    path: "/search",
    title: "知识搜索",
    icon: Search,
  },
  {
    path: "/report",
    title: "学习报告",
    icon: DocumentCopy,
  },
  {
    path: "/research",
    title: "AI学习研究",
    icon: TrendCharts,
  },
  {
    path: "/settings",
    title: "个人设置",
    icon: Setting,
  },
];

const userAvatar = computed(() => {
  return userStore.userInfo.avatar || "";
});

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + "/");
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
  background: #f6f8fa;
}

.top-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1000;
}

.navbar-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.navbar-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: white;
  font-size: 20px;
  font-weight: bold;
  text-decoration: none;
  transition: all 0.3s;
}

.logo:hover {
  transform: scale(1.05);
}

.logo-text {
  background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.navbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.main-menu {
  display: flex;
  gap: 8px;
  align-items: center;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
  position: relative;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  transform: translateY(-2px);
}

.menu-item.active {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-weight: 600;
}

.menu-item.active::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 3px;
  background: white;
  border-radius: 2px;
  box-shadow: 0 2px 4px rgba(255, 255, 255, 0.3);
}

.navbar-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  color: white;
}

.user-dropdown:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

.username {
  font-size: 14px;
  font-weight: 500;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-icon {
  font-size: 12px;
  transition: transform 0.3s;
}

.user-dropdown:hover .dropdown-icon {
  transform: rotate(180deg);
}

.main-content {
  padding-top: 64px;
  min-height: calc(100vh - 64px);
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.3s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}

:deep(.el-dropdown-menu) {
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border-radius: 12px;
  padding: 8px 0;
  min-width: 180px;
}

:deep(.el-dropdown-menu__item) {
  padding: 12px 16px;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

:deep(.el-dropdown-menu__item.is-divided) {
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
  padding-top: 12px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 8px;
  color: #909399;
}

:deep(.el-dropdown-menu__item:hover .el-icon) {
  color: #667eea;
}

:deep(.el-avatar) {
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
}

@media (max-width: 768px) {
  .navbar-content {
    padding: 0 12px;
  }

  .logo-text {
    display: none;
  }

  .main-menu {
    gap: 4px;
  }

  .menu-item {
    padding: 8px 12px;
    font-size: 13px;
  }

  .menu-item span {
    display: none;
  }

  .menu-item .el-icon {
    margin: 0;
  }

  .username {
    display: none;
  }

  .user-dropdown {
    padding: 8px;
  }
}

@media (max-width: 480px) {
  .top-navbar {
    height: 56px;
  }

  .main-content {
    padding-top: 56px;
  }

  .logo {
    font-size: 18px;
  }

  .menu-item {
    padding: 6px 10px;
  }
}
</style>
