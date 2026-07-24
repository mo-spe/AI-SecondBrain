package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识标签实体.
 *
 * <p>用于知识节点的分类标记，支持领域排行榜的按标签筛选。</p>
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

    private LocalDateTime createTime;

    /**
     * 逻辑删除标记
     */
    private Integer deleted;
}
