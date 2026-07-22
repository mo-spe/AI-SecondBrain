package com.secondbrain.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口.
 * <p>提供文件上传、删除等功能</p>
 */
public interface FileService {

    /**
     * 上传头像.
     *
     * @param file 头像文件
     * @param userId 用户ID
     * @return 文件URL
     */
    String uploadAvatar(MultipartFile file, Long userId);

    /**
     * 删除文件.
     *
     * @param fileUrl 文件URL
     */
    void deleteFile(String fileUrl);
}
