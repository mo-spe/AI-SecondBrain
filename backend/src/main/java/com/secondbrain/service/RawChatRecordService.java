package com.secondbrain.service;

import com.secondbrain.entity.RawChatRecord;

/**
 * 原始对话记录服务接口.
 * <p>提供原始对话记录的保存和查询功能</p>
 */
public interface RawChatRecordService {

    /**
     * 保存原始对话记录.
     *
     * @param record 原始聊天记录
     * @return 保存后的记录
     */
    RawChatRecord save(RawChatRecord record);
}
