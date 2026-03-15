package com.secondbrain.service;

import com.secondbrain.dto.KnowledgeReference;

import java.util.List;

public interface VectorSearchService {

    List<KnowledgeReference> searchSimilar(String question, Long userId, int topK);

    List<KnowledgeReference> searchSimilar(String question, Long userId, int topK, String userApiKey);

    double calculateSimilarity(List<Float> vec1, List<Float> vec2);
}