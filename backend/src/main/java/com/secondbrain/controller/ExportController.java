package com.secondbrain.controller;

import com.secondbrain.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 数据导出控制器. <p>提供知识点多种格式导出接口</p> */
@RestController
@RequestMapping("/export")
@Tag(name = "数据导出", description = "数据导出相关接口")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    /**
     * 导出为Markdown.
     *
     * @param ids 知识点ID列表
     * @param httpRequest HTTP请求对象
     * @param response HTTP响应对象
     * @return void
     */
    @PostMapping("/markdown")
    @Operation(summary = "导出为Markdown", description = "将知识点导出为Markdown格式")
    public void exportToMarkdown(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Long workspaceId = getWorkspaceId(httpRequest);
        exportService.exportToMarkdown(userId, workspaceId, ids, response);
    }

    /**
     * 导出为PDF.
     *
     * @param ids 知识点ID列表
     * @param httpRequest HTTP请求对象
     * @param response HTTP响应对象
     * @return void
     */
    @PostMapping("/pdf")
    @Operation(summary = "导出为PDF", description = "将知识点导出为PDF格式")
    public void exportToPDF(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Long workspaceId = getWorkspaceId(httpRequest);
        exportService.exportToPDF(userId, workspaceId, ids, response);
    }

    /**
     * 导出为Word.
     *
     * @param ids 知识点ID列表
     * @param httpRequest HTTP请求对象
     * @param response HTTP响应对象
     * @return void
     */
    @PostMapping("/word")
    @Operation(summary = "导出为Word", description = "将知识点导出为Word格式")
    public void exportToWord(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Long workspaceId = getWorkspaceId(httpRequest);
        exportService.exportToWord(userId, workspaceId, ids, response);
    }

    /**
     * 导出为JSON.
     *
     * @param ids 知识点ID列表
     * @param httpRequest HTTP请求对象
     * @param response HTTP响应对象
     * @return void
     */
    @PostMapping("/json")
    @Operation(summary = "导出为JSON", description = "将知识点导出为JSON格式")
    public void exportToJSON(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Long workspaceId = getWorkspaceId(httpRequest);
        exportService.exportToJSON(userId, workspaceId, ids, response);
    }

    /**
     * 导出为CSV.
     *
     * @param ids 知识点ID列表
     * @param httpRequest HTTP请求对象
     * @param response HTTP响应对象
     * @return void
     */
    @PostMapping("/csv")
    @Operation(summary = "导出为CSV", description = "将知识点导出为CSV格式")
    public void exportToCSV(@RequestBody(required = false) List<Long> ids, HttpServletRequest httpRequest, HttpServletResponse response) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Long workspaceId = getWorkspaceId(httpRequest);
        exportService.exportToCSV(userId, workspaceId, ids, response);
    }
}
