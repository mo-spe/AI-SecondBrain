package com.secondbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.common.Result;
import com.secondbrain.dto.BatchChatImportRequest;
import com.secondbrain.dto.ChatCollectRequest;
import com.secondbrain.dto.ChatRecordDTO;
import com.secondbrain.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** 对话管理控制器. <p>提供对话采集相关接口</p> */
@RestController
@RequestMapping("/chat")
@Tag(name = "对话管理", description = "对话采集相关接口")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    /**
     * 采集对话.
     *
     * @param request 对话采集请求
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PostMapping("/collect")
    @Operation(summary = "采集对话", description = "采集AI对话并提取知识点")
    public Result<Void> collect(@RequestBody ChatCollectRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        chatService.collectChat(request, userId, workspaceId);
        return Result.<Void>success("采集成功", null);
    }

    /**
     * 批量导入对话.
     *
     * @param request 批量导入请求
     * @param httpRequest HTTP请求对象
     * @return 导入数量
     */
    @PostMapping("/batch-import")
    @Operation(summary = "批量导入对话", description = "批量导入AI对话并提取知识点")
    public Result<String> batchImport(@RequestBody BatchChatImportRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        int count = chatService.batchImportChats(request, userId, workspaceId);
        return Result.success("批量导入成功，共导入" + count + "条对话", String.valueOf(count));
    }

    /**
     * 对话列表.
     *
     * @param current 当前页
     * @param size 每页大小
     * @param platform 平台
     * @param keyword 关键词
     * @param httpRequest HTTP请求对象
     * @return 对话分页数据
     */
    @GetMapping("/list")
    @Operation(summary = "对话列表", description = "分页查询对话记录")
    public Result<Page<ChatRecordDTO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String keyword,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        Page<ChatRecordDTO> page = chatService.getChatList(current, size, platform, keyword, userId, workspaceId);
        return Result.success(page);
    }

    /**
     * 对话详情.
     *
     * @param id 对话ID
     * @param httpRequest HTTP请求对象
     * @return 对话详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "对话详情", description = "根据ID查询对话详情")
    public Result<ChatRecordDTO> getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        ChatRecordDTO record = chatService.getChatById(id, userId, workspaceId);
        return Result.success(record);
    }
}
