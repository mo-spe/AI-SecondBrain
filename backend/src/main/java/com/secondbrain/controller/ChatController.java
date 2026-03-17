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

@RestController
@RequestMapping("/chat")
@Tag(name = "对话管理", description = "对话采集相关接口")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/collect")
    @Operation(summary = "采集对话", description = "采集AI对话并提取知识点")
    public Result<Void> collect(@RequestBody ChatCollectRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        chatService.collectChat(request, userId);
        return Result.success("采集成功", null);
    }

    @PostMapping("/batch-import")
    @Operation(summary = "批量导入对话", description = "批量导入AI对话并提取知识点")
    public Result<String> batchImport(@RequestBody BatchChatImportRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        int count = chatService.batchImportChats(request, userId);
        return Result.success("批量导入成功，共导入" + count + "条对话", String.valueOf(count));
    }

    @GetMapping("/list")
    @Operation(summary = "对话列表", description = "分页查询对话记录")
    public Result<Page<ChatRecordDTO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String keyword,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Page<ChatRecordDTO> page = chatService.getChatList(current, size, platform, keyword, userId);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "对话详情", description = "根据ID查询对话详情")
    public Result<ChatRecordDTO> getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        ChatRecordDTO record = chatService.getChatById(id, userId);
        return Result.success(record);
    }
}
