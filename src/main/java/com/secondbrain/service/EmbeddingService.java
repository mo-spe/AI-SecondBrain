package com.secondbrain.service;

import java.util.List;

public interface EmbeddingService {

    List<Float> generateEmbedding(String text);

    List<Float> generateEmbedding(String text, String model);

    List<Float> generateEmbedding(String text, String model, String userApiKey);
}
