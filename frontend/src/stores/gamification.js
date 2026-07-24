import { defineStore } from "pinia";
import { ref } from "vue";
import { gamificationAPI } from "@/api/gamification";

export const useGamificationStore = defineStore("gamification", () => {
  const profile = ref(null);
  const achievements = ref([]);
  const leaderboard = ref(null);
  const pointsLog = ref([]);
  const streakCalendar = ref([]);
  const loading = ref(false);

  const fetchProfile = async () => {
    try {
      profile.value = await gamificationAPI.getProfile();
    } catch {
      // ignore — not critical
    }
  };

  const checkIn = async () => {
    const result = await gamificationAPI.checkIn();
    profile.value = result;
    return result;
  };

  const fetchAchievements = async (category, filter) => {
    loading.value = true;
    try {
      achievements.value = await gamificationAPI.getAchievements({ category, filter });
    } catch {
      // ignore
    } finally {
      loading.value = false;
    }
  };

  const fetchLeaderboard = async (period, domain, size) => {
    loading.value = true;
    try {
      leaderboard.value = await gamificationAPI.getLeaderboard({ period, domain, size });
    } catch {
      // ignore
    } finally {
      loading.value = false;
    }
  };

  const useMakeupCard = async () => {
    const result = await gamificationAPI.useMakeupCard();
    profile.value = result;
    return result;
  };

  const fetchPointsLog = async (current, size) => {
    try {
      const data = await gamificationAPI.getPointsLog({ current, size });
      pointsLog.value = data.records || [];
      return data;
    } catch {
      return { records: [], total: 0 };
    }
  };

  const fetchStreakCalendar = async (months) => {
    try {
      streakCalendar.value = await gamificationAPI.getStreakCalendar(months);
    } catch {
      streakCalendar.value = [];
    }
  };

  return {
    profile,
    achievements,
    leaderboard,
    pointsLog,
    streakCalendar,
    loading,
    fetchProfile,
    checkIn,
    fetchAchievements,
    fetchLeaderboard,
    useMakeupCard,
    fetchPointsLog,
    fetchStreakCalendar,
  };
});
