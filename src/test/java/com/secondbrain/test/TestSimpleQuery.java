package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestSimpleQuery {

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void testSimpleQuery() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("=== 测试简单查询 ===");
        
        try {
            List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findByUserId(5L);
            System.out.println("用户5的文档总数: " + allDocs.size());
            
            if (allDocs.isEmpty()) {
                System.out.println("没有文档！");
                return;
            }
            
            System.out.println("\n前3个文档:");
            for (int i = 0; i < Math.min(3, allDocs.size()); i++) {
                KnowledgeDocument doc = allDocs.get(i);
                System.out.println("  " + (i+1) + ". ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + "'");
            }
            
            List<KnowledgeDocument> pythonDocs = knowledgeDocumentRepository.findByUserIdAndTitleContaining(5L, "Python");
            System.out.println("\n包含'Python'的文档数: " + pythonDocs.size());
            for (int i = 0; i < Math.min(3, pythonDocs.size()); i++) {
                KnowledgeDocument doc = pythonDocs.get(i);
                System.out.println("  " + (i+1) + ". ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + "'");
            }
            
        } catch (Exception e) {
            System.out.println("查询失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
