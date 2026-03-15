package com.secondbrain.service.impl;

import com.secondbrain.dto.*;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.RagService;
import com.secondbrain.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private AiService aiService;

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Override
    public RagResponse answer(RagRequest request, Long userId) {
        return answer(request, userId, null);
    }

    @Override
    public RagResponse answer(RagRequest request, Long userId, String userApiKey) {
        long startTime = System.currentTimeMillis();
        
        String question = request.getQuestion();
        int topK = request.getTopK() != null ? request.getTopK() : 3;
        
        log.info("开始RAG问答，问题：'{}'，topK：{}，userId：{}，使用用户API Key：{}", 
                 question, topK, userId, userApiKey != null && !userApiKey.isEmpty());

        RagResponse response = new RagResponse();

        try {
            long retrievalStart = System.currentTimeMillis();
            
            List<KnowledgeReference> references = retrieveKnowledge(question, userId, topK, userApiKey);
            
            long retrievalEnd = System.currentTimeMillis();
            response.setRetrievalTime(retrievalEnd - retrievalStart);
            
            log.info("检索到{}条相关知识，耗时{}ms", references.size(), response.getRetrievalTime());

            if (references.isEmpty()) {
                response.setAnswer("抱歉，我在您的知识库中没有找到相关的知识。您可以先添加一些相关的知识点，然后再提问。");
                response.setReferences(new ArrayList<>());
                return response;
            }

            long generationStart = System.currentTimeMillis();
            
            String answer = generateAnswer(question, references, userApiKey);
            
            long generationEnd = System.currentTimeMillis();
            response.setGenerationTime(generationEnd - generationStart);
            
            response.setAnswer(answer);
            response.setReferences(request.getIncludeReferences() ? references : new ArrayList<>());
            
            log.info("RAG问答完成，答案长度：{}，生成耗时：{}ms", answer.length(), response.getGenerationTime());
            
        } catch (Exception e) {
            log.error("RAG问答失败", e);
            response.setAnswer("抱歉，回答问题时出现错误：" + e.getMessage());
        }

        return response;
    }

    private List<KnowledgeReference> retrieveKnowledge(String question, Long userId, int topK, String userApiKey) {
        log.info("开始检索知识，问题：'{}'，topK：{}", question, topK);

        List<KnowledgeReference> references = vectorSearchService.searchSimilar(question, userId, topK, userApiKey);
        
        log.info("从向量检索到{}个文档", references.size());

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

    private String generateAnswer(String question, List<KnowledgeReference> references, String userApiKey) {
        StringBuilder context = new StringBuilder();
        
        for (int i = 0; i < references.size(); i++) {
            KnowledgeReference ref = references.get(i);
            context.append("\n【知识").append(i + 1).append("】\n");
            context.append("标题：").append(ref.getTitle()).append("\n");
            context.append("内容：").append(ref.getMatchedContent()).append("\n");
        }
        
        String prompt = buildPrompt(question, context.toString());
        
        return aiService.generateAnswer(prompt, userApiKey);
    }

    private String buildPrompt(String question, String context) {
        return String.format(
            "你是一个专业的知识问答助手。请基于以下知识内容回答用户的问题。\n\n" +
            "【用户问题】\n%s\n\n" +
            "【相关知识】\n%s\n\n" +
            "【回答要求】\n" +
            "1. 只基于提供的知识内容回答，不要编造信息\n" +
            "2. 如果知识内容不足以回答问题，请明确说明\n" +
            "3. 回答要准确、清晰、有条理\n" +
            "4. 可以引用知识标题作为参考\n" +
            "5. 回答要简洁明了，不要过于冗长\n\n" +
            "请回答：",
            question, context
        );
    }
}