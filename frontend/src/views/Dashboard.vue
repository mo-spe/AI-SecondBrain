<template>
  <div class="dashboard-container">
    <div class="dashboard-grid">
      <aside class="sidebar-panel">
        <div class="user-profile-card">
          <div class="profile-header">
            <el-icon size="20" color="#f59e0b"><Trophy /></el-icon>
            <span class="welcome-text">こんにちは, {{ userStore.userInfo.username }}</span>
            <p class="welcome-subtitle">欢迎回到 AI-SecondBrain</p>
          </div>
          <div class="avatar-section">
            <el-avatar :size="100" :src="userAvatar">
              <el-icon size="40"><User /></el-icon>
            </el-avatar>
          </div>
          <div class="user-info-list">
            <div class="info-item">
              <el-icon size="14" color="#7c3aed"><Key /></el-icon>
              <span class="info-label">用户ID</span>
              <span class="info-value">{{ userStore.userInfo.id }}</span>
            </div>
            <div class="info-item">
              <el-icon size="14" color="#3b82f6"><Message /></el-icon>
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ userStore.userInfo.email }}</span>
            </div>
            <div class="info-item">
              <el-icon size="14" color="#10b981"><Phone /></el-icon>
              <span class="info-label">手机号</span>
              <span class="info-value">{{ userStore.userInfo.phone }}</span>
            </div>
            <div class="info-item">
              <el-icon size="14" color="#8b5cf6"><Calendar /></el-icon>
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatDate(userStore.userInfo.registerTime) }}</span>
            </div>
            <div class="info-item">
              <el-icon size="14" color="#06b6d4"><Clock /></el-icon>
              <span class="info-label">最后登录</span>
              <span class="info-value">{{ formatDate(userStore.userInfo.lastLoginTime) }}</span>
            </div>
          </div>
          <button class="edit-profile-btn">
            <el-icon size="14"><Edit /></el-icon>
            <span>编辑个人资料</span>
          </button>
        </div>

        <GamificationWidget />
        <StreakCalendar :months="3" style="margin-top: 16px" />
      </aside>

      <main class="main-panel">
        <div class="learning-overview-section">
          <div class="section-header">
            <el-icon size="20" color="#7c3aed"><Document /></el-icon>
            <span class="section-title">学习概览</span>
            <el-select v-model="timeRange" class="time-select" size="small">
              <el-option label="本周" value="week" />
              <el-option label="本月" value="month" />
              <el-option label="本季度" value="quarter" />
            </el-select>
          </div>
          <div class="overview-grid">
            <div class="overview-card review">
              <div class="card-icon">
                <el-icon size="24" color="#7c3aed"><DocumentCopy /></el-icon>
              </div>
              <div class="card-value">{{ statistics.pendingReviewCount }}</div>
              <div class="card-label">待复习</div>
              <div class="card-sub">个卡片</div>
            </div>
            <div class="overview-card completed">
              <div class="card-icon">
                <el-icon size="24" color="#22c55e"><CircleCheck /></el-icon>
              </div>
              <div class="card-value">{{ statistics.completedReviewCount }}</div>
              <div class="card-label">已完成</div>
              <div class="card-sub">个卡片</div>
            </div>
            <div class="overview-card accuracy">
              <div class="card-icon">
                <el-icon size="24" color="#f97316"><Aim /></el-icon>
              </div>
              <div class="card-value">{{ accuracy }}%</div>
              <div class="card-label">正确率</div>
            </div>
            <div class="overview-card streak">
              <div class="card-icon">
                <el-icon size="24" color="#3b82f6"><Medal /></el-icon>
              </div>
              <div class="card-value">{{ streakDays }}</div>
              <div class="card-label">连续天数</div>
              <div class="card-sub">天</div>
            </div>
          </div>
        </div>

        <div class="quick-actions-section">
          <div class="section-header">
            <el-icon size="20" color="#7c3aed"><Lightning /></el-icon>
            <span class="section-title">快捷操作</span>
          </div>
          <div class="actions-grid">
            <div class="action-card purple" @click="router.push('/review')">
              <div class="action-icon">
                <el-icon size="32" color="white"><Document /></el-icon>
              </div>
              <div class="action-title">开始复习</div>
              <div class="action-desc">智能安排复习计划</div>
            </div>
            <div class="action-card blue" @click="router.push('/chat')">
              <div class="action-icon">
                <el-icon size="32" color="white"><Plus /></el-icon>
              </div>
              <div class="action-title">生成复习卡片</div>
              <div class="action-desc">AI 智能生成卡片</div>
            </div>
            <div class="action-card green" @click="router.push('/knowledge')">
              <div class="action-icon">
                <el-icon size="32" color="white"><Grid /></el-icon>
              </div>
              <div class="action-title">知识点管理</div>
              <div class="action-desc">构建知识体系</div>
            </div>
            <div class="action-card orange" @click="router.push('/report')">
              <div class="action-icon">
                <el-icon size="32" color="white"><TrendCharts /></el-icon>
              </div>
              <div class="action-title">学习统计</div>
              <div class="action-desc">查看学习数据</div>
            </div>
          </div>
        </div>

        <div class="bottom-section">
          <div class="review-center-card">
            <div class="card-header">
              <el-icon size="18" color="#7c3aed"><Clock /></el-icon>
              <span class="card-title">复习中心</span>
              <button class="view-all-btn">查看全部</button>
            </div>
            <div class="review-progress">
              <div class="progress-info">
                <div class="progress-label">今日进度</div>
                <div class="progress-text">{{ todayCompleted }} / {{ todayTotal }}</div>
              </div>
              <div class="progress-bar-container">
                <div class="progress-track">
                  <div class="progress-fill" :style="{ width: reviewProgressPercent + '%' }"></div>
                </div>
              </div>
              <div class="progress-ring">
                <svg viewBox="0 0 100 100">
                  <circle
                    cx="50"
                    cy="50"
                    r="42"
                    fill="none"
                    stroke="#e2e8f0"
                    stroke-width="8"
                  />
                  <circle
                    cx="50"
                    cy="50"
                    r="42"
                    fill="none"
                    stroke="#7c3aed"
                    stroke-width="8"
                    stroke-linecap="round"
                    :stroke-dasharray="264"
                    :stroke-dashoffset="264 * (1 - reviewProgressPercent / 100)"
                    transform="rotate(-90 50 50)"
                  />
                </svg>
                <div class="ring-text">{{ reviewProgressPercent }}%</div>
              </div>
            </div>
            <div class="review-stats">
              <div class="review-stat-item">
                <el-icon size="14" color="#7c3aed"><DocumentCopy /></el-icon>
                <span class="stat-label">待复习卡片</span>
                <span class="stat-value">{{ statistics.pendingReviewCount }} 个</span>
              </div>
            </div>
            <button class="start-review-btn">
              <el-icon size="16"><VideoPlay /></el-icon>
              <span>开始复习</span>
            </button>
          </div>

          <div class="knowledge-management-card">
            <div class="card-header">
              <el-icon size="18" color="#7c3aed"><Trophy /></el-icon>
              <span class="card-title">知识管理</span>
              <button class="view-all-btn" @click="router.push('/knowledge')">查看全部</button>
            </div>
            <div class="knowledge-summary">
              <div class="knowledge-stat">
                <span class="ks-value">{{ statistics.knowledgeCount }}</span>
                <span class="ks-label">知识节点总数</span>
              </div>
            </div>
            <button class="add-knowledge-btn" @click="router.push('/knowledge')">
              <el-icon size="14"><Plus /></el-icon>
              <span>添加知识点</span>
            </button>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { statisticsAPI } from "@/api/statistics";
