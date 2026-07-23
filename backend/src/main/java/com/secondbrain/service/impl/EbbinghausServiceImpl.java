package com.secondbrain.service.impl;

import com.secondbrain.service.EbbinghausService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 艾宾浩斯遗忘曲线计算服务实现类. <p>艾宾浩斯遗忘曲线计算服务实现</p> */
@Service
public class EbbinghausServiceImpl implements EbbinghausService {

    private static final Logger log = LoggerFactory.getLogger(EbbinghausServiceImpl.class);

    private static final double[] RETENTION_RATES = {1.0, 0.85, 0.7, 0.6, 0.5, 0.45};
    
    private static final long[] REVIEW_INTERVALS = {
        1440,     
        10080,    
        20160,    
        43200,    
        86400     
    };

    private static final int MAX_REVIEW_COUNT = 5;

    /**
     * 计算记忆保留率.
     *
     * @param reviewCount 复习次数
     * @return 记忆保留率
     */
    @Override
    public double calculateRetentionRate(int reviewCount) {
        if (reviewCount < 0) {
            return 0.0;
        }
        
        int index = Math.min(reviewCount, RETENTION_RATES.length - 1);
        return RETENTION_RATES[index];
    }

    /**
     * 计算下次复习间隔.
     *
     * @param reviewCount 复习次数
     * @param isCorrect   本次回答是否正确
     * @return 下次复习间隔（分钟）
     */
    @Override
    public long calculateNextReviewInterval(int reviewCount, boolean isCorrect) {
        if (reviewCount < 0) {
            return REVIEW_INTERVALS[0];
        }
        
        int index = Math.min(reviewCount, REVIEW_INTERVALS.length - 1);
        
        if (isCorrect) {
            return REVIEW_INTERVALS[index];
        } else {
            return Math.max(REVIEW_INTERVALS[0], REVIEW_INTERVALS[index] / 2);
        }
    }

    /**
     * 计算下次复习时间.
     *
     * @param lastReviewTime 上次复习时间
     * @param reviewCount    复习次数
     * @param isCorrect      本次回答是否正确
     * @return 下次复习时间
     */
    @Override
    public LocalDateTime calculateNextReviewTime(LocalDateTime lastReviewTime, int reviewCount, boolean isCorrect) {
        long intervalMinutes = calculateNextReviewInterval(reviewCount, isCorrect);
        
        LocalDateTime nextReviewTime = lastReviewTime.plusMinutes(intervalMinutes);
        
        log.debug("计算下次复习时间，lastReviewTime：{}，reviewCount：{}，isCorrect：{}，interval：{}分钟，nextReviewTime：{}",
                lastReviewTime, reviewCount, isCorrect, intervalMinutes, nextReviewTime);
        
        return nextReviewTime;
    }

    /**
     * 计算掌握程度.
     *
     * @param reviewCount     复习次数
     * @param averageAccuracy 平均正确率
     * @return 掌握程度等级
     */
    @Override
    public int calculateMasteryLevel(int reviewCount, double averageAccuracy) {
        if (reviewCount == 0) {
            return 0;
        }
        
        double retentionRate = calculateRetentionRate(reviewCount);
        double masteryScore = retentionRate * averageAccuracy;
        
        if (masteryScore >= 0.9) {
            return 5;
        } else if (masteryScore >= 0.8) {
            return 4;
        } else if (masteryScore >= 0.6) {
            return 3;
        } else if (masteryScore >= 0.4) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * 计算记忆强度.
     *
     * @param reviewCount     复习次数
     * @param averageAccuracy 平均正确率
     * @return 记忆强度
     */
    @Override
    public double calculateMemoryStrength(int reviewCount, double averageAccuracy) {
        if (reviewCount == 0) {
            return 0.0;
        }
        
        double retentionRate = calculateRetentionRate(reviewCount);
        double strength = retentionRate * averageAccuracy;
        
        return Math.min(1.0, Math.max(0.0, strength));
    }
}
