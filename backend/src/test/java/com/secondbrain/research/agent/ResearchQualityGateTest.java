package com.secondbrain.research.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Critic 研究质量门禁测试.
 *
 * @author AI
 */
class ResearchQualityGateTest {

    @Test
    void shouldPassWhenEvidenceQualityIsSufficient() {
        List<Map<String, Object>> conclusions = List.of(
                Map.of("statement", "结论一", "confidence", "high"),
                Map.of("statement", "结论二", "confidence", "medium"));

        Map<String, Object> result = ResearchQualityGate.evaluate(conclusions, 0, "研究目标");

        assertThat(result.get("passed")).isEqualTo(true);
        assertThat(result.get("retryRequired")).isEqualTo(false);
    }

    @Test
    void shouldRequestFollowUpResearchForWeakEvidence() {
        List<Map<String, Object>> conclusions = List.of(
                Map.of("statement", "需要补证的结论", "confidence", "low"));

        Map<String, Object> result = ResearchQualityGate.evaluate(conclusions, 0, "研究目标");

        assertThat(result.get("passed")).isEqualTo(false);
        assertThat(result.get("retryRequired")).isEqualTo(true);
        assertThat(castStringList(result.get("followUpQueries")))
                .containsExactly("需要补证的结论");
    }

    @Test
    void shouldFailWhenConclusionsConflict() {
        List<Map<String, Object>> conclusions = List.of(
                Map.of("statement", "结论一", "confidence", "high"));

        Map<String, Object> result = ResearchQualityGate.evaluate(conclusions, 1, "研究目标");

        assertThat(result.get("passed")).isEqualTo(false);
        assertThat(castStringList(result.get("reasons")))
                .contains("存在尚未解决的结论冲突");
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        return (List<String>) value;
    }
}
