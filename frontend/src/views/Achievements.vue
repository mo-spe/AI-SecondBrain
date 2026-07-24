<template>
  <div class="achievements-page">
    <div class="page-header">
      <h1 class="page-title">成就殿堂</h1>
      <p class="page-subtitle">解锁成就，见证你的学习成长之路</p>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="category" size="default" @change="loadAchievements">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="review">复习</el-radio-button>
        <el-radio-button value="knowledge">知识</el-radio-button>
        <el-radio-button value="streak">打卡</el-radio-button>
        <el-radio-button value="accuracy">正确率</el-radio-button>
        <el-radio-button value="mastery">精通</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="filter" size="small" @change="loadAchievements">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="unlocked">已解锁</el-radio-button>
        <el-radio-button value="locked">未解锁</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="achievements-grid">
      <div
        v-for="ach in achievements"
        :key="ach.id"
        class="achievement-card"
        :class="{ unlocked: ach.unlocked, ['tier-' + ach.tier]: true }"
        @click="showDetail(ach)"
      >
        <div class="ach-icon">
          <el-icon :size="28">
            <component :is="iconMap[ach.icon] || Trophy" />
          </el-icon>
        </div>
        <div class="ach-info">
          <span class="ach-name">{{ ach.name }}</span>
          <span class="ach-desc">{{ ach.description }}</span>
          <div v-if="!ach.unlocked" class="ach-progress">
            <div class="ach-progress-bar">
              <div class="ach-progress-fill" :style="{ width: ach.progressPercent + '%' }" />
            </div>
            <span class="ach-progress-text">{{ ach.currentValue }}/{{ ach.triggerValue }}</span>
          </div>
          <div v-else class="ach-unlocked-info">
            <span class="ach-date">{{ formatDate(ach.unlockedAt) }}</span>
            <span class="ach-reward" v-if="ach.pointsReward">+{{ ach.pointsReward }}积分</span>
          </div>
        </div>
        <div class="ach-tier-badge" :class="ach.tier">
          {{ tierLabel(ach.tier) }}
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && achievements.length === 0" description="暂无成就" />

    <!-- 成就详情弹窗 -->
    <el-dialog v-model="showDialog" title="成就详情" width="420px">
      <div class="detail-content" v-if="selectedAchievement">
        <div class="detail-icon-wrap">
          <div class="detail-icon" :class="'tier-' + selectedAchievement.tier">
            <el-icon :size="40">
              <component :is="iconMap[selectedAchievement.icon] || Trophy" />
            </el-icon>
          </div>
        </div>
        <h2 class="detail-name">{{ selectedAchievement.name }}</h2>
        <p class="detail-desc">{{ selectedAchievement.description }}</p>
        <div class="detail-meta">
          <span class="detail-tier">{{ tierLabel(selectedAchievement.tier) }}成就</span>
          <span v-if="selectedAchievement.pointsReward">+{{ selectedAchievement.pointsReward }} 积分</span>
          <span v-if="selectedAchievement.makeupCardReward">+{{ selectedAchievement.makeupCardReward }} 补签卡</span>
        </div>
        <div v-if="selectedAchievement.unlocked" class="detail-unlocked">
          已于 {{ formatDate(selectedAchievement.unlockedAt) }} 解锁
        </div>
        <div v-else class="detail-progress">
          <span>进度: {{ selectedAchievement.currentValue }}/{{ selectedAchievement.triggerValue }}</span>
          <div class="detail-bar">
            <div class="detail-fill" :style="{ width: selectedAchievement.progressPercent + '%' }" />
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useGamificationStore } from "@/stores/gamification";
import {
  Medal, Trophy, Document, Collection, Sunny, Sunrise, Star, StarFilled, MagicStick,
} from "@element-plus/icons-vue";

const gamificationStore = useGamificationStore();

const loading = ref(false);
const achievements = ref([]);
const category = ref("");
const filter = ref("all");
const showDialog = ref(false);
const selectedAchievement = ref(null);

