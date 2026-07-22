package com.secondbrain.service.impl;

import com.secondbrain.service.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 文件处理服务实现类.
 * <p>提供文件内容的提取功能，支持TXT和Markdown格式</p>
 */
@Service
public class FileProcessingServiceImpl implements FileProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingServiceImpl.class);

    @Override
    public String processFile(MultipartFile file) {
        log.info("开始处理文件：{}", file.getOriginalFilename());
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.info("文件处理成功，内容长度：{}", content.length());
            return content;
        } catch (IOException e) {
            log.error("文件处理失败：{}", file.getOriginalFilename(), e);
            throw new IllegalStateException("文件处理失败：" + e.getMessage());
        }
    }
}
