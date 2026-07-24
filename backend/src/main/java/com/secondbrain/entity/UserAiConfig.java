package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户AI场景配置实体.
 * <p>每个用户每个场景一条配置记录，api_key以AES-256-GCM加密存储</p>
 */
@Getter
@Setter
@TableName("user_ai_config")
public class UserAiConfig {

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
     * 场景代码：chat/extraction/question_gen/embedding/research
     */
    private String scenarioCode;

    /**
     * 关联ai_provider.id
     */
    private Long providerId;

    /**
     * 用户选择的模型名称
     */
    private String modelName;

    /**
     * AES-256-GCM加密存储的用户API Key
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
