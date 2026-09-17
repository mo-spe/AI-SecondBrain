package com.secondbrain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 更新个人复习节奏请求。
 */
@Getter
@Setter
public class UpdateReviewPreferenceRequest {

    /**
     * 复习间隔天数，服务端会校验其严格递增性。
     */
    @NotNull(message = "复习间隔不能为空")
    @Size(min = 3, max = 8, message = "复习间隔需设置为3至8个阶段")
    private List<Integer> intervalDays;

    /**
     * 是否开启指定复习提醒的邮件通知；未传时保留当前设置。
     */
    private Boolean reviewEmailEnabled;
}
