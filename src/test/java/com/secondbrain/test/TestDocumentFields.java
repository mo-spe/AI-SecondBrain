package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestDocumentFields {

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void testDocumentFields() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("=== 检查文档字段 ===");
        
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByUserId(5L);
        System.out.println("用户5的文档数: " + documents.size());
        
        int hasSummary = 0;
        int hasContent = 0;
        int hasEmbedding = 0;
        
        for (int i = 0; i < Math.min(10, documents.size()); i++) {
            KnowledgeDocument doc = documents.get(i);
            System.out.println("\n文档 " + (i+1) + ":");
            System.out.println("  ID: " + doc.getId());
            System.out.println("  标题: " + doc.getTitle());
            System.out.println("  摘要: " + (doc.getSummary() != null && !doc.getSummary().isEmpty() ? doc.getSummary() : "空"));
            System.out.println("  内容: " + (doc.getContentMd() != null && !doc.getContentMd().isEmpty() ? "有内容" : "空"));
            System.out.println("  Embedding: " + (doc.getEmbedding() != null && !doc.getEmbedding().isEmpty() ? "有" : "无"));
            
            if (doc.getSummary() != null && !doc.getSummary().isEmpty()) {
                hasSummary++;
            }
            if (doc.getContentMd() != null && !doc.getContentMd().isEmpty()) {
                hasContent++;
            }
            if (doc.getEmbedding() != null && !doc.getEmbedding().isEmpty()) {
                hasEmbedding++;
            }
        }
        
        System.out.println("\n统计:");
        System.out.println("  有摘要的文档: " + hasSummary + "/" + documents.size());
        System.out.println("  有内容的文档: " + hasContent + "/" + documents.size());
        System.out.println("  有Embedding的文档: " + hasEmbedding + "/" + documents.size());
    }
}
