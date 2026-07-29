package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

    @Override
    public String chatStream(Long userId, String scenarioCode,
                             List<Map<String, String>> messages, Consumer<String> onChunk) {
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

        return executeCallWithMessagesStream(config, systemPrompt, filteredMessages, onChunk);
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
                .okHttpClient(new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(120, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build())
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

    /**
     * 流式执行 AI 调用（分片回调）.
     */
    private String executeCallWithMessagesStream(AiCallConfig config, String systemPrompt,
                                                  List<Map<String, String>> messages,
                                                  Consumer<String> onChunk) {
        log.info("ai_stream_call provider={} apiType={} model={} messages={}",
                config.getProviderCode(), config.getApiType(), config.getModelName(), messages.size());

        return switch (config.getApiType()) {
            case "anthropic", "gemini" ->
                // 不支持流式的 provider 回退到同步调用，手动分片回调
                    callNonStream(config, systemPrompt, messages, onChunk);
            default -> callOpenAiCompatibleStream(config, systemPrompt, messages, onChunk);
        };
    }

    /**
     * 非流式 provider 回退：一次调用，逐字符回调模拟流式.
     */
    private String callNonStream(AiCallConfig config, String systemPrompt,
                                  List<Map<String, String>> messages, Consumer<String> onChunk) {
        String result = executeCallWithMessages(config, systemPrompt, messages);
        if (result != null) {
            for (int i = 0; i < result.length(); i += 10) {
                int end = Math.min(i + 10, result.length());
                onChunk.accept(result.substring(i, end));
            }
        }
        return result;
    }

    /**
     * OpenAI 兼容 API 流式调用（原生 SSE 解析）.
     *
     * <p>chatgpt-java 1.1.5 无 chatCompletionStream 方法，
     * 直接通过 OkHttp 发 stream=true 的 POST 请求，
     * 逐行解析 SSE data: 块获取增量内容。</p>
     */
    private String callOpenAiCompatibleStream(AiCallConfig config, String systemPrompt,
                                               List<Map<String, String>> messages,
                                               Consumer<String> onChunk) {
        String baseUrl = config.getBaseUrl();
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        OkHttpClient streamClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)   // 流式：无读取超时
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // 构建 JSON 请求体
        List<Map<String, Object>> apiMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            apiMessages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (Map<String, String> msg : messages) {
            apiMessages.add(Map.of("role", msg.get("role"), "content", msg.get("content")));
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", config.getModelName());
        requestBody.put("messages", apiMessages);
        requestBody.put("stream", true);

        String json;
        try {
            json = OBJECT_MAPPER.writeValueAsString(requestBody);
        } catch (Exception e) {
            log.error("stream_json_build_failed", e);
            return callNonStream(config, systemPrompt, messages, onChunk);
        }

        Request request = new Request.Builder()
                .url(baseUrl + "chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .post(RequestBody.create(json, MediaType.get("application/json")))
                .build();

        StringBuilder fullContent = new StringBuilder();
        try (Response response = streamClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("stream_http_error status={}", response.code());
                return callNonStream(config, systemPrompt, messages, onChunk);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                    String data = line.substring(6);
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> chunk = OBJECT_MAPPER.readValue(data, Map.class);
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> choices =
                                (List<Map<String, Object>>) chunk.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> delta =
                                    (Map<String, Object>) choices.get(0).get("delta");
                            if (delta != null) {
                                String content = (String) delta.get("content");
                                if (content != null) {
                                    fullContent.append(content);
                                    onChunk.accept(content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 跳过无法解析的行
                    }
                }
            }
        } catch (IOException e) {
            log.error("stream_io_error", e);
            if (fullContent.length() > 0) {
                return fullContent.toString();
            }
            return callNonStream(config, systemPrompt, messages, onChunk);
        }

        return fullContent.toString();
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
