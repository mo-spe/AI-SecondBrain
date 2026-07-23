package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工作区/租户实体.
 * <p>每个工作区是一个独立的数据隔离空间，用户可创建或加入多个工作区</p>
 */
@Getter
@Setter
@TableName("workspace")
public class Workspace {

    /**
     * 工作区ID
     */
    @TableId(type = IdType.AUTO)
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
     * 状态（1-正常，0-禁用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标记（0-未删除，1-已删除）
     */
    private Integer deleted;
}
