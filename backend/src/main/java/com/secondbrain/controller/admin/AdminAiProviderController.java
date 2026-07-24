package com.secondbrain.controller.admin;

import com.secondbrain.common.Result;
import com.secondbrain.entity.AiModel;
import com.secondbrain.entity.AiProvider;
import com.secondbrain.service.AiProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI服务商管理控制器.
 * <p>仅 Super Admin 角色可访问</p>
 */
@RestController
@RequestMapping("/admin/ai")
@Tag(name = "AI服务商管理", description = "Super Admin AI服务商和模型管理接口")
public class AdminAiProviderController {

    private static final Logger log = LoggerFactory.getLogger(AdminAiProviderController.class);

    private static final String SUPER_ADMIN = "super_admin";

    private final AiProviderService aiProviderService;

    public AdminAiProviderController(AiProviderService aiProviderService) {
        this.aiProviderService = aiProviderService;
    }

    /**
     * 校验 Super Admin 权限.
     */
    private void checkSuperAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!SUPER_ADMIN.equals(role)) {
            throw new com.secondbrain.exception.WorkspaceAccessDeniedException("仅超级管理员可访问");
        }
    }

    @GetMapping("/providers")
    @Operation(summary = "服务商列表")
    public Result<List<AiProvider>> listProviders(HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);
        return Result.success(aiProviderService.listProviders(true));
    }

    @PostMapping("/providers")
    @Operation(summary = "新增服务商")
    public Result<AiProvider> createProvider(@RequestBody AiProvider provider,
                                              HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);
        return Result.success(aiProviderService.save(provider));
    }

    @PutMapping("/providers/{id}")
    @Operation(summary = "编辑服务商")
    public Result<AiProvider> updateProvider(@PathVariable Long id,
                                              @RequestBody AiProvider provider,
                                              HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);
        provider.setId(id);
        return Result.success(aiProviderService.save(provider));
    }

    @DeleteMapping("/providers/{id}")
    @Operation(summary = "删除服务商")
    public Result<Void> deleteProvider(@PathVariable Long id,
                                        HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);
        aiProviderService.delete(id);
        return Result.success();
    }

    @GetMapping("/providers/{providerId}/models")
    @Operation(summary = "模型列表")
    public Result<List<AiModel>> listModels(@PathVariable Long providerId,
                                             HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);
        return Result.success(aiProviderService.listModels(providerId, true));
    }

    @PostMapping("/providers/{providerId}/models")
    @Operation(summary = "新增模型")
    public Result<AiModel> createModel(@PathVariable Long providerId,
                                        @RequestBody AiModel model,
                                        HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);
        model.setProviderId(providerId);
        return Result.success(aiProviderService.saveModel(model));
    }

    @PutMapping("/models/{id}")
    @Operation(summary = "编辑模型")
    public Result<AiModel> updateModel(@PathVariable Long id,
                                        @RequestBody AiModel model,
                                        HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);
        model.setId(id);
        return Result.success(aiProviderService.saveModel(model));
    }

    @DeleteMapping("/models/{id}")
    @Operation(summary = "删除模型")
    public Result<Void> deleteModel(@PathVariable Long id,
                                     HttpServletRequest httpRequest) {
        checkSuperAdmin(httpRequest);
        aiProviderService.deleteModel(id);
        return Result.success();
    }
}
