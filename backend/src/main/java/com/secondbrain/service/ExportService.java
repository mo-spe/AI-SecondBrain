package com.secondbrain.service;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/** 导出服务接口. <p>提供知识点的多格式导出功能（Markdown/PDF/Word/JSON/CSV）</p> */
public interface ExportService {

    /**
     * 导出为Markdown格式.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param ids 知识点ID列表
     * @param response HTTP响应
     * @return void
     */
    void exportToMarkdown(Long userId, Long workspaceId, List<Long> ids, HttpServletResponse response);

    /**
     * 导出为PDF格式.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param ids 知识点ID列表
     * @param response HTTP响应
     * @return void
     */
    void exportToPDF(Long userId, Long workspaceId, List<Long> ids, HttpServletResponse response);

    /**
     * 导出为Word格式.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param ids 知识点ID列表
     * @param response HTTP响应
     * @return void
     */
    void exportToWord(Long userId, Long workspaceId, List<Long> ids, HttpServletResponse response);

    /**
     * 导出为JSON格式.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param ids 知识点ID列表
     * @param response HTTP响应
     * @return void
     */
    void exportToJSON(Long userId, Long workspaceId, List<Long> ids, HttpServletResponse response);

    /**
     * 导出为CSV格式.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param ids 知识点ID列表
     * @param response HTTP响应
     * @return void
     */
    void exportToCSV(Long userId, Long workspaceId, List<Long> ids, HttpServletResponse response);
}
