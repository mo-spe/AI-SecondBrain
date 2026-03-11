package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import com.secondbrain.service.ElasticsearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestMultiFieldSearchDebug {

    @Autowired(required = false)
    private ElasticsearchService elasticsearchService;

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void testMultiFieldSearchDebug() {
        if (elasticsearchService == null || knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch服务未配置");
            return;
        }

        System.out.println("=== 调试多字段搜索 ===");
        
        Long userId = 5L;
        String keyword = "缓存";
        
        System.out.println("\n1. 查询用户5的所有文档");
        List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findByUserId(userId);
        System.out.println("   文档总数: " + allDocs.size());
        
        System.out.println("\n2. 查询标题包含'缓存'的文档");
        List<KnowledgeDocument> titleDocs = knowledgeDocumentRepository.findByUserIdAndTitleContaining(userId, keyword);
        System.out.println("   结果数: " + titleDocs.size());
        for (int i = 0; i < Math.min(5, titleDocs.size()); i++) {
            KnowledgeDocument doc = titleDocs.get(i);
            System.out.println("   " + (i+1) + ". ID=" + doc.getId() + ", 标题='" + doc.getTitle() + "'");
        }
        
        System.out.println("\n3. 查询摘要包含'缓存'的文档");
        List<KnowledgeDocument> summaryDocs = knowledgeDocumentRepository.findByUserIdAndSummaryContaining(userId, keyword);
        System.out.println("   结果数: " + summaryDocs.size());
        for (int i = 0; i < Math.min(5, summaryDocs.size()); i++) {
            KnowledgeDocument doc = summaryDocs.get(i);
            System.out.println("   " + (i+1) + ". ID=" + doc.getId() + ", 标题='" + doc.getTitle() + 
                "', 摘要='" + (doc.getSummary() != null && doc.getSummary().length() > 30 ? 
                doc.getSummary().substring(0, 30) + "..." : doc.getSummary()) + "'");
        }
        
        System.out.println("\n4. 使用ElasticsearchService进行多字段搜索");
        List<KnowledgeDocument> multiFieldResults = elasticsearchService.multiFieldSearch(keyword, userId);
        System.out.println("   结果数: " + multiFieldResults.size());
        for (int i = 0; i < Math.min(10, multiFieldResults.size()); i++) {
            KnowledgeDocument doc = multiFieldResults.get(i);
            System.out.println("   " + (i+1) + ". ID=" + doc.getId() + ", 标题='" + doc.getTitle() + "'");
        }
        
        System.out.println("\n5. 验证结果");
        System.out.println("   标题搜索结果数: " + titleDocs.size());
        System.out.println("   摘要搜索结果数: " + summaryDocs.size());
        System.out.println("   多字段搜索结果数: " + multiFieldResults.size());
        System.out.println("   预期多字段搜索结果数: " + (titleDocs.size() + summaryDocs.size()) + " (去重前)");
    }
}
