package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 社区主页中的公开贡献摘要。 */
@Getter
@Setter
public class CommunityContributionVO {

    /** 内容类型：QUESTION/ANSWER/KNOWLEDGE。 */
    private String type;

    /** 内容ID。 */
    private Long id;

    /** 内容标题。 */
    private String title;

    /** 内容摘要。 */
    private String summary;

    /** 发布时间。 */
    private LocalDateTime createTime;
}
