package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.AiProvider;
import com.secondbrain.entity.UserAiConfig;
import com.secondbrain.entity.UserAiProviderKey;
import com.secondbrain.mapper.AiProviderMapper;
import com.secondbrain.mapper.UserAiConfigMapper;
import com.secondbrain.mapper.UserAiProviderKeyMapper;
import com.secondbrain.service.ApiKeyEncryptionService;
import com.secondbrain.service.UserAiConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户AI配置服务实现.
 * <p>管理用户按场景的AI服务商、模型和API Key配置</p>
 */
@Service
public class UserAiConfigServiceImpl implements UserAiConfigService {

    private static final Logger log = LoggerFactory.getLogger(UserAiConfigServiceImpl.class);

    private final UserAiConfigMapper userAiConfigMapper;
    private final UserAiProviderKeyMapper userAiProviderKeyMapper;
    private final ApiKeyEncryptionService encryptionService;
    private final AiProviderMapper aiProviderMapper;

    public UserAiConfigServiceImpl(UserAiConfigMapper userAiConfigMapper,
                                   UserAiProviderKeyMapper userAiProviderKeyMapper,
                                   ApiKeyEncryptionService encryptionService,
                                   AiProviderMapper aiProviderMapper) {
        this.userAiConfigMapper = userAiConfigMapper;
        this.userAiProviderKeyMapper = userAiProviderKeyMapper;
        this.encryptionService = encryptionService;
        this.aiProviderMapper = aiProviderMapper;
    }

    @Override
    public List<Map<String, Object>> getUserAiConfig(Long userId) {
        List<UserAiConfig> configs = userAiConfigMapper.selectList(
                new LambdaQueryWrapper<UserAiConfig>().eq(UserAiConfig::getUserId, userId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserAiConfig config : configs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", config.getId());
            map.put("userId", config.getUserId());
            map.put("scenarioCode", config.getScenarioCode());
            map.put("providerId", config.getProviderId());
            map.put("modelName", config.getModelName());
            map.put("apiKey", maskApiKey(config.getApiKey()));

            AiProvider provider = aiProviderMapper.selectById(config.getProviderId());
            if (provider != null) {
                map.put("providerName", provider.getName());
                map.put("providerCode", provider.getCode());
            }
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public void saveUserAiConfig(Long userId, List<Map<String, Object>> configs) {
        for (Map<String, Object> item : configs) {
            String scenarioCode = (String) item.get("scenarioCode");
            if (scenarioCode == null || scenarioCode.isBlank()) {
                continue;
            }

            UserAiConfig existing = userAiConfigMapper.selectOne(
                    new LambdaQueryWrapper<UserAiConfig>()
                            .eq(UserAiConfig::getUserId, userId)
                            .eq(UserAiConfig::getScenarioCode, scenarioCode));

            UserAiConfig entity = existing != null ? existing : new UserAiConfig();
            entity.setUserId(userId);
            entity.setScenarioCode(scenarioCode);

            Object providerIdObj = item.get("providerId");
            if (providerIdObj != null) {
                entity.setProviderId(Long.valueOf(providerIdObj.toString()));
            }

            Object modelNameObj = item.get("modelName");
            if (modelNameObj != null) {
                entity.setModelName(modelNameObj.toString());
            }

            String apiKeyInput = item.get("apiKey") != null ? item.get("apiKey").toString() : null;
            if (apiKeyInput != null && !apiKeyInput.isBlank() && !apiKeyInput.contains("****")) {
                entity.setApiKey(encryptionService.encrypt(apiKeyInput));
            } else if (existing == null) {
                entity.setApiKey(null);
            }

            if (existing != null) {
                userAiConfigMapper.updateById(entity);
            } else {
                userAiConfigMapper.insert(entity);
            }
        }
        log.info("saved_user_ai_config userId={} count={}", userId, configs.size());
    }

    @Override
    @Transactional
    public void saveProviderKey(Long userId, Long providerId, String apiKey) {
        UserAiProviderKey existing = userAiProviderKeyMapper.selectOne(
                new LambdaQueryWrapper<UserAiProviderKey>()
                        .eq(UserAiProviderKey::getUserId, userId)
                        .eq(UserAiProviderKey::getProviderId, providerId));

        UserAiProviderKey entity = existing != null ? existing : new UserAiProviderKey();
        entity.setUserId(userId);
        entity.setProviderId(providerId);

        if (apiKey != null && !apiKey.isBlank() && !apiKey.contains("****")) {
            entity.setApiKey(encryptionService.encrypt(apiKey));
        }

        if (existing != null) {
            userAiProviderKeyMapper.updateById(entity);
        } else {
            userAiProviderKeyMapper.insert(entity);
        }
        log.info("saved_provider_key userId={} providerId={}", userId, providerId);
    }

    @Override
    public String resolveApiKey(Long userId, Long providerId, String scenarioCode) {
        // 优先使用场景级Key
        UserAiConfig config = userAiConfigMapper.selectOne(
                new LambdaQueryWrapper<UserAiConfig>()
                        .eq(UserAiConfig::getUserId, userId)
                        .eq(UserAiConfig::getScenarioCode, scenarioCode));
        if (config != null && config.getApiKey() != null && !config.getApiKey().isBlank()) {
            return encryptionService.decrypt(config.getApiKey());
        }

        // Fallback到服务商全局Key
        UserAiProviderKey providerKey = userAiProviderKeyMapper.selectOne(
                new LambdaQueryWrapper<UserAiProviderKey>()
                        .eq(UserAiProviderKey::getUserId, userId)
                        .eq(UserAiProviderKey::getProviderId, providerId));
        if (providerKey != null && providerKey.getApiKey() != null && !providerKey.getApiKey().isBlank()) {
            return encryptionService.decrypt(providerKey.getApiKey());
        }

        return null;
    }

    /**
     * 对API Key进行脱敏处理.
     * <p>格式：前3位 + **** + 后2位，如 sk-1****MN</p>
     */
    private String maskApiKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isBlank()) {
            return null;
        }
        try {
            String plain = encryptionService.decrypt(encryptedKey);
            if (plain.length() <= 7) {
                return plain.charAt(0) + "****" + plain.charAt(plain.length() - 1);
            }
            return plain.substring(0, 3) + "****" + plain.substring(plain.length() - 2);
        } catch (Exception e) {
            return "****";
        }
    }
}
