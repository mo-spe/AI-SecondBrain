package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识标签实体.
 *
 * <p>用于知识节点的分类标记，支持层级结构（parentId 形成树形标签体系）。</p>
 */
@Getter
@Setter
@TableName("knowledge_tag")
public class KnowledgeTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 标签颜色（十六进制）
     */
    private String tagColor;

    /**
     * 父标签ID，NULL表示顶级标签
     */
    private Long parentId;

    private LocalDateTime createTime;

    /**
     * 逻辑删除标记
     */
    private Integer deleted;

    /**
     * 子标签列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<KnowledgeTag> children;

    /**
     * 关联知识点数量（含子标签递归汇总，非数据库字段）
     */
    @TableField(exist = false)
    private Integer nodeCount;
}
