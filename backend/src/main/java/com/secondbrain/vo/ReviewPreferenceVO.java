package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 个人复习偏好视图对象。
 */
@Getter
@Setter
public class ReviewPreferenceVO {

    /**
     * 当前有效的复习间隔天数。
     */
    private List<Integer> intervalDays;

    /**
     * 指定提醒是否会同时发送邮件。
     */
    private Boolean reviewEmailEnabled;

    /**
     * 是否正在使用系统默认节奏。
     */
    private Boolean usingDefaultIntervals;
}
