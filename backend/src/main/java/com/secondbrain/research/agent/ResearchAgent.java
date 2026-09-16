package com.secondbrain.research.agent;

import com.secondbrain.research.orchestrator.AgentContext;

/**
 * 研究 Agent 接口.
 *
 * <p>所有研究 Agent（Planner、Knowledge、Gap、Research、Critic、Synthesizer、KnowledgeWriter）
 * 必须实现此接口。Agent 必须是无状态的 Spring Bean，所有执行状态通过 AgentContext 传递。</p>
 *
 * @author AI
 */
public interface ResearchAgent {

    /**
     * Agent 唯一标识，如 "PlannerAgent"、"KnowledgeAgent".
     *
     * @return Agent 名称
     */
    String getName();

    /**
     * 执行 Agent 逻辑.
     *
     * @param context 当前研究上下文（包含所有中间结果和预算状态）
     * @return 结构化执行结果
     */
    AgentResult execute(AgentContext context);

    /**
     * 调度前回调，Agent 可根据上下文决定是否跳过.
     *
     * @param context 当前研究上下文
     * @return true 表示需要执行
     */
    default boolean shouldExecute(AgentContext context) {
        return true;
    }

    /**
     * Agent 级别超时时间（毫秒）.
     *
     * @return 超时毫秒数
     */
    default long getTimeoutMs() {
        return 120_000;
    }

    /**
     * 最大重试次数.
     *
     * @return 重试次数
     */
    default int getMaxRetries() {
        return 1;
    }
}
