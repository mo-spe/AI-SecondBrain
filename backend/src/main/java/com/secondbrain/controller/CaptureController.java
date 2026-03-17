package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.NoteCaptureRequest;
import com.secondbrain.service.DocumentCaptureService;
import com.secondbrain.service.NoteCaptureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/capture")
@Tag(name = "数据捕捉", description = "多渠道数据捕捉接口")
public class CaptureController {

    private static final Logger log = LoggerFactory.getLogger(CaptureController.class);

    @Autowired
    private DocumentCaptureService documentCaptureService;

    @Autowired
    private NoteCaptureService noteCaptureService;

    @PostMapping("/document")
    @Operation(summary = "文档捕捉", description = "上传文档并提取内容进行知识捕捉")
    public Result<String> captureDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            HttpServletRequest httpRequest) {
        try {
            Long currentUserId = getUserId(httpRequest, userId);
            String content = documentCaptureService.extractContent(file);
            documentCaptureService.sendToCapture(content, currentUserId, file.getOriginalFilename());
            return Result.success("文档捕捉成功");
        } catch (Exception e) {
            log.error("文档捕捉失败", e);
            return Result.error("文档捕捉失败：" + e.getMessage());
        }
    }

    @PostMapping("/note")
    @Operation(summary = "笔记捕捉", description = "提交笔记内容进行知识捕捉")
    public Result<String> captureNote(@RequestBody NoteCaptureRequest request, HttpServletRequest httpRequest) {
        log.info("========== 收到笔记捕捉请求 ==========");
        log.info("请求参数：title={}, contentLength={}, userId={}", 
            request.getTitle(), 
            request.getContent() != null ? request.getContent().length() : 0,
            request.getUserId());
        
        try {
            Long currentUserId = getUserId(httpRequest, request.getUserId());
            log.info("解析用户 ID 成功：userId={}", currentUserId);
            
            String result = noteCaptureService.captureMarkdownNote(request);
            log.info("笔记捕捉完成：result={}", result);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("笔记捕捉失败，title={}", request.getTitle(), e);
            return Result.error("笔记捕捉失败：" + e.getMessage());
        }
    }

    private Long getUserId(HttpServletRequest httpRequest, Long userId) {
        if (userId != null) {
            return userId;
        }

        Long userIdFromToken = (Long) httpRequest.getAttribute("userId");
        if (userIdFromToken != null) {
            return userIdFromToken;
        }

        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                com.secondbrain.util.JwtUtil jwtUtil = new com.secondbrain.util.JwtUtil();
                return jwtUtil.getUserIdFromToken(token.substring(7));
            } catch (Exception e) {
                log.error("从token解析userId失败", e);
            }
        }

        throw new RuntimeException("无法获取用户ID");
    }
}
