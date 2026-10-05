package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 错题的手动复盘记录。
 *
 * @author AI
 */
@Getter
@Setter
@TableName("wrong_question_review_log")
public class WrongQuestionReviewLog {

    /** 复盘记录标识。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联错题档案。 */
    private Long wrongQuestionId;

    /** 复盘记录所属用户。 */
    private Long userId;

    /** 本次结果：CORRECT、INCORRECT 或 VIEWED。 */
    private String result;

    /** 本次复盘备注。 */
    private String note;

    /** 复盘发生时间。 */
    private LocalDateTime createTime;
}
