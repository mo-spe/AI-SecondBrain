package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户私有的学习疑问档案。
 *
 * @author AI
 */
@Getter
@Setter
@TableName("doubt_record")
public class DoubtRecord {

    /** 疑问档案标识。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 档案所属用户。 */
    private Long userId;

    /** 创建档案时所在工作区，仅用于来源上下文。 */
    private Long workspaceId;

    /** 疑问照片相对存储路径。 */
    private String imagePath;

    /** 疑问正文。 */
    private String content;

    /** 书籍或资料名称。 */
    private String sourceBook;

    /** 原文页码。 */
    private String sourcePage;

    /** 所属章节。 */
    private String chapter;

    /** 疑问类型。 */
    private String doubtType;

    /** 疑问处理状态。 */
    private String status;

    /** 用户对最近一次 AI 参考解释的反馈。 */
    private String aiExplanationFeedback;

    /** 用户主动设置的下次处理时间。 */
    private LocalDateTime nextProcessTime;

    /** 软删除标记。 */
    private Integer isDeleted;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
