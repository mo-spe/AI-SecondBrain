package com.secondbrain.service.impl;

import com.secondbrain.entity.PendingKnowledge;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.mapper.PendingKnowledgeMapper;
import com.secondbrain.service.KnowledgeCaptureService;
import com.secondbrain.service.AiService;
import com.secondbrain.dto.KnowledgeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识采集服务实现类.
 * <p>从原始对话记录中提取知识，写入 pending_knowledge 表等待用户确认</p>
 */
@Service
public class KnowledgeCaptureServiceImpl implements KnowledgeCaptureService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeCaptureServiceImpl.class);

    private final AiService aiService;
    private final PendingKnowledgeMapper pendingKnowledgeMapper;

    public KnowledgeCaptureServiceImpl(AiService aiService, PendingKnowledgeMapper pendingKnowledgeMapper) {
        this.aiService = aiService;
        this.pendingKnowledgeMapper = pendingKnowledgeMapper;
    }

    /**
     * 从对话记录中提取知识.
     * <p>AI 提取结果全部写入 pending_knowledge 表（status=0），不直接入库</p>
     *
     * @param record 对话记录
     * @return 提取的待确认知识点数量
     */
    @Override
    public int extractKnowledge(RawChatRecord record) {
        log.info("开始从对话记录中提取知识 recordId={}", record.getId());
        try {
            List<KnowledgeDTO> knowledgeList = aiService.extractKnowledge(
                    record.getUserId(), "extraction", record.getContent());

            if (knowledgeList == null || knowledgeList.isEmpty()) {
                log.warn("未提取到知识 recordId={}", record.getId());
                return 0;
            }

            int count = 0;
            for (KnowledgeDTO dto : knowledgeList) {
                PendingKnowledge pending = new PendingKnowledge();
                pending.setUserId(record.getUserId());
                pending.setWorkspaceId(record.getWorkspaceId());
                pending.setRawChatId(record.getId());
                pending.setTitle(dto.getTitle());
                pending.setSummary(dto.getSummary());
                pending.setContent(dto.getContent());
                pending.setStatus(0);
                pending.setCreateTime(LocalDateTime.now());
                pending.setUpdateTime(LocalDateTime.now());
                pendingKnowledgeMapper.insert(pending);
                count++;
            }

            log.info("知识提取完成 recordId={} count={}", record.getId(), count);
            return count;
        } catch (Exception e) {
            log.error("知识提取失败 recordId={}", record.getId(), e);
            return 0;
        }
    }
}
