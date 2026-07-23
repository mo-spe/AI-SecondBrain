package com.secondbrain.exception;

/** 工作区权限不足异常. <p>当用户无权访问目标工作区时抛出</p> */
public class WorkspaceAccessDeniedException extends BusinessException {

    /**
     * 构造带自定义消息的权限不足异常.
     *
     * @param message 异常消息
     */
    public WorkspaceAccessDeniedException(String message) {
        super(403, message);
    }

    /**
     * 构造带工作区ID和角色信息的权限不足异常.
     *
     * @param workspaceId 工作区ID
     * @param role        当前角色
     */
    public WorkspaceAccessDeniedException(Long workspaceId, String role) {
        super(403, String.format("权限不足：无法访问工作区 %d，当前角色 %s", workspaceId, role));
    }
}
