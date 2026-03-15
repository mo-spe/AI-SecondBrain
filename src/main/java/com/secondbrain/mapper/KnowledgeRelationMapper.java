package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.KnowledgeRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeRelationMapper extends BaseMapper<KnowledgeRelation> {

    @Select("SELECT * FROM knowledge_relation WHERE user_id = #{userId} AND deleted = 0")
    List<KnowledgeRelation> findByUserId(Long userId);

    @Select("SELECT * FROM knowledge_relation WHERE user_id = #{userId} AND from_knowledge_id = #{fromKnowledgeId} AND deleted = 0")
    List<KnowledgeRelation> findByUserIdAndFromKnowledgeId(Long userId, Long fromKnowledgeId);

    @Select("SELECT * FROM knowledge_relation WHERE user_id = #{userId} AND to_knowledge_id = #{toKnowledgeId} AND deleted = 0")
    List<KnowledgeRelation> findByUserIdAndToKnowledgeId(Long userId, Long toKnowledgeId);

    @Select("SELECT * FROM knowledge_relation WHERE user_id = #{userId} AND from_knowledge_id = #{fromKnowledgeId} AND to_knowledge_id = #{toKnowledgeId} AND deleted = 0")
    KnowledgeRelation findByUserIdAndFromAndTo(Long userId, Long fromKnowledgeId, Long toKnowledgeId);
}