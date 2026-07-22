package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.service.KnowledgeVectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 知识向量管理控制器
 * 提供知识向量生成和管理相关接口
 */
@Tag(name = "知识向量管理", description = "知识向量生成和管理接口")
@RestController
@RequestMapping("/vector")
public class VectorController {

    private static final Logger log = LoggerFactory.getLogger(VectorController.class);

    private final KnowledgeVectorService knowledgeVectorService;

    public VectorController(KnowledgeVectorService knowledgeVectorService) {
        this.knowledgeVectorService = knowledgeVectorService;
    }

    /**
     * 重新生成向量
     *
     * @param knowledgeId 知识节点ID
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/regenerate/{knowledgeId}")
    @Operation(summary = "重新生成向量", description = "为指定知识节点重新生成向量")
    public Result<String> regenerateVector(
            @Parameter(description = "知识节点ID") @PathVariable Long knowledgeId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        knowledgeVectorService.regenerateVector(knowledgeId);
        return Result.success("向量重新生成任务已提交");
    }

    /**
     * 批量生成向量
     *
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/batch-generate")
    @Operation(summary = "批量生成向量", description = "为用户的所有知识节点批量生成向量")
    public Result<String> batchGenerateVectors(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        knowledgeVectorService.batchGenerateVectors(userId);
        return Result.success("批量向量生成任务已提交");
    }
}
