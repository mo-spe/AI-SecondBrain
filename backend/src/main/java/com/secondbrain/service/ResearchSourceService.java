package com.secondbrain.service;

import com.secondbrain.entity.ResearchSource;
import com.secondbrain.vo.ResearchSourceVO;

import java.util.List;

/**
 * 研究来源服务接口.
 *
 * <p>管理 Research Agent 从外部获取的信息源。
 * 来源由 Agent 自动创建，用户可查看和删除。</p>
 *
 * @author AI
 */
public interface ResearchSourceService {

    /**
     * 查询项目下所有来源（不含完整内容）.
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @return 来源列表
     */
    List<ResearchSourceVO> listByProject(Long projectId, Long userId);

    /**
     * 查询来源详情（含完整内容）.
     *
     * @param id     来源ID
     * @param userId 用户ID
     * @return 来源详情
     */
    ResearchSourceVO getById(Long id, Long userId);

    /**
     * 创建来源（由 Agent 调用）.
     *
     * @param source 来源实体
     * @param userId 用户ID
     * @return 创建后的来源
     */
    ResearchSourceVO create(ResearchSource source, Long userId);

    /**
     * 删除来源.
     *
     * @param id     来源ID
     * @param userId 用户ID
     */
    void delete(Long id, Long userId);
}
