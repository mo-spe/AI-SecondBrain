package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 社区公开资料实体。 */
@Getter
@Setter
@TableName("community_user_profile")
public class CommunityUserProfile {

    /** 用户ID，同时作为资料主键。 */
    @TableId(type = IdType.INPUT)
    private Long userId;

    /** 面向社区展示的公开简介。 */
    private String introduction;

    /** 擅长领域标签JSON。 */
    private String expertiseTagsJson;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
