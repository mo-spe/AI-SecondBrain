package com.secondbrain.research.agent;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行结果.
 *
 * @author AI
 */
@Getter
@Setter
public class AgentResult {

    /**
     * Agent 名称
     */
    private String agentName;

    /**
     * 执行状态：COMPLETED/FAILED/SKIPPED
     */
    private String status;

    /**
     * 结构化输出（JSON）
     */
    private String output;

    /**
     * 建议的下一个 Agent（可为 null 表示由 Orchestrator 决定）
     */
    private String nextAgent;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * Token 消耗
     */
    private int tokensUsed;

    /**
     * 错误列表
     */
    private List<String> errors = new ArrayList<>();

    public static AgentResult completed(String agentName, String output) {
        AgentResult result = new AgentResult();
        result.setAgentName(agentName);
        result.setStatus("COMPLETED");
        result.setOutput(output);
        return result;
    }

    public static AgentResult failed(String agentName, String error) {
        AgentResult result = new AgentResult();
        result.setAgentName(agentName);
        result.setStatus("FAILED");
        result.getErrors().add(error);
        return result;
    }

    public static AgentResult skipped(String agentName, String reason) {
        AgentResult result = new AgentResult();
        result.setAgentName(agentName);
        result.setStatus("SKIPPED");
        result.setOutput(reason);
        return result;
    }
}
