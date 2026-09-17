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

    /**
     * 父评论ID（顶层评论为 NULL；楼中楼回复统一挂在其所属顶层评论下）
     */
    private Long parentId;

    /**
     * 被回复人用户ID（用于展示“回复 @昵称”），顶层评论为 NULL
     */
    private Long replyToUserId;
}
