package com.secondbrain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** 复习结果DTO. <p>用于接收复习答题结果</p> */
@Getter
@Setter
public class ReviewResultDTO {

    /**
     * 是否正确
     */
    private boolean correct;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 解析说明
     */
    private String explanation;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 本次提交后计算出的下次复习时间
     */
    private java.time.LocalDateTime nextReviewTime;

    /**
     * 本次提交后的掌握程度
     */
    private Integer masteryLevel;

    public ReviewResultDTO(boolean correct, String correctAnswer, String explanation, String message) {
        this(correct, correctAnswer, explanation, message, null, null);
    }

    /**
     * 创建包含下一次排期和掌握度的复习结果。
     *
     * @param correct 是否答对
     * @param correctAnswer 正确答案
     * @param explanation 解析说明
     * @param message 提示消息
     * @param nextReviewTime 下次复习时间
     * @param masteryLevel 掌握程度
     */
    public ReviewResultDTO(boolean correct, String correctAnswer, String explanation, String message,
                           java.time.LocalDateTime nextReviewTime, Integer masteryLevel) {
        this.correct = correct;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.message = message;
        this.nextReviewTime = nextReviewTime;
        this.masteryLevel = masteryLevel;
    }

    @JsonProperty("isCorrect")
    public boolean isCorrect() {
        return correct;
    }
}
