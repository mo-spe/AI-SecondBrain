package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 广场帖子实体.
 *
 * <p>关联知识节点，用户主动发布到广场后创建。scope=global 时 workspaceId 为 null。</p>
 */
@Getter
@Setter
@TableName("square_post")
public class SquarePost {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的知识节点ID
     */
    private Long nodeId;

    /**
     * 发布者用户ID
     */
    private Long authorId;

    /**
     * 发布范围：global / workspace
     */
    private String scope;

    /**
     * 工作区ID（scope=workspace 时使用）
     */
    private Long workspaceId;

    /**
     * 推荐语/分享理由
     */
    private String recommendText;

    /**
     * 点赞数（冗余计数器）
     */
    private Integer likeCount;

    /**
     * 评论数（冗余计数器）
     */
    private Integer commentCount;

    /**
     * 收藏数（冗余计数器）
     */
    private Integer bookmarkCount;

    /**
     * 状态：published / removed
     */
    private String status;

    /**
     * 发布时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
