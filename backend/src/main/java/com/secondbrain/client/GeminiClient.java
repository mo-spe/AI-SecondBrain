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
 * Google Gemini API 客户端.
 * <p>封装对 Google Gemini 模型的调用，API Key 作为 query parameter 传递</p>
 */
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";

    private final RestTemplate restTemplate;

    public GeminiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 发送消息到 Gemini API.
     *
     * @param baseUrl      API地址，为空时使用默认地址
     * @param apiKey       API Key
     * @param model        模型名称
     * @param systemPrompt 系统提示词
     * @param messages     消息列表，每个元素包含 role 和 content
     * @return AI回复文本
     */
    public String chat(String baseUrl, String apiKey, String model,
                       String systemPrompt, List<Map<String, String>> messages) {
        String url = (baseUrl != null && !baseUrl.isBlank() ? baseUrl : DEFAULT_BASE_URL)
                + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, Object> sysInstruction = new HashMap<>();
            Map<String, Object> sysPart = new HashMap<>();
            sysPart.put("text", systemPrompt);
            sysInstruction.put("parts", List.of(sysPart));
            body.put("systemInstruction", sysInstruction);
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            Map<String, Object> content = new HashMap<>();
            // Gemini uses "model" instead of "assistant"
            String role = "assistant".equals(msg.get("role")) ? "model" : msg.get("role");
            content.put("role", role);
            Map<String, Object> part = new HashMap<>();
            part.put("text", msg.get("content"));
            content.put("parts", List.of(part));
            contents.add(content);
        }
        body.put("contents", contents);

        log.info("gemini_request model={} messages={}", model, messages.size());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        JSONObject respJson = JSON.parseObject(response.getBody());

        String text = respJson.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");
        log.info("gemini_response length={}", text != null ? text.length() : 0);
        return text;
    }
}
