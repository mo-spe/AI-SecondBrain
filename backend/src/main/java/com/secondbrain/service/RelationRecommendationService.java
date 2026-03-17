package com.secondbrain.service;

import com.secondbrain.dto.RelationRecommendation;

import java.util.List;

public interface RelationRecommendationService {

    List<RelationRecommendation> recommendRelations(Long knowledgeId, Long userId);

    List<RelationRecommendation> recommendRelations(Long knowledgeId, Long userId, int topK);
}