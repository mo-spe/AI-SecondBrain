package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.TagSuggestion;
import com.secondbrain.entity.KnowledgeTag;
import com.secondbrain.service.KnowledgeTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识标签控制器.
 *
 * <p>提供标签的 CRUD、层级树构建、AI建议及节点关联操作。</p>
 */
@RestController
@RequestMapping("/tags")
@Tag(name = "知识标签", description = "知识标签分类管理")
public class KnowledgeTagController {

    private final KnowledgeTagService knowledgeTagService;

    public KnowledgeTagController(KnowledgeTagService knowledgeTagService) {
        this.knowledgeTagService = knowledgeTagService;
    }

    /**
     * 从请求中读取当前工作区ID（由 JwtInterceptor 根据 JWT 中的 currentWsId 写入）.
     * <p>为 null 表示当前处于个人空间。</p>
     */
    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    @GetMapping
    @Operation(summary = "获取当前空间的标签（扁平列表）", description = "工作区内返回共享标签，个人空间返回本人标签")
    public Result<List<KnowledgeTag>> listMyTags(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(knowledgeTagService.listByUser(userId, getWorkspaceId(httpRequest)));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取当前空间的标签树", description = "工作区内返回共享标签树，个人空间返回本人标签树")
    public Result<List<KnowledgeTag>> listTree(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(knowledgeTagService.listTreeByUser(userId, getWorkspaceId(httpRequest)));
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
            @Parameter(description = "父标签ID") @RequestParam(required = false) Long parentId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success("创建成功",
                knowledgeTagService.create(tagName, tagColor, parentId, userId, getWorkspaceId(httpRequest)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标签")
    public Result<KnowledgeTag> update(
            @Parameter(description = "标签ID") @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String tagName = (String) body.get("tagName");
        String tagColor = (String) body.get("tagColor");
        Long parentId = body.get("parentId") != null ? ((Number) body.get("parentId")).longValue() : null;
        return Result.success("更新成功",
                knowledgeTagService.update(id, tagName, tagColor, parentId, userId, getWorkspaceId(httpRequest)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签", description = "工作区共享标签，有编辑权限的成员均可删除")
    public Result<Void> delete(@Parameter(description = "标签ID") @PathVariable Long id,
                                HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        knowledgeTagService.delete(id, userId, getWorkspaceId(httpRequest));
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

    @PostMapping("/ai-suggest")
    @Operation(summary = "AI建议标签")
    public Result<List<TagSuggestion>> aiSuggest(
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String title = body.get("title");
        String summary = body.getOrDefault("summary", "");
        if (title == null || title.isBlank()) {
            return Result.success(List.of());
        }
        return Result.success(knowledgeTagService.suggestTags(title, summary, userId, getWorkspaceId(httpRequest)));
    }
}
