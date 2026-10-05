package com.secondbrain.service.impl;

import com.secondbrain.entity.AiProvider;
import com.secondbrain.entity.UserAiConfig;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.AiProviderMapper;
import com.secondbrain.mapper.UserAiConfigMapper;
import com.secondbrain.mapper.UserAiProviderKeyMapper;
import com.secondbrain.service.ApiKeyEncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 确保保存成功并不掩盖服务商、模型和密钥不匹配的问题。 */
class AiServiceVisionConfigTest {
    private final AiProviderMapper providerMapper = mock(AiProviderMapper.class);
    private final UserAiConfigMapper configMapper = mock(UserAiConfigMapper.class);
    private final UserAiProviderKeyMapper providerKeyMapper = mock(UserAiProviderKeyMapper.class);
    private final ApiKeyEncryptionService encryptionService = mock(ApiKeyEncryptionService.class);
    private final AiServiceImpl service = new AiServiceImpl(providerMapper, configMapper, providerKeyMapper,
            encryptionService, mock(RestTemplate.class), mock(RestTemplate.class));

    @Test
    void rejectsQwenModelOnAnotherProvider() {
        givenConfiguration("zhipu", "qwen3-vl-flash");

        assertThatThrownBy(() -> service.resolveVisionConfig(7L, "vision"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("通义千问");
    }

    @Test
    void acceptsMatchingVisionProviderAndModel() {
        givenConfiguration("qwen", "qwen3-vl-flash");

        assertThat(service.resolveVisionConfig(7L, "vision").getModelName()).isEqualTo("qwen3-vl-flash");
    }

    @Test
    void promptsToReplaceUnreadableSavedKey() {
        givenConfiguration("qwen", "qwen3-vl-flash");
        when(encryptionService.decrypt("encrypted-key")).thenThrow(new IllegalStateException("decrypt failed"));

        assertThatThrownBy(() -> service.resolveVisionConfig(7L, "vision"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重新输入");
    }

    private void givenConfiguration(String providerCode, String modelName) {
        UserAiConfig saved = new UserAiConfig();
        saved.setProviderId(3L);
        saved.setModelName(modelName);
        saved.setApiKey("encrypted-key");
        AiProvider provider = new AiProvider();
        provider.setId(3L);
        provider.setCode(providerCode);
        provider.setName(providerCode);
        provider.setApiType("openai_compatible");
        provider.setBaseUrl("https://example.invalid/api/v1");
        provider.setIsEnabled(1);
        when(configMapper.selectOne(any())).thenReturn(saved);
        when(providerMapper.selectById(3L)).thenReturn(provider);
        when(encryptionService.decrypt("encrypted-key")).thenReturn("personal-key");
    }
}
