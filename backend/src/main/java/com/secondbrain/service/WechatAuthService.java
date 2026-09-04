package com.secondbrain.service;

import com.secondbrain.dto.LoginResponseDTO;

/** 微信小程序登录服务。 */
public interface WechatAuthService {

    /**
     * 使用微信一次性 code 登录或创建平台账户。
     *
     * @param code 微信 wx.login 返回的一次性凭证
     * @return 与账号密码登录一致的 JWT 和公开用户信息
     */
    LoginResponseDTO login(String code);
}
