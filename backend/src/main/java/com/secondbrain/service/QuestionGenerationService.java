package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;

import java.util.List;

/**
 * 题目生成服务接口.
 * <p>基于AI为知识点生成高质量的复习题目</p>
 */
public interface QuestionGenerationService {

    /**
     * 生成高质量题目.
     *
     * @param node 知识节点
     * @param cardType 卡片类型
     * @param userId 用户ID
     * @return 复习卡片
     */
    ReviewCard generateHighQualityQuestion(KnowledgeNode node, String cardType, Long userId);

    /**
     * 生成选择题.
     *
     * @param node 知识节点
     * @param userId 用户ID
     * @return 题目JSON
     */
    String generateChoiceQuestion(KnowledgeNode node, Long userId);

    /**
     * 生成填空题.
     *
     * @param node 知识节点
     * @param userId 用户ID
     * @return 题目JSON
     */
    String generateFillQuestion(KnowledgeNode node, Long userId);

    /**
     * 生成简答题.
     *
     * @param node 知识节点
     * @param userId 用户ID
     * @return 题目JSON
     */
    String generateSimpleQuestion(KnowledgeNode node, Long userId);

    /**
     * 评估题目质量.
     *
     * @param question 题目内容
     * @param answer 答案
     * @return 质量分数
     */
    double evaluateQuestionQuality(String question, String answer);

    /**
     * 估算题目难度.
     *
     * @param question 题目内容
     * @param node 知识节点
     * @return 难度等级（1-5）
     */
    int estimateQuestionDifficulty(String question, KnowledgeNode node);

    /**
     * 判断题目是否多样化.
     *
     * @param newQuestion 新题目
     * @param existingQuestions 已有题目列表
     * @return 是否多样化
     */
    boolean isQuestionDiverse(String newQuestion, List<String> existingQuestions);
}
