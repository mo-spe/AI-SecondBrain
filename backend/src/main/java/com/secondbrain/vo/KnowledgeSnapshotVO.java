package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 回答中公开的知识点快照。
 *
 * @author AI
 */
@Getter
@Setter
public class KnowledgeSnapshotVO {

    /** 原知识点ID，仅用于所有者追溯。 */
    private Long sourceId;

    /** 发布时知识点标题。 */
    private String title;

    /** 发布时知识点摘要。 */
    private String summary;

    /** 发布时Markdown正文。 */
    private String content;
}
