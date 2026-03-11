package com.secondbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.common.Result;
import com.secondbrain.dto.ReportRequest;
import com.secondbrain.entity.LearningReport;
import com.secondbrain.service.DeerFlowReportService;
import com.secondbrain.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
@Tag(name = "学习报告", description = "学习报告生成接口")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final DeerFlowReportService deerFlowReportService;
    private final JwtUtil jwtUtil;

    @Autowired
    public ReportController(DeerFlowReportService deerFlowReportService, JwtUtil jwtUtil) {
        this.deerFlowReportService = deerFlowReportService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/generate")
    @Operation(summary = "生成学习报告", description = "基于用户学习数据生成学习报告")
    public Result<String> generateReport(
            @RequestBody ReportRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserId(httpRequest);
            if (userId == null) {
                return Result.error("无法获取用户ID，请重新登录");
            }
            
            log.info("收到生成学习报告请求，用户ID：{}，主题：{}，天数：{}", 
                userId, request.getTopic(), request.getDays());
            
            String report = deerFlowReportService.generateLearningReport(
                userId, 
                request.getTopic(), 
                request.getDays()
            );
            
            return Result.success(report);
        } catch (Exception e) {
            log.error("生成学习报告失败", e);
            return Result.error("生成学习报告失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "获取报告列表", description = "获取用户的学习报告列表")
    public Result<Page<LearningReport>> getReportList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserId(httpRequest);
            if (userId == null) {
                return Result.error("无法获取用户ID，请重新登录");
            }
            
            log.info("收到获取报告列表请求，用户ID：{}，当前页：{}，每页大小：{}", 
                userId, current, size);
            
            Page<LearningReport> result = deerFlowReportService.getReportList(userId, current, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取报告列表失败", e);
            return Result.error("获取报告列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取报告详情", description = "获取指定学习报告的详细内容")
    public Result<LearningReport> getReportDetail(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserId(httpRequest);
            if (userId == null) {
                return Result.error("无法获取用户ID，请重新登录");
            }
            
            log.info("收到获取报告详情请求，用户ID：{}，报告ID：{}", userId, id);
            
            LearningReport report = deerFlowReportService.getReportById(id, userId);
            if (report == null) {
                return Result.error("报告不存在");
            }
            
            return Result.success(report);
        } catch (Exception e) {
            log.error("获取报告详情失败", e);
            return Result.error("获取报告详情失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除报告", description = "删除指定的学习报告")
    public Result<String> deleteReport(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getUserId(httpRequest);
            if (userId == null) {
                return Result.error("无法获取用户ID，请重新登录");
            }
            
            log.info("收到删除报告请求，用户ID：{}，报告ID：{}", userId, id);
            
            boolean success = deerFlowReportService.deleteReport(id, userId);
            if (!success) {
                return Result.error("删除失败，报告不存在或无权限");
            }
            
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除报告失败", e);
            return Result.error("删除报告失败：" + e.getMessage());
        }
    }

    private Long getUserId(HttpServletRequest httpRequest) {
        Long userIdFromToken = (Long) httpRequest.getAttribute("userId");
        if (userIdFromToken != null) {
            return userIdFromToken;
        }

        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                return jwtUtil.getUserIdFromToken(token.substring(7));
            } catch (Exception e) {
                log.error("从token解析userId失败", e);
            }
        }

        return null;
    }
}