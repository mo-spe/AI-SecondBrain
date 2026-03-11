package com.secondbrain.test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.ElasticsearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class FixElasticsearchDataTest {

    @Autowired(required = false)
    private ElasticsearchClient elasticsearchClient;

    @Autowired(required = false)
    private ElasticsearchService elasticsearchService;

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Test
    public void deleteIndexAndResync() {
        if (elasticsearchClient == null || elasticsearchService == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        try {
            System.out.println("开始修复Elasticsearch数据...");
            
            String indexName = "knowledge_nodes";
            System.out.println("删除索引: " + indexName);
            
            elasticsearchClient.indices()
                .delete(DeleteIndexRequest.of(d -> d.index(indexName)));
            
            System.out.println("索引删除成功");
            
            Thread.sleep(2000);
            
            System.out.println("开始重新同步所有知识点...");
            
            List<KnowledgeNode> allNodes = knowledgeNodeMapper.selectList(null);
            System.out.println("找到 " + allNodes.size() + " 个知识点");
            
            int successCount = 0;
            int failCount = 0;
            
            for (KnowledgeNode node : allNodes) {
                try {
                    System.out.println("正在同步知识点 " + node.getId() + ": " + node.getTitle());
                    
                    elasticsearchService.syncKnowledgeNode(node);
                    
                    successCount++;
                    System.out.println("  ✅ 成功");
                } catch (Exception e) {
                    failCount++;
                    System.out.println("  ❌ 失败: " + e.getMessage());
                }
            }
            
            System.out.println();
            System.out.println("重新同步完成！");
            System.out.println("成功: " + successCount);
            System.out.println("失败: " + failCount);
            
        } catch (Exception e) {
            System.out.println("修复失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
