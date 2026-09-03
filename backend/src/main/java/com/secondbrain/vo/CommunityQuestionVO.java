package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区问题视图对象。
 *
 * @author AI
 */
@Getter
@Setter
public class CommunityQuestionVO {

    /** 问题ID。 */
    private Long id;

    /** 发布者ID。 */
    private Long authorId;

    /** 发布者昵称。 */
    private String authorName;

    /** 发布者头像。 */
    private String authorAvatar;

    /** 问题标题。 */
    private String title;

    /** 问题描述。 */
    private String content;

    /** 知识领域标签。 */
    private List<String> tags;

    /** 状态：OPEN/CLOSED。 */
    private String status;

    /** 回答数量。 */
    private Integer answerCount;

    /** 浏览数量。 */
    private Integer viewCount;

    /** 被采纳回答ID。 */
    private Long acceptedAnswerId;

    /** 发布时间。 */
    private LocalDateTime createTime;

    /** 详情页回答列表。 */
    private List<CommunityAnswerVO> answers;
}
