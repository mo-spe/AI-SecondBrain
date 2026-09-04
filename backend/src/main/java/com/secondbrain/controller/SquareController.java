package com.secondbrain.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.common.Result;
import com.secondbrain.dto.SquareCommentRequest;
import com.secondbrain.dto.SquarePublishRequest;
import com.secondbrain.dto.SquareReportRequest;
import com.secondbrain.service.SquareService;
import com.secondbrain.vo.SquarePostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识广场控制器.
 *
 * <p>提供广场帖子发布、浏览、点赞、评论、收藏、举报等用户操作接口。</p>
 */
@RestController
@RequestMapping("/square")
@Tag(name = "知识广场", description = "知识广场帖子浏览与互动")
public class SquareController {

    private static final Logger log = LoggerFactory.getLogger(SquareController.class);

    private final SquareService squareService;

    public SquareController(SquareService squareService) {
        this.squareService = squareService;
    }

    /**
     * 发布一个或多个知识节点到广场。
     *
     * @param request     发布请求，兼容旧版单节点字段
     * @param httpRequest 当前请求，用于读取登录用户
     * @return 创建后的广场帖子
     */
    @PostMapping("/publish")
    @Operation(summary = "发布到广场")
    public Result<SquarePostVO> publish(@Valid @RequestBody SquarePublishRequest request,
                                         HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String scope = request.getScope() != null ? request.getScope() : "global";
        SquarePostVO vo = squareService.publish(request.getNodeIds(), request.getNodeId(), request.getRecommendText(),
                scope, request.getWorkspaceId(), userId);
        return Result.success("发布成功", vo);
    }

    /**
     * 下架自己的帖子.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "下架帖子")
    public Result<Void> unpublish(@Parameter(description = "帖子ID") @PathVariable Long id,
                                   HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        squareService.unpublish(id, userId);
        return Result.success("已下架");
    }

    /**
     * 广场帖子列表.
     */
    @GetMapping("/list")
    @Operation(summary = "广场帖子列表")
    public Result<IPage<SquarePostVO>> list(
            @Parameter(description = "发布范围：global/workspace") @RequestParam(defaultValue = "global") String scope,
            @Parameter(description = "排序方式：newest/hottest") @RequestParam(defaultValue = "newest") String sort,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = (Long) httpRequest.getAttribute("workspaceId");
        IPage<SquarePostVO> page = squareService.list(scope, sort, keyword, current, size, userId, workspaceId);
        return Result.success(page);
    }

    /**
     * 帖子详情（含评论列表）.
     */
    @GetMapping("/{id}")
    @Operation(summary = "帖子详情")
    public Result<SquarePostVO> getDetail(@Parameter(description = "帖子ID") @PathVariable Long id,
                                           HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        SquarePostVO vo = squareService.getDetail(id, userId);
        return Result.success(vo);
    }

    /**
     * 点赞/取消点赞.
     */
    @PostMapping("/{id}/like")
    @Operation(summary = "点赞/取消点赞")
    public Result<Boolean> toggleLike(@Parameter(description = "帖子ID") @PathVariable Long id,
                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        boolean liked = squareService.toggleLike(id, userId);
        return Result.success(liked ? "已点赞" : "已取消点赞", liked);
    }

    /**
     * 发表评论.
     */
    @PostMapping("/{id}/comment")
    @Operation(summary = "发表评论")
    public Result<?> addComment(@Parameter(description = "帖子ID") @PathVariable Long id,
                                 @Valid @RequestBody SquareCommentRequest request,
                                 HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success("评论成功", squareService.addComment(id, request.getContent(), userId));
    }

    /**
     * 删除评论.
     */
    @DeleteMapping("/{id}/comment/{commentId}")
    @Operation(summary = "删除评论")
    public Result<Void> deleteComment(@Parameter(description = "帖子ID") @PathVariable Long id,
                                       @Parameter(description = "评论ID") @PathVariable Long commentId,
                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        squareService.deleteComment(commentId, userId);
        return Result.success("评论已删除");
    }

    /**
     * 收藏/取消收藏.
     */
    @PostMapping("/{id}/bookmark")
    @Operation(summary = "收藏/取消收藏")
    public Result<Boolean> toggleBookmark(@Parameter(description = "帖子ID") @PathVariable Long id,
                                           HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        boolean bookmarked = squareService.toggleBookmark(id, userId);
        return Result.success(bookmarked ? "已收藏" : "已取消收藏", bookmarked);
    }

    /**
     * 我的收藏列表.
     */
    @GetMapping("/bookmarks")
    @Operation(summary = "我的收藏列表")
    public Result<IPage<SquarePostVO>> listMyBookmarks(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        IPage<SquarePostVO> page = squareService.listMyBookmarks(current, size, userId);
        return Result.success(page);
    }

    /**
     * 我的点赞列表.
     */
    @GetMapping("/likes")
    @Operation(summary = "我的点赞列表")
    public Result<IPage<SquarePostVO>> listMyLikes(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        IPage<SquarePostVO> page = squareService.listMyLikes(current, size, userId);
        return Result.success(page);
    }

    /**
     * 举报帖子.
     */
    @PostMapping("/{id}/report")
    @Operation(summary = "举报帖子")
    public Result<Void> report(@Parameter(description = "帖子ID") @PathVariable Long id,
                                @Valid @RequestBody SquareReportRequest request,
                                HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        squareService.report(id, request.getReason(), userId);
        return Result.success("举报已提交");
    }
}
