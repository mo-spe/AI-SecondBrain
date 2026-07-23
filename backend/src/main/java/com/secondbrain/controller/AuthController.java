package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.LoginDTO;
import com.secondbrain.dto.RegisterDTO;
import com.secondbrain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/** 认证控制器. <p>提供用户登录、注册等认证相关接口</p> */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证接口", description = "用户登录、注册等认证相关接口")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
}
