package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.ReviewResultDTO;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;
import com.secondbrain.entity.ReviewCardPool;
import com.secondbrain.entity.UserReviewCard;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.ReviewCardPoolService;
import com.secondbrain.service.ReviewCardService;
import com.secondbrain.vo.ReviewCardPoolVO;
import com.secondbrain.vo.ReviewCardVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** 复习卡片控制器. <p>提供复习卡片的生成、查询、提交、删除等接口</p> */
@RestController
@RequestMapping("/review")
@CrossOrigin
public class ReviewCardController {

    private static final Logger log = LoggerFactory.getLogger(ReviewCardController.class);

    private final ReviewCardService reviewCardService;
    private final ReviewCardPoolService poolService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    public ReviewCardController(ReviewCardService reviewCardService, ReviewCardPoolService poolService,
                                KnowledgeNodeMapper knowledgeNodeMapper) {
        this.reviewCardService = reviewCardService;
        this.poolService = poolService;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
    }

    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    /**
     * 生成复习卡片.
     *
     * @param request 生成卡片请求
     * @param httpRequest HTTP请求对象
     * @return 复习卡片
     */
    @PostMapping("/generate")
    public Result<ReviewCardVO> generateReviewCard(@RequestBody GenerateCardRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String generationType = request.getGenerationType() != null ? request.getGenerationType() : "auto";
        ReviewCard card = reviewCardService.generateReviewCard(
                request.getNodeId(), request.getCardType(), generationType, userId
        );

        if (card == null) {
            return Result.error("生成复习卡片失败");
        }

        ReviewCardVO vo = convertToVO(card);
        return Result.success(vo);
    }

    /**
     * 获取今日复习卡片.
     *
     * @param sortBy 排序方式
     * @param httpRequest HTTP请求对象
     * @return 今日复习卡片列表
     */
    @GetMapping("/today")
    public Result<List<ReviewCardVO>> getTodayReviewCards(
            @RequestParam(required = false) String sortBy,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        List<com.secondbrain.entity.ReviewCard> cards = reviewCardService.getTodayReviewCards(userId, workspaceId);

        List<ReviewCardVO> vos = cards.stream()
                .map(this::convertToVO)
                // 过滤脏数据：question 为空或 nodeTitle/question 都为空的卡片
                .filter(vo -> vo.getQuestion() != null && !vo.getQuestion().isBlank())
                // 过滤空题卡：question 只有 A. B. C. 选项无正文的兜底在 parseQuestionText 做
                .toList();

        if (sortBy != null) {
            vos = sortReviewCards(vos, sortBy);
        }

        return Result.success(vos);
    }

    private List<ReviewCardVO> sortReviewCards(List<ReviewCardVO> cards, String sortBy) {
        return cards.stream()
                .sorted((a, b) -> {
                    switch (sortBy) {
                        case "time":
                            return a.getNextReviewTime().compareTo(b.getNextReviewTime());
                        case "difficulty":
                            return b.getDifficulty().compareTo(a.getDifficulty());
                        default:
                            return 0;
                    }
                })
                .toList();
    }

    /**
     * 提交复习结果.
     *
     * @param request 提交复习请求
     * @param httpRequest HTTP请求对象
     * @return 复习结果
     */
    @PostMapping("/submit")
    public Result<ReviewResultDTO> submitReviewResult(@RequestBody SubmitReviewRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        ReviewResultDTO result = reviewCardService.submitReviewResult(
                request.getCardId(),
                request.getUserAnswer(),
                request.getDuration(),
                userId
        );

        return Result.success(result);
    }

    /**
     * 根据节点ID获取复习卡片.
     *
     * @param nodeId 知识节点ID
     * @param httpRequest HTTP请求对象
     * @return 复习卡片列表
     */
    @GetMapping("/node/{nodeId}")
    public Result<List<ReviewCardVO>> getReviewCardsByNodeId(@PathVariable Long nodeId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        List<com.secondbrain.entity.ReviewCard> cards = reviewCardService.getReviewCardsByNodeId(nodeId, userId, workspaceId);

        List<ReviewCardVO> vos = cards.stream()
                .map(this::convertToVO)
                .toList();

        return Result.success(vos);
    }

