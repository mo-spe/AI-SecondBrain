package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 待确认知识点实体类.
 * <p>AI 提取的知识点先存入此表，经用户确认后才迁移到 knowledge_node</p>
 */
@Getter
@Setter
@TableName("pending_knowledge")
public class PendingKnowledge {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 目标工作区ID（NULL=个人空间）
     */
    private Long workspaceId;

    /**
     * 关联的原始对话ID
     */
    private Long rawChatId;

    /**
     * 知识点标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 详细内容
     */
    private String content;

    /**
     * 状态：0=待确认，1=已确认入库，2=已丢弃
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
     * 逻辑删除：0=未删除，1=已删除
     */
    @TableLogic
    private Integer deleted;
}
