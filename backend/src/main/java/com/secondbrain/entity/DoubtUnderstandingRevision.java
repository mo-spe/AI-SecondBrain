package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 疑问的追加式个人理解记录。
 *
 * @author AI
 */
@Getter
@Setter
@TableName("doubt_understanding_revision")
public class DoubtUnderstandingRevision {

    /** 理解记录标识。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属疑问档案。 */
    private Long doubtId;

    /** 记录所属用户。 */
    private Long userId;

    /** 本次理解正文。 */
    private String content;

    /** 本次理解状态：INITIAL、VERIFYING 或 CONFIRMED。 */
    private String understandingStatus;

    /** 理解记录创建时间。 */
    private LocalDateTime createTime;
}
