package com.secondbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.CreateResearchProjectRequest;
import com.secondbrain.dto.UpdateResearchProjectRequest;
import com.secondbrain.vo.ResearchProjectVO;

/**
 * 研究项目服务接口.
 *
 * <p>提供研究项目的 CRUD 操作及生命周期管理（启动、暂停、恢复、归档）。</p>
 *
 * @author AI
 */
public interface ResearchProjectService {

    /**
     * 分页查询研究项目列表.
     *
     * @param current     当前页
     * @param size        每页大小
     * @param status      状态过滤（可选）
     * @param keyword     标题关键词搜索（可选）
     * @param userId      用户ID
     * @param workspaceId 工作空间ID（null=个人空间）
     * @return 分页结果
     */
    Page<ResearchProjectVO> list(int current, int size, String status, String keyword,
                                 Long userId, Long workspaceId);

    /**
     * 查询项目详情.
     *
     * @param id          项目ID
     * @param userId      用户ID
     * @param workspaceId 工作空间ID
     * @return 项目详情
     */
    ResearchProjectVO getById(Long id, Long userId, Long workspaceId);

    /**
     * 创建研究项目.
     *
     * @param request 创建请求
     * @param userId  用户ID
     * @return 创建后的项目
     */
    ResearchProjectVO create(CreateResearchProjectRequest request, Long userId);

    /**
     * 更新研究项目（仅 DRAFT 状态可编辑）.
     *
     * @param id          项目ID
     * @param request     更新请求（只更新非 null 字段）
     * @param userId      用户ID
     * @param workspaceId 工作空间ID
     * @return 更新后的项目
     */
    ResearchProjectVO update(Long id, UpdateResearchProjectRequest request,
                             Long userId, Long workspaceId);

    /**
     * 删除研究项目（软删除）.
     *
     * @param id          项目ID
     * @param userId      用户ID
     * @param workspaceId 工作空间ID
     */
    void delete(Long id, Long userId, Long workspaceId);

    /**
     * 启动研究执行.
     *
     * <p>状态转换：DRAFT / PAUSED / FAILED → RESEARCHING</p>
     *
     * @param id          项目ID
     * @param userId      用户ID
     * @param workspaceId 工作空间ID
     * @return 更新后的项目
     */
    ResearchProjectVO execute(Long id, Long userId, Long workspaceId);

    /**
     * 暂停研究.
     *
     * <p>状态转换：RESEARCHING / REVIEWING → PAUSED</p>
     *
     * @param id          项目ID
     * @param userId      用户ID
     * @param workspaceId 工作空间ID
     */
    void pause(Long id, Long userId, Long workspaceId);

    /**
     * 恢复研究.
     *
     * <p>状态转换：PAUSED → RESEARCHING</p>
     *
     * @param id          项目ID
     * @param userId      用户ID
     * @param workspaceId 工作空间ID
     * @return 更新后的项目
     */
    ResearchProjectVO resume(Long id, Long userId, Long workspaceId);

    /**
     * 归档研究.
     *
     * <p>状态转换：COMPLETED / FAILED → ARCHIVED</p>
     *
     * @param id          项目ID
     * @param userId      用户ID
     * @param workspaceId 工作空间ID
     */
    void archive(Long id, Long userId, Long workspaceId);
}
