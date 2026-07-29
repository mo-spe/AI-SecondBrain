package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究记忆实体.
 *
 * <p>存储研究过程中的中间状态和发现，支持跨会话持久化。</p>
 *
 * @author AI
 */
@Getter
@Setter
@TableName("research_memory")
public class ResearchMemory {

    /**
     * 记忆ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 记忆键（项目内唯一）
     */
    private String memoryKey;

    /**
     * 记忆类型：knowledge_state/gap_found/search_result/user_preference/decision
     */
    private String memoryType;

    /**
     * 记忆内容（JSON）
     */
    private String content;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessedAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
