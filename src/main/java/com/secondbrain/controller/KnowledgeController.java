package com.secondbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.common.Result;
import com.secondbrain.dto.UpdateKnowledgeRequest;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.vo.KnowledgeNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledge")
@Tag(name = "知识管理", description = "知识节点管理相关接口")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/list")
    @Operation(summary = "知识列表", description = "分页查询知识列表")
    public Result<Page<KnowledgeNodeVO>> list(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "重要程度") @RequestParam(required = false) Integer importance,
            @Parameter(description = "掌握程度") @RequestParam(required = false) Integer masteryLevel,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Page<KnowledgeNodeVO> page = knowledgeService.list(current, size, keyword, userId, importance, masteryLevel);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "知识详情", description = "根据ID查询知识详情")
    public Result<KnowledgeNodeVO> getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        KnowledgeNodeVO vo = knowledgeService.getById(id, userId);
        return Result.success(vo);
    }

    @PostMapping
    @Operation(summary = "创建知识", description = "创建新的知识点")
    public Result<KnowledgeNodeVO> create(@RequestBody UpdateKnowledgeRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        KnowledgeNodeVO vo = knowledgeService.create(request.getTitle(), request.getSummary(), request.getContentMd(), request.getImportance(), userId);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识", description = "根据ID删除知识")
    public Result<Void> deleteById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        knowledgeService.deleteById(id, userId);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/importance")
    @Operation(summary = "更新重要程度", description = "更新知识点的重要程度")
    public Result<Void> updateImportance(
            @PathVariable Long id,
            @RequestParam Integer importance,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        knowledgeService.updateImportance(id, importance, userId);
        return Result.success("更新成功", null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识点", description = "更新知识点的内容")
    public Result<Void> updateKnowledge(
            @PathVariable Long id,
            @RequestBody UpdateKnowledgeRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        knowledgeService.updateKnowledge(id, request.getTitle(), request.getSummary(), request.getContentMd(), userId);
        
        if (request.getImportance() != null) {
            knowledgeService.updateImportance(id, request.getImportance(), userId);
        }
        
        return Result.success("更新成功", null);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索知识点", description = "使用Elasticsearch搜索知识点")
    public Result<java.util.List<KnowledgeNodeVO>> search(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        java.util.List<KnowledgeNodeVO> results = knowledgeService.search(keyword, userId);
        return Result.success(results);
    }

    @GetMapping("/search/multi")
    @Operation(summary = "多字段搜索知识点", description = "使用Elasticsearch在标题、摘要、内容中搜索")
    public Result<java.util.List<KnowledgeNodeVO>> multiFieldSearch(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        java.util.List<KnowledgeNodeVO> results = knowledgeService.multiFieldSearch(keyword, userId);
        return Result.success(results);
    }

    @GetMapping("/search/semantic")
    @Operation(summary = "语义搜索知识点", description = "使用向量相似度进行语义搜索")
    public Result<java.util.List<KnowledgeNodeVO>> semanticSearch(
            @Parameter(description = "搜索文本") @RequestParam String queryText,
            @Parameter(description = "返回结果数量") @RequestParam(defaultValue = "10") Integer topK,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        java.util.List<KnowledgeNodeVO> results = knowledgeService.semanticSearch(queryText, userId, topK);
        return Result.success(results);
    }
}
