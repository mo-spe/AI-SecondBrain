package com.secondbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.entity.LearningReport;

/**
 * DeerFlow报告服务接口.
 * <p>提供学习报告的生成、查询、删除等功能</p>
 */
public interface DeerFlowReportService {

    /**
     * 同步生成学习报告.
     *
     * @param userId 用户ID
     * @param topic 主题
     * @param days 天数
     * @return 报告内容
     */
    String generateLearningReport(Long userId, String topic, Integer days);

    /**
     * 异步生成学习报告.
     *
     * @param userId 用户ID
     * @param topic 主题
     * @param days 天数
     * @return 异步任务响应
     */
    AsyncTaskResponse generateLearningReportAsync(Long userId, String topic, Integer days);

    /**
     * 分页获取报告列表.
     *
     * @param userId 用户ID
     * @param current 当前页码
     * @param size 每页大小
     * @return 分页结果
     */
    Page<LearningReport> getReportList(Long userId, Integer current, Integer size);

    /**
     * 根据ID获取报告.
     *
     * @param id 报告ID
     * @param userId 用户ID
     * @return 学习报告
     */
    LearningReport getReportById(Long id, Long userId);

    /**
     * 删除报告.
     *
     * @param id 报告ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteReport(Long id, Long userId);
}
