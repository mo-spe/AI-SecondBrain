package com.secondbrain.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 流式AI调用服务接口.
 *
 * <p>与 {@link AiService} 并列，专门处理 SSE 流式输出场景。
 * 支持 OpenAI 兼容、Anthropic、Gemini 三种 API 路径</p>
 */
public interface StreamingAiService {

    /**
     * 流式对话，逐 token 回调.
     *
     * @param userId       用户ID（用于解析AI配置）
     * @param scenarioCode 场景代码
     * @param systemPrompt 系统提示词
     * @param messages     消息列表（role + content）
     * @param onToken      每个 token 的回调
     * @throws Exception 流式调用失败
     */
    void streamChat(Long userId, String scenarioCode, String systemPrompt,
                    List<Map<String, String>> messages,
                    Consumer<String> onToken) throws Exception;
}
