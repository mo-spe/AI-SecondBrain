package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.service.ChatService;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.service.ReviewCardService;
import com.secondbrain.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/statistics")
@Tag(name = "数据统计", description = "数据统计相关接口")
public class StatisticsController {

    private static final Logger log = LoggerFactory.getLogger(StatisticsController.class);

    private final ChatService chatService;
    private final KnowledgeService knowledgeService;
    private final ReviewCardService reviewCardService;
    private final JwtUtil jwtUtil;

    public StatisticsController(ChatService chatService, KnowledgeService knowledgeService, ReviewCardService reviewCardService, JwtUtil jwtUtil) {
        this.chatService = chatService;
        this.knowledgeService = knowledgeService;
        this.reviewCardService = reviewCardService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    @Operation(summary = "获取统计数据", description = "获取对话、知识点、复习等统计数据")
    public Result<Map<String, Object>> getStatistics(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        if (userId == null) {
            String token = httpRequest.getHeader("Authorization");
            log.info("从request获取的userId为null，尝试从token解析，Authorization header: {}", 
                token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");
            
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    userId = jwtUtil.getUserIdFromToken(token.substring(7));
                    log.info("从token解析userId成功: {}", userId);
                } catch (Exception e) {
                    log.error("从token解析userId失败", e);
                }
            }
        }
        
        log.info("获取统计数据，userId: {}, request URI: {}", userId, httpRequest.getRequestURI());
        
        if (userId == null) {
            log.error("userId为null，无法获取统计数据");
            return Result.error("用户未登录");
        }
        
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            long chatCount = chatService.countByUserId(userId);
            statistics.put("chatCount", chatCount);
            log.info("对话总数: {}", chatCount);
        } catch (Exception e) {
            log.error("获取对话总数失败", e);
            statistics.put("chatCount", 0);
        }
        
        try {
            long knowledgeCount = knowledgeService.countByUserId(userId);
            statistics.put("knowledgeCount", knowledgeCount);
            log.info("知识点总数: {}", knowledgeCount);
        } catch (Exception e) {
            log.error("获取知识点总数失败", e);
            statistics.put("knowledgeCount", 0);
        }
        
        try {
            long pendingReviewCount = reviewCardService.countPendingByUserId(userId);
            statistics.put("pendingReviewCount", pendingReviewCount);
            log.info("待复习数量: {}", pendingReviewCount);
        } catch (Exception e) {
            log.error("获取待复习数量失败", e);
            statistics.put("pendingReviewCount", 0);
        }
        
        try {
            long completedReviewCount = reviewCardService.countCompletedByUserId(userId);
            statistics.put("completedReviewCount", completedReviewCount);
            log.info("已完成复习数量: {}", completedReviewCount);
        } catch (Exception e) {
            log.error("获取已完成复习数量失败", e);
            statistics.put("completedReviewCount", 0);
        }
        
        log.info("返回统计数据: {}", statistics);
        return Result.success(statistics);
    }

    @GetMapping("/chart")
    @Operation(summary = "获取图表数据", description = "获取学习趋势图表数据")
    public Result<Map<String, Object>> getChartData(
            @Parameter(description = "时间周期：week-本周，month-本月，year-全年")
            @RequestParam(defaultValue = "week") String period,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        if (userId == null) {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    userId = jwtUtil.getUserIdFromToken(token.substring(7));
                } catch (Exception e) {
                    log.error("从token解析userId失败", e);
                }
            }
        }
        
        if (userId == null) {
            log.error("userId为null，无法获取图表数据");
            return Result.error("用户未登录");
        }
        
        log.info("获取图表数据，userId: {}, period: {}", userId, period);
        
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
                chatData.add(getChatCountByDate(userId, date));
                knowledgeData.add(getKnowledgeCountByDate(userId, date));
                reviewData.add(getReviewCountByDate(userId, date));
            }
        } else if ("month".equals(period)) {
            LocalDate startOfMonth = today.minusDays(29);
            for (int i = 0; i < 30; i++) {
                LocalDate date = startOfMonth.plusDays(i);
                labels.add(date.format(formatter));
                chatData.add(getChatCountByDate(userId, date));
                knowledgeData.add(getKnowledgeCountByDate(userId, date));
                reviewData.add(getReviewCountByDate(userId, date));
            }
        } else if ("year".equals(period)) {
            LocalDate startOfYear = today.minusDays(364);
            for (int i = 0; i < 12; i++) {
                LocalDate date = startOfYear.plusMonths(i);
                labels.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                chatData.add(getChatCountByMonth(userId, date));
                knowledgeData.add(getKnowledgeCountByMonth(userId, date));
                reviewData.add(getReviewCountByMonth(userId, date));
            }
        }
        
        chartData.put("labels", labels);
        chartData.put("chatData", chatData);
        chartData.put("knowledgeData", knowledgeData);
        chartData.put("reviewData", reviewData);
        
        log.info("返回图表数据，labels数量: {}", labels.size());
        return Result.success(chartData);
    }
    
    private Long getChatCountByDate(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return chatService.countByUserIdAndDateRange(userId, startOfDay, endOfDay);
    }
    
    private Long getKnowledgeCountByDate(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return knowledgeService.countByUserIdAndDateRange(userId, startOfDay, endOfDay);
    }
    
    private Long getReviewCountByDate(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return reviewCardService.countByUserIdAndDateRange(userId, startOfDay, endOfDay);
    }
    
    private Long getChatCountByMonth(Long userId, LocalDate date) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        return chatService.countByUserIdAndDateRange(userId, startOfMonth.atStartOfDay(), endOfMonth.plusDays(1).atStartOfDay());
    }
    
    private Long getKnowledgeCountByMonth(Long userId, LocalDate date) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        return knowledgeService.countByUserIdAndDateRange(userId, startOfMonth.atStartOfDay(), endOfMonth.plusDays(1).atStartOfDay());
    }
    
    private Long getReviewCountByMonth(Long userId, LocalDate date) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        return reviewCardService.countByUserIdAndDateRange(userId, startOfMonth.atStartOfDay(), endOfMonth.plusDays(1).atStartOfDay());
    }
}
