package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.entity.KnowledgeTag;
import com.secondbrain.service.KnowledgeTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识标签控制器.
 *
 * <p>提供标签的 CRUD 及节点关联操作，支撑领域排行榜的标签筛选。</p>
 */
@RestController
@RequestMapping("/tags")
@Tag(name = "知识标签", description = "知识标签分类管理")
public class KnowledgeTagController {

    private final KnowledgeTagService knowledgeTagService;

    public KnowledgeTagController(KnowledgeTagService knowledgeTagService) {
        this.knowledgeTagService = knowledgeTagService;
    }

    @GetMapping
    @Operation(summary = "获取用户的所有标签")
    public Result<List<KnowledgeTag>> listMyTags(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(knowledgeTagService.listByUser(userId));
    }

    @GetMapping("/all")
    @Operation(summary = "获取全部标签（用于排行榜领域筛选）")
    public Result<List<KnowledgeTag>> listAll() {
        return Result.success(knowledgeTagService.listAll());
    }

    @PostMapping
    @Operation(summary = "创建标签")
    public Result<KnowledgeTag> create(
            @Parameter(description = "标签名称") @RequestParam String tagName,
            @Parameter(description = "标签颜色") @RequestParam(required = false) String tagColor,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success("创建成功", knowledgeTagService.create(tagName, tagColor, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    public Result<Void> delete(@Parameter(description = "标签ID") @PathVariable Long id,
                                HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        knowledgeTagService.delete(id, userId);
        return Result.success("已删除");
    }

    @PostMapping("/node/{nodeId}/tag/{tagId}")
    @Operation(summary = "给知识节点添加标签")
    public Result<Void> addTagToNode(
            @Parameter(description = "知识节点ID") @PathVariable Long nodeId,
            @Parameter(description = "标签ID") @PathVariable Long tagId) {
        knowledgeTagService.addTagToNode(nodeId, tagId);
        return Result.success("标签已添加");
    }

    @DeleteMapping("/node/{nodeId}/tag/{tagId}")
    @Operation(summary = "移除知识节点的标签")
    public Result<Void> removeTagFromNode(
            @Parameter(description = "知识节点ID") @PathVariable Long nodeId,
            @Parameter(description = "标签ID") @PathVariable Long tagId) {
        knowledgeTagService.removeTagFromNode(nodeId, tagId);
        return Result.success("标签已移除");
    }

    @GetMapping("/node/{nodeId}")
    @Operation(summary = "获取知识节点的所有标签")
    public Result<List<KnowledgeTag>> listByNode(
            @Parameter(description = "知识节点ID") @PathVariable Long nodeId) {
        return Result.success(knowledgeTagService.listByNode(nodeId));
    }
}
