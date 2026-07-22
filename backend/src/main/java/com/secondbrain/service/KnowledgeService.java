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
     * @param current 当前页码
     * @param size 每页大小
     * @param keyword 关键词
     * @param userId 用户ID
     * @param importance 重要程度
     * @param masteryLevel 掌握程度
     * @return 分页结果
     */
    Page<KnowledgeNodeVO> list(Integer current, Integer size, String keyword, Long userId, Integer importance, Integer masteryLevel);

    /**
     * 根据ID查询知识点.
     *
     * @param id 知识点ID
     * @param userId 用户ID
     * @return 知识点信息
     */
    KnowledgeNodeVO getById(Long id, Long userId);

    /**
     * 创建知识点.
     *
     * @param title 标题
     * @param summary 摘要
     * @param contentMd 内容（Markdown）
     * @param importance 重要程度
     * @param userId 用户ID
     * @return 创建的知识点
     */
    KnowledgeNodeVO create(String title, String summary, String contentMd, Integer importance, Long userId);

    /**
     * 删除知识点.
     *
     * @param id 知识点ID
     * @param userId 用户ID
     */
    void deleteById(Long id, Long userId);

    /**
     * 更新重要程度.
     *
     * @param id 知识点ID
     * @param importance 重要程度
     * @param userId 用户ID
     */
    void updateImportance(Long id, Integer importance, Long userId);

    /**
     * 更新知识点内容.
     *
     * @param id 知识点ID
     * @param title 标题
     * @param summary 摘要
     * @param contentMd 内容（Markdown）
     * @param userId 用户ID
     */
    void updateKnowledge(Long id, String title, String summary, String contentMd, Long userId);

    /**
     * 关键词搜索.
     *
     * @param keyword 关键词
     * @param userId 用户ID
     * @return 知识点列表
     */
    List<KnowledgeNodeVO> search(String keyword, Long userId);

    /**
     * 多字段搜索.
     *
     * @param keyword 关键词
     * @param userId 用户ID
     * @return 知识点列表
     */
    List<KnowledgeNodeVO> multiFieldSearch(String keyword, Long userId);

    /**
     * 语义搜索.
     *
     * @param queryText 查询文本
     * @param userId 用户ID
     * @param topK 返回数量
     * @return 知识点列表
     */
    List<KnowledgeNodeVO> semanticSearch(String queryText, Long userId, int topK);

    /**
     * 语义搜索（带API Key）.
     *
     * @param queryText 查询文本
     * @param userId 用户ID
     * @param topK 返回数量
     * @param userApiKey 用户API Key
     * @return 知识点列表
     */
    List<KnowledgeNodeVO> semanticSearch(String queryText, Long userId, int topK, String userApiKey);

    /**
     * 同步知识点到Elasticsearch.
     *
     * @param userId 用户ID
     */
    void syncToElasticsearch(Long userId);

    /**
     * 统计用户知识点数量.
     *
     * @param userId 用户ID
     * @return 知识点数量
     */
    long countByUserId(Long userId);

    /**
     * 统计指定时间范围内的知识点数量.
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 知识点数量
     */
    long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
}
