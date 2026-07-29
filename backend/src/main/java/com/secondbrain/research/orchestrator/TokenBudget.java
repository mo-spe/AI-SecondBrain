package com.secondbrain.research.orchestrator;

/**
 * Token 消耗预算追踪.
 *
 * @author AI
 */
public class TokenBudget {

    private final long maxTokens;
    private long usedTokens;
    private final long warningThreshold;

    public TokenBudget(long maxTokens) {
        this.maxTokens = maxTokens;
        this.warningThreshold = (long) (maxTokens * 0.8);
    }

    public void consume(long tokens) {
        this.usedTokens += tokens;
    }

    public boolean isExceeded() {
        return usedTokens >= maxTokens;
    }

    public boolean isWarning() {
        return usedTokens >= warningThreshold;
    }

    public long getRemaining() {
        return Math.max(0, maxTokens - usedTokens);
    }

    public long getUsed() {
        return usedTokens;
    }

    public long getMaxTokens() {
        return maxTokens;
    }
}
