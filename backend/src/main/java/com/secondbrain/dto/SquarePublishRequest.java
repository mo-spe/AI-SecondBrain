package com.secondbrain.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 发布到知识广场请求DTO.
 */
@Getter
@Setter
public class SquarePublishRequest {

    /**
     * 要发布的知识节点ID列表，按合集展示顺序传入，最多10个。
     */
    @Size(max = 10, message = "一次最多发布10个知识点")
    private List<Long> nodeIds;

    /**
     * 旧版单知识节点字段。
     *
     * <p>仅当 {@link #nodeIds} 未提供节点时作为兼容回退使用。</p>
     */
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
