package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import com.secondbrain.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestSemanticSearchAccuracy {

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Test
    public void testSemanticSearchAccuracy() {
        if (knowledgeDocumentRepository == null || embeddingService == null) {
            System.out.println("Elasticsearch或Embedding服务未配置");
            return;
        }

        System.out.println("=== 测试语义搜索准确性 ===");
        
        Long userId = 5L;
        String queryText = "Python的特点";
        
        System.out.println("\n1. 生成查询向量");
        List<Float> queryEmbedding = embeddingService.generateEmbedding(queryText);
        System.out.println("   查询文本: '" + queryText + "'");
        System.out.println("   向量维度: " + queryEmbedding.size());
        
        System.out.println("\n2. 获取所有文档");
        List<KnowledgeDocument> allDocuments = knowledgeDocumentRepository.findByUserId(userId);
        System.out.println("   文档总数: " + allDocuments.size());
        
        System.out.println("\n3. 计算所有文档的相似度");
        List<KnowledgeDocument> results = allDocuments.stream()
                .filter(doc -> doc.getEmbedding() != null && !doc.getEmbedding().isEmpty())
                .sorted((doc1, doc2) -> {
                    double similarity1 = calculateCosineSimilarity(queryEmbedding, doc1.getEmbedding());
                    double similarity2 = calculateCosineSimilarity(queryEmbedding, doc2.getEmbedding());
                    return Double.compare(similarity2, similarity1);
                })
                .limit(10)
                .toList();
        
        System.out.println("\n4. Top 10 相似度结果:");
        for (int i = 0; i < results.size(); i++) {
            KnowledgeDocument doc = results.get(i);
            double similarity = calculateCosineSimilarity(queryEmbedding, doc.getEmbedding());
            System.out.println("   " + (i+1) + ". [" + String.format("%.4f", similarity) + "] " + 
                doc.getTitle() + (doc.getSummary() != null && !doc.getSummary().isEmpty() ? 
                " - " + (doc.getSummary().length() > 50 ? doc.getSummary().substring(0, 50) + "..." : doc.getSummary()) : ""));
        }
        
        System.out.println("\n5. 检查是否包含不相关的结果");
        for (int i = 0; i < results.size(); i++) {
            KnowledgeDocument doc = results.get(i);
            double similarity = calculateCosineSimilarity(queryEmbedding, doc.getEmbedding());
            String title = doc.getTitle().toLowerCase();
            String summary = doc.getSummary() != null ? doc.getSummary().toLowerCase() : "";
            
            if (!title.contains("python") && !summary.contains("python")) {
                System.out.println("   ⚠️  结果 " + (i+1) + " 不包含'Python': " + doc.getTitle() + 
                    " (相似度: " + String.format("%.4f", similarity) + ")");
            }
        }
    }

    private double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += Math.pow(vector1.get(i), 2);
            norm2 += Math.pow(vector2.get(i), 2);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
