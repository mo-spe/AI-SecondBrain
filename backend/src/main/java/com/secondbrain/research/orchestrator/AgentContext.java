package com.secondbrain.research.orchestrator;

import com.secondbrain.entity.ResearchSource;
import com.secondbrain.vo.ResearchPlanVO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 共享执行上下文.
 *
 * <p>Agent 不持有状态，所有中间结果通过此上下文传递。
 * 每完成一个 Agent 后，Orchestrator 将上下文序列化存储以支持暂停/恢复。</p>
 *
 * @author AI
 */
public class AgentContext {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 工作空间ID
     */
    private Long workspaceId;

    /**
     * 执行会话ID（用于关联 research_step.execution_id）
     */
    private Long executionId;

    /**
     * 研究目标
     */
    private String researchGoal;

    /**
     * Planner 输出的研究计划
     */
    private ResearchPlanVO researchPlan;

    /**
     * 各 Agent 的输出结果（key=agentName, value=JSON 输出）
     */
    private final Map<String, String> agentOutputs = new LinkedHashMap<>();

    /**
     * 累计收集的来源
     */
    private final List<ResearchSource> allSources = new ArrayList<>();

    /**
     * 当前执行的 Agent 名称
     */
    private String currentAgent;

    /**
     * 当前执行的任务ID
     */
    private Long currentTaskId;

    /**
     * 当前迭代次数
     */
    private int currentIteration;

    /**
     * 最大迭代次数
     */
    private int maxIterations = 3;

    /**
     * LLM Token 消耗预算
     */
    private TokenBudget tokenBudget;

    /**
     * 外部工具调用预算
     */
    private ToolCallBudget toolCallBudget;

    /**
     * 单次研究项目的总时间预算
     */
    private TimeBudget timeBudget;

    /**
     * 补证循环停止原因，便于前端和日志解释为什么提前结束
     */
    private String recoveryStopReason;

    /**
     * Agent 执行历史
     */
    private final List<String> agentExecutionHistory = new ArrayList<>();

    /**
     * 是否处于降级模式
     */
    private boolean degradedMode;

    /**
     * 上次检查点时间
     */
    private Instant lastCheckpoint;

    /**
     * 暂停标记（用于外部中断）
     */
    private volatile boolean paused;

    // ======== getters/setters ========

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public String getResearchGoal() {
        return researchGoal;
    }

    public void setResearchGoal(String researchGoal) {
        this.researchGoal = researchGoal;
    }

    public ResearchPlanVO getResearchPlan() {
        return researchPlan;
    }

    public void setResearchPlan(ResearchPlanVO researchPlan) {
        this.researchPlan = researchPlan;
    }

    public Map<String, String> getAgentOutputs() {
        return agentOutputs;
    }

    public void putAgentOutput(String agentName, String output) {
        this.agentOutputs.put(agentName, output);
    }

    public String getAgentOutput(String agentName) {
        return this.agentOutputs.get(agentName);
    }

    public List<ResearchSource> getAllSources() {
        return allSources;
    }

    public void addSource(ResearchSource source) {
        this.allSources.add(source);
    }

    public String getCurrentAgent() {
        return currentAgent;
    }

    public void setCurrentAgent(String currentAgent) {
        this.currentAgent = currentAgent;
    }

    public Long getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(Long currentTaskId) {
        this.currentTaskId = currentTaskId;
    }

    public int getCurrentIteration() {
        return currentIteration;
    }

    public void setCurrentIteration(int currentIteration) {
        this.currentIteration = currentIteration;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /** @return Token 预算 */
    public TokenBudget getTokenBudget() {
        return tokenBudget;
    }

    /** @param tokenBudget Token 预算 */
    public void setTokenBudget(TokenBudget tokenBudget) {
        this.tokenBudget = tokenBudget;
    }

    /** @return 工具调用预算 */
    public ToolCallBudget getToolCallBudget() {
        return toolCallBudget;
    }

    /** @param toolCallBudget 工具调用预算 */
    public void setToolCallBudget(ToolCallBudget toolCallBudget) {
        this.toolCallBudget = toolCallBudget;
    }

    /** @return 项目时间预算 */
    public TimeBudget getTimeBudget() {
        return timeBudget;
    }

    /** @param timeBudget 项目时间预算 */
    public void setTimeBudget(TimeBudget timeBudget) {
        this.timeBudget = timeBudget;
    }

    /** @return 补证循环停止原因 */
    public String getRecoveryStopReason() {
        return recoveryStopReason;
    }

    /** @param recoveryStopReason 补证循环停止原因 */
    public void setRecoveryStopReason(String recoveryStopReason) {
        this.recoveryStopReason = recoveryStopReason;
    }

    public List<String> getAgentExecutionHistory() {
        return agentExecutionHistory;
    }

    public void recordAgentExecuted(String agentName) {
        this.agentExecutionHistory.add(agentName);
    }

    public boolean isDegradedMode() {
        return degradedMode;
    }

    public void setDegradedMode(boolean degradedMode) {
        this.degradedMode = degradedMode;
    }

    public Instant getLastCheckpoint() {
        return lastCheckpoint;
    }

    public void setLastCheckpoint(Instant lastCheckpoint) {
        this.lastCheckpoint = lastCheckpoint;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }
}
