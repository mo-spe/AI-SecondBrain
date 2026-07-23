package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 工作区响应DTO. <p>用于返回工作区信息</p> */
@Getter
@Setter
public class WorkspaceResponse {

    /**
     * 工作区ID
     */
    private Long id;

    /**
     * 工作区名称
     */
    private String name;

    /**
     * 工作区描述
     */
    private String description;

    /**
     * 创建者用户ID
     */
    private Long ownerId;

    /**
     * 当前用户在该工作区的角色
     */
    private String role;

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
