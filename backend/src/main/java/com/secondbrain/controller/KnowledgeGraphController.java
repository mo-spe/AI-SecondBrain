package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.KnowledgeGraph;
import com.secondbrain.dto.KnowledgeRelationRequest;
import com.secondbrain.dto.RelationRecommendation;
import com.secondbrain.service.KnowledgeGraphService;
import com.secondbrain.service.RelationRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/knowledge/relation")
@Tag(name = "知识图谱", description = "知识图谱管理")
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;
    private final RelationRecommendationService relationRecommendationService;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService, 
                                  RelationRecommendationService relationRecommendationService) {
        this.knowledgeGraphService = knowledgeGraphService;
        this.relationRecommendationService = relationRecommendationService;
    }

    @GetMapping("/graph")
    @Operation(summary = "获取知识图谱", description = "获取用户的知识图谱")
    public Result<KnowledgeGraph> getGraph(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        KnowledgeGraph graph = knowledgeGraphService.getGraph(userId);
        return Result.success(graph);
    }

    @PostMapping
    @Operation(summary = "添加知识关系", description = "在两个知识节点之间添加关系")
    public Result<Void> addRelation(
            @RequestBody KnowledgeRelationRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        knowledgeGraphService.addRelation(request, userId);
        return Result.success("添加成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识关系", description = "删除知识关系")
    public Result<Void> deleteRelation(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        knowledgeGraphService.deleteRelation(id, userId);
        return Result.success("删除成功", null);
    }

    @PostMapping("/auto-generate")
    @Operation(summary = "自动生成关系", description = "基于相似度自动生成知识关系")
    public Result<Void> autoGenerateRelations(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        knowledgeGraphService.autoGenerateRelations(userId);
        return Result.success("自动生成完成", null);
    }

    @GetMapping("/recommend/{knowledgeId}")
    @Operation(summary = "推荐关系", description = "为指定知识节点推荐可能的关系")
    public Result<List<RelationRecommendation>> recommendRelations(
            @PathVariable Long knowledgeId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<RelationRecommendation> recommendations = relationRecommendationService.recommendRelations(knowledgeId, userId);
        return Result.success(recommendations);
    }
}
