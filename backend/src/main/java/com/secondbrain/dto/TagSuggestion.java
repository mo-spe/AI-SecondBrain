package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 标签建议结果.
 *
 * <p>封装 AI 建议的标签信息，用于前端展示确认/忽略交互。</p>
 */
@Getter
@Setter
public class TagSuggestion {

    /**
     * 建议的标签名称
     */
    private String tagName;

    /**
     * 如与已有标签匹配，则为已有标签的 ID；否则为 null
     */
    private Long existingTagId;

    /**
     * 是否为新建标签（未匹配到已有标签）
     */
    private boolean isNew;

    /**
     * AI 给出的置信度（0-100），仅供参考
     */
    private Integer confidence;

    public static TagSuggestion fromExisting(String tagName, Long existingTagId, int confidence) {
        TagSuggestion s = new TagSuggestion();
        s.setTagName(tagName);
        s.setExistingTagId(existingTagId);
        s.setNew(false);
        s.setConfidence(confidence);
        return s;
    }

    public static TagSuggestion newTag(String tagName, int confidence) {
        TagSuggestion s = new TagSuggestion();
        s.setTagName(tagName);
        s.setExistingTagId(null);
        s.setNew(true);
        s.setConfidence(confidence);
        return s;
    }
}
