package com.secondbrain.test;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.ElasticsearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class RegenerateEmbeddingsTest {

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Test
    public void regenerateAllEmbeddings() {
        System.out.println("开始重新生成所有知识点的向量...");
        
        List<KnowledgeNode> allNodes = knowledgeNodeMapper.selectList(null);
        System.out.println("找到 " + allNodes.size() + " 个知识点");
        
        int successCount = 0;
        int failCount = 0;
        
        for (KnowledgeNode node : allNodes) {
            try {
                System.out.println("正在处理知识点 " + node.getId() + ": " + node.getTitle());
                
                elasticsearchService.syncKnowledgeNode(node);
                
                successCount++;
                System.out.println("  ✅ 成功");
            } catch (Exception e) {
                failCount++;
                System.out.println("  ❌ 失败: " + e.getMessage());
            }
        }
        
        System.out.println();
        System.out.println("重新生成完成！");
        System.out.println("成功: " + successCount);
        System.out.println("失败: " + failCount);
    }

    @Test
    public void regenerateByUserId() {
        Long userId = 5L;
        System.out.println("开始重新生成用户 " + userId + " 的知识点向量...");
        
        List<KnowledgeNode> userNodes = knowledgeNodeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getUserId, userId)
        );
        
        System.out.println("找到 " + userNodes.size() + " 个知识点");
        
        int successCount = 0;
        int failCount = 0;
        
        for (KnowledgeNode node : userNodes) {
            try {
                System.out.println("正在处理知识点 " + node.getId() + ": " + node.getTitle());
                
                elasticsearchService.syncKnowledgeNode(node);
                
                successCount++;
                System.out.println("  ✅ 成功");
            } catch (Exception e) {
                failCount++;
                System.out.println("  ❌ 失败: " + e.getMessage());
            }
        }
        
        System.out.println();
        System.out.println("重新生成完成！");
        System.out.println("成功: " + successCount);
        System.out.println("失败: " + failCount);
    }
}
