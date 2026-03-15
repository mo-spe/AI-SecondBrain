package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.secondbrain.service.EmbeddingService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);

    @Value("${ai.qwen.embedding.api-key:}")
    private String apiKey;

    @Value("${ai.qwen.embedding.url:https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding}")
    private String apiUrl;

    @Value("${ai.qwen.embedding.model:text-embedding-v2}")
    private String defaultModel;

    private final OkHttpClient client;

    public EmbeddingServiceImpl() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        return generateEmbedding(text, defaultModel);
    }

    @Override
    public List<Float> generateEmbedding(String text, String model) {
        return generateEmbedding(text, model, null);
    }

    @Override
    public List<Float> generateEmbedding(String text, String model, String userApiKey) {
        String effectiveApiKey = userApiKey;
        
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            effectiveApiKey = apiKey;
        }
        
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            log.warn("阿里云API Key未配置，无法生成Embedding");
            return new ArrayList<>();
        }

        log.info("生成Embedding，使用API Key来源：{}", userApiKey != null && !userApiKey.isEmpty() ? "用户API Key" : "平台API Key");

        try {
            String processedText = preprocessText(text);
            log.debug("文本预处理: 原文='{}', 处理后='{}'", text, processedText);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            
            JSONObject input = new JSONObject();
            JSONArray texts = new JSONArray();
            texts.add(processedText);
            input.put("texts", texts);
            requestBody.put("input", input);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + effectiveApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toJSONString(), MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("阿里云API调用失败: {}", response.code());
                    if (response.body() != null) {
                        log.error("错误详情: {}", response.body().string());
                    }
                    return new ArrayList<>();
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);

                JSONObject output = jsonResponse.getJSONObject("output");
                if (output != null) {
                    JSONArray embeddings = output.getJSONArray("embeddings");
                    if (embeddings != null && !embeddings.isEmpty()) {
                        JSONObject embeddingObj = embeddings.getJSONObject(0);
                        JSONArray embeddingArray = embeddingObj.getJSONArray("embedding");
                        
                        List<Float> embedding = new ArrayList<>();
                        for (int i = 0; i < embeddingArray.size(); i++) {
                            embedding.add(embeddingArray.getFloatValue(i));
                        }
                        
                        log.info("Embedding生成成功，维度: {}, 模型: {}", embedding.size(), model);
                        return embedding;
                    }
                }
            }
        } catch (IOException e) {
            log.error("生成Embedding失败", e);
        }

        return new ArrayList<>();
    }

    private String preprocessText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String processed = text.trim();
        
        processed = processed.replaceAll("\\s+", " ");
        
        processed = processed.replaceAll("([\\u4e00-\\u9fa5])\\s+([\\u4e00-\\u9fa5])", "$1$2");
        
        processed = processed.replaceAll("\\s+([\\u4e00-\\u9fa5])", "$1");
        processed = processed.replaceAll("([\\u4e00-\\u9fa5])\\s+", "$1");
        
        return processed.trim();
    }
}
