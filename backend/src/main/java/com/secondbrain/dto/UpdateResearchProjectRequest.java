package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 更新研究项目请求.
 *
 * <p>所有字段均为可选，仅更新非 null 字段。</p>
 *
 * @author AI
 */
@Getter
@Setter
public class UpdateResearchProjectRequest {

    /**
     * 研究标题
     */
    private String title;

    /**
     * 研究目标描述
     */
    private String goal;
}
