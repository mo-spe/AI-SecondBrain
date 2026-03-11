import request from "@/utils/request";

export const exportAPI = {
  exportToMarkdown(ids = []) {
    return request({
      url: "/export/markdown",
      method: "post",
      data: ids,
      responseType: "blob",
    });
  },

  exportToPDF(ids = []) {
    return request({
      url: "/export/pdf",
      method: "post",
      data: ids,
      responseType: "blob",
    });
  },

  exportToWord(ids = []) {
    return request({
      url: "/export/word",
      method: "post",
      data: ids,
      responseType: "blob",
    });
  },

  exportToJSON(ids = []) {
    return request({
      url: "/export/json",
      method: "post",
      data: ids,
      responseType: "blob",
    });
  },

  exportToCSV(ids = []) {
    return request({
      url: "/export/csv",
      method: "post",
      data: ids,
      responseType: "blob",
    });
  },
};
