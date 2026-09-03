package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区回答视图对象。
 *
 * @author AI
 */
@Getter
@Setter
public class CommunityAnswerVO {

    /** 回答ID。 */
    private Long id;

    /** 回答者ID。 */
    private Long authorId;

    /** 回答者昵称。 */
    private String authorName;

    /** 回答者头像。 */
    private String authorAvatar;

    /** Markdown回答正文。 */
    private String content;

    /** 是否被采纳。 */
    private Boolean accepted;

    /** 明确授权公开的知识点快照。 */
    private List<KnowledgeSnapshotVO> knowledgeSnapshots;

    /** 发布时间。 */
    private LocalDateTime createTime;
}
