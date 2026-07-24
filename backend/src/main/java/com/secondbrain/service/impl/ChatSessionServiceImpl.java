package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.ChatRequestDTO;
import com.secondbrain.dto.ChatResponseDTO;
import com.secondbrain.entity.ChatMessage;
import com.secondbrain.entity.ChatSession;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.User;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.mapper.ChatMessageMapper;
import com.secondbrain.mapper.ChatSessionMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.service.AiService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天会话服务实现类.
 * <p>提供AI聊天对话、会话管理、消息记录等功能。支持新旧两套AI配置方案切换</p>
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
    private final AiService aiService;

    @Value("${ai.openai.api-key:}")
    private String systemApiKey;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    /**
     * 是否使用数据库驱动的用户AI配置.
     * <p>默认 true，紧急回退时设为 false 恢复旧行为</p>
     */
    @Value("${ai.use-new-config:true}")
    private boolean useNewConfig;

    public ChatSessionServiceImpl(ChatSessionMapper chatSessionMapper, ChatMessageMapper chatMessageMapper,
                                  KnowledgeNodeMapper knowledgeNodeMapper, UserService userService, RagService ragService,
                                  JwtUtil jwtUtil, UserMapper userMapper, AiService aiService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.userService = userService;
        this.ragService = ragService;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.aiService = aiService;
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

        ChatSession session = getOrCreateSession(request.getSessionId(), userId, workspaceId);
        List<ChatMessage> history = getHistoryMessages(session.getId());

        String aiResponse;
        String model;

        if (useNewConfig && userId != null) {
            // 使用数据库驱动的用户AI配置
            List<Map<String, String>> messages = buildMessages(history, request.getContent(), "你是一个智能助手，可以帮助用户回答问题。");
            aiResponse = aiService.chat(userId, AiScenario.CHAT.getCode(), messages);
            model = "user-configured";
        } else {
            // 旧行为：使用 application.yml 系统配置
            String apiKey = getApiKey(userId);
            model = request.getModel() != null ? request.getModel() : "gpt-4o-mini";
            log.info("开始聊天，userId：{}，workspaceId：{}，model：{}", userId, workspaceId, model);

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
            aiResponse = response.getChoices().get(0).getMessage().getContent();
        }

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

        String aiResponse;
        String model;
        String systemPrompt = "你是一个智能助手，基于以下知识库回答用户问题。\n\n知识库：\n" + knowledgeContext.toString();

        if (useNewConfig && userId != null) {
            // 使用数据库驱动的用户AI配置
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", request.getContent());
            messages.add(userMsg);
            aiResponse = aiService.chat(userId, AiScenario.CHAT.getCode(), messages);
            model = "user-configured";
        } else {
            // 旧行为：使用 application.yml 系统配置
            String apiKey = getApiKey(userId);
            model = request.getModel() != null ? request.getModel() : "gpt-4o-mini";
            log.info("开始带知识检索的聊天，userId：{}，workspaceId：{}，model：{}", userId, workspaceId, model);

            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder().role(Message.Role.SYSTEM).content(systemPrompt).build());
            messages.add(Message.builder().role(Message.Role.USER).content(request.getContent()).build());

            OpenAiClient client = createClient(apiKey);
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model(model)
                    .messages(messages)
                    .build();
            ChatCompletionResponse response = client.chatCompletion(chatCompletion);
            aiResponse = response.getChoices().get(0).getMessage().getContent();
        }

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
     * 将历史消息和新消息构建为 Map 列表供新 AiService 使用.
     */
    private List<Map<String, String>> buildMessages(List<ChatMessage> history, String newContent, String systemPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        for (ChatMessage msg : history) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            messages.add(m);
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", newContent);
        messages.add(userMsg);
        return messages;
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

    public ChatSession getOrCreateSession(String sessionId, Long userId, Long workspaceId) {
        if (sessionId != null) {
            ChatSession session = chatSessionMapper.selectById(Long.parseLong(sessionId));
            if (session != null) {
                return session;
            }
        }
        ChatSession session = new ChatSession();
        session.setTitle("新对话");
        session.setUserId(userId);
        session.setWorkspaceId(workspaceId);
        chatSessionMapper.insert(session);
        return session;
    }

    private ChatSession getOrCreateSession(Long sessionId, Long userId, Long workspaceId) {
        if (sessionId != null) {
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session != null) {
                return session;
            }
        }
        ChatSession session = new ChatSession();
        session.setTitle("新对话");
        session.setUserId(userId);
        session.setWorkspaceId(workspaceId);
        chatSessionMapper.insert(session);
        return session;
    }

    public List<ChatMessage> getHistoryMessages(Long sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectList(wrapper);
    }

    public void saveMessage(Long sessionId, Long workspaceId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setWorkspaceId(workspaceId);
        message.setRole(role);
        message.setContent(content);
        chatMessageMapper.insert(message);
    }

    @Override
    public Page<ChatMessage> getMessageList(Long sessionId, Long userId, Integer current, Integer size) {
        Page<ChatMessage> page = new Page<>(current, size);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectPage(page, wrapper);
    }

    @Override
    public ChatSession createSession(Long userId, Long workspaceId, String title) {
        ChatSession session = new ChatSession();
        session.setTitle(title != null ? title : "新对话");
        session.setUserId(userId);
        session.setWorkspaceId(workspaceId);
        chatSessionMapper.insert(session);
        return session;
    }

    @Override
    public void deleteSession(Long sessionId, Long userId, Long workspaceId) {
        chatSessionMapper.deleteById(sessionId);
        LambdaQueryWrapper<ChatMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessage::getSessionId, sessionId);
        chatMessageMapper.delete(msgWrapper);
    }
}
