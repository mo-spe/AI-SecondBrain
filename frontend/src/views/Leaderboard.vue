<template>
  <div class="leaderboard-page">
    <div class="page-header">
      <h1 class="page-title">排行榜</h1>
      <p class="page-subtitle">看看谁是最努力的学习者</p>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="period" size="default" @change="loadLeaderboard">
        <el-radio-button value="daily">今日</el-radio-button>
        <el-radio-button value="weekly">本周</el-radio-button>
        <el-radio-button value="monthly">本月</el-radio-button>
        <el-radio-button value="all">总榜</el-radio-button>
      </el-radio-group>
      <div class="filter-right">
        <span class="filter-label">领域:</span>
        <el-select v-model="domain" size="default" @change="loadLeaderboard" style="width: 160px">
          <el-option label="全部领域" value="all" />
        </el-select>
      </div>
    </div>

    <div v-loading="loading" class="leaderboard-content">
      <div v-if="currentUser" class="current-user-row">
        <div class="current-rank">
          <span class="rank-label">我的排名</span>
          <span class="rank-num">#{{ currentUser.rank || '-' }}</span>
        </div>
        <div class="current-info">
          <span class="current-score">{{ formatNumber(currentUser.score) }} 分</span>
        </div>
      </div>

      <div class="rank-list">
        <div
          v-for="entry in entries"
          :key="entry.userId"
          class="rank-item"
          :class="{ 'is-me': entry.userId === userStore.userInfo.id }"
        >
          <div class="rank-position">
            <span v-if="entry.rank <= 3" class="rank-medal rank-{{ entry.rank }}">
              {{ ['', '🥇', '🥈', '🥉'][entry.rank] }}
            </span>
            <span v-else class="rank-number">#{{ entry.rank }}</span>
          </div>
          <div class="rank-user">
            <el-avatar :size="36" :src="entry.avatar">
              {{ entry.username?.charAt(0) }}
            </el-avatar>
            <div class="rank-user-info">
              <span class="rank-username">{{ entry.username }}</span>
              <span class="rank-level">Lv.{{ entry.level }}</span>
            </div>
          </div>
          <div class="rank-score">
            {{ formatNumber(entry.score) }} 分
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && entries.length === 0" description="暂无排行数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useUserStore } from "@/stores/user";
import { useGamificationStore } from "@/stores/gamification";

const userStore = useUserStore();
const gamificationStore = useGamificationStore();

const loading = ref(false);
const period = ref("daily");
const domain = ref("all");
const entries = ref([]);
const currentUser = ref(null);

const formatNumber = (n) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + "万";
  if (n >= 1000) return (n / 1000).toFixed(1) + "k";
  return String(n);
};

const loadLeaderboard = async () => {
  loading.value = true;
  try {
    await gamificationStore.fetchLeaderboard(period.value, domain.value, 100);
    const data = gamificationStore.leaderboard;
    entries.value = data?.entries || [];
    currentUser.value = data?.currentUser || null;
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadLeaderboard();
});
</script>

<style scoped>
.leaderboard-page {
  max-width: 720px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 当前用户排名 */
.current-user-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(139, 92, 246, 0.04));
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: var(--radius-lg);
  margin-bottom: 12px;
}

.current-rank {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rank-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.rank-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
}

.current-score {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 排行列表 */
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-lighter);
  transition: background var(--transition-fast);
}

.rank-item:hover {
  background: var(--bg-page);
}

.rank-item.is-me {
  border-color: rgba(99, 102, 241, 0.3);
  background: rgba(99, 102, 241, 0.04);
}

.rank-position {
  width: 44px;
  text-align: center;
  flex-shrink: 0;
}

.rank-medal {
  font-size: 22px;
}

.rank-number {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
}

.rank-user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.rank-user-info {
  display: flex;
  flex-direction: column;
}

.rank-username {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.rank-level {
  font-size: 11px;
  color: var(--color-primary);
}

.rank-score {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
