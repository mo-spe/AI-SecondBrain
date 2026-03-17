package com.secondbrain.kafka;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.RawChatRecordMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.ChatContextService;
import com.secondbrain.service.EbbinghausService;
import com.secondbrain.service.KnowledgeVectorService;
import com.secondbrain.service.ReviewCardService;
import com.secondbrain.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final AiService aiService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final ChatContextService chatContextService;
    private final ReviewCardService reviewCardService;
    private final EbbinghausService ebbinghausService;
    private final UserService userService;
    private final RawChatRecordMapper rawChatRecordMapper;
    private final KnowledgeVectorService knowledgeVectorService;

    @Autowired
    public KafkaConsumerService(
            AiService aiService,
            KnowledgeNodeMapper knowledgeNodeMapper,
            ChatContextService chatContextService,
            ReviewCardService reviewCardService,
            EbbinghausService ebbinghausService,
            UserService userService,
            RawChatRecordMapper rawChatRecordMapper,
            KnowledgeVectorService knowledgeVectorService) {
        this.aiService = aiService;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.chatContextService = chatContextService;
        this.reviewCardService = reviewCardService;
        this.ebbinghausService = ebbinghausService;
        this.userService = userService;
        this.rawChatRecordMapper = rawChatRecordMapper;
        this.knowledgeVectorService = knowledgeVectorService;
    }

    @KafkaListener(topics = "chat-collect-topic", groupId = "ai-second-brain-group")
    public void handleChatCollect(String message) {
        log.info("收到 Kafka 消息，开始处理对话采集...");
        
        try {
            RawChatRecord chatRecord = JSON.parseObject(message, RawChatRecord.class);
            log.info("解析对话记录成功，recordId：{}，userId：{}，platform：{}", 
                    chatRecord.getId(), chatRecord.getUserId(), chatRecord.getPlatform());
            
            // 检查是否已处理过，避免重复消费
            if (chatRecord.getProcessed() != null && chatRecord.getProcessed() == 1) {
                log.info("消息已处理过，跳过，recordId：{}，userId：{}", chatRecord.getId(), chatRecord.getUserId());
                return;
            }
            
            // 验证消息时间戳，只处理最近 10 分钟内的消息，避免消费历史旧消息
            if (chatRecord.getCreateTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                long minutesDiff = java.time.Duration.between(chatRecord.getCreateTime(), now).toMinutes();
                if (minutesDiff > 10) {
                    log.warn("消息时间超过 10 分钟，跳过处理，recordId：{}，userId：{}，createTime：{}，当前时间：{}，相差分钟：{}", 
                            chatRecord.getId(), chatRecord.getUserId(), chatRecord.getCreateTime(), now, minutesDiff);
                    // 标记为已处理，避免下次继续消费
                    markAsProcessed(chatRecord.getId());
                    return;
                }
                log.info("消息时间验证通过，相差{}分钟", minutesDiff);
            } else {
                log.warn("消息 createTime 为 null，跳过处理，recordId：{}", chatRecord.getId());
                return;
            }
            
            String userApiKey = userService.getUserById(chatRecord.getUserId()).getApiKey();
            List<KnowledgeDTO> knowledgeList = aiService.extractKnowledge(chatRecord.getContent(), userApiKey);
            
            if (knowledgeList == null || knowledgeList.isEmpty()) {
                log.warn("AI 提取知识点失败，可能是 API Key 未配置，recordId：{}，userId：{}", chatRecord.getId(), chatRecord.getUserId());
                // 标记为已处理
                markAsProcessed(chatRecord.getId());
                return;
            }
            
            log.info("AI 提取知识点成功，共提取{}个知识点", knowledgeList.size());
            
            // 使用 userId 和时间戳生成临时 ID，避免空指针异常
            String tempRecordId = chatRecord.getUserId() + "_" + System.currentTimeMillis();
            chatContextService.cacheExtractedKnowledge(tempRecordId, knowledgeList);
            
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
                    
                    // 异步触发向量生成
                    try {
                        KnowledgeNode savedNode = knowledgeNodeMapper.selectById(node.getId());
                        knowledgeVectorService.generateAndSaveVector(savedNode);
                        log.info("异步触发向量生成成功，nodeId：{}", node.getId());
                    } catch (Exception e) {
                        log.error("异步触发向量生成失败，nodeId：{}", node.getId(), e);
                    }
                    
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
            
            // 标记为已处理
            markAsProcessed(chatRecord.getId());
            
            log.info("对话采集处理完成，共保存{}个知识点", savedCount);
            
        } catch (Exception e) {
            log.error("处理对话采集消息失败", e);
            throw new RuntimeException("处理对话采集消息失败", e);
        }
    }
    
    /**
     * 标记消息为已处理
     */
    private void markAsProcessed(Long recordId) {
        try {
            LambdaUpdateWrapper<RawChatRecord> wrapper = new LambdaUpdateWrapper<>();
            wrapper.set(RawChatRecord::getProcessed, 1)
                   .eq(RawChatRecord::getId, recordId)
                   .eq(RawChatRecord::getProcessed, 0);
            rawChatRecordMapper.update(null, wrapper);
            log.info("标记消息为已处理，recordId：{}", recordId);
        } catch (Exception e) {
            log.error("标记消息处理状态失败，recordId：{}", recordId, e);
        }
    }
}