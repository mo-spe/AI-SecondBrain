package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 广场合集帖子与知识节点的关联实体。
 *
 * <p>将多个知识节点按发布者指定的顺序归入同一帖子，同时保留
 * {@code square_post.node_id} 作为旧客户端可继续读取的首节点。</p>
 */
@Getter
@Setter
@TableName("square_post_node")
public class SquarePostNode {

    /** 关联记录ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 广场帖子ID。 */
    private Long postId;

    /** 知识节点ID。 */
    private Long nodeId;

    /** 节点在合集中的排序位置，从0开始。 */
    private Integer position;

    /** 创建时间。 */
    private LocalDateTime createdAt;
}
