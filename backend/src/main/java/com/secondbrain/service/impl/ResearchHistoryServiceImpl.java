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

/**
 * 研究历史服务实现类.
 * <p>提供AI研究历史的保存、分页查询及删除功能</p>
 */
@Service
public class ResearchHistoryServiceImpl extends ServiceImpl<ResearchHistoryMapper, ResearchHistory> implements ResearchHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ResearchHistoryServiceImpl.class);

    private final ObjectMapper objectMapper;

    public ResearchHistoryServiceImpl() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 保存研究历史记录.
     *
     * @param request     研究历史请求
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 保存后的研究历史记录
     */
    @Override
    public ResearchHistory save(ResearchHistoryRequest request, Long userId, Long workspaceId) {
        log.info("开始保存研究历史，用户ID：{}，workspaceId：{}，类型：{}，主题：{}", userId, workspaceId, request.getType(), request.getTopic());

        ResearchHistory history = new ResearchHistory();
        history.setUserId(userId);
        history.setWorkspaceId(workspaceId);
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

    /**
     * 分页查询研究历史列表.
     *
     * @param current     当前页
     * @param size        每页数量
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 研究历史分页结果
     */
    @Override
    public IPage<ResearchHistory> getList(int current, int size, Long userId, Long workspaceId) {
        Page<ResearchHistory> page = new Page<>(current, size);
        LambdaQueryWrapper<ResearchHistory> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        wrapper.orderByDesc(ResearchHistory::getCreateTime);
        return this.page(page, wrapper);
    }

    /**
     * 分页查询研究历史列表（按类型过滤）.
     *
     * @param current     当前页
     * @param size        每页数量
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @param type        研究类型
     * @return 研究历史分页结果
     */
    @Override
    public IPage<ResearchHistory> getList(int current, int size, Long userId, Long workspaceId, String type) {
        Page<ResearchHistory> page = new Page<>(current, size);
        LambdaQueryWrapper<ResearchHistory> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(ResearchHistory::getType, type);
        }
        wrapper.orderByDesc(ResearchHistory::getCreateTime);
        return this.page(page, wrapper);
    }

    /**
     * 根据ID获取研究历史.
     *
     * @param id          研究历史ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 研究历史记录
     */
    @Override
    public ResearchHistory getById(Long id, Long userId, Long workspaceId) {
        LambdaQueryWrapper<ResearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResearchHistory::getId, id);
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        return this.getOne(wrapper);
    }

    /**
     * 根据ID删除研究历史.
     *
     * @param id          研究历史ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    @Override
    public void deleteById(Long id, Long userId, Long workspaceId) {
        LambdaQueryWrapper<ResearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResearchHistory::getId, id);
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        this.remove(wrapper);
    }

    /**
     * 应用用户或工作区过滤条件.
     * workspaceId 为 null 时降级为 userId 过滤，兼容迁移前未分配工作区的历史数据.
     */
    private void applyUserOrWorkspaceFilter(LambdaQueryWrapper<ResearchHistory> wrapper, Long userId, Long workspaceId) {
        if (workspaceId != null) {
            wrapper.eq(ResearchHistory::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(ResearchHistory::getUserId, userId);
        }
    }
}
