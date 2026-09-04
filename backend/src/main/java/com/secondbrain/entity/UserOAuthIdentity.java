package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 第三方登录身份映射实体。
 *
 * 通过独立表保存微信等平台身份，避免为兼容移动端登录而修改既有用户表。
 *
 * @author AI
 */
@Getter
@Setter
@TableName("user_oauth_identity")
public class UserOAuthIdentity {

    /** 映射记录ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台用户ID。 */
    private Long userId;

    /** 身份提供方。 */
    private String provider;

    /** 提供方用户标识，例如微信 openid。 */
    private String providerUserId;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
