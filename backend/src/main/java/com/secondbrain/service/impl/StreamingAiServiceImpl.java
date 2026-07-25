package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.secondbrain.dto.AiCallConfig;
import com.secondbrain.service.AiService;
import com.secondbrain.service.StreamingAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 流式AI调用服务实现.
 *
 * <p>通过 RestTemplate.execute() 直接消费 SSE 响应流，
 * 支持 OpenAI 兼容、Anthropic、Gemini 三种 API 路径</p>
 */
@Service
public class StreamingAiServiceImpl implements StreamingAiService {

    private static final Logger log = LoggerFactory.getLogger(StreamingAiServiceImpl.class);

    private static final String ANTHROPIC_DEFAULT_URL = "https://api.anthropic.com";
    private static final String GEMINI_DEFAULT_URL = "https://generativelanguage.googleapis.com";

    private final AiService aiService;
    private final RestTemplate streamingRestTemplate;

    public StreamingAiServiceImpl(AiService aiService,
                                  @Qualifier("streamingRestTemplate") RestTemplate streamingRestTemplate) {
        this.aiService = aiService;
        this.streamingRestTemplate = streamingRestTemplate;
    }

    @Override
    public void streamChat(Long userId, String scenarioCode, String systemPrompt,
                           List<Map<String, String>> messages,
                           Consumer<String> onToken) throws Exception {
        AiCallConfig config = aiService.resolveConfig(userId, scenarioCode);

        log.info("streaming_chat provider={} apiType={} model={}",
                config.getProviderCode(), config.getApiType(), config.getModelName());

        switch (config.getApiType()) {
            case "anthropic" -> streamAnthropic(config, systemPrompt, messages, onToken);
            case "gemini" -> streamGemini(config, systemPrompt, messages, onToken);
            default -> streamOpenAiCompatible(config, systemPrompt, messages, onToken);
        }
    }

    // ==================== OpenAI 兼容路径（覆盖7家供应商） ====================

    private void streamOpenAiCompatible(AiCallConfig config, String systemPrompt,
                                        List<Map<String, String>> messages,
                                        Consumer<String> onToken) throws Exception {
        String baseUrl = config.getBaseUrl();
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        String url = baseUrl + "v1/chat/completions";

        List<Map<String, Object>> apiMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, Object> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            apiMessages.add(sysMsg);
        }
        for (Map<String, String> msg : messages) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", msg.get("role"));
            m.put("content", msg.get("content"));
            apiMessages.add(m);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", apiMessages);
        body.put("stream", true);
        body.put("temperature", 0.7);

        String bodyJson = JSON.toJSONString(body);

        streamingRestTemplate.execute(URI.create(url), HttpMethod.POST, request -> {
            ClientHttpRequest req = (ClientHttpRequest) request;
            req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            req.getHeaders().setBearerAuth(config.getApiKey());
            try (OutputStream os = req.getBody()) {
                os.write(bodyJson.getBytes(StandardCharsets.UTF_8));
            }
        }, response -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || !line.startsWith("data: ")) {
                        continue;
                    }
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        JSONObject json = JSON.parseObject(data);
                        var choices = json.getJSONArray("choices");
                        if (choices != null && !choices.isEmpty()) {
                            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                            if (delta != null) {
                                String content = delta.getString("content");
                                if (content != null && !content.isEmpty()) {
                                    onToken.accept(content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("parse_openai_sse_line_failed line={}", line, e);
                    }
                }
            }
            return null;
        });
    }

    // ==================== Anthropic 路径 ====================

    private void streamAnthropic(AiCallConfig config, String systemPrompt,
                                 List<Map<String, String>> messages,
                                 Consumer<String> onToken) throws Exception {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = ANTHROPIC_DEFAULT_URL;
        }
        String url = baseUrl + "/v1/messages";

        Map<String, Object> body = buildAnthropicBody(config.getModelName(), systemPrompt, messages);
        body.put("stream", true);

        String bodyJson = JSON.toJSONString(body);

        streamingRestTemplate.execute(URI.create(url), HttpMethod.POST, request -> {
            ClientHttpRequest req = (ClientHttpRequest) request;
            req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            req.getHeaders().set("x-api-key", config.getApiKey());
            req.getHeaders().set("anthropic-version", "2023-06-01");
            try (OutputStream os = req.getBody()) {
                os.write(bodyJson.getBytes(StandardCharsets.UTF_8));
            }
        }, response -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || !line.startsWith("data: ")) {
                        continue;
                    }
                    String jsonStr = line.substring(6).trim();
                    try {
                        JSONObject json = JSON.parseObject(jsonStr);
                        String type = json.getString("type");
                        if ("content_block_delta".equals(type)) {
                            JSONObject delta = json.getJSONObject("delta");
                            if (delta != null && "text_delta".equals(delta.getString("type"))) {
                                String text = delta.getString("text");
                                if (text != null) {
                                    onToken.accept(text);
                                }
                            }
                        } else if ("message_stop".equals(type)) {
                            break;
                        }
                    } catch (Exception e) {
                        log.debug("parse_anthropic_sse_line_failed line={}", line, e);
                    }
                }
            }
            return null;
        });
    }

    private Map<String, Object> buildAnthropicBody(String model, String systemPrompt,
                                                    List<Map<String, String>> messages) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }

        List<Map<String, Object>> apiMessages = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", msg.get("role"));
            m.put("content", msg.get("content"));
            apiMessages.add(m);
        }
        body.put("messages", apiMessages);
        return body;
    }

    // ==================== Gemini 路径 ====================

    private void streamGemini(AiCallConfig config, String systemPrompt,
                              List<Map<String, String>> messages,
                              Consumer<String> onToken) throws Exception {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = GEMINI_DEFAULT_URL;
        }
        String url = baseUrl + "/v1beta/models/" + config.getModelName()
                + ":streamGenerateContent?alt=sse&key=" + config.getApiKey();

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
            String role = "assistant".equals(msg.get("role")) ? "model" : msg.get("role");
            content.put("role", role);
            Map<String, Object> part = new HashMap<>();
            part.put("text", msg.get("content"));
            content.put("parts", List.of(part));
            contents.add(content);
        }
        body.put("contents", contents);

        String bodyJson = JSON.toJSONString(body);

        streamingRestTemplate.execute(URI.create(url), HttpMethod.POST, request -> {
            ClientHttpRequest req = (ClientHttpRequest) request;
            req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try (OutputStream os = req.getBody()) {
                os.write(bodyJson.getBytes(StandardCharsets.UTF_8));
            }
        }, response -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || !line.startsWith("data: ")) {
                        continue;
                    }
                    String jsonStr = line.substring(6).trim();
                    try {
                        JSONObject json = JSON.parseObject(jsonStr);
                        var candidates = json.getJSONArray("candidates");
                        if (candidates != null && !candidates.isEmpty()) {
                            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                            if (content != null) {
                                var parts = content.getJSONArray("parts");
                                if (parts != null && !parts.isEmpty()) {
                                    String text = parts.getJSONObject(0).getString("text");
                                    if (text != null && !text.isEmpty()) {
                                        onToken.accept(text);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("parse_gemini_sse_line_failed line={}", line, e);
                    }
                }
            }
            return null;
        });
    }
}
