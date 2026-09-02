package com.secondbrain.research.orchestrator;

/**
 * 补证循环来源增长追踪器.
 *
 * <p>连续两轮没有新增来源说明继续调用搜索与模型的边际收益很低，应提前终止。</p>
 *
 * @author AI
 */
final class SourceGrowthTracker {

    private static final int MAX_CONSECUTIVE_STAGNANT_ROUNDS = 2;

    private int previousSourceCount;
    private int consecutiveStagnantRounds;

    SourceGrowthTracker(int initialSourceCount) {
        this.previousSourceCount = Math.max(initialSourceCount, 0);
    }

    boolean recordAndShouldStop(int currentSourceCount) {
        if (currentSourceCount > previousSourceCount) {
            consecutiveStagnantRounds = 0;
        } else {
            consecutiveStagnantRounds++;
        }
        previousSourceCount = Math.max(currentSourceCount, previousSourceCount);
        return consecutiveStagnantRounds >= MAX_CONSECUTIVE_STAGNANT_ROUNDS;
    }

    int getConsecutiveStagnantRounds() {
        return consecutiveStagnantRounds;
    }
}
