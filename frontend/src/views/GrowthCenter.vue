<template>
  <main class="growth-center">
    <section class="growth-hero" :aria-busy="profileLoading">
      <div class="hero-copy">
        <p class="eyebrow">LEARNING MOMENTUM</p>
        <h1>成长中心</h1>
        <p class="hero-description">把每一次专注、复习与分享，沉淀成看得见的长期成长。</p>

        <div class="level-progress">
          <div class="level-mark" aria-hidden="true">
            <el-icon :size="22"><Trophy /></el-icon>
            <strong>Lv.{{ profile?.level ?? "—" }}</strong>
          </div>
          <div class="experience-copy">
            <div class="experience-label">
              <span>当前学习等级</span>
              <strong>{{ formatNumber(profile?.experience) }} / {{ formatNumber(profile?.experienceToNextLevel) }} XP</strong>
            </div>
            <div
              class="experience-track"
              role="progressbar"
              aria-label="等级经验进度"
              aria-valuemin="0"
              aria-valuemax="100"
              :aria-valuenow="profileProgress"
            >
              <span :style="{ width: `${profileProgress}%` }" />
            </div>
          </div>
        </div>

        <button
          v-if="profile?.nextAchievement"
          class="next-achievement-link"
          type="button"
          @click="activateTab('achievements')"
        >
          <span class="next-achievement-icon"><el-icon><Medal /></el-icon></span>
          <span class="next-achievement-copy">
            <small>下一枚成就</small>
            <strong>{{ profile.nextAchievement.name }}</strong>
          </span>
          <span class="next-achievement-progress">{{ profile.nextAchievement.progressPercent ?? 0 }}%</span>
          <el-icon class="next-arrow"><ArrowRight /></el-icon>
        </button>
      </div>

      <dl class="hero-metrics" aria-label="学习成长数据">
        <div class="metric-card points">
          <dt>累计积分</dt>
          <dd>{{ formatNumber(profile?.totalPoints) }}</dd>
          <span><el-icon><Coin /></el-icon>长期积累</span>
        </div>
        <div class="metric-card streak">
          <dt>连续学习</dt>
          <dd>{{ profile?.currentStreak ?? "—" }}<small v-if="profile">天</small></dd>
          <span><el-icon><Calendar /></el-icon>最长 {{ profile?.maxStreak ?? 0 }} 天</span>
        </div>
        <div class="metric-card accuracy">
          <dt>复习正确率</dt>
          <dd>{{ formatPercent(profile?.accuracyRate) }}</dd>
          <span><el-icon><Aim /></el-icon>{{ formatNumber(profile?.totalReviewCount) }} 次复习</span>
        </div>
        <div class="metric-card achievements">
          <dt>已解锁成就</dt>
          <dd>{{ achievementsLoaded ? unlockedCount : "—" }}</dd>
          <span><el-icon><Collection /></el-icon>共 {{ achievementsLoaded ? achievements.length : "—" }} 枚</span>
        </div>
      </dl>
    </section>

    <section class="growth-workspace">
      <el-tabs v-model="activeTab" class="growth-tabs" @tab-change="onTabChange">
        <el-tab-pane name="achievements">
          <template #label>
            <span class="tab-label"><el-icon><Trophy /></el-icon>我的成长</span>
          </template>

          <div class="workspace-heading">
            <div>
              <p class="section-kicker">ACHIEVEMENT CABINET</p>
              <h2>把努力收进自己的成就册</h2>
              <p>筛选查看已点亮与正在推进的学习里程碑。</p>
            </div>
            <div v-if="achievementsLoaded" class="achievement-summary" aria-label="成就完成情况">
              <strong>{{ unlockedCount }}</strong>
              <span>/ {{ achievements.length }} 已解锁</span>
            </div>
          </div>

          <div class="achievement-filters" aria-label="成就筛选">
            <div class="filter-stack">
              <span class="filter-label">成长领域</span>
              <el-radio-group v-model="category" aria-label="成就成长领域" @change="loadAchievements">
                <el-radio-button value="">全部</el-radio-button>
                <el-radio-button value="review">复习</el-radio-button>
                <el-radio-button value="knowledge">知识</el-radio-button>
                <el-radio-button value="streak">打卡</el-radio-button>
                <el-radio-button value="accuracy">正确率</el-radio-button>
                <el-radio-button value="mastery">精通</el-radio-button>
              </el-radio-group>
            </div>
            <div class="filter-stack filter-status">
              <span class="filter-label">解锁状态</span>
              <el-radio-group v-model="filter" aria-label="成就解锁状态" @change="loadAchievements">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="unlocked">已解锁</el-radio-button>
                <el-radio-button value="locked">未解锁</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div v-loading="achievementsLoading" class="achievement-grid" aria-live="polite">
            <button
              v-for="achievement in achievements"
              :key="achievement.id"
              type="button"
              class="achievement-card"
              :class="[`tier-${achievement.tier}`, { unlocked: achievement.unlocked }]"
              :aria-label="`${achievement.name}，${achievement.unlocked ? '已解锁' : `进度 ${achievement.progressPercent ?? 0}%`}，查看详情`"
              @click="showAchievement(achievement)"
            >
              <span class="achievement-icon" aria-hidden="true">
                <el-icon :size="26"><component :is="iconMap[achievement.icon] || Trophy" /></el-icon>
              </span>
              <span class="achievement-content">
                <span class="achievement-title-row">
                  <strong>{{ achievement.name }}</strong>
                  <span class="tier-badge">{{ tierLabel(achievement.tier) }}</span>
                </span>
                <span class="achievement-description">{{ achievement.description }}</span>
                <span v-if="achievement.unlocked" class="unlocked-meta">
                  <span>{{ formatDate(achievement.unlockedAt) }} 解锁</span>
                  <span v-if="achievement.pointsReward">+{{ achievement.pointsReward }} 积分</span>
                </span>
                <span v-else class="achievement-progress">
                  <span class="achievement-progress-track"><span :style="{ width: `${achievement.progressPercent ?? 0}%` }" /></span>
                  <span>{{ achievement.currentValue ?? 0 }}/{{ achievement.triggerValue ?? 0 }}</span>
                </span>
              </span>
              <el-icon class="achievement-arrow" aria-hidden="true"><ArrowRight /></el-icon>
            </button>
          </div>

          <el-empty v-if="!achievementsLoading && achievements.length === 0" description="暂无符合条件的成就" />
        </el-tab-pane>

        <el-tab-pane name="leaderboard">
          <template #label>
            <span class="tab-label"><el-icon><TrendCharts /></el-icon>排行榜</span>
          </template>

          <div class="workspace-heading leaderboard-heading">
            <div>
              <p class="section-kicker">LEARNING LEADERBOARD</p>
              <h2>在同一段学习旅程里相互看见</h2>
              <p>排名只是一种回望，更重要的是持续向前的节奏。</p>
            </div>
            <div v-if="currentUser" class="my-rank-panel">
              <span>我的排名</span>
              <strong>#{{ currentUser.rank ?? "—" }}</strong>
              <small>{{ formatNumber(currentUser.score) }} 分</small>
            </div>
          </div>

          <div class="leaderboard-controls" aria-label="排行榜筛选">
            <el-radio-group v-model="period" aria-label="排行榜周期" @change="loadLeaderboard">
              <el-radio-button value="daily">今日</el-radio-button>
              <el-radio-button value="weekly">本周</el-radio-button>
              <el-radio-button value="monthly">本月</el-radio-button>
              <el-radio-button value="all">总榜</el-radio-button>
            </el-radio-group>
            <label class="domain-select">
              <span>领域</span>
              <el-select v-model="domain" aria-label="排行榜领域" @change="loadLeaderboard">
                <el-option label="全部领域" value="all" />
              </el-select>
            </label>
          </div>

          <div v-loading="leaderboardLoading" class="leaderboard-content" aria-live="polite">
            <div v-if="podiumEntries.length" class="podium" aria-label="排行榜前三名">
              <article
                v-for="entry in podiumEntries"
                :key="entry.userId"
                class="podium-card"
                :class="[`rank-${entry.rank}`, { 'is-me': isCurrentUser(entry) }]"
              >
                <span class="podium-rank" aria-hidden="true">{{ entry.rank }}</span>
                <el-avatar :size="entry.rank === 1 ? 58 : 46" :src="entry.avatar">
                  {{ entry.username?.charAt(0) }}
                </el-avatar>
                <strong>{{ entry.username || "匿名学习者" }}</strong>
                <span>Lv.{{ entry.level ?? 1 }}</span>
                <b>{{ formatNumber(entry.score) }} 分</b>
              </article>
            </div>

            <div v-if="entries.length" class="rank-list" aria-label="完整排行榜">
              <article
                v-for="entry in remainingEntries"
                :key="entry.userId"
                class="rank-row"
                :class="{ 'is-me': isCurrentUser(entry) }"
              >
                <span class="rank-index">{{ entry.rank }}</span>
                <el-avatar :size="38" :src="entry.avatar">{{ entry.username?.charAt(0) }}</el-avatar>
                <span class="rank-person">
                  <strong>{{ entry.username || "匿名学习者" }}</strong>
                  <small>Lv.{{ entry.level ?? 1 }}</small>
                </span>
                <b>{{ formatNumber(entry.score) }} <small>分</small></b>
              </article>
            </div>
          </div>

          <el-empty v-if="!leaderboardLoading && entries.length === 0" description="当前周期暂无排行数据" />
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="detailVisible" title="成就详情" width="420px" class="achievement-dialog">
      <div v-if="selectedAchievement" class="achievement-detail">
        <div class="detail-icon" :class="`tier-${selectedAchievement.tier}`">
          <el-icon :size="38"><component :is="iconMap[selectedAchievement.icon] || Trophy" /></el-icon>
        </div>
        <p class="section-kicker">{{ tierLabel(selectedAchievement.tier) }} 成就</p>
        <h2>{{ selectedAchievement.name }}</h2>
        <p>{{ selectedAchievement.description }}</p>
        <div class="detail-rewards">
          <span v-if="selectedAchievement.pointsReward"><el-icon><Coin /></el-icon>+{{ selectedAchievement.pointsReward }} 积分</span>
          <span v-if="selectedAchievement.makeupCardReward"><el-icon><Calendar /></el-icon>+{{ selectedAchievement.makeupCardReward }} 补签卡</span>
        </div>
        <div v-if="selectedAchievement.unlocked" class="detail-status unlocked-status">
          已于 {{ formatDate(selectedAchievement.unlockedAt) }} 解锁
        </div>
        <div v-else class="detail-status">
          <div><span>当前进度</span><strong>{{ selectedAchievement.currentValue ?? 0 }}/{{ selectedAchievement.triggerValue ?? 0 }}</strong></div>
          <span class="detail-progress-track"><span :style="{ width: `${selectedAchievement.progressPercent ?? 0}%` }" /></span>
        </div>
      </div>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useGamificationStore } from "@/stores/gamification";
