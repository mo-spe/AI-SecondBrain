package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.secondbrain.dto.KnowledgeReference;
import com.secondbrain.dto.RagRequest;
import com.secondbrain.dto.StreamEvent;
import com.secondbrain.entity.ChatMessage;
import com.secondbrain.entity.ChatSession;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.ChatMessageMapper;
import com.secondbrain.mapper.ChatSessionMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.StreamingAiService;
import com.secondbrain.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * RAG流式问答编排服务.
 *
 * <p>编排检索→流式生成的完整流程，通过 {@link Consumer} 回调逐事件推送</p>
 */
@Service
public class RagStreamingServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(RagStreamingServiceImpl.class);

    private final VectorSearchService vectorSearchService;
    private final StreamingAiService streamingAiService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    private static final String SYSTEM_PROMPT = """
            你是一个专业的知识问答助手。请基于以下知识内容回答用户的问题。

            【回答要求】
            1. 只基于提供的知识内容回答，不要编造信息
            2. 如果知识内容不足以回答问题，请明确说明
            3. 回答要准确、清晰、有条理
            4. 在引用知识时标注来源：[来源: 知识标题]
            5. 回答末尾列出实际引用的知识来源
            """;

    public RagStreamingServiceImpl(VectorSearchService vectorSearchService,
                                   StreamingAiService streamingAiService,
                                   KnowledgeNodeMapper knowledgeNodeMapper,
                                   ChatSessionMapper chatSessionMapper,
                                   ChatMessageMapper chatMessageMapper) {
        this.vectorSearchService = vectorSearchService;
        this.streamingAiService = streamingAiService;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    /**
     * 流式回答用户问题.
     *
     * @param request       RAG请求
     * @param userId        用户ID
     * @param eventConsumer 事件消费者（逐事件回调）
     */
    public void streamAnswer(RagRequest request, Long userId, Consumer<StreamEvent> eventConsumer) {
        String question = request.getQuestion();
        int topK = request.getTopK() != null ? request.getTopK() : 3;

        log.info("rag_stream_start question='{}' topK={} userId={}", question, topK, userId);

        try {
            // Step 1: 向量检索
            long retrievalStart = System.currentTimeMillis();
            List<KnowledgeReference> references = retrieveKnowledge(question, userId, topK);
            long retrievalTime = System.currentTimeMillis() - retrievalStart;

            log.info("rag_stream_retrieval count={} time={}ms", references.size(), retrievalTime);

            // Step 2: 发送引用事件
            if (request.getIncludeReferences() != null && request.getIncludeReferences()) {
                eventConsumer.accept(StreamEvent.references(JSON.toJSONString(references)));
            }

            // Step 3: 发送指标事件
            Map<String, Long> metrics = new HashMap<>();
            metrics.put("retrievalTime", retrievalTime);
            eventConsumer.accept(StreamEvent.metrics(JSON.toJSONString(metrics)));

            // Step 4: 构建 RAG 提示词
            String context = buildContextFromReferences(references);
            String systemPrompt = SYSTEM_PROMPT + "\n\n【相关知识】\n" + context;

            // Step 5: 构建消息列表（含对话历史）
            List<Map<String, String>> messages = buildMessages(question, request.getSessionId(), userId);

            // Step 6: 流式生成
            long generationStart = System.currentTimeMillis();
            StringBuilder fullAnswer = new StringBuilder();

            streamingAiService.streamChat(userId, "chat", systemPrompt, messages, token -> {
                fullAnswer.append(token);
                eventConsumer.accept(StreamEvent.token(token));
            });

            long generationTime = System.currentTimeMillis() - generationStart;
            log.info("rag_stream_done answerLength={} generationTime={}ms", fullAnswer.length(), generationTime);

            // Step 7: 保存消息到会话
            if (request.getSessionId() != null) {
                saveMessages(request.getSessionId(), userId, question, fullAnswer.toString());
            }

            // Step 8: 完成
            eventConsumer.accept(StreamEvent.done());

        } catch (Exception e) {
            log.error("rag_stream_failed", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
            if (errorMsg.contains("API Key") || errorMsg.contains("请先在设置页配置")) {
                eventConsumer.accept(StreamEvent.error("AI服务不可用。" + errorMsg));
            } else {
                eventConsumer.accept(StreamEvent.error("流式回答失败：" + errorMsg));
            }
            eventConsumer.accept(StreamEvent.done());
        }
    }

    private List<KnowledgeReference> retrieveKnowledge(String question, Long userId, int topK) {
        List<KnowledgeReference> references = vectorSearchService.searchSimilar(question, userId, topK);

        for (KnowledgeReference ref : references) {
            KnowledgeNode node = knowledgeNodeMapper.selectById(ref.getKnowledgeId());
            if (node != null) {
                ref.setMatchedContent(extractRelevantContent(node, question));
            }
        }

        return references;
    }

    private String extractRelevantContent(KnowledgeNode node, String question) {
        String content = node.getContentMd();
        if (content == null || content.isEmpty()) {
            return node.getSummary() != null ? node.getSummary() : "";
        }

        String[] questionWords = question.toLowerCase().split("\\s+");
        String contentLower = content.toLowerCase();

        int bestMatchStart = -1;
        int maxMatches = 0;

        for (int i = 0; i < content.length() - 200; i++) {
            String window = contentLower.substring(i, i + 200);
            int matches = 0;
            for (String word : questionWords) {
                if (word.length() >= 2 && window.contains(word)) {
                    matches++;
                }
            }
            if (matches > maxMatches) {
                maxMatches = matches;
                bestMatchStart = i;
            }
        }

        if (bestMatchStart >= 0) {
            int end = Math.min(bestMatchStart + 500, content.length());
            String excerpt = content.substring(bestMatchStart, end);
            if (bestMatchStart > 0) {
                excerpt = "..." + excerpt;
            }
            if (end < content.length()) {
                excerpt = excerpt + "...";
            }
            return excerpt;
        }

        if (content.length() > 500) {
            return content.substring(0, 500) + "...";
        }

        return content;
    }

    private String buildContextFromReferences(List<KnowledgeReference> references) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < references.size(); i++) {
            KnowledgeReference ref = references.get(i);
            context.append("\n【知识").append(i + 1).append("】\n");
            context.append("标题：").append(ref.getTitle()).append("\n");
            context.append("内容：").append(ref.getMatchedContent()).append("\n");
        }
        return context.toString();
    }

    /**
     * 构建消息列表，包含对话历史（如有sessionId）.
     */
    private List<Map<String, String>> buildMessages(String question, Long sessionId, Long userId) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 加载历史消息（最近10轮）
        if (sessionId != null) {
            var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, sessionId)
                    .orderByDesc(ChatMessage::getCreateTime)
                    .last("LIMIT 20");
            List<ChatMessage> history = chatMessageMapper.selectList(wrapper);
            // 反转回时间正序
            Collections.reverse(history);
            for (ChatMessage msg : history) {
                Map<String, String> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                messages.add(m);
            }
        }

        // 当前问题
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", question);
        messages.add(userMsg);

        return messages;
    }

    private void saveMessages(Long sessionId, Long userId, String question, String answer) {
        try {
            ChatMessage userMsg = new ChatMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setRole("user");
            userMsg.setContent(question);
            chatMessageMapper.insert(userMsg);

            ChatMessage aiMsg = new ChatMessage();
            aiMsg.setSessionId(sessionId);
            aiMsg.setRole("assistant");
            aiMsg.setContent(answer);
            chatMessageMapper.insert(aiMsg);
        } catch (Exception e) {
            log.error("save_rag_messages_failed sessionId={}", sessionId, e);
        }
    }
}
