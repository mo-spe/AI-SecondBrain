package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.ReviewResultDTO;
import com.secondbrain.service.ReviewCardService;
import com.secondbrain.util.JwtUtil;
import com.secondbrain.vo.ReviewCardVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/review")
@CrossOrigin
public class ReviewCardController {

    private final ReviewCardService reviewCardService;
    private final JwtUtil jwtUtil;

    @Autowired
    public ReviewCardController(ReviewCardService reviewCardService, JwtUtil jwtUtil) {
        this.reviewCardService = reviewCardService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/generate")
    public Result<ReviewCardVO> generateReviewCard(@RequestBody GenerateCardRequest request, HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            com.secondbrain.entity.ReviewCard card = reviewCardService.generateReviewCard(
                    request.getNodeId(), request.getCardType()
            );

            if (card == null) {
                return Result.error("生成复习卡片失败");
            }

            ReviewCardVO vo = convertToVO(card);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("生成复习卡片失败：" + e.getMessage());
        }
    }

    @GetMapping("/today")
    public Result<List<ReviewCardVO>> getTodayReviewCards(
            @RequestParam(required = false) String sortBy,
            HttpServletRequest httpRequest) {
        try {
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
        } catch (Exception e) {
            return Result.error("获取今日复习卡片失败：" + e.getMessage());
        }
    }

    private List<ReviewCardVO> sortReviewCards(List<ReviewCardVO> cards, String sortBy) {
        return cards.stream()
                .sorted((a, b) -> {
                    switch (sortBy) {
                        case "time":
                            return a.getNextReviewTime().compareTo(b.getNextReviewTime());
                        case "difficulty":
                            return b.getDifficulty().compareTo(a.getDifficulty());
                        case "accuracy":
                            return a.getAverageAccuracy().compareTo(b.getAverageAccuracy());
                        default:
                            return 0;
                    }
                })
                .toList();
    }

    @PostMapping("/submit")
    public Result<ReviewResultDTO> submitReviewResult(@RequestBody SubmitReviewRequest request, HttpServletRequest httpRequest) {
        try {
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
        } catch (Exception e) {
            return Result.error("提交复习结果失败：" + e.getMessage());
        }
    }

    @GetMapping("/node/{nodeId}")
    public Result<List<ReviewCardVO>> getReviewCardsByNodeId(@PathVariable Long nodeId, HttpServletRequest httpRequest) {
        try {
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
        } catch (Exception e) {
            return Result.error("获取复习卡片失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteReviewCard(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            reviewCardService.deleteReviewCard(id);

            return Result.success();
        } catch (Exception e) {
            return Result.error("删除复习卡片失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/all")
    public Result<Void> deleteAllReviewCards(HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            reviewCardService.deleteAllReviewCards();

            return Result.success();
        } catch (Exception e) {
            return Result.error("删除所有复习卡片失败：" + e.getMessage());
        }
    }

    @PostMapping("/generate-all")
    public Result<String> generateAllReviewCards(HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            int generatedCount = reviewCardService.generateReviewCardsForAllNodes();
            
            return Result.success("成功生成" + generatedCount + "张复习卡片");
        } catch (Exception e) {
            return Result.error("生成复习卡片失败：" + e.getMessage());
        }
    }

    @PostMapping("/update-answers")
    public Result<String> updateMissingAnswers() {
        try {
            reviewCardService.updateMissingAnswers();
            return Result.success("成功更新缺失答案的复习卡片");
        } catch (Exception e) {
            return Result.error("更新答案失败：" + e.getMessage());
        }
    }

    @GetMapping("/streak-days")
    public Result<Integer> getStreakDays(HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            int streakDays = reviewCardService.calculateStreakDays(userId);
            
            return Result.success(streakDays);
        } catch (Exception e) {
            return Result.error("获取连续天数失败：" + e.getMessage());
        }
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

        return vo;
    }

    public static class GenerateCardRequest {
        private Long nodeId;
        private String cardType;

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
    }

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
}
