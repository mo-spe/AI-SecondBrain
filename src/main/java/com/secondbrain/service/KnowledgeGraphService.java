package com.secondbrain.service;

import com.secondbrain.dto.KnowledgeGraph;
import com.secondbrain.dto.KnowledgeRelationRequest;

public interface KnowledgeGraphService {

    KnowledgeGraph getGraph(Long userId);

    void addRelation(KnowledgeRelationRequest request, Long userId);

    void deleteRelation(Long relationId, Long userId);

    void autoGenerateRelations(Long userId);
}
