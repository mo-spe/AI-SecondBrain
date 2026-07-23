package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 收藏记录实体.
 *
 * <p>通过数据库唯一约束保证同一用户对同一帖子只能收藏一次。</p>
 */
@Getter
@Setter
@TableName("square_bookmark")
public class SquareBookmark {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 收藏用户ID
     */
    private Long userId;

    /**
     * 收藏时间
     */
    private LocalDateTime createdAt;
}
