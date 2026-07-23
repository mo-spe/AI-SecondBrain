package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 原始对话记录实体类.
 * <p>存储从外部平台采集的原始对话数据，待后续处理</p>
 */
@Getter
@Setter
@TableName("raw_chat_record")
public class RawChatRecord {

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 工作区ID
     */
    private Long workspaceId;

    /**
     * 来源平台（wechat/chatgpt/other）
     */
    private String platform;

    /**
     * 对话内容
     */
    private String content;

    /**
     * 来源URL
     */
    private String sourceUrl;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 删除标记（逻辑删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 是否已处理（0-未处理，1-已处理）
     */
    private Integer processed;
}
