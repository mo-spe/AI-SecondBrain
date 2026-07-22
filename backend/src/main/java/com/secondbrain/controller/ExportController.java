package com.secondbrain.controller;

import com.secondbrain.service.ExportService;
import com.secondbrain.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据导出控制器
 * 提供知识点多种格式导出接口
 */
@RestController
@RequestMapping("/export")
@Tag(name = "数据导出", description = "数据导出相关接口")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    private final ExportService exportService;
    private final JwtUtil jwtUtil;

    public ExportController(ExportService exportService, JwtUtil jwtUtil) {
        this.exportService = exportService;
        this.jwtUtil = jwtUtil;
    }

    private Long getUserId(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        if (userId == null) {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    userId = jwtUtil.getUserIdFromToken(token.substring(7));
                } catch (RuntimeException e) {
                    log.error("从token解析userId失败", e);
                }
            }
        }
        
        return userId;
    }

    /**
     * 导出为Markdown
     *
     * @param ids 知识节点ID列表
     * @param httpRequest HTTP请求
     * @param response HTTP响应
     */
    @PostMapping("/markdown")
    @Operation(summary = "导出为Markdown", description = "将知识点导出为Markdown格式")
    public void exportToMarkdown(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = getUserId(httpRequest);
        
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        exportService.exportToMarkdown(userId, ids, response);
    }

    /**
     * 导出为PDF
     *
     * @param ids 知识节点ID列表
     * @param httpRequest HTTP请求
     * @param response HTTP响应
     */
    @PostMapping("/pdf")
    @Operation(summary = "导出为PDF", description = "将知识点导出为PDF格式")
    public void exportToPDF(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = getUserId(httpRequest);
        
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        exportService.exportToPDF(userId, ids, response);
    }

    /**
     * 导出为Word
     *
     * @param ids 知识节点ID列表
     * @param httpRequest HTTP请求
     * @param response HTTP响应
     */
    @PostMapping("/word")
    @Operation(summary = "导出为Word", description = "将知识点导出为Word格式")
    public void exportToWord(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = getUserId(httpRequest);
        
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        exportService.exportToWord(userId, ids, response);
    }

    /**
     * 导出为JSON
     *
     * @param ids 知识节点ID列表
     * @param httpRequest HTTP请求
     * @param response HTTP响应
     */
    @PostMapping("/json")
    @Operation(summary = "导出为JSON", description = "将知识点导出为JSON格式")
    public void exportToJSON(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = getUserId(httpRequest);
        
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        exportService.exportToJSON(userId, ids, response);
    }

    /**
     * 导出为CSV
     *
     * @param ids 知识节点ID列表
     * @param httpRequest HTTP请求
     * @param response HTTP响应
     */
    @PostMapping("/csv")
    @Operation(summary = "导出为CSV", description = "将知识点导出为CSV格式")
    public void exportToCSV(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = getUserId(httpRequest);
        
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        exportService.exportToCSV(userId, ids, response);
    }
}
