package com.secondbrain.service.impl;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.KnowledgeCaptureService;
import com.secondbrain.service.AiService;
import com.secondbrain.dto.KnowledgeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识采集服务实现类.
 * <p>从原始对话记录中提取知识并入库</p>
 */
@Service
public class KnowledgeCaptureServiceImpl implements KnowledgeCaptureService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeCaptureServiceImpl.class);

    private final AiService aiService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    public KnowledgeCaptureServiceImpl(AiService aiService, KnowledgeNodeMapper knowledgeNodeMapper) {
        this.aiService = aiService;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
    }

    /**
     * 从对话记录中提取知识.
     *
     * @param record 对话记录
     * @return 知识节点
     */
    @Override
    public KnowledgeNode extractKnowledge(RawChatRecord record) {
        log.info("开始从对话记录中提取知识，recordId：{}", record.getId());
        try {
            List<KnowledgeDTO> knowledgeList = aiService.extractKnowledge(record.getContent());
            if (knowledgeList != null && !knowledgeList.isEmpty()) {
                KnowledgeDTO dto = knowledgeList.get(0);
                KnowledgeNode node = new KnowledgeNode();
                node.setUserId(record.getUserId());
                node.setTitle(dto.getTitle());
                node.setSummary(dto.getSummary());
                node.setContentMd(dto.getContent());
                node.setImportance(3);
                node.setMasteryLevel(0);
                node.setReviewCount(0);
                knowledgeNodeMapper.insert(node);
                log.info("知识提取成功，nodeId：{}", node.getId());
                return node;
            }
            log.warn("未提取到知识，recordId：{}", record.getId());
            return null;
        } catch (Exception e) {
            log.error("知识提取失败，recordId：{}", record.getId(), e);
            return null;
        }
    }
}
