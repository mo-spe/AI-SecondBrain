import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "@/stores/user";

const routes = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/Login.vue"),
    meta: { title: "登录", requiresAuth: false },
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("@/views/Register.vue"),
    meta: { title: "注册", requiresAuth: false },
  },
  {
    path: "/",
    component: () => import("@/layout/MainLayout.vue"),
    redirect: "/dashboard",
    meta: { requiresAuth: true },
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("@/views/Dashboard.vue"),
        meta: { title: "数据统计" },
      },
      {
        path: "capture",
        name: "Capture",
        component: () => import("@/views/Capture.vue"),
        meta: { title: "数据采集中心" },
      },
      {
        path: "knowledge",
        name: "Knowledge",
        component: () => import("@/views/Knowledge.vue"),
        meta: { title: "知识管理" },
      },
      {
        path: "knowledge/new",
        name: "KnowledgeCreate",
        component: () => import("@/views/KnowledgeEdit.vue"),
        meta: { title: "创建知识" },
      },
      {
        path: "knowledge/:id",
        name: "KnowledgeDetail",
        component: () => import("@/views/KnowledgeDetail.vue"),
        meta: { title: "知识详情" },
      },
      {
        path: "knowledge/:id/edit",
        name: "KnowledgeEdit",
        component: () => import("@/views/KnowledgeEdit.vue"),
        meta: { title: "编辑知识" },
      },
      {
        path: "review",
        name: "Review",
        component: () => import("@/views/Review.vue"),
        meta: { title: "复习提醒" },
      },
      {
        path: "search",
        name: "Search",
        component: () => import("@/views/Search.vue"),
        meta: { title: "知识搜索" },
      },
      {
        path: "knowledge-system",
        name: "KnowledgeSystem",
        component: () => import("@/views/KnowledgeSystem.vue"),
        meta: { title: "知识体系" },
      },
      {
        path: "research",
        name: "Research",
        component: () => import("@/views/Research.vue"),
        meta: { title: "AI学习研究" },
      },
      {
        path: "settings",
        name: "Settings",
        component: () => import("@/views/Settings.vue"),
        meta: { title: "个人设置" },
      },
      {
        path: "workspace/:id/members",
        name: "WorkspaceMembers",
        component: () => import("@/views/WorkspaceMembers.vue"),
        meta: { title: "成员管理" },
      },
      {
        path: "square",
        name: "Square",
        component: () => import("@/views/Square.vue"),
        meta: { title: "知识广场" },
      },
      {
        path: "notifications",
        name: "Notifications",
        component: () => import("@/views/Notifications.vue"),
        meta: { title: "通知中心" },
      },
      {
        path: "admin",
        name: "AdminDashboard",
        component: () => import("@/views/AdminDashboard.vue"),
        meta: { title: "平台管理", requiresAdmin: true },
      },
      {
        path: "achievements",
        name: "Achievements",
        component: () => import("@/views/Achievements.vue"),
        meta: { title: "成就殿堂" },
      },
      {
        path: "leaderboard",
        name: "Leaderboard",
        component: () => import("@/views/Leaderboard.vue"),
        meta: { title: "排行榜" },
      },
    ],
  },
  {
    path: "/share/:token",
    name: "ShareView",
    component: () => import("@/views/ShareView.vue"),
    meta: { title: "知识分享", requiresAuth: false },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  document.title = to.meta.title
    ? `${to.meta.title} - AI-SecondBrain`
    : "AI-SecondBrain";

  const userStore = useUserStore();

  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn()) {
    next("/login");
  } else if (to.meta.requiresAdmin && userStore.userInfo.role !== "super_admin") {
    next("/dashboard");
  } else if (
    (to.path === "/login" || to.path === "/register") &&
    userStore.isLoggedIn()
  ) {
    next("/dashboard");
  } else {
    next();
  }
});

export default router;
