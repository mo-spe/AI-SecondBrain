package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;

public interface ImportanceCalculationService {
    
    int calculateImportance(KnowledgeNode node);
    
    int calculateImportance(int manualImportance, int contentComplexity, int reviewFrequency, int interactionFrequency);
}
