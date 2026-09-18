package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

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
     * 父评论ID（顶层评论为 NULL）
     */
    private Long parentId;

    /**
     * 被回复人用户ID（用于展示“回复 @昵称”）
     */
    private Long replyToUserId;

    /**
     * 被回复人用户名
     */
    private String replyToUsername;

    /**
     * 评论点赞数
     */
    private Integer likeCount;

    /**
     * 当前用户是否已点赞该评论
     */
    private Boolean isLikedByMe;

    /**
     * 评论时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否已删除：0-正常 / 1-已删除
     */
    private Integer deleted;

    /**
     * 楼中楼回复列表（仅顶层评论携带，按时间正序）
     */
    private List<SquareCommentVO> replies;
}
