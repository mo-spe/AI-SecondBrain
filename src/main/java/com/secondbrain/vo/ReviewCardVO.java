package com.secondbrain.vo;

public class ReviewCardVO {

    private Long id;

    private Long nodeId;

    private String nodeTitle;

    private String nodeSummary;

    private String question;

    private String answer;

    private String cardType;

    private Integer difficulty;

    private Integer reviewCount;

    private Integer correctCount;

    private Integer incorrectCount;

    private Double averageAccuracy;

    private Integer masteryLevel;

    private Double memoryStrength;

    private String lastReviewTime;

    private String nextReviewTime;

    private Integer status;

    private String aiGenerated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeTitle() {
        return nodeTitle;
    }

    public void setNodeTitle(String nodeTitle) {
        this.nodeTitle = nodeTitle;
    }

    public String getNodeSummary() {
        return nodeSummary;
    }

    public void setNodeSummary(String nodeSummary) {
        this.nodeSummary = nodeSummary;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getIncorrectCount() {
        return incorrectCount;
    }

    public void setIncorrectCount(Integer incorrectCount) {
        this.incorrectCount = incorrectCount;
    }

    public Double getAverageAccuracy() {
        return averageAccuracy;
    }

    public void setAverageAccuracy(Double averageAccuracy) {
        this.averageAccuracy = averageAccuracy;
    }

    public Integer getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(Integer masteryLevel) {
        this.masteryLevel = masteryLevel;
    }

    public Double getMemoryStrength() {
        return memoryStrength;
    }

    public void setMemoryStrength(Double memoryStrength) {
        this.memoryStrength = memoryStrength;
    }

    public String getLastReviewTime() {
        return lastReviewTime;
    }

    public void setLastReviewTime(String lastReviewTime) {
        this.lastReviewTime = lastReviewTime;
    }

    public String getNextReviewTime() {
        return nextReviewTime;
    }

    public void setNextReviewTime(String nextReviewTime) {
        this.nextReviewTime = nextReviewTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(String aiGenerated) {
        this.aiGenerated = aiGenerated;
    }
}
