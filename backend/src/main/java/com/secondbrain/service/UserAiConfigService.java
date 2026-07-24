package com.secondbrain.service;

import java.util.List;
import java.util.Map;

/**
 * 用户AI配置服务.
 * <p>管理用户按场景的服务商、模型和API Key配置</p>
 */
public interface UserAiConfigService {

    /**
     * 获取用户所有场景的AI配置.
     * <p>返回的apiKey脱敏显示（sk-12****MN格式）</p>
     *
     * @param userId 用户ID
     * @return 场景配置列表，每个元素包含scenarioCode/providerId/modelName/apiKey/providerName等
     */
    List<Map<String, Object>> getUserAiConfig(Long userId);

    /**
     * 批量保存用户场景配置.
     * <p>apiKey为脱敏值或空时跳过更新，保留现有Key</p>
     *
     * @param userId  用户ID
     * @param configs 场景配置列表
     */
    void saveUserAiConfig(Long userId, List<Map<String, Object>> configs);

    /**
     * 保存用户对某服务商的全局API Key.
     *
     * @param userId     用户ID
     * @param providerId 服务商ID
     * @param apiKey     明文API Key
     */
    void saveProviderKey(Long userId, Long providerId, String apiKey);

    /**
     * 解析用户在某场景下应使用的API Key.
     * <p>优先使用场景级Key，为空时fallback到服务商全局Key</p>
     *
     * @param userId     用户ID
     * @param providerId 服务商ID
     * @param scenarioCode 场景代码
     * @return 明文API Key，未配置时返回null
     */
    String resolveApiKey(Long userId, Long providerId, String scenarioCode);
}
