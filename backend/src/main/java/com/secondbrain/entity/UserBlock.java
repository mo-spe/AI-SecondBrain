package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 用户拉黑关系实体。 */
@Getter
@Setter
@TableName("user_block")
public class UserBlock {

    /** 拉黑关系ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起拉黑的用户ID。 */
    private Long blockerId;

    /** 被拉黑用户ID。 */
    private Long blockedId;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
