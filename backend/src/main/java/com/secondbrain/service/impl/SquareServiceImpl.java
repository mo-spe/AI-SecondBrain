package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.common.SystemConstants;
import com.secondbrain.entity.*;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.SensitiveWordMapper;
import com.secondbrain.mapper.SquareBookmarkMapper;
import com.secondbrain.mapper.SquareCommentMapper;
import com.secondbrain.mapper.SquareLikeMapper;
import com.secondbrain.mapper.SquarePostNodeMapper;
import com.secondbrain.mapper.SquarePostMapper;
import com.secondbrain.mapper.SquareReportMapper;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.mapper.WorkspaceMemberMapper;
import com.secondbrain.service.CacheService;
import com.secondbrain.service.SquareNotificationService;
import com.secondbrain.service.SquareService;
import com.secondbrain.util.SensitiveWordMatcher;
import com.secondbrain.vo.SquareCommentVO;
import com.secondbrain.vo.SquarePostKnowledgeNodeVO;
import com.secondbrain.vo.SquarePostVO;
import com.secondbrain.vo.SquareReportVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 知识广场服务实现.
 *
 * <p>核心业务包括发布/下架、列表浏览、点赞、评论、收藏、举报及管理审核。
 * 计数器通过从源表重算保证一致性；敏感词缓存在内存中提升性能。</p>
 */
@Service
public class SquareServiceImpl implements SquareService {

    private static final Logger log = LoggerFactory.getLogger(SquareServiceImpl.class);
    private static final int MAX_PUBLISHED_NODE_COUNT = 10;

    private final SquarePostMapper squarePostMapper;
    private final SquarePostNodeMapper squarePostNodeMapper;
    private final SquareLikeMapper squareLikeMapper;
    private final SquareCommentMapper squareCommentMapper;
    private final SquareBookmarkMapper squareBookmarkMapper;
    private final SquareReportMapper squareReportMapper;
    private final SensitiveWordMapper sensitiveWordMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final UserMapper userMapper;
    private final SquareNotificationService notificationService;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final CacheService cacheService;

    /**
     * 敏感词 AC 自动机本地缓存（volatile 保证可见性）.
     * 从 Redis 缓存的词列表编译而来，本地 TTL 5 分钟自动重建。
     */
    private volatile SensitiveWordMatcher sensitiveWordMatcher;
    private volatile long matcherBuiltAt;
    private static final long MATCHER_TTL_MS = 5 * 60 * 1000L;

    public SquareServiceImpl(SquarePostMapper squarePostMapper,
                              SquarePostNodeMapper squarePostNodeMapper,
                              SquareLikeMapper squareLikeMapper,
                             SquareCommentMapper squareCommentMapper,
                             SquareBookmarkMapper squareBookmarkMapper,
                             SquareReportMapper squareReportMapper,
                             SensitiveWordMapper sensitiveWordMapper,
                             KnowledgeNodeMapper knowledgeNodeMapper,
                             UserMapper userMapper,
                             SquareNotificationService notificationService,
                             WorkspaceMemberMapper workspaceMemberMapper, CacheService cacheService) {
        this.squarePostMapper = squarePostMapper;
        this.squarePostNodeMapper = squarePostNodeMapper;
        this.squareLikeMapper = squareLikeMapper;
        this.squareCommentMapper = squareCommentMapper;
        this.squareBookmarkMapper = squareBookmarkMapper;
        this.squareReportMapper = squareReportMapper;
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.workspaceMemberMapper = workspaceMemberMapper;
        this.cacheService = cacheService;
    }

    // ==================== 发布/下架 ====================

    @Override
    @Transactional
    public SquarePostVO publish(List<Long> nodeIds, Long legacyNodeId, String recommendText, String scope,
                                Long workspaceId, Long userId) {
        List<Long> resolvedNodeIds = normalizeNodeIds(nodeIds, legacyNodeId);
        Map<Long, KnowledgeNode> nodesById = loadPublishableNodes(resolvedNodeIds, userId);

        // 工作区范围校验
        String resolvedScope = (scope == null || scope.isBlank()) ? "global" : scope;
        if ("workspace".equals(resolvedScope)) {
            if (workspaceId == null) {
                throw new BusinessException(400, "发布到工作区广场需要指定工作区");
            }
            Long memberCount = workspaceMemberMapper.selectCount(
                    new LambdaQueryWrapper<com.secondbrain.entity.WorkspaceMember>()
                            .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                            .eq(WorkspaceMember::getUserId, userId)
                            .eq(WorkspaceMember::getStatus, "accepted"));
            if (memberCount == 0) {
                throw new BusinessException(403, "您不是该工作区的成员，无法发布到工作区广场");
            }
        }

        checkSensitiveWords(recommendText);
        for (Long nodeId : resolvedNodeIds) {
            checkSensitiveWords(nodesById.get(nodeId).getTitle());
        }
        ensureNodesNotAlreadyPublished(resolvedNodeIds);

        LocalDateTime now = LocalDateTime.now();
        SquarePost post = new SquarePost();
        // 旧字段始终保存首个节点，确保旧客户端和历史查询仍能读取帖子摘要。
        post.setNodeId(resolvedNodeIds.get(0));
        post.setAuthorId(userId);
        post.setScope(resolvedScope);
        post.setWorkspaceId("workspace".equals(resolvedScope) ? workspaceId : null);
        post.setRecommendText(recommendText);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setBookmarkCount(0);
        post.setStatus("published");
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        squarePostMapper.insert(post);
        savePostNodes(post.getId(), resolvedNodeIds, now);

        log.info("square_post_published postId={} nodeCount={} userId={}",
                post.getId(), resolvedNodeIds.size(), userId);
        return toPostVO(post, userId);
    }

