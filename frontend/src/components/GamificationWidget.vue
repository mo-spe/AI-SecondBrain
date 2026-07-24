<template>
  <div class="gamification-widget">
    <div class="widget-header">
      <h4 class="widget-title">学习成长</h4>
      <router-link to="/achievements" class="widget-link">全部成就</router-link>
    </div>

    <div class="widget-body" v-if="profile">
      <div class="level-section">
        <div class="level-badge">
          <span class="level-num">Lv.{{ profile.level }}</span>
        </div>
        <div class="xp-bar-wrap">
          <div class="xp-bar">
            <div class="xp-fill" :style="{ width: profile.levelProgressPercent + '%' }" />
          </div>
          <span class="xp-text">{{ profile.experience }} / {{ profile.experienceToNextLevel }} XP</span>
        </div>
      </div>

      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-icon">⭐</span>
          <span class="stat-value">{{ formatNumber(profile.totalPoints) }}</span>
          <span class="stat-label">积分</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">🔥</span>
          <span class="stat-value">{{ profile.currentStreak }}</span>
          <span class="stat-label">连续天</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">🎯</span>
          <span class="stat-value">{{ profile.accuracyRate }}%</span>
          <span class="stat-label">正确率</span>
        </div>
      </div>

      <div class="next-achievement" v-if="profile.nextAchievement" @click="$router.push('/achievements')">
        <span class="next-label">下一个成就</span>
        <div class="next-info">
          <span class="next-name">{{ profile.nextAchievement.name }}</span>
          <span class="next-progress">{{ profile.nextAchievement.progressPercent }}%</span>
        </div>
        <div class="next-bar">
          <div class="next-fill" :style="{ width: profile.nextAchievement.progressPercent + '%' }" />
        </div>
      </div>

      <div class="widget-actions">
        <CheckInButton @checkedIn="onCheckedIn" />
        <button
          v-if="showMakeup"
          class="makeup-btn"
          @click="handleMakeup"
          :disabled="makeupLoading"
        >
          补签 ({{ profile.makeupCardsRemaining }})
        </button>
      </div>
    </div>

    <div v-else class="widget-empty">
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useGamificationStore } from "@/stores/gamification";
import CheckInButton from "@/components/CheckInButton.vue";

const gamificationStore = useGamificationStore();
const makeupLoading = ref(false);

const profile = computed(() => gamificationStore.profile);

const showMakeup = computed(() => {
  if (!profile.value) return false;
  if (profile.value.checkedInToday) return false;
  return profile.value.makeupCardsRemaining > 0;
});

const formatNumber = (n) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + "万";
  if (n >= 1000) return (n / 1000).toFixed(1) + "k";
  return String(n);
};

const onCheckedIn = () => {
  gamificationStore.fetchProfile();
};

const handleMakeup = async () => {
  try {
    await ElMessageBox.confirm(
      `使用一张补签卡恢复昨天的签到？剩余：${profile.value.makeupCardsRemaining}张`,
      "补签确认",
      { confirmButtonText: "确认使用", cancelButtonText: "取消", type: "info" }
    );
  } catch {
    return;
  }
  makeupLoading.value = true;
  try {
    await gamificationStore.useMakeupCard();
    ElMessage.success("补签成功，连续天数已恢复");
    gamificationStore.fetchProfile();
  } catch (e) {
    ElMessage.error(e.message || "补签失败");
  } finally {
    makeupLoading.value = false;
  }
};

onMounted(() => {
  gamificationStore.fetchProfile();
});
</script>

<style scoped>
.gamification-widget {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px;
  border: 1px solid var(--border-lighter);
}

.widget-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.widget-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.widget-link {
  font-size: 12px;
  color: var(--color-primary);
  text-decoration: none;
}

.widget-link:hover {
  text-decoration: underline;
}

.level-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.level-badge {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.level-num {
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}

.xp-bar-wrap {
  flex: 1;
  min-width: 0;
}

.xp-bar {
  height: 6px;
  background: var(--bg-page);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 4px;
}

.xp-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.xp-text {
  font-size: 11px;
  color: var(--text-secondary);
}

.stats-row {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 4px;
  background: var(--bg-page);
  border-radius: var(--radius-md);
}

.stat-icon {
  font-size: 16px;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 11px;
  color: var(--text-secondary);
}

.next-achievement {
  padding: 10px;
  background: rgba(99, 102, 241, 0.04);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.next-achievement:hover {
  background: rgba(99, 102, 241, 0.08);
}

.next-label {
  font-size: 11px;
  color: var(--text-secondary);
  display: block;
  margin-bottom: 4px;
}

.next-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.next-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.next-progress {
  font-size: 11px;
  color: var(--color-primary);
}

.next-bar {
  height: 4px;
  background: var(--bg-page);
  border-radius: 2px;
  overflow: hidden;
}

.next-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 2px;
  transition: width 0.5s ease;
}

.widget-actions {
  display: flex;
  gap: 8px;
}

.makeup-btn {
  padding: 8px 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.makeup-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.makeup-btn:disabled {
  opacity: 0.5;
  cursor: default;
}

.widget-empty {
  text-align: center;
  padding: 20px 0;
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
