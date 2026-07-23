package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.MemberResponse;
import com.secondbrain.dto.WorkspaceResponse;
import com.secondbrain.entity.User;
import com.secondbrain.entity.Workspace;
import com.secondbrain.entity.WorkspaceMember;
import com.secondbrain.exception.WorkspaceAccessDeniedException;
import com.secondbrain.exception.WorkspaceNotFoundException;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.mapper.WorkspaceMapper;
import com.secondbrain.mapper.WorkspaceMemberMapper;
import com.secondbrain.service.WorkspaceService;
import com.secondbrain.util.WorkspaceRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** 工作区服务实现类. <p>工作区管理服务实现</p> */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceServiceImpl.class);

    private static final int MAX_WORKSPACES_PER_USER = 10;

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper memberMapper;
    private final UserMapper userMapper;

    public WorkspaceServiceImpl(WorkspaceMapper workspaceMapper,
                                WorkspaceMemberMapper memberMapper,
                                UserMapper userMapper) {
        this.workspaceMapper = workspaceMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
    }

    /**
     * 创建工作区.
     *
     * @param name        工作区名称
     * @param description 工作区描述
     * @param userId      用户ID
     * @return 工作区响应
     */
    @Override
    @Transactional
    public WorkspaceResponse create(String name, String description, Long userId) {
        long count = workspaceMapper.selectCount(
                new LambdaQueryWrapper<Workspace>().eq(Workspace::getOwnerId, userId));
        if (count >= MAX_WORKSPACES_PER_USER) {
            throw new IllegalStateException("每个用户最多创建" + MAX_WORKSPACES_PER_USER + "个工作区");
        }

        Workspace workspace = new Workspace();
        workspace.setName(name);
        workspace.setDescription(description);
        workspace.setOwnerId(userId);
        workspace.setStatus(1);
        workspaceMapper.insert(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspace.getId());
        member.setUserId(userId);
        member.setRole(WorkspaceRole.OWNER);
        member.setJoinedTime(LocalDateTime.now());
        member.setStatus("accepted");
        memberMapper.insert(member);

        log.info("workspace_created id={} name={} userId={}", workspace.getId(), name, userId);
        return toResponse(workspace, WorkspaceRole.OWNER, 1);
    }

    /**
     * 查询用户所在的工作区列表.
     *
     * @param userId 用户ID
     * @return 工作区响应列表
     */
    @Override
    public List<WorkspaceResponse> listByUser(Long userId) {
        LambdaQueryWrapper<WorkspaceMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkspaceMember::getUserId, userId);
        List<WorkspaceMember> memberships = memberMapper.selectList(wrapper);

        return memberships.stream().map(m -> {
            Workspace workspace = workspaceMapper.selectById(m.getWorkspaceId());
            if (workspace == null || workspace.getDeleted() == 1) {
                return null;
            }
            // 仅统计已确认的成员
            long memberCount = memberMapper.selectCount(
                    new LambdaQueryWrapper<WorkspaceMember>()
                            .eq(WorkspaceMember::getWorkspaceId, m.getWorkspaceId())
                            .eq(WorkspaceMember::getStatus, "accepted"));
            return toResponse(workspace, m.getRole(), (int) memberCount);
        }).filter(r -> r != null).collect(Collectors.toList());
    }

    /**
     * 根据工作区ID获取工作区详情.
     *
     * @param workspaceId 工作区ID
     * @param userId      用户ID
     * @return 工作区响应
     */
    @Override
    public WorkspaceResponse getById(Long workspaceId, Long userId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        WorkspaceMember member = findMemberOrThrow(workspaceId, userId);
        long memberCount = memberMapper.selectCount(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getWorkspaceId, workspaceId));
        return toResponse(workspace, member.getRole(), (int) memberCount);
    }

    /**
     * 更新工作区信息.
     *
     * @param workspaceId 工作区ID
     * @param name        工作区名称
     * @param description 工作区描述
     * @param userId      用户ID
     * @return void
     */
    @Override
    public void update(Long workspaceId, String name, String description, Long userId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        WorkspaceMember member = findMemberOrThrow(workspaceId, userId);

        if (!WorkspaceRole.isAdmin(member.getRole())) {
            throw new WorkspaceAccessDeniedException(workspaceId, member.getRole());
        }

        workspace.setName(name);
        workspace.setDescription(description);
        workspaceMapper.updateById(workspace);

        log.info("workspace_updated id={} name={} userId={}", workspaceId, name, userId);
    }

    /**
     * 删除工作区.
     *
     * @param workspaceId 工作区ID
     * @param userId      用户ID
     * @return void
     */
    @Override
    @Transactional
    public void delete(Long workspaceId, Long userId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        WorkspaceMember member = findMemberOrThrow(workspaceId, userId);

        if (!WorkspaceRole.OWNER.equals(member.getRole())) {
            throw new WorkspaceAccessDeniedException("只有工作区所有者才能删除工作区");
        }

        workspace.setDeleted(1);
        workspaceMapper.updateById(workspace);

        memberMapper.delete(new LambdaQueryWrapper<WorkspaceMember>()
                .eq(WorkspaceMember::getWorkspaceId, workspaceId));

        log.info("workspace_deleted id={} userId={}", workspaceId, userId);
    }

    /**
     * 转让工作区所有权.
     *
     * @param workspaceId    工作区ID
     * @param newOwnerUserId 新所有者用户ID
     * @param currentUserId  当前用户ID
     * @return void
     */
    @Override
    @Transactional
    public void transferOwnership(Long workspaceId, Long newOwnerUserId, Long currentUserId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);

        if (!workspace.getOwnerId().equals(currentUserId)) {
            throw new WorkspaceAccessDeniedException("只有工作区所有者才能转让所有权");
        }

        WorkspaceMember targetMember = findMemberOrThrow(workspaceId, newOwnerUserId);

        WorkspaceMember currentMember = memberMapper.selectOne(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                        .eq(WorkspaceMember::getUserId, currentUserId));
        currentMember.setRole(WorkspaceRole.ADMIN);
        memberMapper.updateById(currentMember);

        targetMember.setRole(WorkspaceRole.OWNER);
        memberMapper.updateById(targetMember);

        workspace.setOwnerId(newOwnerUserId);
        workspaceMapper.updateById(workspace);

        log.info("workspace_ownership_transferred id={} from={} to={}",
                workspaceId, currentUserId, newOwnerUserId);
    }

    /**
     * 添加工作区成员（邀请）.
     * 被邀请人状态设为pending，需对方确认后才成为正式成员。
     */
    @Override
    public void addMember(Long workspaceId, Long targetUserId, String role, Long operatorUserId) {
        if (!WorkspaceRole.isValid(role)) {
            throw new IllegalArgumentException("无效的角色: " + role);
        }
        if (WorkspaceRole.OWNER.equals(role)) {
            throw new IllegalArgumentException("不能直接添加Owner角色，请使用转让所有权功能");
        }

        WorkspaceMember operatorMember = findMemberOrThrow(workspaceId, operatorUserId);

        if (!WorkspaceRole.isAdmin(operatorMember.getRole())) {
            throw new WorkspaceAccessDeniedException(workspaceId, operatorMember.getRole());
        }

        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new IllegalArgumentException("用户不存在: " + targetUserId);
        }

        // 检查是否已存在成员记录（包括pending状态）
        WorkspaceMember existingMember = memberMapper.selectOne(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                        .eq(WorkspaceMember::getUserId, targetUserId));
        if (existingMember != null) {
            if ("pending".equals(existingMember.getStatus())) {
                throw new IllegalStateException("已向该用户发送过邀请，请等待对方确认");
            }
            throw new IllegalStateException("该用户已经是工作区成员");
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(targetUserId);
        member.setRole(role);
        member.setStatus("pending");
        memberMapper.insert(member);

        log.info("workspace_member_invited workspaceId={} targetUserId={} role={} operatorUserId={}",
                workspaceId, targetUserId, role, operatorUserId);
    }

    /**
     * 被邀请人确认加入工作区.
     * 将pending状态更新为accepted，设置加入时间。
     */
    @Override
    public void acceptInvitation(Long workspaceId, Long userId) {
        WorkspaceMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                        .eq(WorkspaceMember::getUserId, userId)
                        .eq(WorkspaceMember::getStatus, "pending"));
        if (member == null) {
            throw new IllegalStateException("未找到待确认的邀请");
        }

        member.setStatus("accepted");
        member.setJoinedTime(LocalDateTime.now());
        memberMapper.updateById(member);

        log.info("workspace_invitation_accepted workspaceId={} userId={}", workspaceId, userId);
    }

    /**
     * 查询工作区成员列表.
     *
     * @param workspaceId 工作区ID
     * @param userId      用户ID
     * @return 成员响应列表
     */
    @Override
    public List<MemberResponse> listMembers(Long workspaceId, Long userId) {
        findMemberOrThrow(workspaceId, userId);

        List<WorkspaceMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getWorkspaceId, workspaceId));

        return members.stream().map(m -> {
            User user = userMapper.selectById(m.getUserId());
            MemberResponse response = new MemberResponse();
            response.setUserId(m.getUserId());
            response.setUsername(user != null ? user.getUsername() : "未知用户");
            response.setAvatar(user != null ? user.getAvatar() : null);
            response.setRole(m.getRole());
            response.setJoinedTime(m.getJoinedTime());
            response.setStatus(m.getStatus());
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 更新工作区成员角色.
     *
     * @param workspaceId    工作区ID
     * @param targetUserId   目标用户ID
     * @param newRole        新角色
     * @param operatorUserId 操作者用户ID
     * @return void
     */
    @Override
    public void updateMemberRole(Long workspaceId, Long targetUserId, String newRole, Long operatorUserId) {
        if (!WorkspaceRole.isValid(newRole)) {
            throw new IllegalArgumentException("无效的角色: " + newRole);
        }
        if (WorkspaceRole.OWNER.equals(newRole)) {
            throw new IllegalArgumentException("不能将成员设为Owner，请使用转让所有权功能");
        }

        WorkspaceMember operatorMember = findMemberOrThrow(workspaceId, operatorUserId);

        if (!WorkspaceRole.isAdmin(operatorMember.getRole())) {
            throw new WorkspaceAccessDeniedException(workspaceId, operatorMember.getRole());
        }

        WorkspaceMember targetMember = findMemberOrThrow(workspaceId, targetUserId);

        if (WorkspaceRole.OWNER.equals(targetMember.getRole())) {
            throw new WorkspaceAccessDeniedException("不能修改Owner的角色");
        }

        targetMember.setRole(newRole);
        memberMapper.updateById(targetMember);

        log.info("workspace_member_role_updated workspaceId={} targetUserId={} newRole={}",
                workspaceId, targetUserId, newRole);
    }

    /**
     * 移除工作区成员.
     *
     * @param workspaceId    工作区ID
     * @param targetUserId   目标用户ID
     * @param operatorUserId 操作者用户ID
     * @return void
     */
    @Override
    public void removeMember(Long workspaceId, Long targetUserId, Long operatorUserId) {
        WorkspaceMember operatorMember = findMemberOrThrow(workspaceId, operatorUserId);

        if (!WorkspaceRole.isAdmin(operatorMember.getRole())) {
            throw new WorkspaceAccessDeniedException(workspaceId, operatorMember.getRole());
        }

        WorkspaceMember targetMember = findMemberOrThrow(workspaceId, targetUserId);

        if (WorkspaceRole.OWNER.equals(targetMember.getRole())) {
            throw new WorkspaceAccessDeniedException("不能移除工作区所有者，请先转让所有权或删除工作区");
        }

        memberMapper.deleteById(targetMember.getId());

        log.info("workspace_member_removed workspaceId={} targetUserId={} operatorUserId={}",
                workspaceId, targetUserId, operatorUserId);
    }

    /**
     * 根据工作区ID和用户ID获取成员信息.
     *
     * @param workspaceId 工作区ID
     * @param userId      用户ID
     * @return 工作区成员
     */
    @Override
    public WorkspaceMember getMemberByWorkspaceAndUser(Long workspaceId, Long userId) {
        return memberMapper.selectOne(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                        .eq(WorkspaceMember::getUserId, userId));
    }

    /**
     * 根据工作区ID获取工作区.
     *
     * @param workspaceId 工作区ID
     * @return 工作区
     */
    @Override
    public Workspace getWorkspaceById(Long workspaceId) {
        return workspaceMapper.selectById(workspaceId);
    }

    private Workspace findWorkspaceOrThrow(Long workspaceId) {
        Workspace workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null || workspace.getDeleted() == 1) {
            throw new WorkspaceNotFoundException(workspaceId);
        }
        return workspace;
    }

    private WorkspaceMember findMemberOrThrow(Long workspaceId, Long userId) {
        WorkspaceMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                        .eq(WorkspaceMember::getUserId, userId));
        if (member == null) {
            throw new WorkspaceAccessDeniedException("您不是该工作区的成员");
        }
        return member;
    }

    private WorkspaceResponse toResponse(Workspace workspace, String role, int memberCount) {
        WorkspaceResponse response = new WorkspaceResponse();
        response.setId(workspace.getId());
        response.setName(workspace.getName());
        response.setDescription(workspace.getDescription());
        response.setOwnerId(workspace.getOwnerId());
        response.setRole(role);
        response.setMemberCount(memberCount);
        response.setStatus(workspace.getStatus());
        response.setCreateTime(workspace.getCreateTime());
        return response;
    }
}
