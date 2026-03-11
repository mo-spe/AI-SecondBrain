package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestElasticsearchQuery {

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void testAllQueries() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("=== 测试所有Elasticsearch查询 ===");
        
        System.out.println("\n1. 按用户ID查询");
        try {
            List<KnowledgeDocument> results = knowledgeDocumentRepository.findByUserId(5L);
            System.out.println("   结果数: " + results.size());
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                KnowledgeDocument doc = results.get(i);
                System.out.println("   " + (i+1) + ". ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + 
                    "', 摘要='" + doc.getSummary() + "'");
            }
        } catch (Exception e) {
            System.out.println("   失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n2. 按用户ID和标题包含查询");
        try {
            List<KnowledgeDocument> results = knowledgeDocumentRepository.findByUserIdAndTitleContaining(5L, "Python");
            System.out.println("   结果数: " + results.size());
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                KnowledgeDocument doc = results.get(i);
                System.out.println("   " + (i+1) + ". ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + "'");
            }
        } catch (Exception e) {
            System.out.println("   失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n3. 按用户ID和摘要包含查询");
        try {
            List<KnowledgeDocument> results = knowledgeDocumentRepository.findByUserIdAndSummaryContaining(5L, "Python");
            System.out.println("   结果数: " + results.size());
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                KnowledgeDocument doc = results.get(i);
                System.out.println("   " + (i+1) + ". ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + 
                    "', 摘要='" + doc.getSummary() + "'");
            }
        } catch (Exception e) {
            System.out.println("   失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
