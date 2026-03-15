package com.secondbrain.service;

import com.secondbrain.dto.KnowledgeDTO;

import java.util.List;

public interface AiService {

    List<KnowledgeDTO> extractKnowledge(String content);
    
    String generateQuestion(String prompt);
    
    String generateAnswer(String prompt);
    
    List<KnowledgeDTO> extractKnowledge(String content, String userApiKey);
    
    String generateQuestion(String prompt, String userApiKey);
    
    String generateAnswer(String prompt, String userApiKey);
}
