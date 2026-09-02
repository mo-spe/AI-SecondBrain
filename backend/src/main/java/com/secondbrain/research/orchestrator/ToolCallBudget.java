package com.secondbrain.research.orchestrator;

import java.util.HashSet;
import java.util.Set;

/**
 * 工具调用预算追踪.
 *
 * @author AI
 */
public class ToolCallBudget {

    private final int maxCalls;
    private int usedCalls;
    private final Set<String> callKeys = new HashSet<>();

    /**
     * 创建工具调用预算.
     *
     * @param maxCalls 最大工具调用次数
     */
    public ToolCallBudget(int maxCalls) {
        if (maxCalls <= 0) {
            throw new IllegalArgumentException("maxCalls 必须大于 0");
        }
        this.maxCalls = maxCalls;
    }

    /**
     * 判断是否仍可调用工具.
     *
     * @return 有剩余额度时返回 true
     */
    public boolean canCall() {
        return usedCalls < maxCalls;
    }

    /**
     * 记录一次真实工具调用.
     *
     * @param toolName  工具名称
     * @param paramsHash 参数哈希
     */
    public void recordCall(String toolName, String paramsHash) {
        if (!canCall()) {
            throw new IllegalStateException("工具调用预算已耗尽");
        }
        usedCalls++;
        callKeys.add(buildCallKey(toolName, paramsHash));
    }

    /**
     * 判断相同工具参数是否已执行.
     *
     * @param toolName  工具名称
     * @param paramsHash 参数哈希
     * @return 已执行过时返回 true
     */
    public boolean isDuplicateCall(String toolName, String paramsHash) {
        return callKeys.contains(buildCallKey(toolName, paramsHash));
    }

    /**
     * 获取剩余调用次数.
     *
     * @return 非负剩余次数
     */
    public int getRemaining() {
        return Math.max(0, maxCalls - usedCalls);
    }

    /**
     * 获取累计调用次数.
     *
     * @return 已使用次数
     */
    public int getUsed() {
        return usedCalls;
    }

    /**
     * 获取调用上限.
     *
     * @return 最大调用次数
     */
    public int getMaxCalls() {
        return maxCalls;
    }

    private String buildCallKey(String toolName, String paramsHash) {
        return toolName + ":" + paramsHash;
    }
}
