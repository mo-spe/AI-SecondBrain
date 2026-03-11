package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestElasticsearchDataContent {

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void testElasticsearchDataContent() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("=== 检查Elasticsearch数据内容 ===");
        
        Long userId = 5L;
        
        System.out.println("\n1. 查询用户5的所有文档");
        List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findByUserId(userId);
        System.out.println("   文档总数: " + allDocs.size());
        
        System.out.println("\n2. 显示所有文档的标题和摘要");
        for (int i = 0; i < Math.min(20, allDocs.size()); i++) {
            KnowledgeDocument doc = allDocs.get(i);
            String title = doc.getTitle();
            String summary = doc.getSummary();
            System.out.println("   " + (i+1) + ". 标题: '" + title + "'");
            if (summary != null && !summary.isEmpty()) {
                System.out.println("      摘要: '" + (summary.length() > 50 ? summary.substring(0, 50) + "..." : summary) + "'");
            } else {
                System.out.println("      摘要: null");
            }
        }
        
        System.out.println("\n3. 检查包含'缓存'的文档");
        int count = 0;
        for (KnowledgeDocument doc : allDocs) {
            String title = doc.getTitle();
            String summary = doc.getSummary();
            boolean hasCache = (title != null && title.contains("缓存")) || 
                           (summary != null && summary.contains("缓存"));
            if (hasCache) {
                count++;
                System.out.println("   " + count + ". 标题: '" + title + "'");
                if (summary != null && !summary.isEmpty()) {
                    System.out.println("      摘要: '" + (summary.length() > 50 ? summary.substring(0, 50) + "..." : summary) + "'");
                }
            }
        }
        System.out.println("   总共找到 " + count + " 个包含'缓存'的文档");
    }
}