    /**
     * 删除复习卡片.
     *
     * @param id 卡片ID
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteReviewCard(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        reviewCardService.deleteReviewCard(id, userId);
        return Result.success();
    }

    /**
     * 删除所有复习卡片.
     *
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @DeleteMapping("/all")
    public Result<Void> deleteAllReviewCards(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        reviewCardService.deleteAllReviewCards(userId, workspaceId);
        return Result.success();
    }

    /**
     * 为所有节点生成复习卡片.
     *
     * @param httpRequest HTTP请求对象
     * @return 生成结果信息
     */
    @PostMapping("/generate-all")
    public Result<String> generateAllReviewCards(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        int generatedCount = reviewCardService.generateReviewCardsForAllNodes(userId, workspaceId);
        return Result.success("成功生成" + generatedCount + "张练习卡片");
    }

    /**
     * 恢复复习卡片.
     *
     * @param httpRequest HTTP请求对象
     * @return 恢复数量
     */
    @PostMapping("/restore")
    public Result<Integer> restoreReviewCards(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        int restoredCount = reviewCardService.restoreReviewCards(userId, workspaceId);
        return Result.success(restoredCount);
    }

    /**
     * 更新缺失答案的复习卡片.
     *
     * @return 更新结果信息
     */
    @PostMapping("/update-answers")
    public Result<String> updateMissingAnswers() {
        reviewCardService.updateMissingAnswers();
        return Result.success("成功更新缺失答案的复习卡片");
    }

    /**
     * 获取连续复习天数.
     *
     * @param httpRequest HTTP请求对象
     * @return 连续复习天数
     */
    @GetMapping("/streak-days")
    public Result<Integer> getStreakDays(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        int streakDays = reviewCardService.calculateStreakDays(userId, workspaceId);
        return Result.success(streakDays);
    }

    /**
     * 提交质量反馈.
     *
     * @param request 质量反馈请求
     * @param httpRequest HTTP请求对象
     * @return 反馈结果
     */
    @PostMapping("/quality-feedback")
    public Result<String> submitQualityFeedback(@RequestBody QualityFeedbackRequest request, HttpServletRequest httpRequest) {
        reviewCardService.recordQualityFeedback(
                request.getCardId(),
                request.getRating(),
                request.getComment()
        );
        return Result.success("感谢您的反馈！");
    }

    /**
     * 获取用户准确率.
     *
     * @param httpRequest HTTP请求对象
     * @return 准确率
     */
    @GetMapping("/accuracy")
    public Result<Integer> getUserAccuracy(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        int accuracy = reviewCardService.getUserAccuracy(userId, workspaceId);
        return Result.success(accuracy);
    }

    /**
     * 获取复习中心概览统计（紫框、卡片、队列分类数量等）.
     *
     * @param httpRequest HTTP请求对象
     * @return 概览统计 Map
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        Map<String, Object> data = reviewCardService.getOverview(userId, workspaceId);
        return Result.success(data);
    }

    // ========== 题目池接口 ==========

    /**
     * 获取工作区题目池列表（含社区标签）.
     *
     * @param workspaceId 工作区ID
     * @param httpRequest HTTP请求对象
     * @return 题目池列表
     */
    @GetMapping("/pool")
    public Result<List<ReviewCardPoolVO>> getPoolList(@RequestParam Long workspaceId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<ReviewCardPoolVO> list = poolService.getPoolList(workspaceId, userId);
        return Result.success(list);
    }