    @Override
    @Transactional
    public void unpublish(Long postId, Long userId) {
        SquarePost post = squarePostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(400, "帖子不存在");
        }
        if (!Objects.equals(post.getAuthorId(), userId)) {
            throw new BusinessException(403, "只能下架自己的帖子");
        }
        post.setStatus("removed");
        post.setUpdatedAt(LocalDateTime.now());
        squarePostMapper.updateById(post);
        log.info("square_post_unpublished postId={} userId={}", postId, userId);
    }

    // ==================== 列表/详情 ====================

    @Override
    public IPage<SquarePostVO> list(String scope, String sort, String keyword, Integer current, Integer size, Long userId, Long workspaceId) {
        String resolvedScope = (scope == null || scope.isBlank()) ? "global" : scope;
        LambdaQueryWrapper<SquarePost> wrapper = new LambdaQueryWrapper<SquarePost>()
                .eq(SquarePost::getScope, resolvedScope)
                .eq(SquarePost::getStatus, "published");

        // 工作区广场校验成员身份
        if ("workspace".equals(resolvedScope)) {
            if (workspaceId == null) {
                throw new BusinessException(400, "查看工作区广场需要指定工作区");
            }
            Long memberCount = workspaceMemberMapper.selectCount(
                    new LambdaQueryWrapper<com.secondbrain.entity.WorkspaceMember>()
                            .eq(com.secondbrain.entity.WorkspaceMember::getWorkspaceId, workspaceId)
                            .eq(com.secondbrain.entity.WorkspaceMember::getUserId, userId)
                            .eq(com.secondbrain.entity.WorkspaceMember::getStatus, "accepted"));
            if (memberCount == 0) {
                throw new BusinessException(403, "您不是该工作区的成员");
            }
            wrapper.eq(SquarePost::getWorkspaceId, workspaceId);
        }

        // 关键词同时匹配推荐语、旧首节点和合集中的任一知识节点标题。
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            List<Long> matchingNodeIds = findMatchingKnowledgeNodeIds(normalizedKeyword);
            List<Long> relatedPostIds = findRelatedPostIds(matchingNodeIds);
            wrapper.and(query -> {
                query.like(SquarePost::getRecommendText, normalizedKeyword);
                if (!matchingNodeIds.isEmpty()) {
                    query.or().in(SquarePost::getNodeId, matchingNodeIds);
                }
                if (!relatedPostIds.isEmpty()) {
                    query.or().in(SquarePost::getId, relatedPostIds);
                }
            });
        }

        // 排序
        if ("hottest".equals(sort)) {
            wrapper.orderByDesc(SquarePost::getLikeCount)
                   .orderByDesc(SquarePost::getCreatedAt);
        } else {
            wrapper.orderByDesc(SquarePost::getCreatedAt);
        }

        Page<SquarePost> page = new Page<>(current, size);
        Page<SquarePost> resultPage = squarePostMapper.selectPage(page, wrapper);

        // 批量查询关联数据，避免 N+1
        List<SquarePost> posts = resultPage.getRecords();
        if (posts.isEmpty()) {
            Page<SquarePostVO> voPage = new Page<>(current, size, 0);
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        List<SquarePostVO> voList = batchToPostVO(posts, userId);

        Page<SquarePostVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public SquarePostVO getDetail(Long postId, Long userId) {
        SquarePost post = squarePostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(400, "帖子不存在");
        }

        SquarePostVO vo = toPostVO(post, userId);

        // 查询评论列表
        List<SquareComment> comments = squareCommentMapper.selectList(
                new LambdaQueryWrapper<SquareComment>()
                        .eq(SquareComment::getPostId, postId)
                        .eq(SquareComment::getDeleted, 0)
                        .orderByAsc(SquareComment::getCreatedAt));

        vo.setComments(batchToCommentVO(comments));

        return vo;
    }

    // ==================== 点赞 ====================

    @Override
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        SquareLike existing = squareLikeMapper.selectOne(
                new LambdaQueryWrapper<SquareLike>()
                        .eq(SquareLike::getPostId, postId)
                        .eq(SquareLike::getUserId, userId));

        if (existing != null) {
            squareLikeMapper.deleteById(existing.getId());
            refreshLikeCount(postId);
            log.info("square_unliked postId={} userId={}", postId, userId);
            return false;
        } else {
            SquareLike like = new SquareLike();
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            squareLikeMapper.insert(like);
            refreshLikeCount(postId);

            // 通知帖子作者
            SquarePost post = squarePostMapper.selectById(postId);
            if (post != null && !post.getAuthorId().equals(userId)) {
                User liker = userMapper.selectById(userId);
                String likerName = liker != null ? liker.getUsername() : "有人";
                notificationService.create(post.getAuthorId(), "like",
                        likerName + " 赞了你的分享",
                        null, "post", postId);
            }

            log.info("square_liked postId={} userId={}", postId, userId);
            return true;
        }
    }

    // ==================== 评论 ====================

    @Override
    @Transactional
    public SquareCommentVO addComment(Long postId, String content, Long userId) {
        SquarePost post = squarePostMapper.selectById(postId);
        if (post == null || !"published".equals(post.getStatus())) {
            throw new BusinessException(400, "帖子不存在或已下架");
        }

        // 频率限制：单用户每分钟最多 5 条评论
        Long recentCount = squareCommentMapper.selectCount(
                new LambdaQueryWrapper<SquareComment>()
                        .eq(SquareComment::getUserId, userId)
                        .ge(SquareComment::getCreatedAt, LocalDateTime.now().minusMinutes(1)));
        if (recentCount >= 5) {
            throw new BusinessException(400, "评论太频繁，请稍后再试");
        }

        checkSensitiveWords(content);

        SquareComment comment = new SquareComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setDeleted(0);
        squareCommentMapper.insert(comment);

        refreshCommentCount(postId);

        // 通知帖子作者
        if (!post.getAuthorId().equals(userId)) {
            User commenter = userMapper.selectById(userId);
            String commenterName = commenter != null ? commenter.getUsername() : "有人";
            notificationService.create(post.getAuthorId(), "comment",
                    commenterName + " 评论了你的分享",
                    content.length() > 50 ? content.substring(0, 50) + "..." : content,
                    "post", postId);
        }

        log.info("square_comment_added postId={} commentId={} userId={}", postId, comment.getId(), userId);

        return toCommentVO(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        SquareComment comment = squareCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(400, "评论不存在");
        }
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new BusinessException(403, "只能删除自己的评论");
        }

        comment.setDeleted(1);
        squareCommentMapper.updateById(comment);
        refreshCommentCount(comment.getPostId());
        log.info("square_comment_deleted commentId={} userId={}", commentId, userId);
    }

    // ==================== 收藏 ====================

    @Override
    @Transactional
    public boolean toggleBookmark(Long postId, Long userId) {
        SquareBookmark existing = squareBookmarkMapper.selectOne(
                new LambdaQueryWrapper<SquareBookmark>()
                        .eq(SquareBookmark::getPostId, postId)
                        .eq(SquareBookmark::getUserId, userId));

        if (existing != null) {
            squareBookmarkMapper.deleteById(existing.getId());
            refreshBookmarkCount(postId);
            log.info("square_unbookmarked postId={} userId={}", postId, userId);
            return false;
        } else {
            SquareBookmark bookmark = new SquareBookmark();
            bookmark.setPostId(postId);
            bookmark.setUserId(userId);
            bookmark.setCreatedAt(LocalDateTime.now());
            squareBookmarkMapper.insert(bookmark);
            refreshBookmarkCount(postId);
            log.info("square_bookmarked postId={} userId={}", postId, userId);
            return true;
        }
    }

    @Override
    public IPage<SquarePostVO> listMyBookmarks(Integer current, Integer size, Long userId) {
        // 查询用户的收藏记录（按时间倒序）
        Page<SquareBookmark> page = new Page<>(current, size);
        LambdaQueryWrapper<SquareBookmark> wrapper = new LambdaQueryWrapper<SquareBookmark>()
                .eq(SquareBookmark::getUserId, userId)
                .orderByDesc(SquareBookmark::getCreatedAt);
        Page<SquareBookmark> bookmarkPage = squareBookmarkMapper.selectPage(page, wrapper);

        List<SquareBookmark> bookmarks = bookmarkPage.getRecords();
        if (bookmarks.isEmpty()) {
            Page<SquarePostVO> voPage = new Page<>(current, size, 0);
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 批量查询帖子
        List<Long> postIds = bookmarks.stream().map(SquareBookmark::getPostId).toList();
        List<SquarePost> posts = squarePostMapper.selectBatchIds(postIds);

        // 只保留 published 状态的帖子（removed 的自动过滤掉）
        List<SquarePost> publishedPosts = posts.stream()
                .filter(p -> "published".equals(p.getStatus()))
                .toList();

        List<SquarePostVO> voList = batchToPostVO(publishedPosts, userId);

        Page<SquarePostVO> voPage = new Page<>(bookmarkPage.getCurrent(), bookmarkPage.getSize(), bookmarkPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public IPage<SquarePostVO> listMyLikes(Integer current, Integer size, Long userId) {
        Page<SquareLike> page = new Page<>(current, size);
        LambdaQueryWrapper<SquareLike> wrapper = new LambdaQueryWrapper<SquareLike>()
                .eq(SquareLike::getUserId, userId)
                .orderByDesc(SquareLike::getCreatedAt);
        Page<SquareLike> likePage = squareLikeMapper.selectPage(page, wrapper);

        List<SquareLike> likes = likePage.getRecords();
        if (likes.isEmpty()) {
            Page<SquarePostVO> voPage = new Page<>(current, size, 0);
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        List<Long> postIds = likes.stream().map(SquareLike::getPostId).toList();
        List<SquarePost> posts = squarePostMapper.selectBatchIds(postIds);

        // 只保留已发布状态的帖子
        List<SquarePost> publishedPosts = posts.stream()
                .filter(p -> "published".equals(p.getStatus()))
                .toList();

        List<SquarePostVO> voList = batchToPostVO(publishedPosts, userId);

        Page<SquarePostVO> voPage = new Page<>(likePage.getCurrent(), likePage.getSize(), likePage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 举报 ====================

    @Override
    @Transactional
    public void report(Long postId, String reason, Long userId) {
        SquarePost post = squarePostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(400, "帖子不存在");
        }

        // 检查重复举报
        Long existingCount = squareReportMapper.selectCount(
                new LambdaQueryWrapper<SquareReport>()
                        .eq(SquareReport::getPostId, postId)
                        .eq(SquareReport::getReporterId, userId));
        if (existingCount > 0) {
            throw new BusinessException(400, "您已举报过该帖子");
        }

        SquareReport report = new SquareReport();
        report.setPostId(postId);
        report.setReporterId(userId);
        report.setReason(reason);
        report.setStatus("pending");
        report.setCreatedAt(LocalDateTime.now());
        squareReportMapper.insert(report);
        log.info("square_reported reportId={} postId={} userId={}", report.getId(), postId, userId);
    }

    // ==================== 管理 ====================

    @Override
    public IPage<SquareReportVO> listReports(String status, Integer current, Integer size) {
        LambdaQueryWrapper<SquareReport> wrapper = new LambdaQueryWrapper<SquareReport>()
                .orderByAsc(SquareReport::getCreatedAt);

        if (status != null && !status.isBlank()) {
            wrapper.eq(SquareReport::getStatus, status);
        }

        Page<SquareReport> page = new Page<>(current, size);
        Page<SquareReport> resultPage = squareReportMapper.selectPage(page, wrapper);

        List<SquareReport> reports = resultPage.getRecords();
        List<SquareReportVO> voList = new ArrayList<>();
        for (SquareReport report : reports) {
            SquareReportVO vo = new SquareReportVO();
            vo.setId(report.getId());
            vo.setPostId(report.getPostId());
            vo.setReporterId(report.getReporterId());
            vo.setReason(report.getReason());
            vo.setStatus(report.getStatus());
            vo.setHandlerId(report.getHandlerId());
            vo.setHandleNote(report.getHandleNote());
            vo.setCreatedAt(report.getCreatedAt());
            vo.setHandledAt(report.getHandledAt());

            // 查帖子标题
            SquarePost post = squarePostMapper.selectById(report.getPostId());
            if (post != null) {
                KnowledgeNode node = knowledgeNodeMapper.selectById(post.getNodeId());
                vo.setPostTitle(node != null ? node.getTitle() : "已删除");
            }

            // 查举报人名称
            User reporter = userMapper.selectById(report.getReporterId());
            vo.setReporterName(reporter != null ? reporter.getUsername() : "未知");

            voList.add(vo);
        }

        Page<SquareReportVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional
    public void handleReport(Long reportId, String action, String handleNote, Long handlerId) {
        SquareReport report = squareReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(400, "举报记录不存在");
        }
        if (!"pending".equals(report.getStatus())) {
            throw new BusinessException(400, "该举报已处理");
        }

        LocalDateTime now = LocalDateTime.now();

        if ("remove".equals(action)) {
            // 下架帖子
            SquarePost post = squarePostMapper.selectById(report.getPostId());
            if (post != null) {
                post.setStatus("removed");
                post.setUpdatedAt(now);
                squarePostMapper.updateById(post);

                // 通知帖子作者内容被下架
                notificationService.create(post.getAuthorId(), "report_result",
                        "你的分享内容已被管理员下架",
                        handleNote, "post", post.getId());
            }
            report.setStatus("removed");
        } else if ("ignore".equals(action)) {
            report.setStatus("ignored");
        } else {
            throw new BusinessException(400, "无效的处理方式: " + action);
        }

        report.setHandlerId(handlerId);
        report.setHandleNote(handleNote);
        report.setHandledAt(now);
        squareReportMapper.updateById(report);

        // 同帖子的其他 pending 举报也一并处理
        List<SquareReport> samePostReports = squareReportMapper.selectList(
                new LambdaQueryWrapper<SquareReport>()
                        .eq(SquareReport::getPostId, report.getPostId())
                        .eq(SquareReport::getStatus, "pending"));
        for (SquareReport r : samePostReports) {
            r.setStatus(report.getStatus());
            r.setHandlerId(handlerId);
            r.setHandleNote("与举报#" + reportId + " 一并处理");
            r.setHandledAt(now);
            squareReportMapper.updateById(r);
        }

        log.info("square_report_handled reportId={} action={} handlerId={}", reportId, action, handlerId);
    }

    // ==================== 敏感词 ====================

    @Override
    public List<SensitiveWord> getSensitiveWords() {
        return getSensitiveWordCache();
    }

    @Override
    @Transactional
    public void addSensitiveWord(String word) {
        if (word == null || word.isBlank()) {
            throw new BusinessException(400, "敏感词不能为空");
        }
        Long existing = sensitiveWordMapper.selectCount(
                new LambdaQueryWrapper<SensitiveWord>().eq(SensitiveWord::getWord, word));
        if (existing > 0) {
            throw new BusinessException(400, "该敏感词已存在");
        }
        SensitiveWord sw = new SensitiveWord();
        sw.setWord(word);
        sw.setCreatedAt(LocalDateTime.now());
        sensitiveWordMapper.insert(sw);
        refreshSensitiveWordCache();
        log.info("sensitive_word_added word={}", word);
    }

    @Override
    @Transactional
    public void deleteSensitiveWord(Long id) {
        SensitiveWord sw = sensitiveWordMapper.selectById(id);
        if (sw == null) {
            throw new BusinessException(400, "敏感词不存在");
        }
        sensitiveWordMapper.deleteById(id);
        refreshSensitiveWordCache();
        log.info("sensitive_word_deleted id={} word={}", id, sw.getWord());
    }

    // ==================== 私有方法 ====================

    private List<Long> normalizeNodeIds(List<Long> nodeIds, Long legacyNodeId) {
        List<Long> resolvedNodeIds = (nodeIds == null || nodeIds.isEmpty())
                ? (legacyNodeId == null ? Collections.emptyList() : Collections.singletonList(legacyNodeId))
                : nodeIds;
        if (resolvedNodeIds.isEmpty()) {
            throw new BusinessException(400, "请选择至少一个知识节点");
        }
        if (resolvedNodeIds.size() > MAX_PUBLISHED_NODE_COUNT) {
            throw new BusinessException(400, "一次最多发布10个知识点");
        }
        if (resolvedNodeIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(400, "知识节点ID不能为空");
        }
        if (new LinkedHashSet<>(resolvedNodeIds).size() != resolvedNodeIds.size()) {
            throw new BusinessException(400, "不能重复选择相同的知识节点");
        }
        return List.copyOf(resolvedNodeIds);
    }

    private Map<Long, KnowledgeNode> loadPublishableNodes(List<Long> nodeIds, Long userId) {
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectBatchIds(nodeIds);
        if (nodes.size() != nodeIds.size()) {
            throw new BusinessException(400, "知识节点不存在或已删除");
        }
        if (nodes.stream().anyMatch(node -> !Objects.equals(node.getUserId(), userId))) {
            throw new BusinessException(403, "只能发布自己的知识节点");
        }
        return nodes.stream().collect(Collectors.toMap(KnowledgeNode::getId, node -> node));
    }

    private void ensureNodesNotAlreadyPublished(List<Long> nodeIds) {
        Long existingLegacyPostCount = squarePostMapper.selectCount(
                new LambdaQueryWrapper<SquarePost>()
                        .in(SquarePost::getNodeId, nodeIds)
                        .ne(SquarePost::getStatus, "removed"));
        if (existingLegacyPostCount > 0) {
            throw new BusinessException(400, "所选知识节点中已有内容发布到广场");
        }

        List<SquarePostNode> nodeRelations = squarePostNodeMapper.selectList(
                new LambdaQueryWrapper<SquarePostNode>().in(SquarePostNode::getNodeId, nodeIds));
        if (nodeRelations.isEmpty()) {
            return;
        }
        Set<Long> relatedPostIds = nodeRelations.stream()
                .map(SquarePostNode::getPostId)
                .collect(Collectors.toSet());
        Long existingRelatedPostCount = squarePostMapper.selectCount(
                new LambdaQueryWrapper<SquarePost>()
                        .in(SquarePost::getId, relatedPostIds)
                        .ne(SquarePost::getStatus, "removed"));
        if (existingRelatedPostCount > 0) {
            throw new BusinessException(400, "所选知识节点中已有内容发布到广场");
        }
    }

    private void savePostNodes(Long postId, List<Long> nodeIds, LocalDateTime createdAt) {
        for (int index = 0; index < nodeIds.size(); index++) {
            SquarePostNode relation = new SquarePostNode();
            relation.setPostId(postId);
            relation.setNodeId(nodeIds.get(index));
            relation.setPosition(index);
            relation.setCreatedAt(createdAt);
            squarePostNodeMapper.insert(relation);
        }
    }

    private List<Long> findMatchingKnowledgeNodeIds(String keyword) {
        return knowledgeNodeMapper.selectList(new LambdaQueryWrapper<KnowledgeNode>()
                        .select(KnowledgeNode::getId)
                        .like(KnowledgeNode::getTitle, keyword))
                .stream()
                .map(KnowledgeNode::getId)
                .toList();
    }

    private List<Long> findRelatedPostIds(List<Long> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(squarePostNodeMapper.selectList(
                        new LambdaQueryWrapper<SquarePostNode>()
                                .select(SquarePostNode::getPostId)
                                .in(SquarePostNode::getNodeId, nodeIds))
                .stream()
                .map(SquarePostNode::getPostId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * 检查文本是否包含敏感词。
     * 使用 AC 自动机一次扫描完成多模式匹配，O(text_length)。
     */
    private void checkSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (getSensitiveWordMatcher().matches(text)) {
            throw new BusinessException(400, "内容包含敏感词，请修改后重试");
        }
    }

    /**
     * 获取敏感词列表（Redis 缓存，TTL 1 小时后从 DB 重新加载）.
     */
    private List<SensitiveWord> getSensitiveWordCache() {
        return cacheService.getOrLoad(
                SystemConstants.SENSITIVE_WORD_CACHE_KEY,
                List.class,
                SystemConstants.CACHE_TTL_HOURS,
                TimeUnit.HOURS,
                () -> sensitiveWordMapper.selectList(null)
        );
    }

    /**
     * 获取编译好的 AC 自动机。
     * DCL + 本地 TTL 保证多实例最终一致性，避免每次检查都重新编译。
     */
    private SensitiveWordMatcher getSensitiveWordMatcher() {
        SensitiveWordMatcher matcher = sensitiveWordMatcher;
        if (matcher != null && System.currentTimeMillis() - matcherBuiltAt < MATCHER_TTL_MS) {
            return matcher;
        }
        synchronized (this) {
            matcher = sensitiveWordMatcher;
            if (matcher != null && System.currentTimeMillis() - matcherBuiltAt < MATCHER_TTL_MS) {
                return matcher;
            }
            List<SensitiveWord> words = getSensitiveWordCache();
            matcher = SensitiveWordMatcher.compile(words);
            sensitiveWordMatcher = matcher;
            matcherBuiltAt = System.currentTimeMillis();
            return matcher;
        }
    }

    /**
     * 刷新敏感词缓存（增删敏感词后调用）.
     * 同时失效 Redis 缓存和本地 AC 自动机，下次检查时重建。
     */
    private void refreshSensitiveWordCache() {
        cacheService.delete(SystemConstants.SENSITIVE_WORD_CACHE_KEY);
        sensitiveWordMatcher = null;
    }

    /**
     * 单个帖子转 VO.
     */
    private SquarePostVO toPostVO(SquarePost post, Long userId) {
        SquarePostVO vo = new SquarePostVO();
        vo.setPostId(post.getId());
        vo.setNodeId(post.getNodeId());
        vo.setRecommendText(post.getRecommendText());
        vo.setAuthorId(post.getAuthorId());
        vo.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0);
        vo.setCommentCount(post.getCommentCount() != null ? post.getCommentCount() : 0);
        vo.setBookmarkCount(post.getBookmarkCount() != null ? post.getBookmarkCount() : 0);
        vo.setStatus(post.getStatus());
        vo.setCreatedAt(post.getCreatedAt());

        fillKnowledgeNodes(vo, post, loadKnowledgeNodesByPost(Collections.singletonList(post)));

        // 作者信息
        User author = userMapper.selectById(post.getAuthorId());
        if (author != null) {
            vo.setAuthorName(author.getUsername());
            vo.setAuthorAvatar(author.getAvatar());
        }

        // 当前用户点赞/收藏状态
        if (userId != null) {
            vo.setIsLiked(squareLikeMapper.selectCount(
                    new LambdaQueryWrapper<SquareLike>()
                            .eq(SquareLike::getPostId, post.getId())
                            .eq(SquareLike::getUserId, userId)) > 0);

            vo.setIsBookmarked(squareBookmarkMapper.selectCount(
                    new LambdaQueryWrapper<SquareBookmark>()
                            .eq(SquareBookmark::getPostId, post.getId())
                            .eq(SquareBookmark::getUserId, userId)) > 0);
        }

        return vo;
    }

    /**
     * 批量帖子转 VO（使用批量查询优化 N+1）.
     */
    private List<SquarePostVO> batchToPostVO(List<SquarePost> posts, Long userId) {
        Map<Long, List<SquarePostKnowledgeNodeVO>> knowledgeNodesByPost = loadKnowledgeNodesByPost(posts);

        // 批量查作者
        Set<Long> authorIds = posts.stream().map(SquarePost::getAuthorId).collect(Collectors.toSet());
        Map<Long, User> authorMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 批量查当前用户点赞/收藏状态
        Set<Long> likedPostIds = Collections.emptySet();
        Set<Long> bookmarkedPostIds = Collections.emptySet();
        if (userId != null) {
            List<Long> postIds = posts.stream().map(SquarePost::getId).toList();
            likedPostIds = squareLikeMapper.selectList(
                    new LambdaQueryWrapper<SquareLike>()
                            .in(SquareLike::getPostId, postIds)
                            .eq(SquareLike::getUserId, userId))
                    .stream().map(SquareLike::getPostId).collect(Collectors.toSet());

            bookmarkedPostIds = squareBookmarkMapper.selectList(
                    new LambdaQueryWrapper<SquareBookmark>()
                            .in(SquareBookmark::getPostId, postIds)
                            .eq(SquareBookmark::getUserId, userId))
                    .stream().map(SquareBookmark::getPostId).collect(Collectors.toSet());
        }

        List<SquarePostVO> result = new ArrayList<>();
        for (SquarePost post : posts) {
            SquarePostVO vo = new SquarePostVO();
            vo.setPostId(post.getId());
            vo.setNodeId(post.getNodeId());
            vo.setRecommendText(post.getRecommendText());
            vo.setAuthorId(post.getAuthorId());
            vo.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0);
            vo.setCommentCount(post.getCommentCount() != null ? post.getCommentCount() : 0);
            vo.setBookmarkCount(post.getBookmarkCount() != null ? post.getBookmarkCount() : 0);
            vo.setStatus(post.getStatus());
            vo.setCreatedAt(post.getCreatedAt());

            fillKnowledgeNodes(vo, post, knowledgeNodesByPost);

            User author = authorMap.get(post.getAuthorId());
            if (author != null) {
                vo.setAuthorName(author.getUsername());
                vo.setAuthorAvatar(author.getAvatar());
            }

            if (userId != null) {
                vo.setIsLiked(likedPostIds.contains(post.getId()));
                vo.setIsBookmarked(bookmarkedPostIds.contains(post.getId()));
            }

            result.add(vo);
        }
        return result;
    }

    private Map<Long, List<SquarePostKnowledgeNodeVO>> loadKnowledgeNodesByPost(List<SquarePost> posts) {
        if (posts.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> postIds = posts.stream().map(SquarePost::getId).collect(Collectors.toSet());
        List<SquarePostNode> relations = squarePostNodeMapper.selectList(
                new LambdaQueryWrapper<SquarePostNode>()
                        .in(SquarePostNode::getPostId, postIds)
                        .orderByAsc(SquarePostNode::getPostId)
                        .orderByAsc(SquarePostNode::getPosition));
        Map<Long, List<SquarePostNode>> relationsByPost = relations.stream()
                .collect(Collectors.groupingBy(SquarePostNode::getPostId));

        Map<Long, List<SquarePostNode>> effectiveRelationsByPost = new HashMap<>();
        for (SquarePost post : posts) {
            List<SquarePostNode> postRelations = relationsByPost.get(post.getId());
            if (postRelations == null || postRelations.isEmpty()) {
                // 关联表在新版本才出现，旧帖仍以原 node_id 组成单节点合集返回。
                if (post.getNodeId() == null) {
                    effectiveRelationsByPost.put(post.getId(), Collections.emptyList());
                    continue;
                }
                SquarePostNode fallbackRelation = new SquarePostNode();
                fallbackRelation.setPostId(post.getId());
                fallbackRelation.setNodeId(post.getNodeId());
                fallbackRelation.setPosition(0);
                effectiveRelationsByPost.put(post.getId(), Collections.singletonList(fallbackRelation));
                continue;
            }
            effectiveRelationsByPost.put(post.getId(), postRelations);
        }

        Set<Long> nodeIds = effectiveRelationsByPost.values().stream()
                .flatMap(List::stream)
                .map(SquarePostNode::getNodeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, KnowledgeNode> nodeMap = nodeIds.isEmpty() ? Collections.emptyMap()
                : knowledgeNodeMapper.selectBatchIds(nodeIds).stream()
                .collect(Collectors.toMap(KnowledgeNode::getId, node -> node));

        Map<Long, List<SquarePostKnowledgeNodeVO>> result = new HashMap<>();
        for (SquarePost post : posts) {
            List<SquarePostKnowledgeNodeVO> nodes = effectiveRelationsByPost.get(post.getId()).stream()
                    .map(relation -> toKnowledgeNodeVO(relation, nodeMap.get(relation.getNodeId())))
                    .toList();
            result.put(post.getId(), nodes);
        }
        return result;
    }

    private SquarePostKnowledgeNodeVO toKnowledgeNodeVO(SquarePostNode relation, KnowledgeNode node) {
        SquarePostKnowledgeNodeVO vo = new SquarePostKnowledgeNodeVO();
        vo.setNodeId(relation.getNodeId());
        vo.setPosition(relation.getPosition());
        if (node == null) {
            vo.setTitle("知识内容已不可见");
            return vo;
        }
        vo.setTitle(node.getTitle());
        vo.setSummary(node.getSummary());
        vo.setContentMd(node.getContentMd());
        return vo;
    }

    private void fillKnowledgeNodes(SquarePostVO vo, SquarePost post,
                                    Map<Long, List<SquarePostKnowledgeNodeVO>> knowledgeNodesByPost) {
        List<SquarePostKnowledgeNodeVO> knowledgeNodes = knowledgeNodesByPost.getOrDefault(
                post.getId(), Collections.emptyList());
        vo.setKnowledgeNodes(knowledgeNodes);
        if (!knowledgeNodes.isEmpty()) {
            SquarePostKnowledgeNodeVO primaryNode = knowledgeNodes.get(0);
            vo.setNodeTitle(primaryNode.getTitle());
            vo.setNodeSummary(primaryNode.getSummary());
        }
    }

    /**
     * 单个评论转 VO.
     */
    private SquareCommentVO toCommentVO(SquareComment comment) {
        SquareCommentVO vo = new SquareCommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setDeleted(comment.getDeleted());

        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }

    /**
     * 批量评论转 VO.
     */
    private List<SquareCommentVO> batchToCommentVO(List<SquareComment> comments) {
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = comments.stream().map(SquareComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<SquareCommentVO> result = new ArrayList<>();
        for (SquareComment comment : comments) {
            SquareCommentVO vo = new SquareCommentVO();
            vo.setId(comment.getId());
            vo.setPostId(comment.getPostId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getContent());
            vo.setCreatedAt(comment.getCreatedAt());
            vo.setDeleted(comment.getDeleted());

            User user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 从源表刷新点赞计数器.
     */
    private void refreshLikeCount(Long postId) {
        long count = squareLikeMapper.selectCount(
                new LambdaQueryWrapper<SquareLike>().eq(SquareLike::getPostId, postId));
        SquarePost post = squarePostMapper.selectById(postId);
        if (post != null) {
            post.setLikeCount((int) count);
            post.setUpdatedAt(LocalDateTime.now());
            squarePostMapper.updateById(post);
        }
    }

    /**
     * 从源表刷新评论计数器（排除已删除评论）.
     */
    private void refreshCommentCount(Long postId) {
        long count = squareCommentMapper.selectCount(
                new LambdaQueryWrapper<SquareComment>()
                        .eq(SquareComment::getPostId, postId)
                        .eq(SquareComment::getDeleted, 0));
        SquarePost post = squarePostMapper.selectById(postId);
        if (post != null) {
            post.setCommentCount((int) count);
            post.setUpdatedAt(LocalDateTime.now());
            squarePostMapper.updateById(post);
        }
    }

    /**
     * 从源表刷新收藏计数器.
     */
    private void refreshBookmarkCount(Long postId) {
        long count = squareBookmarkMapper.selectCount(
                new LambdaQueryWrapper<SquareBookmark>().eq(SquareBookmark::getPostId, postId));
        SquarePost post = squarePostMapper.selectById(postId);
        if (post != null) {
            post.setBookmarkCount((int) count);
            post.setUpdatedAt(LocalDateTime.now());
            squarePostMapper.updateById(post);
        }
    }
}
