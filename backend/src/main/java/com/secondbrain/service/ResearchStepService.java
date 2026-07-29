package com.secondbrain.service;

import com.secondbrain.vo.ResearchStepVO;

import java.util.List;

/**
 * 研究步骤服务接口.
 *
 * <p>提供研究执行日志的查询能力。步骤由 Orchestrator 写入，对外只读。</p>
 *
 * @author AI
 */
public interface ResearchStepService {

    /**
     * 查询任务下的所有步骤.
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 步骤列表（按序号升序）
     */
    List<ResearchStepVO> listByTask(Long taskId, Long userId);

    /**
     * 查询项目下所有步骤.
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @return 步骤列表
     */
    List<ResearchStepVO> listByProject(Long projectId, Long userId);
}
