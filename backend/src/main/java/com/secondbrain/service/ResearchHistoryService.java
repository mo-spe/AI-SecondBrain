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
     * @param request 请求信息
     * @param userId 用户ID
     * @return 研究历史
     */
    ResearchHistory save(ResearchHistoryRequest request, Long userId);

    /**
     * 分页获取研究历史列表.
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param userId 用户ID
     * @return 分页结果
     */
    IPage<ResearchHistory> getList(int current, int size, Long userId);

    /**
     * 分页获取研究历史列表（按类型）.
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param userId 用户ID
     * @param type 研究类型
     * @return 分页结果
     */
    IPage<ResearchHistory> getList(int current, int size, Long userId, String type);

    /**
     * 根据ID获取研究历史.
     *
     * @param id 记录ID
     * @param userId 用户ID
     * @return 研究历史
     */
    ResearchHistory getById(Long id, Long userId);

    /**
     * 删除研究历史.
     *
     * @param id 记录ID
     * @param userId 用户ID
     */
    void deleteById(Long id, Long userId);
}