    /**
     * 获取池子题目详情.
     *
     * @param poolId 池子题目ID
     * @param httpRequest HTTP请求对象
     * @return 题目详情
     */
    @GetMapping("/pool/{poolId}")
    public Result<ReviewCardPoolVO> getPoolDetail(@PathVariable Long poolId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        ReviewCardPoolVO vo = poolService.getPoolDetail(poolId, userId);
        return Result.success(vo);
    }

    /**
     * 加入复习（生成个人副本）.
     *
     * @param poolId 池子题目ID
     * @param httpRequest HTTP请求对象
     * @return 个人副本
     */
    @PostMapping("/pool/{poolId}/join")
    public Result<UserReviewCard> joinPool(@PathVariable Long poolId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        UserReviewCard card = poolService.joinPool(poolId, userId);
        return Result.success(card);
    }

    /**
     * 删除池子题目（仅 owner）.
     *
     * @param poolId 池子题目ID
     * @param httpRequest HTTP请求对象
     */
    @DeleteMapping("/pool/{poolId}")
    public Result<Void> deletePoolItem(@PathVariable Long poolId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        poolService.deletePoolItem(poolId, userId);
        return Result.success();
    }

    /**
     * 编辑池子题目（仅 owner）.
     *
     * @param poolId 池子题目ID
     * @param request 编辑请求
     * @param httpRequest HTTP请求对象
     * @return 更新后的池子题目
     */
    @PutMapping("/pool/{poolId}")
    public Result<ReviewCardPool> updatePoolItem(@PathVariable Long poolId, @RequestBody UpdatePoolRequest request,
                                                  HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        ReviewCardPool pool = poolService.updatePoolItem(poolId, request.getQuestion(), request.getAnswer(), userId);
        return Result.success(pool);
    }

    private ReviewCardVO convertToVO(ReviewCard card) {
        ReviewCardVO vo = new ReviewCardVO();
        BeanUtils.copyProperties(card, vo);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (card.getLastReviewTime() != null) {
            vo.setLastReviewTime(card.getLastReviewTime().format(formatter));
        }
        if (card.getNextReviewTime() != null) {
            vo.setNextReviewTime(card.getNextReviewTime().format(formatter));
        }
        if (card.getCreateTime() != null) {
            vo.setCreateTime(card.getCreateTime().format(formatter));
        }

        // 获取知识点的复习次数和掌握程度
        if (card.getNodeId() != null) {
           KnowledgeNode node = knowledgeNodeMapper.selectById(card.getNodeId());
            if (node != null) {
                vo.setNodeReviewCount(node.getReviewCount() != null ? node.getReviewCount() : 0);
                vo.setNodeMasteryLevel(node.getMasteryLevel() != null ? node.getMasteryLevel() : 0);
                vo.setNodeTitle(node.getTitle());
            }
        }

        return vo;
    }

    /**
     * 生成卡片请求
     */
    @Getter
    @Setter
    public static class GenerateCardRequest {
        /**
         * 知识点ID
         */
        private Long nodeId;
        /**
         * 卡片类型（choice/fill/essay/judge）
         */
        private String cardType;
        /**
         * 生成类型（auto/manual）
         */
        private String generationType;
    }

    /**
     * 提交复习结果请求
     */
    @Getter
    @Setter
    public static class SubmitReviewRequest {
        /**
         * 卡片ID
         */
        private Long cardId;
        /**
         * 用户答案
         */
        private String userAnswer;
        /**
         * 答题耗时（秒）
         */
        private Integer duration;
    }

    /**
     * 质量反馈请求
     */
    @Getter
    @Setter
    public static class QualityFeedbackRequest {
        /**
         * 卡片ID
         */
        private Long cardId;
        /**
         * 评分（1-5）
         */
        private Integer rating;
        /**
         * 反馈内容
         */
        private String comment;
    }

    /**
     * 编辑池子题目请求
     */
    @Getter
    @Setter
    public static class UpdatePoolRequest {
        private String question;
        private String answer;
    }
}
