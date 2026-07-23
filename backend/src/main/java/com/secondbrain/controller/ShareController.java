package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.entity.ShareLink;
import com.secondbrain.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分享链接控制器.
 * <p>提供分享链接的创建、访问、撤销和列表查询接口。
 * GET /share/{token} 为公开接口，无需鉴权。</p>
 */
@RestController
@RequestMapping("/share")
@Tag(name = "知识分享", description = "知识节点对外分享相关接口")
public class ShareController {

    private static final Logger log = LoggerFactory.getLogger(ShareController.class);

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /**
     * 创建分享链接.
     *
     * @param request     请求体 { nodeId, expireType }
     * @param httpRequest HTTP请求对象
     * @return 分享链接信息
     */
    @PostMapping
    @Operation(summary = "创建分享链接")
    public Result<Map<String, Object>> createShare(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        Long nodeId = request.get("nodeId") != null
                ? Long.valueOf(request.get("nodeId").toString()) : null;
        String expireType = request.get("expireType") != null
                ? request.get("expireType").toString() : "permanent";

        if (nodeId == null) {
            return Result.error(400, "请指定要分享的知识节点");
        }
        if (!"permanent".equals(expireType) && !"7d".equals(expireType) && !"24h".equals(expireType)) {
            return Result.error(400, "有效期类型无效，可选：permanent/7d/24h");
        }

        Map<String, Object> result = shareService.createShareLink(nodeId, userId, expireType);
        return Result.success("分享链接创建成功", result);
    }

    /**
     * 公开访问分享内容（无需鉴权）.
     *
     * @param token 分享令牌
     * @return 知识节点内容
     */
    @GetMapping("/{token}")
    @Operation(summary = "访问分享内容（公开）")
    public Result<Map<String, Object>> accessShare(
            @Parameter(description = "分享令牌") @PathVariable String token) {
        Map<String, Object> content = shareService.getShareContent(token);
        if (content == null) {
            return Result.error(404, "链接已失效或不存在");
        }
        return Result.success(content);
    }

    /**
     * 撤销分享链接.
     *
     * @param id          分享记录ID
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "撤销分享")
    public Result<Void> revokeShare(
            @Parameter(description = "分享记录ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        shareService.revokeShare(id, userId);
        return Result.success("分享已撤销", null);
    }

    /**
     * 我的分享列表.
     *
     * @param httpRequest HTTP请求对象
     * @return 分享记录列表
     */
    @GetMapping("/list")
    @Operation(summary = "我的分享列表")
    public Result<List<ShareLink>> listShares(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<ShareLink> shares = shareService.listMyShares(userId);
        return Result.success(shares);
    }
}
