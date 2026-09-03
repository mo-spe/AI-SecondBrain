package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 社区问题实体。
 *
 * @author AI
 */
@Getter
@Setter
@TableName("community_question")
public class CommunityQuestion {

    /** 问题ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布者ID。 */
    private Long authorId;

    /** 问题标题。 */
    private String title;

    /** 问题正文。 */
    private String content;

    /** 标签JSON数组。 */
    private String tagsJson;

    /** 状态：OPEN/CLOSED。 */
    private String status;

    /** 回答数量冗余计数器。 */
    private Integer answerCount;

    /** 浏览数量冗余计数器。 */
    private Integer viewCount;

    /** 被采纳的回答ID。 */
    private Long acceptedAnswerId;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记。 */
    @TableLogic
    private Integer deleted;
}
