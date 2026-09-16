package com.secondbrain.service;

import com.secondbrain.entity.ResearchMemory;

import java.util.List;

/**
 * 研究记忆服务接口.
 *
 * @author AI
 */
public interface ResearchMemoryService {

    /**
     * 保存或更新记忆（按 projectId + memoryKey 去重）.
     */
    ResearchMemory save(Long projectId, Long userId, String memoryKey,
                         String memoryType, String content);

    /**
     * 查询项目下所有记忆.
     */
    List<ResearchMemory> listByProject(Long projectId, Long userId);

    /**
     * 按类型查询记忆.
     */
    List<ResearchMemory> listByType(Long projectId, Long userId, String memoryType);

    /**
     * 按 key 查询单条记忆.
     */
    ResearchMemory getByKey(Long projectId, Long userId, String memoryKey);

    /**
     * 删除记忆.
     */
    void delete(Long id, Long userId);

    /**
     * 按类型批量删除项目下的记忆.
     *
     * <p>用于研究重新执行时清理某一类型的旧记忆，避免重复堆积。</p>
     *
     * @param projectId  项目ID
     * @param userId     用户ID
     * @param memoryType 记忆类型
     */
    void deleteByProjectAndType(Long projectId, Long userId, String memoryType);
}
