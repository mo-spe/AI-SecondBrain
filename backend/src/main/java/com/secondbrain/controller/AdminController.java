package com.secondbrain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.common.Result;
import com.secondbrain.entity.User;
import com.secondbrain.entity.Workspace;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.mapper.WorkspaceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员控制器.
 * <p>提供平台管理功能，仅 Super Admin 角色可访问</p>
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "平台管理", description = "Super Admin 管理接口")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private static final String SUPER_ADMIN = "super_admin";

    private final UserMapper userMapper;
    private final WorkspaceMapper workspaceMapper;

    public AdminController(UserMapper userMapper, WorkspaceMapper workspaceMapper) {
        this.userMapper = userMapper;
        this.workspaceMapper = workspaceMapper;
    }

    /**
     * 校验 Super Admin 权限.
     */
    private void checkSuperAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!SUPER_ADMIN.equals(role)) {
            throw new com.secondbrain.exception.WorkspaceAccessDeniedException("仅超级管理员可访问");
        }
    }

    /**
     * 平台统计.
     *
     * @param httpRequest HTTP请求对象
     * @return 平台统计数据
     */
    @GetMapping("/statistics")
    @Operation(summary = "平台统计")
    public Result<Map<String, Object>> statistics(HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);

        long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getDeleted, 0));
        long workspaceCount = workspaceMapper.selectCount(
                new LambdaQueryWrapper<Workspace>().eq(Workspace::getDeleted, 0));
        long knowledgeCount = 0; // 由 KnowledgeService 提供，此处简化

        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userCount);
        stats.put("workspaceCount", workspaceCount);
        stats.put("knowledgeCount", knowledgeCount);

        log.info("admin_statistics_accessed");
        return Result.success(stats);
    }

    /**
     * 用户列表.
     *
     * @param current 当前页
     * @param size 每页大小
     * @param httpRequest HTTP请求对象
     * @return 用户分页数据
     */
    @GetMapping("/users")
    @Operation(summary = "用户列表")
    public Result<Page<User>> listUsers(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
                .orderByDesc(User::getCreateTime);
        Page<User> page = new Page<>(current, size);
        Page<User> result = userMapper.selectPage(page, wrapper);

        result.getRecords().forEach(u -> u.setPassword(null));

        return Result.success(result);
    }

    /**
     * 禁用/启用用户.
     *
     * @param id 用户ID
     * @param status 状态：1-启用，0-禁用
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PutMapping("/users/{id}/disable")
    @Operation(summary = "禁用/启用用户")
    public Result<Void> disableUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态：1-启用，0-禁用") @RequestParam Integer status,
            HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);

        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        user.setStatus(status);
        userMapper.updateById(user);

        log.info("admin_user_status_changed id={} status={}", id, status);
        return Result.<Void>success(status == 1 ? "用户已启用" : "用户已禁用", null);
    }

    /**
     * 工作区列表.
     *
     * @param current 当前页
     * @param size 每页大小
     * @param httpRequest HTTP请求对象
     * @return 工作区分页数据
     */
    @GetMapping("/workspaces")
    @Operation(summary = "工作区列表")
    public Result<Page<Workspace>> listWorkspaces(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);

        LambdaQueryWrapper<Workspace> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Workspace::getDeleted, 0)
                .orderByDesc(Workspace::getCreateTime);
        Page<Workspace> page = new Page<>(current, size);
        Page<Workspace> result = workspaceMapper.selectPage(page, wrapper);

        return Result.success(result);
    }

    /**
     * 禁用/启用工作区.
     *
     * @param id 工作区ID
     * @param status 状态：1-启用，0-禁用
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PutMapping("/workspaces/{id}/disable")
    @Operation(summary = "禁用/启用工作区")
    public Result<Void> disableWorkspace(
            @Parameter(description = "工作区ID") @PathVariable Long id,
            @Parameter(description = "状态：1-启用，0-禁用") @RequestParam Integer status,
            HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);

        Workspace workspace = workspaceMapper.selectById(id);
        if (workspace == null) {
            return Result.error(404, "工作区不存在");
        }

        workspace.setStatus(status);
        workspaceMapper.updateById(workspace);

        log.info("admin_workspace_status_changed id={} status={}", id, status);
        return Result.<Void>success(status == 1 ? "工作区已启用" : "工作区已禁用", null);
    }
}
