package com.secondbrain.research.agent;

import com.secondbrain.enums.AiScenario;
import com.secondbrain.research.orchestrator.AgentContext;
import com.secondbrain.service.AiService;
import com.secondbrain.service.ResearchMemoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 研究记忆隔离与知识候选生成测试.
 *
 * @author AI
 */
@ExtendWith(MockitoExtension.class)
class ResearchMemoryAndCandidateTest {

    @Mock
    private AiService aiService;

    @Mock
    private ResearchMemoryService researchMemoryService;

    @Test
    void shouldNamespaceSameLogicalKeyByMemoryType() {
        assertThat(ResearchMemoryKey.of("FINDING", "同一陈述"))
                .isEqualTo("FINDING:同一陈述");
        assertThat(ResearchMemoryKey.of("CONCLUSION", "同一陈述"))
                .isEqualTo("CONCLUSION:同一陈述");
    }

    @Test
    void shouldPersistCandidateWhenDedupMetadataContainsNullNodeId() {
        KnowledgeWriterAgent agent = new KnowledgeWriterAgent(aiService, researchMemoryService);
        AgentContext context = new AgentContext();
        context.setProjectId(1L);
        context.setUserId(2L);
        context.setResearchGoal("研究目标");
        context.putAgentOutput("CriticAgent", """
                {"conclusions":[{"statement":"可信结论","confidence":"medium"}],
                 "validationStats":{"totalFindings":1}}
                """);

        when(aiService.chat(eq(2L), eq(AiScenario.RESEARCH.getCode()), anyList()))
                .thenReturn("""
                        [{"title":"知识标题","contentMd":"知识内容","summary":"摘要",
                          "tags":["研究"],"importance":4}]
                        """);

        AgentResult result = agent.execute(context);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getOutput()).contains("candidates", "知识标题");
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(researchMemoryService).save(
                eq(1L), eq(2L), keyCaptor.capture(), eq("CANDIDATE"),
                org.mockito.ArgumentMatchers.anyString());
        assertThat(keyCaptor.getValue()).isEqualTo("CANDIDATE:知识标题");
    }
}
