package com.secondbrain.service;

import com.secondbrain.entity.ResearchMemory;
import com.secondbrain.entity.ResearchProject;
import com.secondbrain.mapper.ResearchMemoryMapper;
import com.secondbrain.mapper.ResearchProjectMapper;
import com.secondbrain.service.impl.ResearchMemoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 研究知识候选持久化操作测试。
 *
 * <p>确保候选仅在知识库写入成功后移除，避免前端显示成功但数据未落库。</p>
 *
 * @author AI
 */
@ExtendWith(MockitoExtension.class)
class ResearchMemoryCandidateServiceTest {

    @Mock
    private ResearchMemoryMapper researchMemoryMapper;

    @Mock
    private ResearchProjectMapper researchProjectMapper;

    @Mock
    private KnowledgeService knowledgeService;

    private ResearchMemoryServiceImpl researchMemoryService;

    @BeforeEach
    void setUp() {
        researchMemoryService = new ResearchMemoryServiceImpl(
                researchMemoryMapper, researchProjectMapper, knowledgeService);
    }

    @Test
    void shouldCreateKnowledgeAndDeleteCandidateWhenAccepted() {
        ResearchProject project = project();
        ResearchMemory candidate = candidate();
        when(researchProjectMapper.selectById(80L)).thenReturn(project);
        when(researchMemoryMapper.selectById(16L)).thenReturn(candidate);

        researchMemoryService.acceptCandidate(80L, 16L, 7L);

        verify(knowledgeService).create("Token Budget", null, candidate.getContent(), 3, 7L, 9L);
        verify(researchMemoryMapper).deleteById(16L);
    }

    @Test
    void shouldKeepCandidateWhenKnowledgeCreationFails() {
        ResearchProject project = project();
        ResearchMemory candidate = candidate();
        when(researchProjectMapper.selectById(80L)).thenReturn(project);
        when(researchMemoryMapper.selectById(16L)).thenReturn(candidate);
        doThrow(new IllegalStateException("write failed"))
                .when(knowledgeService).create("Token Budget", null, candidate.getContent(), 3, 7L, 9L);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> researchMemoryService.acceptCandidate(80L, 16L, 7L))
                .isInstanceOf(IllegalStateException.class);

        verify(researchMemoryMapper, never()).deleteById(16L);
    }

    @Test
    void shouldPersistentlyDeleteCandidateWhenDismissed() {
        when(researchProjectMapper.selectById(80L)).thenReturn(project());
        when(researchMemoryMapper.selectById(16L)).thenReturn(candidate());

        researchMemoryService.dismissCandidate(80L, 16L, 7L);

        verify(researchMemoryMapper).deleteById(16L);
        verify(knowledgeService, never()).create(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private ResearchProject project() {
        ResearchProject project = new ResearchProject();
        project.setId(80L);
        project.setUserId(7L);
        project.setWorkspaceId(9L);
        return project;
    }

    private ResearchMemory candidate() {
        ResearchMemory candidate = new ResearchMemory();
        candidate.setId(16L);
        candidate.setProjectId(80L);
        candidate.setUserId(7L);
        candidate.setMemoryType("CANDIDATE");
        candidate.setMemoryKey("CANDIDATE:Token Budget");
        candidate.setContent("### Token Budget\n\n用于限制研究消耗。");
        return candidate;
    }
}
