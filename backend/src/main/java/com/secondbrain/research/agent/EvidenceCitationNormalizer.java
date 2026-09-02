package com.secondbrain.research.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 研究发现引用规范化器.
 *
 * <p>LLM 返回的来源索引只在当前研究任务内有效。该类负责剔除无效引用，
 * 并将任务内索引转换为整个研究结果中的全局索引，避免发现引用到其他任务的来源。</p>
 *
 * @author AI
 */
final class EvidenceCitationNormalizer {

    private EvidenceCitationNormalizer() {
    }

    /**
     * 规范化研究发现的来源引用.
     *
     * <p>没有任何有效来源引用的发现不会进入后续 Critic 和报告流程，
     * 防止模型自身知识被误标记为已有外部证据的研究结论。</p>
     *
     * @param findings        当前任务提取的发现
     * @param localSourceCount 当前任务可引用的来源数量
     * @param globalOffset    当前任务来源在全局来源列表中的起始偏移
     * @return 仅包含有效证据引用的规范化发现
     */
    static List<Map<String, Object>> normalize(List<Map<String, Object>> findings,
                                                int localSourceCount,
                                                int globalOffset) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> finding : findings) {
            List<Integer> sourceIndices = normalizeIndices(
                    finding.get("sourceIndices"), localSourceCount, globalOffset);
            if (sourceIndices.isEmpty()) {
                continue;
            }

            Map<String, Object> normalizedFinding = new LinkedHashMap<>(finding);
            normalizedFinding.put("sourceIndices", sourceIndices);
            normalized.add(normalizedFinding);
        }
        return normalized;
    }

    private static List<Integer> normalizeIndices(Object rawIndices,
                                                   int localSourceCount,
                                                   int globalOffset) {
        if (!(rawIndices instanceof List<?> indices)) {
            return List.of();
        }

        return indices.stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue)
                .filter(index -> index >= 1 && index <= localSourceCount)
                .distinct()
                .map(index -> globalOffset + index)
                .toList();
    }
}
