package com.secondbrain.dto;

import com.secondbrain.entity.DoubtRecord;
import com.secondbrain.entity.DoubtUnderstandingRevision;
import com.secondbrain.entity.WrongQuestionRecord;
import com.secondbrain.entity.WrongQuestionReviewLog;

import java.util.List;

/** 考研学习档案 API 响应对象。 */
public final class StudyArchiveViews {

    private StudyArchiveViews() {
    }

    /** 错题详情和手动复盘历史。 */
    public record WrongQuestionDetail(WrongQuestionRecord record, List<WrongQuestionReviewLog> reviews) {
    }

    /** 疑问详情和追加式理解历史。 */
    public record DoubtDetail(DoubtRecord record, List<DoubtUnderstandingRevision> understandings) {
    }

    /** AI 对错题文本的可编辑建议。 */
    public record AiSuggestion(String raw, String subject, String chapter, List<String> knowledgePoints,
                               String errorType, Double confidence) {
    }

    /** 由用户主动启用的视觉模型返回的可编辑题目候选。 */
    public record VisualSuggestion(String raw, String questionText, String subject, String chapter,
                                   List<String> knowledgePoints, String errorType,
                                   List<String> needsConfirmation) {
    }

    /** 今日到期的错题和疑问。 */
    public record TodayArchives(List<WrongQuestionRecord> wrongQuestions, List<DoubtRecord> doubts) {
    }
}
