package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.entity.EditingLock;
import com.secondbrain.entity.User;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.service.EditingLockService;
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
 * 编辑锁控制器.
 * <p>提供编辑锁的获取、释放和查询接口</p>
 */
@RestController
@RequestMapping("/knowledge")
@Tag(name = "编辑锁管理", description = "知识节点编辑锁相关接口")
public class EditingLockController {

    private static final Logger log = LoggerFactory.getLogger(EditingLockController.class);

    private final EditingLockService editingLockService;
    private final UserMapper userMapper;

    public EditingLockController(EditingLockService editingLockService, UserMapper userMapper) {
        this.editingLockService = editingLockService;
        this.userMapper = userMapper;
    }

    /**
     * 获取编辑锁.
     *
     * @param nodeId      知识节点ID
     * @param httpRequest HTTP请求对象
     * @return 锁状态信息
     */
    @PostMapping("/{nodeId}/lock")
    @Operation(summary = "获取编辑锁")
    public Result<Map<String, Object>> acquireLock(
            @Parameter(description = "知识节点ID") @PathVariable Long nodeId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        EditingLock lock = editingLockService.acquireLock(nodeId, userId);
        if (lock == null) {
            EditingLock currentLock = editingLockService.getLockStatus(nodeId);
            String holderName = "其他用户";
            if (currentLock != null) {
                User holder = userMapper.selectById(currentLock.getUserId());
                if (holder != null) {
                    holderName = holder.getUsername();
                }
            }
            Map<String, Object> conflictInfo = new HashMap<>();
            conflictInfo.put("lockedBy", currentLock != null ? currentLock.getUserId() : null);
            conflictInfo.put("lockedByName", holderName);
            conflictInfo.put("expiresAt", currentLock != null ? currentLock.getExpiresAt() : null);
            return Result.error(409, holderName + " 正在编辑此知识点，请稍后再试");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", lock.getNodeId());
        result.put("userId", lock.getUserId());
        result.put("expiresAt", lock.getExpiresAt());
        return Result.success("获取编辑锁成功", result);
    }

    /**
     * 释放编辑锁.
     *
     * @param nodeId      知识节点ID
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @DeleteMapping("/{nodeId}/lock")
    @Operation(summary = "释放编辑锁")
    public Result<Void> releaseLock(
            @Parameter(description = "知识节点ID") @PathVariable Long nodeId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        editingLockService.releaseLock(nodeId, userId);
        return Result.success("释放编辑锁成功", null);
    }

    /**
     * 查询锁状态.
     *
     * @param nodeId      知识节点ID
     * @param httpRequest HTTP请求对象
     * @return 锁状态信息
     */
    @GetMapping("/{nodeId}/lock")
    @Operation(summary = "查询锁状态")
    public Result<Map<String, Object>> getLockStatus(
            @Parameter(description = "知识节点ID") @PathVariable Long nodeId,
            HttpServletRequest httpRequest) {
        EditingLock lock = editingLockService.getLockStatus(nodeId);
        if (lock == null) {
            return Result.success("该知识点当前无人编辑", null);
        }

        User holder = userMapper.selectById(lock.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", lock.getNodeId());
        result.put("userId", lock.getUserId());
        result.put("userName", holder != null ? holder.getUsername() : "未知用户");
        result.put("acquiredAt", lock.getAcquiredAt());
        result.put("expiresAt", lock.getExpiresAt());
        return Result.success(result);
    }
}
