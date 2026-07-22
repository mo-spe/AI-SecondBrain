package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.NoteCaptureRequest;
import com.secondbrain.service.DocumentCaptureService;
import com.secondbrain.service.NoteCaptureService;
import com.secondbrain.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 数据捕捉控制器
 *
 * 负责处理文档上传和笔记提交的数据捕捉请求，
 * 将多渠道内容提取并送入知识管理系统。
 */
@RestController
@RequestMapping("/capture")
@Tag(name = "数据捕捉", description = "多渠道数据捕捉接口")
public class CaptureController {

    private static final Logger log = LoggerFactory.getLogger(CaptureController.class);

    private final DocumentCaptureService documentCaptureService;
    private final NoteCaptureService noteCaptureService;
    private final JwtUtil jwtUtil;

    /**
     * @param documentCaptureService 文档捕捉服务
     * @param noteCaptureService     笔记捕捉服务
     * @param jwtUtil                JWT工具类
     */
    public CaptureController(DocumentCaptureService documentCaptureService,
                             NoteCaptureService noteCaptureService,
                             JwtUtil jwtUtil) {
        this.documentCaptureService = documentCaptureService;
        this.noteCaptureService = noteCaptureService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 文档捕捉接口
     *
     * @param file        上传的文档文件
     * @param userId      用户ID（可选，优先从token解析）
     * @param httpRequest HTTP请求对象
     * @return 捕捉结果
     */
    @PostMapping("/document")
    @Operation(summary = "文档捕捉", description = "上传文档并提取内容进行知识捕捉")
    public Result<String> captureDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            HttpServletRequest httpRequest) {
        Long currentUserId = getUserId(httpRequest, userId);
        String content = documentCaptureService.extractContent(file);
        documentCaptureService.sendToCapture(content, currentUserId, file.getOriginalFilename());
        return Result.success("文档捕捉成功");
    }

    /**
     * 笔记捕捉接口
     *
     * @param request     笔记内容请求
     * @param httpRequest HTTP请求对象
     * @return 捕捉结果
     */
    @PostMapping("/note")
    @Operation(summary = "笔记捕捉", description = "提交笔记内容进行知识捕捉")
    public Result<String> captureNote(@RequestBody NoteCaptureRequest request, HttpServletRequest httpRequest) {
        log.info("收到笔记捕捉请求：title={}, contentLength={}, userId={}",
                request.getTitle(),
                request.getContent() != null ? request.getContent().length() : 0,
                request.getUserId());

        Long currentUserId = getUserId(httpRequest, request.getUserId());
        String result = noteCaptureService.captureMarkdownNote(request);
        log.info("笔记捕捉完成：result={}", result);

        return Result.success(result);
    }

    /**
     * 从请求参数或JWT Token中解析用户ID
     *
     * @param httpRequest HTTP请求对象
     * @param userId      请求中传入的用户ID（可选）
     * @return 解析后的用户ID
     */
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
            return jwtUtil.getUserIdFromToken(token.substring(7));
        }

        throw new IllegalStateException("无法获取用户ID");
    }
}
