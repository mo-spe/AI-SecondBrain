package com.secondbrain.research.orchestrator;

import com.secondbrain.entity.ResearchProject;
import com.secondbrain.mapper.ResearchProjectMapper;
import com.secondbrain.mapper.ResearchStepMapper;
import com.secondbrain.research.agent.AgentResult;
import com.secondbrain.research.agent.ResearchAgent;
import com.secondbrain.service.ResearchPlanService;
import com.secondbrain.service.ResearchTaskService;
import com.secondbrain.vo.ResearchPlanVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 研究编排器终态语义测试.
 *
 * <p>使用纯 Mockito 验证 Agent 链结果与项目、任务终态的一致性，避免依赖数据库环境。</p>
 *
 * @author AI
 */
@ExtendWith(MockitoExtension.class)
class ResearchOrchestratorTest {

    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 20L;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ResearchProjectMapper researchProjectMapper;

    @Mock
    private ResearchPlanService researchPlanService;

    @Mock
    private ResearchStepMapper researchStepMapper;

    @Mock
    private ResearchTaskService researchTaskService;

    @Mock
    private ResearchAgent researchAgent;

    private ResearchProject project;
    private ResearchOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        project = new ResearchProject();
        project.setId(PROJECT_ID);
        project.setUserId(USER_ID);
        project.setGoal("验证研究质量");
        project.setStatus("DRAFT");
        project.setMaxIterations(3);

        ResearchPlanVO plan = new ResearchPlanVO();
        plan.setAgentChain("TestAgent");

        when(researchProjectMapper.selectById(PROJECT_ID)).thenReturn(project);
        when(researchPlanService.getLatestByProject(PROJECT_ID, USER_ID)).thenReturn(plan);
        when(applicationContext.getBean(eq("TestAgent"), eq(ResearchAgent.class)))
                .thenReturn(researchAgent);
        when(researchAgent.shouldExecute(org.mockito.ArgumentMatchers.any(AgentContext.class)))
                .thenReturn(true);

        orchestrator = new ResearchOrchestrator(
                applicationContext,
                researchProjectMapper,
                researchPlanService,
                researchStepMapper,
                researchTaskService,
                60_000,
                60,
                600_000);
    }

    @Test
    void shouldCompleteProjectWhenAgentChainSucceeds() {
        when(researchAgent.execute(org.mockito.ArgumentMatchers.any(AgentContext.class)))
                .thenReturn(AgentResult.completed("TestAgent", "{}"));

        orchestrator.start(PROJECT_ID, USER_ID);

        assertThat(project.getStatus()).isEqualTo("COMPLETED");
        assertThat(project.getCompletedAt()).isNotNull();
        verify(researchTaskService).updateStatusByProject(PROJECT_ID, USER_ID, "COMPLETED");
    }

    @Test
    void shouldMarkProjectPartialAndTasksFailedWhenAgentChainDegrades() {
        when(researchAgent.execute(org.mockito.ArgumentMatchers.any(AgentContext.class)))
                .thenReturn(AgentResult.failed("TestAgent", "外部依赖不可用"));

        orchestrator.start(PROJECT_ID, USER_ID);

        assertThat(project.getStatus()).isEqualTo("PARTIAL");
        assertThat(project.getCompletedAt()).isNotNull();
        verify(researchTaskService).updateStatusByProject(PROJECT_ID, USER_ID, "FAILED");
    }
}
