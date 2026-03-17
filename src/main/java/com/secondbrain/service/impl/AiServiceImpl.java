package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private static final int MAX_RETRY_TIMES = 3;
    private static final int TIMEOUT_SECONDS = 30;

    @Value("${ai.provider:qwen}")
    private String provider;

    @Value("${ai.qwen.api-key}")
    private String qwenApiKey;

    @Value("${ai.qwen.base-url}")
    private String qwenBaseUrl;

    @Value("${ai.qwen.model}")
    private String qwenModel;

    @Value("${ai.deepseek.api-key}")
    private String deepseekApiKey;

    @Value("${ai.deepseek.base-url}")
    private String deepseekBaseUrl;

    @Value("${ai.deepseek.model}")
    private String deepseekModel;

    @Value("${ai.openai.api-key}")
    private String openaiApiKey;

    @Value("${ai.openai.base-url}")
    private String openaiBaseUrl;

    @Value("${ai.openai.model}")
    private String openaiModel;

    private final RestTemplate restTemplate;

    public AiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<KnowledgeDTO> extractKnowledge(String content) {
        String prompt = buildPrompt(content);
        String response = callAiApiWithRetry(prompt);
        return parseResponse(response);
    }

    @Override
    public String generateQuestion(String prompt) {
        return callAiApiWithRetry(prompt);
    }

    @Override
    public String generateAnswer(String prompt) {
        return callAiApiWithRetry(prompt);
    }

    @Override
    public List<KnowledgeDTO> extractKnowledge(String content, String userApiKey) {
        String prompt = buildPrompt(content);
        String response = callAiApiWithRetry(prompt, userApiKey);
        return parseResponse(response);
    }

    @Override
    public String generateQuestion(String prompt, String userApiKey) {
        return callAiApiWithRetry(prompt, userApiKey);
    }

    @Override
    public String generateAnswer(String prompt, String userApiKey) {
        return callAiApiWithRetry(prompt, userApiKey);
    }

    private String callAiApiWithRetry(String prompt) {
        return callAiApiWithRetry(prompt, null);
    }

    private String callAiApiWithRetry(String prompt, String userApiKey) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRY_TIMES) {
            try {
                String response = callAiApi(prompt, userApiKey);
                if (response != null && !response.isEmpty()) {
                    log.info("AI API调用成功，重试次数：{}", retryCount);
                    return response;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("AI API调用失败，第{}次重试，错误：{}", retryCount + 1, e.getMessage());
            }

            retryCount++;
            if (retryCount < MAX_RETRY_TIMES) {
                try {
                    Thread.sleep(1000 * retryCount);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("AI API调用失败，已重试{}次，请检查API Key配置", MAX_RETRY_TIMES, lastException);
        throw new RuntimeException("AI服务调用失败，请配置有效的API Key。请前往个人设置添加API Key，或联系管理员配置平台API Key。");
    }

    private String fallbackResponse() {
        log.warn("使用降级策略，返回空响应");
        return "{\"knowledge\": []}";
    }

    private String buildPrompt(String content) {
        return """
                请从以下对话内容中提取关键知识点，以JSON格式返回。
                要求：
                1. 提取3-5个最重要的知识点
                2. 每个知识点包含：
                   - title（标题）：简洁明确的知识点名称
                   - summary（摘要）：高度概括知识点的核心要点，每个要点前加序号（1.、2.、3.等），控制在200-300字之间，只列出要点，不展开详细说明
                   - content（内容）：详细、完整地阐述该知识点，包括：
                     * 核心概念的定义和详细说明
                     * 重要原理或机制的深入解释
                     * 具体应用场景或实例分析
                     * 相关注意事项或限制条件
                     * 与其他知识点的关联和对比
                     * 实际使用中的最佳实践
                     内容要条理清晰，分段明确，便于学习和记忆，至少400字，建议500-800字
                   - keywords（关键词列表）：3-5个相关关键词
                3. 内容要求：
                   - summary和content必须是完全不同的内容，summary是高度概括的要点，content是详细展开的说明
                   - summary不能简单复制content的开头部分
                   - content必须比summary详细得多，包含更多具体信息
                   - 详细但不冗余，确保能学到完整的知识
                   - 结构清晰，有层次感，使用段落和标点符号组织内容
                4. 返回格式：{"knowledge": [{"title": "...", "summary": "...", "content": "...", "keywords": ["...", "..."]}]}
                
                对话内容：
                %s
                """.formatted(content);
    }

    private String callAiApi(String prompt) {
        return callAiApi(prompt, null);
    }

    private String callAiApi(String prompt, String userApiKey) {
        try {
            String url, apiKey, model;

            if ("qwen".equalsIgnoreCase(provider)) {
                url = qwenBaseUrl + "/chat/completions";
                apiKey = userApiKey != null && !userApiKey.isEmpty() ? userApiKey : qwenApiKey;
                model = qwenModel;
            } else if ("deepseek".equalsIgnoreCase(provider)) {
                url = deepseekBaseUrl + "/chat/completions";
                apiKey = userApiKey != null && !userApiKey.isEmpty() ? userApiKey : deepseekApiKey;
                model = deepseekModel;
            } else {
                url = openaiBaseUrl + "/chat/completions";
                apiKey = userApiKey != null && !userApiKey.isEmpty() ? userApiKey : openaiApiKey;
                model = openaiModel;
            }

            if (apiKey == null || apiKey.isEmpty()) {
                log.error("API Key为空，无法调用AI服务");
                return null;
            }

            String apiKeySource = userApiKey != null && !userApiKey.isEmpty() ? "用户API Key" : "平台API Key";
            String maskedApiKey = apiKey.substring(0, Math.min(8, apiKey.length())) + "..." + apiKey.substring(Math.max(0, apiKey.length() - 4));
            log.info("使用{}调用AI服务，API Key：{}，模型：{}", apiKeySource, maskedApiKey, model);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.put("messages", messages);

            requestBody.put("temperature", 0.8);
            requestBody.put("max_tokens", 15000);
            requestBody.put("top_p", 0.9);
            requestBody.put("presence_penalty", 0.1);
            requestBody.put("frequency_penalty", 0.1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("AI API响应状态码：{}，响应体长度：{}，使用的API Key来源：{}", response.getStatusCode(), response.getBody().length(), apiKeySource);
                log.info("AI API响应体前100字符：{}", response.getBody().substring(0, Math.min(100, response.getBody().length())));
                
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject messageObj = choice.getJSONObject("message");
                    String content = messageObj.getString("content");
                    log.info("AI API响应内容长度：{}，使用的API Key来源：{}", content.length(), apiKeySource);
                    return content;
                }
            }

            log.error("AI API调用失败：{}，使用的API Key来源：{}", response.getStatusCode(), apiKeySource);
            return null;

        } catch (Exception e) {
            log.error("AI API调用异常", e);
            return null;
        }
    }

    private List<KnowledgeDTO> parseResponse(String response) {
        List<KnowledgeDTO> knowledgeList = new ArrayList<>();

        if (response == null || response.isEmpty()) {
            log.warn("AI响应为空");
            return knowledgeList;
        }

        try {
            log.info("开始解析AI响应，响应长度：{}", response.length());
            
            String cleanedResponse = response.trim();
            
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            } else if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            
            cleanedResponse = cleanedResponse.trim();
            
            log.info("清理后的响应长度：{}", cleanedResponse.length());
            
            try {
                JSONObject jsonResponse = JSON.parseObject(cleanedResponse);
                JSONArray knowledgeArray = jsonResponse.getJSONArray("knowledge");

                if (knowledgeArray != null) {
                    log.info("成功解析JSON，找到{}个知识点", knowledgeArray.size());
                    
                    for (int i = 0; i < knowledgeArray.size(); i++) {
                        try {
                            JSONObject knowledgeObj = knowledgeArray.getJSONObject(i);
                            KnowledgeDTO knowledge = new KnowledgeDTO();
                            knowledge.setTitle(knowledgeObj.getString("title"));
                            knowledge.setSummary(knowledgeObj.getString("summary"));
                            knowledge.setContent(knowledgeObj.getString("content"));
                            
                            JSONArray keywordsArray = knowledgeObj.getJSONArray("keywords");
                            if (keywordsArray != null) {
                                knowledge.setKeywords(keywordsArray.toJavaList(String.class));
                            } else {
                                knowledge.setKeywords(new ArrayList<>());
                            }
                            
                            knowledgeList.add(knowledge);
                            log.info("成功解析第{}个知识点，标题：{}", i + 1, knowledge.getTitle());
                        } catch (Exception e) {
                            log.error("解析第{}个知识点失败", i + 1, e);
                        }
                    }
                } else {
                    log.warn("JSON响应中未找到knowledge数组");
                }
            } catch (Exception e) {
                log.warn("标准JSON解析失败，尝试使用宽松解析模式，错误：{}", e.getMessage());
                
                cleanedResponse = cleanedResponse.replace("\u201c", "\"").replace("\u201d", "\"");
                log.info("替换中文引号后的响应长度：{}", cleanedResponse.length());
                
                knowledgeList = parseResponseWithLenientMode(cleanedResponse);
            }
        } catch (Exception e) {
            log.error("解析AI响应失败，响应内容：{}", response.substring(0, Math.min(500, response.length())), e);
            log.error("完整响应内容：{}", response);
        }

        log.info("最终解析得到{}个知识点", knowledgeList.size());
        return knowledgeList;
    }

    private List<KnowledgeDTO> parseResponseWithLenientMode(String response) {
        List<KnowledgeDTO> knowledgeList = new ArrayList<>();
        
        try {
            log.info("开始宽松模式解析，响应长度：{}", response.length());
            
            int knowledgeStart = response.indexOf("\"knowledge\"");
            if (knowledgeStart == -1) {
                log.warn("未找到knowledge字段");
                return knowledgeList;
            }
            
            int arrayStart = response.indexOf("[", knowledgeStart);
            if (arrayStart == -1) {
                log.warn("未找到knowledge数组开始");
                return knowledgeList;
            }
            
            int arrayEnd = findMatchingBracket(response, arrayStart, '[', ']');
            if (arrayEnd == -1) {
                log.warn("未找到knowledge数组结束");
                return knowledgeList;
            }
            
            String arrayContent = response.substring(arrayStart + 1, arrayEnd);
            log.info("提取的数组内容长度：{}", arrayContent.length());
            
            List<String> objects = splitJsonObjects(arrayContent);
            log.info("分割得到{}个JSON对象", objects.size());
            
            for (int i = 0; i < objects.size(); i++) {
                try {
                    String objStr = objects.get(i).trim();
                    if (objStr.isEmpty()) continue;
                    
                    String fullObjStr = "{" + objStr + "}";
                    JSONObject knowledgeObj = JSON.parseObject(fullObjStr);
                    
                    KnowledgeDTO knowledge = new KnowledgeDTO();
                    knowledge.setTitle(knowledgeObj.getString("title"));
                    knowledge.setSummary(knowledgeObj.getString("summary"));
                    knowledge.setContent(knowledgeObj.getString("content"));
                    
                    JSONArray keywordsArray = knowledgeObj.getJSONArray("keywords");
                    if (keywordsArray != null) {
                        knowledge.setKeywords(keywordsArray.toJavaList(String.class));
                    } else {
                        knowledge.setKeywords(new ArrayList<>());
                    }
                    
                    knowledgeList.add(knowledge);
                    log.info("宽松模式成功解析第{}个知识点，标题：{}", i + 1, knowledge.getTitle());
                } catch (Exception e) {
                    log.error("宽松模式解析第{}个知识点失败", i + 1, e);
                }
            }
        } catch (Exception e) {
            log.error("宽松模式解析失败", e);
        }
        
        return knowledgeList;
    }

    private int findMatchingBracket(String str, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        
        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (escape) {
                escape = false;
                continue;
            }
            
            if (c == '\\') {
                escape = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == open) {
                    depth++;
                } else if (c == close) {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        
        return -1;
    }

    private List<String> splitJsonObjects(String arrayContent) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        int start = 0;
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            
            if (escape) {
                escape = false;
                continue;
            }
            
            if (c == '\\') {
                escape = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) {
                        start = i;
                    }
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        objects.add(arrayContent.substring(start, i + 1));
                    }
                }
            }
        }
        
        return objects;
    }
}
