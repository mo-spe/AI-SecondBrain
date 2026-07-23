package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.service.AiService;
import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse;
import com.unfbx.chatgpt.entity.chat.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI服务实现类.
 * <p>提供基于OpenAI的知识提取、问答、问题生成、多轮对话等能力</p>
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    @Value("${ai.openai.api-key:}")
    private String systemApiKey;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    /**
     * 创建OpenAI客户端.
     *
     * @param apiKey API Key
     * @return OpenAI客户端
     */
    private OpenAiClient createClient(String apiKey) {
        return OpenAiClient.builder()
                .apiKey(java.util.Collections.singletonList(apiKey))
                .apiHost(baseUrl)
                .build();
    }

    /**
     * 获取有效的API Key.
     *
     * @param userApiKey 用户API Key
     * @return 有效的API Key
     */
    private String getApiKey(String userApiKey) {
        if (userApiKey != null && !userApiKey.isEmpty()) {
            log.info("使用用户自定义 API Key");
            return userApiKey;
        }

        if (systemApiKey != null && !systemApiKey.isEmpty()) {
            log.info("使用系统 API Key");
            return systemApiKey;
        }

        throw new IllegalStateException("API Key 未配置");
    }

    /**
     * 从文本内容提取知识点.
     *
     * @param content 文本内容
     * @return 知识点列表
     */
    @Override
    public List<KnowledgeDTO> extractKnowledge(String content) {
        return extractKnowledge(content, null);
    }

    /**
     * 从文本内容提取知识点.
     *
     * @param content 文本内容
     * @param userApiKey 用户API Key
     * @return 知识点列表
     */
    @Override
    public List<KnowledgeDTO> extractKnowledge(String content, String userApiKey) {
        try {
            String apiKey = getApiKey(userApiKey);
            log.info("开始提取知识，内容长度：{}", content.length());

            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder().role(Message.Role.SYSTEM).content(
                "你是一个知识提取助手。请从给定的内容中提取关键知识点，返回JSON数组格式。" +
                "每个元素包含title(标题)、summary(摘要)、content(详细内容)字段。"
            ).build());
            messages.add(Message.builder().role(Message.Role.USER).content(content).build());

            OpenAiClient client = createClient(apiKey);
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model("gpt-4o-mini")
                    .messages(messages)
                    .build();

            ChatCompletionResponse response = client.chatCompletion(chatCompletion);
            String result = response.getChoices().get(0).getMessage().getContent();

            log.info("知识提取成功，结果长度：{}", result.length());
            return JSON.parseArray(result, KnowledgeDTO.class);
        } catch (Exception e) {
            log.error("知识提取失败", e);
            throw new IllegalStateException("知识提取失败：" + e.getMessage());
        }
    }

    /**
     * 根据提示词生成问题.
     *
     * @param prompt 提示词
     * @return 生成的问题
     */
    @Override
    public String generateQuestion(String prompt) {
        return generateQuestion(prompt, null);
    }

    /**
     * 根据提示词生成问题.
     *
     * @param prompt 提示词
     * @param userApiKey 用户API Key
     * @return 生成的问题
     */
    @Override
    public String generateQuestion(String prompt, String userApiKey) {
        try {
            String apiKey = getApiKey(userApiKey);
            log.info("开始生成问题，prompt 长度：{}", prompt.length());

            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder().role(Message.Role.SYSTEM).content("你是一个专业的教育出题助手。").build());
            messages.add(Message.builder().role(Message.Role.USER).content(prompt).build());

            OpenAiClient client = createClient(apiKey);
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model("gpt-4o-mini")
                    .messages(messages)
                    .build();

            ChatCompletionResponse response = client.chatCompletion(chatCompletion);
            String result = response.getChoices().get(0).getMessage().getContent();

            log.info("问题生成成功，结果长度：{}", result.length());
            return result;
        } catch (Exception e) {
            log.error("问题生成失败", e);
            throw new IllegalStateException("问题生成失败：" + e.getMessage());
        }
    }

    /**
     * 根据提示词生成答案.
     *
     * @param prompt 提示词
     * @return 生成的答案
     */
    @Override
    public String generateAnswer(String prompt) {
        return generateAnswer(prompt, null);
    }

    /**
     * 根据提示词生成答案.
     *
     * @param prompt 提示词
     * @param userApiKey 用户API Key
     * @return 生成的答案
     */
    @Override
    public String generateAnswer(String prompt, String userApiKey) {
        try {
            String apiKey = getApiKey(userApiKey);
            log.info("开始生成答案，prompt 长度：{}", prompt.length());

            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder().role(Message.Role.SYSTEM).content("你是一个专业的知识问答助手。").build());
            messages.add(Message.builder().role(Message.Role.USER).content(prompt).build());

            OpenAiClient client = createClient(apiKey);
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model("gpt-4o-mini")
                    .messages(messages)
                    .build();

            ChatCompletionResponse response = client.chatCompletion(chatCompletion);
            String result = response.getChoices().get(0).getMessage().getContent();

            log.info("答案生成成功，结果长度：{}", result.length());
            return result;
        } catch (Exception e) {
            log.error("答案生成失败", e);
            throw new IllegalStateException("答案生成失败", e);
        }
    }

    /**
     * 多轮对话.
     *
     * @param messages 消息列表
     * @param userApiKey 用户API Key
     * @return 对话结果
     */
    @Override
    public String chat(List<Message> messages, String userApiKey) {
        try {
            String apiKey = getApiKey(userApiKey);
            log.info("开始聊天，消息数量：{}", messages.size());

            OpenAiClient client = createClient(apiKey);
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model("gpt-4o-mini")
                    .messages(messages)
                    .build();

            ChatCompletionResponse response = client.chatCompletion(chatCompletion);
            String result = response.getChoices().get(0).getMessage().getContent();

            log.info("聊天完成，结果长度：{}", result.length());
            return result;
        } catch (Exception e) {
            log.error("聊天失败", e);
            throw new IllegalStateException("聊天失败：" + e.getMessage());
        }
    }
}
