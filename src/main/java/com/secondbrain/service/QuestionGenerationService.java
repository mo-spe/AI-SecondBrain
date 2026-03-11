package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;

import java.util.List;

public interface QuestionGenerationService {

    ReviewCard generateHighQualityQuestion(KnowledgeNode node, String cardType);

    String generateChoiceQuestion(KnowledgeNode node);

    String generateFillQuestion(KnowledgeNode node);

    String generateSimpleQuestion(KnowledgeNode node);

    double evaluateQuestionQuality(String question, String answer);

    int estimateQuestionDifficulty(String question, KnowledgeNode node);

    boolean isQuestionDiverse(String newQuestion, List<String> existingQuestions);
}
