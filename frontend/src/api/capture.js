import request from "@/utils/request";

export const captureAPI = {
  captureDocument(file, userId) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("userId", userId);

    return request({
      url: "/capture/document",
      method: "post",
      data: formData,
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
  },

  captureNote(title, content, userId) {
    return request({
      url: "/capture/note",
      method: "post",
      data: {
        title,
        content,
        userId,
      },
    });
  },
};
