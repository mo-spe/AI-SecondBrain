package com.secondbrain.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 阿里云OSS配置. <p>配置OSS客户端连接参数</p> */
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
@Getter
@Setter
public class OssConfig {

    /** OSS 服务端点. */
    private String endpoint;

    /** OSS 访问密钥ID. */
    private String accessKeyId;

    /** OSS 访问密钥. */
    private String accessKeySecret;

    /** OSS 存储桶名称. */
    private String bucketName;

    /** 头像存储路径. */
    private String avatarPath = "avatar/";

    /**
     * 创建 OSS 客户端.
     *
     * @return OSS 客户端实例，参数缺失时返回 null
     */
    @Bean
    public OSS ossClient() {
        if (endpoint == null || endpoint.isEmpty() ||
            accessKeyId == null || accessKeyId.isEmpty() ||
            accessKeySecret == null || accessKeySecret.isEmpty()) {
            return null;
        }
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
