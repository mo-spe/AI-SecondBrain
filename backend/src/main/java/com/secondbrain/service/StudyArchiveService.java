package com.secondbrain.service;

import com.secondbrain.dto.StudyArchiveRequests;
import com.secondbrain.dto.StudyArchiveViews;
import com.secondbrain.entity.DoubtRecord;
import com.secondbrain.entity.WrongQuestionRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理用户私有的考研错题、疑问和个人图片。
 */
public interface StudyArchiveService {

    /**
     * 将学习资料图片保存到当前用户的私有目录。
     *
     * @param file 用户上传的图片
     * @param userId 当前用户标识
     * @return 图片文件名
     */
    String uploadImage(MultipartFile file, Long userId);

    /**
     * 读取当前用户的私有学习图片。
     *
     * @param fileName 图片文件名
     * @param userId 当前用户标识
     * @return 图片字节
     * @throws IOException 文件读取失败时抛出
     */
    byte[] readImage(String fileName, Long userId) throws IOException;

    /**
     * 使用当前用户配置的 AI 场景为 OCR 文本生成可编辑建议。
     *
     * @param text OCR 或用户输入内容
     * @param userId 当前用户标识
     * @return AI 结构化建议
     */
    StudyArchiveViews.AiSuggestion suggestWrongQuestion(String text, Long userId);

    /**
     * 用户主动使用个人视觉模型配置识别一道错题。
     *
     * @param file 原题图片
     * @param userId 当前用户标识
     * @return 可编辑的识别候选
     */
    StudyArchiveViews.VisualSuggestion recognizeWrongQuestion(MultipartFile file, Long userId);

    /**
     * 创建错题档案。
     *
     * @param request 档案信息
     * @param userId 当前用户标识
     * @param workspaceId 当前工作区标识
     * @return 新建错题
     */
    WrongQuestionRecord createWrongQuestion(StudyArchiveRequests.CreateWrongQuestion request, Long userId, Long workspaceId);

    /**
     * 查询当前用户的错题档案。
     *
     * @param userId 当前用户标识
     * @param status 状态过滤，可为空
     * @param keyword 关键词过滤，可为空
     * @return 错题列表
     */
    List<WrongQuestionRecord> listWrongQuestions(Long userId, String status, String keyword);

    /**
     * 获取错题和复盘历史。
     *
     * @param id 错题标识
     * @param userId 当前用户标识
     * @return 错题详情
     */
    StudyArchiveViews.WrongQuestionDetail getWrongQuestion(Long id, Long userId);

    /**
     * 更新错题的用户确认信息。
     *
     * @param id 错题标识
     * @param request 更新内容
     * @param userId 当前用户标识
     * @return 更新后的错题
     */
    WrongQuestionRecord updateWrongQuestion(Long id, StudyArchiveRequests.UpdateWrongQuestion request, Long userId);

    /**
     * 设置或取消错题复习时间。
     *
     * @param id 错题标识
     * @param scheduledAt 用户选择时间；为空时取消
     * @param userId 当前用户标识
     * @return 更新后的错题
     */
    WrongQuestionRecord scheduleWrongQuestion(Long id, LocalDateTime scheduledAt, Long userId);

    /**
     * 追加一次错题手动复盘历史。
     *
     * @param id 错题标识
     * @param request 复盘结果
     * @param userId 当前用户标识
     * @return 最新错题状态
     */
    WrongQuestionRecord reviewWrongQuestion(Long id, StudyArchiveRequests.WrongQuestionReview request, Long userId);

    /**
     * 将错题标记为已掌握。
     *
     * @param id 错题标识
     * @param userId 当前用户标识
     * @return 更新后的错题
     */
    WrongQuestionRecord masterWrongQuestion(Long id, Long userId);

    /**
     * 软删除错题档案。
     *
     * @param id 错题标识
     * @param userId 当前用户标识
     */
    void deleteWrongQuestion(Long id, Long userId);

    /**
     * 查询当前用户到期的错题和已安排疑问。
     *
     * @param userId 当前用户标识
     * @param now 服务端当前时间
     * @return 到期错题和疑问
     */
    List<WrongQuestionRecord> dueWrongQuestions(Long userId, LocalDateTime now);

    /**
     * 创建个人疑问档案。
     *
     * @param request 疑问信息
     * @param userId 当前用户标识
     * @param workspaceId 当前工作区标识
     * @return 新建疑问
     */
    DoubtRecord createDoubt(StudyArchiveRequests.CreateDoubt request, Long userId, Long workspaceId);

    /**
     * 查询当前用户的疑问。
     *
     * @param userId 当前用户标识
     * @param status 状态过滤，可为空
     * @param keyword 关键词过滤，可为空
     * @return 疑问列表
     */
    List<DoubtRecord> listDoubts(Long userId, String status, String keyword);

    /**
     * 获取疑问和理解历史。
     *
     * @param id 疑问标识
     * @param userId 当前用户标识
     * @return 疑问详情
     */
    StudyArchiveViews.DoubtDetail getDoubt(Long id, Long userId);

    /**
     * 更新疑问来源或状态。
     *
     * @param id 疑问标识
     * @param request 更新内容
     * @param userId 当前用户标识
     * @return 更新后的疑问
     */
    DoubtRecord updateDoubt(Long id, StudyArchiveRequests.UpdateDoubt request, Long userId);

    /**
     * 设置或取消疑问的下次处理时间。
     *
     * @param id 疑问标识
     * @param scheduledAt 用户选择时间；为空时取消
     * @param userId 当前用户标识
     * @return 更新后的疑问
     */
    DoubtRecord scheduleDoubt(Long id, LocalDateTime scheduledAt, Long userId);

    /**
     * 追加一条个人理解历史。
     *
     * @param id 疑问标识
     * @param request 理解内容
     * @param userId 当前用户标识
     * @return 疑问详情
     */
    StudyArchiveViews.DoubtDetail addUnderstanding(Long id, StudyArchiveRequests.AddUnderstanding request, Long userId);

    /**
     * 生成个人疑问的 AI 参考解释。
     *
     * @param id 疑问标识
     * @param userId 当前用户标识
     * @return AI 参考解释
     */
    String explainDoubt(Long id, Long userId);

    /**
     * 查询到期且尚未解决的疑问。
     *
     * @param userId 当前用户标识
     * @param now 服务端当前时间
     * @return 到期疑问
     */
    List<DoubtRecord> dueDoubts(Long userId, LocalDateTime now);

    /**
     * 软删除疑问档案。
     *
     * @param id 疑问标识
     * @param userId 当前用户标识
     */
    void deleteDoubt(Long id, Long userId);
}
