package com.secondbrain.test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class SimpleElasticsearchCheckTest {

    @Autowired(required = false)
    private ElasticsearchClient elasticsearchClient;

    @Test
    public void checkKnowledgeIndex() {
        if (elasticsearchClient == null) {
            System.out.println("Elasticsearch未配置");
            return;
        }

        try {
            String indexName = "knowledge_nodes";
            System.out.println("检查索引: " + indexName);
            
            var existsResponse = elasticsearchClient.indices()
                .exists(ExistsRequest.of(e -> e.index("knowledge_nodes")));
            
            boolean exists = existsResponse.value();
            System.out.println("索引存在: " + exists);
            
            if (!exists) {
                System.out.println("索引不存在，尝试检查knowledge索引");
                existsResponse = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index("knowledge")));
                exists = existsResponse.value();
                System.out.println("knowledge索引存在: " + exists);
                
                if (!exists) {
                    System.out.println("两个索引都不存在！");
                    return;
                }
                indexName = "knowledge";
            }
            
            SearchResponse<Object> searchResponse = elasticsearchClient.search(
                s -> s.index("knowledge_nodes").query(q -> q.matchAll(m -> m)),
                Object.class
            );
            
            System.out.println("文档总数: " + searchResponse.hits().total().value());
            
            List<Hit<Object>> hits = searchResponse.hits().hits();
            System.out.println("前5个文档：");
            for (int i = 0; i < Math.min(5, hits.size()); i++) {
                Hit<Object> hit = hits.get(i);
                System.out.println("  " + (i+1) + ". ID=" + hit.id());
                if (hit.source() instanceof Map) {
                    Map<String, Object> source = (Map<String, Object>) hit.source();
                    System.out.println("     标题=" + source.get("title"));
                    System.out.println("     userId=" + source.get("userId"));
                    System.out.println("     embedding=" + (source.containsKey("embedding") ? "存在" : "不存在"));
                }
            }
            
        } catch (Exception e) {
            System.out.println("检查索引失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