import { useUserStore } from "@/stores/user";
import {
  Aim,
  ArrowRight,
  Calendar,
  Coin,
  Collection,
  Document,
  MagicStick,
  Medal,
  Star,
  StarFilled,
  Sunny,
  Sunrise,
  TrendCharts,
  Trophy,
} from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const gamificationStore = useGamificationStore();
const userStore = useUserStore();

const validTabs = new Set(["achievements", "leaderboard"]);
const activeTab = ref(validTabs.has(route.query.tab) ? route.query.tab : "achievements");
const profileLoading = ref(false);
const achievementsLoading = ref(false);
const leaderboardLoading = ref(false);
const achievementsLoaded = ref(false);
const achievements = ref([]);
const entries = ref([]);
const currentUser = ref(null);
const category = ref("");
const filter = ref("all");
const period = ref("daily");
const domain = ref("all");
const detailVisible = ref(false);
const selectedAchievement = ref(null);

const iconMap = {
  Medal,
  Trophy,
  Document,
  Collection,
  Sunny,
  Sunrise,
  Star,
  StarFilled,
  MagicStick,
};

const profile = computed(() => gamificationStore.profile);
const profileProgress = computed(() => clampPercent(profile.value?.levelProgressPercent));
const unlockedCount = computed(() => achievements.value.filter((item) => item.unlocked).length);
const podiumEntries = computed(() => {
  const topEntries = entries.value.filter((item) => Number(item.rank) <= 3);
  const byRank = new Map(topEntries.map((item) => [Number(item.rank), item]));
  return [byRank.get(2), byRank.get(1), byRank.get(3)].filter(Boolean);
});
const remainingEntries = computed(() => entries.value.filter((item) => Number(item.rank) > 3));

