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

/**
 * 知识图谱控制器.
 * <p>提供知识图谱的构建、关系管理及自动生成接口</p>
 */
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

    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    /**
     * 获取知识图谱.
     *
     * @param httpRequest HTTP请求对象
     * @return 知识图谱
     */
    @GetMapping("/graph")
    @Operation(summary = "获取知识图谱", description = "获取用户的知识图谱")
    public Result<KnowledgeGraph> getGraph(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        KnowledgeGraph graph = knowledgeGraphService.getGraph(userId, workspaceId);
        return Result.success(graph);
    }

    /**
     * 添加知识关系.
     *
     * @param request 知识关系请求
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PostMapping
    @Operation(summary = "添加知识关系", description = "在两个知识节点之间添加关系")
    public Result<Void> addRelation(
            @RequestBody KnowledgeRelationRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        knowledgeGraphService.addRelation(request, userId, workspaceId);
        return Result.<Void>success("添加成功", null);
    }

    /**
     * 删除知识关系.
     *
     * @param id 关系ID
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识关系", description = "删除知识关系")
    public Result<Void> deleteRelation(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        knowledgeGraphService.deleteRelation(id, userId, workspaceId);
        return Result.<Void>success("删除成功", null);
    }

    /**
     * 自动生成关系.
     *
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PostMapping("/auto-generate")
    @Operation(summary = "自动生成关系", description = "基于相似度自动生成知识关系")
    public Result<Void> autoGenerateRelations(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        knowledgeGraphService.autoGenerateRelations(userId, workspaceId);
        return Result.<Void>success("自动生成完成", null);
    }

    /**
     * 推荐关系.
     *
     * @param knowledgeId 知识节点ID
     * @param httpRequest HTTP请求对象
     * @return 关系推荐列表
     */
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
