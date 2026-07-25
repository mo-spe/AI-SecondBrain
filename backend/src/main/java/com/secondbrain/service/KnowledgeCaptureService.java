package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.RawChatRecord;

/**
 * 知识采集服务接口.
 * <p>从原始对话记录中提取知识，写入 pending_knowledge 表等待用户确认</p>
 */
public interface KnowledgeCaptureService {

    /**
     * 从原始记录中提取知识.
     * <p>AI 提取的知识点写入 pending_knowledge 表（status=0），不直接入库</p>
     *
     * @param record 原始聊天记录
     * @return 提取的待确认知识点数量
     */
    int extractKnowledge(RawChatRecord record);
}
