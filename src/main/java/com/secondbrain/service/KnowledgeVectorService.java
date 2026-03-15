package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;

public interface KnowledgeVectorService {

    void generateAndSaveVector(KnowledgeNode node);

    void batchGenerateVectors(Long userId);

    void regenerateVector(Long knowledgeId);
}