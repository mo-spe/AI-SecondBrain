package com.secondbrain.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentCaptureService {

    String extractContent(MultipartFile file) throws Exception;

    void sendToCapture(String content, Long userId, String source);
}
