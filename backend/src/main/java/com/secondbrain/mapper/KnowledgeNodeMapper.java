package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.KnowledgeNode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识点数据访问接口.
 * <p>提供知识点表的数据库操作</p>
 */
@Mapper
public interface KnowledgeNodeMapper extends BaseMapper<KnowledgeNode> {
}
