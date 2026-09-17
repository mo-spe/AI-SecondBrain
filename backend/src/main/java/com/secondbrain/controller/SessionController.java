package com.secondbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.common.Result;
import com.secondbrain.dto.RenameChatSessionRequest;
import com.secondbrain.entity.ChatMessage;
import com.secondbrain.entity.ChatSession;
import com.secondbrain.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会话管理控制器.
 * <p>提供RAG问答的会话和消息管理接口</p>
 */
@RestController
@RequestMapping("/sessions")
@Tag(name = "会话管理", description = "RAG问答会话管理")
public class SessionController {

    private final ChatSessionService chatSessionService;

    public SessionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    /**
     * 获取用户的会话列表.
     */
    @GetMapping
    @Operation(summary = "会话列表", description = "获取当前用户的RAG对话会话列表")
    public Result<Page<ChatSession>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long workspaceId = getWorkspaceId(request);
        Page<ChatSession> page = chatSessionService.getSessionList(userId, workspaceId, current, size);
        return Result.success(page);
    }

    /**
     * 创建新会话.
     */
    @PostMapping
    @Operation(summary = "创建会话", description = "创建新的RAG对话会话")
    public Result<ChatSession> create(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long workspaceId = getWorkspaceId(request);
        String title = body.getOrDefault("title", "新对话");
        ChatSession session = chatSessionService.createSession(userId, workspaceId, title);
        return Result.success(session);
    }

    /**
     * 修改指定会话的标题。
     *
     * @param id      会话ID
     * @param body    重命名请求
     * @param request HTTP请求
     * @return 更新后的会话
     */
    @PutMapping("/{id}/title")
    @Operation(summary = "重命名会话", description = "修改当前用户可访问的RAG对话会话标题")
    public Result<ChatSession> rename(@PathVariable Long id,
                                      @Valid @RequestBody RenameChatSessionRequest body,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long workspaceId = getWorkspaceId(request);
        return Result.success(chatSessionService.renameSession(id, userId, workspaceId, body.getTitle()));
    }

    /**
     * 删除会话.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话", description = "删除指定的RAG对话会话")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long workspaceId = getWorkspaceId(request);
        chatSessionService.deleteSession(id, userId, workspaceId);
        return Result.success();
    }

    /**
     * 获取会话的消息列表.
     */
    @GetMapping("/{id}/messages")
    @Operation(summary = "消息列表", description = "获取指定会话的消息记录")
    public Result<Page<ChatMessage>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "100") Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Page<ChatMessage> page = chatSessionService.getMessageList(id, userId, current, size);
        return Result.success(page);
    }
}
