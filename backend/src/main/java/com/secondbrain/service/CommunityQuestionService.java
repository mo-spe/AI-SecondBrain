package com.secondbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.dto.CreateCommunityAnswerRequest;
import com.secondbrain.dto.CreateCommunityQuestionRequest;
import com.secondbrain.vo.CommunityAnswerVO;
import com.secondbrain.vo.CommunityQuestionVO;

/**
 * 社区问答服务。
 *
 * @author AI
 */
public interface CommunityQuestionService {

    /**
     * 发布公开问题。
     *
     * @param request 问题内容
     * @param userId 当前用户ID
     * @return 创建后的问题
     */
    CommunityQuestionVO createQuestion(CreateCommunityQuestionRequest request, Long userId);

    /**
     * 分页浏览公开问题。
     *
     * @param sort 排序方式：newest/unanswered
     * @param keyword 搜索词
     * @param current 当前页
     * @param size 每页大小
     * @return 问题分页
     */
    IPage<CommunityQuestionVO> listQuestions(String sort, String keyword, Integer current, Integer size);

    /**
     * 获取问题及其回答详情。
     *
     * @param questionId 问题ID
     * @return 问题详情
     */
    CommunityQuestionVO getQuestion(Long questionId);

    /**
     * 发布回答，并为用户主动选择的知识点创建公开快照。
     *
     * @param questionId 问题ID
     * @param request 回答内容
     * @param userId 当前用户ID
     * @return 创建后的回答
     */
    CommunityAnswerVO createAnswer(Long questionId, CreateCommunityAnswerRequest request, Long userId);

    /**
     * 采纳问题下的一条回答。
     *
     * @param questionId 问题ID
     * @param answerId 回答ID
     * @param userId 当前用户ID
     */
    void acceptAnswer(Long questionId, Long answerId, Long userId);
}
