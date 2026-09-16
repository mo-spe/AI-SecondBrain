package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.entity.Achievement;
import com.secondbrain.entity.DailyCheckIn;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.LeaderboardSnapshot;
import com.secondbrain.entity.PointsLog;
import com.secondbrain.entity.ReviewLog;
import com.secondbrain.entity.User;
import com.secondbrain.entity.UserAchievement;
import com.secondbrain.entity.UserGamification;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.AchievementMapper;
import com.secondbrain.mapper.DailyCheckInMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.LeaderboardSnapshotMapper;
import com.secondbrain.mapper.PointsLogMapper;
import com.secondbrain.mapper.ReviewLogMapper;
import com.secondbrain.mapper.UserAchievementMapper;
import com.secondbrain.mapper.UserGamificationMapper;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.service.GamificationService;
import com.secondbrain.vo.AchievementVO;
import com.secondbrain.vo.LeaderboardEntryVO;
import com.secondbrain.vo.LeaderboardVO;
import com.secondbrain.vo.PointsLogVO;
import com.secondbrain.vo.StreakDayVO;
import com.secondbrain.vo.UserGamificationVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 游戏化服务实现.
 *
 * <p>核心业务包括积分计算、成就评估、签到、补签卡、排行榜查询。
 * 积分计算失败不影响主流程，所有对外暴露的方法都做了 try-catch 保护。</p>
 */
@Service
public class GamificationServiceImpl implements GamificationService {

    private static final Logger log = LoggerFactory.getLogger(GamificationServiceImpl.class);

    private static final int BASE_REVIEW_POINTS = 10;
    private static final int CORRECT_BONUS = 5;
    private static final int CREATE_POINTS = 20;
    private static final int BASE_CHECKIN_POINTS = 5;
    private static final int MAX_MAKEUP_CARDS = 5;
    private static final int MONTHLY_FREE_CARDS = 2;
    private static final int MIN_REVIEWS_FOR_ACCURACY = 50;

    private final UserGamificationMapper gamificationMapper;
    private final DailyCheckInMapper checkInMapper;
    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final PointsLogMapper pointsLogMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final LeaderboardSnapshotMapper snapshotMapper;
    private final UserMapper userMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    public GamificationServiceImpl(UserGamificationMapper gamificationMapper,
                                   DailyCheckInMapper checkInMapper,
                                   AchievementMapper achievementMapper,
                                   UserAchievementMapper userAchievementMapper,
                                   PointsLogMapper pointsLogMapper,
                                   ReviewLogMapper reviewLogMapper,
                                   LeaderboardSnapshotMapper snapshotMapper,
                                   UserMapper userMapper,
                                   KnowledgeNodeMapper knowledgeNodeMapper) {
        this.gamificationMapper = gamificationMapper;
        this.checkInMapper = checkInMapper;
        this.achievementMapper = achievementMapper;
        this.userAchievementMapper = userAchievementMapper;
        this.pointsLogMapper = pointsLogMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.snapshotMapper = snapshotMapper;
        this.userMapper = userMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
    }

    // ==================== 用户初始化 ====================

    /**
     * 获取或创建用户的游戏化数据.
     * 首次访问时自动初始化，积分从零开始。
     */
    private UserGamification getOrCreateGamification(Long userId) {
        UserGamification g = gamificationMapper.selectOne(
                new LambdaQueryWrapper<UserGamification>().eq(UserGamification::getUserId, userId));
        if (g == null) {
            g = new UserGamification();
            g.setUserId(userId);
            g.setTotalPoints(0L);
            g.setCurrentPoints(0L);
            g.setLevel(1);
            g.setExperience(0L);
            g.setExperienceToNextLevel(250L);
            g.setCurrentStreak(0);
            g.setMaxStreak(0);
            g.setMakeupCardsRemaining(MONTHLY_FREE_CARDS);
            g.setMakeupCardsUsedThisMonth(0);
            g.setTotalReviewCount(0);
            g.setTotalCorrectCount(0);
            g.setTotalNodeCount(0);
            g.setCreateTime(LocalDateTime.now());
            g.setUpdateTime(LocalDateTime.now());
            gamificationMapper.insert(g);
        }
        return g;
    }

