package com.secondbrain.research.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.entity.ResearchProject;
import com.secondbrain.entity.ResearchStep;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.ResearchProjectMapper;
import com.secondbrain.mapper.ResearchStepMapper;
import com.secondbrain.research.agent.AgentResult;
import com.secondbrain.research.agent.ResearchAgent;
import com.secondbrain.service.ResearchPlanService;
import com.secondbrain.service.ResearchTaskService;
import com.secondbrain.vo.ResearchPlanVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 研究编排器.
 *
 * <p>负责按研究计划中的 agentChain 依次调度 Agent 执行，
 * 管理状态机、暂停/恢复、预算检查和检查点保存。
 * Phase 5 提供基本调度能力，完整多 Agent 链在 Phase 13 实现。</p>
 *
 * @author AI
 */
@Service
public class ResearchOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ResearchOrchestrator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ApplicationContext applicationContext;
    private final ResearchProjectMapper researchProjectMapper;
    private final ResearchPlanService researchPlanService;
    private final ResearchStepMapper researchStepMapper;
    private final ResearchTaskService researchTaskService;
    private final long maxTokens;
    private final int maxToolCalls;
    private final long projectTimeoutMs;

    public ResearchOrchestrator(ApplicationContext applicationContext,
                                ResearchProjectMapper researchProjectMapper,
                                ResearchPlanService researchPlanService,
                                ResearchStepMapper researchStepMapper,
                                ResearchTaskService researchTaskService,
                                @Value("${research.budget.max-tokens:60000}") long maxTokens,
                                @Value("${research.budget.max-tool-calls:60}") int maxToolCalls,
                                @Value("${research.budget.project-timeout-ms:600000}") long projectTimeoutMs) {
        this.applicationContext = applicationContext;
        this.researchProjectMapper = researchProjectMapper;
        this.researchPlanService = researchPlanService;
        this.researchStepMapper = researchStepMapper;
        this.researchTaskService = researchTaskService;
        this.maxTokens = maxTokens;
        this.maxToolCalls = maxToolCalls;
        this.projectTimeoutMs = projectTimeoutMs;
    }

    /**
     * 启动研究执行.
     *
     * <p>加载项目和研究计划，创建 AgentContext，按 agentChain 依次调度 Agent。</p>
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     */
    @Async
    public void start(Long projectId, Long userId) {
        try {
            executeInternal(projectId, userId);
        } catch (Exception e) {
            log.error("research_orchestrator_error projectId={}", projectId, e);
            try {
                ResearchProject project = researchProjectMapper.selectById(projectId);
                if (project != null && "RESEARCHING".equals(project.getStatus())) {
                    project.setStatus("FAILED");
                    researchProjectMapper.updateById(project);
                }
            } catch (Exception inner) {
                log.error("research_failed_mark_error projectId={}", projectId, inner);
            }
        }
    }

    private void executeInternal(Long projectId, Long userId) {
        ResearchProject project = researchProjectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "研究项目不存在");
        }
        if (!project.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权执行该研究项目");
        }

        // 获取最新研究计划
        ResearchPlanVO plan = getLatestPlan(projectId, userId);
        String agentChain = plan != null ? plan.getAgentChain() : null;

        // 构建执行上下文
        AgentContext context = new AgentContext();
        context.setProjectId(projectId);
        context.setUserId(userId);
        context.setWorkspaceId(project.getWorkspaceId());
        context.setResearchGoal(project.getGoal());
        context.setResearchPlan(plan);
        context.setMaxIterations(Math.max(
                project.getMaxIterations() != null ? project.getMaxIterations() : 3, 1));
        context.setCurrentIteration(1);
        context.setTokenBudget(new TokenBudget(maxTokens));
        context.setToolCallBudget(new ToolCallBudget(maxToolCalls));
        context.setTimeBudget(new TimeBudget(projectTimeoutMs));

        // 更新项目状态
        project.setStatus("RESEARCHING");
        project.setStartedAt(LocalDateTime.now());
        project.setCurrentIteration(1);
        researchProjectMapper.updateById(project);

        // 将所有任务标记为执行中
        try {
            researchTaskService.updateStatusByProject(projectId, userId, "RUNNING");
        } catch (Exception e) {
            log.warn("update_tasks_running_failed projectId={}", projectId, e);
        }

        log.info("research_started projectId={} agentChain={}", projectId, agentChain);

        // 若无研究计划，先调用 PlannerAgent 生成计划
        if (agentChain == null || agentChain.isBlank()) {
            log.info("research_no_plan_invoking_planner projectId={}", projectId);
            ResearchAgent planner = findAgent("PlannerAgent");
            if (planner != null) {
                try {
                    AgentResult plannerResult = planner.execute(context);
                    if ("COMPLETED".equals(plannerResult.getStatus())) {
                        context.recordAgentExecuted("PlannerAgent");
                        // 重新加载 Planner 生成的计划
                        plan = getLatestPlan(projectId, userId);
                        context.setResearchPlan(plan);
                        agentChain = plan != null ? plan.getAgentChain() : null;
                        log.info("research_planner_done projectId={} agentChain={}", projectId, agentChain);
                    } else {
                        log.warn("research_planner_failed projectId={} errors={}",
                                projectId, String.join("; ", plannerResult.getErrors()));
                    }
                } catch (Exception e) {
                    log.error("research_planner_error projectId={}", projectId, e);
                }
            } else {
                log.warn("research_planner_not_found projectId={}", projectId);
            }

            // Planner 执行后仍无 agentChain，标记完成
            if (agentChain == null || agentChain.isBlank()) {
                project.setStatus("FAILED");
                project.setCompletedAt(LocalDateTime.now());
                researchProjectMapper.updateById(project);
                try {
                    researchTaskService.updateStatusByProject(projectId, userId, "FAILED");
                } catch (Exception e) {
                    log.warn("update_tasks_failed_status_error projectId={}", projectId, e);
                }
                log.warn("research_failed_no_agent_chain projectId={}", projectId);
                return;
            }
        }

        // 按 agentChain 依次执行 Agent
        String[] agentNames = agentChain.split(",");
        for (String agentName : agentNames) {
            agentName = agentName.trim();
            if (context.isPaused()) {
                log.info("research_paused projectId={} at={}", projectId, agentName);
                break;
            }

            boolean completed = executeAgent(context, projectId, agentName);
            if (completed && "CriticAgent".equals(agentName)) {
                executeEvidenceRecoveryLoop(context, project, projectId);
            }
        }

        // 已产出部分结果但未通过质量门禁时保留 PARTIAL，避免界面把降级结果误报为成功。
        project.setStatus(context.isDegradedMode() ? "PARTIAL" : "COMPLETED");
        project.setCompletedAt(LocalDateTime.now());
        researchProjectMapper.updateById(project);

        try {
            String taskStatus = context.isDegradedMode() ? "FAILED" : "COMPLETED";
            researchTaskService.updateStatusByProject(projectId, userId, taskStatus);
        } catch (Exception e) {
            log.warn("update_tasks_final_status_failed projectId={}", projectId, e);
        }

        log.info("research_finished projectId={} degraded={}", projectId, context.isDegradedMode());
    }

    /**
     * 暂停研究执行.
     *
     * @param context 执行上下文
     */
    public void pause(AgentContext context) {
        context.setPaused(true);
        log.info("research_pause_requested projectId={} agent={}",
                context.getProjectId(), context.getCurrentAgent());
    }

    private boolean executeAgent(AgentContext context, Long projectId, String agentName) {
        ResearchAgent agent = findAgent(agentName);
        if (agent == null) {
            log.warn("agent_not_found name={} projectId={}", agentName, projectId);
            recordStep(projectId, null, agentName, "PROCESSING",
                    "Agent 未找到: " + agentName, "SKIPPED", null);
            return false;
        }
        if (!agent.shouldExecute(context)) {
            log.info("agent_skipped name={} projectId={}", agentName, projectId);
            return false;
        }

        context.setCurrentAgent(agentName);
        long startMs = System.currentTimeMillis();
        try {
            AgentResult result = agent.execute(context);
            long duration = System.currentTimeMillis() - startMs;
            if ("COMPLETED".equals(result.getStatus())) {
                consumeTokens(context, result);
                context.putAgentOutput(agentName, result.getOutput());
                context.recordAgentExecuted(agentName);
                recordStep(projectId, context.getCurrentTaskId(), agentName,
                        "PROCESSING", result.getOutput(), "COMPLETED", (int) duration);
                return true;
            }
            if ("FAILED".equals(result.getStatus())) {
                recordStep(projectId, context.getCurrentTaskId(), agentName,
                        "PROCESSING", String.join("; ", result.getErrors()),
                        "FAILED", (int) duration);
                context.setDegradedMode(true);
                return false;
            }
            recordStep(projectId, context.getCurrentTaskId(), agentName,
                    "PROCESSING", result.getOutput(), "SKIPPED", (int) duration);
            return false;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startMs;
            log.error("agent_error name={} projectId={}", agentName, projectId, e);
            recordStep(projectId, context.getCurrentTaskId(), agentName,
                    "PROCESSING", e.getMessage(), "FAILED", (int) duration);
            context.setDegradedMode(true);
            return false;
        } finally {
            context.setLastCheckpoint(Instant.now());
        }
    }

    private void executeEvidenceRecoveryLoop(AgentContext context,
                                             ResearchProject project,
                                             Long projectId) {
        SourceGrowthTracker sourceGrowthTracker =
                new SourceGrowthTracker(countResearchSources(context));
        while (requiresEvidenceRecovery(context)
                && context.getCurrentIteration() < context.getMaxIterations()
                && !context.isPaused()) {
            String budgetStopReason = resolveBudgetStopReason(context);
            if (budgetStopReason != null) {
                stopEvidenceRecovery(context, projectId, budgetStopReason);
                break;
            }

            int nextIteration = context.getCurrentIteration() + 1;
            context.setCurrentIteration(nextIteration);
            project.setCurrentIteration(nextIteration);
            researchProjectMapper.updateById(project);

            log.info("research_evidence_recovery projectId={} iteration={} maxIterations={}",
                    projectId, nextIteration, context.getMaxIterations());
            if (!executeAgent(context, projectId, "ResearchAgent")) {
                break;
            }
            int sourceCountAfter = countResearchSources(context);

            if (context.getRecoveryStopReason() != null) {
                stopEvidenceRecovery(context, projectId, context.getRecoveryStopReason());
                break;
            }
            String postResearchBudgetStopReason = resolveBudgetStopReason(context);
            if (postResearchBudgetStopReason != null) {
                stopEvidenceRecovery(context, projectId, postResearchBudgetStopReason);
                break;
            }
            if (sourceGrowthTracker.recordAndShouldStop(sourceCountAfter)) {
                stopEvidenceRecovery(context, projectId, "NO_NEW_SOURCES_TWO_ROUNDS");
                break;
            }
            if (!executeAgent(context, projectId, "CriticAgent")) {
                break;
            }
        }

        if (requiresEvidenceRecovery(context)) {
            context.setDegradedMode(true);
            log.warn("research_quality_gate_not_passed projectId={} iterations={}",
                    projectId, context.getCurrentIteration());
        }
    }

    private void consumeTokens(AgentContext context, AgentResult result) {
        TokenBudget budget = context.getTokenBudget();
        if (budget == null) {
            return;
        }
        long tokens = result.getTokensUsed() > 0
                ? result.getTokensUsed() : estimateTokens(result.getOutput());
        budget.consume(tokens);
    }

    private long estimateTokens(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        // AiService 当前不返回供应商 usage；按约 4 字符/Token 估算并向上取整。
        return Math.max(1, (content.length() + 3L) / 4L);
    }

    private String resolveBudgetStopReason(AgentContext context) {
        if (context.getTimeBudget() != null && context.getTimeBudget().isProjectTimeout()) {
            return "TIME_BUDGET_EXHAUSTED";
        }
        if (context.getTokenBudget() != null && context.getTokenBudget().isExceeded()) {
            return "TOKEN_BUDGET_EXHAUSTED";
        }
        if (context.getToolCallBudget() != null && !context.getToolCallBudget().canCall()) {
            return "TOOL_CALL_BUDGET_EXHAUSTED";
        }
        return null;
    }

    private void stopEvidenceRecovery(AgentContext context, Long projectId, String reason) {
        context.setRecoveryStopReason(reason);
        context.setDegradedMode(true);
        recordStep(projectId, context.getCurrentTaskId(), "QualityGate",
                "BUDGET_CHECK", reason, "SKIPPED", null);
        log.warn("research_evidence_recovery_stopped projectId={} reason={} iteration={}",
                projectId, reason, context.getCurrentIteration());
    }

    @SuppressWarnings("unchecked")
    private int countResearchSources(AgentContext context) {
        String researchOutput = context.getAgentOutput("ResearchAgent");
        if (researchOutput == null || researchOutput.isBlank()) {
            return 0;
        }
        try {
            Map<String, Object> researchData = objectMapper.readValue(researchOutput, Map.class);
            Object sources = researchData.get("sources");
            return sources instanceof java.util.List<?> sourceList ? sourceList.size() : 0;
        } catch (Exception e) {
            log.warn("parse_research_source_count_failed projectId={}", context.getProjectId(), e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean requiresEvidenceRecovery(AgentContext context) {
        String criticOutput = context.getAgentOutput("CriticAgent");
        if (criticOutput == null || criticOutput.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> criticData = objectMapper.readValue(criticOutput, Map.class);
            Object gateValue = criticData.get("qualityGate");
            if (!(gateValue instanceof Map<?, ?> qualityGate)) {
                return false;
            }
            return Boolean.TRUE.equals(qualityGate.get("retryRequired"));
        } catch (Exception e) {
            log.warn("parse_quality_gate_failed projectId={}", context.getProjectId(), e);
            context.setDegradedMode(true);
            return false;
        }
    }

    /**
     * 按名称查找 Agent Bean.
     */
    private ResearchAgent findAgent(String agentName) {
        try {
            return applicationContext.getBean(agentName, ResearchAgent.class);
        } catch (Exception e) {
            // 尝试按类名查找
            Map<String, ResearchAgent> agents = applicationContext.getBeansOfType(ResearchAgent.class);
            for (ResearchAgent agent : agents.values()) {
                if (agent.getName().equals(agentName)) {
                    return agent;
                }
            }
            return null;
        }
    }

    /**
     * 获取项目最新研究计划.
     */
    private ResearchPlanVO getLatestPlan(Long projectId, Long userId) {
        return researchPlanService.getLatestByProject(projectId, userId);
    }

    /**
     * 记录执行步骤到 research_step 表.
     */
    private void recordStep(Long projectId, Long taskId, String agentName,
                            String stepType, String content, String status, Integer durationMs) {
        try {
            ResearchStep step = new ResearchStep();
            step.setTaskId(taskId);
            step.setAgentName(agentName);
            step.setStepType(stepType);
            step.setTitle(content != null && content.length() > 200
                    ? content.substring(0, 200) : content);
            step.setContent(content);
            step.setStatus(status);
            step.setDurationMs(durationMs);
            researchStepMapper.insert(step);
        } catch (Exception e) {
            log.warn("research_step_insert_failed agent={} status={}", agentName, status, e);
        }
    }
}
