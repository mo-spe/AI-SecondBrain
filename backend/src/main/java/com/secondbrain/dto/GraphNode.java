package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/** 知识图谱节点DTO. <p>表示知识图谱中的节点数据</p> */
@Getter
@Setter
public class GraphNode {

    /**
     * 节点ID
     */
    private String id;

    /**
     * 节点标签（显示名称）
     */
    private String label;

    /**
     * 节点类型
     */
    private String type;

    /**
     * 重要程度（1-5）
     */
    private Integer importance;

    /**
     * 掌握程度（0-5）
     */
    private Integer masteryLevel;

    /**
     * 节点大小（用于可视化）
     */
    private Integer size;

    /**
     * 节点颜色（用于可视化）
     */
    private String color;
}