watch(
  () => route.query.tab,
  (tab) => {
    const normalizedTab = validTabs.has(tab) ? tab : "achievements";
    if (normalizedTab !== activeTab.value) activeTab.value = normalizedTab;
  }
);

const clampPercent = (value) => {
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) return 0;
  return Math.max(0, Math.min(100, Math.round(numericValue)));
};

const formatNumber = (value) => {
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) return "—";
  if (numericValue >= 10000) return `${(numericValue / 10000).toFixed(1)}万`;
  if (numericValue >= 1000) return `${(numericValue / 1000).toFixed(1)}k`;
  return String(numericValue);
};

const formatPercent = (value) => {
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? `${Math.round(numericValue)}%` : "—";
};

const formatDate = (value) => {
  if (!value) return "—";
  return new Date(value).toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
};

const tierLabel = (tier) => ({
  bronze: "青铜",
  silver: "白银",
  gold: "黄金",
  platinum: "铂金",
}[tier] || "成长");

const isCurrentUser = (entry) => Number(entry.userId) === Number(userStore.userInfo?.id);

const loadProfile = async () => {
  profileLoading.value = true;
  try {
    await gamificationStore.fetchProfile();
  } finally {
    profileLoading.value = false;
  }
};

const loadAchievements = async () => {
  achievementsLoading.value = true;
  try {
    await gamificationStore.fetchAchievements(
      category.value || undefined,
      filter.value === "all" ? undefined : filter.value
    );
    achievements.value = gamificationStore.achievements || [];
    achievementsLoaded.value = true;
  } finally {
    achievementsLoading.value = false;
  }
};

