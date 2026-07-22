package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.RawChatRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原始对话记录数据访问接口.
 * <p>提供原始对话记录表的数据库操作</p>
 */
@Mapper
public interface RawChatRecordMapper extends BaseMapper<RawChatRecord> {
}
