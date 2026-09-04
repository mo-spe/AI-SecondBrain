package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 广场合集帖子中的知识节点视图对象。
 *
 * <p>内容在读取时从当前可见的知识节点加载，因此帖子不会固化一份可能过期的副本。</p>
 */
@Getter
@Setter
public class SquarePostKnowledgeNodeVO {

    /** 知识节点ID。 */
    private Long nodeId;

    /** 节点在合集中的排序位置，从0开始。 */
    private Integer position;

    /** 知识节点标题。 */
    private String title;

    /** 知识节点摘要。 */
    private String summary;

    /** 知识节点当前Markdown正文。 */
    private String contentMd;
}
