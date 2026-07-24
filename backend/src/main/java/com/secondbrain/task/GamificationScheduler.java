package com.secondbrain.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.LeaderboardSnapshot;
import com.secondbrain.entity.PointsLog;
import com.secondbrain.entity.UserGamification;
import com.secondbrain.mapper.LeaderboardSnapshotMapper;
import com.secondbrain.mapper.PointsLogMapper;
import com.secondbrain.mapper.UserGamificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 游戏化定时任务.
 *
 * <p>负责排行榜快照生成和每月补签卡重置。
 * 排行榜每15分钟刷新一次（6:00-23:00），补签卡每月1号0点重置。</p>
 */
@Component
public class GamificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(GamificationScheduler.class);

    private final UserGamificationMapper gamificationMapper;
    private final PointsLogMapper pointsLogMapper;
    private final LeaderboardSnapshotMapper snapshotMapper;

    public GamificationScheduler(UserGamificationMapper gamificationMapper,
                                 PointsLogMapper pointsLogMapper,
                                 LeaderboardSnapshotMapper snapshotMapper) {
        this.gamificationMapper = gamificationMapper;
        this.pointsLogMapper = pointsLogMapper;
        this.snapshotMapper = snapshotMapper;
    }

    /**
     * 排行榜快照生成.
     *
     * <p>每15分钟执行一次（6:00-23:00），生成日/周/月/总榜四个周期的快照数据。
     * 先删除当天已有快照再重新生成，保证数据一致性。</p>
     */
    @Scheduled(cron = "0 */15 6-23 * * *")
    public void generateLeaderboardSnapshots() {
        log.info("leaderboard_snapshot_job_start");
        try {
            LocalDate today = LocalDate.now();
            String[] periods = {"daily", "weekly", "monthly", "all"};

            for (String period : periods) {
                generateSnapshotForPeriod(period, "all", today);
            }
            log.info("leaderboard_snapshot_job_done periods={}", (Object) periods);
        } catch (Exception e) {
            log.error("leaderboard_snapshot_job_failed", e);
        }
    }

    /**
     * 每月补签卡重置.
     *
     * <p>每月1号0点执行，重置所有用户的月补签卡使用计数，并补充免费补签卡至上限。</p>
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void resetMonthlyMakeupCards() {
        log.info("monthly_makeup_reset_start");
        try {
            List<UserGamification> allUsers = gamificationMapper.selectList(null);
            int updated = 0;
            for (UserGamification g : allUsers) {
                g.setMakeupCardsUsedThisMonth(0);
                // 每月免费2张 + 已持有不超过5张上限
                int newCards = Math.min(g.getMakeupCardsRemaining() + 2, 5);
                g.setMakeupCardsRemaining(newCards);
                gamificationMapper.updateById(g);
                updated++;
            }
            log.info("monthly_makeup_reset_done updated={}", updated);
        } catch (Exception e) {
            log.error("monthly_makeup_reset_failed", e);
        }
    }

    private void generateSnapshotForPeriod(String period, String domain, LocalDate today) {
        // 删除当天该周期的旧快照
        snapshotMapper.delete(new LambdaQueryWrapper<LeaderboardSnapshot>()
                .eq(LeaderboardSnapshot::getPeriod, period)
                .eq(LeaderboardSnapshot::getDomain, domain)
                .eq(LeaderboardSnapshot::getSnapshotDate, today));

        // 计算每个用户的周期得分
        List<UserScore> scores = computeUserScores(period);

        if (scores.isEmpty()) {
            return;
        }

        // 排序并生成快照
        List<LeaderboardSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            UserScore us = scores.get(i);
            LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
            snapshot.setUserId(us.userId);
            snapshot.setPeriod(period);
            snapshot.setDomain(domain);
            snapshot.setRankPosition(i + 1);
            snapshot.setScore(us.score);
            snapshot.setSnapshotDate(today);
            snapshot.setCreateTime(LocalDateTime.now());
            snapshots.add(snapshot);
        }

        // 只保留 Top 1000
        if (snapshots.size() > 1000) {
            snapshots = snapshots.subList(0, 1000);
        }

        for (LeaderboardSnapshot s : snapshots) {
            snapshotMapper.insert(s);
        }
    }

    /**
     * 计算所有用户在指定周期的综合得分.
     *
     * <p>日/周/月榜基于对应时段积分总和排名，总榜基于用户游戏化状态中的累计数据，
     * 使用综合评分公式：复习(40%) + 知识创建(25%) + 连续打卡(20%) + 正确率(15%)。</p>
     */
    private List<UserScore> computeUserScores(String period) {
        List<UserGamification> allUsers = gamificationMapper.selectList(null);
        if (allUsers.isEmpty()) {
            return List.of();
        }

        return switch (period) {
            case "daily" -> computePeriodScoresFromPointsLog(
                    LocalDate.now().atStartOfDay(), allUsers);
            case "weekly" -> computePeriodScoresFromPointsLog(
                    LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay(), allUsers);
            case "monthly" -> computePeriodScoresFromPointsLog(
                    LocalDate.now().withDayOfMonth(1).atStartOfDay(), allUsers);
            default -> computeAllTimeScores(allUsers);
        };
    }

    /**
     * 基于积分流水计算周期得分.
     *
     * <p>查询指定时间范围内的积分流水，按用户汇总作为排名依据。</p>
     */
    private List<UserScore> computePeriodScoresFromPointsLog(LocalDateTime since,
                                                              List<UserGamification> allUsers) {
        List<PointsLog> logs = pointsLogMapper.selectList(
                new LambdaQueryWrapper<PointsLog>()
                        .ge(PointsLog::getCreateTime, since));

        Map<Long, Long> userPoints = logs.stream()
                .filter(log -> log.getPoints() > 0)
                .collect(Collectors.groupingBy(PointsLog::getUserId,
                        Collectors.summingLong(PointsLog::getPoints)));

        return allUsers.stream()
                .map(u -> new UserScore(u.getUserId(),
                        userPoints.getOrDefault(u.getUserId(), 0L)))
                .sorted(Comparator.comparingLong(UserScore::score).reversed())
                .toList();
    }

    /**
     * 基于累计数据计算总榜综合得分.
     *
     * <p>综合评分 = 复习积分(40%) + 知识创建积分(25%) + 连续打卡积分(20%) + 正确率积分(15%)。
     * 确保即使某一维度为0也能正常计算。</p>
     */
    private List<UserScore> computeAllTimeScores(List<UserGamification> allUsers) {
        return allUsers.stream()
                .map(u -> {
                    long reviewScore = (long) (u.getTotalReviewCount() * 10L * 0.40);
                    long createScore = (long) (u.getTotalNodeCount() * 20L * 0.25);
                    long streakScore = (long) (u.getCurrentStreak() * 5L * 0.20);
                    double accuracy = u.getTotalReviewCount() > 0
                            ? (double) u.getTotalCorrectCount() / u.getTotalReviewCount()
                            : 0;
                    long accuracyScore = (long) (accuracy * 100 * 0.5 * 0.15);
                    long total = reviewScore + createScore + streakScore + accuracyScore;
                    return new UserScore(u.getUserId(), total);
                })
                .sorted(Comparator.comparingLong(UserScore::score).reversed())
                .toList();
    }

    private record UserScore(Long userId, Long score) {}
}
