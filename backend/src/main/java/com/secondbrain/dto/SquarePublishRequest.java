package com.secondbrain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 发布到知识广场请求DTO.
 */
@Getter
@Setter
public class SquarePublishRequest {

    /**
     * 知识节点ID
     */
    @NotNull(message = "请选择知识节点")
    private Long nodeId;

    /**
     * 推荐语/分享理由（≤200字）
     */
    @Size(max = 200, message = "推荐语不能超过200字")
    private String recommendText;

    /**
     * 发布范围：global / workspace（默认 global）
     */
    private String scope;

    /**
     * 工作区ID（scope=workspace 时必填）
     */
    private Long workspaceId;
}
