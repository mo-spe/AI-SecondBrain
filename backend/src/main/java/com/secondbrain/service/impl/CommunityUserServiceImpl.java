package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.UpdateCommunityProfileRequest;
import com.secondbrain.entity.CommunityAnswer;
import com.secondbrain.entity.CommunityQuestion;
import com.secondbrain.entity.CommunityUserProfile;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.SquarePost;
import com.secondbrain.entity.User;
import com.secondbrain.entity.UserBlock;
import com.secondbrain.entity.UserFollow;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.CommunityAnswerMapper;
import com.secondbrain.mapper.CommunityQuestionMapper;
import com.secondbrain.mapper.CommunityUserProfileMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.SquarePostMapper;
import com.secondbrain.mapper.UserBlockMapper;
import com.secondbrain.mapper.UserFollowMapper;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.service.CommunityUserService;
import com.secondbrain.service.SquareNotificationService;
import com.secondbrain.vo.CommunityContributionVO;
import com.secondbrain.vo.CommunityUserListItemVO;
import com.secondbrain.vo.CommunityUserProfileVO;
import org.springframework.dao.DuplicateKeyException;
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
 * 社区用户资料与关系服务实现。
 *
 * <p>公开资料通过专用视图输出，避免账户联系方式和密钥进入社区接口。</p>
 */
@Service
public class CommunityUserServiceImpl implements CommunityUserService {

    private static final int MAX_RELATION_PAGE_SIZE = 50;
    private static final int RECENT_CONTRIBUTION_LIMIT = 10;

    private final UserMapper userMapper;
    private final CommunityUserProfileMapper profileMapper;
    private final UserFollowMapper followMapper;
    private final UserBlockMapper blockMapper;
    private final CommunityQuestionMapper questionMapper;
    private final CommunityAnswerMapper answerMapper;
    private final SquarePostMapper squarePostMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final SquareNotificationService notificationService;

    public CommunityUserServiceImpl(UserMapper userMapper,
                                    CommunityUserProfileMapper profileMapper,
                                    UserFollowMapper followMapper,
                                    UserBlockMapper blockMapper,
                                    CommunityQuestionMapper questionMapper,
                                    CommunityAnswerMapper answerMapper,
                                    SquarePostMapper squarePostMapper,
                                    KnowledgeNodeMapper knowledgeNodeMapper,
                                    SquareNotificationService notificationService) {
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
        this.followMapper = followMapper;
        this.blockMapper = blockMapper;
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.squarePostMapper = squarePostMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.notificationService = notificationService;
    }

    /** {@inheritDoc} */
    @Override
    public CommunityUserProfileVO getProfile(Long profileUserId, Long viewerId) {
        User user = requireActiveUser(profileUserId);
        CommunityUserProfile profile = profileMapper.selectById(profileUserId);
        RelationState relation = relationState(viewerId, profileUserId);

        CommunityUserProfileVO vo = baseProfile(user, profile, relation);
        if (relation.blocked()) {
            hideBlockedProfileDetails(vo);
            return vo;
        }
        populateRelationCounts(vo, profileUserId);
        populateContributions(vo, profileUserId);
        return vo;
    }

