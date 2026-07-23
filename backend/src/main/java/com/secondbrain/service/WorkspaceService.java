package com.secondbrain.service;

import com.secondbrain.dto.MemberResponse;
import com.secondbrain.dto.WorkspaceResponse;
import com.secondbrain.entity.Workspace;
import com.secondbrain.entity.WorkspaceMember;

import java.util.List;

/**
 * 工作区服务接口.
 * <p>提供工作区的创建、查询、更新、删除及成员管理功能</p>
 */
public interface WorkspaceService {

    /**
     * 创建工作区.
     *
     * @param name 工作区名称
     * @param description 工作区描述
     * @param userId 创建者用户ID
     * @return 创建的工作区信息
     */
    WorkspaceResponse create(String name, String description, Long userId);

    /**
     * 查询用户所属的工作区列表.
     *
     * @param userId 用户ID
     * @return 工作区列表
     */
    List<WorkspaceResponse> listByUser(Long userId);

    /**
     * 查询工作区详情.
     *
     * @param workspaceId 工作区ID
     * @param userId 请求用户ID
     * @return 工作区信息
     */
    WorkspaceResponse getById(Long workspaceId, Long userId);

    /**
     * 更新工作区信息.
     *
     * @param workspaceId 工作区ID
     * @param name 新名称
     * @param description 新描述
     * @param userId 请求用户ID
     */
    void update(Long workspaceId, String name, String description, Long userId);

    /**
     * 删除工作区（软删除）.
     *
     * @param workspaceId 工作区ID
     * @param userId 请求用户ID（需为Owner）
     */
    void delete(Long workspaceId, Long userId);

    /**
     * 转让工作区所有权.
     *
     * @param workspaceId 工作区ID
     * @param newOwnerUserId 新所有者用户ID
     * @param currentUserId 当前所有者用户ID
     */
    void transferOwnership(Long workspaceId, Long newOwnerUserId, Long currentUserId);

    /**
     * 添加成员到工作区.
     *
     * @param workspaceId 工作区ID
     * @param targetUserId 被邀请的用户ID
     * @param role 角色
     * @param operatorUserId 操作者用户ID
     */
    void addMember(Long workspaceId, Long targetUserId, String role, Long operatorUserId);

    /**
     * 查询工作区成员列表.
     *
     * @param workspaceId 工作区ID
     * @param userId 请求用户ID
     * @return 成员列表
     */
    List<MemberResponse> listMembers(Long workspaceId, Long userId);

    /**
     * 更新成员角色.
     *
     * @param workspaceId 工作区ID
     * @param targetUserId 目标用户ID
     * @param newRole 新角色
     * @param operatorUserId 操作者用户ID
     */
    void updateMemberRole(Long workspaceId, Long targetUserId, String newRole, Long operatorUserId);

    /**
     * 移除成员.
     *
     * @param workspaceId 工作区ID
     * @param targetUserId 目标用户ID
     * @param operatorUserId 操作者用户ID
     */
    void removeMember(Long workspaceId, Long targetUserId, Long operatorUserId);

    /**
     * 查询用户在工作区中的成员记录.
     *
     * @param workspaceId 工作区ID
     * @param userId 用户ID
     * @return 成员记录（不存在则返回null）
     */
    WorkspaceMember getMemberByWorkspaceAndUser(Long workspaceId, Long userId);

    /**
     * 查询工作区实体.
     *
     * @param workspaceId 工作区ID
     * @return 工作区实体
     */
    Workspace getWorkspaceById(Long workspaceId);
}
