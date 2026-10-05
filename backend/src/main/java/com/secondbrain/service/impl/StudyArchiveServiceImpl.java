package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.dto.StudyArchiveRequests;
import com.secondbrain.dto.StudyArchiveViews;
import com.secondbrain.entity.DoubtRecord;
import com.secondbrain.entity.DoubtUnderstandingRevision;
import com.secondbrain.entity.WrongQuestionRecord;
import com.secondbrain.entity.WrongQuestionReviewLog;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.DoubtRecordMapper;
import com.secondbrain.mapper.DoubtUnderstandingRevisionMapper;
import com.secondbrain.mapper.WrongQuestionRecordMapper;
import com.secondbrain.mapper.WrongQuestionReviewLogMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.StudyArchiveService;
import com.secondbrain.util.VisionImageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 考研错题与疑问档案服务。
 *
 * <p>错题排期只在用户明确设置时更新；理解记录采用追加方式，避免用户的新理解覆盖历史。</p>
 *
 * @author AI
 */
@Service
public class StudyArchiveServiceImpl implements StudyArchiveService {

    private static final Logger log = LoggerFactory.getLogger(StudyArchiveServiceImpl.class);
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final List<String> IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp", "heic");
    private static final List<String> WRONG_STATUSES = List.of("UNSCHEDULED", "SCHEDULED", "MASTERED", "ARCHIVED");
    private static final List<String> DOUBT_STATUSES = List.of("PENDING", "UNDERSTOOD", "VERIFYING", "RESOLVED");

