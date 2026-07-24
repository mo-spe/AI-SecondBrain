package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识节点与标签关联实体（M:N 中间表）.
 */
@Getter
@Setter
@TableName("knowledge_node_tag_relation")
public class KnowledgeNodeTagRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识节点ID
     */
    private Long nodeId;

    /**
     * 标签ID
     */
    private Long tagId;

    private LocalDateTime createTime;
}
