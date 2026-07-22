package com.secondbrain.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.common.Result;
import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.dto.DeerFlowResearchRequest;
import com.secondbrain.entity.ResearchHistory;
import com.secondbrain.entity.User;
import com.secondbrain.service.DeerFlowResearchService;
import com.secondbrain.service.ResearchHistoryService;
import com.secondbrain.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * AI知识研究控制器.
 * <p>提供学习报告、学习路径、知识盲区分析等AI研究功能接口</p>
 */
@RestController
@RequestMapping("/deerflow")
@Tag(name = "AI知识研究", description = "AI知识研究接口")
public class DeerFlowResearchController {

    private static final Logger log = LoggerFactory.getLogger(DeerFlowResearchController.class);

    private final DeerFlowResearchService deerFlowResearchService;
    private final ResearchHistoryService researchHistoryService;
    private final UserService userService;

    public DeerFlowResearchController(DeerFlowResearchService deerFlowResearchService,
                                      ResearchHistoryService researchHistoryService,
                                      UserService userService) {
        this.deerFlowResearchService = deerFlowResearchService;
        this.researchHistoryService = researchHistoryService;
        this.userService = userService;
    }

    @PostMapping("/research/learning-report-async")
    @Operation(summary = "异步生成学习报告", description = "异步生成学习报告，返回任务ID")
    public Result<AsyncTaskResponse> generateLearningReportAsync(
            @RequestBody DeerFlowResearchRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);

        String topic = resolveTopic(request);
        String depth = resolveDepth(request);

        log.info("收到异步生成学习报告请求，用户ID：{}，主题：{}，深度：{}", userId, topic, depth);

        String userApiKey = getUserApiKey(userId);

        AsyncTaskResponse task = deerFlowResearchService.generateLearningReportAsync(
            userId, topic, depth, userApiKey
        );