const iconMap = {
  Medal, Trophy, Document, Collection, Sunny, Sunrise, Star, StarFilled, MagicStick,
};

const tierLabel = (tier) => {
  const map = { bronze: "青铜", silver: "白银", gold: "黄金", platinum: "铂金" };
  return map[tier] || tier;
};

const loadAchievements = async () => {
  loading.value = true;
  try {
    const data = await gamificationStore.fetchAchievements(
      category.value || undefined,
      filter.value === "all" ? undefined : filter.value
    );
    achievements.value = gamificationStore.achievements;
  } finally {
    loading.value = false;
  }
};

const showDetail = (ach) => {
  selectedAchievement.value = ach;
  showDialog.value = true;
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

onMounted(() => {
  loadAchievements();
});
</script>

<style scoped>
.achievements-page {
  max-width: 960px;
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

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.achievement-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-lighter);
  cursor: pointer;
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
  position: relative;
  opacity: 0.6;
}

.achievement-card.unlocked {
  opacity: 1;
}

.achievement-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.achievement-card.tier-platinum.unlocked {
  border-color: rgba(6, 182, 212, 0.3);
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.04), rgba(6, 182, 212, 0.01));
}

.achievement-card.tier-gold.unlocked {
  border-color: rgba(245, 158, 11, 0.3);
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.04), rgba(245, 158, 11, 0.01));
}

.ach-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  flex-shrink: 0;
}

.unlocked .ach-icon {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(139, 92, 246, 0.1));
  color: var(--color-primary);
}

.ach-info {
  flex: 1;
  min-width: 0;
}

.ach-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  display: block;
  margin-bottom: 2px;
}

.ach-desc {
  font-size: 12px;
  color: var(--text-secondary);
  display: block;
  margin-bottom: 8px;
}

.ach-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ach-progress-bar {
  flex: 1;
  height: 4px;
  background: var(--bg-page);
  border-radius: 2px;
  overflow: hidden;
}

.ach-progress-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 2px;
  transition: width 0.5s ease;
}

.ach-progress-text {
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.ach-unlocked-info {
  display: flex;
  gap: 8px;
}

.ach-date {
  font-size: 11px;
  color: var(--text-secondary);
}

.ach-reward {
  font-size: 11px;
  color: var(--color-primary);
  font-weight: 500;
}

.ach-tier-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 600;
}

.ach-tier-badge.bronze {
  background: rgba(217, 119, 6, 0.1);
  color: #d97706;
}

.ach-tier-badge.silver {
  background: rgba(148, 163, 184, 0.15);
  color: #64748b;
}

.ach-tier-badge.gold {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}

.ach-tier-badge.platinum {
  background: rgba(6, 182, 212, 0.1);
  color: #0891b2;
}

/* 详情弹窗 */
.detail-icon-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.detail-icon {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  color: var(--text-secondary);
}

.detail-icon.tier-platinum { background: rgba(6, 182, 212, 0.1); color: #0891b2; }
.detail-icon.tier-gold { background: rgba(245, 158, 11, 0.1); color: #d97706; }
.detail-icon.tier-silver { background: rgba(148, 163, 184, 0.15); color: #64748b; }
.detail-icon.tier-bronze { background: rgba(217, 119, 6, 0.1); color: #d97706; }

.detail-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  text-align: center;
  margin: 0 0 8px;
}

.detail-desc {
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
  margin: 0 0 16px;
}

.detail-meta {
  display: flex;
  justify-content: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.detail-unlocked {
  text-align: center;
  font-size: 13px;
  color: var(--color-primary);
  padding: 10px;
  background: rgba(99, 102, 241, 0.06);
  border-radius: var(--radius-md);
}

.detail-progress {
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
}

.detail-bar {
  height: 6px;
  background: var(--bg-page);
  border-radius: 3px;
  overflow: hidden;
  margin-top: 8px;
}

.detail-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 3px;
  transition: width 0.5s ease;
}
</style>
