import request from "@/utils/request";

export const statisticsAPI = {
  getStatistics() {
    return request({
      url: "/statistics",
      method: "get",
    });
  },

  getChartData(period) {
    return request({
      url: "/statistics/chart",
      method: "get",
      params: { period },
    });
  },
};
