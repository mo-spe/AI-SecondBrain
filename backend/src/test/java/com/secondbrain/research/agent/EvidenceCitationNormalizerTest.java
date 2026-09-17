package com.secondbrain.research.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 研究发现引用规范化测试.
 *
 * @author AI
 */
class EvidenceCitationNormalizerTest {

    /**
     * 验证任务内引用会转换为全局来源索引，并清理越界及重复索引.
     */
    @Test
    void shouldNormalizeLocalSourceIndicesToGlobalIndices() {
        List<Map<String, Object>> findings = List.of(Map.of(
                "statement", "有证据的发现",
                "sourceIndices", List.of(1, 2, 2, 4)));

        List<Map<String, Object>> result = EvidenceCitationNormalizer.normalize(findings, 3, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("sourceIndices")).isEqualTo(List.of(6, 7));
    }

    /**
     * 验证没有有效来源的发现不会进入后续结论链路.
     */
    @Test
    void shouldRejectFindingsWithoutValidEvidence() {
        List<Map<String, Object>> findings = List.of(
                Map.of("statement", "无引用", "sourceIndices", List.of()),
                Map.of("statement", "越界引用", "sourceIndices", List.of(3)),
                Map.of("statement", "缺少引用字段"));

        List<Map<String, Object>> result = EvidenceCitationNormalizer.normalize(findings, 2, 0);

        assertThat(result).isEmpty();
    }
}
