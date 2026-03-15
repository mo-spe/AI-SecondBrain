package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.AsyncTaskRequest;
import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.LearningReport;
import com.secondbrain.kafka.KafkaProducerService;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.LearningReportMapper;
import com.secondbrain.service.AsyncTaskService;
import com.secondbrain.service.DeerFlowReportService;
import com.secondbrain.service.DeerFlowResearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeerFlowReportServiceImpl implements DeerFlowReportService {

    private static final Logger log = LoggerFactory.getLogger(DeerFlowReportServiceImpl.class);

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Autowired
    private LearningReportMapper learningReportMapper;

    @Autowired
    private DeerFlowResearchService deerFlowResearchService;

    @Autowired(required = false)
    private AsyncTaskService asyncTaskService;

    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${deerflow.local.enabled:false}")
    private boolean useLocalService;

    @Value("${deerflow.local.report-url:http://localhost:8002}")
    private String localReportUrl;

    @Override
    public String generateLearningReport(Long userId, String topic, Integer days) {
        try {
            log.info("开始生成学习报告，用户ID：{}，主题：{}，天数：{}", userId, topic, days);

            List<KnowledgeNode> knowledgeNodes = getLearningData(userId, days);
            if (knowledgeNodes.isEmpty()) {
                throw new RuntimeException("指定时间范围内没有学习数据");
            }

            String report;
            if (useLocalService) {
                log.info("使用本地服务生成学习报告");
                report = generateLocalReport(topic, days);
            } else {
                String learningData = buildLearningData(topic, days, knowledgeNodes);
                report = deerFlowResearchService.generateDeepLearningReport(learningData, topic, "deep", null);
            }
            
            saveReportRecord(userId, topic, report, days);

            log.info("学习报告生成成功，用户ID：{}，报告长度：{}", userId, report.length());
            return report;
        } catch (Exception e) {
            log.error("生成学习报告失败，用户ID：{}，主题：{}", userId, topic, e);
            throw new RuntimeException("生成学习报告失败：" + e.getMessage(), e);
        }
    }

    private String generateLocalReport(String topic, Integer days) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("topic", topic);
            request.put("days", days);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("使用本地服务，发送请求到：{}", localReportUrl + "/api/report/generate");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                localReportUrl + "/api/report/generate",
                entity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (Boolean.TRUE.equals(body.get("success"))) {
                    return (String) body.get("data");
                }
            }

            throw new RuntimeException("本地报告服务返回错误");

        } catch (Exception e) {
            log.error("调用本地报告服务失败", e);
            throw new RuntimeException("调用本地报告服务失败：" + e.getMessage(), e);
        }
    }

    private List<KnowledgeNode> getLearningData(Long userId, Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getUserId, userId)
                .ge(KnowledgeNode::getCreateTime, startDate)
                .orderByDesc(KnowledgeNode::getCreateTime)
        );
    }

    private String buildLearningData(String topic, Integer days, List<KnowledgeNode> knowledgeNodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("学习主题：").append(topic).append("\n\n");
        sb.append("学习数据概览：\n");
        sb.append("- 知识点数量：").append(knowledgeNodes.size()).append("个\n");
        sb.append("- 学习时间范围：最近").append(days).append("天\n\n");

        double avgImportance = knowledgeNodes.stream()
            .mapToInt(KnowledgeNode::getImportance)
            .average()
            .orElse(0);
        double avgMastery = knowledgeNodes.stream()
            .mapToInt(KnowledgeNode::getMasteryLevel)
            .average()
            .orElse(0);

        sb.append("学习统计：\n");
        sb.append("- 平均重要程度：").append(String.format("%.1f", avgImportance)).append("/5\n");
        sb.append("- 平均掌握程度：").append(String.format("%.1f", avgMastery)).append("/5\n\n");

        sb.append("主要知识点：\n");
        knowledgeNodes.stream()
            .limit(15)
            .forEach(node -> {
                sb.append("- ").append(node.getTitle()).append("\n");
                sb.append("  ").append(node.getSummary()).append("\n");
            });

        return sb.toString();
    }

    private void saveReportRecord(Long userId, String topic, String report, Integer days) {
        LearningReport reportRecord = new LearningReport();
        reportRecord.setUserId(userId);
        reportRecord.setTopic(topic);
        reportRecord.setContent(report);
        reportRecord.setDays(days);
        learningReportMapper.insert(reportRecord);
    }

    @Override
    public AsyncTaskResponse generateLearningReportAsync(Long userId, String topic, Integer days) {
        log.info("异步生成学习报告，使用同步方式处理");
        
        log.warn("异步任务服务未启用，使用同步方式生成学习报告");
        String report = generateLearningReport(userId, topic, days);
        AsyncTaskResponse response = new AsyncTaskResponse();
        response.setStatus("COMPLETED");
        response.setTaskType("LEARNING_REPORT");
        response.setResult(report);
        response.setProgress(100);
        return response;
    }

    @Override
    public Page<LearningReport> getReportList(Long userId, Integer current, Integer size) {
        Page<LearningReport> page = new Page<>(current, size);
        return learningReportMapper.selectPage(
            page,
            new LambdaQueryWrapper<LearningReport>()
                .eq(LearningReport::getUserId, userId)
                .orderByDesc(LearningReport::getCreateTime)
        );
    }

    @Override
    public LearningReport getReportById(Long id, Long userId) {
        return learningReportMapper.selectOne(
            new LambdaQueryWrapper<LearningReport>()
                .eq(LearningReport::getId, id)
                .eq(LearningReport::getUserId, userId)
        );
    }

    @Override
    public boolean deleteReport(Long id, Long userId) {
        int result = learningReportMapper.delete(
            new LambdaQueryWrapper<LearningReport>()
                .eq(LearningReport::getId, id)
                .eq(LearningReport::getUserId, userId)
        );
        return result > 0;
    }
}