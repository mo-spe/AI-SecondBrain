import request from "@/utils/request";

export const userAPI = {
  getUserInfo() {
    return request({
      url: "/user/info",
      method: "get",
    });
  },

  updateUser(data) {
    return request({
      url: "/user/update",
      method: "put",
      data,
    });
  },

  updatePassword(data) {
    return request({
      url: "/user/password",
      method: "put",
      data,
    });
  },

  uploadAvatar(file) {
    const formData = new FormData();
    formData.append("file", file);

    return request({
      url: "/user/avatar",
      method: "post",
      data: formData,
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
  },
};
