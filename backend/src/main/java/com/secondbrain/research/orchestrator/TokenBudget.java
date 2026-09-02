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

    /**
     * 创建 Token 预算.
     *
     * @param maxTokens 最大可用 Token
     */
    public TokenBudget(long maxTokens) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens 必须大于 0");
        }
        this.maxTokens = maxTokens;
        this.warningThreshold = (long) (maxTokens * 0.8);
    }

    /**
     * 累计已使用 Token.
     *
     * @param tokens 本次消耗量
     */
    public void consume(long tokens) {
        if (tokens < 0) {
            throw new IllegalArgumentException("tokens 不能小于 0");
        }
        this.usedTokens += tokens;
    }

    /**
     * 判断预算是否耗尽.
     *
     * @return 达到上限时返回 true
     */
    public boolean isExceeded() {
        return usedTokens >= maxTokens;
    }

    /**
     * 判断是否达到 80% 预警线.
     *
     * @return 达到预警线时返回 true
     */
    public boolean isWarning() {
        return usedTokens >= warningThreshold;
    }

    /**
     * 获取剩余 Token.
     *
     * @return 非负剩余额度
     */
    public long getRemaining() {
        return Math.max(0, maxTokens - usedTokens);
    }

    /**
     * 获取累计使用量.
     *
     * @return 已使用 Token
     */
    public long getUsed() {
        return usedTokens;
    }

    /**
     * 获取预算上限.
     *
     * @return 最大 Token
     */
    public long getMaxTokens() {
        return maxTokens;
    }
}
