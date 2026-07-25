package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.secondbrain.client.AnthropicClient;
import com.secondbrain.client.GeminiClient;
import com.secondbrain.dto.AiCallConfig;
import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.entity.AiProvider;
import com.secondbrain.entity.UserAiConfig;
import com.secondbrain.entity.UserAiProviderKey;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.AiProviderMapper;
import com.secondbrain.mapper.UserAiConfigMapper;
import com.secondbrain.mapper.UserAiProviderKeyMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.ApiKeyEncryptionService;
import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse;
import com.unfbx.chatgpt.entity.chat.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI服务实现类.
 * <p>新方法通过 userId + scenarioCode 从数据库动态获取用户配置，
 * 旧方法（@Deprecated）继续使用 application.yml 系统配置以保证兼容</p>
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    // ========== 旧配置（仅用于 @Deprecated 方法，Phase 5 迁移完成后移除） ==========
    @Value("${ai.provider:qwen}")
    private String provider;

    @Value("${ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;

    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String openaiModel;

    @Value("${ai.qwen.api-key:}")
    private String qwenApiKey;

    @Value("${ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String qwenBaseUrl;

    @Value("${ai.qwen.model:qwen-plus}")
    private String qwenModel;

    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    // ========== 新依赖 ==========
    private final AiProviderMapper aiProviderMapper;
    private final UserAiConfigMapper userAiConfigMapper;
    private final UserAiProviderKeyMapper userAiProviderKeyMapper;
    private final ApiKeyEncryptionService encryptionService;
    private final RestTemplate restTemplate;
    private final RestTemplate streamingRestTemplate;

    public AiServiceImpl(AiProviderMapper aiProviderMapper,
                         UserAiConfigMapper userAiConfigMapper,
                         UserAiProviderKeyMapper userAiProviderKeyMapper,
                         ApiKeyEncryptionService encryptionService,
                         RestTemplate restTemplate,
                         @Qualifier("streamingRestTemplate") RestTemplate streamingRestTemplate) {
        this.aiProviderMapper = aiProviderMapper;
        this.userAiConfigMapper = userAiConfigMapper;
        this.userAiProviderKeyMapper = userAiProviderKeyMapper;
        this.encryptionService = encryptionService;
        this.restTemplate = restTemplate;
        this.streamingRestTemplate = streamingRestTemplate;
    }

    // ========== 新接口实现 ==========

    @Override
    public AiCallConfig resolveConfig(Long userId, String scenarioCode) {
        UserAiConfig config = userAiConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAiConfig>()
                        .eq(UserAiConfig::getUserId, userId)
                        .eq(UserAiConfig::getScenarioCode, scenarioCode));

        if (config == null || config.getProviderId() == null) {
            throw new BusinessException(400, "请先在设置页配置 " + scenarioCode + " 场景的AI服务商和模型");
        }

        AiProvider provider = aiProviderMapper.selectById(config.getProviderId());
        if (provider == null || provider.getIsEnabled() != 1) {
            throw new BusinessException(400, "AI服务商不可用");
        }

        AiCallConfig callConfig = new AiCallConfig();
        callConfig.setProviderId(provider.getId());
        callConfig.setProviderCode(provider.getCode());
        callConfig.setProviderName(provider.getName());
        callConfig.setBaseUrl(provider.getBaseUrl());
        callConfig.setApiType(provider.getApiType());
        callConfig.setModelName(config.getModelName());

        // 解析 API Key：场景级 → 服务商全局级
        String apiKey = null;
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            apiKey = encryptionService.decrypt(config.getApiKey());
        } else {
            UserAiProviderKey providerKey = userAiProviderKeyMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAiProviderKey>()
                            .eq(UserAiProviderKey::getUserId, userId)
                            .eq(UserAiProviderKey::getProviderId, provider.getId()));
            if (providerKey != null && providerKey.getApiKey() != null && !providerKey.getApiKey().isBlank()) {
                apiKey = encryptionService.decrypt(providerKey.getApiKey());
            }
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(400, "请先在设置页配置 " + provider.getName() + " 的API Key");
        }
        callConfig.setApiKey(apiKey);

        log.info("resolved_ai_config userId={} scenario={} provider={} model={}",
                userId, scenarioCode, provider.getCode(), config.getModelName());
        return callConfig;
    }

    @Override
    public String generateAnswer(Long userId, String scenarioCode, String prompt) {
        AiCallConfig config = resolveConfig(userId, scenarioCode);
        return executeCall(config, "你是一个专业的知识问答助手。", prompt);
    }

    @Override
    public String generateQuestion(Long userId, String scenarioCode, String prompt) {
        AiCallConfig config = resolveConfig(userId, scenarioCode);
        return executeCall(config, "你是一个专业的教育出题助手。", prompt);
    }

    @Override
    public List<KnowledgeDTO> extractKnowledge(Long userId, String scenarioCode, String content) {
        AiCallConfig config = resolveConfig(userId, scenarioCode);
        String result = executeCall(config,
                "你是一个知识提取助手。请从给定的内容中提取关键知识点，返回JSON数组格式。" +
                "每个元素包含title(标题)、summary(摘要)、content(详细内容)字段。" +
                "直接返回JSON，不要用markdown代码块包裹。",
                content);
        String json = extractJsonArray(result);
        return JSON.parseArray(json, KnowledgeDTO.class);
    }

    /**
     * 从 AI 返回的文本中提取 JSON 数组.
     * <p>处理 AI 可能将 JSON 包裹在 markdown 代码块或额外文字中的情况</p>
     */
    private String extractJsonArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return "[]";
        }
        String trimmed = raw.trim();
        // 去掉 markdown 代码块标记 ```json 和 ```
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("\n");
            if (start == -1) start = 3;
            else start = start + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                trimmed = trimmed.substring(start, end).trim();
            } else {
                trimmed = trimmed.substring(start).trim();
            }
        }
        // 如果前面还有非 JSON 文字，从第一个 '[' 开始截取
        int arrayStart = trimmed.indexOf('[');
        if (arrayStart > 0) {
            trimmed = trimmed.substring(arrayStart);
        }
        return trimmed;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(Long userId, String scenarioCode, List<Map<String, String>> messages) {
        AiCallConfig config = resolveConfig(userId, scenarioCode);

        String systemPrompt = null;
        List<Map<String, String>> filteredMessages = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            if ("system".equals(msg.get("role"))) {
                systemPrompt = msg.get("content");
            } else {
                filteredMessages.add(msg);
            }
        }

        return executeCallWithMessages(config, systemPrompt, filteredMessages);
    }

    /**
     * 执行AI调用（单轮：system prompt + user prompt）.
     */
    private String executeCall(AiCallConfig config, String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);
        return executeCallWithMessages(config, systemPrompt, messages);
    }

    /**
     * 执行AI调用（多轮消息）.
     * <p>根据 apiType 分发到不同的客户端实现</p>
     */
    private String executeCallWithMessages(AiCallConfig config, String systemPrompt,
                                           List<Map<String, String>> messages) {
        log.info("ai_call provider={} apiType={} model={} messages={}",
                config.getProviderCode(), config.getApiType(), config.getModelName(), messages.size());

        return switch (config.getApiType()) {
            case "anthropic" -> {
                AnthropicClient client = new AnthropicClient(restTemplate);
                yield client.chat(config.getBaseUrl(), config.getApiKey(),
                        config.getModelName(), systemPrompt, messages);
            }
            case "gemini" -> {
                GeminiClient client = new GeminiClient(restTemplate);
                yield client.chat(config.getBaseUrl(), config.getApiKey(),
                        config.getModelName(), systemPrompt, messages);
            }
            default -> callOpenAiCompatible(config, systemPrompt, messages);
        };
    }

    /**
     * 调用 OpenAI 兼容 API.
     * <p>覆盖 OpenAI / 千问 / DeepSeek / Kimi / 智谱 / 豆包 / MiniMax 等</p>
     */
    private String callOpenAiCompatible(AiCallConfig config, String systemPrompt,
                                        List<Map<String, String>> messages) {
        String baseUrl = config.getBaseUrl();
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        OpenAiClient client = OpenAiClient.builder()
                .apiKey(java.util.Collections.singletonList(config.getApiKey()))
                .apiHost(baseUrl)
                .build();

        List<Message> apiMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            apiMessages.add(Message.builder().role(Message.Role.SYSTEM).content(systemPrompt).build());
        }
        for (Map<String, String> msg : messages) {
            Message.Role role = "assistant".equals(msg.get("role"))
                    ? Message.Role.ASSISTANT : Message.Role.USER;
            apiMessages.add(Message.builder().role(role).content(msg.get("content")).build());
        }

        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(config.getModelName())
                .messages(apiMessages)
                .build();

        ChatCompletionResponse response = client.chatCompletion(chatCompletion);
        return response.getChoices().get(0).getMessage().getContent();
    }

    // ========== @Deprecated 方法（保留兼容，Phase 5 迁移完成后移除） ==========

    @Deprecated
    @Override
    public List<KnowledgeDTO> extractKnowledge(String content) {
        return extractKnowledge(content, null);
    }

    @Deprecated
    @Override
    public List<KnowledgeDTO> extractKnowledge(String content, String userApiKey) {
        try {
            String apiKey = getApiKey(userApiKey);
            log.info("开始提取知识，内容长度：{}", content.length());

            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder().role(Message.Role.SYSTEM).content(
                "你是一个知识提取助手。请从给定的内容中提取关键知识点，返回JSON数组格式。" +
                "每个元素包含title(标题)、summary(摘要)、content(详细内容)字段。"
            ).build());
            messages.add(Message.builder().role(Message.Role.USER).content(content).build());

            OpenAiClient client = createClient(apiKey);
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model(getActiveModel())
                    .messages(messages)
                    .build();

            ChatCompletionResponse response = client.chatCompletion(chatCompletion);
            String result = response.getChoices().get(0).getMessage().getContent();

            log.info("知识提取成功，结果长度：{}", result.length());
            return JSON.parseArray(result, KnowledgeDTO.class);
        } catch (Exception e) {
            log.error("知识提取失败", e);
            throw new IllegalStateException("知识提取失败：" + e.getMessage());
        }
    }

    @Deprecated
    @Override
    public String generateQuestion(String prompt) {
        return generateQuestion(prompt, null);
    }

    @Deprecated
    @Override
    public String generateQuestion(String prompt, String userApiKey) {
        try {
            String apiKey = getApiKey(userApiKey);
            log.info("开始生成问题，prompt 长度：{}", prompt.length());

            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder().role(Message.Role.SYSTEM).content("你是一个专业的教育出题助手。").build());
            messages.add(Message.builder().role(Message.Role.USER).content(prompt).build());

            OpenAiClient client = createClient(apiKey);
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model(getActiveModel())
                    .messages(messages)
                    .build();

            ChatCompletionResponse response = client.chatCompletion(chatCompletion);
            String result = response.getChoices().get(0).getMessage().getContent();

            log.info("问题生成成功，结果长度：{}", result.length());
            return result;
        } catch (Exception e) {
            log.error("问题生成失败", e);
            throw new IllegalStateException("问题生成失败：" + e.getMessage());
        }
    }

    @Deprecated
    @Override
    public String generateAnswer(String prompt) {
        return generateAnswer(prompt, null);
    }

    @Deprecated
    @Override
    public String generateAnswer(String prompt, String userApiKey) {
        try {
            String apiKey = getApiKey(userApiKey);
            log.info("开始生成答案，prompt 长度：{}", prompt.length());

            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder().role(Message.Role.SYSTEM).content("你是一个专业的知识问答助手。").build());
            messages.add(Message.builder().role(Message.Role.USER).content(prompt).build());

            OpenAiClient client = createClient(apiKey);
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model(getActiveModel())
                    .messages(messages)
                    .build();

            ChatCompletionResponse response = client.chatCompletion(chatCompletion);
            String result = response.getChoices().get(0).getMessage().getContent();

            log.info("答案生成成功，结果长度：{}", result.length());
            return result;
        } catch (Exception e) {
            log.error("答案生成失败", e);
            throw new IllegalStateException("答案生成失败：" + e.getMessage(), e);
        }
    }

    @Deprecated
    @Override
    public String chat(List<Message> messages, String userApiKey) {
        try {
            String apiKey = getApiKey(userApiKey);
            log.info("开始聊天，消息数量：{}", messages.size());

            OpenAiClient client = createClient(apiKey);
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model(getActiveModel())
                    .messages(messages)
                    .build();

            ChatCompletionResponse response = client.chatCompletion(chatCompletion);
            String result = response.getChoices().get(0).getMessage().getContent();

            log.info("聊天完成，结果长度：{}", result.length());
            return result;
        } catch (Exception e) {
            log.error("聊天失败", e);
            throw new IllegalStateException("聊天失败：" + e.getMessage());
        }
    }

    // ========== 旧辅助方法（仅用于 @Deprecated 方法） ==========

    private String getSystemApiKey() {
        return switch (provider) {
            case "openai" -> openaiApiKey;
            case "deepseek" -> deepseekApiKey;
            default -> qwenApiKey;
        };
    }

    private String getActiveBaseUrl() {
        return switch (provider) {
            case "openai" -> openaiBaseUrl;
            case "deepseek" -> deepseekBaseUrl;
            default -> qwenBaseUrl;
        };
    }

    private String getActiveModel() {
        return switch (provider) {
            case "openai" -> openaiModel;
            case "deepseek" -> deepseekModel;
            default -> qwenModel;
        };
    }

    private OpenAiClient createClient(String apiKey) {
        String activeBaseUrl = getActiveBaseUrl();
        log.info("AI服务商：{}，Base URL：{}", provider, activeBaseUrl);
        return OpenAiClient.builder()
                .apiKey(java.util.Collections.singletonList(apiKey))
                .apiHost(activeBaseUrl)
                .build();
    }

    private String getApiKey(String userApiKey) {
        if (userApiKey != null && !userApiKey.isEmpty()) {
            log.info("使用用户自定义 API Key");
            return userApiKey;
        }

        String systemKey = getSystemApiKey();
        if (systemKey != null && !systemKey.isEmpty()) {
            log.info("使用系统 {} API Key", provider);
            return systemKey;
        }

        throw new IllegalStateException("API Key 未配置，请设置 " + provider + " 的 API Key");
    }
}
