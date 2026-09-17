package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 用户单向关注关系实体。 */
@Getter
@Setter
@TableName("user_follow")
public class UserFollow {

    /** 关注关系ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起关注的用户ID。 */
    private Long followerId;

    /** 被关注用户ID。 */
    private Long followedId;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
