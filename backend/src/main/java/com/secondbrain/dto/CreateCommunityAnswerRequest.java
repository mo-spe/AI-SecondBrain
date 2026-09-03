package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 发布社区回答请求。
 *
 * @author AI
 */
@Getter
@Setter
public class CreateCommunityAnswerRequest {

    /** Markdown回答正文。 */
    @NotBlank(message = "回答内容不能为空")
    @Size(min = 10, max = 10000, message = "回答内容应为10到10000个字符")
    private String content;

    /** 用户主动选择公开的个人知识点ID。 */
    @Size(max = 5, message = "一次最多引用5个知识点")
    private List<Long> knowledgeNodeIds;
}
