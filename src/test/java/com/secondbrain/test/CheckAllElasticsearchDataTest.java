package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class CheckAllElasticsearchDataTest {

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void checkAllData() {
        if (knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        System.out.println("开始检查所有Elasticsearch数据...");
        
        Iterable<KnowledgeDocument> allDocuments = knowledgeDocumentRepository.findAll();
        List<KnowledgeDocument> docList = new ArrayList<>();
        allDocuments.forEach(docList::add);
        
        System.out.println("总文档数: " + docList.size());
        
        if (docList.isEmpty()) {
            System.out.println("⚠️ Elasticsearch中没有数据！");
            System.out.println("可能的原因：");
            System.out.println("1. 索引名称不匹配");
            System.out.println("2. 知识点没有同步到Elasticsearch");
            System.out.println("3. Elasticsearch连接失败");
            return;
        }
        
        System.out.println("\n文档列表：");
        for (KnowledgeDocument doc : docList) {
            System.out.println("  ID=" + doc.getId() + 
                ", userId=" + doc.getUserId() + 
                ", 标题='" + doc.getTitle() + "'" +
                ", embedding=" + (doc.getEmbedding() != null ? doc.getEmbedding().size() + "维" : "null"));
        }
    }
}
