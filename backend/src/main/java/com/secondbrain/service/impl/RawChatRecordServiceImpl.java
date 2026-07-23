package com.secondbrain.service.impl;

import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.mapper.RawChatRecordMapper;
import com.secondbrain.service.RawChatRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 原始对话记录服务实现类.
 * <p>提供原始对话记录的保存和查询功能</p>
 */
@Service
public class RawChatRecordServiceImpl implements RawChatRecordService {

    private static final Logger log = LoggerFactory.getLogger(RawChatRecordServiceImpl.class);

    private final RawChatRecordMapper rawChatRecordMapper;

    public RawChatRecordServiceImpl(RawChatRecordMapper rawChatRecordMapper) {
        this.rawChatRecordMapper = rawChatRecordMapper;
    }

    /**
     * 保存原始对话记录.
     *
     * @param record 原始对话记录
     * @return 保存后的原始对话记录
     */
    @Override
    public RawChatRecord save(RawChatRecord record) {
        rawChatRecordMapper.insert(record);
        log.info("原始对话记录保存成功，id：{}", record.getId());
        return record;
    }
}
