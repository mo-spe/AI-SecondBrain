package com.secondbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.vo.AchievementVO;
import com.secondbrain.vo.LeaderboardVO;
import com.secondbrain.vo.PointsLogVO;
import com.secondbrain.vo.StreakDayVO;
import com.secondbrain.vo.UserGamificationVO;

import java.util.List;

/**
 * 游戏化服务接口.
 *
 * <p>提供积分奖励、签到、成就、排行榜、补签卡等核心游戏化功能。</p>
 */
public interface GamificationService {

    /**
     * 获取用户游戏化概览.
     *
     * @param userId 用户ID
     * @return 游戏化概览VO
     */
    UserGamificationVO getProfile(Long userId);

    /**
     * 每日签到.
     *
     * @param userId 用户ID
     * @return 签到结果（含积分、连续天数、新解锁成就）
     */
    UserGamificationVO checkIn(Long userId);

    /**
     * 获取成就列表（含用户进度）.
     *
     * @param userId   用户ID
     * @param category 分类筛选（可选）
     * @param filter   状态筛选：all / unlocked / locked（可选）
     * @return 成就VO列表
     */
    List<AchievementVO> getAchievements(Long userId, String category, String filter);

    /**
     * 获取排行榜.
     *
     * @param period 周期：daily / weekly / monthly / all
     * @param domain 领域标签名或 "all"
     * @param size   返回条数
     * @param userId 当前用户ID
     * @return 排行榜VO
     */
    LeaderboardVO getLeaderboard(String period, String domain, Integer size, Long userId);

    /**
     * 使用补签卡恢复昨日签到.
     *
     * @param userId 用户ID
     * @return 补签结果
     */
    UserGamificationVO useMakeupCard(Long userId);

    /**
     * 获取积分流水.
     *
     * @param userId  用户ID
     * @param current 当前页
     * @param size    每页大小
     * @return 分页积分流水
     */
    IPage<PointsLogVO> getPointsLog(Long userId, Integer current, Integer size);

    /**
     * 获取签到热力图数据.
     *
     * @param userId 用户ID
     * @param months 回溯月数
     * @return 每日签到数据列表
     */
    List<StreakDayVO> getStreakCalendar(Long userId, Integer months);

    /**
     * 复习积分奖励（由 ReviewCardServiceImpl 调用）.
     *
     * @param userId     用户ID
     * @param difficulty 卡片难度（1-5）
     * @param isCorrect  是否回答正确
     * @param cardId     复习卡片ID
     */
    void awardReviewPoints(Long userId, Integer difficulty, boolean isCorrect, Long cardId);

    /**
     * 知识创建积分奖励（由 KnowledgeServiceImpl 调用）.
     *
     * @param userId 用户ID
     * @param nodeId 知识节点ID
     */
    void awardCreatePoints(Long userId, Long nodeId);

    /**
     * 更新最后复习日期（用于连续打卡判定）.
     *
     * @param userId 用户ID
     */
    void updateLastReviewDate(Long userId);

    /**
     * 增加节点创建计数.
     *
     * @param userId 用户ID
     */
    void incrementNodeCount(Long userId);

    /**
     * 检查并解锁成就（在积分变动后调用）.
     *
     * @param userId 用户ID
     * @return 新解锁的成就列表
     */
    List<AchievementVO> checkAndUnlockAchievements(Long userId);
}
