package com.secondbrain.service;

import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.dto.AsyncTaskRequest;
import com.secondbrain.dto.DeerFlowResearchRequest;
import com.secondbrain.dto.DeerFlowResearchResponse;
import com.secondbrain.kafka.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
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

    @Value("${deerflow.local.enabled:false}")
    private boolean useLocalService;

    @Value("${deerflow.local.learning-path-url:http://localhost:8001}")
    private String localLearningPathUrl;

    @Value("${deerflow.local.report-url:http://localhost:8002}")
    private String localReportUrl;

    private final RestTemplate restTemplate;

    @Lazy
    @Autowired(required = false)
    private AsyncTaskService asyncTaskService;

    @Lazy
    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;

    @Autowired
    public DeerFlowResearchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateDeepLearningReport(String learningData, String topic, String depth, String userApiKey) {
        try {
            log.info("调用DeerFlow研究服务生成深度学习报告，主题：{}，深度：{}，使用用户API Key：{}", 
                     topic, depth, userApiKey != null && !userApiKey.isEmpty());

            Map<String, Object> request = new HashMap<>();
            request.put("learning_data", learningData);
            request.put("topic", topic);
            request.put("depth", depth != null ? depth : "deep");
            if (userApiKey != null && !userApiKey.isEmpty()) {
                request.put("api_key", userApiKey);
            }

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

    public String generateLearningPath(String topic, String currentLevel, String targetLevel, String userApiKey) {
        try {
            log.info("生成学习路径，主题：{}，当前水平：{}，目标水平：{}，使用用户API Key：{}", 
                     topic, currentLevel, targetLevel, userApiKey != null && !userApiKey.isEmpty());

            Map<String, Object> request = new HashMap<>();
            request.put("topic", topic);
            request.put("current_level", currentLevel != null ? currentLevel : "beginner");
            request.put("target_level", targetLevel != null ? targetLevel : "advanced");
            if (userApiKey != null && !userApiKey.isEmpty()) {
                request.put("api_key", userApiKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            String apiUrl = useLocalService ? 
                localLearningPathUrl + "/api/research/learning-path" : 
                deerFlowApiUrl + "/api/research/learning-path";

            log.info("使用{}服务，发送请求到：{}", useLocalService ? "本地" : "DeerFlow", apiUrl);
            log.info("请求体：{}", request);

            ResponseEntity<DeerFlowResearchResponse> response = restTemplate.postForEntity(
                apiUrl,
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
                    String serviceName = useLocalService ? "本地服务" : "DeerFlow研究服务";
                    log.info("{}返回成功，学习路径长度：{}", serviceName, body.getData().length());
                    return body.getData();
                }
            }

            String serviceName = useLocalService ? "本地服务" : "DeerFlow研究服务";
            throw new RuntimeException(serviceName + "返回错误");

        } catch (Exception e) {
            String serviceName = useLocalService ? "本地服务" : "DeerFlow研究服务";
            log.error("调用{}失败", serviceName, e);
            throw new RuntimeException("调用" + serviceName + "失败：" + e.getMessage(), e);
        }
    }

    public String researchKnowledgeGap(java.util.List<String> userKnowledge, String targetTopic, String userApiKey) {
        try {
            log.info("调用DeerFlow研究服务分析知识盲区，目标主题：{}，知识点数量：{}，使用用户API Key：{}", 
                     targetTopic, userKnowledge.size(), userApiKey != null && !userApiKey.isEmpty());

            Map<String, Object> request = new HashMap<>();
            request.put("user_knowledge", userKnowledge);
            request.put("target_topic", targetTopic);
            if (userApiKey != null && !userApiKey.isEmpty()) {
                request.put("api_key", userApiKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            String apiUrl = useLocalService ? 
                localLearningPathUrl + "/api/research/knowledge-gap" : 
                deerFlowApiUrl + "/api/research/knowledge-gap";

            log.info("使用{}服务，发送请求到：{}", useLocalService ? "本地" : "DeerFlow", apiUrl);

            ResponseEntity<DeerFlowResearchResponse> response = restTemplate.postForEntity(
                apiUrl,
                entity,
                DeerFlowResearchResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                DeerFlowResearchResponse body = response.getBody();
                if (body != null && Boolean.TRUE.equals(body.getSuccess())) {
                    String serviceName = useLocalService ? "本地服务" : "DeerFlow研究服务";
                    log.info("{}返回成功，知识盲区分析长度：{}", serviceName, body.getData().length());
                    return body.getData();
                }
            }

            String serviceName = useLocalService ? "本地服务" : "DeerFlow研究服务";
            throw new RuntimeException(serviceName + "返回错误");

        } catch (Exception e) {
            String serviceName = useLocalService ? "本地服务" : "DeerFlow研究服务";
            log.error("调用{}失败", serviceName, e);
            throw new RuntimeException("调用" + serviceName + "失败：" + e.getMessage(), e);
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
        return CompletableFuture.completedFuture(generateDeepLearningReport(learningData, topic, depth, null));
    }

    public AsyncTaskResponse generateLearningReportAsync(Long userId, String topic, String depth, String userApiKey) {
        log.info("异步生成学习报告，用户ID：{}，主题：{}，深度：{}，使用用户API Key：{}", 
                 userId, topic, depth, userApiKey != null && !userApiKey.isEmpty());
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("topic", topic);
        parameters.put("depth", depth);
        if (userApiKey != null && !userApiKey.isEmpty()) {
            parameters.put("api_key", userApiKey);
        }
        
        AsyncTaskResponse task = asyncTaskService.createTask("LEARNING_REPORT", userId, parameters);
        
        log.info("创建异步任务，taskNumber：{}，消息已发送到Kafka", task.getTaskId());
        
        return task;
    }

    public AsyncTaskResponse generateLearningPathAsync(Long userId, String topic, String currentLevel, String targetLevel, String userApiKey) {
        log.info("异步生成学习路径，用户ID：{}，主题：{}，使用用户API Key：{}", 
                 userId, topic, userApiKey != null && !userApiKey.isEmpty());
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("topic", topic);
        parameters.put("current_level", currentLevel);
        parameters.put("target_level", targetLevel);
        if (userApiKey != null && !userApiKey.isEmpty()) {
            parameters.put("api_key", userApiKey);
        }
        
        AsyncTaskResponse task = asyncTaskService.createTask("LEARNING_PATH", userId, parameters);
        
        log.info("创建异步任务，taskNumber：{}，消息已发送到Kafka", task.getTaskId());
        
        return task;
    }
    
    public AsyncTaskResponse getTaskStatus(String taskId) {
        return asyncTaskService.getTaskStatus(taskId);
    }

    public AsyncTaskResponse researchKnowledgeGapAsync(Long userId, java.util.List<String> userKnowledge, String targetTopic, String userApiKey) {
        log.info("异步分析知识盲区，用户ID：{}，目标主题：{}，使用用户API Key：{}", 
                 userId, targetTopic, userApiKey != null && !userApiKey.isEmpty());
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("user_knowledge", userKnowledge);
        parameters.put("topic", targetTopic);
        if (userApiKey != null && !userApiKey.isEmpty()) {
            parameters.put("api_key", userApiKey);
        }
        
        AsyncTaskResponse task = asyncTaskService.createTask("KNOWLEDGE_BLIND_SPOT", userId, parameters);
        
        log.info("创建异步任务，taskNumber：{}，消息已发送到Kafka", task.getTaskId());
        
        return task;
    }

    @Async("deerFlowTaskExecutor")
    public CompletableFuture<String> generateLearningPathAsync(String topic, String currentLevel, String targetLevel) {
        return CompletableFuture.completedFuture(generateLearningPath(topic, currentLevel, targetLevel, null));
    }

    @Async("deerFlowTaskExecutor")
    public CompletableFuture<String> researchKnowledgeGapAsync(java.util.List<String> userKnowledge, String targetTopic) {
        return CompletableFuture.completedFuture(researchKnowledgeGap(userKnowledge, targetTopic, null));
    }
}