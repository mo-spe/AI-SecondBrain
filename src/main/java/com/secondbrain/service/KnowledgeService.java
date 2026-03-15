package com.secondbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.vo.KnowledgeNodeVO;

import java.time.LocalDateTime;
import java.util.List;

public interface KnowledgeService {

    Page<KnowledgeNodeVO> list(Integer current, Integer size, String keyword, Long userId, Integer importance, Integer masteryLevel);

    KnowledgeNodeVO getById(Long id, Long userId);

    KnowledgeNodeVO create(String title, String summary, String contentMd, Integer importance, Long userId);

    void deleteById(Long id, Long userId);

    void updateImportance(Long id, Integer importance, Long userId);

    void updateKnowledge(Long id, String title, String summary, String contentMd, Long userId);

    List<KnowledgeNodeVO> search(String keyword, Long userId);

    List<KnowledgeNodeVO> multiFieldSearch(String keyword, Long userId);

    List<KnowledgeNodeVO> semanticSearch(String queryText, Long userId, int topK);

    void syncToElasticsearch(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
}
