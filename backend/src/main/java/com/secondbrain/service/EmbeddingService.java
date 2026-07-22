package com.secondbrain.service;

import java.util.List;

/**
 * Embedding向量服务接口.
 * <p>提供文本向量生成功能</p>
 */
public interface EmbeddingService {

    /**
     * 生成文本向量.
     *
     * @param text 文本内容
     * @return 向量列表
     */
    List<Float> generateEmbedding(String text);

    /**
     * 生成文本向量（指定模型）.
     *
     * @param text 文本内容
     * @param model 模型名称
     * @return 向量列表
     */
    List<Float> generateEmbedding(String text, String model);

    /**
     * 生成文本向量（指定模型和API Key）.
     *
     * @param text 文本内容
     * @param model 模型名称
     * @param userApiKey 用户API Key
     * @return 向量列表
     */
    List<Float> generateEmbedding(String text, String model, String userApiKey);
}
