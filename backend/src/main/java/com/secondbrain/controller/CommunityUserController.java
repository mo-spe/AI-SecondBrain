package com.secondbrain.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.common.Result;
import com.secondbrain.dto.UpdateCommunityProfileRequest;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.service.CommunityUserService;
import com.secondbrain.vo.CommunityUserListItemVO;
import com.secondbrain.vo.CommunityUserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社区公开资料与用户关系控制器。
 *
 * @author AI
 */
@RestController
@RequestMapping("/community/users")
@Tag(name = "社区用户", description = "公开主页、关注与拉黑关系")
public class CommunityUserController {

    private final CommunityUserService communityUserService;

    public CommunityUserController(CommunityUserService communityUserService) {
        this.communityUserService = communityUserService;
    }

    /**
     * 获取指定用户的公开社区主页。
     *
     * @param userId 主页用户ID
     * @param request HTTP请求
     * @return 公开主页及贡献摘要
     */
    @GetMapping("/{userId}")
    @Operation(summary = "获取公开个人主页")
    public Result<CommunityUserProfileVO> profile(@PathVariable Long userId, HttpServletRequest request) {
        return Result.success(communityUserService.getProfile(userId, currentUserId(request)));
    }

    /**
     * 更新当前用户的社区公开资料。
     *
     * @param profileRequest 公开简介和擅长领域
     * @param request HTTP请求
     * @return 更新后的公开主页
     */
    @PutMapping("/me/profile")
    @Operation(summary = "更新我的社区资料")
    public Result<CommunityUserProfileVO> updateMyProfile(
            @Valid @RequestBody UpdateCommunityProfileRequest profileRequest,
            HttpServletRequest request) {
        return Result.success(communityUserService.updateProfile(currentUserId(request), profileRequest));
    }

    /**
     * 关注指定用户。
     *
     * @param userId 目标用户ID
     * @param request HTTP请求
     * @return 空响应
     */
    @PostMapping("/{userId}/follow")
    @Operation(summary = "关注用户")
    public Result<Void> follow(@PathVariable Long userId, HttpServletRequest request) {
        communityUserService.follow(currentUserId(request), userId);
        return Result.success("关注成功");
    }

    /**
     * 取消关注指定用户。
     *
     * @param userId 目标用户ID
     * @param request HTTP请求
     * @return 空响应
     */
    @DeleteMapping("/{userId}/follow")
    @Operation(summary = "取消关注")
    public Result<Void> unfollow(@PathVariable Long userId, HttpServletRequest request) {
        communityUserService.unfollow(currentUserId(request), userId);
        return Result.success("已取消关注");
    }

    /**
     * 分页获取指定用户的粉丝。
     *
     * @param userId 主页用户ID
     * @param current 当前页
     * @param size 每页大小，最大50
     * @param request HTTP请求
     * @return 粉丝分页
     */
    @GetMapping("/{userId}/followers")
    @Operation(summary = "粉丝列表")
    public Result<IPage<CommunityUserListItemVO>> followers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest request) {
        return Result.success(communityUserService.listFollowers(userId, currentUserId(request), current, size));
    }

    /**
     * 分页获取指定用户关注的人。
     *
     * @param userId 主页用户ID
     * @param current 当前页
     * @param size 每页大小，最大50
     * @param request HTTP请求
     * @return 关注用户分页
     */
    @GetMapping("/{userId}/following")
    @Operation(summary = "关注列表")
    public Result<IPage<CommunityUserListItemVO>> following(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest request) {
        return Result.success(communityUserService.listFollowing(userId, currentUserId(request), current, size));
    }

    /**
     * 拉黑指定用户，并解除双方现有关注关系。
     *
     * @param userId 目标用户ID
     * @param request HTTP请求
     * @return 空响应
     */
    @PostMapping("/{userId}/block")
    @Operation(summary = "拉黑用户")
    public Result<Void> block(@PathVariable Long userId, HttpServletRequest request) {
        communityUserService.block(currentUserId(request), userId);
        return Result.success("已拉黑该用户");
    }

    /**
     * 解除当前用户对指定用户的拉黑。
     *
     * @param userId 目标用户ID
     * @param request HTTP请求
     * @return 空响应
     */
    @DeleteMapping("/{userId}/block")
    @Operation(summary = "解除拉黑")
    public Result<Void> unblock(@PathVariable Long userId, HttpServletRequest request) {
        communityUserService.unblock(currentUserId(request), userId);
        return Result.success("已解除拉黑");
    }

    private Long currentUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
