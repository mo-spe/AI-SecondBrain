package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.RagRequest;
import com.secondbrain.dto.RagResponse;
import com.secondbrain.dto.StreamEvent;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.KnowledgeVectorService;
import com.secondbrain.service.RagService;
import com.secondbrain.service.impl.RagStreamingServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** RAG知识问答控制器. <p>提供基于知识库的智能问答接口</p> */
@RestController
@RequestMapping("/rag")
@Tag(name = "RAG知识问答", description = "基于知识库的智能问答")
@Slf4j
public class RagController {

    private final RagService ragService;
    private final KnowledgeVectorService knowledgeVectorService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final RagStreamingServiceImpl ragStreamingService;

    public RagController(RagService ragService, KnowledgeVectorService knowledgeVectorService,
                         KnowledgeNodeMapper knowledgeNodeMapper,
                         RagStreamingServiceImpl ragStreamingService) {
        this.ragService = ragService;
        this.knowledgeVectorService = knowledgeVectorService;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.ragStreamingService = ragStreamingService;
    }

    /**
     * 知识问答
     *
     * @param request 问答请求
     * @param httpRequest HTTP请求
     * @return 问答响应结果
     */
    @PostMapping("/answer")
    @Operation(summary = "知识问答", description = "基于知识库回答用户问题")
    public Result<RagResponse> answer(
            @RequestBody RagRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        RagResponse response = ragService.answer(request, userId);
        
        return Result.success(response);
    }

    /**
     * 流式知识问答（SSE）
     *
     * @param request     问答请求
     * @param httpRequest HTTP请求
     * @return SSE流式响应
     */
    @PostMapping(value = "/answer/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式知识问答", description = "基于知识库流式回答用户问题（SSE逐字输出）")
    public SseEmitter streamAnswer(
            @RequestBody RagRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = (Long) httpRequest.getAttribute("workspaceId");

        SseEmitter emitter = new SseEmitter(600_000L);

        CompletableFuture.runAsync(() -> {
            try {
                ragStreamingService.streamAnswer(request, userId, workspaceId, event -> {
                    try {
                        switch (event.getType()) {
                            case TOKEN:
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(event.getData()));
                                break;
                            case REFERENCES:
                                emitter.send(SseEmitter.event()
                                        .name("references")
                                        .data(event.getData()));
                                break;
                            case METRICS:
                                emitter.send(SseEmitter.event()
                                        .name("metrics")
                                        .data(event.getData()));
                                break;
                            case DONE:
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data("completed"));
                                emitter.complete();
                                break;
                            case ERROR:
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(event.getData()));
                                emitter.complete();
                                break;
                        }
                    } catch (Exception e) {
                        log.error("sse_send_failed", e);
                        emitter.completeWithError(e);
                    }
                });
            } catch (Exception e) {
                log.error("stream_answer_failed", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(e.getMessage() != null ? e.getMessage() : "未知错误"));
                } catch (Exception ignored) {
                    // emitter already completed
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    /**
     * 生成向量
     *
     * @param httpRequest HTTP请求
     * @return 向量生成任务提交结果
     */
    @PostMapping("/generate-vectors")
    @Operation(summary = "生成向量", description = "为用户的所有知识节点生成向量")
    public Result<String> generateVectors(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        knowledgeVectorService.batchGenerateVectors(userId);
        
        return Result.success("向量生成任务已提交", null);
    }
}