    /** {@inheritDoc} */
    @Override
    public CommunityUserProfileVO updateProfile(Long userId, UpdateCommunityProfileRequest request) {
        requireActiveUser(userId);
        CommunityUserProfile profile = profileMapper.selectById(userId);
        if (profile == null) {
            profile = new CommunityUserProfile();
            profile.setUserId(userId);
            profile.setIntroduction(normalizeIntroduction(request.getIntroduction()));
            profile.setExpertiseTagsJson(JSON.toJSONString(normalizeTags(request.getExpertiseTags())));
            profileMapper.insert(profile);
        } else {
            profile.setIntroduction(normalizeIntroduction(request.getIntroduction()));
            profile.setExpertiseTagsJson(JSON.toJSONString(normalizeTags(request.getExpertiseTags())));
            profileMapper.updateById(profile);
        }
        return getProfile(userId, userId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void follow(Long followerId, Long followedId) {
        validateDifferentUsers(followerId, followedId, "不能关注自己");
        requireActiveUser(followerId);
        User target = requireActiveUser(followedId);
        if (hasBlockEitherDirection(followerId, followedId)) {
            throw new BusinessException(400, "存在拉黑关系，无法关注该用户");
        }
        if (isFollowing(followerId, followedId)) {
            return;
        }

        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFollowedId(followedId);
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException ignored) {
            // 唯一索引用于收敛并发关注，重复请求仍按幂等成功处理。
            return;
        }
        User follower = userMapper.selectById(followerId);
        String name = follower != null ? follower.getUsername() : "一位用户";
        notificationService.create(target.getId(), "user_followed", "你有了新的关注者",
                name + " 关注了你", "community_user", followerId);
    }

    /** {@inheritDoc} */
    @Override
    public void unfollow(Long followerId, Long followedId) {
        followMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowedId, followedId));
    }

    /** {@inheritDoc} */
    @Override
    public IPage<CommunityUserListItemVO> listFollowers(Long userId, Long viewerId,
                                                        Integer current, Integer size) {
        requireActiveUser(userId);
        if (!Objects.equals(userId, viewerId) && hasBlockEitherDirection(userId, viewerId)) {
            return emptyRelationPage(current, size);
        }
        Page<UserFollow> page = followMapper.selectPage(safePage(current, size),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowedId, userId)
                        .orderByDesc(UserFollow::getCreateTime));
        List<Long> ids = page.getRecords().stream().map(UserFollow::getFollowerId).toList();
        return mapRelationPage(page, ids, viewerId);
    }

    /** {@inheritDoc} */
    @Override
    public IPage<CommunityUserListItemVO> listFollowing(Long userId, Long viewerId,
                                                        Integer current, Integer size) {
        requireActiveUser(userId);
        if (!Objects.equals(userId, viewerId) && hasBlockEitherDirection(userId, viewerId)) {
            return emptyRelationPage(current, size);
        }
        Page<UserFollow> page = followMapper.selectPage(safePage(current, size),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId)
                        .orderByDesc(UserFollow::getCreateTime));
        List<Long> ids = page.getRecords().stream().map(UserFollow::getFollowedId).toList();
        return mapRelationPage(page, ids, viewerId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void block(Long blockerId, Long blockedId) {
        validateDifferentUsers(blockerId, blockedId, "不能拉黑自己");
        requireActiveUser(blockerId);
        requireActiveUser(blockedId);
        if (!isBlockedBy(blockerId, blockedId)) {
            UserBlock block = new UserBlock();
            block.setBlockerId(blockerId);
            block.setBlockedId(blockedId);
            try {
                blockMapper.insert(block);
            } catch (DuplicateKeyException ignored) {
                // 并发重复拉黑不应让用户看到失败。
            }
        }
        // 拉黑优先于关注，双向关系都需清理，避免关注流继续暴露对方内容。
        followMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .and(wrapper -> wrapper
                        .eq(UserFollow::getFollowerId, blockerId).eq(UserFollow::getFollowedId, blockedId)
                        .or()
                        .eq(UserFollow::getFollowerId, blockedId).eq(UserFollow::getFollowedId, blockerId)));
    }

    /** {@inheritDoc} */
    @Override
    public void unblock(Long blockerId, Long blockedId) {
        blockMapper.delete(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getBlockerId, blockerId)
                .eq(UserBlock::getBlockedId, blockedId));
    }

    private CommunityUserProfileVO baseProfile(User user, CommunityUserProfile profile, RelationState relation) {
        CommunityUserProfileVO vo = new CommunityUserProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setAvatar(user.getAvatar());
        vo.setIntroduction(profile != null ? profile.getIntroduction() : user.getBio());
        vo.setExpertiseTags(profile != null ? parseTags(profile.getExpertiseTagsJson()) : Collections.emptyList());
        vo.setIsSelf(relation.self());
        vo.setIsFollowing(relation.following());
        vo.setIsFollowedBy(relation.followedBy());
        vo.setIsBlocked(relation.blocked());
        vo.setIsBlockedByMe(relation.blockedByMe());
        vo.setIsBlockedByTarget(relation.blockedByTarget());
        return vo;
    }

    private void populateRelationCounts(CommunityUserProfileVO vo, Long userId) {
        vo.setFollowerCount(followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowedId, userId)));
        vo.setFollowingCount(followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)));
    }

    private void populateContributions(CommunityUserProfileVO vo, Long userId) {
        List<CommunityQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<CommunityQuestion>()
                .eq(CommunityQuestion::getAuthorId, userId)
                .orderByDesc(CommunityQuestion::getCreateTime));
        List<CommunityAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<CommunityAnswer>()
                .eq(CommunityAnswer::getAuthorId, userId)
                .orderByDesc(CommunityAnswer::getCreateTime));
        List<SquarePost> posts = squarePostMapper.selectList(new LambdaQueryWrapper<SquarePost>()
                .eq(SquarePost::getAuthorId, userId)
                .eq(SquarePost::getScope, "global")
                .eq(SquarePost::getStatus, "published")
                .orderByDesc(SquarePost::getCreatedAt));

        vo.setQuestionCount((long) questions.size());
        vo.setAnswerCount((long) answers.size());
        vo.setAcceptedAnswerCount(answers.stream().filter(answer -> Objects.equals(answer.getAccepted(), 1)).count());
        vo.setKnowledgePostCount((long) posts.size());
        vo.setReceivedLikeCount(posts.stream().mapToLong(post -> valueOrZero(post.getLikeCount())).sum());
        vo.setReceivedBookmarkCount(posts.stream().mapToLong(post -> valueOrZero(post.getBookmarkCount())).sum());
        vo.setRecentQuestions(questions.stream().limit(RECENT_CONTRIBUTION_LIMIT)
                .map(this::questionContribution).toList());
        vo.setRecentAnswers(answerContributions(answers));
        vo.setRecentKnowledgePosts(knowledgeContributions(posts));
    }

    private List<CommunityContributionVO> answerContributions(List<CommunityAnswer> answers) {
        List<CommunityAnswer> recent = answers.stream().limit(RECENT_CONTRIBUTION_LIMIT).toList();
        Set<Long> questionIds = recent.stream().map(CommunityAnswer::getQuestionId).collect(Collectors.toSet());
        Map<Long, CommunityQuestion> questions = questionIds.isEmpty() ? Collections.emptyMap()
                : questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(CommunityQuestion::getId, Function.identity()));
        return recent.stream().map(answer -> {
            CommunityQuestion question = questions.get(answer.getQuestionId());
            CommunityContributionVO contribution = new CommunityContributionVO();
            contribution.setType("ANSWER");
            contribution.setId(answer.getQuestionId());
            contribution.setTitle(question != null ? question.getTitle() : "问题已不可见");
            contribution.setSummary(answer.getContent());
            contribution.setCreateTime(answer.getCreateTime());
            return contribution;
        }).toList();
    }

    private List<CommunityContributionVO> knowledgeContributions(List<SquarePost> posts) {
        List<SquarePost> recent = posts.stream().limit(RECENT_CONTRIBUTION_LIMIT).toList();
        Set<Long> nodeIds = recent.stream().map(SquarePost::getNodeId).collect(Collectors.toSet());
        Map<Long, KnowledgeNode> nodes = nodeIds.isEmpty() ? Collections.emptyMap()
                : knowledgeNodeMapper.selectBatchIds(nodeIds).stream()
                .collect(Collectors.toMap(KnowledgeNode::getId, Function.identity()));
        return recent.stream().map(post -> {
            KnowledgeNode node = nodes.get(post.getNodeId());
            CommunityContributionVO contribution = new CommunityContributionVO();
            contribution.setType("KNOWLEDGE");
            contribution.setId(post.getId());
            contribution.setTitle(node != null ? node.getTitle() : "知识内容已不可见");
            contribution.setSummary(node != null ? node.getSummary() : post.getRecommendText());
            contribution.setCreateTime(post.getCreatedAt());
            return contribution;
        }).toList();
    }

    private CommunityContributionVO questionContribution(CommunityQuestion question) {
        CommunityContributionVO contribution = new CommunityContributionVO();
        contribution.setType("QUESTION");
        contribution.setId(question.getId());
        contribution.setTitle(question.getTitle());
        contribution.setSummary(question.getContent());
        contribution.setCreateTime(question.getCreateTime());
        return contribution;
    }

    private IPage<CommunityUserListItemVO> mapRelationPage(Page<UserFollow> source, List<Long> ids,
                                                           Long viewerId) {
        Map<Long, User> users = ids.isEmpty() ? Collections.emptyMap() : userMapper.selectBatchIds(ids).stream()
                .filter(user -> Objects.equals(user.getStatus(), 1))
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, CommunityUserProfile> profiles = ids.isEmpty() ? Collections.emptyMap()
                : profileMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CommunityUserProfile::getUserId, Function.identity()));
        List<CommunityUserListItemVO> records = new ArrayList<>();
        for (Long id : ids) {
            User user = users.get(id);
            if (user == null) {
                continue;
            }
            CommunityUserProfile profile = profiles.get(id);
            RelationState state = relationState(viewerId, id);
            CommunityUserListItemVO item = new CommunityUserListItemVO();
            item.setUserId(id);
            item.setUsername(user.getUsername());
            item.setAvatar(user.getAvatar());
            item.setIntroduction(state.blocked() ? null
                    : profile != null ? profile.getIntroduction() : user.getBio());
            item.setExpertiseTags(state.blocked() || profile == null
                    ? Collections.emptyList() : parseTags(profile.getExpertiseTagsJson()));
            item.setIsFollowing(state.following());
            item.setIsBlocked(state.blocked());
            records.add(item);
        }
        return new Page<CommunityUserListItemVO>(source.getCurrent(), source.getSize(), source.getTotal())
                .setRecords(records);
    }

    private RelationState relationState(Long viewerId, Long targetId) {
        boolean self = Objects.equals(viewerId, targetId);
        if (self) {
            return new RelationState(true, false, false, false, false);
        }
        return new RelationState(false, isFollowing(viewerId, targetId), isFollowing(targetId, viewerId),
                isBlockedBy(viewerId, targetId), isBlockedBy(targetId, viewerId));
    }

    private boolean isFollowing(Long followerId, Long followedId) {
        return followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowedId, followedId)) > 0;
    }

    private boolean hasBlockEitherDirection(Long firstId, Long secondId) {
        return blockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .and(wrapper -> wrapper
                        .eq(UserBlock::getBlockerId, firstId).eq(UserBlock::getBlockedId, secondId)
                        .or()
                        .eq(UserBlock::getBlockerId, secondId).eq(UserBlock::getBlockedId, firstId))) > 0;
    }

    private boolean isBlockedBy(Long blockerId, Long blockedId) {
        return blockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getBlockerId, blockerId)
                .eq(UserBlock::getBlockedId, blockedId)) > 0;
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getStatus(), 1)) {
            throw new BusinessException(404, "社区用户不存在");
        }
        return user;
    }

    private void validateDifferentUsers(Long firstId, Long secondId, String message) {
        if (Objects.equals(firstId, secondId)) {
            throw new BusinessException(400, message);
        }
    }

    private Page<UserFollow> safePage(Integer current, Integer size) {
        int pageNumber = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, MAX_RELATION_PAGE_SIZE);
        return new Page<>(pageNumber, pageSize);
    }

    private IPage<CommunityUserListItemVO> emptyRelationPage(Integer current, Integer size) {
        Page<UserFollow> safePage = safePage(current, size);
        return new Page<CommunityUserListItemVO>(safePage.getCurrent(), safePage.getSize(), 0L)
                .setRecords(Collections.emptyList());
    }

    private String normalizeIntroduction(String introduction) {
        return introduction == null ? "" : introduction.trim();
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(new LinkedHashSet<>(tags.stream()
                .filter(Objects::nonNull).map(String::trim).filter(tag -> !tag.isBlank()).toList()));
    }

    private List<String> parseTags(String json) {
        return json == null || json.isBlank() ? Collections.emptyList()
                : JSON.parseObject(json, new TypeReference<List<String>>() { });
    }

    private long valueOrZero(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private void hideBlockedProfileDetails(CommunityUserProfileVO vo) {
        vo.setIntroduction(null);
        vo.setExpertiseTags(Collections.emptyList());
        vo.setFollowerCount(0L);
        vo.setFollowingCount(0L);
        vo.setQuestionCount(0L);
        vo.setAnswerCount(0L);
        vo.setAcceptedAnswerCount(0L);
        vo.setKnowledgePostCount(0L);
        vo.setReceivedLikeCount(0L);
        vo.setReceivedBookmarkCount(0L);
        vo.setRecentQuestions(Collections.emptyList());
        vo.setRecentAnswers(Collections.emptyList());
        vo.setRecentKnowledgePosts(Collections.emptyList());
    }

    private record RelationState(boolean self, boolean following, boolean followedBy,
                                 boolean blockedByMe, boolean blockedByTarget) {

        private boolean blocked() {
            return blockedByMe || blockedByTarget;
        }
    }
}
