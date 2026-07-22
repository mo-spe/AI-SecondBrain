package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.KnowledgeEmbedding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识向量数据访问接口.
 * <p>提供知识向量表的数据库操作</p>
 */
@Mapper
public interface KnowledgeEmbeddingMapper extends BaseMapper<KnowledgeEmbedding> {

    /**
     * 根据知识点ID获取向量.
     *
     * @param knowledgeId 知识点ID
     * @return 知识向量
     */
    @Select("SELECT * FROM knowledge_embedding WHERE knowledge_id = #{knowledgeId}")
    KnowledgeEmbedding getByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    /**
     * 根据用户ID获取所有向量.
     *
     * @param userId 用户ID
     * @return 知识向量列表
     */
    @Select("SELECT * FROM knowledge_embedding WHERE knowledge_id IN " +
            "(SELECT id FROM knowledge_node WHERE user_id = #{userId} AND deleted = 0)")
    List<KnowledgeEmbedding> getByUserId(@Param("userId") Long userId);
}
