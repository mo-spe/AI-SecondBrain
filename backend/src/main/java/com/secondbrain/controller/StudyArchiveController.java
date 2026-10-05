package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.StudyArchiveRequests;
import com.secondbrain.dto.StudyArchiveViews;
import com.secondbrain.entity.DoubtRecord;
import com.secondbrain.entity.WrongQuestionRecord;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.service.StudyArchiveService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Android 考研错题与疑问档案接口。
 *
 * <p>所有档案只按 JWT 中的用户身份访问，图片文件名不能绕过用户目录权限。</p>
 *
 * @author AI
 */
@RestController
@RequestMapping("/study")
public class StudyArchiveController {

    private final StudyArchiveService studyArchiveService;

    /**
     * 构造学习档案接口控制器。
     *
     * @param studyArchiveService 错题和疑问业务服务
     */
    public StudyArchiveController(StudyArchiveService studyArchiveService) {
        this.studyArchiveService = studyArchiveService;
    }

    /**
     * 保存一张当前用户私有的学习图片。
     *
     * @param file 图片文件
     * @param request HTTP 请求
     * @return 私有图片文件名
     */
    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        return Result.success("图片上传成功", studyArchiveService.uploadImage(file, userId(request)));
    }

    /**
     * 读取当前用户自己的学习图片。
     *
     * @param fileName 文件名
     * @param request HTTP 请求
     * @return 图片内容
     * @throws IOException 文件读取失败
     */
    @GetMapping("/media/{fileName}")
    public ResponseEntity<byte[]> readImage(@PathVariable String fileName, HttpServletRequest request) throws IOException {
        byte[] bytes = studyArchiveService.readImage(fileName, userId(request));
        MediaType mediaType = mediaType(fileName);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(bytes);
    }

    /**
     * 为 OCR 文本生成可编辑错题整理建议。
     *
     * @param text OCR 文本
     * @param request HTTP 请求
     * @return AI 建议
     */
    @PostMapping("/wrong-questions/suggestions")
    public Result<StudyArchiveViews.AiSuggestion> suggestWrongQuestion(@RequestBody SuggestionRequest text,
                                                                        HttpServletRequest request) {
        return Result.success(studyArchiveService.suggestWrongQuestion(text.text(), userId(request)));
    }

    /**
     * 仅在用户点击精准识别后转发原题图片到个人配置的视觉模型。
     *
     * @param file 原题图片
     * @param request HTTP 请求
     * @return 待用户核对的题目和公式
     */
    @PostMapping(value = "/wrong-questions/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<StudyArchiveViews.VisualSuggestion> recognizeWrongQuestion(
            @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        return Result.success(studyArchiveService.recognizeWrongQuestion(file, userId(request)));
    }

    /**
     * 创建错题档案。
     *
     * @param body 错题内容
     * @param request HTTP 请求
     * @return 创建后的错题
     */
    @PostMapping("/wrong-questions")
    public Result<WrongQuestionRecord> createWrongQuestion(@Valid @RequestBody StudyArchiveRequests.CreateWrongQuestion body,
                                                            HttpServletRequest request) {
        return Result.success(studyArchiveService.createWrongQuestion(body, userId(request), workspaceId(request)));
    }

    /**
     * 查询当前用户的错题档案。
     *
     * @param status 状态过滤
     * @param keyword 搜索词
     * @param request HTTP 请求
     * @return 错题列表
     */
    @GetMapping("/wrong-questions")
    public Result<List<WrongQuestionRecord>> listWrongQuestions(@RequestParam(required = false) String status,
                                                                 @RequestParam(required = false) String keyword,
                                                                 HttpServletRequest request) {
        return Result.success(studyArchiveService.listWrongQuestions(userId(request), status, keyword));
    }

    /**
     * 查询错题详情和复盘历史。
     *
     * @param id 错题标识
     * @param request HTTP 请求
     * @return 错题详情
     */
    @GetMapping("/wrong-questions/{id}")
    public Result<StudyArchiveViews.WrongQuestionDetail> getWrongQuestion(@PathVariable Long id,
                                                                           HttpServletRequest request) {
        return Result.success(studyArchiveService.getWrongQuestion(id, userId(request)));
    }

    /**
     * 修改用户确认的错题元信息。
     *
     * @param id 错题标识
     * @param body 更新内容
     * @param request HTTP 请求
     * @return 更新后的错题
     */
    @PatchMapping("/wrong-questions/{id}")
    public Result<WrongQuestionRecord> updateWrongQuestion(@PathVariable Long id,
                                                            @RequestBody StudyArchiveRequests.UpdateWrongQuestion body,
                                                            HttpServletRequest request) {
        return Result.success(studyArchiveService.updateWrongQuestion(id, body, userId(request)));
    }

    /**
     * 安排或取消错题复习时间。
     *
     * @param id 错题标识
     * @param body 用户选定时间
     * @param request HTTP 请求
     * @return 更新后的错题
     */
    @PostMapping("/wrong-questions/{id}/schedule")
    public Result<WrongQuestionRecord> scheduleWrongQuestion(@PathVariable Long id,
                                                             @RequestBody StudyArchiveRequests.Schedule body,
                                                             HttpServletRequest request) {
        return Result.success(studyArchiveService.scheduleWrongQuestion(id, body.getScheduledAt(), userId(request)));
    }

    /**
     * 记录一次错题复盘，但不自动安排下一次时间。
     *
     * @param id 错题标识
     * @param body 复盘结果
     * @param request HTTP 请求
     * @return 更新后的错题
     */
    @PostMapping("/wrong-questions/{id}/reviews")
    public Result<WrongQuestionRecord> reviewWrongQuestion(@PathVariable Long id,
                                                           @Valid @RequestBody StudyArchiveRequests.WrongQuestionReview body,
                                                           HttpServletRequest request) {
        return Result.success(studyArchiveService.reviewWrongQuestion(id, body, userId(request)));
    }

    /**
     * 手动标记错题已掌握。
     *
     * @param id 错题标识
     * @param request HTTP 请求
     * @return 更新后的错题
     */
    @PostMapping("/wrong-questions/{id}/master")
    public Result<WrongQuestionRecord> masterWrongQuestion(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(studyArchiveService.masterWrongQuestion(id, userId(request)));
    }

    /**
     * 归档错题并停止其待复习状态。
     *
     * @param id 错题标识
     * @param request HTTP 请求
     * @return 操作结果
     */
    @DeleteMapping("/wrong-questions/{id}")
    public Result<Void> deleteWrongQuestion(@PathVariable Long id, HttpServletRequest request) {
        studyArchiveService.deleteWrongQuestion(id, userId(request));
        return Result.success();
    }

    /**
     * 查询当前用户到期的错题与疑问。
     *
     * @param request HTTP 请求
     * @return 今日到期档案
     */
    @GetMapping("/today")
    public Result<StudyArchiveViews.TodayArchives> today(HttpServletRequest request) {
        Long userId = userId(request);
        LocalDateTime now = LocalDateTime.now();
        return Result.success(new StudyArchiveViews.TodayArchives(
                studyArchiveService.dueWrongQuestions(userId, now), studyArchiveService.dueDoubts(userId, now)));
    }

    /**
     * 创建个人疑问档案。
     *
     * @param body 疑问信息
     * @param request HTTP 请求
     * @return 新建疑问
     */
    @PostMapping("/doubts")
    public Result<DoubtRecord> createDoubt(@Valid @RequestBody StudyArchiveRequests.CreateDoubt body,
                                            HttpServletRequest request) {
        return Result.success(studyArchiveService.createDoubt(body, userId(request), workspaceId(request)));
    }

    /**
     * 查询个人疑问档案。
     *
     * @param status 状态过滤
     * @param keyword 搜索词
     * @param request HTTP 请求
     * @return 疑问列表
     */
    @GetMapping("/doubts")
    public Result<List<DoubtRecord>> listDoubts(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword,
                                                HttpServletRequest request) {
        return Result.success(studyArchiveService.listDoubts(userId(request), status, keyword));
    }

    /**
     * 查询疑问详情和理解历史。
     *
     * @param id 疑问标识
     * @param request HTTP 请求
     * @return 疑问详情
     */
    @GetMapping("/doubts/{id}")
    public Result<StudyArchiveViews.DoubtDetail> getDoubt(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(studyArchiveService.getDoubt(id, userId(request)));
    }

    /**
     * 修改疑问来源或处理状态。
     *
     * @param id 疑问标识
     * @param body 更新内容
     * @param request HTTP 请求
     * @return 更新后的疑问
     */
    @PatchMapping("/doubts/{id}")
    public Result<DoubtRecord> updateDoubt(@PathVariable Long id, @RequestBody StudyArchiveRequests.UpdateDoubt body,
                                           HttpServletRequest request) {
        return Result.success(studyArchiveService.updateDoubt(id, body, userId(request)));
    }

    /**
     * 安排或取消疑问的下次处理时间。
     *
     * @param id 疑问标识
     * @param body 用户选定时间
     * @param request HTTP 请求
     * @return 更新后的疑问
     */
    @PostMapping("/doubts/{id}/schedule")
    public Result<DoubtRecord> scheduleDoubt(@PathVariable Long id, @RequestBody StudyArchiveRequests.Schedule body,
                                              HttpServletRequest request) {
        return Result.success(studyArchiveService.scheduleDoubt(id, body.getScheduledAt(), userId(request)));
    }

    /**
     * 追加个人对疑问的理解。
     *
     * @param id 疑问标识
     * @param body 理解内容
     * @param request HTTP 请求
     * @return 更新后的详情
     */
    @PostMapping("/doubts/{id}/understandings")
    public Result<StudyArchiveViews.DoubtDetail> addUnderstanding(@PathVariable Long id,
                                                                   @Valid @RequestBody StudyArchiveRequests.AddUnderstanding body,
                                                                   HttpServletRequest request) {
        return Result.success(studyArchiveService.addUnderstanding(id, body, userId(request)));
    }

    /**
     * 生成不会自动改变疑问状态的 AI 参考解释。
     *
     * @param id 疑问标识
     * @param request HTTP 请求
     * @return AI 解释文本
     */
    @PostMapping("/doubts/{id}/analyze")
    public Result<String> explainDoubt(@PathVariable Long id, HttpServletRequest request) {
        return Result.success("AI 解释已生成", studyArchiveService.explainDoubt(id, userId(request)));
    }

    /**
     * 归档疑问。
     *
     * @param id 疑问标识
     * @param request HTTP 请求
     * @return 操作结果
     */
    @DeleteMapping("/doubts/{id}")
    public Result<Void> deleteDoubt(@PathVariable Long id, HttpServletRequest request) {
        studyArchiveService.deleteDoubt(id, userId(request));
        return Result.success();
    }

    private Long userId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "请先登录");
        return userId;
    }

    private Long workspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    private MediaType mediaType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "heic" -> MediaType.parseMediaType("image/heic");
            default -> MediaType.IMAGE_JPEG;
        };
    }

    /** 图片 OCR 文本请求。 */
    public record SuggestionRequest(String text) {
    }
}
