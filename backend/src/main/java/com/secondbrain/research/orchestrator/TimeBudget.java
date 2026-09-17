package com.secondbrain.research.orchestrator;

/**
 * 时间预算追踪.
 *
 * @author AI
 */
public class TimeBudget {

    private final long projectTimeoutMs;
    private final long startTimeMs;

    /**
     * 创建项目时间预算并立即开始计时.
     *
     * @param projectTimeoutMs 项目超时时间
     */
    public TimeBudget(long projectTimeoutMs) {
        if (projectTimeoutMs <= 0) {
            throw new IllegalArgumentException("projectTimeoutMs 必须大于 0");
        }
        this.projectTimeoutMs = projectTimeoutMs;
        this.startTimeMs = System.currentTimeMillis();
    }

    /**
     * 判断项目是否超过总时间预算.
     *
     * @return 已超时时返回 true
     */
    public boolean isProjectTimeout() {
        return System.currentTimeMillis() - startTimeMs > projectTimeoutMs;
    }

    /**
     * 判断单个 Agent 是否超过声明的超时时间.
     *
     * @param agentTimeoutMs Agent 超时时间
     * @param agentStartMs   Agent 开始时间
     * @return 已超时时返回 true
     */
    public boolean isAgentTimeout(long agentTimeoutMs, long agentStartMs) {
        return System.currentTimeMillis() - agentStartMs > agentTimeoutMs;
    }

    /**
     * 获取项目已执行时间.
     *
     * @return 已执行毫秒数
     */
    public long getElapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    /**
     * 获取项目剩余时间.
     *
     * @return 非负剩余毫秒数
     */
    public long getRemainingMs() {
        return Math.max(0, projectTimeoutMs - getElapsedMs());
    }

    /**
     * 获取项目时间上限.
     *
     * @return 最大执行毫秒数
     */
    public long getProjectTimeoutMs() {
        return projectTimeoutMs;
    }
}
