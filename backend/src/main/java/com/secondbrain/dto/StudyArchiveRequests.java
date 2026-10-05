package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考研学习档案接口请求对象。
 */
public final class StudyArchiveRequests {

    private StudyArchiveRequests() {
    }

    /** 新建错题档案请求。 */
    @Getter
    @Setter
    public static class CreateWrongQuestion {
        /** 图片接口返回的个人文件名。 */
        @NotBlank
        private String imagePath;
        /** OCR 文本。 */
        private String ocrText;
        /** 用户作答内容。 */
        private String userAnswer;
        /** 用户确认的正确答案。 */
        private String correctAnswer;
        /** 用户确认的题目解析。 */
        private String explanation;
        /** 学习科目。 */
        private String subject;
        /** 书籍或资料名。 */
        private String sourceBook;
        /** 页码。 */
        private String sourcePage;
        /** 章节。 */
        private String chapter;
        /** 确认的知识点。 */
        private List<String> knowledgePoints;
        /** 确认的错误类型。 */
        private String errorType;
        /** 个人备注。 */
        private String userNote;
        /** 已由用户确认或修改的 AI 建议快照。 */
        private String aiSuggestionJson;
        /** AI 建议的整体置信度。 */
        private Double aiConfidence;
        /** 创建时用户所在工作区。 */
        private Long workspaceId;
    }

    /** 修改错题元信息请求。 */
    @Getter
    @Setter
    public static class UpdateWrongQuestion {
        /** 学习科目。 */
        private String subject;
        /** OCR 题面文字。 */
        private String ocrText;
        /** 用户作答内容。 */
        private String userAnswer;
        /** 用户确认的正确答案。 */
        private String correctAnswer;
        /** 用户确认的题目解析。 */
        private String explanation;
        /** 书籍或资料名。 */
        private String sourceBook;
        /** 页码。 */
        private String sourcePage;
        /** 章节。 */
        private String chapter;
        /** 确认的知识点。 */
        private List<String> knowledgePoints;
        /** 确认的错误类型。 */
        private String errorType;
        /** 个人备注。 */
        private String userNote;
    }

    /** 用户安排或取消错题复习的请求。 */
    @Getter
    @Setter
    public static class Schedule {
        /** 用户选择的下次时间；为空时取消排期。 */
        private LocalDateTime scheduledAt;
    }

    /** 记录错题复盘结果的请求。 */
    @Getter
    @Setter
    public static class WrongQuestionReview {
        /** 本次结果：CORRECT、INCORRECT 或 VIEWED。 */
        @NotBlank
        private String result;
        /** 本次复盘备注。 */
        private String note;
    }

    /** 新建疑问请求。 */
    @Getter
    @Setter
    public static class CreateDoubt {
        /** 用户提出的问题。 */
        @NotBlank
        private String content;
        /** 可选的个人图片文件名。 */
        private String imagePath;
        /** 书籍或资料名。 */
        private String sourceBook;
        /** 页码。 */
        private String sourcePage;
        /** 章节。 */
        private String chapter;
        /** 疑问类型。 */
        private String doubtType;
        /** 创建时用户所在工作区。 */
        private Long workspaceId;
    }

    /** 修改疑问来源或处理状态请求。 */
    @Getter
    @Setter
    public static class UpdateDoubt {
        /** 疑问正文。 */
        private String content;
        /** 书籍或资料名。 */
        private String sourceBook;
        /** 页码。 */
        private String sourcePage;
        /** 章节。 */
        private String chapter;
        /** 疑问类型。 */
        private String doubtType;
        /** PENDING、UNDERSTOOD、VERIFYING 或 RESOLVED。 */
        private String status;
        /** 最近一次 AI 解释反馈：HELPFUL 或 NOT_HELPFUL。 */
        private String aiExplanationFeedback;
    }

    /** 追加疑问理解记录请求。 */
    @Getter
    @Setter
    public static class AddUnderstanding {
        /** 本次理解正文。 */
        @NotBlank
        private String content;
        /** INITIAL、VERIFYING 或 CONFIRMED。 */
        private String understandingStatus;
    }
}
