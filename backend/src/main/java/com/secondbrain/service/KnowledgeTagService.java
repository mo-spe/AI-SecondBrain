package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeTag;

import java.util.List;

/**
 * 知识标签服务接口.
 *
 * <p>提供标签的 CRUD 操作，支撑知识节点的分类管理和领域排行榜筛选。</p>
 */
public interface KnowledgeTagService {

    /**
     * 获取用户的所有标签.
     *
     * @param userId 用户ID
     * @return 标签列表
     */
    List<KnowledgeTag> listByUser(Long userId);

    /**
     * 创建标签.
     *
     * @param tagName  标签名称
     * @param tagColor 标签颜色
     * @param userId   用户ID
     * @return 创建后的标签
     */
    KnowledgeTag create(String tagName, String tagColor, Long userId);

    /**
     * 删除标签.
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     */
    void delete(Long tagId, Long userId);

    /**
     * 给知识节点添加标签.
     *
     * @param nodeId 知识节点ID
     * @param tagId  标签ID
     */
    void addTagToNode(Long nodeId, Long tagId);

    /**
     * 移除知识节点的标签.
     *
     * @param nodeId 知识节点ID
     * @param tagId  标签ID
     */
    void removeTagFromNode(Long nodeId, Long tagId);

    /**
     * 获取知识节点的所有标签.
     *
     * @param nodeId 知识节点ID
     * @return 标签列表
     */
    List<KnowledgeTag> listByNode(Long nodeId);

    /**
     * 获取所有标签（用于排行榜领域筛选下拉）.
     *
     * @return 全部标签列表
     */
    List<KnowledgeTag> listAll();
}