    // ==================== Profile ====================

    @Override
    public UserGamificationVO getProfile(Long userId) {
        UserGamification g = getOrCreateGamification(userId);
        UserGamificationVO vo = buildProfileVO(g);
        vo.setNextAchievement(findNextAchievement(userId, g));
        return vo;
    }

    // ==================== 签到 ====================

    @Override
    @Transactional
    public UserGamificationVO checkIn(Long userId) {
        UserGamification g = getOrCreateGamification(userId);
        LocalDate today = LocalDate.now();

        // 检查今日是否已签到
        Long todayCount = checkInMapper.selectCount(
                new LambdaQueryWrapper<DailyCheckIn>()
                        .eq(DailyCheckIn::getUserId, userId)
                        .eq(DailyCheckIn::getCheckInDate, today));
        if (todayCount > 0) {
            throw new BusinessException(400, "今日已签到");
        }

        // 计算连续天数
        int newStreak = calculateNewStreak(g);
        double streakMultiplier = getStreakMultiplier(newStreak);
        int pointsEarned = (int) Math.round(BASE_CHECKIN_POINTS * streakMultiplier);

        // 插入签到记录
        DailyCheckIn checkIn = new DailyCheckIn();
        checkIn.setUserId(userId);
        checkIn.setCheckInDate(today);
        checkIn.setPointsEarned(pointsEarned);
        checkIn.setStreakBonusMultiplier(new java.math.BigDecimal(streakMultiplier));
        checkIn.setIsMakeup(0);
        checkIn.setCreateTime(LocalDateTime.now());
        checkInMapper.insert(checkIn);

        // 更新游戏化数据
        g.setCurrentStreak(newStreak);
        if (newStreak > g.getMaxStreak()) {
            g.setMaxStreak(newStreak);
        }
        g.setLastCheckInDate(today);
        addPoints(g, pointsEarned, "checkin", "每日签到 (连续" + newStreak + "天, x" + streakMultiplier + ")", null);
        g.setUpdateTime(LocalDateTime.now());
        gamificationMapper.updateById(g);

        // 检查成就
        List<AchievementVO> newAchievements = checkAndUnlockAchievements(userId);

        // 连续7天奖励补签卡
        if (newStreak == 7 || newStreak == 30 || newStreak == 100) {
            int cardReward = newStreak == 100 ? 5 : (newStreak == 30 ? 2 : 1);
            g.setMakeupCardsRemaining(Math.min(MAX_MAKEUP_CARDS, g.getMakeupCardsRemaining() + cardReward));
            gamificationMapper.updateById(g);
        }

        UserGamificationVO vo = buildProfileVO(g);
        vo.setPointsEarned(pointsEarned);
        vo.setNewAchievements(newAchievements.isEmpty() ? null : newAchievements);
        vo.setCheckedInToday(true);
        return vo;
    }

    // ==================== 成就 ====================

