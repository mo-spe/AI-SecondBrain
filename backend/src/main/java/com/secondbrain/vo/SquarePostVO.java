package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 广场帖子视图对象.
 *
 * <p>列表接口不填充 comments 字段，详情接口填充。</p>
 */
@Getter
@Setter
public class SquarePostVO {

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 关联的知识节点ID
     */
    private Long nodeId;

    /**
     * 知识节点标题
     */
    private String nodeTitle;

    /**
     * 知识节点摘要
     */
    private String nodeSummary;

    /**
     * 推荐语
     */
    private String recommendText;

    /**
     * 发布者用户ID
     */
    private Long authorId;

    /**
     * 发布者用户名
     */
    private String authorName;

    /**
     * 发布者头像
     */
    private String authorAvatar;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 收藏数
     */
    private Integer bookmarkCount;

    /**
     * 当前用户是否已点赞
     */
    private Boolean isLiked;

    /**
     * 当前用户是否已收藏
     */
    private Boolean isBookmarked;

    /**
     * 帖子状态：published / removed
     */
    private String status;

    /**
     * 发布时间
     */
    private LocalDateTime createdAt;

    /**
     * 评论列表（仅详情接口填充）
     */
    private List<SquareCommentVO> comments;
}
