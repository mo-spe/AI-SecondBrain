package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestSimpleSearch {

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void testSimpleSearch() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("=== 测试简单搜索 ===");
        
        Long userId = 5L;
        
        System.out.println("\n1. 查询用户5的所有文档");
        List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findByUserId(userId);
        System.out.println("   文档总数: " + allDocs.size());
        
        System.out.println("\n2. 查询标题包含'Python'的文档");
        List<KnowledgeDocument> pythonDocs = knowledgeDocumentRepository.findByUserIdAndTitleContaining(userId, "Python");
        System.out.println("   结果数: " + pythonDocs.size());
        for (int i = 0; i < Math.min(5, pythonDocs.size()); i++) {
            KnowledgeDocument doc = pythonDocs.get(i);
            System.out.println("   " + (i+1) + ". ID=" + doc.getId() + ", 标题='" + doc.getTitle() + "'");
        }
    }
}
