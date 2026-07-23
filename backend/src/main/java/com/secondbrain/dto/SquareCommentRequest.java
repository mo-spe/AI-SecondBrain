package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 发表评论请求DTO.
 */
@Getter
@Setter
public class SquareCommentRequest {

    /**
     * 评论内容（1-500字）
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 500, message = "评论内容1-500字")
    private String content;
}
