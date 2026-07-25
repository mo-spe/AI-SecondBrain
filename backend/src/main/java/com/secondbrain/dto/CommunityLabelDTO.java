package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 社区标签统计 DTO.
 * <p>存储某个池子题目的社区复习统计，用于计算 🟢🟡🔴 标签</p>
 */
@Getter
@Setter
public class CommunityLabelDTO {

    /**
     * 池子题目ID
     */
    private Long poolId;

    /**
     * 参与复习的成员数
     */
    private Integer memberCount;

    /**
     * 已掌握的人数（mastery_level >= 4）
     */
    private Integer masteredCount;

    /**
     * 获取社区标签颜色代码.
     * <p>mastered/member >= 0.6 → green, >= 0.3 → yellow, < 0.3 → red</p>
     *
     * @return 颜色代码，memberCount=0 时返回 null
     */
    public String getLabel() {
        if (memberCount == null || memberCount == 0) {
            return null;
        }
        double ratio = (double) masteredCount / memberCount;
        if (ratio >= 0.6) {
            return "green";
        }
        if (ratio >= 0.3) {
            return "yellow";
        }
        return "red";
    }

    /**
     * 获取社区标签中文描述.
     *
     * @return 中文标签文本，memberCount=0 时返回 null
     */
    public String getLabelText() {
        if (memberCount == null || memberCount == 0) {
            return null;
        }
        double ratio = (double) masteredCount / memberCount;
        if (ratio >= 0.6) {
            return "多数已掌握";
        }
        if (ratio >= 0.3) {
            return "半数掌握";
        }
        return "普遍困难";
    }
}
