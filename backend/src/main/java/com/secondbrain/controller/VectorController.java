package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.service.KnowledgeVectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "知识向量管理", description = "知识向量生成和管理接口")
@RestController
@RequestMapping("/vector")
public class VectorController {

    @Autowired
    private KnowledgeVectorService knowledgeVectorService;

    @PostMapping("/regenerate/{knowledgeId}")
    @Operation(summary = "重新生成向量", description = "为指定知识节点重新生成向量")
    public Result<String> regenerateVector(
            @Parameter(description = "知识节点ID") @PathVariable Long knowledgeId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            knowledgeVectorService.regenerateVector(knowledgeId);
            return Result.success("向量重新生成任务已提交");
        } catch (Exception e) {
            return Result.error("重新生成向量失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch-generate")
    @Operation(summary = "批量生成向量", description = "为用户的所有知识节点批量生成向量")
    public Result<String> batchGenerateVectors(HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            knowledgeVectorService.batchGenerateVectors(userId);
            return Result.success("批量向量生成任务已提交");
        } catch (Exception e) {
            return Result.error("批量生成向量失败：" + e.getMessage());
        }
    }
}