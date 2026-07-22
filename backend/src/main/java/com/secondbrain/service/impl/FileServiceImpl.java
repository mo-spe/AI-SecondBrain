package com.secondbrain.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.secondbrain.config.OssConfig;
import com.secondbrain.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件服务实现类
 * 提供头像上传、文件删除等OSS文件操作功能
 */
@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final OssConfig ossConfig;

    public FileServiceImpl(OssConfig ossConfig) {
        this.ossConfig = ossConfig;
    }

    /**
     * 上传用户头像到OSS
     *
     * @param file   头像文件
     * @param userId 用户ID
     * @return 文件访问URL
     */
    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        OSS ossClient = getOssClient();
        if (ossClient == null) {
            log.warn("OSS客户端未配置，使用本地存储");
            return null;
        }
        
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            
            String fileName = ossConfig.getAvatarPath() + userId + "/" + 
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + 
                            "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            
            ossClient.putObject(ossConfig.getBucketName(), fileName, file.getInputStream(), metadata);
            
            String fileUrl = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + fileName;
            
            log.info("头像上传成功，userId: {}, fileName: {}", userId, fileName);
            return fileUrl;
        } catch (IOException e) {
            log.error("头像上传失败，userId: {}", userId, e);
            throw new IllegalStateException("头像上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除OSS上的文件
     *
     * @param fileUrl 文件URL
     */
    @Override
    public void deleteFile(String fileUrl) {
        OSS ossClient = getOssClient();
        if (ossClient == null) {
            log.warn("OSS客户端未配置，跳过文件删除");
            return;
        }
        
        try {
            String fileName = fileUrl.substring(fileUrl.indexOf("/", 8) + 1);
            ossClient.deleteObject(ossConfig.getBucketName(), fileName);
            log.info("文件删除成功，fileName: {}", fileName);
        } catch (Exception e) {
            log.error("文件删除失败，fileUrl: {}", fileUrl, e);
        }
    }
    
    private OSS getOssClient() {
        if (ossConfig.getEndpoint() == null || ossConfig.getEndpoint().isEmpty() || 
            ossConfig.getAccessKeyId() == null || ossConfig.getAccessKeyId().isEmpty() || 
            ossConfig.getAccessKeySecret() == null || ossConfig.getAccessKeySecret().isEmpty()) {
            return null;
        }
        return new com.aliyun.oss.OSSClientBuilder()
            .build(ossConfig.getEndpoint(), ossConfig.getAccessKeyId(), ossConfig.getAccessKeySecret());
    }
}
