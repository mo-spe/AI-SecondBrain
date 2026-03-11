package com.secondbrain.test;

import com.secondbrain.service.KnowledgeService;
import com.secondbrain.vo.KnowledgeNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class SearchAPITest {

    @Autowired
    private KnowledgeService knowledgeService;

    @Test
    public void testKeywordSearch() {
        System.out.println("=== 测试关键词搜索 ===");
        try {
            List<KnowledgeNodeVO> results = knowledgeService.search("Python", 5L);
            System.out.println("搜索结果数: " + results.size());
            for (int i = 0; i < Math.min(5, results.size()); i++) {
                System.out.println("  " + (i+1) + ". " + results.get(i).getTitle());
            }
        } catch (Exception e) {
            System.out.println("搜索失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testMultiFieldSearch() {
        System.out.println("=== 测试多字段搜索 ===");
        try {
            List<KnowledgeNodeVO> results = knowledgeService.multiFieldSearch("Python", 5L);
            System.out.println("搜索结果数: " + results.size());
            for (int i = 0; i < Math.min(5, results.size()); i++) {
                System.out.println("  " + (i+1) + ". " + results.get(i).getTitle());
            }
        } catch (Exception e) {
            System.out.println("搜索失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testSemanticSearch() {
        System.out.println("=== 测试语义搜索 ===");
        try {
            List<KnowledgeNodeVO> results = knowledgeService.semanticSearch("Python的特点", 5L, 10);
            System.out.println("搜索结果数: " + results.size());
            for (int i = 0; i < Math.min(5, results.size()); i++) {
                System.out.println("  " + (i+1) + ". " + results.get(i).getTitle());
            }
        } catch (Exception e) {
            System.out.println("搜索失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testSemanticSearchWithSpace() {
        System.out.println("=== 测试语义搜索（带空格） ===");
        try {
            List<KnowledgeNodeVO> results = knowledgeService.semanticSearch("Python 的特点", 5L, 10);
            System.out.println("搜索结果数: " + results.size());
            for (int i = 0; i < Math.min(5, results.size()); i++) {
                System.out.println("  " + (i+1) + ". " + results.get(i).getTitle());
            }
        } catch (Exception e) {
            System.out.println("搜索失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
