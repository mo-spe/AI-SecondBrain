package com.secondbrain.dto;

public class DeerFlowResearchRequest {
    
    private String learningData;
    
    private String topic;
    
    private String goal;
    
    private String depth;
    
    private String currentLevel;
    
    private String targetLevel;
    
    private java.util.List<String> userKnowledge;

    public String getLearningData() {
        return learningData;
    }

    public void setLearningData(String learningData) {
        this.learningData = learningData;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }

    public java.util.List<String> getUserKnowledge() {
        return userKnowledge;
    }

    public void setUserKnowledge(java.util.List<String> userKnowledge) {
        this.userKnowledge = userKnowledge;
    }
}
