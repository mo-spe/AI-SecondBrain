package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.KnowledgeRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识关系数据访问接口.
 * <p>提供知识关系表的数据库操作</p>
 */
@Mapper
public interface KnowledgeRelationMapper extends BaseMapper<KnowledgeRelation> {

    /**
     * 根据用户ID查询知识关系列表.
     *
     * @param userId 用户ID
     * @return 知识关系列表
     */
    @Select("SELECT * FROM knowledge_relation WHERE user_id = #{userId} AND deleted = 0")
    List<KnowledgeRelation> findByUserId(Long userId);

    /**
     * 根据用户ID和来源知识点ID查询知识关系列表.
     *
     * @param userId 用户ID
     * @param fromKnowledgeId 来源知识点ID
     * @return 知识关系列表
     */
    @Select("SELECT * FROM knowledge_relation WHERE user_id = #{userId} AND from_knowledge_id = #{fromKnowledgeId} AND deleted = 0")
    List<KnowledgeRelation> findByUserIdAndFromKnowledgeId(Long userId, Long fromKnowledgeId);

    /**
     * 根据用户ID和目标知识点ID查询知识关系列表.
     *
     * @param userId 用户ID
     * @param toKnowledgeId 目标知识点ID
     * @return 知识关系列表
     */
    @Select("SELECT * FROM knowledge_relation WHERE user_id = #{userId} AND to_knowledge_id = #{toKnowledgeId} AND deleted = 0")
    List<KnowledgeRelation> findByUserIdAndToKnowledgeId(Long userId, Long toKnowledgeId);

    /**
     * 根据用户ID、来源知识点ID和目标知识点ID查询知识关系.
     *
     * @param userId 用户ID
     * @param fromKnowledgeId 来源知识点ID
     * @param toKnowledgeId 目标知识点ID
     * @return 知识关系
     */
    @Select("SELECT * FROM knowledge_relation WHERE user_id = #{userId} AND from_knowledge_id = #{fromKnowledgeId} AND to_knowledge_id = #{toKnowledgeId} AND deleted = 0")
    KnowledgeRelation findByUserIdAndFromAndTo(Long userId, Long fromKnowledgeId, Long toKnowledgeId);
}
