package com.secondbrain.service;

import com.secondbrain.dto.KnowledgeDTO;

import java.util.List;

public interface AiService {

    List<KnowledgeDTO> extractKnowledge(String content);
    
    String generateQuestion(String prompt);
}
