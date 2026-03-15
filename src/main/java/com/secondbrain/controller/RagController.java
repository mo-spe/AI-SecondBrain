package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.RagRequest;
import com.secondbrain.dto.RagResponse;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.KnowledgeVectorService;
import com.secondbrain.service.RagService;
import com.secondbrain.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rag")
@Tag(name = "RAG知识问答", description = "基于知识库的智能问答")
@Slf4j
public class RagController {

    private final RagService ragService;
    private final KnowledgeVectorService knowledgeVectorService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final UserService userService;

    public RagController(RagService ragService, KnowledgeVectorService knowledgeVectorService, KnowledgeNodeMapper knowledgeNodeMapper, UserService userService) {
        this.ragService = ragService;
        this.knowledgeVectorService = knowledgeVectorService;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.userService = userService;
    }

    @PostMapping("/answer")
    @Operation(summary = "知识问答", description = "基于知识库回答用户问题")
    public Result<RagResponse> answer(
            @RequestBody RagRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        String userApiKey = null;
        try {
            com.secondbrain.entity.User user = userService.getUserById(userId);
            if (user != null) {
                userApiKey = user.getApiKey();
            }
        } catch (Exception e) {
            log.warn("获取用户API Key失败：{}", e.getMessage());
        }
        
        RagResponse response = ragService.answer(request, userId, userApiKey);
        
        return Result.success(response);
    }

    @PostMapping("/generate-vectors")
    @Operation(summary = "生成向量", description = "为用户的所有知识节点生成向量")
    public Result<String> generateVectors(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        knowledgeVectorService.batchGenerateVectors(userId);
        
        return Result.success("向量生成任务已提交", null);
    }
}