import { reviewAPI } from "@/api/review";
import GamificationWidget from "@/components/GamificationWidget.vue";
import StreakCalendar from "@/components/StreakCalendar.vue";
import {
  User,
  Key,
  Message,
  Phone,
  Calendar,
  Clock,
  Edit,
  Document,
  DocumentCopy,
  CircleCheck,
  Aim,
  Medal,
  Lightning,
  Plus,
  Grid,
  TrendCharts,
  VideoPlay,
  Trophy,
} from "@element-plus/icons-vue";

const router = useRouter();
const userStore = useUserStore();

const statistics = ref({
  chatCount: 0,
  knowledgeCount: 0,
  pendingReviewCount: 0,
  completedReviewCount: 0,
});

const timeRange = ref("week");
const accuracy = ref(0);
const streakDays = ref(0);
const todayCards = ref([]);

const todayTotal = computed(() => todayCards.value.length);

const todayCompleted = computed(() =>
  todayCards.value.filter((c) => c.reviewCount > 0).length
);

const userAvatar = computed(() => {
  return userStore.userInfo.avatar || "";
});

const reviewProgressPercent = computed(() => {
  if (todayTotal.value === 0) return 0;
  return Math.round((todayCompleted.value / todayTotal.value) * 100);
});

const formatDate = (dateStr) => {
  if (!dateStr) return "";
  return new Date(dateStr).toLocaleString("zh-CN");
};

const loadStatistics = async () => {
  try {
    const data = await statisticsAPI.getStatistics();
    statistics.value = data;
  } catch (error) {
    console.error("加载统计数据失败:", error);
  }
};

