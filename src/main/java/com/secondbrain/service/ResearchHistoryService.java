package com.secondbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.dto.ResearchHistoryRequest;
import com.secondbrain.entity.ResearchHistory;

public interface ResearchHistoryService {

    ResearchHistory save(ResearchHistoryRequest request, Long userId);

    IPage<ResearchHistory> getList(int current, int size, Long userId);

    ResearchHistory getById(Long id, Long userId);

    void deleteById(Long id, Long userId);
}
