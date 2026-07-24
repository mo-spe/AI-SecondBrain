package com.secondbrain.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.common.Result;
import com.secondbrain.service.GamificationService;
import com.secondbrain.vo.AchievementVO;
import com.secondbrain.vo.LeaderboardVO;
import com.secondbrain.vo.PointsLogVO;
import com.secondbrain.vo.StreakDayVO;
import com.secondbrain.vo.UserGamificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 游戏化控制器.
 *
 * <p>提供签到、成就、排行榜、补签卡、积分流水、签到热力图等接口。</p>
 */
@RestController
@RequestMapping("/gamification")
@Tag(name = "游戏化", description = "学习游戏化相关接口")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    /**
     * 获取用户游戏化概览.
     */
    @GetMapping("/profile")
    @Operation(summary = "获取游戏化概览")
    public Result<UserGamificationVO> getProfile(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(gamificationService.getProfile(userId));
    }

    /**
     * 每日签到.
     */
    @PostMapping("/check-in")
    @Operation(summary = "每日签到")
    public Result<UserGamificationVO> checkIn(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success("签到成功", gamificationService.checkIn(userId));
    }

    /**
     * 成就列表（含用户进度）.
     */
    @GetMapping("/achievements")
    @Operation(summary = "获取成就列表")
    public Result<List<AchievementVO>> getAchievements(
            @Parameter(description = "分类筛选") @RequestParam(required = false) String category,
            @Parameter(description = "状态筛选：all/unlocked/locked") @RequestParam(required = false) String filter,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(gamificationService.getAchievements(userId, category, filter));
    }

    /**
     * 排行榜.
     */
    @GetMapping("/leaderboard")
    @Operation(summary = "获取排行榜")
    public Result<LeaderboardVO> getLeaderboard(
            @Parameter(description = "周期：daily/weekly/monthly/all") @RequestParam(defaultValue = "daily") String period,
            @Parameter(description = "领域标签名或all") @RequestParam(defaultValue = "all") String domain,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "100") Integer size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(gamificationService.getLeaderboard(period, domain, size, userId));
    }

    /**
     * 使用补签卡.
     */
    @PostMapping("/makeup")
    @Operation(summary = "使用补签卡")
    public Result<UserGamificationVO> useMakeupCard(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success("补签成功", gamificationService.useMakeupCard(userId));
    }

    /**
     * 积分流水.
     */
    @GetMapping("/points-log")
    @Operation(summary = "获取积分流水")
    public Result<IPage<PointsLogVO>> getPointsLog(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(gamificationService.getPointsLog(userId, current, size));
    }

    /**
     * 签到热力图数据.
     */
    @GetMapping("/streak-calendar")
    @Operation(summary = "获取签到热力图数据")
    public Result<List<StreakDayVO>> getStreakCalendar(
            @Parameter(description = "回溯月数") @RequestParam(defaultValue = "3") Integer months,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(gamificationService.getStreakCalendar(userId, months));
    }
}
