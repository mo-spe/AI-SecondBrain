package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 笔记捕捉请求DTO.
 */
@Getter
@Setter
public class NoteCaptureRequest {

    /**
     * 笔记标题
     */
    private String title;

    /**
     * 笔记内容
     */
    private String content;

    /**
     * 用户ID
     */
    private Long userId;
}
