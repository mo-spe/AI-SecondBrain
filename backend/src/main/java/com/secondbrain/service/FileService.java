package com.secondbrain.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    String uploadAvatar(MultipartFile file, Long userId);

    void deleteFile(String fileUrl);
}
