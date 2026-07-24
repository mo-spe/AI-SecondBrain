package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.entity.AiModel;
import com.secondbrain.entity.AiProvider;
import com.secondbrain.service.AiProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI服务商和模型查询控制器.
 * <p>供前端用户选择服务商和模型</p>
 */
@RestController
@RequestMapping("/ai")
@Tag(name = "AI服务商", description = "用户端AI服务商和模型查询接口")
public class AiProviderController {

    private final AiProviderService aiProviderService;

    public AiProviderController(AiProviderService aiProviderService) {
        this.aiProviderService = aiProviderService;
    }

    @GetMapping("/providers")
    @Operation(summary = "可用服务商列表")
    public Result<List<AiProvider>> listProviders() {
        return Result.success(aiProviderService.listProviders(false));
    }

    @GetMapping("/providers/{providerId}/models")
    @Operation(summary = "服务商下的可用模型列表")
    public Result<List<AiModel>> listModels(@PathVariable Long providerId) {
        return Result.success(aiProviderService.listModels(providerId, false));
    }
}
