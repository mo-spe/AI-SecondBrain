package com.secondbrain.research.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Critic 研究质量门禁.
 *
 * <p>将“结论是否足以进入报告”转换为确定性规则，避免由 LLM 自行判断是否通过。</p>
 *
 * @author AI
 */
final class ResearchQualityGate {

    private static final double MIN_ACCEPTABLE_CONFIDENCE_RATIO = 0.7;
    private static final int MIN_MEDIUM_CONFIDENCE_CONCLUSIONS = 2;
    private static final int MAX_FOLLOW_UP_QUERIES = 5;

    private ResearchQualityGate() {
    }

    static Map<String, Object> evaluate(List<Map<String, Object>> conclusions,
                                        int controversialCount,
                                        String researchGoal) {
        List<String> reasons = new ArrayList<>();
        long highCount = countByConfidence(conclusions, "high");
        long mediumCount = countByConfidence(conclusions, "medium");
        long acceptableCount = highCount + mediumCount;
        double acceptableRatio = conclusions.isEmpty()
                ? 0.0 : (double) acceptableCount / conclusions.size();

        if (conclusions.isEmpty()) {
            reasons.add("没有可验证的研究结论");
        }
        if (controversialCount > 0) {
            reasons.add("存在尚未解决的结论冲突");
        }
        if (!conclusions.isEmpty() && acceptableRatio < MIN_ACCEPTABLE_CONFIDENCE_RATIO) {
            reasons.add("中高置信结论占比不足 70%");
        }
        if (highCount == 0 && mediumCount < MIN_MEDIUM_CONFIDENCE_CONCLUSIONS) {
            reasons.add("缺少高置信结论，且中置信结论少于 2 条");
        }

        List<String> followUpQueries = buildFollowUpQueries(conclusions, researchGoal);
        boolean passed = reasons.isEmpty();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", passed);
        result.put("retryRequired", !passed && !followUpQueries.isEmpty());
        result.put("reasons", reasons);
        result.put("acceptableConfidenceRatio", acceptableRatio);
        result.put("followUpQueries", followUpQueries);
        return result;
    }

    private static long countByConfidence(List<Map<String, Object>> conclusions,
                                          String confidence) {
        return conclusions.stream()
                .filter(conclusion -> confidence.equals(conclusion.get("confidence")))
                .count();
    }

    private static List<String> buildFollowUpQueries(List<Map<String, Object>> conclusions,
                                                      String researchGoal) {
        List<String> queries = conclusions.stream()
                .filter(conclusion -> !"high".equals(conclusion.get("confidence")))
                .map(conclusion -> conclusion.get("statement"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(statement -> !statement.isBlank())
                .distinct()
                .limit(MAX_FOLLOW_UP_QUERIES)
                .toList();
        if (!queries.isEmpty()) {
            return queries;
        }
        return researchGoal == null || researchGoal.isBlank()
                ? List.of() : List.of(researchGoal);
    }
}
