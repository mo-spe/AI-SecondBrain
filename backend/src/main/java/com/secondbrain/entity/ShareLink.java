package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 分享链接实体（预留V1.1）.
 * <p>存储知识节点对外分享链接信息，token使用SHA-256随机生成不可猜测</p>
 */
@Getter
@Setter
@TableName("share_link")
public class ShareLink {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识节点ID
     */
    private Long nodeId;

    /**
     * 分享者用户ID
     */
    private Long ownerId;

    /**
     * 随机分享令牌（64字符，SHA-256生成）
     */
    private String token;

    /**
     * 有效期类型：permanent/7d/24h
     */
    private String expireType;

    /**
     * 具体过期时间（permanent时为null）
     */
    private LocalDateTime expiresAt;

    /**
     * 累计访问次数
     */
    private Integer accessCount;

    /**
     * 是否已撤销（0-未撤销，1-已撤销）
     */
    private Integer isRevoked;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
