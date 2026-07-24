package com.secondbrain.dto;

/**
 * AI调用配置DTO.
 * <p>封装一次AI调用所需的全部配置信息，由 resolveConfig 方法构建</p>
 */
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

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiType() { return apiType; }
    public void setApiType(String apiType) { this.apiType = apiType; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}
