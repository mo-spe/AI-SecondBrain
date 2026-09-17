package com.secondbrain.service;

import com.secondbrain.entity.ResearchProject;
import com.secondbrain.mapper.ResearchProjectMapper;
import com.secondbrain.research.orchestrator.ResearchOrchestrator;
import com.secondbrain.service.impl.ResearchProjectServiceImpl;
import com.secondbrain.vo.ResearchProjectVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 部分完成研究项目生命周期测试.
 *
 * @author AI
 */
@ExtendWith(MockitoExtension.class)
class ResearchProjectPartialStatusTest {

    private static final Long PROJECT_ID = 30L;
    private static final Long USER_ID = 40L;

    @Mock
    private ResearchProjectMapper researchProjectMapper;

    @Mock
    private ResearchOrchestrator researchOrchestrator;

    private ResearchProject project;
    private ResearchProjectServiceImpl service;

    @BeforeEach
    void setUp() {
        project = new ResearchProject();
        project.setId(PROJECT_ID);
        project.setUserId(USER_ID);
        project.setTitle("部分完成项目");
        project.setGoal("继续补充证据");
        project.setStatus("PARTIAL");

        when(researchProjectMapper.selectById(PROJECT_ID)).thenReturn(project);
        service = new ResearchProjectServiceImpl(researchProjectMapper, researchOrchestrator);
    }

    @Test
    void shouldAllowPartialProjectToExecuteAgain() {
        ResearchProjectVO result = service.execute(PROJECT_ID, USER_ID, null);

        assertThat(result.getStatus()).isEqualTo("RESEARCHING");
        verify(researchOrchestrator).start(PROJECT_ID, USER_ID);
    }

    @Test
    void shouldAllowPartialProjectToBeArchived() {
        service.archive(PROJECT_ID, USER_ID, null);

        assertThat(project.getStatus()).isEqualTo("ARCHIVED");
        verify(researchProjectMapper).updateById(project);
    }
}
