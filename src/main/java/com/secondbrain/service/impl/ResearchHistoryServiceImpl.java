package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.secondbrain.dto.ResearchHistoryRequest;
import com.secondbrain.entity.ResearchHistory;
import com.secondbrain.mapper.ResearchHistoryMapper;
import com.secondbrain.service.ResearchHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ResearchHistoryServiceImpl extends ServiceImpl<ResearchHistoryMapper, ResearchHistory> implements ResearchHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ResearchHistoryServiceImpl.class);
    
    private final ObjectMapper objectMapper;

    public ResearchHistoryServiceImpl() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ResearchHistory save(ResearchHistoryRequest request, Long userId) {
        log.info("开始保存研究历史，用户ID：{}，类型：{}，主题：{}", userId, request.getType(), request.getTopic());
        
        ResearchHistory history = new ResearchHistory();
        history.setUserId(userId);
        history.setType(request.getType());
        history.setTopic(request.getTopic());
        history.setContent(request.getContent());
        history.setCurrentLevel(request.getCurrentLevel());
        history.setTargetLevel(request.getTargetLevel());
        history.setDepth(request.getDepth());
        
        if (request.getUserKnowledge() != null && !request.getUserKnowledge().isEmpty()) {
            try {
                history.setUserKnowledge(objectMapper.writeValueAsString(request.getUserKnowledge()));
            } catch (Exception e) {
                log.error("序列化userKnowledge失败", e);
                history.setUserKnowledge(null);
            }
        } else {
            history.setUserKnowledge(null);
        }
        
        history.setKnowledgeCount(request.getKnowledgeCount() != null ? request.getKnowledgeCount() : 0);
        
        log.info("准备保存到数据库，history对象：{}", history);
        boolean result = this.save(history);
        log.info("保存结果：{}", result);
        
        return history;
    }

    @Override
    public IPage<ResearchHistory> getList(int current, int size, Long userId) {
        Page<ResearchHistory> page = new Page<>(current, size);
        LambdaQueryWrapper<ResearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResearchHistory::getUserId, userId)
               .orderByDesc(ResearchHistory::getCreateTime);
        
        return this.page(page, wrapper);
    }

    @Override
    public IPage<ResearchHistory> getList(int current, int size, Long userId, String type) {
        Page<ResearchHistory> page = new Page<>(current, size);
        LambdaQueryWrapper<ResearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResearchHistory::getUserId, userId);
        
        if (type != null && !type.isEmpty()) {
            wrapper.eq(ResearchHistory::getType, type);
        }
        
        wrapper.orderByDesc(ResearchHistory::getCreateTime);
        
        return this.page(page, wrapper);
    }

    @Override
    public ResearchHistory getById(Long id, Long userId) {
        LambdaQueryWrapper<ResearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResearchHistory::getId, id)
               .eq(ResearchHistory::getUserId, userId);
        
        return this.getOne(wrapper);
    }

    @Override
    public void deleteById(Long id, Long userId) {
        LambdaQueryWrapper<ResearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResearchHistory::getId, id)
               .eq(ResearchHistory::getUserId, userId);
        
        this.remove(wrapper);
    }
}
