package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 微信小程序登录请求。 */
@Getter
@Setter
public class WxLoginRequest {

    /** wx.login 返回的一次性 code。 */
    @NotBlank(message = "微信登录凭证不能为空")
    private String code;
}
