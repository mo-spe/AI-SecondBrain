package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.PendingKnowledge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待确认知识点 Mapper.
 */
@Mapper
public interface PendingKnowledgeMapper extends BaseMapper<PendingKnowledge> {
}
