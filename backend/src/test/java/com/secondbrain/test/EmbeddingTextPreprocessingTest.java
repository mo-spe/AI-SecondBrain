package com.secondbrain.test;

import com.secondbrain.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class EmbeddingTextPreprocessingTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    public void testTextPreprocessing() {
        String text1 = "Python 的特点";
        String text2 = "Python的特点";
        String text3 = "Python  的  特点";
        String text4 = "Python 的 特点";

        System.out.println("测试文本预处理功能：");
        System.out.println("文本1: '" + text1 + "'");
        System.out.println("文本2: '" + text2 + "'");
        System.out.println("文本3: '" + text3 + "'");
        System.out.println("文本4: '" + text4 + "'");
        System.out.println();

        List<Float> embedding1 = embeddingService.generateEmbedding(text1);
        List<Float> embedding2 = embeddingService.generateEmbedding(text2);
        List<Float> embedding3 = embeddingService.generateEmbedding(text3);
        List<Float> embedding4 = embeddingService.generateEmbedding(text4);

        System.out.println("向量生成结果：");
        System.out.println("文本1向量维度: " + embedding1.size());
        System.out.println("文本2向量维度: " + embedding2.size());
        System.out.println("文本3向量维度: " + embedding3.size());
        System.out.println("文本4向量维度: " + embedding4.size());
        System.out.println();

        double similarity12 = calculateCosineSimilarity(embedding1, embedding2);
        double similarity13 = calculateCosineSimilarity(embedding1, embedding3);
        double similarity14 = calculateCosineSimilarity(embedding1, embedding4);
        double similarity23 = calculateCosineSimilarity(embedding2, embedding3);
        double similarity24 = calculateCosineSimilarity(embedding2, embedding4);
        double similarity34 = calculateCosineSimilarity(embedding3, embedding4);

        System.out.println("向量相似度：");
        System.out.println("文本1 vs 文本2: " + String.format("%.4f", similarity12));
        System.out.println("文本1 vs 文本3: " + String.format("%.4f", similarity13));
        System.out.println("文本1 vs 文本4: " + String.format("%.4f", similarity14));
        System.out.println("文本2 vs 文本3: " + String.format("%.4f", similarity23));
        System.out.println("文本2 vs 文本4: " + String.format("%.4f", similarity24));
        System.out.println("文本3 vs 文本4: " + String.format("%.4f", similarity34));
        System.out.println();

        if (similarity12 > 0.99 && similarity13 > 0.99 && similarity14 > 0.99) {
            System.out.println("✅ 测试通过：所有文本的向量相似度都接近1.0，说明文本预处理功能正常工作");
        } else {
            System.out.println("❌ 测试失败：向量相似度不够高，文本预处理功能可能存在问题");
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
