package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.RawChatRecord;

/**
 * 知识采集服务接口.
 * <p>从原始对话记录中提取知识并入库</p>
 */
public interface KnowledgeCaptureService {

    /**
     * 从原始记录中提取知识.
     *
     * @param record 原始聊天记录
     * @return 提取的知识节点
     */
    KnowledgeNode extractKnowledge(RawChatRecord record);
}
