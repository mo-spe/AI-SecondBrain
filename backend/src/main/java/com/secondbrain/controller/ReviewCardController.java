package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.ReviewResultDTO;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.ReviewCardService;
import com.secondbrain.util.JwtUtil;
import com.secondbrain.vo.ReviewCardVO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 复习卡片控制器
 * 提供复习卡片的生成、查询、提交、删除等接口
 */
@RestController
@RequestMapping("/review")
@CrossOrigin
public class ReviewCardController {

    private static final Logger log = LoggerFactory.getLogger(ReviewCardController.class);

    private final ReviewCardService reviewCardService;
    private final JwtUtil jwtUtil;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    public ReviewCardController(ReviewCardService reviewCardService, JwtUtil jwtUtil, KnowledgeNodeMapper knowledgeNodeMapper) {
        this.reviewCardService = reviewCardService;
        this.jwtUtil = jwtUtil;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
    }

    /**
     * 生成复习卡片
     *
     * @param request 生成卡片请求
     * @param httpRequest HTTP请求
     * @return 生成的复习卡片
     */
    @PostMapping("/generate")
    public Result<ReviewCardVO> generateReviewCard(@RequestBody GenerateCardRequest request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        String generationType = request.getGenerationType() != null ? request.getGenerationType() : "auto";
        com.secondbrain.entity.ReviewCard card = reviewCardService.generateReviewCard(
                request.getNodeId(), request.getCardType(), generationType
        );

        if (card == null) {
            return Result.error("生成复习卡片失败");
        }

        ReviewCardVO vo = convertToVO(card);
        return Result.success(vo);
    }

    /**
     * 获取今日复习卡片
     *
     * @param sortBy 排序方式
     * @param httpRequest HTTP请求
     * @return 今日复习卡片列表
     */
    @GetMapping("/today")
    public Result<List<ReviewCardVO>> getTodayReviewCards(
            @RequestParam(required = false) String sortBy,
            HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        List<com.secondbrain.entity.ReviewCard> cards = reviewCardService.getTodayReviewCards(userId);

        List<ReviewCardVO> vos = cards.stream()
                .map(this::convertToVO)
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
     * 提交复习结果
     *
     * @param request 提交复习结果请求
     * @param httpRequest HTTP请求
     * @return 复习结果
     */
    @PostMapping("/submit")
    public Result<ReviewResultDTO> submitReviewResult(@RequestBody SubmitReviewRequest request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        ReviewResultDTO result = reviewCardService.submitReviewResult(
                request.getCardId(),
                request.getUserAnswer(),
                request.getDuration()
        );

        return Result.success(result);
    }

    /**
     * 根据节点ID获取复习卡片
     *
     * @param nodeId 知识节点ID
     * @param httpRequest HTTP请求
     * @return 复习卡片列表
     */
    @GetMapping("/node/{nodeId}")
    public Result<List<ReviewCardVO>> getReviewCardsByNodeId(@PathVariable Long nodeId, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        List<com.secondbrain.entity.ReviewCard> cards = reviewCardService.getReviewCardsByNodeId(nodeId);

        List<ReviewCardVO> vos = cards.stream()
                .map(this::convertToVO)
                .toList();

        return Result.success(vos);
    }

    /**
     * 删除复习卡片
     *
     * @param id 卡片ID
     * @param httpRequest HTTP请求
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteReviewCard(@PathVariable Long id, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        reviewCardService.deleteReviewCard(id);

        return Result.success();
    }

    /**
     * 删除所有复习卡片
     *
     * @param httpRequest HTTP请求
     * @return 删除结果
     */
    @DeleteMapping("/all")
    public Result<Void> deleteAllReviewCards(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        reviewCardService.deleteAllReviewCards(userId);

        return Result.success();
    }

    /**
     * 为所有节点生成复习卡片
     *
     * @param httpRequest HTTP请求
     * @return 生成结果
     */
    @PostMapping("/generate-all")
    public Result<String> generateAllReviewCards(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        int generatedCount = reviewCardService.generateReviewCardsForAllNodes(userId);
        
        return Result.success("成功生成" + generatedCount + "张练习卡片");
    }

    /**
     * 恢复复习卡片
     *
     * @param httpRequest HTTP请求
     * @return 恢复的卡片数量
     */
    @PostMapping("/restore")
    public Result<Integer> restoreReviewCards(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        int restoredCount = reviewCardService.restoreReviewCards(userId);
        
        return Result.success(restoredCount);
    }

    /**
     * 更新缺失答案的复习卡片
     *
     * @return 更新结果
     */
    @PostMapping("/update-answers")
    public Result<String> updateMissingAnswers() {
        reviewCardService.updateMissingAnswers();
        return Result.success("成功更新缺失答案的复习卡片");
    }

    /**
     * 获取连续复习天数
     *
     * @param httpRequest HTTP请求
     * @return 连续复习天数
     */
    @GetMapping("/streak-days")
    public Result<Integer> getStreakDays(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        int streakDays = reviewCardService.calculateStreakDays(userId);
        
        return Result.success(streakDays);
    }

    /**
     * 提交质量反馈
     *
     * @param request 质量反馈请求
     * @param httpRequest HTTP请求
     * @return 提交结果
     */
    @PostMapping("/quality-feedback")
    public Result<String> submitQualityFeedback(@RequestBody QualityFeedbackRequest request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        reviewCardService.recordQualityFeedback(
                request.getCardId(),
                request.getRating(),
                request.getComment()
        );
        
        return Result.success("感谢您的反馈！");
    }

    /**
     * 获取用户准确率
     *
     * @param httpRequest HTTP请求
     * @return 用户准确率
     */
    @GetMapping("/accuracy")
    public Result<Integer> getUserAccuracy(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        int accuracy = reviewCardService.getUserAccuracy(userId);
        
        return Result.success(accuracy);
    }

    private ReviewCardVO convertToVO(com.secondbrain.entity.ReviewCard card) {
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
            com.secondbrain.entity.KnowledgeNode node = knowledgeNodeMapper.selectById(card.getNodeId());
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
    public static class GenerateCardRequest {
        private Long nodeId;
        private String cardType;
        private String generationType;

        public Long getNodeId() {
            return nodeId;
        }

        public void setNodeId(Long nodeId) {
            this.nodeId = nodeId;
        }

        public String getCardType() {
            return cardType;
        }

        public void setCardType(String cardType) {
            this.cardType = cardType;
        }

        public String getGenerationType() {
            return generationType;
        }

        public void setGenerationType(String generationType) {
            this.generationType = generationType;
        }
    }

    /**
     * 提交复习结果请求
     */
    public static class SubmitReviewRequest {
        private Long cardId;
        private String userAnswer;
        private Integer duration;

        public Long getCardId() {
            return cardId;
        }

        public void setCardId(Long cardId) {
            this.cardId = cardId;
        }

        public String getUserAnswer() {
            return userAnswer;
        }

        public void setUserAnswer(String userAnswer) {
            this.userAnswer = userAnswer;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }
    }

    /**
     * 质量反馈请求
     */
    public static class QualityFeedbackRequest {
        private Long cardId;
        private Integer rating;
        private String comment;

        public Long getCardId() {
            return cardId;
        }

        public void setCardId(Long cardId) {
            this.cardId = cardId;
        }

        public Integer getRating() {
            return rating;
        }

        public void setRating(Integer rating) {
            this.rating = rating;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
