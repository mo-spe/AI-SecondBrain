package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class ElasticsearchDataCheckTest {

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void checkElasticsearchData() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("开始检查Elasticsearch数据...");
        
        Iterable<KnowledgeDocument> allDocuments = knowledgeDocumentRepository.findAll();
        List<KnowledgeDocument> docList = new java.util.ArrayList<>();
        allDocuments.forEach(docList::add);
        
        System.out.println("总文档数: " + docList.size());
        
        int docsWithEmbedding = 0;
        int docsWithoutEmbedding = 0;
        
        for (KnowledgeDocument doc : docList) {
            if (doc.getEmbedding() != null && !doc.getEmbedding().isEmpty()) {
                docsWithEmbedding++;
                System.out.println("✅ 文档ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + 
                    "', embedding维度=" + doc.getEmbedding().size());
            } else {
                docsWithoutEmbedding++;
                System.out.println("❌ 文档ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + 
                    "', embedding=null");
            }
        }
        
        System.out.println();
        System.out.println("统计结果:");
        System.out.println("有embedding的文档: " + docsWithEmbedding);
        System.out.println("没有embedding的文档: " + docsWithoutEmbedding);
        
        if (docsWithEmbedding == 0) {
            System.out.println();
            System.out.println("⚠️ 警告：所有文档都没有embedding数据！");
            System.out.println("请检查：");
            System.out.println("1. Embedding服务是否正常配置");
            System.out.println("2. 知识点同步时是否成功生成embedding");
            System.out.println("3. Elasticsearch索引mapping是否包含embedding字段");
        }
    }

    @Test
    public void checkUser5Data() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("开始检查用户5的数据...");
        
        Iterable<KnowledgeDocument> user5DocsIterable = knowledgeDocumentRepository.findByUserId(5L);
        List<KnowledgeDocument> user5Docs = new java.util.ArrayList<>();
        user5DocsIterable.forEach(user5Docs::add);
        
        System.out.println("用户5的文档数: " + user5Docs.size());
        
        int docsWithEmbedding = 0;
        
        for (KnowledgeDocument doc : user5Docs) {
            if (doc.getEmbedding() != null && !doc.getEmbedding().isEmpty()) {
                docsWithEmbedding++;
                System.out.println("✅ 文档ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + 
                    "', embedding维度=" + doc.getEmbedding().size());
            } else {
                System.out.println("❌ 文档ID=" + doc.getId() + 
                    ", 标题='" + doc.getTitle() + 
                    "', embedding=null");
            }
        }
        
        System.out.println();
        System.out.println("用户5有embedding的文档: " + docsWithEmbedding);
    }
}
