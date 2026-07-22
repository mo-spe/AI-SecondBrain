package com.secondbrain.service;

import com.secondbrain.dto.LoginDTO;
import com.secondbrain.dto.LoginResponseDTO;
import com.secondbrain.dto.RegisterDTO;

/**
 * 认证服务接口.
 * <p>提供用户注册和登录功能</p>
 */
public interface AuthService {

    /**
     * 用户注册.
     *
     * @param registerDTO 注册信息
     */
    void register(RegisterDTO registerDTO);

    /**
     * 用户登录.
     *
     * @param loginDTO 登录信息
     * @return 登录响应（包含Token和用户信息）
     */
    LoginResponseDTO login(LoginDTO loginDTO);
}
