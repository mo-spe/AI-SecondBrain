package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.ResearchMemory;
import com.secondbrain.entity.ResearchProject;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.ResearchMemoryMapper;
import com.secondbrain.mapper.ResearchProjectMapper;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.service.ResearchMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 研究记忆服务实现.
 *
 * @author AI
 */
@Service
public class ResearchMemoryServiceImpl implements ResearchMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ResearchMemoryServiceImpl.class);
    private static final String CANDIDATE_MEMORY_TYPE = "CANDIDATE";
    private static final String MEMORY_KEY_SEPARATOR = ":";
    private static final int DEFAULT_IMPORTANCE = 3;

    private final ResearchMemoryMapper researchMemoryMapper;
    private final ResearchProjectMapper researchProjectMapper;
    private final KnowledgeService knowledgeService;

    public ResearchMemoryServiceImpl(ResearchMemoryMapper researchMemoryMapper,
                                     ResearchProjectMapper researchProjectMapper,
                                     KnowledgeService knowledgeService) {
        this.researchMemoryMapper = researchMemoryMapper;
        this.researchProjectMapper = researchProjectMapper;
        this.knowledgeService = knowledgeService;
    }

    @Override
    @Transactional
    public ResearchMemory save(Long projectId, Long userId, String memoryKey,
                                String memoryType, String content) {
        checkProjectAccess(projectId, userId);

        // 按 projectId + memoryKey 查找现有记录
        List<ResearchMemory> existing = researchMemoryMapper.selectList(
                new LambdaQueryWrapper<ResearchMemory>()
                        .eq(ResearchMemory::getProjectId, projectId)
                        .eq(ResearchMemory::getMemoryKey, memoryKey));

        if (!existing.isEmpty()) {
            ResearchMemory mem = existing.get(0);
            mem.setContent(content);
            mem.setLastAccessedAt(LocalDateTime.now());
            researchMemoryMapper.updateById(mem);
            return mem;
        }

        ResearchMemory mem = new ResearchMemory();
        mem.setProjectId(projectId);
        mem.setUserId(userId);
        mem.setMemoryKey(memoryKey);
        mem.setMemoryType(memoryType);
        mem.setContent(content);
        mem.setLastAccessedAt(LocalDateTime.now());
        researchMemoryMapper.insert(mem);
        return mem;
    }

    @Override
    public List<ResearchMemory> listByProject(Long projectId, Long userId) {
        checkProjectAccess(projectId, userId);
        return researchMemoryMapper.selectList(
                new LambdaQueryWrapper<ResearchMemory>()
                        .eq(ResearchMemory::getProjectId, projectId)
                        .orderByDesc(ResearchMemory::getCreateTime));
    }

    @Override
    public List<ResearchMemory> listByType(Long projectId, Long userId, String memoryType) {
        checkProjectAccess(projectId, userId);
        return researchMemoryMapper.selectList(
                new LambdaQueryWrapper<ResearchMemory>()
                        .eq(ResearchMemory::getProjectId, projectId)
                        .eq(ResearchMemory::getMemoryType, memoryType)
                        .orderByDesc(ResearchMemory::getCreateTime));
    }

    @Override
    public ResearchMemory getByKey(Long projectId, Long userId, String memoryKey) {
        checkProjectAccess(projectId, userId);
        List<ResearchMemory> list = researchMemoryMapper.selectList(
                new LambdaQueryWrapper<ResearchMemory>()
                        .eq(ResearchMemory::getProjectId, projectId)
                        .eq(ResearchMemory::getMemoryKey, memoryKey)
                        .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        ResearchMemory mem = researchMemoryMapper.selectById(id);
        if (mem == null) {
            throw new BusinessException(404, "研究记忆不存在");
        }
        checkProjectAccess(mem.getProjectId(), userId);
        researchMemoryMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByProjectAndType(Long projectId, Long userId, String memoryType) {
        checkProjectAccess(projectId, userId);
        researchMemoryMapper.delete(new LambdaQueryWrapper<ResearchMemory>()
                .eq(ResearchMemory::getProjectId, projectId)
                .eq(ResearchMemory::getMemoryType, memoryType));
    }

    @Override
    @Transactional
    public void acceptCandidate(Long projectId, Long memoryId, Long userId) {
        ResearchProject project = checkProjectAccess(projectId, userId);
        ResearchMemory candidate = getCandidate(projectId, memoryId, userId);
        String title = extractCandidateTitle(candidate.getMemoryKey());

        // 只有知识节点完整创建成功后才移除候选，避免外部同步或入库失败造成知识丢失。
        knowledgeService.create(title, null, candidate.getContent(), DEFAULT_IMPORTANCE,
                userId, project.getWorkspaceId());
        researchMemoryMapper.deleteById(candidate.getId());
        log.info("研究知识候选已入库 projectId={} memoryId={} userId={}", projectId, memoryId, userId);
    }

    @Override
    @Transactional
    public void dismissCandidate(Long projectId, Long memoryId, Long userId) {
        ResearchMemory candidate = getCandidate(projectId, memoryId, userId);
        researchMemoryMapper.deleteById(candidate.getId());
        log.info("研究知识候选已忽略 projectId={} memoryId={} userId={}", projectId, memoryId, userId);
    }

    private ResearchMemory getCandidate(Long projectId, Long memoryId, Long userId) {
        checkProjectAccess(projectId, userId);
        ResearchMemory memory = researchMemoryMapper.selectById(memoryId);
        if (memory == null || !projectId.equals(memory.getProjectId()) || !userId.equals(memory.getUserId())) {
            throw new BusinessException(404, "知识候选不存在");
        }
        if (!CANDIDATE_MEMORY_TYPE.equals(memory.getMemoryType())) {
            throw new BusinessException(400, "仅知识候选可执行此操作");
        }
        return memory;
    }

    private String extractCandidateTitle(String memoryKey) {
        if (memoryKey == null || memoryKey.isBlank()) {
            return "研究知识候选";
        }
        int separatorIndex = memoryKey.indexOf(MEMORY_KEY_SEPARATOR);
        if (separatorIndex < 0 || separatorIndex == memoryKey.length() - 1) {
            return memoryKey;
        }
        return memoryKey.substring(separatorIndex + 1);
    }

    private ResearchProject checkProjectAccess(Long projectId, Long userId) {
        ResearchProject project = researchProjectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "研究项目不存在");
        }
        if (!project.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该研究项目");
        }
        return project;
    }
}
