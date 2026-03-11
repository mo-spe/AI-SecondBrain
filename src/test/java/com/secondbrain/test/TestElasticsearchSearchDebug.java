package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestElasticsearchSearchDebug {

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void testSearchDebug() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("=== 调试Elasticsearch搜索 ===");
        
        Long userId = 5L;
        
        System.out.println("\n1. 查询用户5的所有文档");
        List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findByUserId(userId);
        System.out.println("   文档总数: " + allDocs.size());
        
        System.out.println("\n2. 查询标题包含'Python'的文档");
        List<KnowledgeDocument> titleDocs = knowledgeDocumentRepository.findByUserIdAndTitleContaining(userId, "Python");
        System.out.println("   结果数: " + titleDocs.size());
        for (int i = 0; i < Math.min(5, titleDocs.size()); i++) {
            KnowledgeDocument doc = titleDocs.get(i);
            System.out.println("   " + (i+1) + ". ID=" + doc.getId() + ", 标题='" + doc.getTitle() + "'");
        }
        
        System.out.println("\n3. 查询标题包含'Python 的特点'的文档");
        List<KnowledgeDocument> titleDocs2 = knowledgeDocumentRepository.findByUserIdAndTitleContaining(userId, "Python 的特点");
        System.out.println("   结果数: " + titleDocs2.size());
        for (int i = 0; i < Math.min(5, titleDocs2.size()); i++) {
            KnowledgeDocument doc = titleDocs2.get(i);
            System.out.println("   " + (i+1) + ". ID=" + doc.getId() + ", 标题='" + doc.getTitle() + "'");
        }
        
        System.out.println("\n4. 查询标题包含'Python的特点'的文档");
        List<KnowledgeDocument> titleDocs3 = knowledgeDocumentRepository.findByUserIdAndTitleContaining(userId, "Python的特点");
        System.out.println("   结果数: " + titleDocs3.size());
        for (int i = 0; i < Math.min(5, titleDocs3.size()); i++) {
            KnowledgeDocument doc = titleDocs3.get(i);
            System.out.println("   " + (i+1) + ". ID=" + doc.getId() + ", 标题='" + doc.getTitle() + "'");
        }
        
        System.out.println("\n5. 查询摘要包含'Python'的文档");
        List<KnowledgeDocument> summaryDocs = knowledgeDocumentRepository.findByUserIdAndSummaryContaining(userId, "Python");
        System.out.println("   结果数: " + summaryDocs.size());
        for (int i = 0; i < Math.min(5, summaryDocs.size()); i++) {
            KnowledgeDocument doc = summaryDocs.get(i);
            System.out.println("   " + (i+1) + ". ID=" + doc.getId() + ", 标题='" + doc.getTitle() + "', 摘要='" + doc.getSummary() + "'");
        }
    }
}
