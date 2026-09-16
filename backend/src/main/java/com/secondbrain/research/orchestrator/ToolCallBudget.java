package com.secondbrain.research.orchestrator;

/**
 * 工具调用预算追踪.
 *
 * @author AI
 */
public class ToolCallBudget {

    private final int maxCalls;
    private int usedCalls;

    public ToolCallBudget(int maxCalls) {
        this.maxCalls = maxCalls;
    }

    public boolean canCall() {
        return usedCalls < maxCalls;
    }

    public void recordCall() {
        usedCalls++;
    }

    public boolean isDuplicateCall(String toolName, String paramsHash) {
        // 简化实现：不做实际的去重检查
        return false;
    }

    public int getRemaining() {
        return Math.max(0, maxCalls - usedCalls);
    }

    public int getUsed() {
        return usedCalls;
    }
}