const loadLeaderboard = async () => {
  leaderboardLoading.value = true;
  try {
    await gamificationStore.fetchLeaderboard(period.value, domain.value, 100);
    entries.value = gamificationStore.leaderboard?.entries || [];
    currentUser.value = gamificationStore.leaderboard?.currentUser || null;
  } finally {
    leaderboardLoading.value = false;
  }
};

const activateTab = (tab) => {
  activeTab.value = tab;
  syncTabToRoute(tab);
};

const syncTabToRoute = (tab) => {
  router.replace({
    query: {
      ...route.query,
      tab,
    },
  });
};

const onTabChange = (tab) => {
  syncTabToRoute(tab);
  if (tab === "achievements" && !achievementsLoaded.value) {
    loadAchievements();
  }
  if (tab === "leaderboard" && entries.value.length === 0) {
    loadLeaderboard();
  }
};

const showAchievement = (achievement) => {
  selectedAchievement.value = achievement;
  detailVisible.value = true;
};

onMounted(async () => {
  await Promise.all([
    loadProfile(),
    loadAchievements(),
    activeTab.value === "leaderboard" ? loadLeaderboard() : Promise.resolve(),
  ]);
});
</script>

<style scoped>
.growth-center {
  width: min(100%, 1240px);
  margin: 0 auto;
  padding-bottom: var(--spacing-3xl);
}

.growth-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(360px, 0.95fr);
  gap: clamp(24px, 4vw, 56px);
  overflow: hidden;
  padding: clamp(28px, 4vw, 48px);
  border: 1px solid var(--border-light);
  border-left: 4px solid var(--color-primary);
  border-radius: 0 var(--radius-2xl) var(--radius-2xl) 0;
  background: var(--bg-card);
  box-shadow: var(--shadow-md);
}

.growth-hero::before {
  position: absolute;
  top: -190px;
  right: -120px;
  width: 440px;
  height: 440px;
  border: 1px solid var(--color-primary-alpha-15);
  border-radius: 50%;
  box-shadow: 0 0 0 36px var(--color-primary-alpha-10), 0 0 0 92px color-mix(in srgb, var(--color-primary-alpha-10) 55%, transparent);
  content: "";
  pointer-events: none;
}

