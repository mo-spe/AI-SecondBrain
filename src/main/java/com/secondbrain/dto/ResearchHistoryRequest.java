package com.secondbrain.dto;

import java.util.List;

public class ResearchHistoryRequest {
    private String type;
    private String topic;
    private String content;
    private String currentLevel;
    private String targetLevel;
    private List<String> userKnowledge;
    private Integer knowledgeCount;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public List<String> getUserKnowledge() {
        return userKnowledge;
    }

    public void setUserKnowledge(List<String> userKnowledge) {
        this.userKnowledge = userKnowledge;
    }

    public Integer getKnowledgeCount() {
        return knowledgeCount;
    }

    public void setKnowledgeCount(Integer knowledgeCount) {
        this.knowledgeCount = knowledgeCount;
    }
}
