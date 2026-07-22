package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天会话数据访问接口.
 * <p>提供聊天会话表的数据库操作</p>
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
