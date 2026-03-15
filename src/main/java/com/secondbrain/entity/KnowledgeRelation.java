package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("knowledge_relation")
public class KnowledgeRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long fromKnowledgeId;

    private Long toKnowledgeId;

    private String relationType;

    private String relationName;

    private Double weight;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFromKnowledgeId() {
        return fromKnowledgeId;
    }

    public void setFromKnowledgeId(Long fromKnowledgeId) {
        this.fromKnowledgeId = fromKnowledgeId;
    }

    public Long getToKnowledgeId() {
        return toKnowledgeId;
    }

    public void setToKnowledgeId(Long toKnowledgeId) {
        this.toKnowledgeId = toKnowledgeId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}