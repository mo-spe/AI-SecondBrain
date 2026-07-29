package com.secondbrain.research.orchestrator;

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

    private final ApplicationContext applicationContext;
    private final ResearchProjectMapper researchProjectMapper;
    private final ResearchPlanService researchPlanService;
    private final ResearchStepMapper researchStepMapper;
    private final ResearchTaskService researchTaskService;

    public ResearchOrchestrator(ApplicationContext applicationContext,
                                ResearchProjectMapper researchProjectMapper,
                                ResearchPlanService researchPlanService,
                                ResearchStepMapper researchStepMapper,
                                ResearchTaskService researchTaskService) {
        this.applicationContext = applicationContext;
        this.researchProjectMapper = researchProjectMapper;
        this.researchPlanService = researchPlanService;
        this.researchStepMapper = researchStepMapper;
        this.researchTaskService = researchTaskService;
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
        context.setMaxIterations(project.getMaxIterations() != null ? project.getMaxIterations() : 3);

        // 更新项目状态
        project.setStatus("RESEARCHING");
        project.setStartedAt(LocalDateTime.now());
        project.setCurrentIteration(0);
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
                project.setStatus("COMPLETED");
                project.setCompletedAt(LocalDateTime.now());
                researchProjectMapper.updateById(project);
                // 任务直接标记为完成（降级模式）
                try {
                    researchTaskService.updateStatusByProject(projectId, userId, "COMPLETED");
                } catch (Exception e) {
                    log.warn("update_tasks_completed_failed projectId={}", projectId, e);
                }
                log.info("research_completed_no_chain projectId={}", projectId);
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

            ResearchAgent agent = findAgent(agentName);
            if (agent == null) {
                log.warn("agent_not_found name={} projectId={}", agentName, projectId);
                recordStep(projectId, null, agentName, "PROCESSING",
                        "Agent 未找到: " + agentName, "SKIPPED", null);
                continue;
            }

            if (!agent.shouldExecute(context)) {
                log.info("agent_skipped name={} projectId={}", agentName, projectId);
                continue;
            }

            context.setCurrentAgent(agentName);
            long startMs = System.currentTimeMillis();

            try {
                AgentResult result = agent.execute(context);
                long duration = System.currentTimeMillis() - startMs;

                if ("COMPLETED".equals(result.getStatus())) {
                    context.putAgentOutput(agentName, result.getOutput());
                    context.recordAgentExecuted(agentName);
                    recordStep(projectId, context.getCurrentTaskId(), agentName,
                            "PROCESSING", result.getOutput(), "COMPLETED", (int) duration);
                } else if ("FAILED".equals(result.getStatus())) {
                    recordStep(projectId, context.getCurrentTaskId(), agentName,
                            "PROCESSING", String.join("; ", result.getErrors()),
                            "FAILED", (int) duration);
                    context.setDegradedMode(true);
                } else {
                    recordStep(projectId, context.getCurrentTaskId(), agentName,
                            "PROCESSING", result.getOutput(), "SKIPPED", (int) duration);
                }
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startMs;
                log.error("agent_error name={} projectId={}", agentName, projectId, e);
                recordStep(projectId, context.getCurrentTaskId(), agentName,
                        "PROCESSING", e.getMessage(), "FAILED", (int) duration);
                context.setDegradedMode(true);
            }

            context.setLastCheckpoint(Instant.now());
        }

        // 更新项目最终状态
        project.setStatus(context.isDegradedMode() ? "COMPLETED" : "COMPLETED");
        project.setCompletedAt(LocalDateTime.now());
        researchProjectMapper.updateById(project);

        // 将所有任务标记为完成（降级模式下仍标记为完成，因为已尽力）
        try {
            researchTaskService.updateStatusByProject(projectId, userId, "COMPLETED");
        } catch (Exception e) {
            log.warn("update_tasks_completed_failed projectId={}", projectId, e);
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
