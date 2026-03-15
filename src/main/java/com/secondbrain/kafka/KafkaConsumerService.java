package com.secondbrain.kafka;

import com.alibaba.fastjson2.JSON;
import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.ChatContextService;
import com.secondbrain.service.EbbinghausService;
import com.secondbrain.service.ReviewCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final AiService aiService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final ChatContextService chatContextService;
    private final ReviewCardService reviewCardService;
    private final EbbinghausService ebbinghausService;

    @Autowired
    public KafkaConsumerService(
            AiService aiService,
            KnowledgeNodeMapper knowledgeNodeMapper,
            ChatContextService chatContextService,
            ReviewCardService reviewCardService,
            EbbinghausService ebbinghausService) {
        this.aiService = aiService;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.chatContextService = chatContextService;
        this.reviewCardService = reviewCardService;
        this.ebbinghausService = ebbinghausService;
    }

    public void handleChatCollect(String message) {
        log.info("收到Kafka消息，开始处理对话采集...");
        
        try {
            RawChatRecord chatRecord = JSON.parseObject(message, RawChatRecord.class);
            log.info("解析对话记录成功，recordId：{}，userId：{}，platform：{}", 
                    chatRecord.getId(), chatRecord.getUserId(), chatRecord.getPlatform());
            
            List<KnowledgeDTO> knowledgeList = aiService.extractKnowledge(chatRecord.getContent());
            log.info("AI提取知识点成功，共提取{}个知识点", knowledgeList.size());
            
            chatContextService.cacheExtractedKnowledge(chatRecord.getId().toString(), knowledgeList);
            
            int savedCount = 0;
            for (KnowledgeDTO knowledge : knowledgeList) {
                try {
                    KnowledgeNode node = new KnowledgeNode();
                    node.setUserId(chatRecord.getUserId());
                    node.setTitle(knowledge.getTitle());
                    node.setSummary(knowledge.getSummary());
                    node.setContentMd(knowledge.getContent() != null ? knowledge.getContent() : knowledge.getSummary());
                    node.setImportance(3);
                    node.setMasteryLevel(0);
                    node.setReviewCount(0);
                    node.setNextReviewTime(ebbinghausService.calculateNextReviewTime(LocalDateTime.now(), 0, true));
                    
                    knowledgeNodeMapper.insert(node);
                    savedCount++;
                    log.info("知识点保存成功，nodeId：{}，title：{}", node.getId(), node.getTitle());
                    
                    try {
                        reviewCardService.generateReviewCard(node.getId(), "choice");
                        log.info("自动生成复习卡片成功，nodeId：{}", node.getId());
                    } catch (Exception e) {
                        log.error("自动生成复习卡片失败，nodeId：{}", node.getId(), e);
                    }
                } catch (Exception e) {
                    log.error("保存知识点失败，title：{}", knowledge.getTitle(), e);
                }
            }
            
            log.info("对话采集处理完成，共保存{}个知识点", savedCount);
            
        } catch (Exception e) {
            log.error("处理对话采集消息失败", e);
            throw new RuntimeException("处理对话采集消息失败", e);
        }
    }
}