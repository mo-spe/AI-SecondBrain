package com.secondbrain.service;

import com.secondbrain.dto.DeerFlowResearchRequest;
import com.secondbrain.dto.DeerFlowResearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class DeerFlowResearchService {

    private static final Logger log = LoggerFactory.getLogger(DeerFlowResearchService.class);

    @Value("${deerflow.api.url:http://localhost:8000}")
    private String deerFlowApiUrl;

    @Value("${deerflow.api.timeout:300}")
    private int apiTimeout;

    private final RestTemplate restTemplate;

    @Autowired
    public DeerFlowResearchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateDeepLearningReport(String learningData, String topic, String depth) {
        try {
            log.info("调用DeerFlow研究服务生成深度学习报告，主题：{}，深度：{}", topic, depth);

            Map<String, Object> request = new HashMap<>();
            request.put("learning_data", learningData);
            request.put("topic", topic);
            request.put("depth", depth != null ? depth : "deep");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<DeerFlowResearchResponse> response = restTemplate.postForEntity(
                deerFlowApiUrl + "/api/research/learning-report",
                entity,
                DeerFlowResearchResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                DeerFlowResearchResponse body = response.getBody();
                if (body != null && Boolean.TRUE.equals(body.getSuccess())) {
                    log.info("DeerFlow研究服务返回成功，报告长度：{}", body.getData().length());
                    return body.getData();
                }
            }

            throw new RuntimeException("DeerFlow研究服务返回错误");

        } catch (Exception e) {
            log.error("调用DeerFlow研究服务失败", e);
            throw new RuntimeException("调用DeerFlow研究服务失败：" + e.getMessage(), e);
        }
    }

    public String generateLearningPath(String topic, String currentLevel, String targetLevel) {
        try {
            log.info("调用DeerFlow研究服务生成学习路径，主题：{}，当前水平：{}，目标水平：{}", 
                     topic, currentLevel, targetLevel);

            Map<String, Object> request = new HashMap<>();
            request.put("topic", topic);
            request.put("current_level", currentLevel != null ? currentLevel : "beginner");
            request.put("target_level", targetLevel != null ? targetLevel : "advanced");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("发送请求到：{}", deerFlowApiUrl + "/api/research/learning-path");
            log.info("请求体：{}", request);

            ResponseEntity<DeerFlowResearchResponse> response = restTemplate.postForEntity(
                deerFlowApiUrl + "/api/research/learning-path",
                entity,
                DeerFlowResearchResponse.class
            );

            log.info("响应状态码：{}", response.getStatusCode());
            log.info("响应头：{}", response.getHeaders());
            
            if (response.getBody() != null) {
                log.info("响应体类型：{}", response.getBody().getClass().getName());
                log.info("响应体success：{}", response.getBody().getSuccess());
                log.info("响应体data长度：{}", response.getBody().getData() != null ? response.getBody().getData().length() : 0);
            }

            if (response.getStatusCode() == HttpStatus.OK) {
                DeerFlowResearchResponse body = response.getBody();
                if (body != null && Boolean.TRUE.equals(body.getSuccess())) {
                    log.info("DeerFlow研究服务返回成功，学习路径长度：{}", body.getData().length());
                    return body.getData();
                }
            }

            throw new RuntimeException("DeerFlow研究服务返回错误");

        } catch (Exception e) {
            log.error("调用DeerFlow研究服务失败", e);
            throw new RuntimeException("调用DeerFlow研究服务失败：" + e.getMessage(), e);
        }
    }

    public String researchKnowledgeGap(java.util.List<String> userKnowledge, String targetTopic) {
        try {
            log.info("调用DeerFlow研究服务分析知识盲区，目标主题：{}，知识点数量：{}", 
                     targetTopic, userKnowledge.size());

            Map<String, Object> request = new HashMap<>();
            request.put("user_knowledge", userKnowledge);
            request.put("target_topic", targetTopic);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<DeerFlowResearchResponse> response = restTemplate.postForEntity(
                deerFlowApiUrl + "/api/research/knowledge-gap",
                entity,
                DeerFlowResearchResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                DeerFlowResearchResponse body = response.getBody();
                if (body != null && Boolean.TRUE.equals(body.getSuccess())) {
                    log.info("DeerFlow研究服务返回成功，知识盲区分析长度：{}", body.getData().length());
                    return body.getData();
                }
            }

            throw new RuntimeException("DeerFlow研究服务返回错误");

        } catch (Exception e) {
            log.error("调用DeerFlow研究服务失败", e);
            throw new RuntimeException("调用DeerFlow研究服务失败：" + e.getMessage(), e);
        }
    }

    public boolean checkHealth() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                deerFlowApiUrl + "/health",
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                if (body != null && "healthy".equals(body.get("status"))) {
                    log.info("DeerFlow研究服务健康检查通过");
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.error("DeerFlow研究服务健康检查失败", e);
            return false;
        }
    }

    @Async("deerFlowTaskExecutor")
    public CompletableFuture<String> generateDeepLearningReportAsync(String learningData, String topic, String depth) {
        return CompletableFuture.completedFuture(generateDeepLearningReport(learningData, topic, depth));
    }

    @Async("deerFlowTaskExecutor")
    public CompletableFuture<String> generateLearningPathAsync(String topic, String currentLevel, String targetLevel) {
        return CompletableFuture.completedFuture(generateLearningPath(topic, currentLevel, targetLevel));
    }

    @Async("deerFlowTaskExecutor")
    public CompletableFuture<String> researchKnowledgeGapAsync(java.util.List<String> userKnowledge, String targetTopic) {
        return CompletableFuture.completedFuture(researchKnowledgeGap(userKnowledge, targetTopic));
    }
}
