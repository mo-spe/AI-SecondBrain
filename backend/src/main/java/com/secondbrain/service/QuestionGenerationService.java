package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;

import java.util.List;

public interface QuestionGenerationService {

    ReviewCard generateHighQualityQuestion(KnowledgeNode node, String cardType, Long userId);

    String generateChoiceQuestion(KnowledgeNode node, Long userId);

    String generateFillQuestion(KnowledgeNode node, Long userId);

    String generateSimpleQuestion(KnowledgeNode node, Long userId);

    double evaluateQuestionQuality(String question, String answer);

    int estimateQuestionDifficulty(String question, KnowledgeNode node);

    boolean isQuestionDiverse(String newQuestion, List<String> existingQuestions);
}
