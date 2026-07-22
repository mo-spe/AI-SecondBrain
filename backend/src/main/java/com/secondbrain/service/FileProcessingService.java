package com.secondbrain.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件处理服务接口.
 * <p>提供文件内容的提取和处理功能</p>
 */
public interface FileProcessingService {

    /**
     * 处理上传的文件并提取内容.
     *
     * @param file 上传的文件
     * @return 提取的文本内容
     */
    String processFile(MultipartFile file);
}