    private final WrongQuestionRecordMapper wrongQuestionMapper;
    private final WrongQuestionReviewLogMapper wrongQuestionReviewLogMapper;
    private final DoubtRecordMapper doubtMapper;
    private final DoubtUnderstandingRevisionMapper understandingMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    /**
     * 构造学习档案服务及其私有文件存储根目录。
     *
     * @param wrongQuestionMapper 错题档案数据访问接口
     * @param wrongQuestionReviewLogMapper 错题复盘历史数据访问接口
     * @param doubtMapper 疑问档案数据访问接口
     * @param understandingMapper 疑问理解历史数据访问接口
     * @param aiService 现有 AI 服务
     * @param objectMapper JSON 序列化工具
     * @param storagePath 学习图片的持久化根目录
     */
    public StudyArchiveServiceImpl(WrongQuestionRecordMapper wrongQuestionMapper,
                                   WrongQuestionReviewLogMapper wrongQuestionReviewLogMapper,
                                   DoubtRecordMapper doubtMapper,
                                   DoubtUnderstandingRevisionMapper understandingMapper,
                                   AiService aiService,
                                   ObjectMapper objectMapper,
                                   @Value("${study-archive.storage-path:uploads/study}") String storagePath) {
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.wrongQuestionReviewLogMapper = wrongQuestionReviewLogMapper;
        this.doubtMapper = doubtMapper;
        this.understandingMapper = understandingMapper;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    /** {@inheritDoc} */
    @Override
    public String uploadImage(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException(400, "图片不能为空且不能超过 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException(400, "只支持图片文件");
        }
        String extension = fileExtension(file.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "图片格式仅支持 JPG、PNG、WEBP 或 HEIC");
        }
        String fileName = UUID.randomUUID() + "." + extension;
        Path userDirectory = storageRoot.resolve(String.valueOf(userId)).normalize();
        try {
            Files.createDirectories(userDirectory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, userDirectory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
            return fileName;
        } catch (IOException exception) {
            throw new BusinessException(500, "图片保存失败，请稍后重试", exception);
        }
    }

    /** {@inheritDoc} */
    @Override
    public byte[] readImage(String fileName, Long userId) throws IOException {
        return Files.readAllBytes(resolveOwnedImage(fileName, userId));
    }

    /** {@inheritDoc} */
    @Override
    public StudyArchiveViews.AiSuggestion suggestWrongQuestion(String text, Long userId) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(400, "请先识别或输入题目内容");
        }
        String prompt = "根据用户提供的考研错题 OCR 文本，只返回 JSON，不要 markdown。字段为 subject、chapter、knowledgePoints(字符串数组)、errorType、confidence(0到1)。"
                + "不要推测书名或页码；不确定时用空字符串。错误类型只能从概念不清、公式记错、计算错误、审题错误、方法不会、粗心、不确定中选。"
                + "当前只提供题面 OCR，错误原因可能无法判断，请降低 confidence 并使用不确定。文本：\n" + text;
        String response;
        try {
            response = aiService.generateAnswer(userId, "extraction", prompt);
        } catch (RuntimeException exception) {
            log.info("study_archive_ai_suggestion_unavailable userId={} reason={}", userId, exception.getMessage());
            throw new BusinessException(503, "AI 建议暂时不可用，你仍可手动整理错题");
        }
        String json = extractJsonObject(response);
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> knowledgePoints = root.path("knowledgePoints").isArray()
                    ? objectMapper.convertValue(root.path("knowledgePoints"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class))
                    : List.of();
            return new StudyArchiveViews.AiSuggestion(
                    json,
                    root.path("subject").asText(""),
                    root.path("chapter").asText(""),
                    knowledgePoints,
                    root.path("errorType").asText("不确定"),
                    root.path("confidence").isNumber() ? root.path("confidence").asDouble() : null);
        } catch (IOException exception) {
            throw new BusinessException(502, "AI 返回格式无法识别，请手动整理错题");
        }
    }

    /** {@inheritDoc} */
    @Override
    public StudyArchiveViews.VisualSuggestion recognizeWrongQuestion(MultipartFile file, Long userId) {
        byte[] image = readVisionImage(file);
        String prompt = "请只根据图片中可见内容识别这一道考研错题。只返回 JSON 对象："
                + "questionText(题干和选项，数学公式尽量用 LaTeX)、subject、chapter、"
                + "knowledgePoints(字符串数组)、errorType、needsConfirmation(字符串数组)。"
                + "图片中有多题时只取最清晰完整的一道，并在 needsConfirmation 写明题号需核对；"
                + "模糊的数字、符号、页码不要猜测。没有用户解题过程时 errorType 必须为不确定。";
        String response = aiService.analyzeImages(userId, "vision", prompt,
                List.of(image), List.of(file.getContentType()));
        try {
            String json = extractJsonObject(response);
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new BusinessException(502, "视觉模型没有返回题目对象，请对照原图手动整理");
            }
            List<String> points = readStringArray(root.path("knowledgePoints"));
            List<String> uncertainties = readStringArray(root.path("needsConfirmation"));
            String errorType = root.path("errorType").asText("不确定");
            return new StudyArchiveViews.VisualSuggestion(json,
                    root.path("questionText").asText(""), root.path("subject").asText(""),
                    root.path("chapter").asText(""), points,
                    errorType.isBlank() ? "不确定" : errorType, uncertainties);
        } catch (IOException exception) {
            throw new BusinessException(502, "视觉模型结果无法解析，请对照原图手动整理");
        }
    }

    private List<String> readStringArray(JsonNode node) {
        return node.isArray() ? objectMapper.convertValue(node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)) : List.of();
    }

    private byte[] readVisionImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException(400, "请选择不超过 10 MB 的原题图片");
        }
        String mime = file.getContentType();
        if (!("image/jpeg".equals(mime) || "image/png".equals(mime))) {
            throw new BusinessException(400, "精准识别只支持 JPEG 或 PNG 图片");
        }
        try {
            byte[] bytes = file.getBytes();
            VisionImageValidator.validate(bytes, mime);
            return bytes;
        } catch (IOException exception) {
            throw new BusinessException(400, "图片读取失败，请重新选择");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public WrongQuestionRecord createWrongQuestion(StudyArchiveRequests.CreateWrongQuestion request, Long userId, Long workspaceId) {
        validateOwnedImage(request.getImagePath(), userId);
        WrongQuestionRecord record = new WrongQuestionRecord();
        record.setUserId(userId);
        record.setWorkspaceId(workspaceId);
        record.setImagePath(request.getImagePath());
        record.setOcrText(request.getOcrText());
        record.setUserAnswer(request.getUserAnswer());
        record.setCorrectAnswer(request.getCorrectAnswer());
        record.setExplanation(request.getExplanation());
        record.setSubject(request.getSubject());
        record.setSourceBook(request.getSourceBook());
        record.setSourcePage(request.getSourcePage());
        record.setChapter(request.getChapter());
        record.setKnowledgePoints(writeJson(request.getKnowledgePoints()));
        record.setErrorType(request.getErrorType());
        record.setUserNote(request.getUserNote());
        record.setAiSuggestionJson(request.getAiSuggestionJson());
        record.setAiConfidence(request.getAiConfidence());
        record.setReviewStatus("UNSCHEDULED");
        record.setIsDeleted(0);
        wrongQuestionMapper.insert(record);
        return record;
    }

    /** {@inheritDoc} */
    @Override
    public List<WrongQuestionRecord> listWrongQuestions(Long userId, String status, String keyword) {
        LambdaQueryWrapper<WrongQuestionRecord> wrapper = new LambdaQueryWrapper<WrongQuestionRecord>()
                .eq(WrongQuestionRecord::getUserId, userId)
                .eq(WrongQuestionRecord::getIsDeleted, 0);
        if (status != null && !status.isBlank()) {
            validateStatus(status, WRONG_STATUSES);
            wrapper.eq(WrongQuestionRecord::getReviewStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(query -> query.like(WrongQuestionRecord::getSubject, keyword)
                    .or().like(WrongQuestionRecord::getSourceBook, keyword)
                    .or().like(WrongQuestionRecord::getChapter, keyword)
                    .or().like(WrongQuestionRecord::getKnowledgePoints, keyword)
                    .or().like(WrongQuestionRecord::getErrorType, keyword)
                    .or().like(WrongQuestionRecord::getOcrText, keyword));
        }
        return wrongQuestionMapper.selectList(wrapper.orderByDesc(WrongQuestionRecord::getCreateTime));
    }

    /** {@inheritDoc} */
    @Override
    public StudyArchiveViews.WrongQuestionDetail getWrongQuestion(Long id, Long userId) {
        WrongQuestionRecord record = requireWrongQuestion(id, userId);
        List<WrongQuestionReviewLog> reviews = wrongQuestionReviewLogMapper.selectList(
                new LambdaQueryWrapper<WrongQuestionReviewLog>()
                        .eq(WrongQuestionReviewLog::getWrongQuestionId, id)
                        .eq(WrongQuestionReviewLog::getUserId, userId)
                        .orderByDesc(WrongQuestionReviewLog::getCreateTime));
        return new StudyArchiveViews.WrongQuestionDetail(record, reviews);
    }

    /** {@inheritDoc} */
    @Override
    public WrongQuestionRecord updateWrongQuestion(Long id, StudyArchiveRequests.UpdateWrongQuestion request, Long userId) {
        WrongQuestionRecord record = requireWrongQuestion(id, userId);
        record.setSubject(request.getSubject());
        record.setOcrText(request.getOcrText());
        record.setUserAnswer(request.getUserAnswer());
        record.setCorrectAnswer(request.getCorrectAnswer());
        record.setExplanation(request.getExplanation());
        record.setSourceBook(request.getSourceBook());
        record.setSourcePage(request.getSourcePage());
        record.setChapter(request.getChapter());
        record.setKnowledgePoints(writeJson(request.getKnowledgePoints()));
        record.setErrorType(request.getErrorType());
        record.setUserNote(request.getUserNote());
        wrongQuestionMapper.updateById(record);
        return record;
    }

    /** {@inheritDoc} */
    @Override
    public WrongQuestionRecord scheduleWrongQuestion(Long id, LocalDateTime scheduledAt, Long userId) {
        WrongQuestionRecord record = requireWrongQuestion(id, userId);
        if ("MASTERED".equals(record.getReviewStatus()) || "ARCHIVED".equals(record.getReviewStatus())) {
            throw new BusinessException(400, "已掌握或已归档的错题不能安排复习");
        }
        record.setNextReviewTime(scheduledAt);
        record.setReviewStatus(scheduledAt == null ? "UNSCHEDULED" : "SCHEDULED");
        wrongQuestionMapper.update(null, new LambdaUpdateWrapper<WrongQuestionRecord>()
                .eq(WrongQuestionRecord::getId, id)
                .eq(WrongQuestionRecord::getUserId, userId)
                .set(WrongQuestionRecord::getNextReviewTime, scheduledAt)
                .set(WrongQuestionRecord::getReviewStatus, record.getReviewStatus()));
        return record;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public WrongQuestionRecord reviewWrongQuestion(Long id, StudyArchiveRequests.WrongQuestionReview request, Long userId) {
        WrongQuestionRecord record = requireWrongQuestion(id, userId);
        String result = request.getResult().trim().toUpperCase();
        if (!List.of("CORRECT", "INCORRECT", "VIEWED").contains(result)) {
            throw new BusinessException(400, "复盘结果无效");
        }
        WrongQuestionReviewLog logEntry = new WrongQuestionReviewLog();
        logEntry.setWrongQuestionId(id);
        logEntry.setUserId(userId);
        logEntry.setResult(result);
        logEntry.setNote(request.getNote());
        wrongQuestionReviewLogMapper.insert(logEntry);
        record.setLastReviewTime(LocalDateTime.now());
        record.setNextReviewTime(null);
        record.setReviewStatus("UNSCHEDULED");
        wrongQuestionMapper.update(null, new LambdaUpdateWrapper<WrongQuestionRecord>()
                .eq(WrongQuestionRecord::getId, id)
                .eq(WrongQuestionRecord::getUserId, userId)
                .set(WrongQuestionRecord::getLastReviewTime, record.getLastReviewTime())
                .set(WrongQuestionRecord::getNextReviewTime, null)
                .set(WrongQuestionRecord::getReviewStatus, "UNSCHEDULED"));
        return record;
    }

    /** {@inheritDoc} */
    @Override
    public WrongQuestionRecord masterWrongQuestion(Long id, Long userId) {
        WrongQuestionRecord record = requireWrongQuestion(id, userId);
        record.setReviewStatus("MASTERED");
        record.setNextReviewTime(null);
        wrongQuestionMapper.update(null, new LambdaUpdateWrapper<WrongQuestionRecord>()
                .eq(WrongQuestionRecord::getId, id)
                .eq(WrongQuestionRecord::getUserId, userId)
                .set(WrongQuestionRecord::getReviewStatus, "MASTERED")
                .set(WrongQuestionRecord::getNextReviewTime, null));
        return record;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteWrongQuestion(Long id, Long userId) {
        WrongQuestionRecord record = requireWrongQuestion(id, userId);
        record.setIsDeleted(1);
        record.setReviewStatus("ARCHIVED");
        record.setNextReviewTime(null);
        wrongQuestionMapper.update(null, new LambdaUpdateWrapper<WrongQuestionRecord>()
                .eq(WrongQuestionRecord::getId, id)
                .eq(WrongQuestionRecord::getUserId, userId)
                .set(WrongQuestionRecord::getIsDeleted, 1)
                .set(WrongQuestionRecord::getReviewStatus, "ARCHIVED")
                .set(WrongQuestionRecord::getNextReviewTime, null));
    }

    /** {@inheritDoc} */
    @Override
    public List<WrongQuestionRecord> dueWrongQuestions(Long userId, LocalDateTime now) {
        return wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestionRecord>()
                .eq(WrongQuestionRecord::getUserId, userId)
                .eq(WrongQuestionRecord::getIsDeleted, 0)
                .eq(WrongQuestionRecord::getReviewStatus, "SCHEDULED")
                .le(WrongQuestionRecord::getNextReviewTime, now)
                .orderByAsc(WrongQuestionRecord::getNextReviewTime));
    }

    /** {@inheritDoc} */
    @Override
    public DoubtRecord createDoubt(StudyArchiveRequests.CreateDoubt request, Long userId, Long workspaceId) {
        if (request.getImagePath() != null && !request.getImagePath().isBlank()) {
            validateOwnedImage(request.getImagePath(), userId);
        }
        DoubtRecord record = new DoubtRecord();
        record.setUserId(userId);
        record.setWorkspaceId(workspaceId);
        record.setImagePath(request.getImagePath());
        record.setContent(request.getContent().trim());
        record.setSourceBook(request.getSourceBook());
        record.setSourcePage(request.getSourcePage());
        record.setChapter(request.getChapter());
        record.setDoubtType(request.getDoubtType());
        record.setStatus("PENDING");
        record.setIsDeleted(0);
        doubtMapper.insert(record);
        return record;
    }

    /** {@inheritDoc} */
    @Override
    public List<DoubtRecord> listDoubts(Long userId, String status, String keyword) {
        LambdaQueryWrapper<DoubtRecord> wrapper = new LambdaQueryWrapper<DoubtRecord>()
                .eq(DoubtRecord::getUserId, userId)
                .eq(DoubtRecord::getIsDeleted, 0);
        if (status != null && !status.isBlank()) {
            validateStatus(status, DOUBT_STATUSES);
            wrapper.eq(DoubtRecord::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(query -> query.like(DoubtRecord::getContent, keyword)
                    .or().like(DoubtRecord::getSourceBook, keyword)
                    .or().like(DoubtRecord::getChapter, keyword)
                    .or().like(DoubtRecord::getDoubtType, keyword));
        }
        return doubtMapper.selectList(wrapper.orderByDesc(DoubtRecord::getCreateTime));
    }

    /** {@inheritDoc} */
    @Override
    public StudyArchiveViews.DoubtDetail getDoubt(Long id, Long userId) {
        DoubtRecord record = requireDoubt(id, userId);
        List<DoubtUnderstandingRevision> understandings = understandingMapper.selectList(
                new LambdaQueryWrapper<DoubtUnderstandingRevision>()
                        .eq(DoubtUnderstandingRevision::getDoubtId, id)
                        .eq(DoubtUnderstandingRevision::getUserId, userId)
                        .orderByAsc(DoubtUnderstandingRevision::getCreateTime));
        return new StudyArchiveViews.DoubtDetail(record, understandings);
    }

    /** {@inheritDoc} */
    @Override
    public DoubtRecord updateDoubt(Long id, StudyArchiveRequests.UpdateDoubt request, Long userId) {
        DoubtRecord record = requireDoubt(id, userId);
        if (request.getContent() != null && !request.getContent().isBlank()) record.setContent(request.getContent().trim());
        record.setSourceBook(request.getSourceBook());
        record.setSourcePage(request.getSourcePage());
        record.setChapter(request.getChapter());
        record.setDoubtType(request.getDoubtType());
        if (request.getAiExplanationFeedback() != null && !request.getAiExplanationFeedback().isBlank()) {
            String feedback = request.getAiExplanationFeedback().trim().toUpperCase();
            if (!List.of("HELPFUL", "NOT_HELPFUL").contains(feedback)) {
                throw new BusinessException(400, "AI 解释反馈无效");
            }
            record.setAiExplanationFeedback(feedback);
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            validateStatus(request.getStatus(), DOUBT_STATUSES);
            record.setStatus(request.getStatus());
            if ("RESOLVED".equals(request.getStatus())) {
                record.setNextProcessTime(null);
                doubtMapper.update(null, new LambdaUpdateWrapper<DoubtRecord>()
                        .eq(DoubtRecord::getId, id)
                        .eq(DoubtRecord::getUserId, userId)
                        .set(DoubtRecord::getStatus, "RESOLVED")
                        .set(DoubtRecord::getNextProcessTime, null));
                return record;
            }
        }
        doubtMapper.updateById(record);
        return record;
    }

    /** {@inheritDoc} */
    @Override
    public DoubtRecord scheduleDoubt(Long id, LocalDateTime scheduledAt, Long userId) {
        DoubtRecord record = requireDoubt(id, userId);
        if ("RESOLVED".equals(record.getStatus())) throw new BusinessException(400, "已解决的疑问不能安排处理");
        record.setNextProcessTime(scheduledAt);
        doubtMapper.update(null, new LambdaUpdateWrapper<DoubtRecord>()
                .eq(DoubtRecord::getId, id)
                .eq(DoubtRecord::getUserId, userId)
                .set(DoubtRecord::getNextProcessTime, scheduledAt));
        return record;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public StudyArchiveViews.DoubtDetail addUnderstanding(Long id, StudyArchiveRequests.AddUnderstanding request, Long userId) {
        DoubtRecord record = requireDoubt(id, userId);
        String status = request.getUnderstandingStatus() == null ? "INITIAL" : request.getUnderstandingStatus().toUpperCase();
        if (!List.of("INITIAL", "VERIFYING", "CONFIRMED").contains(status)) {
            throw new BusinessException(400, "理解状态无效");
        }
        DoubtUnderstandingRevision revision = new DoubtUnderstandingRevision();
        revision.setDoubtId(id);
        revision.setUserId(userId);
        revision.setContent(request.getContent().trim());
        revision.setUnderstandingStatus(status);
        understandingMapper.insert(revision);
        if ("CONFIRMED".equals(status)) record.setStatus("UNDERSTOOD");
        else if ("VERIFYING".equals(status)) record.setStatus("VERIFYING");
        else record.setStatus("UNDERSTOOD");
        doubtMapper.updateById(record);
        return getDoubt(id, userId);
    }

    /** {@inheritDoc} */
    @Override
    public String explainDoubt(Long id, Long userId) {
        DoubtRecord record = requireDoubt(id, userId);
        String prompt = "请用考研学习者能理解的方式解释下面的疑问，并给出推理步骤和一个自检问题。只作为参考解释，不要声称用户已经掌握。"
                + "疑问：" + record.getContent() + "\n资料：" + nullToEmpty(record.getSourceBook())
                + " 第" + nullToEmpty(record.getSourcePage()) + "页，章节：" + nullToEmpty(record.getChapter());
        try {
            return aiService.generateAnswer(userId, "chat", prompt);
        } catch (RuntimeException exception) {
            log.info("study_doubt_ai_unavailable userId={} doubtId={} reason={}", userId, id, exception.getMessage());
            throw new BusinessException(503, "AI 解释暂时不可用，疑问记录仍已保留");
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<DoubtRecord> dueDoubts(Long userId, LocalDateTime now) {
        return doubtMapper.selectList(new LambdaQueryWrapper<DoubtRecord>()
                .eq(DoubtRecord::getUserId, userId)
                .eq(DoubtRecord::getIsDeleted, 0)
                .ne(DoubtRecord::getStatus, "RESOLVED")
                .isNotNull(DoubtRecord::getNextProcessTime)
                .le(DoubtRecord::getNextProcessTime, now)
                .orderByAsc(DoubtRecord::getNextProcessTime));
    }

    /** {@inheritDoc} */
    @Override
    public void deleteDoubt(Long id, Long userId) {
        DoubtRecord record = requireDoubt(id, userId);
        record.setIsDeleted(1);
        record.setNextProcessTime(null);
        doubtMapper.update(null, new LambdaUpdateWrapper<DoubtRecord>()
                .eq(DoubtRecord::getId, id)
                .eq(DoubtRecord::getUserId, userId)
                .set(DoubtRecord::getIsDeleted, 1)
                .set(DoubtRecord::getNextProcessTime, null));
    }

    private WrongQuestionRecord requireWrongQuestion(Long id, Long userId) {
        WrongQuestionRecord record = wrongQuestionMapper.selectOne(new LambdaQueryWrapper<WrongQuestionRecord>()
                .eq(WrongQuestionRecord::getId, id)
                .eq(WrongQuestionRecord::getUserId, userId)
                .eq(WrongQuestionRecord::getIsDeleted, 0));
        if (record == null) throw new BusinessException(404, "错题档案不存在");
        return record;
    }

    private DoubtRecord requireDoubt(Long id, Long userId) {
        DoubtRecord record = doubtMapper.selectOne(new LambdaQueryWrapper<DoubtRecord>()
                .eq(DoubtRecord::getId, id)
                .eq(DoubtRecord::getUserId, userId)
                .eq(DoubtRecord::getIsDeleted, 0));
        if (record == null) throw new BusinessException(404, "疑问档案不存在");
        return record;
    }

    private void validateOwnedImage(String fileName, Long userId) {
        resolveOwnedImage(fileName, userId);
    }

    private Path resolveOwnedImage(String fileName, Long userId) {
        if (fileName == null || fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")
                || fileName.contains("..")) {
            throw new BusinessException(404, "图片不存在");
        }
        Path userDirectory = storageRoot.resolve(String.valueOf(userId)).normalize();
        Path imagePath = userDirectory.resolve(fileName).normalize();
        if (!imagePath.startsWith(userDirectory) || !Files.isRegularFile(imagePath)) {
            throw new BusinessException(404, "图片不存在");
        }
        return imagePath;
    }

    private String writeJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new BusinessException(400, "知识点数据格式无效");
        }
    }

    private void validateStatus(String status, List<String> allowed) {
        if (!allowed.contains(status.toUpperCase())) throw new BusinessException(400, "档案状态无效");
    }

    private String fileExtension(String originalName) {
        if (originalName == null || !originalName.contains(".")) return "";
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String extractJsonObject(String response) {
        if (response == null) return "{}";
        String text = response.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('\n');
            int end = text.lastIndexOf("```");
            if (start >= 0 && end > start) text = text.substring(start + 1, end).trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return start >= 0 && end >= start ? text.substring(start, end + 1) : "{}";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
