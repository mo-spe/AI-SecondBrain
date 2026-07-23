package com.secondbrain.exception;

/** 工作区不存在异常. <p>当目标工作区不存在时抛出</p> */
public class WorkspaceNotFoundException extends BusinessException {

    /**
     * 构造带工作区ID的不存在异常.
     *
     * @param workspaceId 工作区ID
     */
    public WorkspaceNotFoundException(Long workspaceId) {
        super(404, "工作区不存在: " + workspaceId);
    }
}
