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
 * 社区问题回答实体。
 *
 * @author AI
 */
@Getter
@Setter
@TableName("community_answer")
public class CommunityAnswer {

    /** 回答ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属问题ID。 */
    private Long questionId;

    /** 回答者ID。 */
    private Long authorId;

    /** Markdown回答正文。 */
    private String content;

    /** 经用户确认公开的知识点快照JSON。 */
    private String knowledgeSnapshotsJson;

    /** 是否被问题作者采纳。 */
    private Integer accepted;

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
