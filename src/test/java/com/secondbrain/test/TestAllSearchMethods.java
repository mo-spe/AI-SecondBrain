package com.secondbrain.test;

import com.secondbrain.service.KnowledgeService;
import com.secondbrain.vo.KnowledgeNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestAllSearchMethods {

    @Autowired
    private KnowledgeService knowledgeService;

    @Test
    public void testAllSearchMethods() {
        System.out.println("=== 测试所有搜索方法 ===");
        
        Long userId = 5L;
        String keyword = "Python";
        
        System.out.println("\n1. 关键词搜索");
        try {
            List<KnowledgeNodeVO> results = knowledgeService.search(keyword, userId);
            System.out.println("   结果数: " + results.size());
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                System.out.println("   " + (i+1) + ". " + results.get(i).getTitle());
            }
        } catch (Exception e) {
            System.out.println("   失败: " + e.getMessage());
        }
        
        System.out.println("\n2. 多字段搜索");
        try {
            List<KnowledgeNodeVO> results = knowledgeService.multiFieldSearch(keyword, userId);
            System.out.println("   结果数: " + results.size());
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                System.out.println("   " + (i+1) + ". " + results.get(i).getTitle());
            }
        } catch (Exception e) {
            System.out.println("   失败: " + e.getMessage());
        }
        
        System.out.println("\n3. 语义搜索");
        try {
            List<KnowledgeNodeVO> results = knowledgeService.semanticSearch(keyword, userId, 10);
            System.out.println("   结果数: " + results.size());
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                System.out.println("   " + (i+1) + ". " + results.get(i).getTitle());
            }
        } catch (Exception e) {
            System.out.println("   失败: " + e.getMessage());
        }
    }
}
