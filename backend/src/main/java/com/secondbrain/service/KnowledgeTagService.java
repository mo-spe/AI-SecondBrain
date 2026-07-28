package com.secondbrain.service;

import com.secondbrain.dto.TagSuggestion;
import com.secondbrain.entity.KnowledgeTag;

import java.util.List;

/**
 * 知识标签服务接口.
 *
 * <p>提供标签的 CRUD 操作、层级树构建、AI 标签建议等功能。</p>
 */
public interface KnowledgeTagService {

    /**
     * 获取用户的所有标签（扁平列表）.
     *
     * @param userId 用户ID
     * @return 标签列表
     */
    List<KnowledgeTag> listByUser(Long userId);

    /**
     * 获取用户的标签树（含层级结构和知识点数量）.
     *
     * @param userId 用户ID
     * @return 标签树（根节点为 parentId==null 的标签）
     */
    List<KnowledgeTag> listTreeByUser(Long userId);

    /**
     * 创建标签.
     *
     * @param tagName  标签名称
     * @param tagColor 标签颜色
     * @param parentId 父标签ID（可选）
     * @param userId   用户ID
     * @return 创建后的标签
     */
    KnowledgeTag create(String tagName, String tagColor, Long parentId, Long userId);

    /**
     * 更新标签.
     *
     * @param tagId    标签ID
     * @param tagName  新标签名称（null 则不更新）
     * @param tagColor 新标签颜色（null 则不更新）
     * @param parentId 新父标签ID（null 表示不更新，设置为 0 表示置为顶级）
     * @param userId   用户ID
     * @return 更新后的标签
     */
    KnowledgeTag update(Long tagId, String tagName, String tagColor, Long parentId, Long userId);

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

    /**
     * AI 建议标签.
     *
     * <p>根据知识点标题和摘要，调用 AI 推荐 1-3 个最合适的标签。</p>
     *
     * @param title   知识点标题
     * @param summary 知识点摘要
     * @param userId  用户ID（用于获取 AI 配置）
     * @return 建议标签列表
     */
    List<TagSuggestion> suggestTags(String title, String summary, Long userId);
}
