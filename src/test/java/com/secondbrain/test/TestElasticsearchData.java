package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestElasticsearchData {

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void testFindByUserId() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("=== 测试按用户ID查找 ===");
        try {
            List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByUserId(5L);
            System.out.println("找到文档数: " + documents.size());
            for (int i = 0; i < Math.min(5, documents.size()); i++) {
                KnowledgeDocument doc = documents.get(i);
                System.out.println("  " + (i+1) + ". ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + 
                    "', 摘要='" + doc.getSummary() + "'");
            }
        } catch (Exception e) {
            System.out.println("查找失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testFindByUserIdAndTitleContaining() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("=== 测试按用户ID和标题查找 ===");
        try {
            List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByUserIdAndTitleContaining(5L, "Python");
            System.out.println("找到文档数: " + documents.size());
            for (int i = 0; i < Math.min(5, documents.size()); i++) {
                KnowledgeDocument doc = documents.get(i);
                System.out.println("  " + (i+1) + ". ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + "'");
            }
        } catch (Exception e) {
            System.out.println("查找失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testFindByUserIdAndSummaryContaining() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("=== 测试按用户ID和摘要查找 ===");
        try {
            List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByUserIdAndSummaryContaining(5L, "Python");
            System.out.println("找到文档数: " + documents.size());
            for (int i = 0; i < Math.min(5, documents.size()); i++) {
                KnowledgeDocument doc = documents.get(i);
                System.out.println("  " + (i+1) + ". ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + 
                    "', 摘要='" + doc.getSummary() + "'");
            }
        } catch (Exception e) {
            System.out.println("查找失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
