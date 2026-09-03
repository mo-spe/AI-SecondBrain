package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 社区公开个人主页视图对象。 */
@Getter
@Setter
public class CommunityUserProfileVO {

    /** 用户ID。 */
    private Long userId;

    /** 公开昵称。 */
    private String username;

    /** 头像地址。 */
    private String avatar;

    /** 社区公开简介。 */
    private String introduction;

    /** 擅长领域。 */
    private List<String> expertiseTags;

    /** 是否为当前用户本人。 */
    private Boolean isSelf;

    /** 当前用户是否关注了主页用户。 */
    private Boolean isFollowing;

    /** 主页用户是否关注了当前用户。 */
    private Boolean isFollowedBy;

    /** 双方是否存在任一方向的拉黑关系。 */
    private Boolean isBlocked;

    /** 当前用户是否主动拉黑了主页用户。 */
    private Boolean isBlockedByMe;

    /** 主页用户是否拉黑了当前用户。 */
    private Boolean isBlockedByTarget;

    /** 粉丝数。 */
    private Long followerCount;

    /** 关注数。 */
    private Long followingCount;

    /** 公开问题数。 */
    private Long questionCount;

    /** 公开回答数。 */
    private Long answerCount;

    /** 被采纳回答数。 */
    private Long acceptedAnswerCount;

    /** 公开知识文章数。 */
    private Long knowledgePostCount;

    /** 公开知识文章累计获赞数。 */
    private Long receivedLikeCount;

    /** 公开知识文章累计被收藏数。 */
    private Long receivedBookmarkCount;

    /** 最近公开问题。 */
    private List<CommunityContributionVO> recentQuestions;

    /** 最近公开回答。 */
    private List<CommunityContributionVO> recentAnswers;

    /** 最近公开知识文章。 */
    private List<CommunityContributionVO> recentKnowledgePosts;
}
