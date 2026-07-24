package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI模型实体.
 * <p>管理员为每个服务商维护的预设模型列表</p>
 */
@Getter
@Setter
@TableName("ai_model")
public class AiModel {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联ai_provider.id
     */
    private Long providerId;

    /**
     * 模型标识
     */
    private String modelName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 是否启用（0-禁用，1-启用）
     */
    private Integer isEnabled;

    /**
     * 适用场景JSON数组
     */
    private String supportedScenarios;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
