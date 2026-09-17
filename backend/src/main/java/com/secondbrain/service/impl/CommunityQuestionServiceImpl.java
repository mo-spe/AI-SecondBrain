package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.CreateCommunityAnswerRequest;
import com.secondbrain.dto.CreateCommunityQuestionRequest;
import com.secondbrain.entity.CommunityAnswer;
import com.secondbrain.entity.CommunityQuestion;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.User;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.CommunityAnswerMapper;
import com.secondbrain.mapper.CommunityQuestionMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.service.CommunityQuestionService;
import com.secondbrain.service.SquareNotificationService;
import com.secondbrain.vo.CommunityAnswerVO;
import com.secondbrain.vo.CommunityQuestionVO;
import com.secondbrain.vo.KnowledgeSnapshotVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 社区问答服务实现。
 *
 * <p>问题与回答使用独立领域表，互动能力后续经统一互动模型复用现有知识广场。</p>
 *
 * @author AI
 */
@Service
public class CommunityQuestionServiceImpl implements CommunityQuestionService {

    private static final String STATUS_OPEN = "OPEN";

    private final CommunityQuestionMapper questionMapper;
    private final CommunityAnswerMapper answerMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final UserMapper userMapper;
    private final SquareNotificationService notificationService;

    public CommunityQuestionServiceImpl(CommunityQuestionMapper questionMapper,
                                        CommunityAnswerMapper answerMapper,
                                        KnowledgeNodeMapper knowledgeNodeMapper,
                                        UserMapper userMapper,
                                        SquareNotificationService notificationService) {
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    /**
     * 发布公开问题。
     *
     * @param request 问题内容
     * @param userId 当前用户ID
     * @return 创建后的问题
     */
    @Override
    public CommunityQuestionVO createQuestion(CreateCommunityQuestionRequest request, Long userId) {
        CommunityQuestion question = new CommunityQuestion();
        question.setAuthorId(userId);
        question.setTitle(request.getTitle().trim());
        question.setContent(request.getContent().trim());
        question.setTagsJson(JSON.toJSONString(normalizeTags(request.getTags())));
        question.setStatus(STATUS_OPEN);
        question.setAnswerCount(0);
        question.setViewCount(0);
        questionMapper.insert(question);
        return toQuestionVO(question, loadUsers(Set.of(userId)), false);
    }

    /**
     * 分页浏览公开问题。
     *
     * @param sort 排序方式
     * @param keyword 搜索词
     * @param current 当前页
     * @param size 每页大小
     * @return 问题分页
     */
    @Override
    public IPage<CommunityQuestionVO> listQuestions(String sort, String keyword, Integer current, Integer size) {
        LambdaQueryWrapper<CommunityQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityQuestion::getStatus, STATUS_OPEN);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(query -> query.like(CommunityQuestion::getTitle, keyword.trim())
                    .or().like(CommunityQuestion::getContent, keyword.trim())
                    .or().like(CommunityQuestion::getTagsJson, keyword.trim()));
        }
        if ("unanswered".equalsIgnoreCase(sort)) {
            wrapper.eq(CommunityQuestion::getAnswerCount, 0);
        }
        wrapper.orderByDesc(CommunityQuestion::getCreateTime);

