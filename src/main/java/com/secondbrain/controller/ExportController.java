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
                } catch (Exception e) {
                    log.error("从token解析userId失败", e);
                }
            }
        }
        
        return userId;
    }

    @PostMapping("/markdown")
    @Operation(summary = "导出为Markdown", description = "将知识点导出为Markdown格式")
    public void exportToMarkdown(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = getUserId(httpRequest);
        
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        try {
            exportService.exportToMarkdown(userId, ids, response);
        } catch (Exception e) {
            log.error("Markdown导出失败，userId: {}, ids: {}", userId, ids, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/pdf")
    @Operation(summary = "导出为PDF", description = "将知识点导出为PDF格式")
    public void exportToPDF(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = getUserId(httpRequest);
        
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        try {
            exportService.exportToPDF(userId, ids, response);
        } catch (Exception e) {
            log.error("PDF导出失败，userId: {}, ids: {}", userId, ids, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/word")
    @Operation(summary = "导出为Word", description = "将知识点导出为Word格式")
    public void exportToWord(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = getUserId(httpRequest);
        
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        try {
            exportService.exportToWord(userId, ids, response);
        } catch (Exception e) {
            log.error("Word导出失败，userId: {}, ids: {}", userId, ids, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/json")
    @Operation(summary = "导出为JSON", description = "将知识点导出为JSON格式")
    public void exportToJSON(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = getUserId(httpRequest);
        
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        try {
            exportService.exportToJSON(userId, ids, response);
        } catch (Exception e) {
            log.error("JSON导出失败，userId: {}, ids: {}", userId, ids, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/csv")
    @Operation(summary = "导出为CSV", description = "将知识点导出为CSV格式")
    public void exportToCSV(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = getUserId(httpRequest);
        
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        try {
            exportService.exportToCSV(userId, ids, response);
        } catch (Exception e) {
            log.error("CSV导出失败，userId: {}, ids: {}", userId, ids, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}