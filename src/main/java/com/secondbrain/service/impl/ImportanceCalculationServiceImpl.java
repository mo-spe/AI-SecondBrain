package com.secondbrain.service.impl;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.service.ImportanceCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ImportanceCalculationServiceImpl implements ImportanceCalculationService {

    private static final Logger log = LoggerFactory.getLogger(ImportanceCalculationServiceImpl.class);

    @Override
    public int calculateImportance(KnowledgeNode node) {
        if (node == null) {
            return 3;
        }

        int manualImportance = node.getImportance() != null ? node.getImportance() : 3;
        
        int contentComplexity = calculateContentComplexity(node);
        
        int reviewFrequency = node.getReviewCount() != null ? Math.min(node.getReviewCount(), 5) : 0;
        
        int interactionFrequency = 0;
        
        int calculatedImportance = calculateImportance(manualImportance, contentComplexity, reviewFrequency, interactionFrequency);
        
        log.debug("计算重要程度，nodeId：{}，manualImportance：{}，contentComplexity：{}，reviewFrequency：{}，result：{}",
                node.getId(), manualImportance, contentComplexity, reviewFrequency, calculatedImportance);
        
        return calculatedImportance;
    }

    @Override
    public int calculateImportance(int manualImportance, int contentComplexity, int reviewFrequency, int interactionFrequency) {
        double importanceScore = 0.0;
        
        importanceScore += manualImportance * 0.4;
        
        importanceScore += contentComplexity * 0.3;
        
        importanceScore += reviewFrequency * 0.2;
        
        importanceScore += interactionFrequency * 0.1;
        
        int finalImportance = (int) Math.round(importanceScore);
        
        return Math.max(1, Math.min(5, finalImportance));
    }

    private int calculateContentComplexity(KnowledgeNode node) {
        int complexity = 3;
        
        String content = node.getContentMd() != null ? node.getContentMd() : "";
        String summary = node.getSummary() != null ? node.getSummary() : "";
        
        int contentLength = content.length();
        
        if (contentLength > 2000) {
            complexity = 5;
        } else if (contentLength > 1000) {
            complexity = 4;
        } else if (contentLength > 500) {
            complexity = 3;
        } else if (contentLength > 200) {
            complexity = 2;
        } else {
            complexity = 1;
        }
        
        int keywordCount = countKeywords(content);
        if (keywordCount > 20) {
            complexity = Math.min(complexity + 1, 5);
        } else if (keywordCount > 10) {
            complexity = Math.min(complexity + 0, 5);
        } else if (keywordCount < 5) {
            complexity = Math.max(complexity - 1, 1);
        }
        
        if (summary.length() > 200) {
            complexity = Math.min(complexity + 1, 5);
        }
        
        return complexity;
    }

    private int countKeywords(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        
        String[] keywords = {"原理", "概念", "方法", "算法", "技术", "架构", "模式", "框架", "协议", "标准",
                "定义", "特点", "优势", "缺点", "应用", "实现", "优化", "性能", "安全", "可靠"};
        
        int count = 0;
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                count++;
            }
        }
        
        return count;
    }
}