        Page<CommunityQuestion> result = questionMapper.selectPage(new Page<>(current, size), wrapper);
        Set<Long> authorIds = result.getRecords().stream()
                .map(CommunityQuestion::getAuthorId)
                .collect(Collectors.toSet());
        Map<Long, User> users = loadUsers(authorIds);
        List<CommunityQuestionVO> records = result.getRecords().stream()
                .map(question -> toQuestionVO(question, users, false))
                .toList();
        return new Page<CommunityQuestionVO>(result.getCurrent(), result.getSize(), result.getTotal())
                .setRecords(records);
    }

    /**
     * 获取问题及其回答详情，并记录一次浏览。
     *
     * @param questionId 问题ID
     * @return 问题详情
     */
    @Override
    public CommunityQuestionVO getQuestion(Long questionId) {
        CommunityQuestion question = requireQuestion(questionId);
        questionMapper.update(null, new LambdaUpdateWrapper<CommunityQuestion>()
                .eq(CommunityQuestion::getId, questionId)
                .setSql("view_count = view_count + 1"));
        question.setViewCount(question.getViewCount() + 1);

        List<CommunityAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<CommunityAnswer>()
                .eq(CommunityAnswer::getQuestionId, questionId)
                .orderByDesc(CommunityAnswer::getAccepted)
                .orderByAsc(CommunityAnswer::getCreateTime));
        Set<Long> userIds = answers.stream().map(CommunityAnswer::getAuthorId).collect(Collectors.toSet());
        userIds.add(question.getAuthorId());
        Map<Long, User> users = loadUsers(userIds);

        CommunityQuestionVO result = toQuestionVO(question, users, false);
        result.setAnswers(answers.stream().map(answer -> toAnswerVO(answer, users)).toList());
        return result;
    }

    /**
     * 发布回答，并将用户明确选择的个人知识复制为不可变展示快照。
     *
     * @param questionId 问题ID
     * @param request 回答内容
     * @param userId 当前用户ID
     * @return 创建后的回答
     */
    @Override
    @Transactional
    public CommunityAnswerVO createAnswer(Long questionId, CreateCommunityAnswerRequest request, Long userId) {
        CommunityQuestion question = requireQuestion(questionId);
        if (!STATUS_OPEN.equals(question.getStatus())) {
            throw new BusinessException(400, "该问题已关闭，暂时不能回答");
        }

        List<KnowledgeSnapshotVO> snapshots = createKnowledgeSnapshots(request.getKnowledgeNodeIds(), userId);
        CommunityAnswer answer = new CommunityAnswer();
        answer.setQuestionId(questionId);
        answer.setAuthorId(userId);
        answer.setContent(request.getContent().trim());
        answer.setKnowledgeSnapshotsJson(JSON.toJSONString(snapshots));
        answer.setAccepted(0);
        answerMapper.insert(answer);
        questionMapper.update(null, new LambdaUpdateWrapper<CommunityQuestion>()
                .eq(CommunityQuestion::getId, questionId)
                .setSql("answer_count = answer_count + 1"));

        if (!Objects.equals(question.getAuthorId(), userId)) {
            User answerAuthor = userMapper.selectById(userId);
            String authorName = answerAuthor != null ? answerAuthor.getUsername() : "一位用户";
            notificationService.create(question.getAuthorId(), "question_answered", "你的问题有了新回答",
                    authorName + " 回答了《" + question.getTitle() + "》", "community_question", questionId);
        }
        return toAnswerVO(answer, loadUsers(Set.of(userId)));
    }

    /**
     * 由问题作者采纳所属问题的一条回答。
     *
     * @param questionId 问题ID
     * @param answerId 回答ID
     * @param userId 当前用户ID
     */
    @Override
    @Transactional
    public void acceptAnswer(Long questionId, Long answerId, Long userId) {
        CommunityQuestion question = requireQuestion(questionId);
        if (!Objects.equals(question.getAuthorId(), userId)) {
            throw new BusinessException(403, "只有问题发布者可以采纳回答");
        }
        if (question.getAcceptedAnswerId() != null) {
            throw new BusinessException(400, "该问题已有采纳回答");
        }
        CommunityAnswer answer = answerMapper.selectById(answerId);
        if (answer == null || !Objects.equals(questionId, answer.getQuestionId())) {
            throw new BusinessException(404, "回答不存在或不属于该问题");
        }
        question.setAcceptedAnswerId(answerId);
        questionMapper.updateById(question);
        answer.setAccepted(1);
        answerMapper.updateById(answer);
        if (!Objects.equals(answer.getAuthorId(), userId)) {
            notificationService.create(answer.getAuthorId(), "answer_accepted", "你的回答被采纳了",
                    "你对《" + question.getTitle() + "》的回答已被采纳", "community_question", questionId);
        }
    }

    private CommunityQuestion requireQuestion(Long questionId) {
        CommunityQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(404, "问题不存在");
        }
        return question;
    }

    private List<KnowledgeSnapshotVO> createKnowledgeSnapshots(List<Long> nodeIds, Long userId) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(nodeIds));
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectBatchIds(uniqueIds);
        if (nodes.size() != uniqueIds.size()
                || nodes.stream().anyMatch(node -> !Objects.equals(node.getUserId(), userId))) {
            throw new BusinessException(403, "只能引用并公开自己的知识点");
        }
        Map<Long, KnowledgeNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(KnowledgeNode::getId, Function.identity()));
        return uniqueIds.stream().map(nodeMap::get).map(this::toSnapshot).toList();
    }

    private KnowledgeSnapshotVO toSnapshot(KnowledgeNode node) {
        KnowledgeSnapshotVO snapshot = new KnowledgeSnapshotVO();
        snapshot.setSourceId(node.getId());
        snapshot.setTitle(node.getTitle());
        snapshot.setSummary(node.getSummary());
        snapshot.setContent(node.getContentMd());
        return snapshot;
    }

    private Map<Long, User> loadUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private CommunityQuestionVO toQuestionVO(CommunityQuestion question, Map<Long, User> users,
                                              boolean includeAnswers) {
        User author = users.get(question.getAuthorId());
        CommunityQuestionVO vo = new CommunityQuestionVO();
        vo.setId(question.getId());
        vo.setAuthorId(question.getAuthorId());
        vo.setAuthorName(author != null ? author.getUsername() : "已注销用户");
        vo.setAuthorAvatar(author != null ? author.getAvatar() : null);
        vo.setTitle(question.getTitle());
        vo.setContent(question.getContent());
        vo.setTags(parseTags(question.getTagsJson()));
        vo.setStatus(question.getStatus());
        vo.setAnswerCount(question.getAnswerCount());
        vo.setViewCount(question.getViewCount());
        vo.setAcceptedAnswerId(question.getAcceptedAnswerId());
        vo.setCreateTime(question.getCreateTime());
        if (includeAnswers) {
            vo.setAnswers(Collections.emptyList());
        }
        return vo;
    }

    private CommunityAnswerVO toAnswerVO(CommunityAnswer answer, Map<Long, User> users) {
        User author = users.get(answer.getAuthorId());
        CommunityAnswerVO vo = new CommunityAnswerVO();
        vo.setId(answer.getId());
        vo.setAuthorId(answer.getAuthorId());
        vo.setAuthorName(author != null ? author.getUsername() : "已注销用户");
        vo.setAuthorAvatar(author != null ? author.getAvatar() : null);
        vo.setContent(answer.getContent());
        vo.setAccepted(Objects.equals(answer.getAccepted(), 1));
        vo.setKnowledgeSnapshots(parseSnapshots(answer.getKnowledgeSnapshotsJson()));
        vo.setCreateTime(answer.getCreateTime());
        return vo;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags.stream().map(String::trim).filter(tag -> !tag.isBlank()).distinct().toList();
    }

    private List<String> parseTags(String json) {
        return json == null || json.isBlank()
                ? Collections.emptyList()
                : JSON.parseObject(json, new TypeReference<List<String>>() { });
    }

    private List<KnowledgeSnapshotVO> parseSnapshots(String json) {
        return json == null || json.isBlank()
                ? Collections.emptyList()
                : JSON.parseObject(json, new TypeReference<List<KnowledgeSnapshotVO>>() { });
    }
}
