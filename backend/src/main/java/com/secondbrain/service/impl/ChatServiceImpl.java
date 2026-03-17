package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.BatchChatImportRequest;
import com.secondbrain.dto.ChatCollectRequest;
import com.secondbrain.dto.ChatRecordDTO;
import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.entity.User;
import com.secondbrain.kafka.KafkaProducerService;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.RawChatRecordMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.ChatContextService;
import com.secondbrain.service.ChatService;
import com.secondbrain.service.EbbinghausService;
import com.secondbrain.service.ReviewCardService;
import com.secondbrain.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final RawChatRecordMapper rawChatRecordMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final AiService aiService;
    private final ChatContextService chatContextService;
    private final ReviewCardService reviewCardService;
    private final EbbinghausService ebbinghausService;
    private final UserService userService;

    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;

    public ChatServiceImpl(RawChatRecordMapper rawChatRecordMapper, 
                          KnowledgeNodeMapper knowledgeNodeMapper,
                          AiService aiService,
                          ChatContextService chatContextService,
                          ReviewCardService reviewCardService,
                          EbbinghausService ebbinghausService,
                          UserService userService) {
        this.rawChatRecordMapper = rawChatRecordMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.aiService = aiService;
        this.chatContextService = chatContextService;
        this.reviewCardService = reviewCardService;
        this.ebbinghausService = ebbinghausService;
        this.userService = userService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectChat(ChatCollectRequest request, Long userId) {
        User user = userService.getUserById(userId);
        String userApiKey = user != null ? user.getApiKey() : null;
        collectChat(request, userId, userApiKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectChat(ChatCollectRequest request, Long userId, String userApiKey) {
        RawChatRecord record = new RawChatRecord();
        record.setUserId(userId);
        record.setPlatform(request.getPlatform());
        record.setContent(request.getContent());
        record.setSourceUrl(request.getSourceUrl());
        rawChatRecordMapper.insert(record);

        chatContextService.cacheChatContext(record.getId().toString(), request.getContent());

        if (kafkaProducerService != null) {
            kafkaProducerService.sendChatCollect(record);
            log.info("对话采集成功，消息已发送到Kafka，recordId：{}", record.getId());
        } else {
            List<KnowledgeDTO> knowledgeList = extractKnowledge(request.getContent(), userApiKey);
            chatContextService.cacheExtractedKnowledge(record.getId().toString(), knowledgeList);

            for (KnowledgeDTO knowledge : knowledgeList) {
                KnowledgeNode node = new KnowledgeNode();
                node.setUserId(userId);
                node.setTitle(knowledge.getTitle());
                node.setSummary(knowledge.getSummary());
                node.setContentMd(knowledge.getContent() != null ? knowledge.getContent() : knowledge.getSummary());
                node.setImportance(3);
                node.setMasteryLevel(0);
                node.setReviewCount(0);
                node.setNextReviewTime(ebbinghausService.calculateNextReviewTime(LocalDateTime.now(), 0, true));
                knowledgeNodeMapper.insert(node);
                
                try {
                    reviewCardService.generateReviewCard(node.getId(), "choice");
                    log.info("自动生成复习卡片成功，nodeId：{}", node.getId());
                } catch (Exception e) {
                    log.error("自动生成复习卡片失败，nodeId：{}", node.getId(), e);
                }
            }
            log.info("对话采集成功（同步处理），生成{}个知识点", knowledgeList.size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchImportChats(BatchChatImportRequest request, Long userId) {
        User user = userService.getUserById(userId);
        String userApiKey = user != null ? user.getApiKey() : null;
        return batchImportChats(request, userId, userApiKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchImportChats(BatchChatImportRequest request, Long userId, String userApiKey) {
        if (request.getChats() == null || request.getChats().isEmpty()) {
            return 0;
        }

        int totalKnowledgeCount = 0;
        
        for (BatchChatImportRequest.ChatItem chatItem : request.getChats()) {
            RawChatRecord record = new RawChatRecord();
            record.setUserId(userId);
            record.setPlatform(chatItem.getPlatform());
            record.setContent(chatItem.getContent());
            record.setSourceUrl(chatItem.getSourceUrl());
            rawChatRecordMapper.insert(record);

            chatContextService.cacheChatContext(record.getId().toString(), chatItem.getContent());

            if (kafkaProducerService != null) {
                kafkaProducerService.sendChatCollect(record);
                log.info("批量导入：消息已发送到Kafka，recordId：{}", record.getId());
            } else {
                List<KnowledgeDTO> knowledgeList = extractKnowledge(chatItem.getContent(), userApiKey);
                chatContextService.cacheExtractedKnowledge(record.getId().toString(), knowledgeList);

                for (KnowledgeDTO knowledge : knowledgeList) {
                    KnowledgeNode node = new KnowledgeNode();
                    node.setUserId(userId);
                    node.setTitle(knowledge.getTitle());
                    node.setSummary(knowledge.getSummary());
                    node.setContentMd(knowledge.getContent() != null ? knowledge.getContent() : knowledge.getSummary());
                    node.setImportance(3);
                    node.setMasteryLevel(0);
                    node.setReviewCount(0);
                    node.setNextReviewTime(ebbinghausService.calculateNextReviewTime(LocalDateTime.now(), 0, true));
                    knowledgeNodeMapper.insert(node);
                    totalKnowledgeCount++;
                    
                    try {
                        reviewCardService.generateReviewCard(node.getId(), "choice");
                        log.info("自动生成复习卡片成功，nodeId：{}", node.getId());
                    } catch (Exception e) {
                        log.error("自动生成复习卡片失败，nodeId：{}", node.getId(), e);
                    }
                }
            }
        }

        if (kafkaProducerService != null) {
            log.info("批量导入成功，共导入{}条对话，已发送到Kafka异步处理", request.getChats().size());
            return request.getChats().size();
        } else {
            log.info("批量导入成功（同步处理），共导入{}条对话，生成{}个知识点", request.getChats().size(), totalKnowledgeCount);
            return totalKnowledgeCount;
        }
    }

    @Override
    public Page<ChatRecordDTO> getChatList(Long current, Long size, String platform, String keyword, Long userId) {
        Page<RawChatRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<RawChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RawChatRecord::getDeleted, 0);
        wrapper.eq(RawChatRecord::getUserId, userId);
        if (StringUtils.hasText(platform)) {
            wrapper.eq(RawChatRecord::getPlatform, platform);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(RawChatRecord::getContent, keyword);
        }
        wrapper.orderByDesc(RawChatRecord::getCreateTime);
        
        Page<RawChatRecord> recordPage = rawChatRecordMapper.selectPage(page, wrapper);
        
        Page<ChatRecordDTO> dtoPage = new Page<>(current, size, recordPage.getTotal());
        List<ChatRecordDTO> dtoList = recordPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }

    @Override
    public ChatRecordDTO getChatById(Long id, Long userId) {
        RawChatRecord record = rawChatRecordMapper.selectById(id);
        if (record == null || record.getDeleted() == 1) {
            return null;
        }
        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此对话");
        }
        return convertToDTO(record);
    }

    private ChatRecordDTO convertToDTO(RawChatRecord record) {
        ChatRecordDTO dto = new ChatRecordDTO();
        BeanUtils.copyProperties(record, dto);
        return dto;
    }

    @Override
    public List<KnowledgeDTO> extractKnowledge(String content) {
        return aiService.extractKnowledge(content);
    }

    @Override
    public List<KnowledgeDTO> extractKnowledge(String content, String userApiKey) {
        return aiService.extractKnowledge(content, userApiKey);
    }

    @Override
    public long countByUserId(Long userId) {
        return rawChatRecordMapper.selectCount(
            new LambdaQueryWrapper<RawChatRecord>()
                .eq(RawChatRecord::getUserId, userId)
        );
    }

    @Override
    public long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return rawChatRecordMapper.selectCount(
            new LambdaQueryWrapper<RawChatRecord>()
                .eq(RawChatRecord::getUserId, userId)
                .ge(RawChatRecord::getCreateTime, startTime)
                .lt(RawChatRecord::getCreateTime, endTime)
        );
    }
}
