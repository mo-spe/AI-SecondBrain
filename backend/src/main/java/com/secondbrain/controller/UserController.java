package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.UpdateUserDTO;
import com.secondbrain.dto.UpdatePasswordDTO;
import com.secondbrain.entity.User;
import com.secondbrain.service.UserService;
import com.secondbrain.service.FileService;
import com.secondbrain.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户信息管理相关接口")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final FileService fileService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, FileService fileService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.fileService = fileService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    public Result<User> getUserInfo(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        if (userId == null) {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    userId = jwtUtil.getUserIdFromToken(token.substring(7));
                } catch (Exception e) {
                    log.error("从token解析userId失败", e);
                }
            }
        }
        
        if (userId == null) {
            log.error("userId为null，无法获取用户信息");
            return Result.error("用户未登录");
        }
        
        User user = userService.getUserById(userId);
        user.setPassword(null);
        
        log.info("获取用户信息成功，userId: {}", userId);
        return Result.success(user);
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的基本信息")
    public Result<String> updateUser(@RequestBody UpdateUserDTO updateUserDTO, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        if (userId == null) {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    userId = jwtUtil.getUserIdFromToken(token.substring(7));
                } catch (Exception e) {
                    log.error("从token解析userId失败", e);
                }
            }
        }
        
        if (userId == null) {
            log.error("userId为null，无法更新用户信息");
            return Result.error("用户未登录");
        }
        
        try {
            userService.updateUser(userId, updateUserDTO.getUsername(), updateUserDTO.getEmail(), updateUserDTO.getPhone(), updateUserDTO.getBio(), updateUserDTO.getApiKey());
            log.info("更新用户信息成功，userId: {}", userId);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public Result<String> updatePassword(@RequestBody UpdatePasswordDTO updatePasswordDTO, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        if (userId == null) {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    userId = jwtUtil.getUserIdFromToken(token.substring(7));
                } catch (Exception e) {
                    log.error("从token解析userId失败", e);
                }
            }
        }
        
        if (userId == null) {
            log.error("userId为null，无法修改密码");
            return Result.error("用户未登录");
        }
        
        try {
            userService.updatePassword(userId, updatePasswordDTO.getOldPassword(), updatePasswordDTO.getNewPassword());
            log.info("修改密码成功，userId: {}", userId);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return Result.error("密码修改失败：" + e.getMessage());
        }
    }

    @PostMapping("/avatar")
    @Operation(summary = "上传头像", description = "上传用户头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        if (userId == null) {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    userId = jwtUtil.getUserIdFromToken(token.substring(7));
                } catch (Exception e) {
                    log.error("从token解析userId失败", e);
                }
            }
        }
        
        if (userId == null) {
            log.error("userId为null，无法上传头像");
            return Result.error("用户未登录");
        }
        
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只支持上传图片文件");
        }
        
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("图片大小不能超过5MB");
        }
        
        try {
            String avatarUrl = fileService.uploadAvatar(file, userId);
            userService.updateAvatar(userId, avatarUrl);
            log.info("头像上传成功，userId: {}, avatarUrl: {}", userId, avatarUrl);
            return Result.success(avatarUrl);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败：" + e.getMessage());
        }
    }
}
