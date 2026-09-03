package com.secondbrain.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.common.Result;
import com.secondbrain.dto.CreateCommunityAnswerRequest;
import com.secondbrain.dto.CreateCommunityQuestionRequest;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.service.CommunityQuestionService;
import com.secondbrain.vo.CommunityAnswerVO;
import com.secondbrain.vo.CommunityQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识社区问答控制器。
 *
 * @author AI
 */
@RestController
@RequestMapping("/community/questions")
@Tag(name = "知识社区问答", description = "公开问题、回答和知识引用")
public class CommunityQuestionController {

    private final CommunityQuestionService questionService;

    public CommunityQuestionController(CommunityQuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * 发布公开问题。
     *
     * @param request 问题内容
     * @param httpRequest HTTP请求
     * @return 创建后的问题
     */
    @PostMapping
    @Operation(summary = "发布问题")
    public Result<CommunityQuestionVO> create(@Valid @RequestBody CreateCommunityQuestionRequest request,
                                              HttpServletRequest httpRequest) {
        return Result.success(questionService.createQuestion(request, currentUserId(httpRequest)));
    }

    /**
     * 分页浏览问题。
     *
     * @param sort 排序方式
     * @param keyword 搜索词
     * @param current 当前页
     * @param size 每页大小
     * @return 问题分页
     */
    @GetMapping
    @Operation(summary = "问题列表")
    public Result<IPage<CommunityQuestionVO>> list(
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(questionService.listQuestions(sort, keyword, current, size));
    }

    /**
     * 获取问题与回答详情。
     *
     * @param id 问题ID
     * @return 问题详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "问题详情")
    public Result<CommunityQuestionVO> detail(@PathVariable Long id) {
        return Result.success(questionService.getQuestion(id));
    }

    /**
     * 回答问题并可附带个人知识点公开快照。
     *
     * @param id 问题ID
     * @param request 回答内容
     * @param httpRequest HTTP请求
     * @return 创建后的回答
     */
    @PostMapping("/{id}/answers")
    @Operation(summary = "发布回答")
    public Result<CommunityAnswerVO> answer(@PathVariable Long id,
                                            @Valid @RequestBody CreateCommunityAnswerRequest request,
                                            HttpServletRequest httpRequest) {
        return Result.success(questionService.createAnswer(id, request, currentUserId(httpRequest)));
    }

    /**
     * 采纳指定回答。
     *
     * @param id 问题ID
     * @param answerId 回答ID
     * @param httpRequest HTTP请求
     * @return 空响应
     */
    @PostMapping("/{id}/answers/{answerId}/accept")
    @Operation(summary = "采纳回答")
    public Result<Void> accept(@PathVariable Long id, @PathVariable Long answerId,
                               HttpServletRequest httpRequest) {
        questionService.acceptAnswer(id, answerId, currentUserId(httpRequest));
        return Result.success("回答已采纳");
    }

    private Long currentUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