.hero-copy,
.hero-metrics {
  position: relative;
  z-index: 1;
}

.eyebrow,
.section-kicker {
  margin: 0;
  color: var(--color-primary);
  font-family: var(--font-family-ui);
  font-size: 10px;
  font-weight: var(--font-weight-bold);
  letter-spacing: 0.15em;
  line-height: 1.4;
}

.hero-copy h1 {
  max-width: 8ch;
  margin: 8px 0 10px;
  color: var(--text-primary);
  font-size: clamp(38px, 4vw, 58px);
  font-weight: var(--font-weight-bold);
  letter-spacing: -0.045em;
}

.hero-description {
  max-width: 38ch;
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--font-size-md);
  line-height: var(--line-height-relaxed);
}

.level-progress {
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: 410px;
  margin-top: 28px;
}

.level-mark {
  display: grid;
  width: 58px;
  height: 58px;
  place-content: center;
  gap: 1px;
  flex: 0 0 auto;
  border: 1px solid var(--color-primary-alpha-20);
  border-radius: var(--radius-full);
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
  text-align: center;
}

.level-mark strong {
  font-family: var(--font-family-ui);
  font-size: 11px;
  line-height: 1;
}

.experience-copy {
  min-width: 0;
  flex: 1;
}

.experience-label {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-family: var(--font-family-ui);
  font-size: 12px;
}

.experience-label strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-semibold);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.experience-track,
.achievement-progress-track,
.detail-progress-track {
  display: block;
  height: 7px;
  overflow: hidden;
  border-radius: var(--radius-full);
  background: var(--bg-input);
}

.experience-track > span,
.achievement-progress-track > span,
.detail-progress-track > span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary);
  transition: width var(--transition-slow);
}

.next-achievement-link {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto 18px;
  align-items: center;
  gap: 10px;
  width: min(100%, 410px);
  min-height: 54px;
  margin-top: 22px;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  transition: border-color var(--transition-fast), background var(--transition-fast), transform var(--transition-fast);
}

.next-achievement-link:hover {
  border-color: var(--border-light);
  background: var(--bg-list-item);
  transform: translateX(2px);
}

.next-achievement-link:focus-visible,
.achievement-card:focus-visible {
  outline: 3px solid var(--color-primary-alpha-20);
  outline-offset: 3px;
}

.next-achievement-icon {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: var(--radius-full);
  background: var(--color-accent-alpha-10);
  color: var(--color-accent);
}

.next-achievement-copy,
.next-achievement-copy small,
.next-achievement-copy strong {
  min-width: 0;
}

.next-achievement-copy small,
.next-achievement-copy strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.next-achievement-copy small {
  color: var(--text-muted);
  font-family: var(--font-family-ui);
  font-size: 11px;
}

.next-achievement-copy strong {
  margin-top: 1px;
  color: var(--text-primary);
  font-size: 13px;
}

.next-achievement-progress {
  color: var(--color-primary);
  font-family: var(--font-family-ui);
  font-size: 12px;
  font-weight: var(--font-weight-semibold);
}

.next-arrow,
.achievement-arrow {
  color: var(--text-muted);
  transition: color var(--transition-fast), transform var(--transition-fast);
}

.next-achievement-link:hover .next-arrow,
.achievement-card:hover .achievement-arrow {
  color: var(--color-primary);
  transform: translateX(2px);
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-content: center;
  gap: 12px;
  margin: 0;
}

.metric-card {
  min-height: 130px;
  padding: 18px;
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--bg-card) 88%, var(--bg-page));
}

.metric-card dt {
  color: var(--text-secondary);
  font-family: var(--font-family-ui);
  font-size: 12px;
}

.metric-card dd {
  margin: 12px 0 6px;
  color: var(--text-primary);
  font-family: var(--font-family-display);
  font-size: clamp(30px, 3vw, 40px);
  font-weight: var(--font-weight-bold);
  line-height: 0.9;
}

.metric-card dd small {
  margin-left: 4px;
  color: var(--text-secondary);
  font-family: var(--font-family-ui);
  font-size: 12px;
}

.metric-card > span {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--text-muted);
  font-family: var(--font-family-ui);
  font-size: 11px;
}

