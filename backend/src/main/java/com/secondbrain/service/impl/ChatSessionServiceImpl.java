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

    /**
     * AI聊天对话.
     *
     * @param request     聊天请求（内容、模型、会话ID）
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return AI回复内容
     */
    @Override
    public ChatResponseDTO chat(ChatRequestDTO request, Long userId, Long workspaceId) {
        long startTime = System.currentTimeMillis();

        String apiKey = getApiKey(userId);
        String model = request.getModel() != null ? request.getModel() : "gpt-4o-mini";

        log.info("开始聊天，userId：{}，workspaceId：{}，model：{}", userId, workspaceId, model);

        ChatSession session = getOrCreateSession(request.getSessionId(), userId, workspaceId);
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

        saveMessage(session.getId(), workspaceId, "user", request.getContent());
        saveMessage(session.getId(), workspaceId, "assistant", aiResponse);

        long endTime = System.currentTimeMillis();
        log.info("聊天完成，耗时：{}ms", endTime - startTime);

        ChatResponseDTO result = new ChatResponseDTO();
        result.setContent(aiResponse);
        result.setSessionId(session.getId());
        result.setModel(model);
        return result;
    }

    /**
     * 带知识库检索的AI聊天.
     *
     * @param request     聊天请求
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return AI回复内容
     */
    @Override
    public ChatResponseDTO chatWithKnowledge(ChatRequestDTO request, Long userId, Long workspaceId) {
        long startTime = System.currentTimeMillis();

        String apiKey = getApiKey(userId);
        String model = request.getModel() != null ? request.getModel() : "gpt-4o-mini";

        log.info("开始带知识检索的聊天，userId：{}，workspaceId：{}，model：{}", userId, workspaceId, model);

        ChatSession session = getOrCreateSession(request.getSessionId(), userId, workspaceId);

        LambdaQueryWrapper<KnowledgeNode> nodeWrapper = new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getDeleted, 0);
        if (workspaceId != null) {
            nodeWrapper.eq(KnowledgeNode::getWorkspaceId, workspaceId);
        } else {
            nodeWrapper.eq(KnowledgeNode::getUserId, userId);
        }
        List<KnowledgeNode> knowledgeNodes = knowledgeNodeMapper.selectList(nodeWrapper);

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

        saveMessage(session.getId(), workspaceId, "user", request.getContent());
        saveMessage(session.getId(), workspaceId, "assistant", aiResponse);

        long endTime = System.currentTimeMillis();
        log.info("带知识检索的聊天完成，耗时：{}ms", endTime - startTime);

        ChatResponseDTO result = new ChatResponseDTO();
        result.setContent(aiResponse);
        result.setSessionId(session.getId());
        result.setModel(model);
        return result;
    }

    /**
     * 分页查询会话列表.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @param current     当前页
     * @param size        每页大小
     * @return 会话分页
     */
    @Override
    public Page<ChatSession> getSessionList(Long userId, Long workspaceId, Integer current, Integer size) {
        Page<ChatSession> page = new Page<>(current, size);
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        if (workspaceId != null) {
            wrapper.eq(ChatSession::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(ChatSession::getUserId, userId);
        }
        wrapper.orderByDesc(ChatSession::getUpdateTime);
        return chatSessionMapper.selectPage(page, wrapper);
    }

    /**
     * 分页查询会话消息列表.
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param current   当前页
     * @param size      每页大小
     * @return 消息分页
     */
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

    /**
     * 创建会话.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @param title       会话标题
     * @return 会话实体
     */
    @Override
    public ChatSession createSession(Long userId, Long workspaceId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setWorkspaceId(workspaceId);
        session.setTitle(title);
        chatSessionMapper.insert(session);
        return session;
    }

    /**
     * 删除会话.
     *
     * @param sessionId   会话ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return void
     */
    @Override
    public void deleteSession(Long sessionId, Long userId, Long workspaceId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId);
        if (workspaceId != null) {
            wrapper.eq(ChatSession::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(ChatSession::getUserId, userId);
        }
        chatSessionMapper.delete(wrapper);
    }

    private ChatSession getOrCreateSession(Long sessionId, Long userId, Long workspaceId) {
        if (sessionId != null) {
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session != null && hasAccess(session, userId, workspaceId)) {
                return session;
            }
        }
        return createSession(userId, workspaceId, "新对话");
    }

    private List<ChatMessage> getHistoryMessages(Long sessionId) {
        return chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT 10")
        );
    }

    private void saveMessage(Long sessionId, Long workspaceId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setWorkspaceId(workspaceId);
        message.setRole(role);
        message.setContent(content);
        chatMessageMapper.insert(message);
    }

    /**
     * 检查会话访问权限.
     * workspaceId 不为 null 时按工作区校验，否则按 userId 校验.
     */
    private boolean hasAccess(ChatSession session, Long userId, Long workspaceId) {
        if (workspaceId != null) {
            return workspaceId.equals(session.getWorkspaceId());
        }
        return session.getUserId().equals(userId);
    }
}
