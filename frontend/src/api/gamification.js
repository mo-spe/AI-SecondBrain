import request from "@/utils/request";

export const gamificationAPI = {
  getProfile() {
    return request({ url: "/gamification/profile", method: "get" });
  },

  checkIn() {
    return request({ url: "/gamification/check-in", method: "post" });
  },

  getAchievements(params) {
    return request({ url: "/gamification/achievements", method: "get", params });
  },

  getLeaderboard(params) {
    return request({ url: "/gamification/leaderboard", method: "get", params });
  },

  useMakeupCard() {
    return request({ url: "/gamification/makeup", method: "post" });
  },

  getPointsLog(params) {
    return request({ url: "/gamification/points-log", method: "get", params });
  },

  getStreakCalendar(months) {
    return request({ url: "/gamification/streak-calendar", method: "get", params: { months } });
  },
};
