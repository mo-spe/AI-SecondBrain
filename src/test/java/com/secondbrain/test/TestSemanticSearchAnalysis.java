package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import com.secondbrain.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestSemanticSearchAnalysis {

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void testSemanticSearchAnalysis() {
        if (embeddingService == null || knowledgeDocumentRepository == null) {
            System.out.println("Elasticsearch或Embedding服务未配置");
            return;
        }

        System.out.println("=== 语义搜索分析 ===");
        
        Long userId = 5L;
        
        System.out.println("\n1. 测试查询：'特点'");
        String query1 = "特点";
        List<Float> embedding1 = embeddingService.generateEmbedding(query1);
        System.out.println("   查询文本: '" + query1 + "'");
        System.out.println("   向量维度: " + embedding1.size());
        
        System.out.println("\n2. 测试查询：'Python的特点'");
        String query2 = "Python的特点";
        List<Float> embedding2 = embeddingService.generateEmbedding(query2);
        System.out.println("   查询文本: '" + query2 + "'");
        System.out.println("   向量维度: " + embedding2.size());
        
        System.out.println("\n3. 获取用户5的所有文档");
        List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findByUserId(userId);
        System.out.println("   文档总数: " + allDocs.size());
        
        System.out.println("\n4. 使用'特点'进行语义搜索");
        List<KnowledgeDocument> results1 = allDocs.stream()
                .filter(doc -> doc.getEmbedding() != null && !doc.getEmbedding().isEmpty())
                .sorted((doc1, doc2) -> {
                    double similarity1 = calculateCosineSimilarity(embedding1, doc1.getEmbedding());
                    double similarity2 = calculateCosineSimilarity(embedding1, doc2.getEmbedding());
                    return Double.compare(similarity2, similarity1);
                })
                .filter(doc -> calculateCosineSimilarity(embedding1, doc.getEmbedding()) >= 0.4)
                .limit(10)
                .toList();
        
        System.out.println("   结果数: " + results1.size());
        for (int i = 0; i < results1.size(); i++) {
            KnowledgeDocument doc = results1.get(i);
            double similarity = calculateCosineSimilarity(embedding1, doc.getEmbedding());
            System.out.println("   " + (i+1) + ". [" + String.format("%.4f", similarity) + "] " + doc.getTitle());
        }
        
        System.out.println("\n5. 使用'Python的特点'进行语义搜索");
        List<KnowledgeDocument> results2 = allDocs.stream()
                .filter(doc -> doc.getEmbedding() != null && !doc.getEmbedding().isEmpty())
                .sorted((doc1, doc2) -> {
                    double similarity1 = calculateCosineSimilarity(embedding2, doc1.getEmbedding());
                    double similarity2 = calculateCosineSimilarity(embedding2, doc2.getEmbedding());
                    return Double.compare(similarity2, similarity1);
                })
                .filter(doc -> calculateCosineSimilarity(embedding2, doc.getEmbedding()) >= 0.4)
                .limit(10)
                .toList();
        
        System.out.println("   结果数: " + results2.size());
        for (int i = 0; i < results2.size(); i++) {
            KnowledgeDocument doc = results2.get(i);
            double similarity = calculateCosineSimilarity(embedding2, doc.getEmbedding());
            System.out.println("   " + (i+1) + ". [" + String.format("%.4f", similarity) + "] " + doc.getTitle());
        }
        
        System.out.println("\n6. 分析'Python的特点'的相似度");
        for (KnowledgeDocument doc : allDocs) {
            if (doc.getTitle() != null && doc.getTitle().contains("Python的特点")) {
                double similarity1 = calculateCosineSimilarity(embedding1, doc.getEmbedding());
                double similarity2 = calculateCosineSimilarity(embedding2, doc.getEmbedding());
                System.out.println("   文档: " + doc.getTitle());
                System.out.println("   与'特点'的相似度: " + String.format("%.4f", similarity1));
                System.out.println("   与'Python的特点'的相似度: " + String.format("%.4f", similarity2));
                System.out.println("   是否超过阈值(0.4): " + (similarity1 >= 0.4));
                break;
            }
        }
        
        System.out.println("\n7. 分析其他Python相关文档的相似度");
        for (KnowledgeDocument doc : allDocs) {
            if (doc.getTitle() != null && doc.getTitle().contains("Python")) {
                double similarity1 = calculateCosineSimilarity(embedding1, doc.getEmbedding());
                double similarity2 = calculateCosineSimilarity(embedding2, doc.getEmbedding());
                System.out.println("   " + doc.getTitle());
                System.out.println("     与'特点'的相似度: " + String.format("%.4f", similarity1));
                System.out.println("     与'Python的特点'的相似度: " + String.format("%.4f", similarity2));
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
