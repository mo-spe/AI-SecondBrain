package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;

/**
 * 重要度计算服务接口.
 * <p>根据知识点内容和使用情况计算重要程度</p>
 */
public interface ImportanceCalculationService {
    
    /**
     * 计算知识点重要程度.
     *
     * @param node 知识节点
     * @return 重要程度（1-5）
     */
    int calculateImportance(KnowledgeNode node);
    
    /**
     * 计算知识点重要程度（多维度）.
     *
     * @param manualImportance 手动设置的重要程度
     * @param contentComplexity 内容复杂度
     * @param reviewFrequency 复习频率
     * @param interactionFrequency 交互频率
     * @return 重要程度（1-5）
     */
    int calculateImportance(int manualImportance, int contentComplexity, int reviewFrequency, int interactionFrequency);
}
