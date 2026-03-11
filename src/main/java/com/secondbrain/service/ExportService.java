package com.secondbrain.service;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface ExportService {

    void exportToMarkdown(Long userId, List<Long> ids, HttpServletResponse response);

    void exportToPDF(Long userId, List<Long> ids, HttpServletResponse response);

    void exportToWord(Long userId, List<Long> ids, HttpServletResponse response);

    void exportToJSON(Long userId, List<Long> ids, HttpServletResponse response);

    void exportToCSV(Long userId, List<Long> ids, HttpServletResponse response);
}
