package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.*;
import com.secondbrain.entity.User;
import com.secondbrain.entity.WorkspaceMember;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.service.WorkspaceService;
import com.secondbrain.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作区控制器.
 * <p>提供工作区创建、查询、更新、删除及成员管理接口</p>
 */
@RestController
@RequestMapping("/workspace")
@Tag(name = "工作区管理", description = "工作区CRUD及成员管理接口")
public class WorkspaceController {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceController.class);

    private final WorkspaceService workspaceService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public WorkspaceController(WorkspaceService workspaceService, JwtUtil jwtUtil, UserMapper userMapper) {
        this.workspaceService = workspaceService;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    /**
     * 创建工作区.
     *
     * @param request 创建工作区请求
     * @param httpRequest HTTP请求对象
     * @return 工作区响应
     */
    @PostMapping
    @Operation(summary = "创建工作区")
    public Result<WorkspaceResponse> create(@Valid @RequestBody CreateWorkspaceRequest request,
                                            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        WorkspaceResponse response = workspaceService.create(
                request.getName(), request.getDescription(), userId);
        return Result.success("工作区创建成功", response);
    }

    /**
     * 我的工作区列表.
     *
     * @param httpRequest HTTP请求对象
     * @return 工作区列表
     */
    @GetMapping
    @Operation(summary = "我的工作区列表")
    public Result<List<WorkspaceResponse>> list(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<WorkspaceResponse> list = workspaceService.listByUser(userId);
        return Result.success(list);
    }

    /**
     * 工作区详情.
     *
     * @param id 工作区ID
     * @param httpRequest HTTP请求对象
     * @return 工作区详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "工作区详情")
    public Result<WorkspaceResponse> getById(
            @Parameter(description = "工作区ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        WorkspaceResponse response = workspaceService.getById(id, userId);
        return Result.success(response);
    }

    /**
     * 更新工作区信息.
     *
     * @param id 工作区ID
     * @param request 更新工作区请求
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新工作区信息")
    public Result<Void> update(
            @Parameter(description = "工作区ID") @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        workspaceService.update(id, request.getName(), request.getDescription(), userId);
        return Result.<Void>success("更新成功", null);
    }

    /**
     * 删除工作区.
     *
     * @param id 工作区ID
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除工作区")
    public Result<Void> delete(
            @Parameter(description = "工作区ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        workspaceService.delete(id, userId);
        return Result.<Void>success("删除成功", null);
    }

    /**
     * 转让工作区所有权.
     *
     * @param id 工作区ID
     * @param request 转让所有权请求
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PutMapping("/{id}/transfer")
    @Operation(summary = "转让工作区所有权")
    public Result<Void> transferOwnership(
            @Parameter(description = "工作区ID") @PathVariable Long id,
            @Valid @RequestBody TransferOwnershipRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        workspaceService.transferOwnership(id, request.getNewOwnerUserId(), userId);
        return Result.<Void>success("所有权转让成功", null);
    }

    /**
     * 添加成员.
     *
     * @param id 工作区ID
     * @param request 添加成员请求
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PostMapping("/{id}/members")
    @Operation(summary = "添加成员")
    public Result<Void> addMember(
            @Parameter(description = "工作区ID") @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        workspaceService.addMember(id, request.getUserId(), request.getRole(), userId);
        return Result.<Void>success("成员添加成功", null);
    }

    /**
     * 成员列表.
     *
     * @param id 工作区ID
     * @param httpRequest HTTP请求对象
     * @return 成员列表
     */
    @GetMapping("/{id}/members")
    @Operation(summary = "成员列表")
    public Result<List<MemberResponse>> listMembers(
            @Parameter(description = "工作区ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<MemberResponse> members = workspaceService.listMembers(id, userId);
        return Result.success(members);
    }

    /**
     * 更新成员角色.
     *
     * @param id 工作区ID
     * @param targetUserId 目标用户ID
     * @param request 更新成员角色请求
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PutMapping("/{id}/members/{targetUserId}")
    @Operation(summary = "更新成员角色")
    public Result<Void> updateMemberRole(
            @Parameter(description = "工作区ID") @PathVariable Long id,
            @Parameter(description = "目标用户ID") @PathVariable Long targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        workspaceService.updateMemberRole(id, targetUserId, request.getRole(), userId);
        return Result.<Void>success("角色更新成功", null);
    }

    /**
     * 移除成员.
     *
     * @param id 工作区ID
     * @param targetUserId 目标用户ID
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @DeleteMapping("/{id}/members/{targetUserId}")
    @Operation(summary = "移除成员")
    public Result<Void> removeMember(
            @Parameter(description = "工作区ID") @PathVariable Long id,
            @Parameter(description = "目标用户ID") @PathVariable Long targetUserId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        workspaceService.removeMember(id, targetUserId, userId);
        return Result.<Void>success("成员已移除", null);
    }

    /**
     * 切换到指定工作区.
     *
     * @param id 工作区ID
     * @param httpRequest HTTP请求对象
     * @return 切换结果，包含新token
     */
    @PutMapping("/{id}/switch")
    @Operation(summary = "切换到指定工作区")
    public Result<Map<String, Object>> switchWorkspace(
            @Parameter(description = "工作区ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        WorkspaceMember member = workspaceService.getMemberByWorkspaceAndUser(id, userId);
        if (member == null) {
            return Result.error(403, "您不是该工作区的成员");
        }

        User user = userMapper.selectById(userId);
        String newToken = jwtUtil.generateToken(userId, user.getUsername(), user.getRole(), id);

        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("workspaceId", id);

        log.info("workspace_switched userId={} workspaceId={}", userId, id);
        return Result.success("工作区切换成功", result);
    }

    /**
     * 切换到个人空间.
     *
     * @param httpRequest HTTP请求对象
     * @return 切换结果，包含不含workspaceId的新token
     */
    @PutMapping("/personal")
    @Operation(summary = "切换到个人空间")
    public Result<Map<String, Object>> switchToPersonal(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        User user = userMapper.selectById(userId);
        String newToken = jwtUtil.generateToken(userId, user.getUsername(), user.getRole(), null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);

        log.info("workspace_switched_to_personal userId={}", userId);
        return Result.success("已切换到个人空间", result);
    }
}