    @Override
    public List<AchievementVO> getAchievements(Long userId, String category, String filter) {
        UserGamification g = getOrCreateGamification(userId);

        LambdaQueryWrapper<Achievement> wrapper = new LambdaQueryWrapper<Achievement>()
                .orderByAsc(Achievement::getSortOrder);
        if (category != null && !category.isBlank()) {
            wrapper.eq(Achievement::getCategory, category);
        }
        List<Achievement> allAchievements = achievementMapper.selectList(wrapper);

        // 已解锁的成就ID集合
        List<UserAchievement> unlockedList = userAchievementMapper.selectList(
                new LambdaQueryWrapper<UserAchievement>().eq(UserAchievement::getUserId, userId));
        Map<Long, UserAchievement> unlockedMap = unlockedList.stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, ua -> ua));

        List<AchievementVO> result = new ArrayList<>();
        for (Achievement a : allAchievements) {
            AchievementVO vo = toAchievementVO(a, g, unlockedMap);
            // 应用筛选
            if ("unlocked".equals(filter) && Boolean.FALSE.equals(vo.getUnlocked())) continue;
            if ("locked".equals(filter) && Boolean.TRUE.equals(vo.getUnlocked())) continue;
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public List<AchievementVO> checkAndUnlockAchievements(Long userId) {
        UserGamification g = getOrCreateGamification(userId);

        // 获取所有未解锁的成就
        List<Long> unlockedIds = userAchievementMapper.selectList(
                        new LambdaQueryWrapper<UserAchievement>().eq(UserAchievement::getUserId, userId))
                .stream().map(UserAchievement::getAchievementId).toList();

        List<Achievement> lockedAchievements;
        if (unlockedIds.isEmpty()) {
            lockedAchievements = achievementMapper.selectList(null);
        } else {
            lockedAchievements = achievementMapper.selectList(
                    new LambdaQueryWrapper<Achievement>().notIn(Achievement::getId, unlockedIds));
        }

        List<AchievementVO> newUnlocks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Achievement a : lockedAchievements) {
            boolean shouldUnlock = switch (a.getTriggerType()) {
                case "review_count" -> g.getTotalReviewCount() >= a.getTriggerValue();
                case "knowledge_count" -> g.getTotalNodeCount() >= a.getTriggerValue();
                case "streak_days" -> g.getCurrentStreak() >= a.getTriggerValue();
                case "accuracy_rate" -> {
                    if (g.getTotalReviewCount() < MIN_REVIEWS_FOR_ACCURACY) yield false;
                    double rate = g.getTotalReviewCount() > 0
                            ? (double) g.getTotalCorrectCount() / g.getTotalReviewCount() * 100.0
                            : 0.0;
                    yield rate >= a.getTriggerValue();
                }
                case "mastery_first" -> {
                    Long mastered = knowledgeNodeMapper.selectCount(
                            new LambdaQueryWrapper<KnowledgeNode>()
                                    .eq(KnowledgeNode::getUserId, userId)
                                    .ge(KnowledgeNode::getMasteryLevel, 4));
                    yield mastered >= a.getTriggerValue();
                }
                default -> false;
            };

            if (shouldUnlock) {
                UserAchievement ua = new UserAchievement();
                ua.setUserId(userId);
                ua.setAchievementId(a.getId());
                ua.setUnlockedAt(now);
                ua.setNotified(0);
                userAchievementMapper.insert(ua);

                // 奖励积分
                if (a.getPointsReward() > 0) {
                    addPoints(g, a.getPointsReward(), "achievement", "解锁成就: " + a.getName(), a.getId());
                }
                // 奖励补签卡
                if (a.getMakeupCardReward() > 0) {
                    g.setMakeupCardsRemaining(
                            Math.min(MAX_MAKEUP_CARDS, g.getMakeupCardsRemaining() + a.getMakeupCardReward()));
                }

                AchievementVO vo = new AchievementVO();
                vo.setId(a.getId());
                vo.setCode(a.getCode());
                vo.setName(a.getName());
                vo.setDescription(a.getDescription());
                vo.setIcon(a.getIcon());
                vo.setCategory(a.getCategory());
                vo.setTier(a.getTier());
                vo.setPointsReward(a.getPointsReward());
                vo.setMakeupCardReward(a.getMakeupCardReward());
                vo.setUnlocked(true);
                vo.setUnlockedAt(now);
                newUnlocks.add(vo);

                log.info("achievement_unlocked userId={} code={} name={}", userId, a.getCode(), a.getName());
            }
        }

        if (!newUnlocks.isEmpty()) {
            gamificationMapper.updateById(g);
        }

        return newUnlocks;
    }

    // ==================== 排行榜 ====================

    @Override
    public LeaderboardVO getLeaderboard(String period, String domain, Integer size, Long userId) {
        String resolvedPeriod = (period == null || period.isBlank()) ? "daily" : period;
        String resolvedDomain = (domain == null || domain.isBlank()) ? "all" : domain;
        int resolvedSize = (size == null || size <= 0) ? 100 : Math.min(size, 100);
        LocalDate today = LocalDate.now();

        List<LeaderboardSnapshot> snapshots = snapshotMapper.selectList(
                new LambdaQueryWrapper<LeaderboardSnapshot>()
                        .eq(LeaderboardSnapshot::getPeriod, resolvedPeriod)
                        .eq(LeaderboardSnapshot::getDomain, resolvedDomain)
                        .eq(LeaderboardSnapshot::getSnapshotDate, today)
                        .orderByAsc(LeaderboardSnapshot::getRankPosition)
                        .last("LIMIT " + resolvedSize));

        LeaderboardVO vo = new LeaderboardVO();
        List<LeaderboardEntryVO> entries = new ArrayList<>();

        // 批量查用户信息
        List<Long> userIds = snapshots.stream().map(LeaderboardSnapshot::getUserId).toList();
        Map<Long, User> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        }

        for (LeaderboardSnapshot s : snapshots) {
            LeaderboardEntryVO entry = new LeaderboardEntryVO();
            entry.setRank(s.getRankPosition());
            entry.setUserId(s.getUserId());
            entry.setScore(s.getScore());
            User u = userMap.get(s.getUserId());
            if (u != null) {
                entry.setUsername(u.getUsername());
                entry.setAvatar(u.getAvatar());
            }
            UserGamification g = gamificationMapper.selectOne(
                    new LambdaQueryWrapper<UserGamification>().eq(UserGamification::getUserId, s.getUserId()));
            entry.setLevel(g != null ? g.getLevel() : 1);
            entries.add(entry);
        }
        vo.setEntries(entries);

        // 查找当前用户的排名
        LeaderboardSnapshot currentUserSnapshot = snapshotMapper.selectOne(
                new LambdaQueryWrapper<LeaderboardSnapshot>()
                        .eq(LeaderboardSnapshot::getPeriod, resolvedPeriod)
                        .eq(LeaderboardSnapshot::getDomain, resolvedDomain)
                        .eq(LeaderboardSnapshot::getSnapshotDate, today)
                        .eq(LeaderboardSnapshot::getUserId, userId));
        if (currentUserSnapshot != null) {
            LeaderboardEntryVO currentEntry = new LeaderboardEntryVO();
            currentEntry.setRank(currentUserSnapshot.getRankPosition());
            currentEntry.setUserId(userId);
            currentEntry.setScore(currentUserSnapshot.getScore());
            UserGamification g = getOrCreateGamification(userId);
            currentEntry.setLevel(g.getLevel());
            User u = userMapper.selectById(userId);
            if (u != null) {
                currentEntry.setUsername(u.getUsername());
                currentEntry.setAvatar(u.getAvatar());
            }
            vo.setCurrentUser(currentEntry);
        }

        return vo;
    }

    // ==================== 补签卡 ====================

    @Override
    @Transactional
    public UserGamificationVO useMakeupCard(Long userId) {
        UserGamification g = getOrCreateGamification(userId);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        if (g.getMakeupCardsRemaining() <= 0) {
            throw new BusinessException(400, "没有可用的补签卡");
        }

        // 检查昨日是否已签到
        Long yesterdayCount = checkInMapper.selectCount(
                new LambdaQueryWrapper<DailyCheckIn>()
                        .eq(DailyCheckIn::getUserId, userId)
                        .eq(DailyCheckIn::getCheckInDate, yesterday));
        if (yesterdayCount > 0) {
            throw new BusinessException(400, "昨日已签到，无需补签");
        }

        // 检查昨日是否有复习活动
        boolean hadReviewYesterday = g.getLastReviewDate() != null
                && g.getLastReviewDate().equals(yesterday);

        // 插入补签记录
        DailyCheckIn checkIn = new DailyCheckIn();
        checkIn.setUserId(userId);
        checkIn.setCheckInDate(yesterday);
        checkIn.setPointsEarned(0);
        checkIn.setStreakBonusMultiplier(java.math.BigDecimal.ONE);
        checkIn.setIsMakeup(1);
        checkIn.setCreateTime(LocalDateTime.now());
        checkInMapper.insert(checkIn);

        // 扣减补签卡
        g.setMakeupCardsRemaining(g.getMakeupCardsRemaining() - 1);
        g.setMakeupCardsUsedThisMonth(g.getMakeupCardsUsedThisMonth() + 1);

        // 重新计算连续天数
        if (hadReviewYesterday) {
            // 昨日有复习活动，连续天数不受影响
        }
        g.setLastCheckInDate(LocalDate.now());
        int newStreak = calculateNewStreak(g);
        g.setCurrentStreak(newStreak);
        if (newStreak > g.getMaxStreak()) {
            g.setMaxStreak(newStreak);
        }

        g.setUpdateTime(LocalDateTime.now());
        gamificationMapper.updateById(g);

        addPoints(g, 0, "makeup_use", "使用补签卡恢复 " + yesterday, null);

        log.info("makeup_card_used userId={} remaining={}", userId, g.getMakeupCardsRemaining());

        UserGamificationVO vo = buildProfileVO(g);
        // 补签后也检查成就（可能达到 streak 成就）
        vo.setNewAchievements(checkAndUnlockAchievements(userId));
        return vo;
    }

    // ==================== 积分流水 ====================

    @Override
    public IPage<PointsLogVO> getPointsLog(Long userId, Integer current, Integer size) {
        Page<PointsLog> page = new Page<>(current, size);
        LambdaQueryWrapper<PointsLog> wrapper = new LambdaQueryWrapper<PointsLog>()
                .eq(PointsLog::getUserId, userId)
                .orderByDesc(PointsLog::getCreateTime);
        Page<PointsLog> resultPage = pointsLogMapper.selectPage(page, wrapper);

        List<PointsLogVO> voList = resultPage.getRecords().stream().map(pl -> {
            PointsLogVO vo = new PointsLogVO();
            vo.setId(pl.getId());
            vo.setPoints(pl.getPoints());
            vo.setType(pl.getType());
            vo.setDescription(pl.getDescription());
            vo.setCreateTime(pl.getCreateTime());
            return vo;
        }).toList();

        Page<PointsLogVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 签到热力图 ====================

    @Override
    public List<StreakDayVO> getStreakCalendar(Long userId, Integer months) {
        int resolvedMonths = (months == null || months <= 0) ? 3 : Math.min(months, 12);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(resolvedMonths);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // 1. 查询期间内的签到记录（按日期聚合 pointsEarned）
        List<DailyCheckIn> checkIns = checkInMapper.selectList(
                new LambdaQueryWrapper<DailyCheckIn>()
                        .eq(DailyCheckIn::getUserId, userId)
                        .ge(DailyCheckIn::getCheckInDate, startDate)
                        .le(DailyCheckIn::getCheckInDate, endDate));
        Map<LocalDate, Integer> checkInPointsMap = checkIns.stream()
                .collect(Collectors.toMap(
                        DailyCheckIn::getCheckInDate,
                        c -> c.getPointsEarned() != null ? c.getPointsEarned() : 0,
                        Integer::sum));

        // 2. 查询期间内的积分流水（review/create/achievement 等所有产生积分的行为），按日期聚合
        List<PointsLog> pointsLogs = pointsLogMapper.selectList(
                new LambdaQueryWrapper<PointsLog>()
                        .eq(PointsLog::getUserId, userId)
                        .ge(PointsLog::getCreateTime, startDateTime)
                        .le(PointsLog::getCreateTime, endDateTime));
        Map<LocalDate, Integer> dailyPointsMap = new java.util.HashMap<>();
        for (PointsLog pl : pointsLogs) {
            if (pl.getCreateTime() == null || pl.getPoints() == null) continue;
            LocalDate d = pl.getCreateTime().toLocalDate();
            // 签到积分只统计一次到 checkInPointsMap，不再计入流水聚合
            if ("checkin".equals(pl.getType()) || "streak_bonus".equals(pl.getType())) continue;
            if (pl.getPoints() <= 0) continue; // 只计正向得分，不要扣分/消耗
            dailyPointsMap.merge(d, pl.getPoints(), Integer::sum);
        }

        // 3. 查询期间内的复习记录（按日期 distinct），用于 hasReview 标记
        List<ReviewLog> reviewLogs = reviewLogMapper.selectList(
                new LambdaQueryWrapper<ReviewLog>()
                        .eq(ReviewLog::getUserId, userId)
                        .ge(ReviewLog::getCreateTime, startDateTime)
                        .le(ReviewLog::getCreateTime, endDateTime)
                        .select(ReviewLog::getCreateTime));
        java.util.Set<LocalDate> reviewDates = reviewLogs.stream()
                .filter(r -> r.getCreateTime() != null)
                .map(r -> r.getCreateTime().toLocalDate())
                .collect(Collectors.toSet());

        // 4. 逐天构造结果：签到积分 + 其它行为积分合并
        List<StreakDayVO> result = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            StreakDayVO day = new StreakDayVO();
            day.setDate(d);

            Integer checkInPoints = checkInPointsMap.get(d);
            Integer behaviorPoints = dailyPointsMap.get(d);

            boolean hasCheckIn = checkInPoints != null && checkInPoints > 0;
            int totalPoints = (checkInPoints == null ? 0 : checkInPoints)
                    + (behaviorPoints == null ? 0 : behaviorPoints);

            day.setHasCheckIn(hasCheckIn);
            day.setHasReview(reviewDates.contains(d));
            day.setPointsEarned(totalPoints);
            result.add(day);
        }
        return result;
    }

    // ==================== 积分奖励（接入点） ====================

    @Override
    @Transactional
    public void awardReviewPoints(Long userId, Integer difficulty, boolean isCorrect, Long cardId) {
        try {
            UserGamification g = getOrCreateGamification(userId);

            double difficultyMultiplier = getDifficultyMultiplier(difficulty);
            int points = (int) Math.round((BASE_REVIEW_POINTS + (isCorrect ? CORRECT_BONUS : 0)) * difficultyMultiplier);

            g.setTotalReviewCount(g.getTotalReviewCount() + 1);
            if (isCorrect) {
                g.setTotalCorrectCount(g.getTotalCorrectCount() + 1);
            }
            g.setLastReviewDate(LocalDate.now());

            String desc = "完成复习" + (isCorrect ? "(正确)" : "") + " 难度:" + difficulty;
            addPoints(g, points, "review", desc, cardId);

            // 自动同步签到：如果今天还没签到但有复习活动，自动记录
            autoCheckInOnReview(g, userId);

            g.setUpdateTime(LocalDateTime.now());
            gamificationMapper.updateById(g);

            // 异步检查成就
            checkAndUnlockAchievements(userId);
        } catch (Exception e) {
            log.error("award_review_points_failed userId={} cardId={}", userId, cardId, e);
        }
    }

    @Override
    @Transactional
    public void awardCreatePoints(Long userId, Long nodeId) {
        try {
            UserGamification g = getOrCreateGamification(userId);
            g.setTotalNodeCount(g.getTotalNodeCount() + 1);
            addPoints(g, CREATE_POINTS, "create", "创建知识节点", nodeId);
            g.setUpdateTime(LocalDateTime.now());
            gamificationMapper.updateById(g);

            checkAndUnlockAchievements(userId);
        } catch (Exception e) {
            log.error("award_create_points_failed userId={} nodeId={}", userId, nodeId, e);
        }
    }

    @Override
    @Transactional
    public void updateLastReviewDate(Long userId) {
        try {
            UserGamification g = getOrCreateGamification(userId);
            g.setLastReviewDate(LocalDate.now());
            g.setUpdateTime(LocalDateTime.now());
            gamificationMapper.updateById(g);
        } catch (Exception e) {
            log.error("update_last_review_date_failed userId={}", userId, e);
        }
    }

    @Override
    @Transactional
    public void incrementNodeCount(Long userId) {
        try {
            UserGamification g = getOrCreateGamification(userId);
            g.setTotalNodeCount(g.getTotalNodeCount() + 1);
            g.setUpdateTime(LocalDateTime.now());
            gamificationMapper.updateById(g);
        } catch (Exception e) {
            log.error("increment_node_count_failed userId={}", userId, e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 计算新的连续签到天数.
     */
    private int calculateNewStreak(UserGamification g) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (g.getLastCheckInDate() == null) {
            return 1;
        }
        if (g.getLastCheckInDate().equals(yesterday)) {
            return g.getCurrentStreak() + 1;
        }
        if (g.getLastCheckInDate().equals(today)) {
            return g.getCurrentStreak();
        }
        return 1;
    }

    /**
     * 签到倍数：7天 x2, 30天 x3.
     */
    private double getStreakMultiplier(int streak) {
        if (streak >= 30) return 3.0;
        if (streak >= 7) return 2.0;
        return 1.0;
    }

    /**
     * 难度系数：1-2 → 1.0, 3-4 → 1.5, 5 → 2.0.
     */
    private double getDifficultyMultiplier(Integer difficulty) {
        if (difficulty == null) return 1.0;
        if (difficulty >= 5) return 2.0;
        if (difficulty >= 3) return 1.5;
        return 1.0;
    }

    /**
     * 增加积分并更新等级.
     */
    private void addPoints(UserGamification g, int points, String type, String description, Long referenceId) {
        g.setTotalPoints(g.getTotalPoints() + points);
        g.setCurrentPoints(g.getCurrentPoints() + points);
        g.setExperience(g.getExperience() + points);

        // 检查升级
        while (g.getExperience() >= g.getExperienceToNextLevel() && g.getLevel() < 50) {
            g.setExperience(g.getExperience() - g.getExperienceToNextLevel());
            g.setLevel(g.getLevel() + 1);
            g.setExperienceToNextLevel((long) g.getLevel() * 150 + 100);
        }

        // 插入积分日志
        PointsLog logEntry = new PointsLog();
        logEntry.setUserId(g.getUserId());
        logEntry.setPoints(points);
        logEntry.setType(type);
        logEntry.setDescription(description);
        logEntry.setReferenceId(referenceId);
        logEntry.setCreateTime(LocalDateTime.now());
        pointsLogMapper.insert(logEntry);
    }

    /**
     * 复习时自动同步签到：如果今天还没签到，标记已签到（不给积分，但保持连续天数）.
     */
    private void autoCheckInOnReview(UserGamification g, Long userId) {
        LocalDate today = LocalDate.now();
        Long todayCount = checkInMapper.selectCount(
                new LambdaQueryWrapper<DailyCheckIn>()
                        .eq(DailyCheckIn::getUserId, userId)
                        .eq(DailyCheckIn::getCheckInDate, today));
        if (todayCount == 0 && (g.getLastCheckInDate() == null || !g.getLastCheckInDate().equals(today))) {
            DailyCheckIn ci = new DailyCheckIn();
            ci.setUserId(userId);
            ci.setCheckInDate(today);
            ci.setPointsEarned(0);
            ci.setStreakBonusMultiplier(java.math.BigDecimal.ONE);
            ci.setIsMakeup(0);
            ci.setCreateTime(LocalDateTime.now());
            checkInMapper.insert(ci);

            int newStreak = calculateNewStreak(g);
            g.setCurrentStreak(newStreak);
            if (newStreak > g.getMaxStreak()) {
                g.setMaxStreak(newStreak);
            }
            g.setLastCheckInDate(today);
        }
    }

    /**
     * 构建 Profile VO.
     */
    private UserGamificationVO buildProfileVO(UserGamification g) {
        UserGamificationVO vo = new UserGamificationVO();
        vo.setTotalPoints(g.getTotalPoints());
        vo.setCurrentPoints(g.getCurrentPoints());
        vo.setLevel(g.getLevel());
        vo.setExperience(g.getExperience());
        vo.setExperienceToNextLevel(g.getExperienceToNextLevel());
        vo.setLevelProgressPercent(g.getExperienceToNextLevel() > 0
                ? Math.round(g.getExperience() * 10000.0 / g.getExperienceToNextLevel()) / 100.0
                : 100.0);
        vo.setCurrentStreak(g.getCurrentStreak());
        vo.setMaxStreak(g.getMaxStreak());
        vo.setLastCheckInDate(g.getLastCheckInDate());
        vo.setCheckedInToday(g.getLastCheckInDate() != null
                && g.getLastCheckInDate().equals(LocalDate.now()));
        vo.setMakeupCardsRemaining(g.getMakeupCardsRemaining());
        vo.setTotalReviewCount(g.getTotalReviewCount());
        vo.setTotalCorrectCount(g.getTotalCorrectCount());
        vo.setTotalNodeCount(g.getTotalNodeCount());
        vo.setAccuracyRate(g.getTotalReviewCount() > 0
                ? Math.round((double) g.getTotalCorrectCount() / g.getTotalReviewCount() * 10000.0) / 100.0
                : 0.0);
        return vo;
    }

    /**
     * 查找下一个待解锁的成就.
     */
    private AchievementVO findNextAchievement(Long userId, UserGamification g) {
        List<Long> unlockedIds = userAchievementMapper.selectList(
                        new LambdaQueryWrapper<UserAchievement>().eq(UserAchievement::getUserId, userId))
                .stream().map(UserAchievement::getAchievementId).toList();

        List<Achievement> locked = unlockedIds.isEmpty()
                ? achievementMapper.selectList(new LambdaQueryWrapper<Achievement>().orderByAsc(Achievement::getSortOrder))
                : achievementMapper.selectList(
                        new LambdaQueryWrapper<Achievement>()
                                .notIn(Achievement::getId, unlockedIds)
                                .orderByAsc(Achievement::getSortOrder));

        if (locked.isEmpty()) return null;

        // 返回进度最高的那个
        Achievement best = null;
        double bestProgress = -1;
        for (Achievement a : locked) {
            int currentValue = getCurrentValueForAchievement(g, a);
            double progress = Math.min(100.0, (double) currentValue / a.getTriggerValue() * 100.0);
            if (progress > bestProgress) {
                bestProgress = progress;
                best = a;
            }
        }
        if (best == null) return null;

        AchievementVO vo = new AchievementVO();
        vo.setId(best.getId());
        vo.setCode(best.getCode());
        vo.setName(best.getName());
        vo.setDescription(best.getDescription());
        vo.setIcon(best.getIcon());
        vo.setCategory(best.getCategory());
        vo.setTier(best.getTier());
        vo.setTriggerValue(best.getTriggerValue());
        vo.setPointsReward(best.getPointsReward());
        vo.setCurrentValue(getCurrentValueForAchievement(g, best));
        vo.setProgressPercent((int) Math.round(bestProgress));
        vo.setUnlocked(false);
        return vo;
    }

    /**
     * 将成就转换为 VO（含用户进度）.
     */
    private AchievementVO toAchievementVO(Achievement a, UserGamification g, Map<Long, UserAchievement> unlockedMap) {
        AchievementVO vo = new AchievementVO();
        vo.setId(a.getId());
        vo.setCode(a.getCode());
        vo.setName(a.getName());
        vo.setDescription(a.getDescription());
        vo.setIcon(a.getIcon());
        vo.setCategory(a.getCategory());
        vo.setTier(a.getTier());
        vo.setTriggerValue(a.getTriggerValue());
        vo.setPointsReward(a.getPointsReward());
        vo.setMakeupCardReward(a.getMakeupCardReward());

        int currentValue = getCurrentValueForAchievement(g, a);
        vo.setCurrentValue(currentValue);
        vo.setProgressPercent((int) Math.min(100, Math.round((double) currentValue / a.getTriggerValue() * 100)));

        UserAchievement ua = unlockedMap.get(a.getId());
        vo.setUnlocked(ua != null);
        vo.setUnlockedAt(ua != null ? ua.getUnlockedAt() : null);

        return vo;
    }

    /**
     * 获取用户在某成就上的当前进度值.
     */
    private int getCurrentValueForAchievement(UserGamification g, Achievement a) {
        return switch (a.getTriggerType()) {
            case "review_count" -> g.getTotalReviewCount();
            case "knowledge_count" -> g.getTotalNodeCount();
            case "streak_days" -> g.getCurrentStreak();
            case "accuracy_rate" -> g.getTotalReviewCount() > 0
                    ? (int) Math.round((double) g.getTotalCorrectCount() / g.getTotalReviewCount() * 100)
                    : 0;
            case "mastery_first" -> (int) (long) knowledgeNodeMapper.selectCount(
                    new LambdaQueryWrapper<KnowledgeNode>()
                            .eq(KnowledgeNode::getUserId, g.getUserId())
                            .ge(KnowledgeNode::getMasteryLevel, 4));
            default -> 0;
        };
    }
}
