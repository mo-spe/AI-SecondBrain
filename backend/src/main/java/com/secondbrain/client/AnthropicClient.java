package com.secondbrain.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API 客户端.
 * <p>封装对 Anthropic Claude 模型的调用，使用 RestTemplate</p>
 */
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestTemplate restTemplate;

    public AnthropicClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 发送消息到 Anthropic API.
     *
     * @param baseUrl     API地址，为空时使用默认地址
     * @param apiKey      API Key
     * @param model       模型名称
     * @param systemPrompt 系统提示词
     * @param messages    消息列表，每个元素包含 role 和 content
     * @return AI回复文本
     */
    public String chat(String baseUrl, String apiKey, String model,
                       String systemPrompt, List<Map<String, String>> messages) {
        String url = (baseUrl != null && !baseUrl.isBlank() ? baseUrl : DEFAULT_BASE_URL)
                + "/v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }

        List<Map<String, Object>> apiMessages = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            Map<String, Object> apiMsg = new HashMap<>();
            apiMsg.put("role", msg.get("role"));
            apiMsg.put("content", msg.get("content"));
            apiMessages.add(apiMsg);
        }
        body.put("messages", apiMessages);

        log.info("anthropic_request model={} messages={}", model, apiMessages.size());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        JSONObject respJson = JSON.parseObject(response.getBody());

        String text = respJson.getJSONArray("content")
                .getJSONObject(0)
                .getString("text");
        log.info("anthropic_response length={}", text != null ? text.length() : 0);
        return text;
    }
}
