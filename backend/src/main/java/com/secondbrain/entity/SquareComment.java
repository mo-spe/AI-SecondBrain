package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 评论实体.
 *
 * <p>仅支持一级评论（不嵌套回复）。使用逻辑删除（deleted 标记）。</p>
 */
@Getter
@Setter
@TableName("square_comment")
public class SquareComment {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 评论者用户ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论时间
     */
    private LocalDateTime createdAt;

    /**
     * 逻辑删除标记：0-正常 / 1-已删除
     */
    private Integer deleted;
}