.metric-card.points { border-top: 2px solid var(--color-primary); }
.metric-card.streak { border-top: 2px solid var(--color-accent); }
.metric-card.accuracy { border-top: 2px solid var(--color-info); }
.metric-card.achievements { border-top: 2px solid var(--color-success); }

.growth-workspace {
  margin-top: 28px;
  padding: clamp(20px, 3vw, 36px);
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-xl);
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
}

.growth-tabs :deep(.el-tabs__header) {
  margin: 0 0 30px;
}

.growth-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--border-lighter);
}

.growth-tabs :deep(.el-tabs__item) {
  height: 48px;
  padding: 0 20px;
  color: var(--text-secondary);
  font-family: var(--font-family-ui);
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
}

.growth-tabs :deep(.el-tabs__item:hover),
.growth-tabs :deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
}

.growth-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.workspace-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.workspace-heading h2 {
  margin: 7px 0 5px;
  color: var(--text-primary);
  font-size: clamp(26px, 3vw, 34px);
  font-weight: var(--font-weight-bold);
  letter-spacing: -0.025em;
}

.workspace-heading p:not(.section-kicker) {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.achievement-summary {
  display: flex;
  align-items: baseline;
  gap: 3px;
  min-width: max-content;
  padding: 10px 0 10px 18px;
  border-left: 2px solid var(--color-primary);
  color: var(--text-secondary);
  font-family: var(--font-family-ui);
  font-size: 12px;
}

.achievement-summary strong {
  color: var(--color-primary);
  font-family: var(--font-family-display);
  font-size: 30px;
  line-height: 1;
}

.achievement-filters,
.leaderboard-controls {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding: 16px 18px;
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-lg);
  background: var(--bg-list-item);
}

.filter-stack {
  display: grid;
  gap: 7px;
}

.filter-label,
.domain-select > span {
  color: var(--text-muted);
  font-family: var(--font-family-ui);
  font-size: 11px;
  font-weight: var(--font-weight-semibold);
}

.achievement-filters :deep(.el-radio-group),
.leaderboard-controls :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.achievement-filters :deep(.el-radio-button),
.leaderboard-controls :deep(.el-radio-button) {
  margin: 0;
}

.achievement-filters :deep(.el-radio-button__inner),
.leaderboard-controls :deep(.el-radio-button__inner) {
  min-height: 44px;
  padding: 12px;
  border: 1px solid var(--border-light) !important;
  border-radius: var(--radius-sm) !important;
  box-shadow: none !important;
  color: var(--text-secondary);
  font-family: var(--font-family-ui);
  font-size: 12px;
}

.achievement-filters :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner),
.leaderboard-controls :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  border-color: var(--color-primary) !important;
  background: var(--color-primary) !important;
  color: var(--text-inverse);
}

.domain-select {
  display: grid;
  gap: 7px;
  min-width: 160px;
}

.domain-select :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: var(--radius-sm);
  background: var(--bg-card);
}

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  min-height: 120px;
}

.achievement-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 18px;
  align-items: center;
  gap: 13px;
  width: 100%;
  min-height: 132px;
  padding: 16px;
  border: 1px solid var(--border-lighter);
  border-left: 3px solid var(--border-base);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition: border-color var(--transition-base), box-shadow var(--transition-base), transform var(--transition-base), background var(--transition-base);
}

.achievement-card:hover {
  border-color: var(--border-base);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.achievement-card.unlocked { border-left-color: var(--color-primary); }
.achievement-card.tier-gold.unlocked { border-left-color: var(--color-accent); }
.achievement-card.tier-platinum.unlocked { border-left-color: var(--color-info); }

.achievement-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-md);
  background: var(--bg-list-item);
  color: var(--text-muted);
}

.achievement-card.unlocked .achievement-icon {
  border-color: var(--color-primary-alpha-20);
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
}

.achievement-card.tier-gold.unlocked .achievement-icon {
  border-color: var(--color-accent-alpha-10);
  background: var(--color-accent-alpha-10);
  color: var(--color-accent);
}

.achievement-card.tier-platinum.unlocked .achievement-icon {
  border-color: var(--color-info-border);
  background: var(--color-info-bg);
  color: var(--color-info);
}

