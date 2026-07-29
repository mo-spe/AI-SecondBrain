package com.secondbrain.service;

import com.secondbrain.dto.CreateResearchTaskRequest;
import com.secondbrain.dto.UpdateResearchTaskRequest;
import com.secondbrain.vo.ResearchTaskVO;

import java.util.List;

/**
 * 研究任务服务接口.
 *
 * <p>提供研究任务的 CRUD 及状态管理。任务属于某个研究项目，任务之间可以有依赖关系。</p>
 *
 * @author AI
 */
public interface ResearchTaskService {

    /**
     * 查询项目下所有任务（按 sortOrder 排序）.
     *
     * @param projectId 项目ID
     * @param userId    用户ID（权限校验）
     * @return 任务列表
     */
    List<ResearchTaskVO> listByProject(Long projectId, Long userId);

    /**
     * 查询任务详情（含依赖信息）.
     *
     * @param id        任务ID
     * @param projectId 项目ID
     * @param userId    用户ID
     * @return 任务详情
     */
    ResearchTaskVO getById(Long id, Long projectId, Long userId);

    /**
     * 创建任务.
     *
     * @param projectId 项目ID
     * @param request   创建请求
     * @param userId    用户ID
     * @return 创建后的任务
     */
    ResearchTaskVO create(Long projectId, CreateResearchTaskRequest request, Long userId);

    /**
     * 更新任务（仅更新非 null 字段）.
     *
     * @param id        任务ID
     * @param projectId 项目ID
     * @param request   更新请求
     * @param userId    用户ID
     * @return 更新后的任务
     */
    ResearchTaskVO update(Long id, Long projectId, UpdateResearchTaskRequest request, Long userId);

    /**
     * 删除任务.
     *
     * @param id        任务ID
     * @param projectId 项目ID
     * @param userId    用户ID
     */
    void delete(Long id, Long projectId, Long userId);

    /**
     * 批量创建任务（用于 Planner 生成计划后批量写入）.
     *
     * @param projectId 项目ID
     * @param requests  任务列表
     * @param userId    用户ID
     * @return 创建后的任务列表
     */
    List<ResearchTaskVO> batchCreate(Long projectId, List<CreateResearchTaskRequest> requests, Long userId);

    /**
     * 批量更新项目下所有任务的状态.
     *
     * <p>研究流程启动时将所有任务设为 RUNNING，完成时设为 COMPLETED/FAILED/SKIPPED。</p>
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @param status    目标状态
     */
    void updateStatusByProject(Long projectId, Long userId, String status);

    /**
     * 标记单个任务完成并设置结果摘要.
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @param taskId    任务ID
     * @param summary   结果摘要
     */
    void markTaskCompleted(Long projectId, Long userId, Long taskId, String summary);
}
