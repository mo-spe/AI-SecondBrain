package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 评论视图对象.
 */
@Getter
@Setter
public class SquareCommentVO {

    /**
     * 评论ID
     */
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
     * 评论者用户名
     */
    private String username;

    /**
     * 评论者头像
     */
    private String avatar;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否已删除：0-正常 / 1-已删除
     */
    private Integer deleted;
}
