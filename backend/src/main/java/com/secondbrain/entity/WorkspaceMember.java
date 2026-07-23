package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工作区成员实体.
 * <p>记录用户在工作区中的角色信息</p>
 */
@Getter
@Setter
@TableName("workspace_member")
public class WorkspaceMember {

    /**
     * 成员记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工作区ID
     */
    private Long workspaceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色（owner/admin/editor/viewer）
     */
    private String role;

    /**
     * 加入时间
     */
    private LocalDateTime joinedTime;

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

    /**
     * 成员状态（pending-待确认，accepted-已确认）
     */
    private String status;
}
