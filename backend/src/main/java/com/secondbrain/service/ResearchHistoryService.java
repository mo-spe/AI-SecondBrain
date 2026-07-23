package com.secondbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.dto.ResearchHistoryRequest;
import com.secondbrain.entity.ResearchHistory;

/**
 * 研究历史服务接口.
 * <p>提供研究历史的保存、查询、删除等功能</p>
 */
public interface ResearchHistoryService {

    /**
     * 保存研究历史.
     *
     * @param request 研究历史请求
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 研究历史
     */
    ResearchHistory save(ResearchHistoryRequest request, Long userId, Long workspaceId);

    /**
     * 分页查询研究历史列表.
     *
     * @param current 当前页
     * @param size 每页大小
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 研究历史分页
     */
    IPage<ResearchHistory> getList(int current, int size, Long userId, Long workspaceId);

    /**
     * 分页查询研究历史列表（按类型）.
     *
     * @param current 当前页
     * @param size 每页大小
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param type 类型
     * @return 研究历史分页
     */
    IPage<ResearchHistory> getList(int current, int size, Long userId, Long workspaceId, String type);

    /**
     * 根据ID查询研究历史.
     *
     * @param id 研究历史ID
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 研究历史
     */
    ResearchHistory getById(Long id, Long userId, Long workspaceId);

    /**
     * 根据ID删除研究历史.
     *
     * @param id 研究历史ID
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    void deleteById(Long id, Long userId, Long workspaceId);
}
