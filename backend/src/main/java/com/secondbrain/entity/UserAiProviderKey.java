package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户服务商全局Key实体.
 * <p>用户可为每个服务商设置一个全局API Key，场景级Key为空时自动复用</p>
 */
@Getter
@Setter
@TableName("user_ai_provider_key")
public class UserAiProviderKey {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 关联ai_provider.id
     */
    private Long providerId;

    /**
     * AES-256-GCM加密存储的API Key
     */
    private String apiKey;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
