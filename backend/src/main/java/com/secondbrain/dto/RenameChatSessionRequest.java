package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 会话重命名请求。
 *
 * @author AI
 */
@Getter
@Setter
public class RenameChatSessionRequest {

    /**
     * 用户设置的会话标题。
     */
    @NotBlank(message = "会话标题不能为空")
    @Size(max = 100, message = "会话标题不能超过100个字符")
    private String title;
}
