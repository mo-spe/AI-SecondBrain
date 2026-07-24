package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.service.UserAiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户AI配置控制器.
 * <p>管理用户按场景的AI服务商、模型和API Key配置</p>
 */
@RestController
@RequestMapping("/user/ai-config")
@Tag(name = "用户AI配置", description = "用户AI场景配置接口")
public class UserAiConfigController {

    private static final Logger log = LoggerFactory.getLogger(UserAiConfigController.class);

    private final UserAiConfigService userAiConfigService;

    public UserAiConfigController(UserAiConfigService userAiConfigService) {
        this.userAiConfigService = userAiConfigService;
    }

    @GetMapping
    @Operation(summary = "获取用户AI配置")
    public Result<List<Map<String, Object>>> getConfig(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userAiConfigService.getUserAiConfig(userId));
    }

    @PutMapping
    @Operation(summary = "批量保存场景配置")
    public Result<Void> saveConfig(@RequestBody List<Map<String, Object>> configs,
                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userAiConfigService.saveUserAiConfig(userId, configs);
        log.info("user_ai_config_saved userId={}", userId);
        return Result.success();
    }

    @PutMapping("/provider-key")
    @Operation(summary = "保存服务商全局API Key")
    public Result<Void> saveProviderKey(@RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long providerId = Long.valueOf(body.get("providerId").toString());
        String apiKey = body.get("apiKey") != null ? body.get("apiKey").toString() : null;
        userAiConfigService.saveProviderKey(userId, providerId, apiKey);
        log.info("provider_key_saved userId={} providerId={}", userId, providerId);
        return Result.success();
    }
}