.achievement-content {
  min-width: 0;
}

.achievement-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.achievement-title-row strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tier-badge {
  flex: 0 0 auto;
  color: var(--text-muted);
  font-family: var(--font-family-ui);
  font-size: 10px;
}

.achievement-description {
  display: -webkit-box;
  overflow: hidden;
  margin: 4px 0 10px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.unlocked-meta,
.achievement-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-family: var(--font-family-ui);
  font-size: 10px;
}

.unlocked-meta span + span {
  color: var(--color-primary);
}

.achievement-progress-track {
  min-width: 72px;
  flex: 1;
  height: 5px;
}

.achievement-progress-track > span,
.detail-progress-track > span {
  background: var(--text-muted);
}

.achievement-card.unlocked .achievement-progress-track > span {
  background: var(--color-primary);
}

.my-rank-panel {
  display: grid;
  grid-template-columns: auto auto;
  align-items: baseline;
  column-gap: 7px;
  min-width: 150px;
  padding: 10px 0 10px 18px;
  border-left: 2px solid var(--color-accent);
}

.my-rank-panel span,
.my-rank-panel small {
  color: var(--text-muted);
  font-family: var(--font-family-ui);
  font-size: 11px;
}

.my-rank-panel strong {
  color: var(--color-accent);
  font-family: var(--font-family-display);
  font-size: 30px;
  line-height: 1;
}

.my-rank-panel small { grid-column: 1 / -1; margin-top: 3px; }

.leaderboard-controls { align-items: end; }

.leaderboard-content {
  min-height: 160px;
}

.podium {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.16fr) minmax(0, 0.92fr);
  align-items: end;
  gap: 12px;
  margin-bottom: 18px;
}

.podium-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 162px;
  padding: 22px 12px 16px;
  border: 1px solid var(--border-lighter);
  border-top: 3px solid var(--border-base);
  border-radius: var(--radius-lg);
  background: var(--bg-list-item);
  text-align: center;
}

.podium-card.rank-1 {
  min-height: 190px;
  border-top-color: var(--color-accent);
  background: color-mix(in srgb, var(--color-accent-alpha-10) 54%, var(--bg-card));
}

.podium-card.rank-2 { border-top-color: var(--color-info); }
.podium-card.rank-3 { border-top-color: var(--color-primary); }
.podium-card.is-me { box-shadow: inset 0 0 0 1px var(--color-primary); }

.podium-rank {
  position: absolute;
  top: 10px;
  left: 11px;
  color: var(--text-muted);
  font-family: var(--font-family-display);
  font-size: 21px;
  font-weight: var(--font-weight-bold);
  line-height: 1;
}

.podium-card :deep(.el-avatar) {
  margin-top: 3px;
  border: 2px solid var(--bg-card);
  background: var(--color-primary-alpha-15);
  color: var(--color-primary);
}

