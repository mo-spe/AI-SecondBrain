package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.KnowledgeEmbedding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeEmbeddingMapper extends BaseMapper<KnowledgeEmbedding> {

    @Select("SELECT * FROM knowledge_embedding WHERE knowledge_id = #{knowledgeId}")
    KnowledgeEmbedding getByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    @Select("SELECT * FROM knowledge_embedding WHERE knowledge_id IN " +
            "(SELECT id FROM knowledge_node WHERE user_id = #{userId} AND deleted = 0)")
    List<KnowledgeEmbedding> getByUserId(@Param("userId") Long userId);
}