        return Result.success("任务已创建，请使用任务ID查询进度", task);
    }

    @PostMapping("/research/learning-report")
    @Operation(summary = "生成学习报告", description = "同步生成深度学习报告")
    public Result<String> generateLearningReport(@RequestBody DeerFlowResearchRequest request) {
        log.info("收到生成学习报告请求，主题：{}", request.getTopic());

        String report = deerFlowResearchService.generateDeepLearningReport(
            request.getLearningData(),
            request.getTopic(),
            request.getDepth(),
            null
        );

        return Result.success("学习报告生成成功", report);
    }

    @PostMapping("/research/learning-path")
    @Operation(summary = "生成学习路径", description = "同步生成个性化学习路径")
    public Result<String> generateLearningPath(@RequestBody DeerFlowResearchRequest request) {
        log.info("收到生成学习路径请求，主题：{}", request.getTopic());

        String learningPath = deerFlowResearchService.generateLearningPath(
            request.getTopic(),
            request.getCurrentLevel(),
            request.getTargetLevel(),
            null
        );

        return Result.success("学习路径生成成功", learningPath);
    }

    @PostMapping("/research/learning-path-async")
    @Operation(summary = "异步生成学习路径", description = "异步生成学习路径，返回任务ID")
    public Result<AsyncTaskResponse> generateLearningPathAsync(
            @RequestBody DeerFlowResearchRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);

        String topic = resolveTopic(request);
        String currentLevel = resolveCurrentLevel(request);
        String targetLevel = resolveTargetLevel(request);

        log.info("收到异步生成学习路径请求，用户ID：{}，主题：{}，当前水平：{}，目标水平：{}",
                 userId, topic, currentLevel, targetLevel);

        String userApiKey = getUserApiKey(userId);

        AsyncTaskResponse task = deerFlowResearchService.generateLearningPathAsync(
            userId, topic, currentLevel, targetLevel, userApiKey
        );

        return Result.success("任务已创建，请使用任务ID查询进度", task);
    }

    @PostMapping("/research/knowledge-gap")
    @Operation(summary = "分析知识盲区", description = "同步分析用户知识盲区")
    public Result<String> researchKnowledgeGap(@RequestBody DeerFlowResearchRequest request) {
        log.info("收到知识盲区分析请求，目标主题：{}", request.getTopic());

        String gapAnalysis = deerFlowResearchService.researchKnowledgeGap(
            request.getUserKnowledge(),
            request.getTopic(),
            null
        );

        return Result.success("知识盲区分析成功", gapAnalysis);
    }

    @PostMapping("/research/knowledge-gap-async")
    @Operation(summary = "异步分析知识盲区", description = "异步分析知识盲区，返回任务ID")
    public Result<AsyncTaskResponse> researchKnowledgeGapAsync(
            @RequestBody DeerFlowResearchRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);

        log.info("收到异步知识盲区分析请求，用户ID：{}，目标主题：{}", userId, request.getTopic());

        String userApiKey = getUserApiKey(userId);

        AsyncTaskResponse task = deerFlowResearchService.researchKnowledgeGapAsync(
            userId, request.getUserKnowledge(), request.getTopic(), userApiKey
        );

        return Result.success("任务已创建，请使用任务ID查询进度", task);
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查DeerFlow研究服务健康状态")
    public Result<Map<String, Object>> healthCheck() {
        boolean isHealthy = deerFlowResearchService.checkHealth();

        Map<String, Object> data = new HashMap<>();
        data.put("deerflow_healthy", isHealthy);
        data.put("message", isHealthy ? "DeerFlow研究服务正常" : "DeerFlow研究服务异常");

        return Result.success("健康检查完成", data);
    }

    @PostMapping("/research/history")
    @Operation(summary = "保存研究历史", description = "保存AI研究历史记录")
    public Result<ResearchHistory> saveResearchHistory(@RequestBody com.secondbrain.dto.ResearchHistoryRequest request,
                                                        HttpServletRequest httpRequest) {
        log.info("收到保存研究历史请求，类型：{}，主题：{}", request.getType(), request.getTopic());

        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.success("未登录，不保存历史记录", null);
        }

        ResearchHistory history = researchHistoryService.save(request, userId);
        return Result.success("保存成功", history);
    }

    @GetMapping("/research/history")
    @Operation(summary = "获取研究历史列表", description = "分页获取研究历史记录")
    public Result<Map<String, Object>> getResearchHistoryList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            HttpServletRequest httpRequest) {
        log.info("收到获取研究历史列表请求，当前页：{}，每页大小：{}，类型：{}", current, size, type);

        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            Map<String, Object> data = new HashMap<>();
            data.put("records", new ArrayList<>());
            data.put("total", 0);
            data.put("current", current);
            data.put("size", size);
            return Result.success("获取成功", data);
        }

        IPage<ResearchHistory> page = researchHistoryService.getList(current, size, userId, type);

        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());

        return Result.success("获取成功", data);
    }

    @DeleteMapping("/research/history/{id}")
    @Operation(summary = "删除研究历史", description = "根据ID删除研究历史记录")
    public Result<Void> deleteResearchHistory(@PathVariable Long id, HttpServletRequest httpRequest) {
        log.info("收到删除研究历史请求，ID：{}", id);

        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.success("未登录，不删除历史记录", null);
        }

        researchHistoryService.deleteById(id, userId);
        return Result.success("删除成功", null);
    }

    @GetMapping("/async-task/status/{taskId}")
    @Operation(summary = "查询异步任务状态", description = "根据任务ID查询异步任务的执行状态和结果")
    public Result<AsyncTaskResponse> getTaskStatus(@PathVariable String taskId) {
        log.info("收到任务状态查询请求，taskNumber：{}", taskId);

        AsyncTaskResponse task = deerFlowResearchService.getTaskStatus(taskId);

        if (task == null) {
            log.warn("任务不存在，taskNumber：{}", taskId);
            return Result.error("任务不存在");
        }

        return Result.success("查询成功", task);
    }

    /**
     * 从请求属性中获取用户ID，如不存在则返回默认值.
     *
     * @param request HTTP请求
     * @return 用户ID
     */
    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            userId = 1L;
            log.warn("无法获取用户ID，使用默认用户ID：{}", userId);
        }
        return userId;
    }

    /**
     * 从请求中解析研究主题.
     *
     * @param request 研究请求
     * @return 主题
     */
    private String resolveTopic(DeerFlowResearchRequest request) {
        String topic = request.getTopic();
        if (topic == null || topic.isEmpty()) {
            topic = request.getLearningData();
        }
        if (topic == null || topic.isEmpty()) {
            topic = request.getGoal();
        }
        return topic;
    }

    /**
     * 从请求中解析深度参数.
     *
     * @param request 研究请求
     * @return 深度
     */
    private String resolveDepth(DeerFlowResearchRequest request) {
        String depth = request.getDepth();
        return (depth == null || depth.isEmpty()) ? "medium" : depth;
    }

    /**
     * 从请求中解析当前水平.
     *
     * @param request 研究请求
     * @return 当前水平
     */
    private String resolveCurrentLevel(DeerFlowResearchRequest request) {
        String level = request.getCurrentLevel();
        return (level == null || level.isEmpty()) ? "beginner" : level;
    }

    /**
     * 从请求中解析目标水平.
     *
     * @param request 研究请求
     * @return 目标水平
     */
    private String resolveTargetLevel(DeerFlowResearchRequest request) {
        String level = request.getTargetLevel();
        return (level == null || level.isEmpty()) ? "advanced" : level;
    }

    /**
     * 获取用户API Key.
     *
     * @param userId 用户ID
     * @return API Key，获取失败返回null
     */
    private String getUserApiKey(Long userId) {
        try {
            User user = userService.getUserById(userId);
            if (user != null) {
                return user.getApiKey();
            }
        } catch (Exception e) {
            log.warn("获取用户API Key失败：{}", e.getMessage());
        }
        return null;
    }
}
