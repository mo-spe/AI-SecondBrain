package com.secondbrain.service;

import com.secondbrain.dto.AiCallConfig;
import com.secondbrain.dto.KnowledgeDTO;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI服务接口.
 * <p>提供知识提取、题目生成、答案生成、对话等AI能力。
 * 新方法使用 userId + scenarioCode 从数据库动态获取配置，
 * 旧方法（带 userApiKey 参数）标记为 @Deprecated 保留兼容</p>
 */
public interface AiService {

    // ========== 新接口（数据库驱动配置） ==========

    /**
     * 解析用户在某场景下的AI调用配置.
     *
     * @param userId       用户ID
     * @param scenarioCode 场景代码
     * @return AI调用配置，未配置时抛出 BusinessException
     */
    AiCallConfig resolveConfig(Long userId, String scenarioCode);

    /**
     * 使用用户配置生成答案.
     *
     * @param userId       用户ID
     * @param scenarioCode 场景代码
     * @param prompt       提示词
     * @return 答案内容
     */
    String generateAnswer(Long userId, String scenarioCode, String prompt);

    /**
     * 使用用户配置生成题目.
     *
     * @param userId       用户ID
     * @param scenarioCode 场景代码
     * @param prompt       提示词
     * @return 题目内容
     */
    String generateQuestion(Long userId, String scenarioCode, String prompt);

    /**
     * 使用用户配置提取知识.
     *
     * @param userId       用户ID
     * @param scenarioCode 场景代码
     * @param content      内容
     * @return 知识列表
     */
    List<KnowledgeDTO> extractKnowledge(Long userId, String scenarioCode, String content);

    /**
     * 使用用户配置执行多轮对话.
     *
     * @param userId       用户ID
     * @param scenarioCode 场景代码
     * @param messages     消息列表，每个元素包含 role 和 content
     * @return AI回复内容
     */
    String chat(Long userId, String scenarioCode, List<Map<String, String>> messages);

    /**
     * 使用用户配置执行流式多轮对话.
     *
     * <p>每收到一个 token 片段就回调 onChunk，避免长生成任务超时。</p>
     *
     * @param userId       用户ID
     * @param scenarioCode 场景代码
     * @param messages     消息列表
     * @param onChunk      每块内容的回调
     * @return 完整回复内容
     */
    String chatStream(Long userId, String scenarioCode,
                      List<Map<String, String>> messages, Consumer<String> onChunk);

    // ========== 旧接口（保留兼容，逐步迁移） ==========

    /** @deprecated 使用 {@link #extractKnowledge(Long, String, String)} 替代 */
    @Deprecated
    List<KnowledgeDTO> extractKnowledge(String content);

    /** @deprecated 使用 {@link #extractKnowledge(Long, String, String)} 替代 */
    @Deprecated
    List<KnowledgeDTO> extractKnowledge(String content, String userApiKey);

    /** @deprecated 使用 {@link #generateQuestion(Long, String, String)} 替代 */
    @Deprecated
    String generateQuestion(String prompt);

    /** @deprecated 使用 {@link #generateQuestion(Long, String, String)} 替代 */
    @Deprecated
    String generateQuestion(String prompt, String userApiKey);

    /** @deprecated 使用 {@link #generateAnswer(Long, String, String)} 替代 */
    @Deprecated
    String generateAnswer(String prompt);

    /** @deprecated 使用 {@link #generateAnswer(Long, String, String)} 替代 */
    @Deprecated
    String generateAnswer(String prompt, String userApiKey);

    /** @deprecated 使用 {@link #chat(Long, String, List)} 替代 */
    @Deprecated
    String chat(List<com.unfbx.chatgpt.entity.chat.Message> messages, String userApiKey);
}
