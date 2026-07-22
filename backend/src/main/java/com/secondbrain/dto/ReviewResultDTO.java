package com.secondbrain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 复习结果DTO.
 */
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

    public ReviewResultDTO(boolean correct, String correctAnswer, String explanation, String message) {
        this.correct = correct;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.message = message;
    }

    @JsonProperty("isCorrect")
    public boolean isCorrect() {
        return correct;
    }
}
