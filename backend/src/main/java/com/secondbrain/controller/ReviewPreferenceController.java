package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.SetKnowledgeReviewReminderRequest;
import com.secondbrain.dto.UpdateReviewPreferenceRequest;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.service.ReviewPreferenceService;
import com.secondbrain.service.ReviewReminderService;
import com.secondbrain.vo.KnowledgeReviewReminderVO;
import com.secondbrain.vo.ReviewPreferenceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 个人复习节奏与指定提醒控制器。
 *
 * <p>所有接口均只使用当前登录用户身份，避免用户通过请求体篡改他人的学习偏好或提醒。</p>
 */
@RestController
@RequestMapping("/review")
@Tag(name = "复习偏好与提醒", description = "个人复习节奏及知识点指定提醒")
public class ReviewPreferenceController {

    private final ReviewPreferenceService reviewPreferenceService;
    private final ReviewReminderService reviewReminderService;

    public ReviewPreferenceController(ReviewPreferenceService reviewPreferenceService,
                                      ReviewReminderService reviewReminderService) {
        this.reviewPreferenceService = reviewPreferenceService;
        this.reviewReminderService = reviewReminderService;
    }

    /**
     * 获取当前用户的复习节奏偏好。
     *
     * @param request HTTP 请求
     * @return 当前有效偏好；未配置时返回默认节奏
     */
    @GetMapping("/preferences")
    @Operation(summary = "获取复习偏好")
    public Result<ReviewPreferenceVO> getPreference(HttpServletRequest request) {
        return Result.success(reviewPreferenceService.getPreference(currentUserId(request)));
    }

    /**
     * 更新当前用户的复习节奏偏好。
     *
     * @param preferenceRequest 偏好更新请求
     * @param request HTTP 请求
     * @return 保存后的有效偏好
     */
    @PutMapping("/preferences")
    @Operation(summary = "更新复习偏好")
    public Result<ReviewPreferenceVO> updatePreference(
            @Valid @RequestBody UpdateReviewPreferenceRequest preferenceRequest,
            HttpServletRequest request) {
        return Result.success(reviewPreferenceService.updatePreference(currentUserId(request), preferenceRequest));
    }

    /**
     * 获取当前用户待触发的指定复习提醒。
     *
     * @param request HTTP 请求
     * @return 待触发提醒列表
     */
    @GetMapping("/reminders")
    @Operation(summary = "获取指定复习提醒")
    public Result<List<KnowledgeReviewReminderVO>> listReminders(HttpServletRequest request) {
        return Result.success(reviewReminderService.listReminders(currentUserId(request)));
    }

    /**
     * 为一个知识点新增或更新指定复习提醒。
     *
     * @param nodeId 知识点ID
     * @param reminderRequest 计划提醒时间
     * @param request HTTP 请求
     * @return 保存后的提醒
     */
    @PutMapping("/reminders/nodes/{nodeId}")
    @Operation(summary = "设置知识点复习提醒")
    public Result<KnowledgeReviewReminderVO> saveReminder(
            @PathVariable Long nodeId,
            @Valid @RequestBody SetKnowledgeReviewReminderRequest reminderRequest,
            HttpServletRequest request) {
        return Result.success(reviewReminderService.saveReminder(currentUserId(request), nodeId, reminderRequest));
    }

    /**
     * 取消当前用户对一个知识点的指定复习提醒。
     *
     * @param nodeId 知识点ID
     * @param request HTTP 请求
     * @return 空响应
     */
    @DeleteMapping("/reminders/nodes/{nodeId}")
    @Operation(summary = "取消知识点复习提醒")
    public Result<Void> cancelReminder(@PathVariable Long nodeId, HttpServletRequest request) {
        reviewReminderService.cancelReminder(currentUserId(request), nodeId);
        return Result.success();
    }

    private Long currentUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
