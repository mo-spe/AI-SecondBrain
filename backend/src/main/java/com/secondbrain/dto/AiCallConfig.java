package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * AI调用配置DTO.
 * <p>封装一次AI调用所需的全部配置信息，由 resolveConfig 方法构建</p>
 */
@Getter
@Setter
public class AiCallConfig {

    /**
     * 服务商ID
     */
    private Long providerId;

    /**
     * 服务商标识
     */
    private String providerCode;

    /**
     * 服务商名称
     */
    private String providerName;

    /**
     * API地址
     */
    private String baseUrl;

    /**
     * API类型：openai_compatible / anthropic / gemini
     */
    private String apiType;

    /**
     * API Key（明文）
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String modelName;

}
