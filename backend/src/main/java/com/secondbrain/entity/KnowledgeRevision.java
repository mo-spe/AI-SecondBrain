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
 * 知识节点版本历史实体.
 * <p>每次更新知识节点时自动保存快照，用于版本回溯和编辑审计</p>
 */
@Getter
@Setter
@TableName("knowledge_revision")
public class KnowledgeRevision {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识节点ID
     */
    private Long nodeId;

    /**
     * 编辑用户ID
     */
    private Long userId;

    /**
     * 快照标题
     */
    private String title;

    /**
     * 快照内容（Markdown）
     */
    private String contentMd;

    /**
     * 快照摘要
     */
    private String summary;

    /**
     * 版本号（每个节点独立自增）
     */
    private Integer revisionNum;

    /**
     * 变更说明
     */
    private String changeSummary;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
