import request from "@/utils/request";

export const reportAPI = {
  generateReport(data) {
    return request({
      url: "/report/generate-async",
      method: "post",
      data,
    });
  },

  getReportList(params) {
    return request({
      url: "/report/list",
      method: "get",
      params,
    });
  },

  getReportDetail(id) {
    return request({
      url: `/report/detail/${id}`,
      method: "get",
    });
  },

  deleteReport(id) {
    return request({
      url: `/report/delete/${id}`,
      method: "delete",
    });
  },
};