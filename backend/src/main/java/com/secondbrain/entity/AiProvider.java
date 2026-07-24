package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI服务商定义实体.
 * <p>管理员维护的可选AI服务商列表</p>
 */
@Getter
@Setter
@TableName("ai_provider")
public class AiProvider {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 服务商标识
     */
    private String code;

    /**
     * 服务商显示名称
     */
    private String name;

    /**
     * API基础地址
     */
    private String baseUrl;

    /**
     * API协议类型：openai_compatible/anthropic/gemini
     */
    private String apiType;

    /**
     * Logo图标地址
     */
    private String logoUrl;

    /**
     * 是否启用（0-禁用，1-启用）
     */
    private Integer isEnabled;

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
