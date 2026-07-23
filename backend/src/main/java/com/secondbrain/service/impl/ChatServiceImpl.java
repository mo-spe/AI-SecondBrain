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

    /**
     * 采集单条对话记录.
     *
     * @param request     对话采集请求
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return void
     */
    @Override
    public void collectChat(ChatCollectRequest request, Long userId, Long workspaceId) {
        log.info("采集对话，userId：{}，workspaceId：{}，platform：{}", userId, workspaceId, request.getPlatform());
        RawChatRecord record = new RawChatRecord();
        record.setUserId(userId);
        record.setWorkspaceId(workspaceId);
        record.setPlatform(request.getPlatform());
        record.setContent(request.getContent());
        record.setSourceUrl(request.getSourceUrl());
        record.setCreateTime(LocalDateTime.now());
        rawChatRecordMapper.insert(record);
        kafkaProducerService.sendChatCollect(record);
        log.info("对话采集成功，recordId：{}", record.getId());
    }

    /**
     * 采集单条对话记录.
     *
     * @param request     对话采集请求
     * @param userId      用户ID
     * @param userApiKey  用户API Key
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return void
     */
    @Override
    public void collectChat(ChatCollectRequest request, Long userId, String userApiKey, Long workspaceId) {
        collectChat(request, userId, workspaceId);
    }

    /**
     * 批量导入对话记录.
     *
     * @param request     批量导入请求
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return 成功导入数量
     */
    @Override
    public int batchImportChats(BatchChatImportRequest request, Long userId, Long workspaceId) {
        log.info("批量导入对话，userId：{}，workspaceId：{}，数量：{}", userId, workspaceId, request.getChats() != null ? request.getChats().size() : 0);
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
                collectChat(collectRequest, userId, workspaceId);
                count++;
            } catch (Exception e) {
                log.warn("导入对话失败，跳过：{}", e.getMessage());
            }
        }
        return count;
    }

    /**
     * 批量导入对话记录.
     *
     * @param request     批量导入请求
     * @param userId      用户ID
     * @param userApiKey  用户API Key
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return 成功导入数量
     */
    @Override
    public int batchImportChats(BatchChatImportRequest request, Long userId, String userApiKey, Long workspaceId) {
        return batchImportChats(request, userId, workspaceId);
    }

    /**
     * 分页查询对话列表.
     *
     * @param current     当前页
     * @param size        每页大小
     * @param platform    平台过滤（可选）
     * @param keyword     关键字过滤（可选）
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return 对话记录分页
     */
    @Override
    public Page<ChatRecordDTO> getChatList(Long current, Long size, String platform, String keyword, Long userId, Long workspaceId) {
        Page<RawChatRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<RawChatRecord> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
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

    /**
     * 根据ID查询对话记录.
     *
     * @param id          对话ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return 对话记录
     */
    @Override
    public ChatRecordDTO getChatById(Long id, Long userId, Long workspaceId) {
        RawChatRecord record = rawChatRecordMapper.selectById(id);
        if (record == null || !hasAccess(record, userId, workspaceId)) {
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

    /**
     * 从文本内容提取知识点.
     *
     * @param content 文本内容
     * @return 知识点列表
     */
    @Override
    public List<KnowledgeDTO> extractKnowledge(String content) {
        return aiService.extractKnowledge(content);
    }

    /**
     * 从文本内容提取知识点.
     *
     * @param content    文本内容
     * @param userApiKey 用户API Key
     * @return 知识点列表
     */
    @Override
    public List<KnowledgeDTO> extractKnowledge(String content, String userApiKey) {
        return aiService.extractKnowledge(content, userApiKey);
    }

    /**
     * 统计用户对话数量.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return 对话数量
     */
    @Override
    public long countByUserId(Long userId, Long workspaceId) {
        LambdaQueryWrapper<RawChatRecord> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        return rawChatRecordMapper.selectCount(wrapper);
    }

    /**
     * 统计指定时间范围内用户对话数量.
     *
     * @param userId      用户ID
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return 对话数量
     */
    @Override
    public long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime, Long workspaceId) {
        LambdaQueryWrapper<RawChatRecord> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        wrapper.ge(RawChatRecord::getCreateTime, startTime);
        wrapper.le(RawChatRecord::getCreateTime, endTime);
        return rawChatRecordMapper.selectCount(wrapper);
    }

    /**
     * 应用用户或工作区过滤条件.
     * workspaceId 为 null 时降级为 userId 过滤，兼容迁移前未分配工作区的历史数据.
     */
    private void applyUserOrWorkspaceFilter(LambdaQueryWrapper<RawChatRecord> wrapper, Long userId, Long workspaceId) {
        if (workspaceId != null) {
            wrapper.eq(RawChatRecord::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(RawChatRecord::getUserId, userId);
        }
    }

    /**
     * 检查记录访问权限.
     * workspaceId 不为 null 时按工作区校验，否则按 userId 校验.
     */
    private boolean hasAccess(RawChatRecord record, Long userId, Long workspaceId) {
        if (workspaceId != null) {
            return workspaceId.equals(record.getWorkspaceId());
        }
        return record.getUserId().equals(userId);
    }
}
