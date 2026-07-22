package com.secondbrain.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文档采集服务接口.
 * <p>提供文档内容提取和采集功能</p>
 */
public interface DocumentCaptureService {

    /**
     * 提取文档内容.
     *
     * @param file 上传的文档文件
     * @return 文档文本内容
     */
    String extractContent(MultipartFile file);

    /**
     * 发送内容到采集处理.
     *
     * @param content 内容
     * @param userId 用户ID
     * @param source 来源
     */
    void sendToCapture(String content, Long userId, String source);
}
