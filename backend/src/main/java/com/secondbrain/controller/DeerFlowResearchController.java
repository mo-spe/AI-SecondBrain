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

    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    /**
     * 异步生成学习报告.
     *
     * @param request 研究请求
     * @param httpRequest HTTP请求对象
     * @return 异步任务响应
     */
    @PostMapping("/research/learning-report-async")
    @Operation(summary = "异步生成学习报告", description = "异步生成学习报告，返回任务ID")
    public Result<AsyncTaskResponse> generateLearningReportAsync(
            @RequestBody DeerFlowResearchRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);

        String topic = resolveTopic(request);
        String depth = resolveDepth(request);

        log.info("收到异步生成学习报告请求，用户ID：{}，主题：{}，深度：{}", userId, topic, depth);

        String userApiKey = getUserApiKey(userId);

        AsyncTaskResponse task = deerFlowResearchService.generateLearningReportAsync(
                userId, workspaceId, topic, depth, userApiKey);

        return Result.success(task);
    }

    /**
     * 异步生成学习路径.
     *
     * @param request 研究请求
     * @param httpRequest HTTP请求对象
     * @return 异步任务响应
     */
    @PostMapping("/research/learning-path-async")
    @Operation(summary = "异步生成学习路径", description = "异步生成学习路径，返回任务ID")
    public Result<AsyncTaskResponse> generateLearningPathAsync(
            @RequestBody DeerFlowResearchRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);

        String topic = resolveTopic(request);
        String currentLevel = request.getCurrentLevel();
        String targetLevel = request.getTargetLevel();

        log.info("收到异步生成学习路径请求，用户ID：{}，主题：{}，当前水平：{}，目标水平：{}", userId, topic, currentLevel, targetLevel);

        String userApiKey = getUserApiKey(userId);

        AsyncTaskResponse task = deerFlowResearchService.generateLearningPathAsync(
                userId, workspaceId, topic, currentLevel, targetLevel, userApiKey);

        return Result.success(task);
    }

    /**
     * 异步分析知识盲区.
     *
     * @param request 研究请求
     * @param httpRequest HTTP请求对象
     * @return 异步任务响应
     */
    @PostMapping("/research/knowledge-blind-spot-async")
    @Operation(summary = "异步分析知识盲区", description = "异步分析知识盲区，返回任务ID")
    public Result<AsyncTaskResponse> analyzeKnowledgeBlindSpotAsync(
            @RequestBody DeerFlowResearchRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);

        String topic = resolveTopic(request);
        java.util.List<String> userKnowledge = request.getUserKnowledge();

        log.info("收到异步分析知识盲区请求，用户ID：{}，主题：{}，知识点数量：{}", userId, topic, userKnowledge != null ? userKnowledge.size() : 0);

        String userApiKey = getUserApiKey(userId);

        AsyncTaskResponse task = deerFlowResearchService.researchKnowledgeGapAsync(
                userId, workspaceId, userKnowledge, topic, userApiKey);

        return Result.success(task);
    }

    /**
     * 查询研究任务状态.
     *
     * @param taskNumber 任务编号
     * @return 异步任务响应
     */
    @GetMapping("/research/status/{taskNumber}")
    @Operation(summary = "查询研究任务状态", description = "根据任务编号查询异步任务的状态")
    public Result<AsyncTaskResponse> getResearchTaskStatus(@PathVariable String taskNumber) {
        log.info("查询研究任务状态，任务编号：{}", taskNumber);

        AsyncTaskResponse task = deerFlowResearchService.getTaskStatus(taskNumber);
        if (task == null) {
            return Result.error("任务不存在");
        }

        return Result.success(task);
    }

    /**
     * 健康检查.
     *
     * @return 健康状态信息
     */
    @GetMapping("/research/health")
    @Operation(summary = "健康检查", description = "检查DeerFlow研究服务是否可用")
    public Result<Map<String, Object>> checkHealth() {
        boolean isHealthy = deerFlowResearchService.checkHealth();

        Map<String, Object> data = new HashMap<>();
        data.put("deerflow_healthy", isHealthy);
        data.put("message", isHealthy ? "DeerFlow研究服务正常" : "DeerFlow研究服务异常");

        return Result.success("健康检查完成", data);
    }

    /**
     * 保存研究历史.
     *
     * @param request 研究历史请求
     * @param httpRequest HTTP请求对象
     * @return 研究历史记录
     */
    @PostMapping("/research/history")
    @Operation(summary = "保存研究历史", description = "保存AI研究历史记录")
    public Result<ResearchHistory> saveResearchHistory(@RequestBody com.secondbrain.dto.ResearchHistoryRequest request,
                                                        HttpServletRequest httpRequest) {
        log.info("收到保存研究历史请求，类型：{}，主题：{}", request.getType(), request.getTopic());

        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.success("未登录，不保存历史记录", null);
        }
        Long workspaceId = getWorkspaceId(httpRequest);

        ResearchHistory history = researchHistoryService.save(request, userId, workspaceId);
        return Result.success("保存成功", history);
    }

    /**
     * 获取研究历史列表.
     *
     * @param current 当前页
     * @param size 每页大小
     * @param type 研究类型
     * @param httpRequest HTTP请求对象
     * @return 研究历史列表数据
     */
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
        Long workspaceId = getWorkspaceId(httpRequest);

        IPage<ResearchHistory> page = researchHistoryService.getList(current, size, userId, workspaceId, type);

        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());

        return Result.success("获取成功", data);
    }

    /**
     * 删除研究历史.
     *
     * @param id 研究历史ID
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @DeleteMapping("/research/history/{id}")
    @Operation(summary = "删除研究历史", description = "删除研究历史记录")
    public Result<Void> deleteResearchHistory(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        Long workspaceId = getWorkspaceId(httpRequest);
        researchHistoryService.deleteById(id, userId, workspaceId);
        return Result.<Void>success("删除成功", null);
    }

    /**
     * 从请求属性中解析当前登录用户ID.
     *
     * @param request HTTP请求
     * @return 用户ID
     */
    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalStateException("用户未登录");
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
        if (request.getTopic() != null && !request.getTopic().isEmpty()) {
            return request.getTopic();
        }
        return "综合学习分析";
    }

    /**
     * 从请求中解析深度参数.
     *
     * @param request 研究请求
     * @return 深度
     */
    private String resolveDepth(DeerFlowResearchRequest request) {
        return request.getDepth() != null ? request.getDepth() : "medium";
    }

    private String getUserApiKey(Long userId) {
        try {
            User user = userService.getUserById(userId);
            if (user != null && user.getApiKey() != null && !user.getApiKey().isEmpty()) {
                return user.getApiKey();
            }
        } catch (Exception e) {
            log.warn("获取用户 API Key 失败，userId：{}", userId);
        }
        return null;
    }
}
