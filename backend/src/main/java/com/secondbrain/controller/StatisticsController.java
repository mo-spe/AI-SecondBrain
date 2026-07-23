package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.service.ChatService;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.service.ReviewCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 数据统计控制器. <p>提供学习数据统计和图表数据查询接口</p> */
@RestController
@RequestMapping("/statistics")
@Tag(name = "数据统计", description = "数据统计相关接口")
public class StatisticsController {

    private static final Logger log = LoggerFactory.getLogger(StatisticsController.class);

    private final ChatService chatService;
    private final KnowledgeService knowledgeService;
    private final ReviewCardService reviewCardService;

    public StatisticsController(ChatService chatService, KnowledgeService knowledgeService, ReviewCardService reviewCardService) {
        this.chatService = chatService;
        this.knowledgeService = knowledgeService;
        this.reviewCardService = reviewCardService;
    }

    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    /**
     * 获取统计数据.
     *
     * @param httpRequest HTTP请求对象
     * @return 统计数据
     */
    @GetMapping
    @Operation(summary = "获取统计数据", description = "获取对话、知识点、复习等统计数据")
    public Result<Map<String, Object>> getStatistics(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);

        if (userId == null) {
            log.error("userId为null，无法获取统计数据");
            return Result.error("用户未登录");
        }

        Map<String, Object> statistics = new HashMap<>();

        try {
            long chatCount = chatService.countByUserId(userId, workspaceId);
            statistics.put("chatCount", chatCount);
        } catch (RuntimeException e) {
            log.error("获取对话总数失败", e);
            statistics.put("chatCount", 0);
        }

        try {
            long knowledgeCount = knowledgeService.countByUserId(userId, workspaceId);
            statistics.put("knowledgeCount", knowledgeCount);
        } catch (RuntimeException e) {
            log.error("获取知识点总数失败", e);
            statistics.put("knowledgeCount", 0);
        }

        try {
            long pendingReviewCount = reviewCardService.countPendingByUserId(userId, workspaceId);
            statistics.put("pendingReviewCount", pendingReviewCount);
        } catch (RuntimeException e) {
            log.error("获取待复习数量失败", e);
            statistics.put("pendingReviewCount", 0);
        }

        try {
            long completedReviewCount = reviewCardService.countCompletedByUserId(userId, workspaceId);
            statistics.put("completedReviewCount", completedReviewCount);
        } catch (RuntimeException e) {
            log.error("获取已完成复习数量失败", e);
            statistics.put("completedReviewCount", 0);
        }

        return Result.success(statistics);
    }

    /**
     * 获取图表数据.
     *
     * @param period 时间周期：week-本周，month-本月，year-全年
     * @param httpRequest HTTP请求对象
     * @return 图表数据
     */
    @GetMapping("/chart")
    @Operation(summary = "获取图表数据", description = "获取学习趋势图表数据")
    public Result<Map<String, Object>> getChartData(
            @Parameter(description = "时间周期：week-本周，month-本月，year-全年")
            @RequestParam(defaultValue = "week") String period,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);

        if (userId == null) {
            log.error("userId为null，无法获取图表数据");
            return Result.error("用户未登录");
        }

        Map<String, Object> chartData = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> chatData = new ArrayList<>();
        List<Long> knowledgeData = new ArrayList<>();
        List<Long> reviewData = new ArrayList<>();

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if ("week".equals(period)) {
            LocalDate startOfWeek = today.minusDays(6);
            for (int i = 0; i < 7; i++) {
                LocalDate date = startOfWeek.plusDays(i);
                labels.add(date.format(formatter));
                chatData.add(getChatCountByDate(userId, date, workspaceId));
                knowledgeData.add(getKnowledgeCountByDate(userId, date, workspaceId));
                reviewData.add(getReviewCountByDate(userId, date, workspaceId));
            }
        } else if ("month".equals(period)) {
            LocalDate startOfMonth = today.minusDays(29);
            for (int i = 0; i < 30; i++) {
                LocalDate date = startOfMonth.plusDays(i);
                labels.add(date.format(formatter));
                chatData.add(getChatCountByDate(userId, date, workspaceId));
                knowledgeData.add(getKnowledgeCountByDate(userId, date, workspaceId));
                reviewData.add(getReviewCountByDate(userId, date, workspaceId));
            }
        } else if ("year".equals(period)) {
            LocalDate startOfYear = today.minusDays(364);
            for (int i = 0; i < 12; i++) {
                LocalDate date = startOfYear.plusMonths(i);
                labels.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                chatData.add(getChatCountByMonth(userId, date, workspaceId));
                knowledgeData.add(getKnowledgeCountByMonth(userId, date, workspaceId));
                reviewData.add(getReviewCountByMonth(userId, date, workspaceId));
            }
        }

        chartData.put("labels", labels);
        chartData.put("chatData", chatData);
        chartData.put("knowledgeData", knowledgeData);
        chartData.put("reviewData", reviewData);

        return Result.success(chartData);
    }

    private Long getChatCountByDate(Long userId, LocalDate date, Long workspaceId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return chatService.countByUserIdAndDateRange(userId, startOfDay, endOfDay, workspaceId);
    }

    private Long getKnowledgeCountByDate(Long userId, LocalDate date, Long workspaceId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return knowledgeService.countByUserIdAndDateRange(userId, startOfDay, endOfDay, workspaceId);
    }

    private Long getReviewCountByDate(Long userId, LocalDate date, Long workspaceId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return reviewCardService.countByUserIdAndDateRange(userId, startOfDay, endOfDay, workspaceId);
    }

    private Long getChatCountByMonth(Long userId, LocalDate date, Long workspaceId) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        return chatService.countByUserIdAndDateRange(userId, startOfMonth.atStartOfDay(), endOfMonth.plusDays(1).atStartOfDay(), workspaceId);
    }

    private Long getKnowledgeCountByMonth(Long userId, LocalDate date, Long workspaceId) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        return knowledgeService.countByUserIdAndDateRange(userId, startOfMonth.atStartOfDay(), endOfMonth.plusDays(1).atStartOfDay(), workspaceId);
    }

    private Long getReviewCountByMonth(Long userId, LocalDate date, Long workspaceId) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        return reviewCardService.countByUserIdAndDateRange(userId, startOfMonth.atStartOfDay(), endOfMonth.plusDays(1).atStartOfDay(), workspaceId);
    }
}
