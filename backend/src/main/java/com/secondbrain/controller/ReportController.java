package com.secondbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.common.Result;
import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.dto.ReportRequest;
import com.secondbrain.entity.LearningReport;
import com.secondbrain.service.DeerFlowReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/** 学习报告控制器. <p>学习报告生成接口</p> */
@RestController
@RequestMapping("/report")
@Tag(name = "学习报告", description = "学习报告生成接口")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final DeerFlowReportService deerFlowReportService;

    public ReportController(DeerFlowReportService deerFlowReportService) {
        this.deerFlowReportService = deerFlowReportService;
    }

    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    /**
     * 生成学习报告.
     *
     * @param request 报告请求
     * @param httpRequest HTTP请求对象
     * @return 学习报告内容
     */
    @PostMapping("/generate")
    @Operation(summary = "生成学习报告", description = "基于用户学习数据生成学习报告")
    public Result<String> generateReport(@RequestBody ReportRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);

        String report = deerFlowReportService.generateLearningReport(userId, workspaceId, request.getTopic(), request.getDays());
        return Result.success(report);
    }

    /**
     * 异步生成学习报告.
     *
     * @param request 报告请求
     * @param httpRequest HTTP请求对象
     * @return 异步任务响应
     */
    @PostMapping("/generate-async")
    @Operation(summary = "异步生成学习报告", description = "异步生成学习报告，返回任务ID")
    public Result<AsyncTaskResponse> generateReportAsync(@RequestBody ReportRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);

        AsyncTaskResponse task = deerFlowReportService.generateLearningReportAsync(userId, workspaceId, request.getTopic(), request.getDays());
        return Result.success("任务已创建，请使用任务ID查询进度", task);
    }

    /**
     * 获取报告列表.
     *
     * @param current 当前页
     * @param size 每页大小
     * @param httpRequest HTTP请求对象
     * @return 学习报告分页数据
     */
    @GetMapping("/list")
    @Operation(summary = "获取报告列表", description = "获取用户的学习报告列表")
    public Result<Page<LearningReport>> getReportList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);

        Page<LearningReport> result = deerFlowReportService.getReportList(userId, workspaceId, current, size);
        return Result.success(result);
    }

    /**
     * 获取报告详情.
     *
     * @param id 报告ID
     * @param httpRequest HTTP请求对象
     * @return 学习报告详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取报告详情", description = "获取指定学习报告的详细内容")
    public Result<LearningReport> getReportDetail(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);

        LearningReport report = deerFlowReportService.getReportById(id, userId, workspaceId);
        if (report == null) {
            return Result.error("报告不存在");
        }

        return Result.success(report);
    }

    /**
     * 删除报告.
     *
     * @param id 报告ID
     * @param httpRequest HTTP请求对象
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除报告", description = "删除指定的学习报告")
    public Result<String> deleteReport(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);

        boolean success = deerFlowReportService.deleteReport(id, userId, workspaceId);
        if (!success) {
            return Result.error("删除失败，报告不存在或无权限");
        }

        return Result.success("删除成功");
    }
}
