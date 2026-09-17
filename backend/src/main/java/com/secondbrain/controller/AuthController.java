package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.LoginDTO;
import com.secondbrain.dto.RegisterDTO;
import com.secondbrain.dto.WxLoginRequest;
import com.secondbrain.service.AuthService;
import com.secondbrain.service.WechatAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 认证控制器. <p>提供用户登录、注册等认证相关接口</p> */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证接口", description = "用户登录、注册等认证相关接口")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final WechatAuthService wechatAuthService;

    @Autowired
    public AuthController(AuthService authService, WechatAuthService wechatAuthService) {
        this.authService = authService;
        this.wechatAuthService = wechatAuthService;
    }

    /**
     * 兼容既有单元测试和手动构造场景的最小构造入口。
     *
     * @param authService 账号密码认证服务
     */
    public AuthController(AuthService authService) {
        this(authService, null);
    }

    /**
     * 用户注册
     *
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success("注册成功");
    }

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果，包含token等信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<com.secondbrain.dto.LoginResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        com.secondbrain.dto.LoginResponseDTO response = authService.login(loginDTO);
        return Result.success(response);
    }

    /**
     * 微信小程序登录。
     *
     * @param request 微信一次性登录凭证
     * @return 登录响应，包含 JWT 和用户公开信息
     */
    @PostMapping("/wx-login")
    @Operation(summary = "微信小程序登录")
    public Result<com.secondbrain.dto.LoginResponseDTO> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        if (wechatAuthService == null) {
            throw new IllegalStateException("微信登录服务未初始化");
        }
        return Result.success(wechatAuthService.login(request.getCode()));
    }
}
