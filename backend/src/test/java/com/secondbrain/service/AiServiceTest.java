package com.secondbrain.service;

import com.secondbrain.dto.KnowledgeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiServiceTest {

    @Autowired
    private AiService aiService;

    @Test
    void testExtractKnowledge() {
        String content = """
                用户：什么是Spring Boot？
                助手：Spring Boot是一个开源框架，用于简化Spring应用的初始搭建和开发过程。它提供了自动配置、起步依赖等特性，让开发者可以快速创建独立的、生产级别的Spring应用。
                """;

        List<KnowledgeDTO> knowledgeList = aiService.extractKnowledge(content);

        assertNotNull(knowledgeList, "知识点列表不应为null");
        assertFalse(knowledgeList.isEmpty(), "知识点列表不应为空");

        KnowledgeDTO knowledge = knowledgeList.get(0);
        assertNotNull(knowledge.getTitle(), "知识点标题不应为null");
        assertNotNull(knowledge.getSummary(), "知识点摘要不应为null");
        assertNotNull(knowledge.getKeywords(), "关键词列表不应为null");
    }

    @Test
    void testExtractKnowledgeWithEmptyContent() {
        String content = "";

        List<KnowledgeDTO> knowledgeList = aiService.extractKnowledge(content);

        assertNotNull(knowledgeList, "知识点列表不应为null");
    }
}
