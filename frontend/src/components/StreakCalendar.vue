<template>
  <div class="streak-calendar">
    <div class="calendar-header">
      <h4 class="calendar-title">学习热力图</h4>
      <span class="calendar-subtitle">最近 {{ months }} 个月</span>
    </div>
    <div class="calendar-grid" v-if="days.length > 0">
      <div
        v-for="day in days"
        :key="day.date"
        class="calendar-cell"
        :class="cellClass(day)"
        :title="cellTitle(day)"
      />
    </div>
    <div class="calendar-legend">
      <span class="legend-label">少</span>
      <span class="legend-dot level-0" />
      <span class="legend-dot level-1" />
      <span class="legend-dot level-2" />
      <span class="legend-dot level-3" />
      <span class="legend-dot level-4" />
      <span class="legend-label">多</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { gamificationAPI } from "@/api/gamification";

const props = defineProps({
  months: { type: Number, default: 3 },
});

const days = ref([]);

const cellClass = (day) => {
  // 颜色级别完全依据当日真实获得的积分（签到 + 复习 + 创建 + 成就等汇总）
  // hasReview / hasCheckIn 只用于 tooltip 展示，不直接决定颜色
  const p = day.pointsEarned || 0;
  // 只要有签到或复习活动，且当日有正积分，最低给到 level-1
  const active = day.hasCheckIn || day.hasReview;
  if (p <= 0 && !active) return "level-0";
  if (p <= 0 && active) return "level-1";  // 有活动但积分 0（旧数据/漏记）
  if (p <= 8) return "level-1";
  if (p <= 20) return "level-2";
  if (p <= 50) return "level-3";
  return "level-4";
};

const cellTitle = (day) => {
  const parts = [day.date];
  if (day.hasCheckIn) parts.push("已签到");
  if (day.hasReview) parts.push("已复习");
  if (day.pointsEarned > 0) parts.push(`+${day.pointsEarned}分`);
  return parts.join(" · ");
};

onMounted(async () => {
  try {
    days.value = await gamificationAPI.getStreakCalendar(props.months);
  } catch {
    days.value = [];
  }
});
</script>

<style scoped>
.streak-calendar {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px;
  border: 1px solid var(--border-lighter);
}

.calendar-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.calendar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.calendar-subtitle {
  font-size: 11px;
  color: var(--text-secondary);
}

.calendar-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  margin-bottom: 10px;
}

.calendar-cell {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  transition: transform 0.15s;
}

.calendar-cell:hover {
  transform: scale(1.3);
}

.level-0 { background: var(--border-lighter); }
.level-1 { background: #c7d2fe; }
.level-2 { background: #a5b4fc; }
.level-3 { background: #818cf8; }
.level-4 { background: #6366f1; }

.calendar-legend {
  display: flex;
  align-items: center;
  gap: 3px;
  justify-content: flex-end;
}

.legend-label {
  font-size: 10px;
  color: var(--text-secondary);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}
</style>