.podium-card strong {
  display: block;
  overflow: hidden;
  max-width: 100%;
  margin-top: 9px;
  color: var(--text-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.podium-card > span:not(.podium-rank) {
  margin-top: 2px;
  color: var(--text-muted);
  font-family: var(--font-family-ui);
  font-size: 11px;
}

.podium-card b {
  margin-top: 6px;
  color: var(--color-primary);
  font-family: var(--font-family-ui);
  font-size: 12px;
}

.podium-card.rank-1 b { color: var(--color-accent); }

.rank-list {
  display: grid;
  gap: 6px;
}

.rank-row {
  display: grid;
  grid-template-columns: 38px 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 62px;
  padding: 10px 14px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: var(--bg-card);
  transition: background var(--transition-fast), border-color var(--transition-fast), transform var(--transition-fast);
}

.rank-row:hover {
  border-color: var(--border-lighter);
  background: var(--bg-list-item);
  transform: translateX(2px);
}

.rank-row.is-me {
  border-color: var(--color-primary-alpha-20);
  background: var(--color-primary-alpha-10);
}

.rank-index {
  color: var(--text-muted);
  font-family: var(--font-family-ui);
  font-size: 13px;
  font-weight: var(--font-weight-semibold);
  text-align: center;
}

.rank-row :deep(.el-avatar) {
  background: var(--color-primary-alpha-15);
  color: var(--color-primary);
}

.rank-person {
  min-width: 0;
}

.rank-person strong,
.rank-person small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-person strong { color: var(--text-primary); font-size: 13px; }
.rank-person small { margin-top: 2px; color: var(--text-muted); font-family: var(--font-family-ui); font-size: 10px; }

.rank-row > b {
  color: var(--text-primary);
  font-family: var(--font-family-ui);
  font-size: 13px;
  white-space: nowrap;
}

.rank-row > b small {
  color: var(--text-muted);
  font-size: 10px;
  font-weight: var(--font-weight-normal);
}

.achievement-detail { text-align: center; }

.detail-icon {
  display: grid;
  width: 78px;
  height: 78px;
  place-items: center;
  margin: 0 auto 18px;
  border-radius: var(--radius-full);
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
}

.detail-icon.tier-gold { background: var(--color-accent-alpha-10); color: var(--color-accent); }
.detail-icon.tier-platinum { background: var(--color-info-bg); color: var(--color-info); }
.detail-icon.tier-silver { background: var(--bg-input); color: var(--text-secondary); }
.detail-icon.tier-bronze { background: var(--color-warning-bg); color: var(--color-warning); }

.achievement-detail h2 {
  margin: 7px 0;
  color: var(--text-primary);
  font-size: 27px;
}

.achievement-detail > p:not(.section-kicker) {
  margin: 0 auto;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.65;
}

.detail-rewards {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 18px 0;
}

.detail-rewards span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  border-radius: var(--radius-sm);
  background: var(--bg-list-item);
  color: var(--color-primary);
  font-family: var(--font-family-ui);
  font-size: 11px;
}

.detail-status {
  padding: 12px;
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-family: var(--font-family-ui);
  font-size: 12px;
  text-align: left;
}

.detail-status > div {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.detail-status strong { color: var(--text-primary); }
.detail-status.unlocked-status { border-color: var(--color-success-border); background: var(--color-success-bg); color: var(--color-success); text-align: center; }

@media (max-width: 1000px) {
  .growth-hero { grid-template-columns: 1fr; }
  .hero-metrics { grid-template-columns: repeat(4, minmax(0, 1fr)); }
  .metric-card { min-height: 118px; padding: 14px; }
}

@media (max-width: 760px) {
  .growth-center { padding-bottom: 32px; }
  .growth-hero { gap: 26px; padding: 28px 22px; }
  .growth-hero::before { top: -250px; right: -200px; }
  .hero-copy h1 { font-size: 42px; }
  .hero-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .metric-card { min-height: 112px; }
  .growth-workspace { margin-top: 18px; padding: 20px 16px; }
  .growth-tabs :deep(.el-tabs__header) { margin-bottom: 24px; }
  .growth-tabs :deep(.el-tabs__item) { padding: 0 14px; }
  .workspace-heading { align-items: flex-start; flex-direction: column; gap: 14px; }
  .achievement-summary, .my-rank-panel { padding-left: 12px; }
  .achievement-filters, .leaderboard-controls { align-items: stretch; flex-direction: column; gap: 15px; padding: 14px; }
  .filter-status { width: 100%; }
  .domain-select { width: 100%; }
  .achievement-grid { grid-template-columns: 1fr; }
}

@media (max-width: 480px) {
  .level-progress { align-items: flex-start; }
  .experience-label { align-items: flex-start; flex-direction: column; gap: 2px; }
  .next-achievement-link { grid-template-columns: 30px minmax(0, 1fr) auto; }
  .next-arrow { display: none; }
  .metric-card { padding: 13px; }
  .metric-card dd { font-size: 31px; }
  .metric-card > span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .achievement-card { grid-template-columns: 44px minmax(0, 1fr); min-height: 126px; padding: 14px; }
  .achievement-icon { width: 44px; height: 44px; }
  .achievement-arrow { display: none; }
  .podium { grid-template-columns: 1fr; align-items: stretch; }
  .podium-card, .podium-card.rank-1 { min-height: 134px; }
  .podium-card.rank-1 { order: -1; }
  .rank-row { grid-template-columns: 30px 38px minmax(0, 1fr) auto; gap: 8px; padding: 10px; }
}
</style>
