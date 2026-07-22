package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.BatchChatImportRequest;
import com.secondbrain.dto.ChatCollectRequest;
import com.secondbrain.dto.ChatRecordDTO;
import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.kafka.KafkaProducerService;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.RawChatRecordMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话服务实现类.
 * <p>提供对话采集、导入、查询及知识提取功能</p>
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final RawChatRecordMapper rawChatRecordMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final AiService aiService;
    private final KafkaProducerService kafkaProducerService;

    public ChatServiceImpl(RawChatRecordMapper rawChatRecordMapper,
                           KnowledgeNodeMapper knowledgeNodeMapper,
                           AiService aiService,
                           KafkaProducerService kafkaProducerService) {
        this.rawChatRecordMapper = rawChatRecordMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.aiService = aiService;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    public void collectChat(ChatCollectRequest request, Long userId) {
        log.info("采集对话，userId：{}，platform：{}", userId, request.getPlatform());
        RawChatRecord record = new RawChatRecord();
        record.setUserId(userId);
        record.setPlatform(request.getPlatform());
        record.setContent(request.getContent());
        record.setSourceUrl(request.getSourceUrl());
        record.setCreateTime(LocalDateTime.now());
        rawChatRecordMapper.insert(record);
        kafkaProducerService.sendChatCollect(record);
        log.info("对话采集成功，recordId：{}", record.getId());
    }

    @Override
    public void collectChat(ChatCollectRequest request, Long userId, String userApiKey) {
        collectChat(request, userId);
    }

    @Override
    public int batchImportChats(BatchChatImportRequest request, Long userId) {
        log.info("批量导入对话，userId：{}，数量：{}", userId, request.getChats() != null ? request.getChats().size() : 0);
        if (request.getChats() == null || request.getChats().isEmpty()) {
            return 0;
        }
        int count = 0;
        for (BatchChatImportRequest.ChatItem item : request.getChats()) {
            try {
                ChatCollectRequest collectRequest = new ChatCollectRequest();
                collectRequest.setContent(item.getContent());
                collectRequest.setPlatform(item.getPlatform());
                collectRequest.setSourceUrl(item.getSourceUrl());
                collectChat(collectRequest, userId);
                count++;
            } catch (Exception e) {
                log.warn("导入对话失败，跳过：{}", e.getMessage());
            }
        }
        return count;
    }

    @Override
    public int batchImportChats(BatchChatImportRequest request, Long userId, String userApiKey) {
        return batchImportChats(request, userId);
    }

    @Override
    public Page<ChatRecordDTO> getChatList(Long current, Long size, String platform, String keyword, Long userId) {
        Page<RawChatRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<RawChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RawChatRecord::getUserId, userId);
        if (platform != null && !platform.isEmpty()) {
            wrapper.eq(RawChatRecord::getPlatform, platform);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(RawChatRecord::getContent, keyword);
        }
        wrapper.orderByDesc(RawChatRecord::getCreateTime);
        Page<RawChatRecord> result = rawChatRecordMapper.selectPage(page, wrapper);

        Page<ChatRecordDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<ChatRecordDTO> dtoList = new ArrayList<>();
        for (RawChatRecord record : result.getRecords()) {
            ChatRecordDTO dto = new ChatRecordDTO();
            dto.setId(record.getId());
            dto.setPlatform(record.getPlatform());
            dto.setContent(record.getContent());
            dto.setSourceUrl(record.getSourceUrl());
            dto.setCreateTime(record.getCreateTime());
            dtoList.add(dto);
        }
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    public ChatRecordDTO getChatById(Long id, Long userId) {
        RawChatRecord record = rawChatRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            return null;
        }
        ChatRecordDTO dto = new ChatRecordDTO();
        dto.setId(record.getId());
        dto.setPlatform(record.getPlatform());
        dto.setContent(record.getContent());
        dto.setSourceUrl(record.getSourceUrl());
        dto.setCreateTime(record.getCreateTime());
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
            new LambdaQueryWrapper<RawChatRecord>().eq(RawChatRecord::getUserId, userId)
        );
    }

    @Override
    public long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return rawChatRecordMapper.selectCount(
            new LambdaQueryWrapper<RawChatRecord>()
                .eq(RawChatRecord::getUserId, userId)
                .ge(RawChatRecord::getCreateTime, startTime)
                .le(RawChatRecord::getCreateTime, endTime)
        );
    }
}
