package com.secondbrain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReviewResultDTO {
    private boolean correct;
    private String correctAnswer;
    private String explanation;
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

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
