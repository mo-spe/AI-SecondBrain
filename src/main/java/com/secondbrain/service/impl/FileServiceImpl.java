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

@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final OSS ossClient;
    private final OssConfig ossConfig;

    public FileServiceImpl(OSS ossClient, OssConfig ossConfig) {
        this.ossClient = ossClient;
        this.ossConfig = ossConfig;
    }

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
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
            throw new RuntimeException("头像上传失败：" + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.indexOf("/", 8) + 1);
            ossClient.deleteObject(ossConfig.getBucketName(), fileName);
            log.info("文件删除成功，fileName: {}", fileName);
        } catch (Exception e) {
            log.error("文件删除失败，fileUrl: {}", fileUrl, e);
        }
    }
}
