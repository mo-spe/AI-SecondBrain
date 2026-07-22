package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息数据访问接口.
 * <p>提供聊天消息表的数据库操作</p>
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
