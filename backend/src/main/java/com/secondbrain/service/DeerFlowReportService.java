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
     * 生成学习报告.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param topic 主题
     * @param days 天数
     * @return 报告内容
     */
    String generateLearningReport(Long userId, Long workspaceId, String topic, Integer days);

    /**
     * 异步生成学习报告.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param topic 主题
     * @param days 天数
     * @return 异步任务响应
     */
    AsyncTaskResponse generateLearningReportAsync(Long userId, Long workspaceId, String topic, Integer days);

    /**
     * 分页查询报告列表.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param current 当前页
     * @param size 每页大小
     * @return 报告分页
     */
    Page<LearningReport> getReportList(Long userId, Long workspaceId, Integer current, Integer size);

    /**
     * 根据ID查询报告.
     *
     * @param id 报告ID
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 报告
     */
    LearningReport getReportById(Long id, Long userId, Long workspaceId);

    /**
     * 删除报告.
     *
     * @param id 报告ID
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 是否删除成功
     */
    boolean deleteReport(Long id, Long userId, Long workspaceId);
}
