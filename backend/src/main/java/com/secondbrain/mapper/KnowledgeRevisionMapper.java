package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.KnowledgeRevision;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识节点版本历史 Mapper.
 * <p>提供版本历史的数据库操作</p>
 */
@Mapper
public interface KnowledgeRevisionMapper extends BaseMapper<KnowledgeRevision> {
}
