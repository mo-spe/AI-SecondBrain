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
 * <p>支持两级评论：顶层评论 parent_id 为 NULL；楼中楼回复的 parent_id 统一指向所属顶层评论，
 * 通过 reply_to_user_id 记录被回复人以展示“回复 @昵称”。使用逻辑删除（deleted 标记）。</p>
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
     * 父评论ID（顶层评论为 NULL；楼中楼回复统一挂在其所属顶层评论下）
     */
    private Long parentId;

    /**
     * 被回复人用户ID（用于展示“回复 @昵称”），顶层评论为 NULL
     */
    private Long replyToUserId;

    /**
     * 评论点赞数，从 square_comment_like 重算保证一致
     */
    private Integer likeCount;

    /**
     * 评论时间
     */
    private LocalDateTime createdAt;

    /**
     * 逻辑删除标记：0-正常 / 1-已删除
     */
    private Integer deleted;
}
