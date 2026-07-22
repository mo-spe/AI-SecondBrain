package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.ChatRequestDTO;
import com.secondbrain.dto.ChatResponseDTO;
import com.secondbrain.entity.ChatMessage;
import com.secondbrain.entity.ChatSession;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.User;
import com.secondbrain.mapper.ChatMessageMapper;
import com.secondbrain.mapper.ChatSessionMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.service.ChatSessionService;
import com.secondbrain.service.RagService;
import com.secondbrain.service.UserService;
import com.secondbrain.util.JwtUtil;
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
import java.util.stream.Collectors;

/**
 * 聊天会话服务实现类.
 * <p>提供AI聊天对话、会话管理、消息记录等功能</p>
 */
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionServiceImpl.class);

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final UserService userService;
    private final RagService ragService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Value("${ai.openai.api-key:}")
    private String systemApiKey;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    public ChatSessionServiceImpl(ChatSessionMapper chatSessionMapper, ChatMessageMapper chatMessageMapper,
                                  KnowledgeNodeMapper knowledgeNodeMapper, UserService userService, RagService ragService,
                                  JwtUtil jwtUtil, UserMapper userMapper) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.userService = userService;
        this.ragService = ragService;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    private OpenAiClient createClient(String apiKey) {
        return OpenAiClient.builder()
                .apiKey(java.util.Collections.singletonList(apiKey))
                .apiHost(baseUrl)
                .build();
    }

    private String getApiKey(Long userId) {
        try {
            if (userId != null) {
                User user = userService.getUserById(userId);
                if (user != null && user.getApiKey() != null && !user.getApiKey().isEmpty()) {
                    log.info("使用用户自定义 API Key，userId：{}", userId);
                    return user.getApiKey();
                }
            }
        } catch (Exception e) {
            log.warn("获取用户 API Key 失败，使用系统 API Key，userId：{}", userId);
        }

        if (systemApiKey != null && !systemApiKey.isEmpty()) {
            log.info("使用系统 API Key");
            return systemApiKey;
        }

        throw new IllegalStateException("API Key 未配置");
    }

    @Override
    public ChatResponseDTO chat(ChatRequestDTO request, Long userId) {
        long startTime = System.currentTimeMillis();

        String apiKey = getApiKey(userId);
        String model = request.getModel() != null ? request.getModel() : "gpt-4o-mini";

        log.info("开始聊天，userId：{}，model：{}", userId, model);

        ChatSession session = getOrCreateSession(request.getSessionId(), userId);
        List<ChatMessage> history = getHistoryMessages(session.getId());

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder().role(Message.Role.SYSTEM).content("你是一个智能助手，可以帮助用户回答问题。").build());

        for (ChatMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(Message.builder().role(Message.Role.USER).content(msg.getContent()).build());
            } else {
                messages.add(Message.builder().role(Message.Role.ASSISTANT).content(msg.getContent()).build());
            }
        }

        messages.add(Message.builder().role(Message.Role.USER).content(request.getContent()).build());

        OpenAiClient client = createClient(apiKey);
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(model)
                .messages(messages)
                .build();

        ChatCompletionResponse response = client.chatCompletion(chatCompletion);
        String aiResponse = response.getChoices().get(0).getMessage().getContent();

        saveMessage(session.getId(), "user", request.getContent());
        saveMessage(session.getId(), "assistant", aiResponse);

        long endTime = System.currentTimeMillis();
        log.info("聊天完成，耗时：{}ms", endTime - startTime);

        ChatResponseDTO result = new ChatResponseDTO();
        result.setContent(aiResponse);
        result.setSessionId(session.getId());
        result.setModel(model);
        return result;
    }

    @Override
    public ChatResponseDTO chatWithKnowledge(ChatRequestDTO request, Long userId) {
        long startTime = System.currentTimeMillis();

        String apiKey = getApiKey(userId);
        String model = request.getModel() != null ? request.getModel() : "gpt-4o-mini";

        log.info("开始带知识检索的聊天，userId：{}，model：{}", userId, model);

        ChatSession session = getOrCreateSession(request.getSessionId(), userId);

        List<KnowledgeNode> knowledgeNodes = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getUserId, userId)
                .eq(KnowledgeNode::getDeleted, 0)
        );

        StringBuilder knowledgeContext = new StringBuilder();
        for (KnowledgeNode node : knowledgeNodes) {
            knowledgeContext.append(node.getTitle()).append("\n");
            if (node.getSummary() != null) {
                knowledgeContext.append(node.getSummary()).append("\n");
            }
            if (node.getContentMd() != null) {
                knowledgeContext.append(node.getContentMd()).append("\n");
            }
            knowledgeContext.append("---\n");
        }

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role(Message.Role.SYSTEM)
                .content("你是一个智能助手，基于以下知识库回答用户问题。\n\n知识库：\n" + knowledgeContext.toString())
                .build());
        messages.add(Message.builder().role(Message.Role.USER).content(request.getContent()).build());

        OpenAiClient client = createClient(apiKey);
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(model)
                .messages(messages)
                .build();

        ChatCompletionResponse response = client.chatCompletion(chatCompletion);
        String aiResponse = response.getChoices().get(0).getMessage().getContent();

        saveMessage(session.getId(), "user", request.getContent());
        saveMessage(session.getId(), "assistant", aiResponse);

        long endTime = System.currentTimeMillis();
        log.info("带知识检索的聊天完成，耗时：{}ms", endTime - startTime);

        ChatResponseDTO result = new ChatResponseDTO();
        result.setContent(aiResponse);
        result.setSessionId(session.getId());
        result.setModel(model);
        return result;
    }

    @Override
    public Page<ChatSession> getSessionList(Long userId, Integer current, Integer size) {
        Page<ChatSession> page = new Page<>(current, size);
        return chatSessionMapper.selectPage(
            page,
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime)
        );
    }

    @Override
    public Page<ChatMessage> getMessageList(Long sessionId, Long userId, Integer current, Integer size) {
        Page<ChatMessage> page = new Page<>(current, size);
        return chatMessageMapper.selectPage(
            page,
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime)
        );
    }

    @Override
    public ChatSession createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        chatSessionMapper.insert(session);
        return session;
    }

    @Override
    public void deleteSession(Long sessionId, Long userId) {
        chatSessionMapper.delete(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
        );
    }

    private ChatSession getOrCreateSession(Long sessionId, Long userId) {
        if (sessionId != null) {
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session != null && session.getUserId().equals(userId)) {
                return session;
            }
        }
        return createSession(userId, "新对话");
    }

    private List<ChatMessage> getHistoryMessages(Long sessionId) {
        return chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT 10")
        );
    }

    private void saveMessage(Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        chatMessageMapper.insert(message);
    }
}
