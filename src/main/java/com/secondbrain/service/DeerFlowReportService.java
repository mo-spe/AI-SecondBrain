package com.secondbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.entity.LearningReport;

public interface DeerFlowReportService {
    String generateLearningReport(Long userId, String topic, Integer days);
    Page<LearningReport> getReportList(Long userId, Integer current, Integer size);
    LearningReport getReportById(Long id, Long userId);
    boolean deleteReport(Long id, Long userId);
}