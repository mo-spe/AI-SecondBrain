package com.secondbrain.research.agent;

/**
 * 研究记忆物理键生成器.
 *
 * <p>数据库唯一约束只有 projectId + memoryKey，因此在键中加入类型命名空间，
 * 避免相同陈述的 FINDING、CONCLUSION 和 CANDIDATE 相互覆盖。</p>
 *
 * @author AI
 */
final class ResearchMemoryKey {

    private static final int MAX_KEY_LENGTH = 100;

    private ResearchMemoryKey() {
    }

    static String of(String memoryType, String logicalKey) {
        String prefix = memoryType + ":";
        String normalizedKey = logicalKey == null ? "" : logicalKey.trim();
        int availableLength = Math.max(0, MAX_KEY_LENGTH - prefix.length());
        if (normalizedKey.length() > availableLength) {
            normalizedKey = normalizedKey.substring(0, availableLength);
        }
        return prefix + normalizedKey;
    }
}
