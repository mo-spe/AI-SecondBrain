package com.secondbrain.service;

import com.secondbrain.vo.ResearchPlanVO;

import java.util.List;

/**
 * 研究计划服务接口.
 *
 * <p>管理 Planner Agent 生成的研究计划及其版本。</p>
 *
 * @author AI
 */
public interface ResearchPlanService {

    /**
     * 获取项目的最新计划.
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @return 最新计划（可能为 null）
     */
    ResearchPlanVO getLatestByProject(Long projectId, Long userId);

    /**
     * 获取项目的所有计划版本.
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @return 计划列表（按版本降序）
     */
    List<ResearchPlanVO> listByProject(Long projectId, Long userId);

    /**
     * 查询计划详情.
     *
     * @param id     计划ID
     * @param userId 用户ID
     * @return 计划详情
     */
    ResearchPlanVO getById(Long id, Long userId);

    /**
     * 创建或更新计划（由 Planner Agent 调用）.
     *
     * <p>每次调用创建新版本。</p>
     *
     * @param projectId      项目ID
     * @param complexity     复杂度评估
     * @param agentChain     Agent 执行链路
     * @param tasksJson      Task 列表 JSON
     * @param rationale      Planner 推理过程
     * @param estimatedTokens 预估 token 消耗
     * @param createdBy      创建者标识
     * @param userId         用户ID
     * @return 创建后的计划
     */
    ResearchPlanVO createPlan(Long projectId, String complexity, String agentChain,
                              String tasksJson, String rationale, Integer estimatedTokens,
                              String createdBy, Long userId);
}
