package com.secondbrain.research.tool;

import com.secondbrain.entity.KnowledgeTag;
import com.secondbrain.service.KnowledgeGraphService;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.service.KnowledgeTagService;
import com.secondbrain.vo.KnowledgeNodeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识详情读取工具.
 *
 * <p>读取单个知识节点的完整内容、标签和图谱关系。</p>
 *
 * @author AI
 */
@Component
public class KnowledgeDetailTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeDetailTool.class);

    private final KnowledgeService knowledgeService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final KnowledgeTagService knowledgeTagService;

    public KnowledgeDetailTool(KnowledgeService knowledgeService,
                               KnowledgeGraphService knowledgeGraphService,
                               KnowledgeTagService knowledgeTagService) {
        this.knowledgeService = knowledgeService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.knowledgeTagService = knowledgeTagService;
    }

    @Override
    public String getName() {
        return "knowledge_detail";
    }

    @Override
    public String getDescription() {
        return "读取单个知识节点的完整内容，包括 Markdown 正文、标签和图谱关系";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "knowledgeId", Map.of("type", "integer", "description", "知识节点ID")
        ));
        schema.put("required", List.of("knowledgeId"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        Long knowledgeId = params.get("knowledgeId") instanceof Number
                ? ((Number) params.get("knowledgeId")).longValue() : null;
        if (knowledgeId == null) {
            return ToolResult.failure("INVALID_PARAM", "knowledgeId 不能为空", false);
        }

        Long userId = params.get("userId") instanceof Number
                ? ((Number) params.get("userId")).longValue() : null;
        Long workspaceId = params.get("workspaceId") instanceof Number
                ? ((Number) params.get("workspaceId")).longValue() : null;

        if (userId == null) {
            return ToolResult.failure("INVALID_PARAM", "userId 不能为空", false);
        }

        try {
            KnowledgeNodeVO node = knowledgeService.getById(knowledgeId, userId, workspaceId);
            List<KnowledgeTag> tags = knowledgeTagService.listByNode(knowledgeId);

            StringBuilder sb = new StringBuilder();
            sb.append("# ").append(node.getTitle()).append("\n\n");
            sb.append("**摘要**: ").append(node.getSummary()).append("\n\n");
            sb.append("**重要度**: ").append(node.getImportance()).append("/5\n");
            sb.append("**掌握度**: ").append(node.getMasteryLevel()).append("/5\n");
            sb.append("**复习次数**: ").append(node.getReviewCount()).append("\n");
            if (tags != null && !tags.isEmpty()) {
                sb.append("**标签**: ");
                sb.append(tags.stream().map(KnowledgeTag::getTagName).toList());
                sb.append("\n");
            }
            sb.append("\n---\n\n");
            sb.append(node.getContentMd());

            ToolResult result = ToolResult.success(sb.toString());
            result.getMetadata().put("title", node.getTitle());
            result.getMetadata().put("importance", node.getImportance());
            result.getMetadata().put("masteryLevel", node.getMasteryLevel());
            return result;
        } catch (Exception e) {
            log.error("knowledge_detail_failed id={} userId={}", knowledgeId, userId, e);
            return ToolResult.failure("READ_ERROR", e.getMessage(), false);
        }
    }
}