const loadAccuracyAndStreak = async () => {
  try {
    const [acc, streak] = await Promise.all([
      reviewAPI.getUserAccuracy(),
      reviewAPI.getStreakDays(),
    ]);
    accuracy.value = acc ?? 0;
    streakDays.value = streak ?? 0;
  } catch (error) {
    console.error("加载正确率和连续天数失败:", error);
  }
};

const loadTodayCards = async () => {
  try {
    const cards = await reviewAPI.getTodayReviewCards();
    todayCards.value = cards || [];
  } catch (error) {
    console.error("加载今日复习卡片失败:", error);
  }
};

onMounted(() => {
  loadStatistics();
  loadAccuracyAndStreak();
  loadTodayCards();
});
</script>

<style scoped>
.dashboard-container {
  min-height: 100%;
  padding: 0;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: var(--spacing-xl);
  height: 100%;
}

.sidebar-panel {
  display: flex;
  flex-direction: column;
}

.user-profile-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-md);
}

.profile-header {
  margin-bottom: var(--spacing-xl);
}

.welcome-text {
  display: block;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin-bottom: 4px;
}

.welcome-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 0;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: var(--spacing-xl);
}

.avatar-section :deep(.el-avatar) {
  margin-bottom: 0;
  border: 3px solid var(--color-primary-alpha-20);
}

.user-info-list {
  margin-bottom: var(--spacing-xl);
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) 0;
  border-bottom: 1px solid var(--border-lighter);
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  width: 60px;
}

.info-value {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--text-regular);
}

.edit-profile-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background: var(--bg-input);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  color: var(--text-regular);
  cursor: pointer;
  transition: background var(--transition-base), color var(--transition-base);
}

.edit-profile-btn:hover {
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
}

.main-panel {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.time-select {
  margin-left: auto;
  width: 120px;
}

.learning-overview-section {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-md);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
}

.overview-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-lg);
  background: var(--bg-page);
  border-radius: var(--radius-lg);
}

.card-icon {
  width: 48px;
  height: 48px;
  background: var(--bg-input);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--spacing-md);
}

.card-value {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin-bottom: 2px;
}

.card-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-regular);
}

.card-sub {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.quick-actions-section {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-md);
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-lg);
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.action-card.purple {
  background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 100%);
}

.action-card.blue {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
}

.action-card.green {
  background: linear-gradient(135deg, #22c55e 0%, #4ade80 100%);
}

.action-card.orange {
  background: linear-gradient(135deg, #f97316 0%, #fb923c 100%);
}

.action-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--spacing-md);
}

.action-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: white;
  margin-bottom: 2px;
}

.action-desc {
  font-size: var(--font-size-xs);
  color: rgba(255, 255, 255, 0.8);
}

.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-xl);
}

.review-center-card,
.knowledge-management-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-xl);
}

.card-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.view-all-btn {
  margin-left: auto;
  padding: var(--spacing-xs) var(--spacing-md);
  background: transparent;
  border: none;
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  cursor: pointer;
  transition: color var(--transition-base);
}

.view-all-btn:hover {
  text-decoration: underline;
}

.review-progress {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.progress-info {
  flex: 1;
}

.progress-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin-bottom: 4px;
}

.progress-text {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}

.progress-bar-container {
  flex: 2;
}

.progress-track {
  height: 8px;
  background: var(--bg-input);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--gradient-primary);
  border-radius: var(--radius-full);
}

.progress-ring {
  position: relative;
  width: 80px;
  height: 80px;
}

.progress-ring svg {
  width: 100%;
  height: 100%;
}

.ring-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.review-stats {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xl);
}

.review-stat-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.review-stat-item .stat-label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  flex: 1;
}

.review-stat-item .stat-value {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.start-review-btn {
  margin-top: auto;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-lg);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: white;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.start-review-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-primary-hover);
}

.knowledge-summary {
  display: flex;
  justify-content: center;
  padding: var(--spacing-xl) 0;
  margin-bottom: var(--spacing-lg);
}

.knowledge-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.ks-value {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.ks-label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.add-knowledge-btn {
  margin-top: auto;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background: var(--bg-input);
  border: none;
  border-radius: var(--radius-lg);
  font-size: var(--font-size-sm);
  color: var(--text-regular);
  cursor: pointer;
  transition: background var(--transition-base), color var(--transition-base);
}

.add-knowledge-btn:hover {
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
}

:deep(.el-select) {
  width: 120px;
}

:deep(.el-select__wrapper) {
  border-radius: var(--radius-md);
}

:deep(.el-avatar) {
  background: var(--color-primary-alpha-15);
}

@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .sidebar-panel {
    order: 2;
  }

  .main-panel {
    order: 1;
  }

  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .bottom-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .actions-grid {
    grid-template-columns: 1fr;
  }

  .review-progress {
    flex-direction: column;
    text-align: center;
  }

  .progress-bar-container {
    width: 100%;
  }
}
</style>
