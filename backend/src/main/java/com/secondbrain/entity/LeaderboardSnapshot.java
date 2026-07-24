package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排行榜快照实体.
 *
 * <p>定时任务预计算的排行榜数据，按周期+领域维度存储，避免实时聚合查询。</p>
 */
@Getter
@Setter
@TableName("leaderboard_snapshot")
public class LeaderboardSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 排行榜周期：daily / weekly / monthly / all
     */
    private String period;

    /**
     * 领域（标签名称或 "all"）
     */
    private String domain;

    /**
     * 排名
     */
    private Integer rankPosition;

    /**
     * 当期综合得分
     */
    private Long score;

    /**
     * 快照日期
     */
    private LocalDate snapshotDate;

    private LocalDateTime createTime;
}
