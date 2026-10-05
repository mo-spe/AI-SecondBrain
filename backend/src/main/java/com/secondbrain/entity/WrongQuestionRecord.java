package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户私有的考研错题档案。
 *
 * @author AI
 */
@Getter
@Setter
@TableName("wrong_question_record")
public class WrongQuestionRecord {

    /** 错题档案标识。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 档案所属用户。 */
    private Long userId;

    /** 创建档案时所在工作区，仅用于来源上下文。 */
    private Long workspaceId;

    /** 用户错题图片相对存储路径。 */
    private String imagePath;

    /** 本机 OCR 识别文本。 */
    private String ocrText;

    /** 用户作答内容，复盘时默认隐藏。 */
    private String userAnswer;

    /** 用户确认的正确答案。 */
    private String correctAnswer;

    /** 用户确认的题目解析。 */
    private String explanation;

    /** 学习科目。 */
    private String subject;

    /** 习题册或资料名称。 */
    private String sourceBook;

    /** 原题页码。 */
    private String sourcePage;

    /** 所属章节。 */
    private String chapter;

    /** 用户确认的知识点，以 JSON 数组保存。 */
    private String knowledgePoints;

    /** 用户确认的错误类型。 */
    private String errorType;

    /** 个人备注。 */
    private String userNote;

    /** 可选 AI 建议原文，以 JSON 保存并与用户确认字段分开。 */
    private String aiSuggestionJson;

    /** AI 建议整体置信度。 */
    private Double aiConfidence;

    /** 用户控制的复习状态。 */
    private String reviewStatus;

    /** 用户主动设置的下次复习时间。 */
    private LocalDateTime nextReviewTime;

    /** 最近一次手动复盘时间。 */
    private LocalDateTime lastReviewTime;

    /** 软删除标记。 */
    private Integer isDeleted;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
