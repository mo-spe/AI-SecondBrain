package com.secondbrain.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.common.Result;
import com.secondbrain.entity.Notification;
import com.secondbrain.service.SquareNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 通知控制器.
 *
 * <p>提供通知列表、未读计数和已读标记接口。</p>
 */
@RestController
@RequestMapping("/notification")
@Tag(name = "通知中心", description = "广场互动通知")
public class NotificationController {

    private final SquareNotificationService notificationService;

    public NotificationController(SquareNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 通知列表.
     */
    @GetMapping("/list")
    @Operation(summary = "通知列表")
    public Result<IPage<Notification>> list(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(notificationService.list(userId, current, size));
    }

    /**
     * 未读通知数.
     */
    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数")
    public Result<Map<String, Long>> unreadCount(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        long count = notificationService.getUnreadCount(userId);
        return Result.success(Map.of("unreadCount", count));
    }

    /**
     * 标记单条已读.
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "标记已读")
    public Result<Void> markAsRead(@Parameter(description = "通知ID") @PathVariable Long id,
                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        notificationService.markAsRead(id, userId);
        return Result.success(null);
    }

    /**
     * 全部标记已读.
     */
    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public Result<Void> markAllAsRead(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        notificationService.markAllAsRead(userId);
        return Result.success(null);
    }
}
