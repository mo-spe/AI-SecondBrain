package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.LearningReport;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.LearningReportMapper;
import com.secondbrain.service.AsyncTaskService;
import com.secondbrain.service.DeerFlowReportService;
import com.secondbrain.service.DeerFlowResearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * DeerFlow报告服务实现类.
 * <p>提供学习报告的生成、查询、删除功能，支持本地和远程两种服务模式</p>
 */
@Service
public class DeerFlowReportServiceImpl implements DeerFlowReportService {

    private static final Logger log = LoggerFactory.getLogger(DeerFlowReportServiceImpl.class);

    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final LearningReportMapper learningReportMapper;
    private final DeerFlowResearchService deerFlowResearchService;
    private final RestTemplate restTemplate;
    private final AsyncTaskService asyncTaskService;

    @Value("${deerflow.local.enabled:false}")
    private boolean useLocalService;

    @Value("${deerflow.local.report-url:http://localhost:8002}")
    private String localReportUrl;

    public DeerFlowReportServiceImpl(KnowledgeNodeMapper knowledgeNodeMapper, LearningReportMapper learningReportMapper,
                                     DeerFlowResearchService deerFlowResearchService, RestTemplate restTemplate,
                                     @Autowired(required = false) AsyncTaskService asyncTaskService) {
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.learningReportMapper = learningReportMapper;
        this.deerFlowResearchService = deerFlowResearchService;
        this.restTemplate = restTemplate;
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * 同步生成学习报告.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @param topic       报告主题
     * @param days        统计天数
     * @return 生成的报告内容
     */
    @Override
    public String generateLearningReport(Long userId, Long workspaceId, String topic, Integer days) {
        try {
            log.info("开始生成学习报告，用户ID：{}，workspaceId：{}，主题：{}，天数：{}", userId, workspaceId, topic, days);

            List<KnowledgeNode> knowledgeNodes = getLearningData(userId, workspaceId, days);
            if (knowledgeNodes.isEmpty()) {
                throw new IllegalStateException("指定时间范围内没有学习数据");
            }

            String report;
            if (useLocalService) {
                log.info("使用本地服务生成学习报告");
                report = generateLocalReport(topic, days);
            } else {
                String learningData = buildLearningData(topic, days, knowledgeNodes);
                report = deerFlowResearchService.generateDeepLearningReport(learningData, topic, "deep", null);
            }

            saveReportRecord(userId, workspaceId, topic, report, days);

            log.info("学习报告生成成功，用户ID：{}，报告长度：{}", userId, report.length());
            return report;
        } catch (Exception e) {
            log.error("生成学习报告失败，用户ID：{}，主题：{}", userId, topic, e);
            throw new IllegalStateException("生成学习报告失败：" + e.getMessage(), e);
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

            throw new IllegalStateException("本地报告服务返回错误");

        } catch (Exception e) {
            log.error("调用本地报告服务失败", e);
            throw new IllegalStateException("调用本地报告服务失败：" + e.getMessage(), e);
        }
    }

    private List<KnowledgeNode> getLearningData(Long userId, Long workspaceId, Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<KnowledgeNode>()
                .ge(KnowledgeNode::getCreateTime, startDate)
                .orderByDesc(KnowledgeNode::getCreateTime);
        if (workspaceId != null) {
            wrapper.eq(KnowledgeNode::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(KnowledgeNode::getUserId, userId);
        }
        return knowledgeNodeMapper.selectList(wrapper);
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

    private void saveReportRecord(Long userId, Long workspaceId, String topic, String report, Integer days) {
        LearningReport reportRecord = new LearningReport();
        reportRecord.setUserId(userId);
        reportRecord.setWorkspaceId(workspaceId);
        reportRecord.setTopic(topic);
        reportRecord.setContent(report);
        reportRecord.setDays(days);
        learningReportMapper.insert(reportRecord);
    }

    /**
     * 异步生成学习报告.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @param topic       报告主题
     * @param days        统计天数
     * @return 异步任务响应
     */
    @Override
    public AsyncTaskResponse generateLearningReportAsync(Long userId, Long workspaceId, String topic, Integer days) {
        log.info("异步生成学习报告，使用同步方式处理");

        log.warn("异步任务服务未启用，使用同步方式生成学习报告");
        String report = generateLearningReport(userId, workspaceId, topic, days);
        AsyncTaskResponse response = new AsyncTaskResponse();
        response.setStatus("COMPLETED");
        response.setTaskType("LEARNING_REPORT");
        response.setResult(report);
        response.setProgress(100);
        return response;
    }

    /**
     * 分页查询报告列表.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @param current     当前页
     * @param size        每页大小
     * @return 报告分页
     */
    @Override
    public Page<LearningReport> getReportList(Long userId, Long workspaceId, Integer current, Integer size) {
        Page<LearningReport> page = new Page<>(current, size);
        LambdaQueryWrapper<LearningReport> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        wrapper.orderByDesc(LearningReport::getCreateTime);
        return learningReportMapper.selectPage(page, wrapper);
    }

    /**
     * 根据ID查询报告.
     *
     * @param id          报告ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return 报告实体
     */
    @Override
    public LearningReport getReportById(Long id, Long userId, Long workspaceId) {
        LambdaQueryWrapper<LearningReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningReport::getId, id);
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        return learningReportMapper.selectOne(wrapper);
    }

    /**
     * 根据ID删除报告.
     *
     * @param id          报告ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return 是否删除成功
     */
    @Override
    public boolean deleteReport(Long id, Long userId, Long workspaceId) {
        LambdaQueryWrapper<LearningReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningReport::getId, id);
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        int result = learningReportMapper.delete(wrapper);
        return result > 0;
    }

    /**
     * 应用用户或工作区过滤条件.
     * workspaceId 为 null 时降级为 userId 过滤，兼容迁移前未分配工作区的历史数据.
     */
    private void applyUserOrWorkspaceFilter(LambdaQueryWrapper<LearningReport> wrapper, Long userId, Long workspaceId) {
        if (workspaceId != null) {
            wrapper.eq(LearningReport::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(LearningReport::getUserId, userId);
        }
    }
}
