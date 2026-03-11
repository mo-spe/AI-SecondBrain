package com.secondbrain.test;

import com.secondbrain.service.KnowledgeService;
import com.secondbrain.vo.KnowledgeNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestMultiFieldSearch {

    @Autowired
    private KnowledgeService knowledgeService;

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
}
