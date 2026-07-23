package com.secondbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.vo.KnowledgeNodeVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识节点服务接口.
 * <p>提供知识点的增删改查、搜索、同步等功能</p>
 */
public interface KnowledgeService {

    /**
     * 分页查询知识点列表.
     *
     * @param current 当前页
     * @param size 每页大小
     * @param keyword 关键词
     * @param userId 用户ID
     * @param importance 重要程度
     * @param masteryLevel 掌握程度
     * @param workspaceId 工作区ID
     * @return 知识节点分页
     */
    Page<KnowledgeNodeVO> list(Integer current, Integer size, String keyword, Long userId, Integer importance, Integer masteryLevel, Long workspaceId);

    /**
     * 根据ID查询知识点.
     *
     * @param id 知识点ID
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 知识节点
     */
    KnowledgeNodeVO getById(Long id, Long userId, Long workspaceId);

    /**
     * 创建知识点.
     *
     * @param title 标题
     * @param summary 摘要
     * @param contentMd Markdown内容
     * @param importance 重要程度
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 知识节点
     */
    KnowledgeNodeVO create(String title, String summary, String contentMd, Integer importance, Long userId, Long workspaceId);

    /**
     * 删除知识点.
     *
     * @param id 知识点ID
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    void deleteById(Long id, Long userId, Long workspaceId);

    /**
     * 更新重要程度.
     *
     * @param id 知识点ID
     * @param importance 重要程度
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    void updateImportance(Long id, Integer importance, Long userId, Long workspaceId);

    /**
     * 更新知识点内容.
     *
     * @param id 知识点ID
     * @param title 标题
     * @param summary 摘要
     * @param contentMd Markdown内容
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    void updateKnowledge(Long id, String title, String summary, String contentMd, Long userId, Long workspaceId);

    /**
     * 关键词搜索.
     *
     * @param keyword 关键词
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 知识节点列表
     */
    List<KnowledgeNodeVO> search(String keyword, Long userId, Long workspaceId);

    /**
     * 多字段搜索.
     *
     * @param keyword 关键词
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 知识节点列表
     */
    List<KnowledgeNodeVO> multiFieldSearch(String keyword, Long userId, Long workspaceId);

    /**
     * 语义搜索.
     *
     * @param queryText 查询文本
     * @param userId 用户ID
     * @param topK 返回前K条
     * @param workspaceId 工作区ID
     * @return 知识节点列表
     */
    List<KnowledgeNodeVO> semanticSearch(String queryText, Long userId, int topK, Long workspaceId);

    /**
     * 语义搜索（带API Key）.
     *
     * @param queryText 查询文本
     * @param userId 用户ID
     * @param topK 返回前K条
     * @param userApiKey 用户API Key
     * @param workspaceId 工作区ID
     * @return 知识节点列表
     */
    List<KnowledgeNodeVO> semanticSearch(String queryText, Long userId, int topK, String userApiKey, Long workspaceId);

    /**
     * 同步知识点到Elasticsearch.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    void syncToElasticsearch(Long userId, Long workspaceId);

    /**
     * 统计用户知识点数量.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 知识点数量
     */
    long countByUserId(Long userId, Long workspaceId);

    /**
     * 统计指定时间范围内的知识点数量.
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param workspaceId 工作区ID
     * @return 知识点数量
     */
    long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime, Long workspaceId);
}
