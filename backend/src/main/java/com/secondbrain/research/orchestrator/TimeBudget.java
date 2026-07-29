package com.secondbrain.research.orchestrator;

/**
 * 时间预算追踪.
 *
 * @author AI
 */
public class TimeBudget {

    private final long projectTimeoutMs;
    private final long startTimeMs;

    public TimeBudget(long projectTimeoutMs) {
        this.projectTimeoutMs = projectTimeoutMs;
        this.startTimeMs = System.currentTimeMillis();
    }

    public boolean isProjectTimeout() {
        return System.currentTimeMillis() - startTimeMs > projectTimeoutMs;
    }

    public boolean isAgentTimeout(long agentTimeoutMs, long agentStartMs) {
        return System.currentTimeMillis() - agentStartMs > agentTimeoutMs;
    }

    public long getElapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    public long getRemainingMs() {
        return Math.max(0, projectTimeoutMs - getElapsedMs());
    }
}
