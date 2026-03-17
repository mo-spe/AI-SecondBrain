package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.LoginDTO;
import com.secondbrain.dto.RegisterDTO;
import com.secondbrain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "认证接口", description = "用户登录、注册等认证相关接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<com.secondbrain.dto.LoginResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        com.secondbrain.dto.LoginResponseDTO response = authService.login(loginDTO);
        return Result.success(response);
    }
}
