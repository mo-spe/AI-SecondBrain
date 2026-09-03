package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 发布社区问题请求。
 *
 * @author AI
 */
@Getter
@Setter
public class CreateCommunityQuestionRequest {

    /** 问题标题。 */
    @NotBlank(message = "问题标题不能为空")
    @Size(min = 5, max = 120, message = "问题标题应为5到120个字符")
    private String title;

    /** 问题背景和具体描述。 */
    @NotBlank(message = "问题描述不能为空")
    @Size(min = 10, max = 5000, message = "问题描述应为10到5000个字符")
    private String content;

    /** 知识领域标签，最多5个。 */
    @Size(max = 5, message = "最多选择5个标签")
    private List<@NotBlank(message = "标签不能为空") @Size(max = 30, message = "单个标签不能超过30个字符") String> tags;
}
